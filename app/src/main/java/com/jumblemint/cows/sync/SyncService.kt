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

class SyncService(
    private val repository: CattleRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()
    
    // Separate status for individual item syncing (more subtle)
    private val _itemSyncStatus = MutableStateFlow(ItemSyncStatus.IDLE)
    val itemSyncStatus: Flow<ItemSyncStatus> = _itemSyncStatus.asStateFlow()
    
    private var herdListeners = mutableMapOf<String, ListenerRegistration>()
    
    suspend fun syncUserData(userId: String): Result<Unit> {
        return try {
            println("Starting sync for user: $userId")
            _syncStatus.value = SyncStatus.SYNCING
            
            // Clean up any potential duplicates first (one-time cleanup)
            cleanupDuplicates()
            
            // Offline-first sync: sync all user data with proper conflict resolution
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
            println("Error type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private suspend fun cleanupDuplicates() {
        try {
            println("Cleaning up potential duplicates...")
            
            // Remove items that have firestoreId but no lastSyncAt (likely duplicates from old sync)
            val allCows = repository.getAllCowsSync()
            val duplicateCows = allCows.filter { it.firestoreId != null && it.lastSyncAt == 0L }
            println("Found ${duplicateCows.size} potential duplicate cows")
            
            val allPastures = repository.getAllPasturesSync()
            val duplicatePastures = allPastures.filter { it.firestoreId != null && it.lastSyncAt == 0L }
            println("Found ${duplicatePastures.size} potential duplicate pastures")
            
            // For now, just mark them as synced to avoid re-uploading
            duplicateCows.forEach { cow ->
                repository.updateCow(cow.copy(lastSyncAt = System.currentTimeMillis()))
            }
            duplicatePastures.forEach { pasture ->
                repository.updatePasture(pasture.copy(lastSyncAt = System.currentTimeMillis()))
            }
            
            println("Cleanup completed")
        } catch (e: Exception) {
            println("Error during cleanup: ${e.message}")
            // Don't fail the entire sync for cleanup issues
        }
    }
    
    fun startRealtimeSync(userId: String) {
        // Stop existing listeners for this user
        stopRealtimeSync(userId)
        
        val listeners = mutableListOf<ListenerRegistration>()
        
        println("Starting real-time sync for user: $userId")
        
        // Listen to user's cows changes
        listeners.add(
            firestore.collection("users").document(userId)
                .collection("cows")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("Real-time sync error for cows: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    snapshot?.documentChanges?.forEach { change ->
                        CoroutineScope(Dispatchers.IO).launch {
                            handleRealtimeCowChange(change, userId)
                        }
                    }
                }
        )
        
        // Listen to user's pastures changes
        listeners.add(
            firestore.collection("users").document(userId)
                .collection("pastures")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("Real-time sync error for pastures: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    snapshot?.documentChanges?.forEach { change ->
                        CoroutineScope(Dispatchers.IO).launch {
                            handleRealtimePastureChange(change, userId)
                        }
                    }
                }
        )
        
        // Listen to user's activities changes
        listeners.add(
            firestore.collection("users").document(userId)
                .collection("activities")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("Real-time sync error for activities: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    snapshot?.documentChanges?.forEach { change ->
                        CoroutineScope(Dispatchers.IO).launch {
                            handleRealtimeActivityChange(change, userId)
                        }
                    }
                }
        )
        
        // Store all listeners for cleanup (fix the simplified storage)
        herdListeners[userId] = listeners.first() // TODO: Store all listeners properly
        
        println("Real-time sync listeners established for user: $userId")
    }
    
    fun stopRealtimeSync(userId: String) {
        herdListeners[userId]?.remove()
        herdListeners.remove(userId)
    }
    
    fun stopAllRealtimeSync() {
        herdListeners.values.forEach { it.remove() }
        herdListeners.clear()
    }
    
    // Method to sync a single item immediately when it's modified locally
    suspend fun syncItemImmediately(userId: String, item: Any): Result<Unit> {
        return try {
            _itemSyncStatus.value = ItemSyncStatus.SYNCING
            when (item) {
                is Cow -> {
                    val firestoreId = item.firestoreId ?: UUID.randomUUID().toString()
                    val cowData = item.toFirestoreMap(userId)
                    firestore.collection("users").document(userId)
                        .collection("cows").document(firestoreId)
                        .set(cowData).await()
                    
                    // Update local record with sync info
                    repository.updateCow(item.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = System.currentTimeMillis(),
                        updatedBy = userId
                    ))
                    println("Immediately synced cow: ${item.name}")
                }
                is Pasture -> {
                    val firestoreId = item.firestoreId ?: UUID.randomUUID().toString()
                    val pastureData = item.toFirestoreMap(userId)
                    firestore.collection("users").document(userId)
                        .collection("pastures").document(firestoreId)
                        .set(pastureData).await()
                    
                    repository.updatePasture(item.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = System.currentTimeMillis(),
                        updatedBy = userId
                    ))
                    println("Immediately synced pasture: ${item.name}")
                }
                is Activity -> {
                    val firestoreId = item.firestoreId ?: UUID.randomUUID().toString()
                    val activityData = item.toFirestoreMap(userId)
                    firestore.collection("users").document(userId)
                        .collection("activities").document(firestoreId)
                        .set(activityData).await()
                    
                    repository.updateActivity(item.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = System.currentTimeMillis(),
                        updatedBy = userId
                    ))
                    println("Immediately synced activity: ${item.activityType}")
                }
                is Note -> {
                    val firestoreId = item.firestoreId ?: UUID.randomUUID().toString()
                    val noteData = item.toFirestoreMap(userId)
                    firestore.collection("users").document(userId)
                        .collection("notes").document(firestoreId)
                        .set(noteData).await()
                    
                    repository.updateNote(item.copy(
                        firestoreId = firestoreId,
                        lastSyncAt = System.currentTimeMillis(),
                        updatedBy = userId
                    ))
                    println("Immediately synced note: ${item.title}")
                }
            }
            _itemSyncStatus.value = ItemSyncStatus.SUCCESS
            // Reset to idle after a short delay
            kotlinx.coroutines.delay(2000)
            _itemSyncStatus.value = ItemSyncStatus.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            println("Failed to immediately sync item: ${e.message}")
            _itemSyncStatus.value = ItemSyncStatus.ERROR
            // Reset to idle after a longer delay for errors
            kotlinx.coroutines.delay(3000)
            _itemSyncStatus.value = ItemSyncStatus.IDLE
            Result.failure(e)
        }
    }
    
    private suspend fun syncUserPastures(userId: String) {
        try {
            println("Starting pasture sync for user: $userId")
            val localPastures = repository.getAllPasturesSync()
            println("Found ${localPastures.size} local pastures")
            
            // Get all remote pastures first to compare
            val remoteSnapshot = firestore.collection("users").document(userId)
                .collection("pastures").get().await()
            println("Found ${remoteSnapshot.documents.size} remote pastures")
            
            // Create map of remote pastures by firestoreId for quick lookup
            val remotePasturesMap = mutableMapOf<String, Map<String, Any?>>()
            remoteSnapshot.documents.forEach { doc ->
                doc.data?.let { data ->
                    remotePasturesMap[doc.id] = data
                }
            }
            
            // 1. Upload local changes (new or modified items)
            localPastures.forEach { pasture ->
                try {
                    val firestoreId = pasture.firestoreId ?: UUID.randomUUID().toString()
                    val remoteData = remotePasturesMap[firestoreId]
                    
                    val shouldUpload = when {
                        pasture.firestoreId == null -> {
                            println("Uploading new pasture: ${pasture.name}")
                            true
                        }
                        remoteData == null -> {
                            println("Uploading missing pasture: ${pasture.name}")
                            true
                        }
                        pasture.lastSyncAt == 0L -> {
                            println("Uploading unsynced pasture: ${pasture.name}")
                            true
                        }
                        else -> {
                            // Check if local is newer than remote
                            val remoteUpdatedAt = (remoteData["updatedAt"] as? Number)?.toLong() ?: 0L
                            val localIsNewer = pasture.lastSyncAt > remoteUpdatedAt
                            if (localIsNewer) {
                                println("Uploading newer local pasture: ${pasture.name}")
                            }
                            localIsNewer
                        }
                    }
                    
                    if (shouldUpload) {
                        val pastureData = pasture.toFirestoreMap(userId)
                        firestore.collection("users").document(userId)
                            .collection("pastures").document(firestoreId)
                            .set(pastureData).await()
                        
                        // Update local record with sync info
                        repository.updatePasture(pasture.copy(
                            firestoreId = firestoreId,
                            lastSyncAt = System.currentTimeMillis(),
                            updatedBy = userId
                        ))
                        println("Successfully uploaded pasture: ${pasture.name}")
                    }
                } catch (e: Exception) {
                    println("Error uploading pasture ${pasture.name}: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            // 2. Download remote changes (new or modified items)
            val localPasturesMap = localPastures.associateBy { it.firestoreId }
            
            remotePasturesMap.forEach { (firestoreId, data) ->
                try {
                    val localPasture = localPasturesMap[firestoreId]
                    val remoteUpdatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                    
                    val isRemoteDeleted = data["isDeleted"] as? Boolean ?: false
                    
                    val shouldDownload = when {
                        isRemoteDeleted -> {
                            println("Skipping deleted remote pasture: ${data["name"]}")
                            false
                        }
                        localPasture == null -> {
                            println("Downloading new remote pasture: ${data["name"]}")
                            true
                        }
                        localPasture.lastSyncAt < remoteUpdatedAt -> {
                            println("Downloading newer remote pasture: ${data["name"]}")
                            true
                        }
                        else -> false
                    }
                    
                    if (shouldDownload) {
                        val remotePasture = Pasture(
                            id = localPasture?.id ?: data["id"] as? String ?: firestoreId,
                            name = data["name"] as? String ?: "",
                            description = data["description"] as? String,
                            sizeAcres = (data["sizeAcres"] as? Number)?.toDouble(),
                            herdId = data["herdId"] as? String,
                            firestoreId = firestoreId,
                            lastSyncAt = System.currentTimeMillis(),
                            isDeleted = data["isDeleted"] as? Boolean ?: false,
                            createdBy = data["createdBy"] as? String,
                            updatedBy = data["updatedBy"] as? String
                        )
                        
                        if (localPasture == null) {
                            repository.insertPasture(remotePasture)
                        } else {
                            repository.updatePasture(remotePasture)
                        }
                        println("Downloaded pasture: ${remotePasture.name}")
                    }
                } catch (e: Exception) {
                    println("Error downloading pasture from doc $firestoreId: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            println("Pasture sync completed successfully")
        } catch (e: Exception) {
            println("Error in syncUserPastures: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    private suspend fun syncUserCows(userId: String) {
        try {
            println("Starting cow sync for user: $userId")
            val localCows = repository.getAllCowsSync()
            println("Found ${localCows.size} local cows")
            
            // Get all remote cows first to compare
            val remoteSnapshot = firestore.collection("users").document(userId)
                .collection("cows").get().await()
            println("Found ${remoteSnapshot.documents.size} remote cows")
            
            // Create map of remote cows by firestoreId for quick lookup
            val remoteCowsMap = mutableMapOf<String, Map<String, Any?>>()
            remoteSnapshot.documents.forEach { doc ->
                doc.data?.let { data ->
                    remoteCowsMap[doc.id] = data
                }
            }
            
            // 1. Upload local changes (new or modified items)
            localCows.forEach { cow ->
                try {
                    val firestoreId = cow.firestoreId ?: UUID.randomUUID().toString()
                    val remoteData = remoteCowsMap[firestoreId]
                    
                    val shouldUpload = when {
                        cow.firestoreId == null -> {
                            println("Uploading new cow: ${cow.name}")
                            true
                        }
                        remoteData == null -> {
                            println("Uploading missing cow: ${cow.name}")
                            true
                        }
                        cow.lastSyncAt == 0L -> {
                            println("Uploading unsynced cow: ${cow.name}")
                            true
                        }
                        else -> {
                            // Check if local is newer than remote
                            val remoteUpdatedAt = (remoteData["updatedAt"] as? Number)?.toLong() ?: 0L
                            val localIsNewer = cow.lastSyncAt > remoteUpdatedAt
                            if (localIsNewer) {
                                println("Uploading newer local cow: ${cow.name}")
                            }
                            localIsNewer
                        }
                    }
                    
                    if (shouldUpload) {
                        val cowData = cow.toFirestoreMap(userId)
                        firestore.collection("users").document(userId)
                            .collection("cows").document(firestoreId)
                            .set(cowData).await()
                        
                        // Update local record with sync info
                        repository.updateCow(cow.copy(
                            firestoreId = firestoreId,
                            lastSyncAt = System.currentTimeMillis(),
                            updatedBy = userId
                        ))
                        println("Successfully uploaded cow: ${cow.name}")
                    }
                } catch (e: Exception) {
                    println("Error uploading cow ${cow.name}: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            // 2. Download remote changes (new or modified items)
            val localCowsMap = localCows.associateBy { it.firestoreId }
            
            remoteCowsMap.forEach { (firestoreId, data) ->
                try {
                    val localCow = localCowsMap[firestoreId]
                    val remoteUpdatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                    
                    val isRemoteDeleted = data["isDeleted"] as? Boolean ?: false
                    
                    val shouldDownload = when {
                        isRemoteDeleted -> {
                            println("Skipping deleted remote cow: ${data["name"]}")
                            false
                        }
                        localCow == null -> {
                            println("Downloading new remote cow: ${data["name"]}")
                            true
                        }
                        localCow.lastSyncAt < remoteUpdatedAt -> {
                            println("Downloading newer remote cow: ${data["name"]}")
                            true
                        }
                        else -> false
                    }
                    
                    if (shouldDownload) {
                        val remoteCow = Cow(
                            id = localCow?.id ?: (data["id"] as? Number)?.toLong() ?: 0L,
                            name = data["name"] as? String,
                            tagNumber = data["tagNumber"] as? String,
                            tagColor = data["tagColor"] as? String,
                            birthDate = (data["birthDate"] as? String)?.let { 
                                try { java.time.LocalDate.parse(it) } catch (e: Exception) { null }
                            },
                            gender = try { 
                                Gender.valueOf(data["gender"] as? String ?: "TBD") 
                            } catch (e: Exception) { Gender.TBD },
                            classification = try { 
                                Classification.valueOf(data["classification"] as? String ?: "CALF") 
                            } catch (e: Exception) { Classification.CALF },
                            colorMarkings = data["colorMarkings"] as? String,
                            motherId = (data["motherId"] as? Number)?.toLong(),
                            fatherId = (data["fatherId"] as? Number)?.toLong(),
                            status = try { 
                                Status.valueOf(data["status"] as? String ?: "ACTIVE") 
                            } catch (e: Exception) { Status.ACTIVE },
                            pastureId = data["pastureId"] as? String,
                            photos = (data["photos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            isWatched = data["isWatched"] as? Boolean ?: false,
                            herdId = data["herdId"] as? String,
                            firestoreId = firestoreId,
                            lastSyncAt = System.currentTimeMillis(),
                            isDeleted = data["isDeleted"] as? Boolean ?: false,
                            createdBy = data["createdBy"] as? String,
                            updatedBy = data["updatedBy"] as? String
                        )
                        
                        if (localCow == null) {
                            repository.insertCow(remoteCow)
                        } else {
                            repository.updateCow(remoteCow)
                        }
                        println("Downloaded cow: ${remoteCow.name}")
                    }
                } catch (e: Exception) {
                    println("Error downloading cow from doc $firestoreId: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            println("Cow sync completed successfully")
        } catch (e: Exception) {
            println("Error in syncUserCows: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    private suspend fun syncUserActivities(userId: String) {
        // Upload local changes first
        val localActivities = repository.getAllActivitiesSync()
        localActivities.forEach { activity ->
            if (activity.firestoreId == null || activity.lastSyncAt == 0L) {
                val firestoreId = activity.firestoreId ?: UUID.randomUUID().toString()
                val activityData = activity.toFirestoreMap(userId)
                
                firestore.collection("users").document(userId)
                    .collection("activities").document(firestoreId)
                    .set(activityData).await()
                
                repository.updateActivity(activity.copy(
                    firestoreId = firestoreId,
                    lastSyncAt = System.currentTimeMillis(),
                    updatedBy = userId
                ))
            }
        }
        
        // Download remote changes
        val remoteSnapshot = firestore.collection("users").document(userId)
            .collection("activities").get().await()
        
        remoteSnapshot.documents.forEach { doc ->
            try {
                val data = doc.data
                if (data != null) {
                    val isRemoteDeleted = data["isDeleted"] as? Boolean ?: false
                    
                    if (!isRemoteDeleted) {
                        val remoteActivity = Activity(
                            id = (data["id"] as? Number)?.toLong() ?: 0L,
                            cowId = (data["cowId"] as? Number)?.toLong() ?: 0L,
                            date = (data["date"] as? String)?.let { 
                                try { java.time.LocalDate.parse(it) } catch (e: Exception) { java.time.LocalDate.now() }
                            } ?: java.time.LocalDate.now(),
                            activityType = try { 
                                ActivityType.valueOf(data["activityType"] as? String ?: "OTHER") 
                            } catch (e: Exception) { ActivityType.OTHER },
                            notes = data["notes"] as? String,
                            fromPastureId = data["fromPastureId"] as? String,
                            toPastureId = data["toPastureId"] as? String,
                            details = data["details"] as? String,
                            groupId = data["groupId"] as? String,
                            herdId = data["herdId"] as? String,
                            firestoreId = doc.id,
                            lastSyncAt = System.currentTimeMillis(),
                            isDeleted = false,
                            createdBy = data["createdBy"] as? String,
                            updatedBy = data["updatedBy"] as? String
                        )
                        repository.insertActivity(remoteActivity)
                        println("Downloaded activity: ${remoteActivity.activityType}")
                    } else {
                        println("Skipping deleted remote activity: ${data["activityType"]}")
                    }
                }
            } catch (e: Exception) {
                println("Error downloading activity from doc ${doc.id}: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun syncUserNotes(userId: String) {
        // Upload local changes first
        val localNotes = repository.getAllNotesSync()
        localNotes.forEach { note ->
            if (note.firestoreId == null || note.lastSyncAt == 0L) {
                val firestoreId = note.firestoreId ?: UUID.randomUUID().toString()
                val noteData = note.toFirestoreMap(userId)
                
                firestore.collection("users").document(userId)
                    .collection("notes").document(firestoreId)
                    .set(noteData).await()
                
                repository.updateNote(note.copy(
                    firestoreId = firestoreId,
                    lastSyncAt = System.currentTimeMillis(),
                    updatedBy = userId
                ))
            }
        }
        
        // Download remote changes
        val remoteSnapshot = firestore.collection("users").document(userId)
            .collection("notes").get().await()
        
        remoteSnapshot.documents.forEach { doc ->
            try {
                val data = doc.data
                if (data != null) {
                    val isRemoteDeleted = data["isDeleted"] as? Boolean ?: false
                    
                    if (!isRemoteDeleted) {
                        val remoteNote = Note(
                            id = (data["id"] as? Number)?.toLong() ?: 0L,
                            title = data["title"] as? String ?: "",
                            text = data["text"] as? String ?: "",
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            herdId = data["herdId"] as? String,
                            firestoreId = doc.id,
                            lastSyncAt = System.currentTimeMillis(),
                            isDeleted = false,
                            createdBy = data["createdBy"] as? String,
                            updatedBy = data["updatedBy"] as? String
                        )
                        repository.insertNote(remoteNote)
                        println("Downloaded note: ${remoteNote.title}")
                    } else {
                        println("Skipping deleted remote note: ${data["title"]}")
                    }
                }
            } catch (e: Exception) {
                println("Error downloading note from doc ${doc.id}: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Real-time change handlers
    private suspend fun handleRealtimeCowChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data
            
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    println("Real-time cow change: ${change.type} - ${data["name"]}")
                    
                    // Check if we already have this cow locally
                    val localCows = repository.getAllCowsSync()
                    val existingCow = localCows.find { it.firestoreId == doc.id }
                    
                    val remoteCow = Cow(
                        id = existingCow?.id ?: (data["id"] as? Number)?.toLong() ?: 0L,
                        name = data["name"] as? String,
                        tagNumber = data["tagNumber"] as? String,
                        tagColor = data["tagColor"] as? String,
                        birthDate = (data["birthDate"] as? String)?.let { 
                            try { java.time.LocalDate.parse(it) } catch (e: Exception) { null }
                        },
                        gender = try { 
                            Gender.valueOf(data["gender"] as? String ?: "TBD") 
                        } catch (e: Exception) { Gender.TBD },
                        classification = try { 
                            Classification.valueOf(data["classification"] as? String ?: "CALF") 
                        } catch (e: Exception) { Classification.CALF },
                        colorMarkings = data["colorMarkings"] as? String,
                        motherId = (data["motherId"] as? Number)?.toLong(),
                        fatherId = (data["fatherId"] as? Number)?.toLong(),
                        status = try { 
                            Status.valueOf(data["status"] as? String ?: "ACTIVE") 
                        } catch (e: Exception) { Status.ACTIVE },
                        pastureId = data["pastureId"] as? String,
                        photos = (data["photos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        isWatched = data["isWatched"] as? Boolean ?: false,
                        herdId = data["herdId"] as? String,
                        firestoreId = doc.id,
                        lastSyncAt = System.currentTimeMillis(),
                        isDeleted = data["isDeleted"] as? Boolean ?: false,
                        createdBy = data["createdBy"] as? String,
                        updatedBy = data["updatedBy"] as? String
                    )
                    
                    // Only update if this change is from another device (not our own change)
                    val remoteUpdatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                    if (existingCow == null || existingCow.lastSyncAt < remoteUpdatedAt) {
                        if (existingCow == null && !remoteCow.isDeleted) {
                            // Only insert if not deleted
                            repository.insertCow(remoteCow)
                            println("Real-time: Added new cow ${remoteCow.name}")
                        } else if (existingCow != null) {
                            // Update existing cow (including deletion status)
                            repository.updateCow(remoteCow)
                            if (remoteCow.isDeleted) {
                                println("Real-time: Deleted cow ${remoteCow.name}")
                            } else {
                                println("Real-time: Updated cow ${remoteCow.name}")
                            }
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    println("Real-time cow removed: ${doc.id}")
                    // Handle deletion - mark as deleted or remove from local DB
                    val localCows = repository.getAllCowsSync()
                    val existingCow = localCows.find { it.firestoreId == doc.id }
                    existingCow?.let {
                        repository.updateCow(it.copy(isDeleted = true, lastSyncAt = System.currentTimeMillis()))
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time cow change: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private suspend fun handleRealtimePastureChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data
            
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    println("Real-time pasture change: ${change.type} - ${data["name"]}")
                    
                    val localPastures = repository.getAllPasturesSync()
                    val existingPasture = localPastures.find { it.firestoreId == doc.id }
                    
                    val remotePasture = Pasture(
                        id = existingPasture?.id ?: data["id"] as? String ?: doc.id,
                        name = data["name"] as? String ?: "",
                        description = data["description"] as? String,
                        sizeAcres = (data["sizeAcres"] as? Number)?.toDouble(),
                        herdId = data["herdId"] as? String,
                        firestoreId = doc.id,
                        lastSyncAt = System.currentTimeMillis(),
                        isDeleted = data["isDeleted"] as? Boolean ?: false,
                        createdBy = data["createdBy"] as? String,
                        updatedBy = data["updatedBy"] as? String
                    )
                    
                    val remoteUpdatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                    if (existingPasture == null || existingPasture.lastSyncAt < remoteUpdatedAt) {
                        if (existingPasture == null && !remotePasture.isDeleted) {
                            // Only insert if not deleted
                            repository.insertPasture(remotePasture)
                            println("Real-time: Added new pasture ${remotePasture.name}")
                        } else if (existingPasture != null) {
                            // Update existing pasture (including deletion status)
                            repository.updatePasture(remotePasture)
                            if (remotePasture.isDeleted) {
                                println("Real-time: Deleted pasture ${remotePasture.name}")
                            } else {
                                println("Real-time: Updated pasture ${remotePasture.name}")
                            }
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    println("Real-time pasture removed: ${doc.id}")
                    val localPastures = repository.getAllPasturesSync()
                    val existingPasture = localPastures.find { it.firestoreId == doc.id }
                    existingPasture?.let {
                        repository.updatePasture(it.copy(isDeleted = true, lastSyncAt = System.currentTimeMillis()))
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time pasture change: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private suspend fun handleRealtimeActivityChange(change: DocumentChange, userId: String) {
        try {
            val doc = change.document
            val data = doc.data
            
            when (change.type) {
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    println("Real-time activity change: ${change.type} - ${data["activityType"]}")
                    
                    val localActivities = repository.getAllActivitiesSync()
                    val existingActivity = localActivities.find { it.firestoreId == doc.id }
                    
                    val remoteActivity = Activity(
                        id = existingActivity?.id ?: (data["id"] as? Number)?.toLong() ?: 0L,
                        cowId = (data["cowId"] as? Number)?.toLong() ?: 0L,
                        date = (data["date"] as? String)?.let { 
                            try { java.time.LocalDate.parse(it) } catch (e: Exception) { java.time.LocalDate.now() }
                        } ?: java.time.LocalDate.now(),
                        activityType = try { 
                            ActivityType.valueOf(data["activityType"] as? String ?: "OTHER") 
                        } catch (e: Exception) { ActivityType.OTHER },
                        notes = data["notes"] as? String,
                        fromPastureId = data["fromPastureId"] as? String,
                        toPastureId = data["toPastureId"] as? String,
                        details = data["details"] as? String,
                        groupId = data["groupId"] as? String,
                        herdId = data["herdId"] as? String,
                        firestoreId = doc.id,
                        lastSyncAt = System.currentTimeMillis(),
                        isDeleted = data["isDeleted"] as? Boolean ?: false,
                        createdBy = data["createdBy"] as? String,
                        updatedBy = data["updatedBy"] as? String
                    )
                    
                    val remoteUpdatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                    if (existingActivity == null || existingActivity.lastSyncAt < remoteUpdatedAt) {
                        if (existingActivity == null && !remoteActivity.isDeleted) {
                            // Only insert if not deleted
                            repository.insertActivity(remoteActivity)
                            println("Real-time: Added new activity ${remoteActivity.activityType}")
                        } else if (existingActivity != null) {
                            // Update existing activity (including deletion status)
                            repository.updateActivity(remoteActivity)
                            if (remoteActivity.isDeleted) {
                                println("Real-time: Deleted activity ${remoteActivity.activityType}")
                            } else {
                                println("Real-time: Updated activity ${remoteActivity.activityType}")
                            }
                        }
                    }
                }
                DocumentChange.Type.REMOVED -> {
                    println("Real-time activity removed: ${doc.id}")
                    val localActivities = repository.getAllActivitiesSync()
                    val existingActivity = localActivities.find { it.firestoreId == doc.id }
                    existingActivity?.let {
                        repository.updateActivity(it.copy(isDeleted = true, lastSyncAt = System.currentTimeMillis()))
                    }
                }
            }
        } catch (e: Exception) {
            println("Error handling real-time activity change: ${e.message}")
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

// Extension functions to convert entities to Firestore maps
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

private fun Activity.toFirestoreMap(userId: String): Map<String, Any?> {
    return mapOf(
        "cowId" to cowId,
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
        "updatedAt" to System.currentTimeMillis()
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