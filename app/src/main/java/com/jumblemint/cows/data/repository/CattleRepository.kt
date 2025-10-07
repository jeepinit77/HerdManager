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
import java.time.LocalDate
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
    fun getActiveFemales(): Flow<List<Cow>> = cowDao.getActiveFemales()
    fun getActiveMales(): Flow<List<Cow>> = cowDao.getActiveMales()
    fun getCalvesByMother(motherId: Long): Flow<List<Cow>> = cowDao.getCalvesByMother(motherId)
    fun getCalvesByFather(fatherId: Long): Flow<List<Cow>> = cowDao.getCalvesByFather(fatherId)
    fun getCowsByIds(ids: List<Long>): Flow<List<Cow>> = cowDao.getCowsByIds(ids)

    fun getMaternalSiblings(cowId: Long, motherId: Long): Flow<List<Cow>> = cowDao.getMaternalSiblings(cowId, motherId)
    fun getPaternalSiblings(cowId: Long, fatherId: Long): Flow<List<Cow>> = cowDao.getPaternalSiblings(cowId, fatherId)

    suspend fun getCowById(id: Long): Cow? = cowDao.getCowById(id)
    fun getCowByIdFlow(id: Long): Flow<Cow?> = cowDao.getCowByIdFlow(id)
    suspend fun getCowByTagNumber(tagNumber: String): Cow? = cowDao.getCowByTagNumber(tagNumber)
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
        // Birth activity is typically for the calf, cowIds would be the calf's ID
        // If you also want to associate this with the mother, you'd handle that separately or adjust Activity model
        val birthActivity = Activity(
            cowId = calfId, // The subject of the BIRTH activity is the calf
            date = birthDate,
            activityType = ActivityType.BIRTH,
            notes = "Born to mother ID: $motherId" + (fatherId?.let { ", father ID: $it" } ?: ""),
            cowIds = listOf(calfId) // The BIRTH activity primarily concerns the calf itself
            // If you want to link it to mother/father in Activity.cowIds, add them: listOf(calfId, motherId)
        )
        insertActivity(birthActivity)
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
        
        if (getSettingByKey(SettingsKeys.TAG_COLORS) == null) {
            insertOrUpdateSetting(
                Settings(
                    SettingsKeys.TAG_COLORS,
                    "Blue,Green,Orange,Yellow,Red,White"
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

    suspend fun isSampleDataInstalled(): Boolean {
        return getSettingByKey(SettingsKeys.SAMPLE_DATA_INSTALLED)?.value == "true"
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
        pastures.forEach { insertPasture(it) }
        return pastures.map { it.id }
    }

    private suspend fun createSampleCows(pastureIds: List<String>): List<Long> {
        val baseDate = LocalDate.now()
        val cows = listOf(
            // Generation 1 - Foundation Bulls
            Cow(name = "Thunder", tagNumber = "B001", tagColor = "Blue", birthDate = baseDate.minusYears(8), gender = Gender.MALE, classification = Classification.BULL, colorMarkings = "Black Angus with white face", pastureId = pastureIds[2], status = Status.ACTIVE),
            Cow(name = "Storm", tagNumber = "B002", tagColor = "Red", birthDate = baseDate.minusYears(6), gender = Gender.MALE, classification = Classification.BULL, colorMarkings = "Red Angus solid", pastureId = pastureIds[2], status = Status.ACTIVE),
            
            // Generation 1 - Foundation Cows
            Cow(name = "Bessie", tagNumber = "C001", tagColor = "Yellow", birthDate = baseDate.minusYears(9), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Holstein black and white", pastureId = pastureIds[0], status = Status.ACTIVE, isWatched = true),
            Cow(name = "Daisy", tagNumber = "C002", tagColor = "Green", birthDate = baseDate.minusYears(8), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Brown Jersey", pastureId = pastureIds[0], status = Status.ACTIVE),
            Cow(name = "Rosie", tagNumber = "C003", tagColor = "Orange", birthDate = baseDate.minusYears(7), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Black Angus", pastureId = pastureIds[1], status = Status.ACTIVE, isWatched = true),
            Cow(name = "Pearl", tagNumber = "C004", tagColor = "White", birthDate = baseDate.minusYears(7), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "White with black spots", pastureId = pastureIds[0], status = Status.ACTIVE, isWatched = true),
            
            // Generation 2 - Daughters of foundation stock
            Cow(name = "Luna", tagNumber = "C005", tagColor = "Blue", birthDate = baseDate.minusYears(4), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Red with white markings", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 3, fatherId = 1),
            Cow(name = "Star", tagNumber = "C006", tagColor = "Green", birthDate = baseDate.minusYears(3), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Brown with white face", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 4, fatherId = 2),
            Cow(name = "Grace", tagNumber = "C007", tagColor = "Yellow", birthDate = baseDate.minusYears(3), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Black with white stripe", pastureId = pastureIds[0], status = Status.ACTIVE, motherId = 5, fatherId = 1),
            Cow(name = "Ruby", tagNumber = "C008", tagColor = "Red", birthDate = baseDate.minusYears(5), gender = Gender.FEMALE, classification = Classification.COW, colorMarkings = "Solid red", pastureId = pastureIds[1], status = Status.DECEASED, motherId = 6, fatherId = 1),
            
            // Generation 2 - Young breeding stock
            Cow(name = "Hope", tagNumber = "H001", tagColor = "Orange", birthDate = baseDate.minusYears(2), gender = Gender.FEMALE, classification = Classification.HEIFER, colorMarkings = "Brown with white face", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 3, fatherId = 2),
            Cow(name = "Faith", tagNumber = "H002", tagColor = "White", birthDate = baseDate.minusYears(2), gender = Gender.FEMALE, classification = Classification.HEIFER, colorMarkings = "Red and white spotted", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 4, fatherId = 1),
            Cow(name = "Joy", tagNumber = "H003", tagColor = "Blue", birthDate = baseDate.minusMonths(20), gender = Gender.FEMALE, classification = Classification.HEIFER, colorMarkings = "Black Angus solid", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 5, fatherId = 2),
            
            // Generation 2 - Steers (castrated males)
            Cow(name = "Max", tagNumber = "S001", tagColor = "Green", birthDate = baseDate.minusYears(2), gender = Gender.MALE, classification = Classification.STEER, colorMarkings = "Black with white stripe", pastureId = pastureIds[3], status = Status.ACTIVE, motherId = 3, fatherId = 1),
            Cow(name = "Duke", tagNumber = "S002", tagColor = "Yellow", birthDate = baseDate.minusYears(2), gender = Gender.MALE, classification = Classification.STEER, colorMarkings = "Red with white markings", pastureId = pastureIds[3], status = Status.ACTIVE, motherId = 4, fatherId = 2),
            Cow(name = "Rex", tagNumber = "S003", tagColor = "Red", birthDate = baseDate.minusMonths(18), gender = Gender.MALE, classification = Classification.STEER, colorMarkings = "Brown and white", pastureId = pastureIds[3], status = Status.SOLD, motherId = 5, fatherId = 1),
            
            // Generation 3 - Current calves
            Cow(name = "Buddy", tagNumber = "K001", tagColor = "Orange", birthDate = baseDate.minusMonths(8), gender = Gender.MALE, classification = Classification.CALF, colorMarkings = "Brown and white spotted", pastureId = pastureIds[0], status = Status.ACTIVE, motherId = 7, fatherId = 2),
            Cow(name = "Bella", tagNumber = "K002", tagColor = "White", birthDate = baseDate.minusMonths(10), gender = Gender.FEMALE, classification = Classification.CALF, colorMarkings = "Solid black", pastureId = pastureIds[0], status = Status.ACTIVE, motherId = 8, fatherId = 1),
            Cow(name = "Charlie", tagNumber = "K003", tagColor = "Blue", birthDate = baseDate.minusMonths(6), gender = Gender.MALE, classification = Classification.CALF, colorMarkings = "Red with white face", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 9, fatherId = 2),
            Cow(name = "Rosebud", tagNumber = "K004", tagColor = "Green", birthDate = baseDate.minusMonths(4), gender = Gender.FEMALE, classification = Classification.CALF, colorMarkings = "Brown Jersey coloring", pastureId = pastureIds[0], status = Status.ACTIVE, motherId = 11, fatherId = 2),
            Cow(name = "Scout", tagNumber = "K005", tagColor = "Yellow", birthDate = baseDate.minusMonths(3), gender = Gender.MALE, classification = Classification.CALF, colorMarkings = "Black with white markings", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 12, fatherId = 1),
            Cow(name = "Spirit", tagNumber = "K006", tagColor = "Red", birthDate = baseDate.minusMonths(2), gender = Gender.FEMALE, classification = Classification.CALF, colorMarkings = "White with black spots", pastureId = pastureIds[1], status = Status.ACTIVE, motherId = 13, fatherId = 2)
        )
        
        val createdCowIds = mutableListOf<Long>()
        cows.forEach { cow ->
            val id = insertCow(cow)
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
        // Example for a bulk activity where all these cows are involved together
        val involvedInSampleMove = cowIds.take(2) // e.g., Thunder and Storm moved together
        val sampleMoveGroupId = UUID.randomUUID().toString()

        involvedInSampleMove.forEach { cowId ->
            activities.add(Activity(
                cowId = cowId, // Specific cow for this record
                date = baseDate.minusMonths(3),
                activityType = ActivityType.MOVED,
                fromPastureId = pastureIds.getOrNull(1),
                toPastureId = pastureIds.getOrNull(0),
                notes = "Sample bulk move",
                groupId = sampleMoveGroupId,
                cowIds = involvedInSampleMove // ALL cows involved in this specific move
            ))
        }

        // Single animal activities
        activities.add(Activity(cowId = cowIds[19], date = baseDate.minusMonths(9), activityType = ActivityType.BIRTH, notes = "Healthy male calf born to Luna", details = "Birth weight: 85 lbs, no complications", groupId = UUID.randomUUID().toString(), cowIds = listOf(cowIds[19])))
        activities.add(Activity(cowId = cowIds[20], date = baseDate.minusMonths(11), activityType = ActivityType.BIRTH, notes = "Female calf born to Star", details = "Birth weight: 78 lbs, assisted birth", groupId = UUID.randomUUID().toString(), cowIds = listOf(cowIds[20])))
        // ... (add cowIds = listOf(specificCowId) for other single activities)

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
        val samplePastureIds = listOf("sample-pasture-1", "sample-pasture-2", "sample-pasture-3", "sample-pasture-4")
        samplePastureIds.forEach { pastureId ->
            getPastureByIdSuspend(pastureId)?.let { pasture -> deletePasture(pasture) }
        }
        val sampleTagPrefixes = listOf("B00", "C00", "H00", "S00", "K00", "Y00")
        val allCows = getAllCows().firstOrNull() ?: emptyList()
        allCows.filter { cow -> cow.tagNumber?.let { tag -> sampleTagPrefixes.any { prefix -> tag.startsWith(prefix) } } ?: false }
            .forEach { cow -> deleteCow(cow) }
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
    suspend fun deleteAllTagColors() { tagColorDao?.let { dao -> val all = dao.getAllTagColorsSync(); for (tc in all) dao.deleteById(tc.id) } }
    suspend fun deleteAllActivityTypeConfigs() { activityTypeConfigDao?.deleteAllActivityTypes() }
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
    suspend fun deleteTagColor(tagColor: TagColor) = tagColorDao?.deleteTagColor(tagColor)
    suspend fun updateTagColorActiveStatus(id: String, isActive: Boolean) = tagColorDao?.updateTagColorActiveStatus(id, isActive)
    suspend fun ensureDefaultTagColorsExist() {
        val existing = tagColorDao?.getAllTagColorsSync() ?: emptyList()
        val existingNames = existing.map { it.name.lowercase() }.toSet()
        val defaults = TagColor.getDefaultColors()
        val missing = defaults.filter { it.name.lowercase() !in existingNames }
        if (missing.isNotEmpty()) { insertTagColors(missing) }
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
