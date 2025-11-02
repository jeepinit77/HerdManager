package com.jumblemint.cows.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

enum class ReplaceServerProgressPhase {
    CLEARING_COLLECTION,
    UPLOADING_COLLECTION
}

enum class ReplaceServerProgressStatus {
    STARTED,
    COMPLETED
}

data class ReplaceServerProgressEvent(
    val phase: ReplaceServerProgressPhase,
    val target: String,
    val status: ReplaceServerProgressStatus
)

class SyncService(
    private val repository: CattleRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        val CLOUD_COLLECTIONS = listOf(
            "cows",
            "pastures",
            "activities",
            "notes",
            "settings",
            "tagColors",
            "activityTypes",
            "breeds"
        )

        val FORCE_UPLOAD_COLLECTIONS = listOf(
            "cows",
            "pastures",
            "activities",
            "notes",
            "settings",
            "tagColors",
            "breeds",
            "activityTypes"
        )
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()

    private val _itemSyncStatus = MutableStateFlow(ItemSyncStatus.IDLE)
    val itemSyncStatus: Flow<ItemSyncStatus> = _itemSyncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: Flow<Long?> = _lastSyncTime.asStateFlow()

    private var activeListeners = mutableMapOf<String, ListenerRegistration>()

    init {
        serviceScope.launch {
            val stored = repository.getSettingByKey(SettingsKeys.LAST_SYNC_TIMESTAMP)?.value?.toLongOrNull()
            _lastSyncTime.value = stored
        }
    }

    private suspend fun recordSyncSuccess(userId: String) {
        val now = System.currentTimeMillis()
        _lastSyncTime.value = now

        try {
            val existing = repository.getSettingByKey(SettingsKeys.LAST_SYNC_TIMESTAMP)
            val setting = existing?.copy(
                value = now.toString(),
                updatedAt = now,
                lastSyncAt = now,
                updatedBy = userId
            ) ?: Settings(
                key = SettingsKeys.LAST_SYNC_TIMESTAMP,
                value = now.toString(),
                createdAt = now,
                updatedAt = now,
                lastSyncAt = now,
                updatedBy = userId
            )

            repository.insertOrUpdateSetting(setting)
        } catch (e: Exception) {
            println("Failed to persist last sync setting for user $userId: ${e.message}")
            e.printStackTrace()
        }

        try {
            repository.updateUserLastSync(userId, now)
        } catch (e: Exception) {
            println("Failed to update user last sync for $userId: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun checkServerDataExists(userId: String): Boolean {
        println("Checking for server data for user ID: $userId")
        try {
            // Check a few key collections. If any has documents, assume server data exists.
            // We limit to 1 document for efficiency, as we only need to know if *any* data exists.

            val cowsCollection = firestore.collection("users").document(userId).collection("cows")
            val cowsSnapshot = cowsCollection.limit(1).get().await()
            if (!cowsSnapshot.isEmpty) {
                println("Server data found in 'cows' collection for user $userId.")
                return true
            }

            val pasturesCollection = firestore.collection("users").document(userId).collection("pastures")
            val pasturesSnapshot = pasturesCollection.limit(1).get().await()
            if (!pasturesSnapshot.isEmpty) {
                println("Server data found in 'pastures' collection for user $userId.")
                return true
            }
            
            println("No server data found in key collections for user $userId.")
            return false
        } catch (e: Exception) {
            println("Error checking for server data for user $userId: ${e.message}")
            e.printStackTrace()
            return true 
        }
    }

    suspend fun clearServerData(
        userId: String,
        progressListener: ((ReplaceServerProgressEvent) -> Unit)? = null
    ) {
        println("Clearing all server data for user ID: $userId")
        try {
            clearServerCollections(userId, CLOUD_COLLECTIONS, progressListener)
            println("Successfully cleared all specified server data for user $userId.")
        } catch (e: Exception) {
            println("Error clearing server data for user $userId: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun clearServerCollections(
        userId: String,
        collections: List<String>,
        progressListener: ((ReplaceServerProgressEvent) -> Unit)? = null
    ) {
        try {
            for (collectionName in collections) {
                progressListener?.invoke(
                    ReplaceServerProgressEvent(
                        phase = ReplaceServerProgressPhase.CLEARING_COLLECTION,
                        target = collectionName,
                        status = ReplaceServerProgressStatus.STARTED
                    )
                )
                val collectionRef = firestore.collection("users").document(userId).collection(collectionName)
                val snapshot = collectionRef.get().await()
                if (snapshot.isEmpty) {
                    println("No documents found in '$collectionName' for user $userId to delete.")
                } else {
                    val batch = firestore.batch()
                    for (document in snapshot.documents) {
                        batch.delete(document.reference)
                    }
                    batch.commit().await()
                    println("Successfully deleted all documents from '$collectionName' for user $userId.")
                }
                progressListener?.invoke(
                    ReplaceServerProgressEvent(
                        phase = ReplaceServerProgressPhase.CLEARING_COLLECTION,
                        target = collectionName,
                        status = ReplaceServerProgressStatus.COMPLETED
                    )
                )
            }
        } catch (e: Exception) {
            println("Error clearing some collections for user $userId: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun forceUploadAllData(
        userId: String,
        progressListener: ((ReplaceServerProgressEvent) -> Unit)? = null
    ) {
        println("Starting force upload of all local data for user ID: $userId")
        _syncStatus.value = SyncStatus.SYNCING
        try {
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "cows",
                    status = ReplaceServerProgressStatus.STARTED
                )
            )
            val localCows = repository.getAllCowsSync()
            println("Found ${localCows.size} local cows to force upload.")
            for (cow in localCows) {
                val firestoreId = cow.firestoreId ?: UUID.randomUUID().toString()
                val cowData = cow.toFirestoreMap(userId, repository) 
                val updatedTimestamp = cowData["updatedAt"] as? Long ?: System.currentTimeMillis()
                firestore.collection("users").document(userId).collection("cows").document(firestoreId).set(cowData).await()
                repository.updateCow(cow.copy(
                    firestoreId = firestoreId,
                    lastSyncAt = updatedTimestamp,
                    updatedBy = userId
                ))
                println("Force uploaded cow: ${cow.name} (FS ID: $firestoreId)")
            }
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "cows",
                    status = ReplaceServerProgressStatus.COMPLETED
                )
            )

            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "pastures",
                    status = ReplaceServerProgressStatus.STARTED
                )
            )
            val localPastures = repository.getAllPasturesSync()
            println("Found ${localPastures.size} local pastures to force upload.")
            for (pasture in localPastures) {
                val firestoreId = pasture.firestoreId ?: pasture.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                val pastureData = pasture.toFirestoreMap(userId)
                val updatedTimestamp = pastureData["updatedAt"] as? Long ?: System.currentTimeMillis()
                firestore.collection("users").document(userId).collection("pastures").document(firestoreId).set(pastureData).await()

                val shouldUpdatePrimaryKey = pasture.id.isBlank()
                val normalizedPasture = pasture.copy(
                    id = if (shouldUpdatePrimaryKey) firestoreId else pasture.id,
                    firestoreId = firestoreId,
                    lastSyncAt = updatedTimestamp,
                    updatedBy = userId
                )
                repository.updatePasture(normalizedPasture)
                println("Force uploaded pasture: ${pasture.name} (FS ID: $firestoreId)")
            }
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "pastures",
                    status = ReplaceServerProgressStatus.COMPLETED
                )
            )

            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "activities",
                    status = ReplaceServerProgressStatus.STARTED
                )
            )
            val localActivities = repository.getAllActivitiesSync()
            println("Found ${localActivities.size} local activities to force upload.")
            for (activity in localActivities) {
                val firestoreId = activity.firestoreId ?: UUID.randomUUID().toString()
                val activityData = activity.toFirestoreMap(userId, repository)
                val updatedTimestamp = activityData["updatedAt"] as? Long ?: System.currentTimeMillis()
                firestore.collection("users").document(userId).collection("activities").document(firestoreId).set(activityData).await()
                repository.updateActivity(activity.copy(
                    firestoreId = firestoreId,
                    lastSyncAt = updatedTimestamp,
                    updatedBy = userId
                ))
                println("Force uploaded activity: ${activity.activityType} (FS ID: $firestoreId)")
            }
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "activities",
                    status = ReplaceServerProgressStatus.COMPLETED
                )
            )

            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "notes",
                    status = ReplaceServerProgressStatus.STARTED
                )
            )
            val localNotes = repository.getAllNotesSync()
            println("Found ${localNotes.size} local notes to force upload.")
            for (note in localNotes) {
                val firestoreId = note.firestoreId ?: UUID.randomUUID().toString()
                val noteData = note.toFirestoreMap(userId)
                val updatedTimestamp = noteData["updatedAt"] as? Long ?: System.currentTimeMillis()
                firestore.collection("users").document(userId).collection("notes").document(firestoreId).set(noteData).await()
                repository.updateNote(note.copy(
                    firestoreId = firestoreId,
                    lastSyncAt = updatedTimestamp,
                    updatedBy = userId
                ))
                println("Force uploaded note: ${note.title} (FS ID: $firestoreId)")
            }
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "notes",
                    status = ReplaceServerProgressStatus.COMPLETED
                )
            )

            // Force upload settings
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "settings",
                    status = ReplaceServerProgressStatus.STARTED
                )
            )
            val localSettings = repository.getAllSettings().first()
            println("Found ${localSettings.size} local settings to force upload.")
            for (setting in localSettings) {
                val firestoreId = setting.firestoreId ?: setting.key
                val settingData = setting.toFirestoreMap(userId)
                val updatedTimestamp = settingData["updatedAt"] as? Long ?: System.currentTimeMillis()
                firestore.collection("users").document(userId).collection("settings").document(firestoreId).set(settingData).await()
                repository.insertOrUpdateSetting(setting.copy(
                    firestoreId = firestoreId,
                    lastSyncAt = updatedTimestamp,
                    updatedBy = userId
                ))
                println("Force uploaded setting: ${setting.key} (FS ID: $firestoreId)")
            }
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "settings",
                    status = ReplaceServerProgressStatus.COMPLETED
                )
            )

            // Force upload tag colors
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "tagColors",
                    status = ReplaceServerProgressStatus.STARTED
                )
            )
            val localTagColors = repository.getAllTagColorsSync()
            println("Found ${localTagColors.size} local tag colors to force upload.")
            for (tagColor in localTagColors) {
                val firestoreId = tagColor.firestoreId ?: tagColor.id
                val tagColorData = tagColor.toFirestoreMap(userId)
                val updatedTimestamp = tagColorData["updatedAt"] as? Long ?: System.currentTimeMillis()
                firestore.collection("users").document(userId).collection("tagColors").document(firestoreId).set(tagColorData).await()
                repository.updateTagColor(tagColor.copy(
                    firestoreId = firestoreId,
                    lastSyncAt = updatedTimestamp,
                    updatedBy = userId
                ))
                println("Force uploaded tag color: ${tagColor.name} (FS ID: $firestoreId)")
            }
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "tagColors",
                    status = ReplaceServerProgressStatus.COMPLETED
                )
            )

            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "breeds",
                    status = ReplaceServerProgressStatus.STARTED
                )
            )
            val localBreeds = repository.getAllBreedsSync()
            println("Found ${localBreeds.size} local breeds to force upload.")
            for (breed in localBreeds) {
                val firestoreId = breed.firestoreId ?: breed.id
                val breedData = breed.toFirestoreMap(userId)
                val updatedTimestamp = breedData["updatedAt"] as? Long ?: System.currentTimeMillis()
                val collection = firestore.collection("users").document(userId).collection("breeds").document(firestoreId)
                if (breed.isDeleted) {
                    collection.delete().await()
                } else {
                    collection.set(breedData).await()
                }
                repository.updateBreed(
                    breed.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = updatedTimestamp,
                        updatedBy = userId
                    )
                )
                println("Force uploaded breed: ${breed.name} (FS ID: $firestoreId)")
            }
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "breeds",
                    status = ReplaceServerProgressStatus.COMPLETED
                )
            )

            // Force upload activity types
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "activityTypes",
                    status = ReplaceServerProgressStatus.STARTED
                )
            )
            val localActivityTypes = repository.getAllActivityTypesSync()
            println("Found ${localActivityTypes.size} local activity types to force upload.")
            for (activityType in localActivityTypes) {
                val firestoreId = activityType.firestoreId ?: activityType.id
                val activityTypeData = activityType.toFirestoreMap(userId)
                val updatedTimestamp = activityTypeData["updatedAt"] as? Long ?: System.currentTimeMillis()
                firestore.collection("users").document(userId).collection("activityTypes").document(firestoreId).set(activityTypeData).await()
                repository.updateActivityType(activityType.copy(
                    firestoreId = firestoreId,
                    lastSyncAt = updatedTimestamp,
                    updatedBy = userId
                ))
                println("Force uploaded activity type: ${activityType.name} (FS ID: $firestoreId)")
            }
            progressListener?.invoke(
                ReplaceServerProgressEvent(
                    phase = ReplaceServerProgressPhase.UPLOADING_COLLECTION,
                    target = "activityTypes",
                    status = ReplaceServerProgressStatus.COMPLETED
                )
            )

            println("Force upload of all local data completed for user ID: $userId")
            _syncStatus.value = SyncStatus.SUCCESS
            recordSyncSuccess(userId)
        } catch (e: Exception) {
            println("Error during force upload of all local data for user $userId: ${e.message}")
            e.printStackTrace()
            _syncStatus.value = SyncStatus.ERROR
            throw e
        } finally {
            if (_syncStatus.value != SyncStatus.IDLE) {
                delay(1000) 
                _syncStatus.value = SyncStatus.IDLE
            }
        }
    }
    
    suspend fun syncUserData(userId: String): Result<Unit> {
        if (_syncStatus.value == SyncStatus.SYNCING) {
            println("Sync already in progress for user: $userId. Skipping.")
            return Result.success(Unit) 
        }
        return try {
            println("Starting sync for user: $userId")
            _syncStatus.value = SyncStatus.SYNCING
            
            println("Syncing settings...")
            syncUserSettings(userId)
            println("Syncing tag colors...")
            syncUserTagColors(userId)
            println("Syncing breeds...")
            syncUserBreeds(userId)
            println("Syncing activity types...")
            syncUserActivityTypes(userId)
            println("Syncing pastures...")
            syncUserPastures(userId)
            println("Syncing cows...")
            syncUserCows(userId)
            println("Syncing activities...")
            syncUserActivities(userId)
            println("Syncing notes...")
            syncUserNotes(userId)
            
            println("Sync completed successfully for user: $userId")
            _syncStatus.value = SyncStatus.SUCCESS
            recordSyncSuccess(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            println("Sync error for user $userId: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (_syncStatus.value != SyncStatus.IDLE && _syncStatus.value != SyncStatus.SUCCESS) { 
                 _syncStatus.value = SyncStatus.IDLE 
            }
        }
    }
    
    fun startRealtimeSync(userId: String) {
        stopRealtimeSync(userId) 
        
        println("Starting real-time sync for user: $userId")

        val pastureListener = firestore.collection("users").document(userId)
            .collection("pastures")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Real-time sync error for pastures: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    CoroutineScope(Dispatchers.IO).launch { handleRealtimePastureChange(change, userId) }
                }
            }
        activeListeners["$userId-pastures"] = pastureListener

        val cowListener = firestore.collection("users").document(userId)
            .collection("cows")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Real-time sync error for cows: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    CoroutineScope(Dispatchers.IO).launch { handleRealtimeCowChange(change, userId) }
                }
            }
        activeListeners["$userId-cows"] = cowListener

        val activityListener = firestore.collection("users").document(userId)
            .collection("activities")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Real-time sync error for activities: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    CoroutineScope(Dispatchers.IO).launch { handleRealtimeActivityChange(change, userId) }
                }
            }
        activeListeners["$userId-activities"] = activityListener

        val noteListener = firestore.collection("users").document(userId)
            .collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Real-time sync error for notes: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                     CoroutineScope(Dispatchers.IO).launch { handleRealtimeNoteChange(change, userId) }
                }
            }
        activeListeners["$userId-notes"] = noteListener

        val settingsListener = firestore.collection("users").document(userId)
            .collection("settings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Real-time sync error for settings: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    CoroutineScope(Dispatchers.IO).launch { handleRealtimeSettingsChange(change, userId) }
                }
            }
        activeListeners["$userId-settings"] = settingsListener

        val tagColorsListener = firestore.collection("users").document(userId)
            .collection("tagColors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Real-time sync error for tag colors: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    CoroutineScope(Dispatchers.IO).launch { handleRealtimeTagColorChange(change, userId) }
                }
            }
        activeListeners["$userId-tagColors"] = tagColorsListener

        val breedsListener = firestore.collection("users").document(userId)
            .collection("breeds")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Real-time sync error for breeds: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    CoroutineScope(Dispatchers.IO).launch { handleRealtimeBreedChange(change, userId) }
                }
            }
        activeListeners["$userId-breeds"] = breedsListener

        val activityTypesListener = firestore.collection("users").document(userId)
            .collection("activityTypes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Real-time sync error for activity types: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    CoroutineScope(Dispatchers.IO).launch { handleRealtimeActivityTypeChange(change, userId) }
                }
            }
        activeListeners["$userId-activityTypes"] = activityTypesListener
        
        println("Real-time sync listeners established for user: $userId for all collections")
    }
    
    fun stopRealtimeSync(userId: String) {
        listOf("cows", "pastures", "activities", "notes", "settings", "tagColors", "breeds", "activityTypes").forEach { collection ->
            activeListeners["$userId-$collection"]?.remove()
            activeListeners.remove("$userId-$collection")
        }
        println("Stopped real-time sync for user: $userId")
    }
    
    fun stopAllRealtimeSync() {
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
        println("Stopped all real-time sync listeners.")
    }
    
    suspend fun syncItemImmediately(userId: String, item: Any): Result<Unit> {
        return try {
            _itemSyncStatus.value = ItemSyncStatus.SYNCING
            when (item) {
                is TagColor -> {
                    val firestoreId = item.firestoreId ?: item.id
                    val tagColorData = item.toFirestoreMap(userId)
                    val updatedTimestamp = tagColorData["updatedAt"] as? Long ?: System.currentTimeMillis()

                    // Update local item with firestoreId BEFORE writing to Firestore
                    repository.updateTagColor(item.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = updatedTimestamp,
                        updatedBy = userId
                    ))

                    if (item.isDeleted) {
                        // Delete from server if item is marked as deleted
                        firestore.collection("users").document(userId).collection("tagColors").document(firestoreId).delete().await()
                        println("Immediately deleted tag color: ${item.name} with FS ID: $firestoreId")
                    } else {
                        // Otherwise upload the data
                        firestore.collection("users").document(userId).collection("tagColors").document(firestoreId).set(tagColorData).await()
                        println("Immediately synced tag color: ${item.name} with FS ID: $firestoreId")
                    }
                }
                is Breed -> {
                    val firestoreId = item.firestoreId ?: item.id
                    val breedData = item.toFirestoreMap(userId)
                    val updatedTimestamp = breedData["updatedAt"] as? Long ?: System.currentTimeMillis()

                    repository.updateBreed(
                        item.copy(
                            firestoreId = firestoreId,
                            lastSyncAt = updatedTimestamp,
                            updatedBy = userId
                        )
                    )

                    val docRef = firestore.collection("users").document(userId).collection("breeds").document(firestoreId)
                    if (item.isDeleted) {
                        docRef.delete().await()
                        println("Immediately deleted breed: ${item.name} with FS ID: $firestoreId")
                    } else {
                        docRef.set(breedData).await()
                        println("Immediately synced breed: ${item.name} with FS ID: $firestoreId")
                    }
                }
                is ActivityTypeConfig -> {
                    val firestoreId = item.firestoreId ?: item.id
                    val activityTypeData = item.toFirestoreMap(userId)
                    val updatedTimestamp = activityTypeData["updatedAt"] as? Long ?: System.currentTimeMillis()

                    // Update local item with firestoreId BEFORE writing to Firestore
                    repository.updateActivityType(item.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = updatedTimestamp,
                        updatedBy = userId
                    ))

                    if (item.isDeleted) {
                        // Delete from server if item is marked as deleted
                        firestore.collection("users").document(userId).collection("activityTypes").document(firestoreId).delete().await()
                        println("Immediately deleted activity type: ${item.name} with FS ID: $firestoreId")
                    } else {
                        // Otherwise upload the data
                        firestore.collection("users").document(userId).collection("activityTypes").document(firestoreId).set(activityTypeData).await()
                        println("Immediately synced activity type: ${item.name} with FS ID: $firestoreId")
                    }
                }
                is Cow -> {
                    val firestoreId = item.firestoreId ?: UUID.randomUUID().toString()
                    val cowData = item.toFirestoreMap(userId, repository)
                    val updatedTimestamp = cowData.get("updatedAt") as? Long ?: System.currentTimeMillis()
                    
                    // Update local item with firestoreId BEFORE writing to Firestore
                    repository.updateCow(item.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = updatedTimestamp,
                        updatedBy = userId
                    ))
                    
                    firestore.collection("users").document(userId).collection("cows").document(firestoreId).set(cowData).await()
                    println("Immediately synced cow: ${item.name} with FS ID: $firestoreId")
                }
                is Pasture -> {
                    val firestoreId = item.firestoreId
                        ?: item.id.takeUnless { it.isBlank() }
                        ?: UUID.randomUUID().toString()

                    val normalizedPasture = item.copy(
                        firestoreId = firestoreId,
                        id = if (item.id.isBlank()) firestoreId else item.id,
                        updatedBy = userId
                    )

                    val pastureData = normalizedPasture.toFirestoreMap(userId)
                    val updatedTimestamp = pastureData["updatedAt"] as? Long ?: System.currentTimeMillis()

                    // Persist updated sync metadata before writing to Firestore to avoid listener races
                    repository.updatePasture(
                        normalizedPasture.copy(lastSyncAt = updatedTimestamp)
                    )

                    firestore.collection("users").document(userId)
                        .collection("pastures")
                        .document(firestoreId)
                        .set(pastureData)
                        .await()
                    println("Immediately synced pasture: ${item.name} with FS ID: $firestoreId")
                }
                is Activity -> {
                    val firestoreId = item.firestoreId ?: UUID.randomUUID().toString()
                    val activityData = item.toFirestoreMap(userId, repository)
                    val updatedTimestamp = activityData.get("updatedAt") as? Long ?: System.currentTimeMillis()
                    
                    // Update local item with firestoreId BEFORE writing to Firestore
                    // This prevents real-time listener from creating duplicates
                    repository.updateActivity(item.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = updatedTimestamp,
                        updatedBy = userId
                    ))
                    
                    firestore.collection("users").document(userId).collection("activities").document(firestoreId).set(activityData).await()
                    println("Immediately synced activity: ${item.activityType} with FS ID: $firestoreId")
                }
                is Note -> {
                    val firestoreId = item.firestoreId ?: UUID.randomUUID().toString()
                    val noteData = item.toFirestoreMap(userId)
                    val updatedTimestamp = noteData.get("updatedAt") as? Long ?: System.currentTimeMillis()
                    
                    // Update local item with firestoreId BEFORE writing to Firestore
                    repository.updateNote(item.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = updatedTimestamp,
                        updatedBy = userId
                    ))
                    
                    firestore.collection("users").document(userId).collection("notes").document(firestoreId).set(noteData).await()
                    println("Immediately synced note: ${item.title} with FS ID: $firestoreId")
                }
            }
            _itemSyncStatus.value = ItemSyncStatus.SUCCESS
            _itemSyncStatus.value = ItemSyncStatus.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            println("Failed to immediately sync item: ${e.message}")
            e.printStackTrace()
            _itemSyncStatus.value = ItemSyncStatus.ERROR
            delay(2000)
            _itemSyncStatus.value = ItemSyncStatus.IDLE
            Result.failure(e)
        }
    }
    
    private suspend fun syncUserPastures(userId: String) {
        try {
            val localPastures = repository.getAllPasturesSync()
            val remoteSnapshot = firestore.collection("users").document(userId).collection("pastures").get().await()
            val remotePasturesMap = remoteSnapshot.documents.associate { it.id to (it.data ?: emptyMap<String, Any>()) }

            // Upload local changes to Firestore
            localPastures.forEach { pasture ->
                try {
                    // If pasture.id is already a UUID (set at local creation), it will be used.
                    // If pasture.firestoreId is also set to pasture.id, this ?: is a fallback for older data.
                    val firestoreId = pasture.firestoreId ?: pasture.id.takeUnless { it.isBlank() } ?: UUID.randomUUID().toString()
                    val remoteData = remotePasturesMap[firestoreId]
                    val normalizedPasture = pasture.copy(
                        firestoreId = firestoreId,
                        id = if (pasture.id.isBlank()) firestoreId else pasture.id,
                        updatedBy = userId
                    )
                    val pastureData = normalizedPasture.toFirestoreMap(userId) // This sets/updates createdBy, updatedBy, updatedAt
                    val localUpdatedAt = pastureData["updatedAt"] as? Long ?: pasture.lastSyncAt

                    val shouldUpload = when {
                        pasture.firestoreId == null -> true // New local item not yet uploaded
                        remoteData == null -> true      // Item exists locally but not on server (should be uploaded)
                        else -> {
                            val remoteUpdatedAt = remoteData["updatedAt"] as? Long ?: 0L
                            (localUpdatedAt > remoteUpdatedAt) || (pasture.isDeleted && !(remoteData["isDeleted"] as? Boolean ?: false))
                        }
                    }
                    if (shouldUpload) {
                        firestore.collection("users").document(userId)
                            .collection("pastures")
                            .document(firestoreId)
                            .set(pastureData)
                            .await()
                        repository.updatePasture(normalizedPasture.copy(lastSyncAt = localUpdatedAt))
                        println("Uploaded pasture: ${pasture.name} (FS ID: $firestoreId)")
                    }
                } catch (e: Exception) { println("Error uploading pasture ${pasture.name}: ${e.message}") }
            }

            // Download remote changes to local
            val currentLocalPastures = repository.getAllPasturesSync().associateBy { it.firestoreId ?: it.id }
            remotePasturesMap.forEach { (firestoreId, data) ->
                try {
                    val localPasture = currentLocalPastures[firestoreId]
                    val remoteUpdatedAt = data["updatedAt"] as? Long ?: 0L
                    val isRemoteDeleted = data["isDeleted"] as? Boolean ?: false

                    val shouldProcess = when {
                        isRemoteDeleted -> {
                            localPasture?.let {
                                if (!it.isDeleted) repository.updatePasture(it.copy(isDeleted = true, lastSyncAt = remoteUpdatedAt, updatedBy = data["updatedBy"] as? String))
                            }
                            false // Processed deletion, no further processing needed for this item
                        }
                        localPasture == null -> true // New item from server
                        else -> remoteUpdatedAt > localPasture.lastSyncAt && !localPasture.isDeleted // Remote is newer and local is not deleted
                    }

                    if (shouldProcess && !isRemoteDeleted) {
                        val resolvedLocalId = localPasture?.id?.takeUnless { it.isBlank() } ?: firestoreId
                        val remotePasture = Pasture(
                            id = resolvedLocalId,
                            name = data["name"] as? String ?: "",
                            description = data["description"] as? String,
                            sizeAcres = (data["sizeAcres"] as? Number)?.toDouble(),
                            herdId = data["herdId"] as? String,
                            firestoreId = firestoreId,
                            lastSyncAt = remoteUpdatedAt,
                            isDeleted = false, // If it was deleted, shouldProcess would be false
                            createdBy = data["createdBy"] as? String,
                            updatedBy = data["updatedBy"] as? String
                        )
                        if (localPasture == null) {
                            repository.insertPasture(remotePasture)
                            println("Downloaded new pasture: ${remotePasture.name} (FS ID: $firestoreId)")
                        } else {
                            repository.updatePasture(remotePasture) // id matches, so it's an update
                            println("Updated local pasture from remote: ${remotePasture.name} (FS ID: $firestoreId)")
                        }
                    }
                } catch (e: Exception) { println("Error downloading/processing pasture $firestoreId: ${e.message}"); e.printStackTrace() }
            }
            println("Pasture sync completed")
        } catch (e: Exception) { println("Error in syncUserPastures: ${e.message}"); throw e }
    }
    
    private suspend fun syncUserCows(userId: String) {
        try {
            val localCows = repository.getAllCowsSync()
            val remoteSnapshot = firestore.collection("users").document(userId).collection("cows").get().await()
            val remoteCowsMap = remoteSnapshot.documents.associate { it.id to (it.data ?: emptyMap<String, Any>()) }

            localCows.forEach { cow ->
                try {
                    val firestoreId = cow.firestoreId ?: UUID.randomUUID().toString() // Use UUID for consistency
                    val remoteData = remoteCowsMap[firestoreId]
                    val cowData = cow.toFirestoreMap(userId, repository)
                    val localUpdatedAt = cowData["updatedAt"] as? Long ?: cow.lastSyncAt
                    
                    val shouldUpload = when {
                        cow.firestoreId == null -> true
                        remoteData == null -> true
                        else -> {
                            val remoteUpdatedAtTs = remoteData["updatedAt"] as? Long ?: 0L
                            (localUpdatedAt > remoteUpdatedAtTs) || (cow.isDeleted && !(remoteData["isDeleted"] as? Boolean ?: false))
                        }
                    }
                    if (shouldUpload) {
                        firestore.collection("users").document(userId).collection("cows").document(firestoreId).set(cowData).await()
                        repository.updateCow(cow.copy(
                            firestoreId = firestoreId, 
                            // id = firestoreId, // Cow ID is Long, can't directly assign String firestoreId
                            lastSyncAt = localUpdatedAt, 
                            updatedBy = userId
                        ))
                        println("Uploaded cow: ${cow.name}")
                    }
                } catch (e: Exception) { println("Error uploading cow ${cow.name}: ${e.message}") }
            }

            val currentLocalCows = repository.getAllCowsSync().associateBy { it.firestoreId }
            remoteCowsMap.forEach { (firestoreId, data) ->
                try {
                    val localCow = currentLocalCows[firestoreId]
                    val remoteUpdatedAt = data["updatedAt"] as? Long ?: 0L
                    val isRemoteDeleted = data["isDeleted"] as? Boolean ?: false

                    val shouldProcess = when {
                        isRemoteDeleted -> {
                            localCow?.let { if (!it.isDeleted) repository.updateCow(it.copy(isDeleted = true, lastSyncAt = remoteUpdatedAt, updatedBy = data["updatedBy"] as? String)) }
                            false
                        }
                        localCow == null -> true
                        else -> remoteUpdatedAt > localCow.lastSyncAt && !localCow.isDeleted
                    }

                    if (shouldProcess && !isRemoteDeleted) {
                        // Resolve mother and father references using firestoreIds
                        val motherFirestoreId = data["motherFirestoreId"] as? String
                        val fatherFirestoreId = data["fatherFirestoreId"] as? String
                        val localMotherCow = motherFirestoreId?.let { currentLocalCows[it] }
                        val localFatherCow = fatherFirestoreId?.let { currentLocalCows[it] }
                        
                        val remoteCow = Cow(
                            id = localCow?.id ?: 0L, // Keep local Long ID if exists, else 0 for new insert
                            name = data["name"] as? String,
                            tagNumber = data["tagNumber"] as? String,
                            tagColor = data["tagColor"] as? String,
                            birthDate = (data["birthDate"] as? String)?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { null } },
                            gender = try { Gender.valueOf(data["gender"] as? String ?: Gender.TBD.name) } catch (e: Exception) { Gender.TBD },
                            classification = try { Classification.valueOf(data["classification"] as? String ?: Classification.CALF.name) } catch (e: Exception) { Classification.CALF },
                            colorMarkings = data["colorMarkings"] as? String,
                            registrationNumber = data["registrationNumber"] as? String,
                            breed = data["breed"] as? String,
                            motherId = localMotherCow?.id ?: (data["motherId"] as? Number)?.toLong(), // Use local mother ID if found, fallback to stored ID
                            fatherId = localFatherCow?.id ?: (data["fatherId"] as? Number)?.toLong(), // Use local father ID if found, fallback to stored ID
                            status = try { Status.valueOf(data["status"] as? String ?: Status.ACTIVE.name) } catch (e: Exception) { Status.ACTIVE },
                            pastureId = data["pastureId"] as? String,
                            photos = (data["photos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            isWatched = data["isWatched"] as? Boolean ?: false,
                            herdId = data["herdId"] as? String,
                            createdAt = (data["createdDate"] as? String)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                            updatedAt = (data["updatedDate"] as? String)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                            firestoreId = firestoreId,
                            lastSyncAt = remoteUpdatedAt,
                            isDeleted = false,
                            createdBy = data["createdBy"] as? String,
                            updatedBy = data["updatedBy"] as? String
                        )
                        if (localCow == null) repository.insertCow(remoteCow)
                        else repository.updateCow(remoteCow.copy(id = localCow.id))
                        println("Downloaded cow: ${remoteCow.name}")
                    }
                } catch (e: Exception) { println("Error downloading cow $firestoreId: ${e.message}") }
            }
            println("Cow sync completed")
        } catch (e: Exception) { println("Error in syncUserCows: ${e.message}"); throw e }
    }

    private suspend fun syncUserActivities(userId: String) {
        try {
            val localActivities = repository.getAllActivitiesSync()
            val remoteSnapshot = firestore.collection("users").document(userId).collection("activities").get().await()
            val remoteActivitiesMap = remoteSnapshot.documents.associate { it.id to (it.data ?: emptyMap<String, Any>()) }

            // Create a mapping from firestoreId to local cow for proper cow reference resolution
            val localCows = repository.getAllCowsSync()
            val firestoreIdToLocalCow = localCows.associateBy { it.firestoreId }

            localActivities.forEach { activity ->
                try {
                    val firestoreId = activity.firestoreId ?: UUID.randomUUID().toString() // Use UUID for consistency
                    val remoteData = remoteActivitiesMap[firestoreId]
                    val activityData = activity.toFirestoreMap(userId, repository)
                    val localUpdatedAt = activityData["updatedAt"] as? Long ?: activity.lastSyncAt

                    val shouldUpload = when {
                        activity.firestoreId == null -> true
                        remoteData == null -> true
                        else -> {
                            val remoteUpdatedAtTs = remoteData["updatedAt"] as? Long ?: 0L
                            (localUpdatedAt > remoteUpdatedAtTs) || (activity.isDeleted && !(remoteData["isDeleted"] as? Boolean ?: false))
                        }
                    }
                    if (shouldUpload) {
                        firestore.collection("users").document(userId).collection("activities").document(firestoreId).set(activityData).await()
                        repository.updateActivity(activity.copy(
                            firestoreId = firestoreId, 
                            lastSyncAt = localUpdatedAt, 
                            updatedBy = userId
                        ))
                        println("Uploaded activity: ${activity.activityType}")
                    }
                } catch (e: Exception) { println("Error uploading activity ${activity.activityType}: ${e.message}") }
            }

            val currentLocalActivities = repository.getAllActivitiesSync().associateBy { it.firestoreId }
            remoteActivitiesMap.forEach { (firestoreId, data) ->
                try {
                    val localActivity = currentLocalActivities[firestoreId]
                    val remoteUpdatedAt = data["updatedAt"] as? Long ?: 0L
                    val isRemoteDeleted = data["isDeleted"] as? Boolean ?: false

                    val shouldProcess = when {
                        isRemoteDeleted -> {
                            localActivity?.let { if (!it.isDeleted) repository.updateActivity(it.copy(isDeleted = true, lastSyncAt = remoteUpdatedAt, updatedBy = data["updatedBy"] as? String)) }
                            false
                        }
                        localActivity == null -> true
                        else -> remoteUpdatedAt > localActivity.lastSyncAt && !localActivity.isDeleted
                    }

                    if (shouldProcess && !isRemoteDeleted) {
                        val primaryCowFirestoreId = data["cowFirestoreId"] as? String
                        val remoteCowFirestoreIds = (data["cowFirestoreIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val resolvedCowIds = LinkedHashSet<Long>()
                        remoteCowFirestoreIds.forEach { fsId ->
                            firestoreIdToLocalCow[fsId]?.id?.let { resolvedCowIds += it }
                        }

                        if (resolvedCowIds.isEmpty()) {
                            val remoteCowIdList = (data["cowIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()
                            remoteCowIdList.forEach { resolvedCowIds += it }
                        }

                        val fallbackCowId = (data["cowId"] as? Number)?.toLong()
                        val primaryCowId = resolvedCowIds.firstOrNull() ?: fallbackCowId
                        val primaryCow = when {
                            primaryCowId != null -> repository.getCowById(primaryCowId)
                            primaryCowFirestoreId != null -> firestoreIdToLocalCow[primaryCowFirestoreId]
                            else -> null
                        }

                        if (primaryCow != null) {
                            val cowIdsForActivity = if (resolvedCowIds.isNotEmpty()) {
                                resolvedCowIds.toList()
                            } else {
                                listOf(primaryCow.id)
                            }

                            val remoteActivity = Activity(
                                id = localActivity?.id ?: 0L, // Keep local Long ID
                                cowId = primaryCow.id,
                                date = (data["date"] as? String)?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { java.time.LocalDate.now() } } ?: java.time.LocalDate.now(),
                                activityType = try { ActivityType.valueOf(data["activityType"] as? String ?: ActivityType.OTHER.name) } catch (e: Exception) { ActivityType.OTHER },
                                notes = data["notes"] as? String,
                                fromPastureId = data["fromPastureId"] as? String,
                                toPastureId = data["toPastureId"] as? String,
                                details = data["details"] as? String,
                                groupId = data["groupId"] as? String,
                                result = data["result"] as? String,
                                quantity = (data["quantity"] as? Number)?.toDouble(),
                                technician = data["technician"] as? String,
                                cost = (data["cost"] as? Number)?.toDouble(),
                                herdId = data["herdId"] as? String,
                                firestoreId = firestoreId,
                                lastSyncAt = remoteUpdatedAt,
                                isDeleted = false,
                                createdBy = data["createdBy"] as? String,
                                updatedBy = data["updatedBy"] as? String,
                                cowIds = cowIdsForActivity
                            )

                            if (localActivity == null) repository.insertActivity(remoteActivity)
                            else repository.updateActivity(remoteActivity.copy(id = localActivity.id))
                            println("Downloaded activity: ${remoteActivity.activityType} for cow ${primaryCow.name} (ID: ${primaryCow.id})")
                        } else {
                            val remoteCowId = fallbackCowId ?: 0L
                            println("Skipping activity ${data["activityType"]} - referenced cow not found locally (cowFirestoreId: $primaryCowFirestoreId, cowId: $remoteCowId)")
                        }
                    }
                } catch (e: Exception) { println("Error downloading activity $firestoreId: ${e.message}") }
            }
            println("Activity sync completed")
        } catch (e: Exception) { println("Error in syncUserActivities: ${e.message}"); throw e }
    }
    
    private suspend fun syncUserNotes(userId: String) {
        try {
            val localNotes = repository.getAllNotesSync()
            val remoteSnapshot = firestore.collection("users").document(userId).collection("notes").get().await()
            val remoteNotesMap = remoteSnapshot.documents.associate { it.id to (it.data ?: emptyMap<String, Any>()) }

            localNotes.forEach { note ->
                try {
                    val firestoreId = note.firestoreId ?: UUID.randomUUID().toString() // Use UUID for consistency
                    val remoteData = remoteNotesMap[firestoreId]
                    val noteData = note.toFirestoreMap(userId)
                    val localUpdatedAt = noteData["updatedAt"] as? Long ?: note.lastSyncAt

                    val shouldUpload = when {
                        note.firestoreId == null -> true
                        remoteData == null -> true
                        else -> {
                            val remoteUpdatedAtTs = remoteData["updatedAt"] as? Long ?: 0L
                            (localUpdatedAt > remoteUpdatedAtTs) || (note.isDeleted && !(remoteData["isDeleted"] as? Boolean ?: false))
                        }
                    }
                    if (shouldUpload) {
                        firestore.collection("users").document(userId).collection("notes").document(firestoreId).set(noteData).await()
                        repository.updateNote(note.copy(
                            firestoreId = firestoreId, 
                            lastSyncAt = localUpdatedAt, 
                            updatedBy = userId
                        ))
                        println("Uploaded note: ${note.title}")
                    }
                } catch (e: Exception) { println("Error uploading note ${note.title}: ${e.message}") }
            }

            val currentLocalNotes = repository.getAllNotesSync().associateBy { it.firestoreId }
            remoteNotesMap.forEach { (firestoreId, data) ->
                try {
                    val localNote = currentLocalNotes[firestoreId]
                    val remoteUpdatedAt = data["updatedAt"] as? Long ?: 0L
                    val isRemoteDeleted = data["isDeleted"] as? Boolean ?: false

                    val shouldProcess = when {
                        isRemoteDeleted -> {
                            localNote?.let { if(!it.isDeleted) repository.updateNote(it.copy(isDeleted = true, lastSyncAt = remoteUpdatedAt, updatedBy = data["updatedBy"] as? String)) }
                            false
                        }
                        localNote == null -> true
                        else -> remoteUpdatedAt > localNote.lastSyncAt && !localNote.isDeleted
                    }

                    if (shouldProcess && !isRemoteDeleted) {
                        val remoteNote = Note(
                            id = localNote?.id ?: 0L, // Keep local Long ID
                            title = data["title"] as? String ?: "",
                            text = data["text"] as? String ?: "",
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            isTodo = data["isTodo"] as? Boolean ?: false,
                            dueDate = (data["dueDate"] as? Number)?.toLong(),
                            isCompleted = data["isCompleted"] as? Boolean ?: false,
                            herdId = data["herdId"] as? String,
                            firestoreId = firestoreId,
                            lastSyncAt = remoteUpdatedAt,
                            isDeleted = false,
                            createdBy = data["createdBy"] as? String,
                            updatedBy = data["updatedBy"] as? String
                        )
                        if (localNote == null) repository.insertNote(remoteNote)
                        else repository.updateNote(remoteNote.copy(id = localNote.id))
                        println("Downloaded note: ${remoteNote.title}")
                    }
                } catch (e: Exception) { println("Error downloading note $firestoreId: ${e.message}") }
            }
            println("Note sync completed")
        } catch (e: Exception) { println("Error in syncUserNotes: ${e.message}"); throw e }
    }
    
    private suspend fun syncUserSettings(userId: String) {
        try {
            val localSettings = repository.getAllSettings().first()
            val remoteSnapshot = firestore.collection("users").document(userId).collection("settings").get().await()
            val remoteSettingsMap = remoteSnapshot.documents.associate { it.id to (it.data ?: emptyMap<String, Any>()) }

            localSettings.forEach { setting ->
                try {
                    val firestoreId = setting.firestoreId ?: setting.key
                    val remoteData = remoteSettingsMap[firestoreId]
                    val settingData = setting.toFirestoreMap(userId)
                    val localUpdatedAt = settingData["updatedAt"] as? Long ?: (setting.updatedAt ?: System.currentTimeMillis())
                    val hasUnsyncedChanges = (setting.lastSyncAt ?: 0L) < setting.updatedAt

                    val shouldUpload = when {
                        !hasUnsyncedChanges -> false
                        setting.firestoreId == null -> true
                        remoteData == null -> true
                        else -> {
                            val remoteUpdatedAt = remoteData["updatedAt"] as? Long ?: 0L
                            localUpdatedAt > remoteUpdatedAt
                        }
                    }
                    if (shouldUpload) {
                        firestore.collection("users").document(userId).collection("settings").document(firestoreId).set(settingData).await()
                        repository.insertOrUpdateSetting(setting.copy(
                            firestoreId = firestoreId,
                            lastSyncAt = localUpdatedAt,
                            updatedBy = userId
                        ))
                        println("Uploaded setting: ${setting.key}")
                    }
                } catch (e: Exception) { println("Error uploading setting ${setting.key}: ${e.message}") }
            }

            val currentLocalSettings = repository.getAllSettings().first()
            remoteSettingsMap.forEach { (firestoreId, data) ->
                try {
                    val localSetting = currentLocalSettings.find { it.firestoreId == firestoreId || it.key == firestoreId }
                    val remoteUpdatedAt = data["updatedAt"] as? Long ?: 0L

                    val shouldProcess = when {
                        localSetting == null -> true
                        else -> remoteUpdatedAt > (localSetting.lastSyncAt ?: 0L)
                    }

                    if (shouldProcess) {
                        val remoteSetting = Settings(
                            key = data["key"] as? String ?: firestoreId,
                            value = data["value"] as? String ?: "",
                            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                            updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                            firestoreId = firestoreId,
                            lastSyncAt = remoteUpdatedAt,
                            updatedBy = data["updatedBy"] as? String
                        )
                        repository.insertOrUpdateSetting(remoteSetting)
                        println("Downloaded setting: ${remoteSetting.key}")
                    }
                } catch (e: Exception) { println("Error downloading setting $firestoreId: ${e.message}") }
            }
            println("Settings sync completed")
        } catch (e: Exception) { println("Error in syncUserSettings: ${e.message}"); throw e }
    }
    
    private suspend fun syncUserTagColors(userId: String) {
        try {
            val localTagColors = repository.getAllTagColorsSync()
            val remoteSnapshot = firestore.collection("users").document(userId).collection("tagColors").get().await()
            val remoteTagColorsMap = remoteSnapshot.documents.associate { it.id to (it.data ?: emptyMap<String, Any>()) }

            localTagColors.forEach { tagColor ->
                try {
                    val firestoreId = tagColor.firestoreId ?: tagColor.id
                    val remoteData = remoteTagColorsMap[firestoreId]
                    val tagColorData = tagColor.toFirestoreMap(userId)
                    val localUpdatedAt = tagColorData["updatedAt"] as? Long ?: tagColor.updatedAt

                    val shouldUpload = when {
                        tagColor.firestoreId == null -> true
                        remoteData == null -> true
                        else -> {
                            val remoteUpdatedAt = remoteData["updatedAt"] as? Long ?: 0L
                            localUpdatedAt > remoteUpdatedAt
                        }
                    }
                    if (shouldUpload) {
                        if (tagColor.isDeleted) {
                            // If local is deleted, delete from server
                            firestore.collection("users").document(userId).collection("tagColors").document(firestoreId).delete().await()
                        } else {
                            // Otherwise upload the data
                            firestore.collection("users").document(userId).collection("tagColors").document(firestoreId).set(tagColorData).await()
                        }
                        repository.updateTagColor(tagColor.copy(
                            firestoreId = firestoreId,
                            lastSyncAt = localUpdatedAt,
                            updatedBy = userId
                        ))
                        println("Uploaded tag color: ${tagColor.name}${if (tagColor.isDeleted) " (deleted)" else ""}")
                    }
                } catch (e: Exception) { println("Error uploading tag color ${tagColor.name}: ${e.message}") }
            }

            remoteTagColorsMap.forEach { (firestoreId, data) ->
                try {
                    val localTagColor = localTagColors.find { it.firestoreId == firestoreId || it.id == firestoreId }
                    val remoteUpdatedAt = data["updatedAt"] as? Long ?: 0L

                    val shouldProcess = when {
                        localTagColor == null -> true
                        else -> remoteUpdatedAt > (localTagColor.lastSyncAt ?: 0L)
                    }

                    if (shouldProcess) {
                        val remoteTagColor = TagColor(
                            id = localTagColor?.id ?: firestoreId,
                            name = data["name"] as? String ?: "",
                            colorValue = (data["colorValue"] as? Number)?.toInt() ?: 0,
                            isActive = data["isActive"] as? Boolean ?: true,
                            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                            updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                            firestoreId = firestoreId,
                            lastSyncAt = remoteUpdatedAt,
                            updatedBy = data["updatedBy"] as? String,
                            isDeleted = data["isDeleted"] as? Boolean ?: false,
                            isDefault = data["isDefault"] as? Boolean ?: false
                        )
                        if (localTagColor == null) repository.insertTagColor(remoteTagColor)
                        else repository.updateTagColor(remoteTagColor)
                        println("Downloaded tag color: ${remoteTagColor.name}")
                    }
                } catch (e: Exception) { println("Error downloading tag color $firestoreId: ${e.message}") }
            }
            
            // Ensure defaults exist only if we have no tag colors at all (local or remote)
            val finalLocalTagColors = repository.getAllTagColorsSync()
            if (finalLocalTagColors.isEmpty()) {
                repository.ensureDefaultTagColorsExist()
                println("No tag colors found after sync, inserted defaults")
            }
            
            println("Tag colors sync completed")
        } catch (e: Exception) { println("Error in syncUserTagColors: ${e.message}"); throw e }
    }

    private suspend fun syncUserBreeds(userId: String) {
        try {
            val localBreeds = repository.getAllBreedsSync()
            val remoteSnapshot = firestore.collection("users").document(userId).collection("breeds").get().await()
            val remoteBreedsMap = remoteSnapshot.documents.associate { it.id to (it.data ?: emptyMap<String, Any>()) }

            localBreeds.forEach { breed ->
                try {
                    val firestoreId = breed.firestoreId ?: breed.id
                    val remoteData = remoteBreedsMap[firestoreId]
                    val breedData = breed.toFirestoreMap(userId)
                    val localUpdatedAt = breedData["updatedAt"] as? Long ?: breed.updatedAt

                    val shouldUpload = when {
                        breed.firestoreId == null -> true
                        remoteData == null -> true
                        else -> {
                            val remoteUpdatedAt = remoteData["updatedAt"] as? Long ?: 0L
                            (localUpdatedAt > remoteUpdatedAt) || (breed.isDeleted && !(remoteData["isDeleted"] as? Boolean ?: false))
                        }
                    }

                    if (shouldUpload) {
                        val docRef = firestore.collection("users").document(userId).collection("breeds").document(firestoreId)
                        if (breed.isDeleted) {
                            docRef.delete().await()
                        } else {
                            docRef.set(breedData).await()
                        }
                        repository.updateBreed(
                            breed.copy(
                                firestoreId = firestoreId,
                                lastSyncAt = localUpdatedAt,
                                updatedBy = userId
                            )
                        )
                        println("Uploaded breed: ${breed.name}${if (breed.isDeleted) " (deleted)" else ""}")
                    }
                } catch (e: Exception) {
                    println("Error uploading breed ${breed.name}: ${e.message}")
                }
            }

            val currentLocalBreeds = repository.getAllBreedsSync()
            remoteBreedsMap.forEach { (firestoreId, data) ->
                try {
                    val localBreed = currentLocalBreeds.find { it.firestoreId == firestoreId || it.id == firestoreId }
                    val remoteUpdatedAt = data["updatedAt"] as? Long ?: 0L
                    val isRemoteDeleted = data["isDeleted"] as? Boolean ?: false

                    val shouldProcess = when {
                        isRemoteDeleted -> {
                            localBreed?.let {
                                if (!it.isDeleted) {
                                    repository.updateBreed(
                                        it.copy(
                                            isActive = false,
                                            isDeleted = true,
                                            lastSyncAt = remoteUpdatedAt,
                                            updatedBy = data["updatedBy"] as? String
                                        )
                                    )
                                }
                            }
                            false
                        }
                        localBreed == null -> true
                        else -> remoteUpdatedAt > (localBreed.lastSyncAt ?: 0L) && !localBreed.isDeleted
                    }

                    if (shouldProcess && !isRemoteDeleted) {
                        val remoteBreed = Breed(
                            id = localBreed?.id ?: firestoreId,
                            name = data["name"] as? String ?: "",
                            isActive = data["isActive"] as? Boolean ?: true,
                            isDefault = data["isDefault"] as? Boolean ?: false,
                            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                            updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                            firestoreId = firestoreId,
                            lastSyncAt = remoteUpdatedAt,
                            updatedBy = data["updatedBy"] as? String,
                            isDeleted = false
                        )

                        if (localBreed == null) {
                            repository.insertBreed(remoteBreed)
                            println("Downloaded breed: ${remoteBreed.name}")
                        } else {
                            repository.updateBreed(remoteBreed.copy(id = localBreed.id))
                            println("Updated breed from remote: ${remoteBreed.name}")
                        }
                    }
                } catch (e: Exception) {
                    println("Error downloading breed $firestoreId: ${e.message}")
                }
            }

            println("Breed sync completed")
        } catch (e: Exception) {
            println("Error in syncUserBreeds: ${e.message}")
            throw e
        }
    }

    private suspend fun syncUserActivityTypes(userId: String) {
        try {
            val localActivityTypes = repository.getAllActivityTypesSync()
            val remoteSnapshot = firestore.collection("users").document(userId).collection("activityTypes").get().await()
            val remoteActivityTypesMap = remoteSnapshot.documents.associate { it.id to (it.data ?: emptyMap<String, Any>()) }

            localActivityTypes.forEach { activityType ->
                try {
                    val firestoreId = activityType.firestoreId ?: activityType.id
                    val remoteData = remoteActivityTypesMap[firestoreId]
                    val activityTypeData = activityType.toFirestoreMap(userId)
                    val localUpdatedAt = activityTypeData["updatedAt"] as? Long ?: activityType.updatedAt

                    val shouldUpload = when {
                        activityType.firestoreId == null -> true
                        remoteData == null -> true
                        else -> {
                            val remoteUpdatedAt = remoteData["updatedAt"] as? Long ?: 0L
                            localUpdatedAt > remoteUpdatedAt
                        }
                    }
                    if (shouldUpload) {
                        if (activityType.isDeleted) {
                            // If local is deleted, delete from server
                            firestore.collection("users").document(userId).collection("activityTypes").document(firestoreId).delete().await()
                        } else {
                            // Otherwise upload the data
                            firestore.collection("users").document(userId).collection("activityTypes").document(firestoreId).set(activityTypeData).await()
                        }
                        repository.updateActivityType(activityType.copy(
                            firestoreId = firestoreId,
                            lastSyncAt = localUpdatedAt,
                            updatedBy = userId
                        ))
                        println("Uploaded activity type: ${activityType.name}${if (activityType.isDeleted) " (deleted)" else ""}")
                    }
                } catch (e: Exception) { println("Error uploading activity type ${activityType.name}: ${e.message}") }
            }

            remoteActivityTypesMap.forEach { (firestoreId, data) ->
                try {
                    val localActivityType = localActivityTypes.find { it.firestoreId == firestoreId || it.id == firestoreId }
                    val remoteUpdatedAt = data["updatedAt"] as? Long ?: 0L

                    val shouldProcess = when {
                        localActivityType == null -> true
                        else -> remoteUpdatedAt > (localActivityType.lastSyncAt ?: 0L)
                    }

                    if (shouldProcess) {
                        val remoteActivityType = ActivityTypeConfig(
                            id = localActivityType?.id ?: firestoreId,
                            name = data["name"] as? String ?: "",
                            displayName = data["displayName"] as? String ?: "",
                            description = data["description"] as? String,
                            iconName = data["iconName"] as? String,
                            isActive = data["isActive"] as? Boolean ?: true,
                            isDefault = data["isDefault"] as? Boolean ?: false,
                            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                            updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                            firestoreId = firestoreId,
                            lastSyncAt = remoteUpdatedAt,
                            updatedBy = data["updatedBy"] as? String,
                            isDeleted = data["isDeleted"] as? Boolean ?: false
                        )
                        if (localActivityType == null) repository.insertActivityType(remoteActivityType)
                        else repository.updateActivityType(remoteActivityType)
                        println("Downloaded activity type: ${remoteActivityType.name}")
                    }
                } catch (e: Exception) { println("Error downloading activity type $firestoreId: ${e.message}") }
            }
            
            // Ensure defaults exist only if we have no activity types at all (local or remote)
            val finalLocalActivityTypes = repository.getAllActivityTypesSync()
            if (finalLocalActivityTypes.isEmpty()) {
                repository.ensureDefaultActivityTypesExist()
                println("No activity types found after sync, inserted defaults")
            }
            
            println("Activity types sync completed")
        } catch (e: Exception) { println("Error in syncUserActivityTypes: ${e.message}"); throw e }
    }
    
    private suspend fun handleRealtimeCowChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data 
            if (data == null) {
                println("Real-time cow change: data is null for doc ${doc.id}")
                return
            }
            val firestoreId = doc.id
            
            // Resolve mother and father references using firestoreIds
            val motherFirestoreId = data["motherFirestoreId"] as? String
            val fatherFirestoreId = data["fatherFirestoreId"] as? String
            val localCows = repository.getAllCowsSync()
            val localMotherCow = motherFirestoreId?.let { fsId -> localCows.find { it.firestoreId == fsId } }
            val localFatherCow = fatherFirestoreId?.let { fsId -> localCows.find { it.firestoreId == fsId } }
            
            val remoteCow = Cow(
                id = 0L,
                name = data["name"] as? String,
                tagNumber = data["tagNumber"] as? String,
                tagColor = data["tagColor"] as? String,
                birthDate = (data["birthDate"] as? String)?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { null } },
                gender = try { Gender.valueOf(data["gender"] as? String ?: Gender.TBD.name) } catch (e: Exception) { Gender.TBD },
                classification = try { Classification.valueOf(data["classification"] as? String ?: Classification.CALF.name) } catch (e: Exception) { Classification.CALF },
                colorMarkings = data["colorMarkings"] as? String,
                registrationNumber = data["registrationNumber"] as? String,
                breed = data["breed"] as? String,
                motherId = localMotherCow?.id ?: (data["motherId"] as? Number)?.toLong(), // Use local mother ID if found, fallback to stored ID
                fatherId = localFatherCow?.id ?: (data["fatherId"] as? Number)?.toLong(), // Use local father ID if found, fallback to stored ID
                status = try { Status.valueOf(data["status"] as? String ?: Status.ACTIVE.name) } catch (e: Exception) { Status.ACTIVE },
                pastureId = data["pastureId"] as? String,
                photos = (data["photos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                isWatched = data["isWatched"] as? Boolean ?: false,
                herdId = data["herdId"] as? String,
                createdAt = (data["createdDate"] as? String)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                updatedAt = (data["updatedDate"] as? String)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                firestoreId = firestoreId,
                lastSyncAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                createdBy = data["createdBy"] as? String,
                updatedBy = data["updatedBy"] as? String
            )

            println("Real-time ${change.type} for cow '${remoteCow.name}' (FS ID: $firestoreId), updatedBy: ${remoteCow.updatedBy}, remoteUpdatedAt: ${remoteCow.lastSyncAt}")

            when (change.type) {
                DocumentChange.Type.ADDED -> {
                    val existingLocalCow = repository.getAllCowsSync().find { it.firestoreId == firestoreId }
                    if (existingLocalCow == null) {
                        if (!remoteCow.isDeleted) {
                            println("Real-time ADDED (local copy missing): Inserting cow '${remoteCow.name}'. FS ID: $firestoreId")
                            repository.insertCow(remoteCow.copy(id = 0L)) 
                        } else {
                            println("Real-time ADDED (local copy missing, but remote is deleted): Ignoring cow '${remoteCow.name}'. FS ID: $firestoreId")
                        }
                    } else {
                        println("Real-time ADDED (local copy exists): Cow '${remoteCow.name}'. FS ID: $firestoreId. Checking for update.")
                        if (existingLocalCow.lastSyncAt < remoteCow.lastSyncAt || remoteCow.isDeleted != existingLocalCow.isDeleted) {
                            repository.updateCow(remoteCow.copy(id = existingLocalCow.id))
                            println("Real-time ADDED (local copy exists): Updated local cow '${remoteCow.name}'.")
                        } else {
                            println("Real-time ADDED (local copy exists): Local cow '${remoteCow.name}' is same or newer. No update needed.")
                        }
                    }
                }
                DocumentChange.Type.MODIFIED -> {
                    val existingLocalCow = repository.getAllCowsSync().find { it.firestoreId == firestoreId }
                    if (existingLocalCow == null) {
                        if (!remoteCow.isDeleted) {
                            println("Real-time MODIFIED: Cow '${remoteCow.name}' (FS ID: $firestoreId) not found locally. Inserting.")
                            repository.insertCow(remoteCow.copy(id = 0L))
                        } else {
                             println("Real-time MODIFIED: Deleted cow '${remoteCow.name}' (FS ID: $firestoreId) not found locally. Ignoring.")
                        }
                    } else {
                        if (existingLocalCow.lastSyncAt < remoteCow.lastSyncAt || remoteCow.isDeleted != existingLocalCow.isDeleted) {
                            println("Real-time MODIFIED: Updating cow '${remoteCow.name}'.")
                            repository.updateCow(remoteCow.copy(id = existingLocalCow.id))
                        } else {
                            println("Real-time MODIFIED: Local cow '${remoteCow.name}' is same or newer. No update.")
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    val existingLocalCow = repository.getAllCowsSync().find { it.firestoreId == firestoreId }
                    println("Real-time REMOVED for cow '${remoteCow.name}' (FS ID: $firestoreId)")
                    existingLocalCow?.let {
                        if (!it.isDeleted) {
                            repository.updateCow(it.copy(isDeleted = true, lastSyncAt = System.currentTimeMillis(), updatedBy = remoteCow.updatedBy ?: userId))
                            println("Real-time REMOVED: Marked local cow '${it.name}' as deleted.")
                        } else {
                             println("Real-time REMOVED: Local cow '${it.name}' already marked as deleted.")
                        }
                    } ?: println("Real-time REMOVED: Cow with FS ID $firestoreId not found locally to mark as deleted.")
                }
            }
        } catch (e: Exception) {
            println("Error in handleRealtimeCowChange for doc ${change.document.id}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private suspend fun handleRealtimePastureChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data 
            if (data == null) {
                println("Real-time pasture change: data is null for doc ${doc.id}")
                return
            }
            val firestoreId = doc.id
           
            val localPastures = repository.getAllPasturesSync()
            val existingLocalPasture = localPastures.find { it.firestoreId == firestoreId || it.id == firestoreId }
            val resolvedLocalId = existingLocalPasture?.id?.takeUnless { it.isBlank() } ?: firestoreId

            val remotePasture = Pasture(
                id = resolvedLocalId,
                name = data["name"] as? String ?: "Unknown Pasture",
                description = data["description"] as? String,
                sizeAcres = (data["sizeAcres"] as? Number)?.toDouble(),
                herdId = data["herdId"] as? String,
                firestoreId = firestoreId,
                lastSyncAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                createdBy = data["createdBy"] as? String,
                updatedBy = data["updatedBy"] as? String
            )
            
            println("Real-time ${change.type} for pasture '${remotePasture.name}' (FS ID: $firestoreId), updatedBy: ${remotePasture.updatedBy}, remoteUpdatedAt: ${remotePasture.lastSyncAt}")

            when (change.type) {
                DocumentChange.Type.ADDED -> {
                    if (existingLocalPasture == null) {
                        if (!remotePasture.isDeleted) {
                            println("Real-time ADDED (local copy missing): Inserting pasture '${remotePasture.name}'. FS ID: $firestoreId")
                            repository.insertPasture(remotePasture) // id is already firestoreId
                        } else {
                            println("Real-time ADDED (local copy missing, but remote is deleted): Ignoring pasture '${remotePasture.name}'. FS ID: $firestoreId")
                        }
                    } else {
                        println("Real-time ADDED (local copy exists): Pasture '${remotePasture.name}'. FS ID: $firestoreId. Checking for update.")
                        if (existingLocalPasture.lastSyncAt < remotePasture.lastSyncAt || remotePasture.isDeleted != existingLocalPasture.isDeleted) {
                            repository.updatePasture(remotePasture)
                            println("Real-time ADDED (local copy exists): Updated local pasture '${remotePasture.name}'.")
                        } else {
                            println("Real-time ADDED (local copy exists): Local pasture '${remotePasture.name}' is same or newer. No update needed.")
                        }
                    }
                }
                DocumentChange.Type.MODIFIED -> {
                    if (existingLocalPasture == null) {
                        if (!remotePasture.isDeleted) {
                             println("Real-time MODIFIED: Pasture '${remotePasture.name}' not local. Inserting.")
                             repository.insertPasture(remotePasture) // id is already firestoreId
                        }
                    } else {
                        if (existingLocalPasture.lastSyncAt < remotePasture.lastSyncAt || remotePasture.isDeleted != existingLocalPasture.isDeleted) {
                            println("Real-time MODIFIED: Updating pasture '${remotePasture.name}'.")
                            repository.updatePasture(remotePasture)
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                     existingLocalPasture?.let {
                        if (!it.isDeleted) {
                            // Use a new timestamp for the deletion, and preserve original createdBy unless remote gives one for deletion
                            val deletionTimestamp = System.currentTimeMillis()
                            val deletedBy = remotePasture.updatedBy ?: userId // Prefer remote updater if available
                            repository.updatePasture(it.copy(isDeleted = true, lastSyncAt = deletionTimestamp, updatedBy = deletedBy))
                             println("Real-time REMOVED: Marked pasture '${it.name}' as deleted.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time pasture change for doc ${change.document.id}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private suspend fun handleRealtimeActivityChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data 
            if (data == null) {
                println("Real-time activity change: data is null for doc ${doc.id}")
                return
            }
            val firestoreId = doc.id
            
            val cowFirestoreId = data["cowFirestoreId"] as? String
            val localCows = repository.getAllCowsSync()
            val remoteCowFirestoreIds = (data["cowFirestoreIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val resolvedCowIds = LinkedHashSet<Long>()
            remoteCowFirestoreIds.forEach { fsId ->
                localCows.find { it.firestoreId == fsId }?.id?.let { resolvedCowIds += it }
            }

            if (resolvedCowIds.isEmpty()) {
                val remoteCowIdList = (data["cowIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()
                remoteCowIdList.forEach { resolvedCowIds += it }
            }

            val fallbackCowId = (data["cowId"] as? Number)?.toLong()
            val primaryCowId = resolvedCowIds.firstOrNull() ?: fallbackCowId
            val primaryCow = when {
                primaryCowId != null -> localCows.find { it.id == primaryCowId } ?: repository.getCowById(primaryCowId)
                cowFirestoreId != null -> localCows.find { it.firestoreId == cowFirestoreId }
                else -> null
            }

            if (primaryCow == null) {
                val remoteCowId = fallbackCowId ?: 0L
                println("Real-time ${change.type} for activity '${data["activityType"]}' (FS ID: $firestoreId): Skipping - referenced cow not found locally (cowFirestoreId: $cowFirestoreId, cowId: $remoteCowId). Will sync during full sync.")
                return
            }

            val cowIdsForActivity = if (resolvedCowIds.isNotEmpty()) resolvedCowIds.toList() else listOf(primaryCow.id)

            val remoteActivity = Activity(
                id = 0L,
                cowId = primaryCow.id, // Use local cow ID
                date = (data["date"] as? String)?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { java.time.LocalDate.now() } } ?: java.time.LocalDate.now(),
                activityType = try { ActivityType.valueOf(data["activityType"] as? String ?: ActivityType.OTHER.name) } catch (e: Exception) { ActivityType.OTHER },
                notes = data["notes"] as? String,
                fromPastureId = data["fromPastureId"] as? String,
                toPastureId = data["toPastureId"] as? String,
                details = data["details"] as? String,
                groupId = data["groupId"] as? String,
                result = data["result"] as? String,
                quantity = (data["quantity"] as? Number)?.toDouble(),
                technician = data["technician"] as? String,
                cost = (data["cost"] as? Number)?.toDouble(),
                herdId = data["herdId"] as? String,
                firestoreId = firestoreId,
                lastSyncAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                createdBy = data["createdBy"] as? String,
                updatedBy = data["updatedBy"] as? String,
                cowIds = cowIdsForActivity
            )

            println("Real-time ${change.type} for activity '${remoteActivity.activityType}' (FS ID: $firestoreId), updatedBy: ${remoteActivity.updatedBy}, remoteUpdatedAt: ${remoteActivity.lastSyncAt}")

            when (change.type) {
                DocumentChange.Type.ADDED -> {
                    val existingLocalActivity = repository.getAllActivitiesSync().find { it.firestoreId == firestoreId }
                    if (existingLocalActivity == null) {
                        if (!remoteActivity.isDeleted) {
                            println("Real-time ADDED (local copy missing): Inserting activity '${remoteActivity.activityType}' for cow ${primaryCow.name}.")
                            repository.insertActivity(remoteActivity.copy(id = 0L))
                        }
                    } else {
                        if (existingLocalActivity.lastSyncAt < remoteActivity.lastSyncAt || remoteActivity.isDeleted != existingLocalActivity.isDeleted) {
                            println("Real-time ADDED (conflict): Updating activity '${remoteActivity.activityType}' for cow ${primaryCow.name}.")
                            repository.updateActivity(remoteActivity.copy(id = existingLocalActivity.id))
                        } else {
                            println("Real-time ADDED (already up-to-date): Skipping activity '${remoteActivity.activityType}' for cow ${primaryCow.name}.")
                        }
                    }
                }
                DocumentChange.Type.MODIFIED -> {
                    val existingLocalActivity = repository.getAllActivitiesSync().find { it.firestoreId == firestoreId }
                    if (existingLocalActivity == null) {
                        if (!remoteActivity.isDeleted) {
                            println("Real-time MODIFIED: Activity '${remoteActivity.activityType}' not local. Inserting for cow ${primaryCow.name}.")
                            repository.insertActivity(remoteActivity.copy(id = 0L))
                        }
                    } else {
                        if (existingLocalActivity.lastSyncAt < remoteActivity.lastSyncAt || remoteActivity.isDeleted != existingLocalActivity.isDeleted) {
                            println("Real-time MODIFIED: Updating activity '${remoteActivity.activityType}' for cow ${primaryCow.name}.")
                            repository.updateActivity(remoteActivity.copy(id = existingLocalActivity.id))
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    val existingLocalActivity = repository.getAllActivitiesSync().find { it.firestoreId == firestoreId }
                    existingLocalActivity?.let {
                        if(!it.isDeleted) {
                            repository.updateActivity(it.copy(isDeleted = true, lastSyncAt = System.currentTimeMillis(), updatedBy = remoteActivity.updatedBy ?: userId))
                             println("Real-time REMOVED: Marked activity '${it.activityType}' as deleted.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time activity change for doc ${change.document.id}: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun handleRealtimeNoteChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data 
            if (data == null) {
                println("Real-time note change: data is null for doc ${doc.id}")
                return
            }
            val firestoreId = doc.id
            
            val remoteNote = Note(
                id = 0L,
                title = data["title"] as? String ?: "Unknown Note",
                text = data["text"] as? String ?: "",
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isTodo = data["isTodo"] as? Boolean ?: false,
                dueDate = (data["dueDate"] as? Number)?.toLong(),
                isCompleted = data["isCompleted"] as? Boolean ?: false,
                herdId = data["herdId"] as? String,
                firestoreId = firestoreId,
                lastSyncAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                createdBy = data["createdBy"] as? String,
                updatedBy = data["updatedBy"] as? String
            )
            
            println("Real-time ${change.type} for note '${remoteNote.title}' (FS ID: $firestoreId), updatedBy: ${remoteNote.updatedBy}, remoteUpdatedAt: ${remoteNote.lastSyncAt}")

            when (change.type) {
                DocumentChange.Type.ADDED -> {
                    val existingLocalNote = repository.getAllNotesSync().find { it.firestoreId == firestoreId }
                    if (existingLocalNote == null) {
                        if (!remoteNote.isDeleted) {
                            println("Real-time ADDED (local copy missing): Inserting note '${remoteNote.title}'.")
                            repository.insertNote(remoteNote.copy(id = 0L))
                        }
                    } else {
                        if (existingLocalNote.lastSyncAt < remoteNote.lastSyncAt || remoteNote.isDeleted != existingLocalNote.isDeleted) {
                            println("Real-time ADDED (conflict): Updating note '${remoteNote.title}'.")
                            repository.updateNote(remoteNote.copy(id = existingLocalNote.id))
                        }
                    }
                }
                DocumentChange.Type.MODIFIED -> {
                    val existingLocalNote = repository.getAllNotesSync().find { it.firestoreId == firestoreId }
                    if (existingLocalNote == null) {
                        if (!remoteNote.isDeleted) {
                            println("Real-time MODIFIED: Note '${remoteNote.title}' not local. Inserting.")
                            repository.insertNote(remoteNote.copy(id = 0L))
                        }
                    } else {
                        if (existingLocalNote.lastSyncAt < remoteNote.lastSyncAt || remoteNote.isDeleted != existingLocalNote.isDeleted) {
                            println("Real-time MODIFIED: Updating note '${remoteNote.title}'.")
                            repository.updateNote(remoteNote.copy(id = existingLocalNote.id))
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    val existingLocalNote = repository.getAllNotesSync().find { it.firestoreId == firestoreId }
                    existingLocalNote?.let {
                        if (!it.isDeleted) {
                            repository.updateNote(it.copy(isDeleted = true, lastSyncAt = System.currentTimeMillis(), updatedBy = remoteNote.updatedBy ?: userId))
                            println("Real-time REMOVED: Marked note '${it.title}' as deleted.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time note change for doc ${change.document.id}: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun handleRealtimeSettingsChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data 
            if (data == null) {
                println("Real-time settings change: data is null for doc ${doc.id}")
                return
            }
            val firestoreId = doc.id
            
            val remoteSetting = Settings(
                key = data["key"] as? String ?: firestoreId,
                value = data["value"] as? String ?: "",
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                firestoreId = firestoreId,
                lastSyncAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                updatedBy = data["updatedBy"] as? String
            )
            
            println("Real-time ${change.type} for setting '${remoteSetting.key}' (FS ID: $firestoreId)")

            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val existingLocalSetting = repository.getAllSettings().first().find { it.firestoreId == firestoreId || it.key == firestoreId }
                    if (existingLocalSetting == null || (existingLocalSetting.lastSyncAt ?: 0L) < (remoteSetting.lastSyncAt ?: 0L)) {
                        repository.insertOrUpdateSetting(remoteSetting)
                        println("Real-time ${change.type}: Updated setting '${remoteSetting.key}'")
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    // Settings are typically not deleted, but we could handle it if needed
                    println("Real-time REMOVED: Setting '${remoteSetting.key}' removed from server")
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time settings change for doc ${change.document.id}: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun handleRealtimeTagColorChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data
            if (data == null) {
                println("Real-time tag color change: data is null for doc ${doc.id}")
                return
            }
            val firestoreId = doc.id

            val remoteTagColor = TagColor(
                id = firestoreId,
                name = data["name"] as? String ?: "",
                colorValue = (data["colorValue"] as? Number)?.toInt() ?: 0,
                isActive = data["isActive"] as? Boolean ?: true,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                firestoreId = firestoreId,
                lastSyncAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                updatedBy = data["updatedBy"] as? String,
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                isDefault = data["isDefault"] as? Boolean ?: false
            )

            println("Real-time ${change.type} for tag color '${remoteTagColor.name}' (FS ID: $firestoreId)")

            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val existingLocalTagColor = repository.getAllTagColorsSync().find { it.firestoreId == firestoreId || it.id == firestoreId }
                    
                    if (remoteTagColor.isDeleted) {
                        // Handle soft deletion from remote
                        existingLocalTagColor?.let {
                            repository.deleteTagColor(it)
                            println("Real-time ${change.type}: Deleted tag color '${it.name}' (marked as deleted remotely)")
                        }
                    } else {
                        // Handle normal add/update
                        if (existingLocalTagColor == null) {
                            repository.insertTagColor(remoteTagColor)
                            println("Real-time ${change.type}: Inserted tag color '${remoteTagColor.name}'")
                        } else if ((existingLocalTagColor.lastSyncAt ?: 0L) < (remoteTagColor.lastSyncAt ?: 0L)) {
                            repository.updateTagColor(remoteTagColor.copy(id = existingLocalTagColor.id))
                            println("Real-time ${change.type}: Updated tag color '${remoteTagColor.name}'")
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    val existingLocalTagColor = repository.getAllTagColorsSync().find { it.firestoreId == firestoreId }
                    existingLocalTagColor?.let {
                        repository.deleteTagColor(it)
                        println("Real-time REMOVED: Deleted tag color '${it.name}'")
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time tag color change for doc ${change.document.id}: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun handleRealtimeBreedChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data
            if (data == null) {
                println("Real-time breed change: data is null for doc ${doc.id}")
                return
            }
            val firestoreId = doc.id

            val remoteBreed = Breed(
                id = firestoreId,
                name = data["name"] as? String ?: "",
                isActive = data["isActive"] as? Boolean ?: true,
                isDefault = data["isDefault"] as? Boolean ?: false,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                firestoreId = firestoreId,
                lastSyncAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                updatedBy = data["updatedBy"] as? String,
                isDeleted = data["isDeleted"] as? Boolean ?: false
            )

            println("Real-time ${change.type} for breed '${remoteBreed.name}' (FS ID: $firestoreId)")

            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val existingLocalBreed = repository.getAllBreedsSync().find { it.firestoreId == firestoreId || it.id == firestoreId }

                    if (remoteBreed.isDeleted) {
                        existingLocalBreed?.let {
                            repository.updateBreed(
                                it.copy(
                                    isActive = false,
                                    isDeleted = true,
                                    lastSyncAt = remoteBreed.lastSyncAt,
                                    updatedBy = remoteBreed.updatedBy
                                )
                            )
                            println("Real-time ${change.type}: Deleted breed '${it.name}' (marked as deleted remotely)")
                        }
                    } else {
                        if (existingLocalBreed == null) {
                            repository.insertBreed(remoteBreed)
                            println("Real-time ${change.type}: Inserted breed '${remoteBreed.name}'")
                        } else if ((existingLocalBreed.lastSyncAt ?: 0L) < (remoteBreed.lastSyncAt ?: 0L)) {
                            repository.updateBreed(remoteBreed.copy(id = existingLocalBreed.id))
                            println("Real-time ${change.type}: Updated breed '${remoteBreed.name}'")
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    val existingLocalBreed = repository.getAllBreedsSync().find { it.firestoreId == firestoreId || it.id == firestoreId }
                    existingLocalBreed?.let {
                        repository.updateBreed(
                            it.copy(
                                isActive = false,
                                isDeleted = true,
                                lastSyncAt = System.currentTimeMillis(),
                                updatedBy = remoteBreed.updatedBy ?: userId
                            )
                        )
                        println("Real-time REMOVED: Marked breed '${it.name}' as deleted.")
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time breed change for doc ${change.document.id}: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun handleRealtimeActivityTypeChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data
            if (data == null) {
                println("Real-time activity type change: data is null for doc ${doc.id}")
                return
            }
            val firestoreId = doc.id
            
            val remoteActivityType = ActivityTypeConfig(
                id = firestoreId,
                name = data["name"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "",
                description = data["description"] as? String,
                iconName = data["iconName"] as? String,
                isActive = data["isActive"] as? Boolean ?: true,
                isDefault = data["isDefault"] as? Boolean ?: false,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                firestoreId = firestoreId,
                lastSyncAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                updatedBy = data["updatedBy"] as? String,
                isDeleted = data["isDeleted"] as? Boolean ?: false
            )
            
            println("Real-time ${change.type} for activity type '${remoteActivityType.name}' (FS ID: $firestoreId)")

            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val existingLocalActivityType = repository.getAllActivityTypesSync().find { it.firestoreId == firestoreId || it.id == firestoreId }
                    
                    if (remoteActivityType.isDeleted) {
                        // Handle soft deletion from remote
                        existingLocalActivityType?.let {
                            if (!it.isDefault) { // Don't delete default activity types
                                repository.deleteActivityType(it)
                                println("Real-time ${change.type}: Deleted activity type '${it.name}' (marked as deleted remotely)")
                            } else {
                                println("Real-time ${change.type}: Skipped deletion of default activity type '${it.name}'")
                            }
                        }
                    } else {
                        // Handle normal add/update
                        if (existingLocalActivityType == null) {
                            repository.insertActivityType(remoteActivityType)
                            println("Real-time ${change.type}: Inserted activity type '${remoteActivityType.name}'")
                        } else if ((existingLocalActivityType.lastSyncAt ?: 0L) < (remoteActivityType.lastSyncAt ?: 0L)) {
                            repository.updateActivityType(remoteActivityType.copy(id = existingLocalActivityType.id))
                            println("Real-time ${change.type}: Updated activity type '${remoteActivityType.name}'")
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    val existingLocalActivityType = repository.getAllActivityTypesSync().find { it.firestoreId == firestoreId }
                    existingLocalActivityType?.let {
                        if (!it.isDefault) { // Don't delete default activity types
                            repository.deleteActivityType(it)
                            println("Real-time REMOVED: Deleted activity type '${it.name}'")
                        } else {
                            println("Real-time REMOVED: Skipped deletion of default activity type '${it.name}'")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time activity type change for doc ${change.document.id}: ${e.message}")
            e.printStackTrace()
        }
    }

}

enum class SyncStatus {
    IDLE, SYNCING, SUCCESS, ERROR
}

enum class ItemSyncStatus {
    IDLE, SYNCING, SUCCESS, ERROR
}

private suspend fun Cow.toFirestoreMap(userId: String, repository: CattleRepository): Map<String, Any?> {
    // Get mother and father firestoreIds for proper cross-device referencing
    val motherCow = motherId?.let { repository.getCowById(it) }
    val fatherCow = fatherId?.let { repository.getCowById(it) }
    
    return mapOf(
        "name" to name,
        "tagNumber" to tagNumber,
        "tagColor" to tagColor,
        "birthDate" to birthDate?.toString(),
        "gender" to gender.name,
        "classification" to classification.name,
        "colorMarkings" to colorMarkings,
        "registrationNumber" to registrationNumber,
        "breed" to breed,
        "createdDate" to createdAt?.toString(),
        "updatedDate" to updatedAt?.toString(),
        "motherId" to motherId, // Keep for backward compatibility
        "motherFirestoreId" to motherCow?.firestoreId, // Use this for cross-device mother referencing
        "fatherId" to fatherId, // Keep for backward compatibility
        "fatherFirestoreId" to fatherCow?.firestoreId, // Use this for cross-device father referencing
        "status" to status.name,
        "pastureId" to pastureId, // This is already a String (firestoreId)
        "photos" to photos,
        "isWatched" to isWatched,
        "herdId" to herdId,
        "isDeleted" to isDeleted,
        "createdBy" to (createdBy ?: userId),
        "updatedBy" to userId,
        "updatedAt" to System.currentTimeMillis()
    )
}

private suspend fun Activity.toFirestoreMap(userId: String, repository: CattleRepository): Map<String, Any?> {
    // Get the cow's firestoreId for proper cross-device referencing
    val cow = repository.getCowById(cowId)
    val cowFirestoreId = cow?.firestoreId
    val associatedCowFirestoreIds = cowIds.mapNotNull { repository.getCowById(it)?.firestoreId }

    return mapOf(
        "cowId" to cowId, // Keep for backward compatibility
        "cowFirestoreId" to cowFirestoreId, // Use this for cross-device cow referencing
        "date" to date.toString(),
        "activityType" to activityType.name,
        "notes" to notes,
        "fromPastureId" to fromPastureId,
        "toPastureId" to toPastureId,
        "details" to details,
        "groupId" to groupId,
        "result" to result,
        "quantity" to quantity,
        "technician" to technician,
        "cost" to cost,
        "cowIds" to cowIds,
        "cowFirestoreIds" to associatedCowFirestoreIds,
        "herdId" to herdId,
        "isDeleted" to isDeleted,
        "createdBy" to (createdBy ?: userId),
        "updatedBy" to userId,
        "updatedAt" to System.currentTimeMillis()
    )
}

private fun Pasture.toFirestoreMap(userId: String): Map<String, Any?> {
    return mapOf(
        "name" to name,
        "description" to description,
        "sizeAcres" to sizeAcres,
        "herdId" to herdId,
        "isDeleted" to isDeleted,
        "createdBy" to (createdBy ?: userId),
        "updatedBy" to userId,
        "updatedAt" to System.currentTimeMillis(),
        // Ensure firestoreId is included if it's part of the model and needed for queries, though it's usually the doc ID
        // "firestoreId" to firestoreId 
    )
}

private fun Note.toFirestoreMap(userId: String): Map<String, Any?> {
    return mapOf(
        "title" to title,
        "text" to text,
        "timestamp" to timestamp,
        "isTodo" to isTodo,
        "dueDate" to dueDate,
        "isCompleted" to isCompleted,
        "herdId" to herdId,
        "isDeleted" to isDeleted,
        "createdBy" to (createdBy ?: userId),
        "updatedBy" to userId,
        "updatedAt" to System.currentTimeMillis()
    )
}

private fun Settings.toFirestoreMap(userId: String): Map<String, Any?> {
    return mapOf(
        "key" to key,
        "value" to value,
        "createdBy" to userId,
        "updatedBy" to userId,
        "createdAt" to (createdAt ?: System.currentTimeMillis()),
        "updatedAt" to System.currentTimeMillis()
    )
}

private fun TagColor.toFirestoreMap(userId: String): Map<String, Any?> {
    val effectiveUpdatedAt = if (updatedAt > 0L) updatedAt else System.currentTimeMillis()
    return mapOf(
        "name" to name,
        "colorValue" to colorValue,
        "isActive" to isActive,
        "isDefault" to isDefault,
        "createdAt" to createdAt,
        "updatedBy" to userId,
        "updatedAt" to effectiveUpdatedAt,
        "isDeleted" to isDeleted
    )
}

private fun ActivityTypeConfig.toFirestoreMap(userId: String): Map<String, Any?> {
    val effectiveUpdatedAt = if (updatedAt > 0L) updatedAt else System.currentTimeMillis()
    return mapOf(
        "name" to name,
        "displayName" to displayName,
        "description" to description,
        "iconName" to iconName,
        "isActive" to isActive,
        "isDefault" to isDefault,
        "createdAt" to createdAt,
        "updatedBy" to userId,
        "updatedAt" to effectiveUpdatedAt,
        "isDeleted" to isDeleted
    )
}
