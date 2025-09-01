package com.jumblemint.cows.data.repository

import com.jumblemint.cows.data.dao.ActivityDao
import com.jumblemint.cows.data.dao.CowDao
import com.jumblemint.cows.data.dao.PastureDao
import com.jumblemint.cows.data.dao.SettingsDao
import com.jumblemint.cows.data.dao.NoteDao
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.ui.viewmodel.PastureWithCowCount // <<< ADDED IMPORT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.util.UUID

class CattleRepository(
    private val cowDao: CowDao,
    private val pastureDao: PastureDao,
    private val activityDao: ActivityDao,
    private val settingsDao: SettingsDao,
    private val noteDao: NoteDao? = null
) {

    // Cow operations
    fun getAllCows(): Flow<List<Cow>> = cowDao.getAllCows()
    fun getCowsByStatus(status: Status): Flow<List<Cow>> = cowDao.getCowsByStatus(status)
    // MODIFIED: pastureId parameter changed from Long to String
    fun getCowsByPasture(pastureId: String): Flow<List<Cow>> = cowDao.getCowsByPasture(pastureId)
    fun getActiveFemales(): Flow<List<Cow>> = cowDao.getActiveFemales()
    fun getActiveMales(): Flow<List<Cow>> = cowDao.getActiveMales()
    fun getCalvesByMother(motherId: Long): Flow<List<Cow>> = cowDao.getCalvesByMother(motherId)
    fun getCalvesByFather(fatherId: Long): Flow<List<Cow>> = cowDao.getCalvesByFather(fatherId)
    suspend fun getCowById(id: Long): Cow? = cowDao.getCowById(id)
    suspend fun getCowByTagNumber(tagNumber: String): Cow? = cowDao.getCowByTagNumber(tagNumber)
    fun getWatchedCows(): Flow<List<Cow>> = cowDao.getWatchedCows()

    suspend fun insertCow(cow: Cow): Long = cowDao.insertCow(cow)
    suspend fun updateCow(cow: Cow) = cowDao.updateCow(cow)
    suspend fun deleteCow(cow: Cow) = cowDao.deleteCow(cow)
    suspend fun updateCowWatchStatus(cowId: Long, isWatched: Boolean) = cowDao.updateCowWatchStatus(cowId, isWatched)

    // Pasture operations
    fun getAllPastures(): Flow<List<Pasture>> = pastureDao.getAllPastures()
    suspend fun getPastureById(id: String): Pasture? = pastureDao.getPastureById(id).firstOrNull()

    suspend fun insertPasture(pasture: Pasture): Long = pastureDao.insert(pasture)
    suspend fun updatePasture(pasture: Pasture) = pastureDao.update(pasture)
    suspend fun deletePasture(pasture: Pasture) = pastureDao.delete(pasture)
    // <<< ADDED METHOD
    fun getPasturesWithCowCount(): Flow<List<PastureWithCowCount>> = pastureDao.getAllPasturesWithCowCounts()
    fun getUnassignedCowCount(): Flow<Int> = pastureDao.getUnassignedCowCount()


    // Activity operations
    fun getAllActivities(): Flow<List<Activity>> = activityDao.getAllActivities()
    fun getActivitiesForCow(cowId: Long): Flow<List<Activity>> = activityDao.getActivitiesForCow(cowId)
    fun getActivitiesByType(activityType: ActivityType): Flow<List<Activity>> = activityDao.getActivitiesByType(activityType)

    suspend fun getActivityById(id: Long): Activity? = activityDao.getActivityById(id)
    suspend fun getActivitiesByGroupId(groupId: String): List<Activity> = activityDao.getActivitiesByGroupId(groupId)
    suspend fun insertActivity(activity: Activity): Long = activityDao.insertActivity(activity)
    suspend fun insertActivities(activities: List<Activity>) = activityDao.insertActivities(activities)
    suspend fun updateActivity(activity: Activity) = activityDao.updateActivity(activity)
    suspend fun deleteActivity(activity: Activity) = activityDao.deleteActivity(activity)

    // Settings operations
    fun getAllSettings(): Flow<List<Settings>> = settingsDao.getAllSettings()
    suspend fun getSettingByKey(key: String): Settings? = settingsDao.getSettingByKey(key)
    suspend fun insertOrUpdateSetting(setting: Settings) = settingsDao.insertOrUpdateSetting(setting)
    suspend fun deleteSetting(setting: Settings) = settingsDao.deleteSetting(setting)

    // Business logic operations
    // MODIFIED: toPastureId parameter changed from Long? to String?
    suspend fun moveCow(cowId: Long, toPastureId: String?) {
        val cow = getCowById(cowId) ?: return
        val fromPastureId = cow.pastureId // This is now String?
        
        cowDao.updateCowPasture(cowId, toPastureId) // updateCowPasture now expects String?
        
        val moveActivity = Activity(
            cowId = cowId,
            date = LocalDate.now(),
            activityType = ActivityType.MOVED,
            fromPastureId = fromPastureId, // Correctly String?
            toPastureId = toPastureId,    // Correctly String?
            groupId = UUID.randomUUID().toString() // Individual activity gets its own group
        )
        insertActivity(moveActivity)
    }

    suspend fun castrateCow(cowId: Long, notes: String? = null) {
        val cow = getCowById(cowId) ?: return
        
        if (cow.classification == Classification.BULL) {
            cowDao.updateCowClassification(cowId, Classification.STEER.name)
        }
        
        val castrationActivity = Activity(
            cowId = cowId,
            date = LocalDate.now(),
            activityType = ActivityType.CASTRATED,
            notes = notes,
            groupId = UUID.randomUUID().toString() // Individual activity gets its own group
        )
        insertActivity(castrationActivity)
    }
    
    suspend fun recordBirth(
        motherId: Long,
        fatherId: Long?,
        calfName: String?,
        calfGender: Gender,
        birthDate: LocalDate = LocalDate.now()
    ): Long {
        // Example: If you have a default calf pasture ID as a String
        // val defaultCalfPastureId: String? = getCalfPasture()?.id 
        val calf = Cow(
            name = calfName,
            birthDate = birthDate,
            gender = calfGender,
            classification = Classification.CALF,
            motherId = motherId,
            fatherId = fatherId,
            pastureId = null, // pastureId is now String?, so null is fine or a String ID
            status = Status.ACTIVE
        )
        
        val calfId = insertCow(calf)
        
        return calfId
    }

    // MODIFIED: toPastureId parameter changed from Long? to String?
    suspend fun createBulkActivity(
        cowIds: List<Long>,
        activityType: ActivityType,
        date: LocalDate = LocalDate.now(),
        notes: String? = null,
        toPastureId: String? = null
    ) {
        // Generate a unique group ID for this bulk activity
        val groupId = UUID.randomUUID().toString()
        
        val activities = cowIds.map { cowId ->
            // For MOVED activities, we\'d need fromPastureId.
            // This might require fetching each cow if their fromPastureId is needed for the Activity record.
            // For simplicity, let\'s assume Activity record only needs toPastureId for MOVED if cows are updated directly.
            // A more robust way would be to fetch each cow, get its current pastureId for fromPastureId.
            // However, the current Activity model doesn\'t make fromPastureId mandatory.
            Activity(
                cowId = cowId,
                date = date,
                activityType = activityType,
                notes = notes,
                toPastureId = if (activityType == ActivityType.MOVED) toPastureId else null,
                groupId = groupId
            )
        }
        
        insertActivities(activities)
        
        if (activityType == ActivityType.MOVED) {
            toPastureId?.let { pastureId -> // pastureId here is String
                cowIds.forEach { cowId ->
                    cowDao.updateCowPasture(cowId, pastureId) // updateCowPasture expects String?
                }
            }
        } else if (activityType == ActivityType.CASTRATED) {
            cowIds.forEach { cowId ->
                val cow = getCowById(cowId)
                if (cow?.classification == Classification.BULL) {
                    cowDao.updateCowClassification(cowId, Classification.STEER.name)
                }
            }
        } else if (activityType == ActivityType.SOLD || activityType == ActivityType.DECEASED) {
            val newStatus = if (activityType == ActivityType.SOLD) Status.SOLD else Status.DECEASED
            cowIds.forEach { cowId ->
                val cow = getCowById(cowId)
                cow?.let {
                    updateCow(it.copy(status = newStatus, pastureId = null, updatedAt = LocalDate.now())) // Cows sold/deceased might be removed from pasture
                }
            }
        }
    }

    // Helper method for editing activities with a specific groupId
    suspend fun createBulkActivityWithGroupId(
        cowIds: List<Long>,
        activityType: ActivityType,
        date: LocalDate = LocalDate.now(),
        notes: String? = null,
        toPastureId: String? = null,
        groupId: String
    ) {
        val activities = cowIds.map { cowId ->
            Activity(
                cowId = cowId,
                date = date,
                activityType = activityType,
                notes = notes,
                toPastureId = if (activityType == ActivityType.MOVED) toPastureId else null,
                groupId = groupId
            )
        }
        
        insertActivities(activities)
        
        if (activityType == ActivityType.MOVED) {
            toPastureId?.let { pastureId ->
                cowIds.forEach { cowId ->
                    cowDao.updateCowPasture(cowId, pastureId)
                }
            }
        } else if (activityType == ActivityType.CASTRATED) {
            cowIds.forEach { cowId ->
                val cow = getCowById(cowId)
                if (cow?.classification == Classification.BULL) {
                    cowDao.updateCowClassification(cowId, Classification.STEER.name)
                }
            }
        } else if (activityType == ActivityType.SOLD || activityType == ActivityType.DECEASED) {
            val newStatus = if (activityType == ActivityType.SOLD) Status.SOLD else Status.DECEASED
            cowIds.forEach { cowId ->
                val cow = getCowById(cowId)
                cow?.let {
                    updateCow(it.copy(status = newStatus, pastureId = null, updatedAt = LocalDate.now()))
                }
            }
        }
    }

    // MODIFIED: newPastureId parameter changed from Long? to String?
    suspend fun weanCalf(calfId: Long, newPastureId: String? = null) {
        val calf = getCowById(calfId) ?: return
        
        val fromPastureId = calf.pastureId // This is now String?
        
        newPastureId?.let { pastureId -> // pastureId here is String
            cowDao.updateCowPasture(calfId, pastureId) // updateCowPasture expects String?
        }
        
        calf.birthDate?.let { birthDate ->
            val age = java.time.Period.between(birthDate, LocalDate.now())
            if (age.months >= 6) { 
                val newClassification = when (calf.gender) {
                    Gender.FEMALE -> Classification.HEIFER
                    Gender.MALE -> Classification.BULL
                    Gender.TBD -> Classification.CALF 
                }
                if (newClassification != calf.classification) {
                    cowDao.updateCowClassification(calfId, newClassification.name)
                }
            }
        }
        
        val weaningActivity = Activity(
            cowId = calfId,
            date = LocalDate.now(),
            activityType = ActivityType.WEANED,
            fromPastureId = fromPastureId,    // Correctly String?
            toPastureId = newPastureId,      // Correctly String?
            notes = "Weaned from mother",
            groupId = UUID.randomUUID().toString() // Individual activity gets its own group
        )
        insertActivity(weaningActivity)
    }

    suspend fun initializeDefaultData() {
        val existingCalfPasture = pastureDao.getAllPastures().firstOrNull()?.find { it.name == "Calf Pasture" }
        if (existingCalfPasture == null) {
            val calfPasture = Pasture(
                id = UUID.randomUUID().toString(), // ID is String, this is good
                name = "Calf Pasture",
                description = "Default pasture for calves",
                sizeAcres = 0.0
            )
            insertPasture(calfPasture)
        }
        
        if (getSettingByKey(SettingsKeys.TAG_COLORS) == null) {
            insertOrUpdateSetting(
                Settings(
                    SettingsKeys.TAG_COLORS,
                    "Red,Blue,Green,Yellow,Orange,Purple,Pink,White,Black,Brown"
                )
            )
        }
        
        if (getSettingByKey(SettingsKeys.ACTIVITY_TYPES) == null) {
            insertOrUpdateSetting(
                Settings(
                    SettingsKeys.ACTIVITY_TYPES,
                    "MOVED,WEANED,SOLD,DECEASED,WORKED,CASTRATED,BIRTH,OTHER"
                )
            )
        }
    }
}
