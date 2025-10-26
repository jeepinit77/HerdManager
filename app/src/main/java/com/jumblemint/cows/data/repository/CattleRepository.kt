package com.jumblemint.cows.data.repository

import com.jumblemint.cows.data.dao.ActivityDao
import com.jumblemint.cows.data.dao.CowDao
import com.jumblemint.cows.data.dao.PastureDao
import com.jumblemint.cows.data.dao.SettingsDao
import com.jumblemint.cows.data.dao.NoteDao
import com.jumblemint.cows.data.dao.UserDao
import com.jumblemint.cows.data.dao.HerdDao
import com.jumblemint.cows.data.dao.HerdMemberDao
import com.jumblemint.cows.data.dao.TagColorDao
import com.jumblemint.cows.data.dao.ActivityTypeConfigDao
import com.jumblemint.cows.data.dao.BreedDao
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.ui.viewmodel.PastureWithCowCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.LinkedHashSet
import java.util.UUID

class CattleRepository(
    private val cowDao: CowDao,
    private val pastureDao: PastureDao,
    private val activityDao: ActivityDao,
    private val settingsDao: SettingsDao,
    private val noteDao: NoteDao? = null,
    private val userDao: UserDao? = null,
    private val herdDao: HerdDao? = null,
    private val herdMemberDao: HerdMemberDao? = null,
    private val tagColorDao: TagColorDao? = null,
    private val activityTypeConfigDao: ActivityTypeConfigDao? = null,
    private val breedDao: BreedDao? = null
) {

    // Cow operations
    fun getAllCows(): Flow<List<Cow>> = cowDao.getAllCows()
    fun getCowsByStatus(status: Status): Flow<List<Cow>> = cowDao.getCowsByStatus(status)
    fun getCowsByPasture(pastureId: String): Flow<List<Cow>> = cowDao.getCowsByPasture(pastureId)
    fun getActiveFemales(): Flow<List<Cow>> = cowDao.getEligibleMothers(null)
    fun getEligibleMothers(cutoffDate: LocalDate): Flow<List<Cow>> = cowDao.getEligibleMothers(cutoffDate)
    fun getActiveMales(): Flow<List<Cow>> = cowDao.getActiveMales()
    fun getCalvesByMother(motherId: Long): Flow<List<Cow>> = cowDao.getCalvesByMother(motherId)
    fun getCalvesByFather(fatherId: Long): Flow<List<Cow>> = cowDao.getCalvesByFather(fatherId)
    fun getCowsByIds(ids: List<Long>): Flow<List<Cow>> = cowDao.getCowsByIds(ids)
    suspend fun getRecentFatherIds(limit: Int): List<Long> = cowDao.getRecentFatherIds(limit)
    suspend fun getCowsByIdsImmediate(ids: List<Long>): List<Cow> = cowDao.getCowsByIdsImmediate(ids)

    suspend fun getRecentSires(limit: Int = DEFAULT_RECENT_SIRES_LIMIT): List<Cow> {
        val recentIds = getRecentFatherIds(limit)
        if (recentIds.isEmpty()) return emptyList()
        val sireRecords = getCowsByIdsImmediate(recentIds)
        val distinctOrderedIds = LinkedHashSet<Long>()
        recentIds.forEach { id ->
            if (id != 0L) {
                distinctOrderedIds.add(id)
            }
        }
        return distinctOrderedIds.mapNotNull { id -> sireRecords.find { it.id == id } }
    }

    suspend fun rememberRecentSire(sireId: Long, limit: Int = DEFAULT_RECENT_SIRES_LIMIT) {
        if (sireId == 0L) return
        val existing = getSettingByKey(SettingsKeys.RECENT_SIRE_IDS)?.value
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
        val updated = buildList {
            add(sireId)
            existing.forEach { id ->
                if (id != sireId) add(id)
            }
        }.take(limit)
        insertOrUpdateSetting(
            Settings(
                key = SettingsKeys.RECENT_SIRE_IDS,
                value = updated.joinToString(separator = ",")
            )
        )
    }

    fun getMaternalSiblings(cowId: Long, motherId: Long): Flow<List<Cow>> = cowDao.getMaternalSiblings(cowId, motherId)
    fun getPaternalSiblings(cowId: Long, fatherId: Long): Flow<List<Cow>> = cowDao.getPaternalSiblings(cowId, fatherId)

    suspend fun getCowById(id: Long): Cow? = cowDao.getCowById(id)
    fun getCowByIdFlow(id: Long): Flow<Cow?> = cowDao.getCowByIdFlow(id)
    suspend fun getCowByTagNumber(tagNumber: String): Cow? = cowDao.getCowByTagNumber(tagNumber)
    suspend fun getCowByFirestoreId(firestoreId: String): Cow? = cowDao.getCowByFirestoreId(firestoreId)
    fun getWatchedCows(): Flow<List<Cow>> = cowDao.getWatchedCows()

    suspend fun insertCow(cow: Cow): Long = cowDao.insertCow(cow)
    suspend fun updateCow(cow: Cow) = cowDao.updateCow(cow)
    suspend fun deleteCow(cow: Cow) = cowDao.deleteCow(cow)
    suspend fun updateCowWatchStatus(cowId: Long, isWatched: Boolean) = cowDao.updateCowWatchStatus(cowId, isWatched)

    // Pasture operations
    fun getAllPastures(): Flow<List<Pasture>> = pastureDao.getAllPastures()
    fun getPastureById(id: String): Flow<Pasture?> = pastureDao.getPastureById(id)
    suspend fun getPastureByIdSuspend(id: String): Pasture? = pastureDao.getPastureById(id).firstOrNull()

    suspend fun insertPasture(pasture: Pasture): Long = pastureDao.insert(pasture)
    suspend fun updatePasture(pasture: Pasture) = pastureDao.update(pasture)
    suspend fun deletePasture(pasture: Pasture) = pastureDao.delete(pasture)
    fun getPasturesWithCowCount(): Flow<List<PastureWithCowCount>> = pastureDao.getAllPasturesWithCowCounts()
    fun getUnassignedCowCount(): Flow<Int> = pastureDao.getUnassignedCowCount()

    // Activity operations
    fun getAllActivities(): Flow<List<Activity>> = activityDao.getAllActivities()
    fun getActivitiesForCow(cowId: Long): Flow<List<Activity>> = activityDao.getActivitiesForCow(cowId)
    fun getActivitiesByType(activityType: ActivityType): Flow<List<Activity>> = activityDao.getActivitiesByType(activityType)
    suspend fun getDistinctActivityTypes(): List<ActivityType> = activityDao.getDistinctActivityTypes()

    suspend fun getActivityById(id: Long): Activity? = activityDao.getActivityById(id)
    suspend fun getActivityByFirestoreId(firestoreId: String): Activity? = activityDao.getActivityByFirestoreId(firestoreId)
    suspend fun getActivitiesByGroupId(groupId: String): List<Activity> = activityDao.getActivitiesByGroupId(groupId)
    suspend fun insertActivity(activity: Activity): Long = activityDao.insertActivity(activity)
    suspend fun insertActivities(activities: List<Activity>) = activityDao.insertActivities(activities)
    suspend fun updateActivity(activity: Activity) = activityDao.updateActivity(activity)
    suspend fun deleteActivity(activity: Activity) = activityDao.deleteActivity(activity)

    // Settings operations
    fun getAllSettings(): Flow<List<Settings>> = settingsDao.getAllSettings()
    suspend fun getSettingByKey(key: String): Settings? = settingsDao.getSettingByKey(key)
    suspend fun insertOrUpdateSetting(setting: Settings) = settingsDao.insertOrUpdateSetting(setting)

    suspend fun setAnimalIdentifierMode(mode: AnimalIdentifierMode) {
        insertOrUpdateSetting(Settings(SettingsKeys.ANIMAL_IDENTIFIER_MODE, mode.name))
    }

    fun getAnimalIdentifierModeFlow(): Flow<AnimalIdentifierMode> =
        getAllSettings().map { settings ->
            val value = settings.firstOrNull { it.key == SettingsKeys.ANIMAL_IDENTIFIER_MODE }?.value
            AnimalIdentifierMode.fromValue(value)
        }

    suspend fun getAnimalIdentifierMode(): AnimalIdentifierMode {
        val setting = getSettingByKey(SettingsKeys.ANIMAL_IDENTIFIER_MODE)
        return AnimalIdentifierMode.fromValue(setting?.value)
    }

    suspend fun markInitialSetupComplete() {
        insertOrUpdateSetting(Settings(SettingsKeys.INITIAL_SETUP_COMPLETE, "true"))
    }
    suspend fun deleteSetting(setting: Settings) = settingsDao.deleteSetting(setting)

    // Business logic operations
    suspend fun moveCow(cowId: Long, toPastureId: String?) {
        val cow = getCowById(cowId) ?: return
        val fromPastureId = cow.pastureId
        
        cowDao.updateCowPasture(cowId, toPastureId)
        
        val moveActivity = Activity(
            cowId = cowId,
            date = LocalDate.now(),
            activityType = ActivityType.MOVED,
            fromPastureId = fromPastureId,
            toPastureId = toPastureId,
            groupId = UUID.randomUUID().toString(),
            cowIds = listOf(cowId) // For a single cow move, cowIds contains just that cow
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
            groupId = UUID.randomUUID().toString(),
            cowIds = listOf(cowId) // For a single cow operation, cowIds contains just that cow
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
        val calf = Cow(
            name = calfName,
            birthDate = birthDate,
            gender = calfGender,
            classification = Classification.CALF,
            motherId = motherId,
            fatherId = fatherId,
            pastureId = null,
            status = Status.ACTIVE
        )
        val calfId = insertCow(calf)
        // Calving activity is typically recorded for the newborn calf
        // If you also want to associate this with the dam, include her ID in cowIds
        val calvingActivity = Activity(
            cowId = calfId,
            date = birthDate,
            activityType = ActivityType.CALVED,
            notes = "Born to mother ID: $motherId" + (fatherId?.let { ", father ID: $it" } ?: ""),
            cowIds = listOf(calfId, motherId)
        )
        insertActivity(calvingActivity)
        return calfId
    }

    suspend fun createBulkActivity(
        cowIds: List<Long>,
        activityType: ActivityType,
        date: LocalDate = LocalDate.now(),
        notes: String? = null,
        toPastureId: String? = null
    ): List<Activity> {
        val groupId = UUID.randomUUID().toString()
        
        val activities = cowIds.map { currentCowId ->
            Activity(
                cowId = currentCowId,
                date = date,
                activityType = activityType,
                notes = notes,
                toPastureId = if (activityType == ActivityType.MOVED) toPastureId else null,
                groupId = groupId,
                cowIds = cowIds // <<< CORRECTED: Assign the full list of cow IDs
            )
        }
        
        val createdActivities = mutableListOf<Activity>()
        activities.forEach { activity ->
            val insertedId = insertActivity(activity)
            createdActivities.add(activity.copy(id = insertedId))
        }
        
        if (activityType == ActivityType.MOVED) {
            toPastureId?.let { pastureId ->
                for (id in cowIds) { // Use the full list here
                    cowDao.updateCowPasture(id, pastureId)
                }
            }
        } else if (activityType == ActivityType.CASTRATED) {
            for (id in cowIds) { // Use the full list here
                val cow = getCowById(id)
                if (cow?.classification == Classification.BULL) {
                    cowDao.updateCowClassification(id, Classification.STEER.name)
                }
            }
        } else if (activityType == ActivityType.SOLD || activityType == ActivityType.DECEASED) {
            val newStatus = if (activityType == ActivityType.SOLD) Status.SOLD else Status.DECEASED
            for (id in cowIds) { // Use the full list here
                val cow = getCowById(id)
                cow?.let { cowData ->
                    updateCow(cowData.copy(status = newStatus, pastureId = null, updatedAt = LocalDate.now()))
                }
            }
        }
        
        return createdActivities
    }

    suspend fun createBulkActivityWithGroupId(
        cowIds: List<Long>,
        activityType: ActivityType,
        date: LocalDate = LocalDate.now(),
        notes: String? = null,
        toPastureId: String? = null,
        groupId: String
    ): List<Activity> {
        val activities = cowIds.map { currentCowId ->
            Activity(
                cowId = currentCowId,
                date = date,
                activityType = activityType,
                notes = notes,
                toPastureId = if (activityType == ActivityType.MOVED) toPastureId else null,
                groupId = groupId,
                cowIds = cowIds // <<< CORRECTED: Assign the full list of cow IDs
            )
        }
        
        val createdActivities = mutableListOf<Activity>()
        activities.forEach { activity ->
            val insertedId = insertActivity(activity)
            createdActivities.add(activity.copy(id = insertedId))
        }
        
        if (activityType == ActivityType.MOVED) {
            toPastureId?.let { pastureId ->
                for (id in cowIds) { // Use the full list here
                    cowDao.updateCowPasture(id, pastureId)
                }
            }
        } else if (activityType == ActivityType.CASTRATED) {
            for (id in cowIds) { // Use the full list here
                val cow = getCowById(id)
                if (cow?.classification == Classification.BULL) {
                    cowDao.updateCowClassification(id, Classification.STEER.name)
                }
            }
        } else if (activityType == ActivityType.SOLD || activityType == ActivityType.DECEASED) {
            val newStatus = if (activityType == ActivityType.SOLD) Status.SOLD else Status.DECEASED
            for (id in cowIds) { // Use the full list here
                val cow = getCowById(id)
                cow?.let { cowData ->
                    updateCow(cowData.copy(status = newStatus, pastureId = null, updatedAt = LocalDate.now()))
                }
            }
        }
        
        return createdActivities
    }

    suspend fun weanCalf(calfId: Long, newPastureId: String? = null) {
        val calf = getCowById(calfId) ?: return
        val fromPastureId = calf.pastureId
        
        newPastureId?.let { pastureId ->
            cowDao.updateCowPasture(calfId, pastureId)
        }
        
        calf.birthDate?.let { birthDate ->
            val age = java.time.Period.between(birthDate, LocalDate.now())
            if (age.months >= 6) { 
                val newClassification: Classification = when (calf.gender) {
                    Gender.FEMALE -> Classification.HEIFER
                    Gender.MALE -> Classification.BULL
                    Gender.TBD -> Classification.CALF // Should ideally resolve TBD before this point
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
            fromPastureId = fromPastureId,
            toPastureId = newPastureId,
            notes = "Weaned from mother",
            groupId = UUID.randomUUID().toString(),
            cowIds = listOf(calfId) // For a single cow operation
        )
        insertActivity(weaningActivity)
    }

    // Notes operations (Flow for UI observers)
    fun getAllNotes(): Flow<List<Note>> = noteDao?.getAllNotes() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun initializeDefaultData() {
        // Only initialize if this is truly the first install (no data at all)
        val hasAnyExistingData = getAllCows().first().isNotEmpty() || 
                                 getAllPastures().first().isNotEmpty() ||
                                 getAllActivities().first().isNotEmpty()
        
        // Only auto-create defaults on first install when there's no data
        if (!hasAnyExistingData) {
            if (tagColorDao != null && getAllTagColors().first().isEmpty()) {
                val defaultColors = com.jumblemint.cows.data.model.TagColor.getDefaultColors()
                defaultColors.forEach { tagColor ->
                    insertTagColor(tagColor)
                }
            }
            
            if (activityTypeConfigDao != null && getAllActivityTypes().first().isEmpty()) {
                val defaultActivityTypes = com.jumblemint.cows.data.model.ActivityTypeConfig.getDefaultActivityTypes()
                defaultActivityTypes.forEach { activityType ->
                    insertActivityType(activityType)
                }
            }
            
            if (breedDao != null && getAllBreeds().first().isEmpty()) {
                val defaultBreeds = com.jumblemint.cows.data.model.Breed.getDefaultBreeds()
                defaultBreeds.forEach { breed ->
                    insertBreed(breed)
                }
            }
        }
        
        if (getSettingByKey(SettingsKeys.TAG_COLORS) == null) {
            insertOrUpdateSetting(
                Settings(
                    SettingsKeys.TAG_COLORS,
                    "Blue,Green,Red,Orange,White,Yellow"
                )
            )
        }
        
        if (getSettingByKey(SettingsKeys.ACTIVITY_TYPES) == null) {
            insertOrUpdateSetting(
                Settings(
                    SettingsKeys.ACTIVITY_TYPES,
                    "MOVED,WEANED,SOLD,DECEASED,WORKED,CASTRATED,BRED,CALVED,VACCINATED,TREATED,WEIGHED,PURCHASED,HEALTH_CHECK,TAGGED,NOTE,OTHER"
                )
            )
        }
    }

    suspend fun isSampleDataInstalled(): Boolean {
        return getSettingByKey(SettingsKeys.SAMPLE_DATA_INSTALLED)?.value == "true"
    }

    suspend fun hasAnyData(): Boolean {
        val cowCount = getAllCows().first().size
        val pastureCount = getAllPastures().first().size
        return cowCount > 0 || pastureCount > 0
    }

    suspend fun installSampleData() {
        if (isSampleDataInstalled()) return

        val pastureIds = createSamplePastures()
        val cowSampleIds = createSampleCows(pastureIds) // Renamed to avoid conflict
        createSampleActivities(cowSampleIds, pastureIds)
        createSampleNotes()
        
        insertOrUpdateSetting(
            Settings(
                SettingsKeys.SAMPLE_DATA_INSTALLED,
                "true"
            )
        )
    }

    private suspend fun createSamplePastures(): List<String> {
        val pastures = listOf(
            Pasture(id = "sample-pasture-1", name = "North Field", description = "Large pasture with good grass coverage", sizeAcres = 25.5),
            Pasture(id = "sample-pasture-2", name = "South Meadow", description = "Rolling hills with creek access", sizeAcres = 18.0),
            Pasture(id = "sample-pasture-3", name = "East Paddock", description = "Smaller paddock for breeding stock", sizeAcres = 8.5),
            Pasture(id = "sample-pasture-4", name = "West Lot", description = "Holding area near barn", sizeAcres = 5.0)
        )
        pastures.forEach { pasture ->
            val existing = getPastureByIdSuspend(pasture.id)
            if (existing == null) {
                insertPasture(pasture)
            }
        }
        return pastures.map { it.id }
    }

    private suspend fun createSampleCows(pastureIds: List<String>): List<Long> {
        val baseDate = LocalDate.now()
        val sampleUuids = listOf(
            "sample-cow-001", "sample-cow-002", "sample-cow-003", "sample-cow-004", "sample-cow-005", "sample-cow-006",
            "sample-cow-007", "sample-cow-008", "sample-cow-009", "sample-cow-010", "sample-cow-011", "sample-cow-012",
            "sample-cow-013", "sample-cow-014", "sample-cow-015", "sample-cow-016", "sample-cow-017", "sample-cow-018",
            "sample-cow-019", "sample-cow-020", "sample-cow-021", "sample-cow-022"
        )
        
        val cows = listOf(
            // Generation 1 - Foundation Bulls
            Cow(firestoreId = sampleUuids[0], name = "Thunder", tagNumber = "B001", tagColor = "Blue", birthDate = baseDate.minusYears(8), gender = Gender.MALE, classification = Classification.BULL, colorMarkings = "Black Angus with white face", pastureId = pastureIds[2], status = Status.ACTIVE),
            Cow(firestoreId = sampleUuids[1], name = "Storm", tagNumber = "B002", tagColor = "Red", birthDate = baseDate.minusYears(6), gender = Gender.MALE, classification = Classification.BULL, colorMarkings = "Red Angus solid", pastureId = pastureIds[2], status = Status.ACTIVE),
            
            // Generation 1 - Foundation Cows
            Cow(firestoreId = sampleUuids[2], name = "Bessie", tagNumber = "C001", tagColor = "Yellow", birthDate = baseDate.minusYears(9), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Holstein black and white", pastureId = pastureIds[0], status = Status.ACTIVE, isWatched = true),
            Cow(firestoreId = sampleUuids[3], name = "Daisy", tagNumber = "C002", tagColor = "Green", birthDate = baseDate.minusYears(8), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Brown Jersey", pastureId = pastureIds[0], status = Status.ACTIVE),
            Cow(firestoreId = sampleUuids[4], name = "Rosie", tagNumber = "C003", tagColor = "Orange", birthDate = baseDate.minusYears(7), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Black Angus", pastureId = pastureIds[1], status = Status.ACTIVE, isWatched = true),
            Cow(firestoreId = sampleUuids[5], name = "Pearl", tagNumber = "C004", tagColor = "White", birthDate = baseDate.minusYears(7), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "White with black spots", pastureId = pastureIds[0], status = Status.ACTIVE, isWatched = true),
            
            // Generation 2 - Daughters of foundation stock
            Cow(firestoreId = sampleUuids[6], name = "Luna", tagNumber = "C005", tagColor = "Blue", birthDate = baseDate.minusYears(4), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Red with white markings", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 3, fatherId = 1),
            Cow(firestoreId = sampleUuids[7], name = "Star", tagNumber = "C006", tagColor = "Green", birthDate = baseDate.minusYears(3), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Brown with white face", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 4, fatherId = 2),
            Cow(firestoreId = sampleUuids[8], name = "Grace", tagNumber = "C007", tagColor = "Yellow", birthDate = baseDate.minusYears(3), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Black with white stripe", pastureId = pastureIds[0], status = Status.ACTIVE, motherId = 5, fatherId = 1),
            Cow(firestoreId = sampleUuids[9], name = "Ruby", tagNumber = "C008", tagColor = "Red", birthDate = baseDate.minusYears(5), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Solid red", pastureId = pastureIds[1], status = Status.DECEASED, motherId = 6, fatherId = 1),
            
            // Generation 2 - Young breeding stock
            Cow(firestoreId = sampleUuids[10], name = "Hope", tagNumber = "H001", tagColor = "Orange", birthDate = baseDate.minusYears(2), gender = Gender.FEMALE, classification = Classification.HEIFER, colorMarkings = "Brown with white face", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 3, fatherId = 2),
            Cow(firestoreId = sampleUuids[11], name = "Faith", tagNumber = "H002", tagColor = "White", birthDate = baseDate.minusYears(2), gender = Gender.FEMALE, classification = Classification.HEIFER, colorMarkings = "Red and white spotted", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 4, fatherId = 1),
            Cow(firestoreId = sampleUuids[12], name = "Joy", tagNumber = "H003", tagColor = "Blue", birthDate = baseDate.minusMonths(20), gender = Gender.FEMALE, classification = Classification.HEIFER, colorMarkings = "Black Angus solid", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 5, fatherId = 2),
            
            // Generation 2 - Steers (castrated males)
            Cow(firestoreId = sampleUuids[13], name = "Max", tagNumber = "S001", tagColor = "Green", birthDate = baseDate.minusYears(2), gender = Gender.MALE, classification = Classification.STEER, colorMarkings = "Black with white stripe", pastureId = pastureIds[3], status = Status.ACTIVE, motherId = 3, fatherId = 1),
            Cow(firestoreId = sampleUuids[14], name = "Duke", tagNumber = "S002", tagColor = "Yellow", birthDate = baseDate.minusYears(2), gender = Gender.MALE, classification = Classification.STEER, colorMarkings = "Red with white markings", pastureId = pastureIds[3], status = Status.ACTIVE, motherId = 4, fatherId = 2),
            Cow(firestoreId = sampleUuids[15], name = "Rex", tagNumber = "S003", tagColor = "Red", birthDate = baseDate.minusMonths(18), gender = Gender.MALE, classification = Classification.STEER, colorMarkings = "Brown and white", pastureId = pastureIds[3], status = Status.SOLD, motherId = 5, fatherId = 1),
            
            // Generation 3 - Current calves
            Cow(firestoreId = sampleUuids[16], name = "Buddy", tagNumber = "K001", tagColor = "Orange", birthDate = baseDate.minusMonths(8), gender = Gender.MALE, classification = Classification.CALF, colorMarkings = "Brown and white spotted", pastureId = pastureIds[0], status = Status.ACTIVE, motherId = 7, fatherId = 2),
            Cow(firestoreId = sampleUuids[17], name = "Bella", tagNumber = "K002", tagColor = "White", birthDate = baseDate.minusMonths(10), gender = Gender.FEMALE, classification = Classification.CALF, colorMarkings = "Solid black", pastureId = pastureIds[0], status = Status.ACTIVE, motherId = 8, fatherId = 1),
            Cow(firestoreId = sampleUuids[18], name = "Charlie", tagNumber = "K003", tagColor = "Blue", birthDate = baseDate.minusMonths(6), gender = Gender.MALE, classification = Classification.CALF, colorMarkings = "Red with white face", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 9, fatherId = 2),
            Cow(firestoreId = sampleUuids[19], name = "Rosebud", tagNumber = "K004", tagColor = "Green", birthDate = baseDate.minusMonths(4), gender = Gender.FEMALE, classification = Classification.CALF, colorMarkings = "Brown Jersey coloring", pastureId = pastureIds[0], status = Status.ACTIVE, motherId = 11, fatherId = 2),
            Cow(firestoreId = sampleUuids[20], name = "Scout", tagNumber = "K005", tagColor = "Yellow", birthDate = baseDate.minusMonths(3), gender = Gender.MALE, classification = Classification.CALF, colorMarkings = "Black with white markings", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 12, fatherId = 1),
            Cow(firestoreId = sampleUuids[21], name = "Spirit", tagNumber = "K006", tagColor = "Red", birthDate = baseDate.minusMonths(2), gender = Gender.FEMALE, classification = Classification.CALF, colorMarkings = "White with black spots", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 13, fatherId = 2)
        )
        
        val createdCowIds = mutableListOf<Long>()
        cows.forEach { cow ->
            val existing = cow.firestoreId?.let { getCowByFirestoreId(it) }
            val id = if (existing != null) {
                updateCow(cow.copy(id = existing.id, createdAt = existing.createdAt, updatedAt = LocalDate.now()))
                existing.id
            } else {
                insertCow(cow)
            }
            createdCowIds.add(id)
        }
        
        for (i in cows.indices) {
            val cow = cows[i]
            val cowId = createdCowIds[i]
            var needsUpdate = false
            var updatedCow = getCowById(cowId)
            if (cow.motherId != null && cow.motherId!! > 0 && cow.motherId!! <= createdCowIds.size) {
                val actualMotherId = createdCowIds[cow.motherId!!.toInt() - 1]
                updatedCow = updatedCow?.copy(motherId = actualMotherId)
                needsUpdate = true
            }
            if (cow.fatherId != null && cow.fatherId!! > 0 && cow.fatherId!! <= createdCowIds.size) {
                val actualFatherId = createdCowIds[cow.fatherId!!.toInt() - 1]
                updatedCow = updatedCow?.copy(fatherId = actualFatherId)
                needsUpdate = true
            }
            if (needsUpdate && updatedCow != null) {
                updateCow(updatedCow)
            }
        }
        return createdCowIds
    }

    private suspend fun createSampleActivities(cowIds: List<Long>, pastureIds: List<String>) {
        val baseDate = LocalDate.now()
        val activities = mutableListOf<Activity>()

        val thunderId = cowIds[0]
        val stormId = cowIds[1]
        val bessieId = cowIds[2]
        val daisyId = cowIds[3]
        val rosieId = cowIds[4]
        val lunaId = cowIds[6]
        val starId = cowIds[7]
        val graceId = cowIds[8]
        val rubyId = cowIds[9]
        val hopeId = cowIds[10]
        val faithId = cowIds[11]
        val maxId = cowIds[13]
        val dukeId = cowIds[14]
        val rexId = cowIds[15]
        val buddyId = cowIds[16]
        val bellaId = cowIds[17]
        val charlieId = cowIds[18]
        val rosebudId = cowIds[19]
        val scoutId = cowIds[20]
        val spiritId = cowIds[21]

        // MOVED (multiple animals)
        val movedCowIds = listOf(thunderId, stormId)
        val moveGroupId = UUID.randomUUID().toString()
        movedCowIds.forEach { cowId ->
            activities.add(
                Activity(
                    cowId = cowId,
                    date = baseDate.minusMonths(3),
                    activityType = ActivityType.MOVED,
                    fromPastureId = pastureIds.getOrNull(1),
                    toPastureId = pastureIds.getOrNull(0),
                    notes = "Sample bulk move",
                    groupId = moveGroupId,
                    cowIds = movedCowIds
                )
            )
        }

        // WEANED (single animal)
        activities.add(
            Activity(
                cowId = buddyId,
                date = baseDate.minusMonths(7),
                activityType = ActivityType.WEANED,
                notes = "Buddy weaned from Luna and transitioned to pasture diet.",
                details = "Weight at weaning: 420 lbs",
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(buddyId)
            )
        )

        // SOLD (single animal)
        activities.add(
            Activity(
                cowId = rexId,
                date = baseDate.minusMonths(5),
                activityType = ActivityType.SOLD,
                notes = "Rex sold at spring livestock auction.",
                details = "Sale price: $1,150",
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(rexId)
            )
        )

        // DECEASED (single animal)
        activities.add(
            Activity(
                cowId = rubyId,
                date = baseDate.minusMonths(12),
                activityType = ActivityType.DECEASED,
                notes = "Ruby found deceased due to suspected bloat.",
                details = "Vet necropsy confirmed digestive upset.",
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(rubyId)
            )
        )

        // WORKED (single animal)
        activities.add(
            Activity(
                cowId = maxId,
                date = baseDate.minusMonths(6),
                activityType = ActivityType.WORKED,
                notes = "Max ran through handling facility for annual boosters.",
                details = "Hoof trimming and fly treatment applied.",
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(maxId)
            )
        )

        // CASTRATED (single animal)
        activities.add(
            Activity(
                cowId = dukeId,
                date = baseDate.minusMonths(14),
                activityType = ActivityType.CASTRATED,
                notes = "Duke castrated to finish as a steer.",
                details = "Banding method with tetanus booster.",
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(dukeId)
            )
        )

        // BRED (multiple animals)
        val bredCowIds = listOf(bessieId, daisyId)
        val bredGroupId = UUID.randomUUID().toString()
        bredCowIds.forEach { cowId ->
            activities.add(
                Activity(
                    cowId = cowId,
                    date = baseDate.minusMonths(10),
                    activityType = ActivityType.BRED,
                    notes = "Synchronized AI breeding with sire Thunder.",
                    details = "Breeding technician: Sarah Lopez",
                    groupId = bredGroupId,
                    cowIds = bredCowIds
                )
            )
        }

        // CALVED (single animal with calf association)
        activities.add(
            Activity(
                cowId = lunaId,
                date = baseDate.minusMonths(8),
                activityType = ActivityType.CALVED,
                notes = "Luna calved a healthy bull calf (Buddy).",
                details = "Calf weight: 85 lbs, unassisted delivery.",
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(lunaId, buddyId)
            )
        )
        activities.add(
            Activity(
                cowId = starId,
                date = baseDate.minusMonths(9),
                activityType = ActivityType.CALVED,
                notes = "Star delivered a vigorous heifer calf (Bella).",
                details = "Assisted delivery with quick recovery.",
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(starId, bellaId)
            )
        )

        // VACCINATED (multiple animals)
        val vaccinationCowIds = listOf(buddyId, bellaId, charlieId)
        val vaccinationGroupId = UUID.randomUUID().toString()
        vaccinationCowIds.forEach { cowId ->
            activities.add(
                Activity(
                    cowId = cowId,
                    date = baseDate.minusMonths(2),
                    activityType = ActivityType.VACCINATED,
                    notes = "Spring respiratory vaccination clinic.",
                    details = "Administered Bovishield Gold FP5 vaccine.",
                    groupId = vaccinationGroupId,
                    technician = "Dr. Johnson",
                    cost = 45.0,
                    cowIds = vaccinationCowIds
                )
            )
        }

        // TREATED (single animal)
        activities.add(
            Activity(
                cowId = rosieId,
                date = baseDate.minusMonths(4),
                activityType = ActivityType.TREATED,
                notes = "Rosie treated for mild mastitis in right quarter.",
                details = "Intramammary antibiotic therapy for three days.",
                technician = "Dr. Johnson",
                cost = 95.0,
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(rosieId)
            )
        )

        // WEIGHED (single animal)
        activities.add(
            Activity(
                cowId = maxId,
                date = baseDate.minusMonths(1),
                activityType = ActivityType.WEIGHED,
                notes = "Mid-summer weight check for Max.",
                details = "Scale weight: 1,280 lbs.",
                quantity = 1280.0,
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(maxId)
            )
        )

        // PURCHASED (single animal)
        activities.add(
            Activity(
                cowId = stormId,
                date = baseDate.minusYears(2),
                activityType = ActivityType.PURCHASED,
                notes = "Storm purchased from Red River Genetics.",
                details = "2-year-old Red Angus bull with proven calving ease.",
                cost = 3200.0,
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(stormId)
            )
        )

        // HEALTH CHECK (multiple animals)
        val healthCheckCowIds = listOf(hopeId, faithId)
        val healthCheckGroupId = UUID.randomUUID().toString()
        healthCheckCowIds.forEach { cowId ->
            activities.add(
                Activity(
                    cowId = cowId,
                    date = baseDate.minusMonths(3),
                    activityType = ActivityType.HEALTH_CHECK,
                    notes = "Annual reproductive soundness exams.",
                    details = "Both heifers cleared for breeding season.",
                    groupId = healthCheckGroupId,
                    technician = "Dr. Johnson",
                    cost = 60.0,
                    cowIds = healthCheckCowIds
                )
            )
        }

        // TAGGED (multiple animals)
        val taggedCowIds = listOf(rosebudId, scoutId, spiritId)
        val taggingGroupId = UUID.randomUUID().toString()
        taggedCowIds.forEach { cowId ->
            activities.add(
                Activity(
                    cowId = cowId,
                    date = baseDate.minusMonths(2),
                    activityType = ActivityType.TAGGED,
                    notes = "Tagged spring calves with new color-coded ear tags.",
                    details = "Applied RFID and visual ID tags.",
                    groupId = taggingGroupId,
                    cowIds = taggedCowIds
                )
            )
        }

        // NOTE (single animal)
        activities.add(
            Activity(
                cowId = bessieId,
                date = baseDate.minusWeeks(2),
                activityType = ActivityType.NOTE,
                notes = "Observation: Bessie showing excellent maternal behavior.",
                details = "Keeping calf close and maintaining body condition score 6.",
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(bessieId)
            )
        )

        // OTHER (single animal)
        activities.add(
            Activity(
                cowId = graceId,
                date = baseDate.minusWeeks(6),
                activityType = ActivityType.OTHER,
                notes = "Training session to halter break Grace for county fair.",
                details = "Practiced leading and setting up for showmanship.",
                groupId = UUID.randomUUID().toString(),
                cowIds = listOf(graceId)
            )
        )

        activities.forEach { activity ->
            insertActivity(activity)
        }
    }

    private suspend fun createSampleNotes() {
        noteDao?.let { dao ->
            val notes = listOf(
                Note(title = "Pasture Rotation Plan", text = "Plan to rotate cattle between North Field and South Meadow every 3 months to prevent overgrazing and maintain grass health.", timestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)),
                Note(title = "Breeding Schedule", text = "Thunder will be bred with Bessie and Daisy in spring. Storm will be bred with Rosie and Luna. Expecting calves in late winter/early spring.", timestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)),
                Note(title = "Feed Inventory", text = "Current hay supply should now last through winter. Need to order mineral supplements and check salt lick supplies in West Lot.", timestamp = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000)),
                Note(title = "Veterinary Visit", text = "Dr. Johnson scheduled for next month to do annual health checks and vaccinations for the entire herd. Need to prepare holding area.", timestamp = System.currentTimeMillis() - (12 * 60 * 60 * 1000))
            )
            notes.forEach { note -> dao.insert(note) }
        }
    }

    suspend fun deleteSampleData() {
        if (!isSampleDataInstalled()) return
        
        // Delete sample pastures
        val samplePastureIds = listOf("sample-pasture-1", "sample-pasture-2", "sample-pasture-3", "sample-pasture-4")
        samplePastureIds.forEach { pastureId ->
            getPastureByIdSuspend(pastureId)?.let { pasture -> deletePasture(pasture) }
        }
        
        // Delete sample cows by UUID
        val allCows = getAllCows().firstOrNull() ?: emptyList()
        allCows.filter { cow -> cow.firestoreId?.startsWith("sample-cow-") == true }
            .forEach { cow -> deleteCow(cow) }
        
        // Delete sample notes
        noteDao?.let { dao ->
            val sampleNoteTitles = listOf("Pasture Rotation Plan", "Breeding Schedule", "Feed Inventory", "Veterinary Visit")
            val allNotes = dao.getAllNotes().firstOrNull() ?: emptyList()
            allNotes.filter { note -> sampleNoteTitles.contains(note.title) }.forEach { note -> dao.delete(note) }
        }
        
        insertOrUpdateSetting(Settings(SettingsKeys.SAMPLE_DATA_INSTALLED, "false"))
    }

    suspend fun deleteAllData() {
        cowDao.deleteAllCows()
        pastureDao.deleteAllPastures()
        activityDao.deleteAllActivities()
        noteDao?.deleteAllNotes()
        insertOrUpdateSetting(Settings(SettingsKeys.SAMPLE_DATA_INSTALLED, "false"))
        initializeDefaultData()
    }
    
    suspend fun deleteAllCows() = cowDao.deleteAllCows()
    suspend fun deleteAllPastures() = pastureDao.deleteAllPastures()
    suspend fun deleteAllActivities() = activityDao.deleteAllActivities()
    suspend fun deleteAllNotes() = noteDao?.deleteAllNotes()
    suspend fun deleteAllTagColors() {
        tagColorDao?.let { dao ->
            val now = System.currentTimeMillis()
            val all = dao.getAllTagColorsSync()
            if (all.isNotEmpty()) {
                val markedDeleted = all.map { tagColor ->
                    tagColor.copy(
                        firestoreId = tagColor.firestoreId ?: tagColor.id,
                        isDeleted = true,
                        isActive = false,
                        updatedAt = now
                    )
                }
                dao.upsertAll(markedDeleted)
            }
        }
    }
    suspend fun deleteAllActivityTypeConfigs() { activityTypeConfigDao?.deleteAllActivityTypes() }
    suspend fun deleteAllBreeds() = breedDao?.deleteAllBreeds()
    suspend fun deleteAllSettings() { settingsDao?.let { dao -> val settings = dao.getAllSettings().first(); for (s in settings) dao.deleteSetting(s) } }

    // User operations
    suspend fun insertUser(user: User) = userDao?.insertUser(user)
    suspend fun getUserById(uid: String): User? = userDao?.getUserById(uid)
    fun getUserByIdFlow(uid: String): Flow<User?> = userDao?.getUserByIdFlow(uid) ?: kotlinx.coroutines.flow.flowOf(null)
    fun getAllUsers(): Flow<List<User>> = userDao?.getAllUsers() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun updateUser(user: User) = userDao?.updateUser(user)
    suspend fun deleteUser(user: User) = userDao?.deleteUser(user)

    // Herd operations
    suspend fun insertHerd(herd: Herd) = herdDao?.insertHerd(herd)
    suspend fun getHerdById(id: String): Herd? = herdDao?.getHerdById(id)
    fun getHerdByIdFlow(id: String): Flow<Herd?> = herdDao?.getHerdByIdFlow(id) ?: kotlinx.coroutines.flow.flowOf(null)
    fun getAllActiveHerds(): Flow<List<Herd>> = herdDao?.getAllActiveHerds() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    fun getHerdsByOwner(ownerId: String): Flow<List<Herd>> = herdDao?.getHerdsByOwner(ownerId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun updateHerd(herd: Herd) = herdDao?.updateHerd(herd)
    suspend fun deleteHerd(herd: Herd) = herdDao?.deleteHerd(herd)

    // Herd member operations
    suspend fun insertHerdMember(member: HerdMember) = herdMemberDao?.insertMember(member)
    fun getMembersByHerd(herdId: String): Flow<List<HerdMember>> = herdMemberDao?.getMembersByHerd(herdId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    fun getHerdsByUser(userId: String): Flow<List<HerdMember>> = herdMemberDao?.getHerdsByUser(userId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun getMembership(herdId: String, userId: String): HerdMember? = herdMemberDao?.getMembership(herdId, userId)
    suspend fun removeMember(herdId: String, userId: String) = herdMemberDao?.removeMember(herdId, userId)

    // Simplified sync methods for single user
    suspend fun getAllCowsSync(): List<Cow> { return try { getAllCows().first() } catch (e: Exception) { emptyList() } }
    suspend fun getAllActivitiesSync(): List<Activity> { return try { getAllActivities().first() } catch (e: Exception) { emptyList() } }
    suspend fun getAllPasturesSync(): List<Pasture> { return try { getAllPastures().first() } catch (e: Exception) { emptyList() } }
    suspend fun getAllNotesSync(): List<Note> { return try { noteDao?.getAllNotes()?.first() ?: emptyList() } catch (e: Exception) { emptyList() } }
    
    suspend fun insertNote(note: Note) = noteDao?.insert(note)
    suspend fun updateNote(note: Note) = noteDao?.update(note)
    
    // Tag Color operations
    fun getAllTagColors(): Flow<List<TagColor>> = tagColorDao?.getAllTagColors() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    fun getAllActiveTagColors(): Flow<List<TagColor>> = tagColorDao?.getAllActiveTagColors() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun getAllTagColorsSync(): List<TagColor> = tagColorDao?.getAllTagColorsSync() ?: emptyList()
    suspend fun getTagColorById(id: String): TagColor? = tagColorDao?.getTagColorById(id)
    suspend fun getTagColorByName(name: String): TagColor? = tagColorDao?.getTagColorByName(name)
    suspend fun insertTagColor(tagColor: TagColor): Long = tagColorDao?.insertTagColor(tagColor) ?: -1
    suspend fun insertTagColors(tagColors: List<TagColor>) = tagColorDao?.insertTagColors(tagColors)
    suspend fun upsertTagColor(tagColor: TagColor) = tagColorDao?.upsert(tagColor)
    suspend fun upsertTagColors(tagColors: List<TagColor>) = tagColorDao?.upsertAll(tagColors)
    suspend fun updateTagColor(tagColor: TagColor) = tagColorDao?.updateTagColor(tagColor)
    suspend fun deleteTagColor(tagColor: TagColor) {
        tagColorDao?.upsert(
            tagColor.copy(
                firestoreId = tagColor.firestoreId ?: tagColor.id,
                isDeleted = true,
                isActive = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
    suspend fun updateTagColorActiveStatus(id: String, isActive: Boolean) = tagColorDao?.updateTagColorActiveStatus(id, isActive)
    suspend fun ensureDefaultTagColorsExist() {
        val existing = tagColorDao?.getAllTagColorsSync() ?: emptyList()
        val activeNames = existing.filter { !it.isDeleted }.map { it.name.lowercase() }.toSet()
        val defaults = TagColor.getDefaultColors()
        val missing = defaults.filter { it.name.lowercase() !in activeNames }
        if (missing.isNotEmpty()) { upsertTagColors(missing) }
    }
    
    // Activity Type Config operations
    fun getAllActivityTypes(): Flow<List<ActivityTypeConfig>> = activityTypeConfigDao?.getAllActivityTypes() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    fun getAllActiveActivityTypes(): Flow<List<ActivityTypeConfig>> = activityTypeConfigDao?.getAllActiveActivityTypes() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun getAllActivityTypesSync(): List<ActivityTypeConfig> = activityTypeConfigDao?.getAllActivityTypesSync() ?: emptyList()
    suspend fun getActivityTypeById(id: String): ActivityTypeConfig? = activityTypeConfigDao?.getActivityTypeById(id)
    suspend fun getActivityTypeByName(name: String): ActivityTypeConfig? = activityTypeConfigDao?.getActivityTypeByName(name)
    suspend fun insertActivityType(activityType: ActivityTypeConfig): Long = activityTypeConfigDao?.insertActivityType(activityType) ?: -1
    suspend fun insertActivityTypes(activityTypes: List<ActivityTypeConfig>) = activityTypeConfigDao?.insertActivityTypes(activityTypes)
    suspend fun upsertActivityType(activityType: ActivityTypeConfig) = activityTypeConfigDao?.upsert(activityType)
    suspend fun upsertActivityTypes(activityTypes: List<ActivityTypeConfig>) = activityTypeConfigDao?.upsertAll(activityTypes)
    suspend fun updateActivityType(activityType: ActivityTypeConfig) = activityTypeConfigDao?.updateActivityType(activityType)
    suspend fun deleteActivityType(activityType: ActivityTypeConfig) = activityTypeConfigDao?.deleteActivityType(activityType)
    suspend fun updateActivityTypeActiveStatus(id: String, isActive: Boolean) = activityTypeConfigDao?.updateActivityTypeActiveStatus(id, isActive)
    suspend fun ensureDefaultActivityTypesExist() {
        val existing = activityTypeConfigDao?.getAllActivityTypesSync() ?: emptyList()
        val existingNames = existing.map { it.name.lowercase() }.toSet()
        val defaults = ActivityTypeConfig.getDefaultActivityTypes()
        val missing = defaults.filter { it.name.lowercase() !in existingNames }
        if (missing.isNotEmpty()) { insertActivityTypes(missing) }
    }
    
    suspend fun restoreDefaultActivityTypes() {
        activityTypeConfigDao?.deleteAllActivityTypes()
        insertActivityTypes(ActivityTypeConfig.getDefaultActivityTypes())
    }
    
    // Breed operations
    fun getAllBreeds(): Flow<List<Breed>> = breedDao?.getAllBreeds() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun getBreedById(id: String): Breed? = breedDao?.getBreedById(id)
    suspend fun insertBreed(breed: Breed) = breedDao?.insertBreed(breed)
    suspend fun insertBreeds(breeds: List<Breed>) = breedDao?.insertBreeds(breeds)
    suspend fun updateBreed(breed: Breed) = breedDao?.updateBreed(breed)
    suspend fun deleteBreed(breed: Breed) = breedDao?.deleteBreed(breed)
    suspend fun restoreDefaultBreeds() {
        breedDao?.deleteAllBreeds()
        insertBreeds(Breed.getDefaultBreeds())
    }
}

private const val DEFAULT_RECENT_SIRES_LIMIT = 5
