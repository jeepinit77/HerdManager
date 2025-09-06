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
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.delay 

class SyncService(
    private val repository: CattleRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _itemSyncStatus = MutableStateFlow(ItemSyncStatus.IDLE)
    val itemSyncStatus: Flow<ItemSyncStatus> = _itemSyncStatus.asStateFlow()
    
    private var activeListeners = mutableMapOf<String, ListenerRegistration>()

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

    suspend fun clearServerData(userId: String) {
        println("Clearing all server data for user ID: $userId")
        try {
            val collectionsToDelete = listOf("cows", "pastures", "activities", "notes")
            for (collectionName in collectionsToDelete) {
                val collectionRef = firestore.collection("users").document(userId).collection(collectionName)
                val snapshot = collectionRef.get().await() 
                if (snapshot.isEmpty) {
                    println("No documents found in '$collectionName' for user $userId to delete.")
                    continue
                }
                val batch = firestore.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }
                batch.commit().await()
                println("Successfully deleted all documents from '$collectionName' for user $userId.")
            }
            println("Successfully cleared all specified server data for user $userId.")
        } catch (e: Exception) {
            println("Error clearing server data for user $userId: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun forceUploadAllData(userId: String) {
        println("Starting force upload of all local data for user ID: $userId")
        _syncStatus.value = SyncStatus.SYNCING 
        try {
            val localCows = repository.getAllCowsSync()
            println("Found ${localCows.size} local cows to force upload.")
            for (cow in localCows) {
                val firestoreId = cow.firestoreId ?: UUID.randomUUID().toString()
                val cowData = cow.toFirestoreMap(userId) 
                val updatedTimestamp = cowData["updatedAt"] as? Long ?: System.currentTimeMillis()
                firestore.collection("users").document(userId).collection("cows").document(firestoreId).set(cowData).await()
                repository.updateCow(cow.copy(
                    firestoreId = firestoreId,
                    lastSyncAt = updatedTimestamp,
                    updatedBy = userId
                ))
                println("Force uploaded cow: ${cow.name} (FS ID: $firestoreId)")
            }

            val localPastures = repository.getAllPasturesSync()
            println("Found ${localPastures.size} local pastures to force upload.")
            for (pasture in localPastures) {
                val firestoreId = pasture.firestoreId ?: UUID.randomUUID().toString() // Ensure ID consistency here
                val pastureData = pasture.toFirestoreMap(userId)
                val updatedTimestamp = pastureData["updatedAt"] as? Long ?: System.currentTimeMillis()
                firestore.collection("users").document(userId).collection("pastures").document(firestoreId).set(pastureData).await()
                // Assuming pasture.id IS already firestoreId if Step 2 of plan is done
                repository.updatePasture(pasture.copy(
                    firestoreId = firestoreId, // This should be redundant if pasture.id = firestoreId
                    id = firestoreId, // Ensure local PK is also the firestoreId
                    lastSyncAt = updatedTimestamp,
                    updatedBy = userId
                ))
                println("Force uploaded pasture: ${pasture.name} (FS ID: $firestoreId)")
            }

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

            println("Force upload of all local data completed for user ID: $userId")
            _syncStatus.value = SyncStatus.SUCCESS
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
        
        println("Real-time sync listeners established for user: $userId for all collections")
    }
    
    fun stopRealtimeSync(userId: String) {
        listOf("cows", "pastures", "activities", "notes").forEach { collection ->
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
                is Cow -> {
                    val firestoreId = item.firestoreId ?: UUID.randomUUID().toString()
                    val cowData = item.toFirestoreMap(userId)
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
                    val firestoreId = item.firestoreId ?: UUID.randomUUID().toString() 
                    val pastureData = item.toFirestoreMap(userId)
                    val updatedTimestamp = pastureData.get("updatedAt") as? Long ?: System.currentTimeMillis()
                    
                    // Update local item with firestoreId BEFORE writing to Firestore
                    repository.updatePasture(item.copy(
                        firestoreId = firestoreId,
                        id = firestoreId, // Ensure local PK is also the firestoreId
                        lastSyncAt = updatedTimestamp,
                        updatedBy = userId
                    ))
                    
                    firestore.collection("users").document(userId).collection("pastures").document(firestoreId).set(pastureData).await()
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
                    val firestoreId = pasture.firestoreId ?: pasture.id // Prioritize firestoreId field, then local id
                    val remoteData = remotePasturesMap[firestoreId]
                    val pastureData = pasture.toFirestoreMap(userId) // This sets/updates createdBy, updatedBy, updatedAt
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
                        firestore.collection("users").document(userId).collection("pastures").document(firestoreId).set(pastureData).await()
                        repository.updatePasture(pasture.copy(
                            firestoreId = firestoreId, // Ensure this is set
                            id = firestoreId, // Ensure PK is aligned
                            lastSyncAt = localUpdatedAt, 
                            updatedBy = userId
                        ))
                        println("Uploaded pasture: ${pasture.name} (FS ID: $firestoreId)")
                    }
                } catch (e: Exception) { println("Error uploading pasture ${pasture.name}: ${e.message}") }
            }

            // Download remote changes to local
            val currentLocalPastures = repository.getAllPasturesSync().associateBy { it.id } // Use primary key `id`
            remotePasturesMap.forEach { (firestoreId, data) ->
                try {
                    val localPasture = currentLocalPastures[firestoreId] // Try to find by firestoreId (which should be local PK)
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
                        val remotePasture = Pasture(
                            id = firestoreId, // Use firestoreId as the local primary key
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
                    val cowData = cow.toFirestoreMap(userId)
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
                        val remoteCow = Cow(
                            id = localCow?.id ?: 0L, // Keep local Long ID if exists, else 0 for new insert
                            name = data["name"] as? String,
                            tagNumber = data["tagNumber"] as? String,
                            tagColor = data["tagColor"] as? String,
                            birthDate = (data["birthDate"] as? String)?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { null } },
                            gender = try { Gender.valueOf(data["gender"] as? String ?: Gender.TBD.name) } catch (e: Exception) { Gender.TBD },
                            classification = try { Classification.valueOf(data["classification"] as? String ?: Classification.CALF.name) } catch (e: Exception) { Classification.CALF },
                            colorMarkings = data["colorMarkings"] as? String,
                            motherId = (data["motherId"] as? Number)?.toLong(),
                            fatherId = (data["fatherId"] as? Number)?.toLong(),
                            status = try { Status.valueOf(data["status"] as? String ?: Status.ACTIVE.name) } catch (e: Exception) { Status.ACTIVE },
                            pastureId = data["pastureId"] as? String,
                            photos = (data["photos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            isWatched = data["isWatched"] as? Boolean ?: false,
                            herdId = data["herdId"] as? String,
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
                        // Try to resolve cow reference using cowFirestoreId first, then fallback to cowId
                        val cowFirestoreId = data["cowFirestoreId"] as? String
                        val localCow = if (cowFirestoreId != null) {
                            firestoreIdToLocalCow[cowFirestoreId]
                        } else {
                            // Fallback: try to find by the stored cowId (might work if IDs happen to match)
                            val remoteCowId = (data["cowId"] as? Number)?.toLong() ?: 0L
                            repository.getCowById(remoteCowId)
                        }

                        if (localCow != null) {
                            val remoteActivity = Activity(
                                id = localActivity?.id ?: 0L, // Keep local Long ID
                                cowId = localCow.id, // Use local cow ID
                                date = (data["date"] as? String)?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { java.time.LocalDate.now() } } ?: java.time.LocalDate.now(),
                                activityType = try { ActivityType.valueOf(data["activityType"] as? String ?: ActivityType.OTHER.name) } catch (e: Exception) { ActivityType.OTHER },
                                notes = data["notes"] as? String,
                                fromPastureId = data["fromPastureId"] as? String,
                                toPastureId = data["toPastureId"] as? String,
                                details = data["details"] as? String,
                                groupId = data["groupId"] as? String,
                                herdId = data["herdId"] as? String,
                                firestoreId = firestoreId,
                                lastSyncAt = remoteUpdatedAt,
                                isDeleted = false,
                                createdBy = data["createdBy"] as? String,
                                updatedBy = data["updatedBy"] as? String
                            )
                            
                            if (localActivity == null) repository.insertActivity(remoteActivity)
                            else repository.updateActivity(remoteActivity.copy(id = localActivity.id))
                            println("Downloaded activity: ${remoteActivity.activityType} for cow ${localCow.name} (ID: ${localCow.id})")
                        } else {
                            val remoteCowId = (data["cowId"] as? Number)?.toLong() ?: 0L
                            println("Skipping activity ${data["activityType"]} - referenced cow not found locally (cowFirestoreId: $cowFirestoreId, cowId: $remoteCowId)")
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
    
    private suspend fun handleRealtimeCowChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data 
            if (data == null) {
                println("Real-time cow change: data is null for doc ${doc.id}")
                return
            }
            val firestoreId = doc.id
            
            val remoteCow = Cow(
                id = 0L, 
                name = data["name"] as? String,
                tagNumber = data["tagNumber"] as? String,
                tagColor = data["tagColor"] as? String,
                birthDate = (data["birthDate"] as? String)?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { null } },
                gender = try { Gender.valueOf(data["gender"] as? String ?: Gender.TBD.name) } catch (e: Exception) { Gender.TBD },
                classification = try { Classification.valueOf(data["classification"] as? String ?: Classification.CALF.name) } catch (e: Exception) { Classification.CALF },
                colorMarkings = data["colorMarkings"] as? String,
                motherId = (data["motherId"] as? Number)?.toLong(),
                fatherId = (data["fatherId"] as? Number)?.toLong(),
                status = try { Status.valueOf(data["status"] as? String ?: Status.ACTIVE.name) } catch (e: Exception) { Status.ACTIVE },
                pastureId = data["pastureId"] as? String,
                photos = (data["photos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                isWatched = data["isWatched"] as? Boolean ?: false,
                herdId = data["herdId"] as? String,
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
           
            val remotePasture = Pasture(
                id = firestoreId, // Corrected: Use firestoreId as the local primary key directly
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
                    val existingLocalPasture = repository.getAllPasturesSync().find { it.id == firestoreId } // Find by primary key
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
                            repository.updatePasture(remotePasture) // id matches, so it's an update
                            println("Real-time ADDED (local copy exists): Updated local pasture '${remotePasture.name}'.")
                        } else {
                            println("Real-time ADDED (local copy exists): Local pasture '${remotePasture.name}' is same or newer. No update needed.")
                        }
                    }
                }
                DocumentChange.Type.MODIFIED -> {
                    val existingLocalPasture = repository.getAllPasturesSync().find { it.id == firestoreId } // Find by primary key
                    if (existingLocalPasture == null) {
                        if (!remotePasture.isDeleted) {
                             println("Real-time MODIFIED: Pasture '${remotePasture.name}' not local. Inserting.")
                             repository.insertPasture(remotePasture) // id is already firestoreId
                        }
                    } else {
                        if (existingLocalPasture.lastSyncAt < remotePasture.lastSyncAt || remotePasture.isDeleted != existingLocalPasture.isDeleted) {
                            println("Real-time MODIFIED: Updating pasture '${remotePasture.name}'.")
                            repository.updatePasture(remotePasture) // id matches, so it's an update
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    val existingLocalPasture = repository.getAllPasturesSync().find { it.id == firestoreId } // Find by primary key
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
            
            // Try to resolve cow reference using cowFirestoreId first, then fallback to cowId
            val cowFirestoreId = data["cowFirestoreId"] as? String
            val localCows = repository.getAllCowsSync()
            val localCow = if (cowFirestoreId != null) {
                localCows.find { it.firestoreId == cowFirestoreId }
            } else {
                // Fallback: try to find by the stored cowId (might work if IDs happen to match)
                val remoteCowId = (data["cowId"] as? Number)?.toLong() ?: 0L
                repository.getCowById(remoteCowId)
            }

            if (localCow == null) {
                val remoteCowId = (data["cowId"] as? Number)?.toLong() ?: 0L
                println("Real-time ${change.type} for activity '${data["activityType"]}' (FS ID: $firestoreId): Skipping - referenced cow not found locally (cowFirestoreId: $cowFirestoreId, cowId: $remoteCowId). Will sync during full sync.")
                return
            }
            
            val remoteActivity = Activity(
                id = 0L, 
                cowId = localCow.id, // Use local cow ID
                date = (data["date"] as? String)?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { java.time.LocalDate.now() } } ?: java.time.LocalDate.now(),
                activityType = try { ActivityType.valueOf(data["activityType"] as? String ?: ActivityType.OTHER.name) } catch (e: Exception) { ActivityType.OTHER },
                notes = data["notes"] as? String,
                fromPastureId = data["fromPastureId"] as? String,
                toPastureId = data["toPastureId"] as? String,
                details = data["details"] as? String,
                groupId = data["groupId"] as? String,
                herdId = data["herdId"] as? String,
                firestoreId = firestoreId,
                lastSyncAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                createdBy = data["createdBy"] as? String,
                updatedBy = data["updatedBy"] as? String
            )

            println("Real-time ${change.type} for activity '${remoteActivity.activityType}' (FS ID: $firestoreId), updatedBy: ${remoteActivity.updatedBy}, remoteUpdatedAt: ${remoteActivity.lastSyncAt}")

            when (change.type) {
                DocumentChange.Type.ADDED -> {
                    val existingLocalActivity = repository.getAllActivitiesSync().find { it.firestoreId == firestoreId }
                    if (existingLocalActivity == null) {
                        if (!remoteActivity.isDeleted) {
                            println("Real-time ADDED (local copy missing): Inserting activity '${remoteActivity.activityType}' for cow ${localCow.name}.")
                            repository.insertActivity(remoteActivity.copy(id = 0L))
                        }
                    } else {
                        if (existingLocalActivity.lastSyncAt < remoteActivity.lastSyncAt || remoteActivity.isDeleted != existingLocalActivity.isDeleted) {
                            println("Real-time ADDED (conflict): Updating activity '${remoteActivity.activityType}' for cow ${localCow.name}.")
                            repository.updateActivity(remoteActivity.copy(id = existingLocalActivity.id))
                        } else {
                            println("Real-time ADDED (already up-to-date): Skipping activity '${remoteActivity.activityType}' for cow ${localCow.name}.")
                        }
                    }
                }
                DocumentChange.Type.MODIFIED -> {
                    val existingLocalActivity = repository.getAllActivitiesSync().find { it.firestoreId == firestoreId }
                    if (existingLocalActivity == null) {
                        if (!remoteActivity.isDeleted) {
                            println("Real-time MODIFIED: Activity '${remoteActivity.activityType}' not local. Inserting for cow ${localCow.name}.")
                            repository.insertActivity(remoteActivity.copy(id = 0L))
                        }
                    } else {
                        if (existingLocalActivity.lastSyncAt < remoteActivity.lastSyncAt || remoteActivity.isDeleted != existingLocalActivity.isDeleted) {
                            println("Real-time MODIFIED: Updating activity '${remoteActivity.activityType}' for cow ${localCow.name}.")
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

}

enum class SyncStatus {
    IDLE, SYNCING, SUCCESS, ERROR
}

enum class ItemSyncStatus {
    IDLE, SYNCING, SUCCESS, ERROR
}

private fun Cow.toFirestoreMap(userId: String): Map<String, Any?> {
    return mapOf(
        "name" to name,
        "tagNumber" to tagNumber,
        "tagColor" to tagColor,
        "birthDate" to birthDate?.toString(),
        "gender" to gender.name,
        "classification" to classification.name,
        "colorMarkings" to colorMarkings,
        "motherId" to motherId,
        "fatherId" to fatherId,
        "status" to status.name,
        "pastureId" to pastureId,
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
        "herdId" to herdId,
        "isDeleted" to isDeleted,
        "createdBy" to (createdBy ?: userId),
        "updatedBy" to userId,
        "updatedAt" to System.currentTimeMillis()
    )
}
