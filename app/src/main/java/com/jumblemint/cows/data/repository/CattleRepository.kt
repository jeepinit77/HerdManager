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
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.ui.viewmodel.PastureWithCowCount // <<< ADDED IMPORT
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
    private val activityTypeConfigDao: ActivityTypeConfigDao? = null
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
    ): List<Activity> {
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
        
        // Insert activities and get the created activities with their IDs
        val createdActivities = mutableListOf<Activity>()
        activities.forEach { activity ->
            val insertedId = insertActivity(activity)
            createdActivities.add(activity.copy(id = insertedId))
        }
        
        if (activityType == ActivityType.MOVED) {
            toPastureId?.let { pastureId -> // pastureId here is String
                for (cowId in cowIds) {
                    cowDao.updateCowPasture(cowId, pastureId) // updateCowPasture expects String?
                }
            }
        } else if (activityType == ActivityType.CASTRATED) {
            for (cowId in cowIds) {
                val cow = getCowById(cowId)
                if (cow?.classification == Classification.BULL) {
                    cowDao.updateCowClassification(cowId, Classification.STEER.name)
                }
            }
        } else if (activityType == ActivityType.SOLD || activityType == ActivityType.DECEASED) {
            val newStatus = if (activityType == ActivityType.SOLD) Status.SOLD else Status.DECEASED
            for (cowId in cowIds) {
                val cow = getCowById(cowId)
                cow?.let { cowData ->
                    updateCow(cowData.copy(status = newStatus, pastureId = null, updatedAt = LocalDate.now())) // Cows sold/deceased might be removed from pasture
                }
            }
        }
        
        return createdActivities
    }

    // Helper method for editing activities with a specific groupId
    suspend fun createBulkActivityWithGroupId(
        cowIds: List<Long>,
        activityType: ActivityType,
        date: LocalDate = LocalDate.now(),
        notes: String? = null,
        toPastureId: String? = null,
        groupId: String
    ): List<Activity> {
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
        
        // Insert activities and get the created activities with their IDs
        val createdActivities = mutableListOf<Activity>()
        activities.forEach { activity ->
            val insertedId = insertActivity(activity)
            createdActivities.add(activity.copy(id = insertedId))
        }
        
        if (activityType == ActivityType.MOVED) {
            toPastureId?.let { pastureId ->
                for (cowId in cowIds) {
                    cowDao.updateCowPasture(cowId, pastureId)
                }
            }
        } else if (activityType == ActivityType.CASTRATED) {
            for (cowId in cowIds) {
                val cow = getCowById(cowId)
                if (cow?.classification == Classification.BULL) {
                    cowDao.updateCowClassification(cowId, Classification.STEER.name)
                }
            }
        } else if (activityType == ActivityType.SOLD || activityType == ActivityType.DECEASED) {
            val newStatus = if (activityType == ActivityType.SOLD) Status.SOLD else Status.DECEASED
            for (cowId in cowIds) {
                val cow = getCowById(cowId)
                cow?.let { cowData ->
                    updateCow(cowData.copy(status = newStatus, pastureId = null, updatedAt = LocalDate.now()))
                }
            }
        }
        
        return createdActivities
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
                val newClassification: Classification = when (calf.gender) {
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
//        val existingCalfPasture = pastureDao.getAllPastures().firstOrNull()?.find { it.name == "Calf Pasture" }
//        if (existingCalfPasture == null) {
//            val calfPasture = Pasture(
//                id = UUID.randomUUID().toString(), // ID is String, this is good
//                name = "Calf Pasture",
//                description = "Default pasture for calves",
//                sizeAcres = 0.0
//            )
//            insertPasture(calfPasture)
//        }
        
        // Initialize default tag colors if none exist
        if (tagColorDao != null && getAllTagColors().first().isEmpty()) {
            val defaultColors = com.jumblemint.cows.data.model.TagColor.getDefaultColors()
            defaultColors.forEach { tagColor ->
                insertTagColor(tagColor)
            }
        }
        
        // Initialize default activity types if none exist
        if (activityTypeConfigDao != null && getAllActivityTypes().first().isEmpty()) {
            val defaultActivityTypes = com.jumblemint.cows.data.model.ActivityTypeConfig.getDefaultActivityTypes()
            defaultActivityTypes.forEach { activityType ->
                insertActivityType(activityType)
            }
        }
        
        // Keep old settings initialization for backward compatibility (can be removed later)
        if (getSettingByKey(SettingsKeys.TAG_COLORS) == null) {
            insertOrUpdateSetting(
                Settings(
                    SettingsKeys.TAG_COLORS,
                    "Red,Blue,Green,Yellow,Orange,White"
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

    // Sample data management
    suspend fun isSampleDataInstalled(): Boolean {
        return getSettingByKey(SettingsKeys.SAMPLE_DATA_INSTALLED)?.value == "true"
    }

    suspend fun installSampleData() {
        if (isSampleDataInstalled()) return

        // Create sample pastures
        val pastureIds = createSamplePastures()
        
        // Create sample cows
        val cowIds = createSampleCows(pastureIds)
        
        // Create sample activities
        createSampleActivities(cowIds, pastureIds)
        
        // Create sample notes
        createSampleNotes()
        
        // Mark sample data as installed
        insertOrUpdateSetting(
            Settings(
                SettingsKeys.SAMPLE_DATA_INSTALLED,
                "true"
            )
        )
    }

    private suspend fun createSamplePastures(): List<String> {
        val pastures = listOf(
            Pasture(
                id = "sample-pasture-1",
                name = "North Field",
                description = "Large pasture with good grass coverage",
                sizeAcres = 25.5
            ),
            Pasture(
                id = "sample-pasture-2", 
                name = "South Meadow",
                description = "Rolling hills with creek access",
                sizeAcres = 18.0
            ),
            Pasture(
                id = "sample-pasture-3",
                name = "East Paddock",
                description = "Smaller paddock for breeding stock",
                sizeAcres = 8.5
            ),
            Pasture(
                id = "sample-pasture-4",
                name = "West Lot",
                description = "Holding area near barn",
                sizeAcres = 5.0
            )
        )
        
        pastures.forEach { insertPasture(it) }
        return pastures.map { it.id }
    }

    private suspend fun createSampleCows(pastureIds: List<String>): List<Long> {
        val baseDate = LocalDate.now().minusYears(2)
        val cows = listOf(
            // Foundation Bulls (older generation)
            Cow(
                name = "Thunder",
                tagNumber = "B001",
                tagColor = "Red",
                birthDate = baseDate.minusYears(8),
                gender = Gender.MALE,
                classification = Classification.BULL,
                colorMarkings = "Black with white face",
                pastureId = pastureIds[2], // East Paddock
                status = Status.ACTIVE
            ),
            Cow(
                name = "Storm",
                tagNumber = "B002", 
                tagColor = "Blue",
                birthDate = baseDate.minusYears(6),
                gender = Gender.MALE,
                classification = Classification.BULL,
                colorMarkings = "Red Angus",
                pastureId = pastureIds[2], // East Paddock
                status = Status.ACTIVE
            ),
            Cow(
                name = "Titan",
                tagNumber = "B003",
                tagColor = "Silver",
                birthDate = baseDate.minusYears(4),
                gender = Gender.MALE,
                classification = Classification.BULL,
                colorMarkings = "Charolais white",
                pastureId = pastureIds[2], // East Paddock
                status = Status.SOLD // Sold bull to show sold animals
            ),
            
            // Foundation Cows (older generation)
            Cow(
                name = "Bessie",
                tagNumber = "C001",
                tagColor = "Yellow",
                birthDate = baseDate.minusYears(9),
                gender = Gender.FEMALE,
                classification = Classification.COW,
                colorMarkings = "Holstein black and white",
                pastureId = pastureIds[0], // North Field
                status = Status.ACTIVE,
                isWatched = true
            ),
            Cow(
                name = "Daisy",
                tagNumber = "C002",
                tagColor = "Green",
                birthDate = baseDate.minusYears(8),
                gender = Gender.FEMALE,
                classification = Classification.COW,
                colorMarkings = "Brown Jersey",
                pastureId = pastureIds[0], // North Field
                status = Status.ACTIVE
            ),
            Cow(
                name = "Rosie",
                tagNumber = "C003",
                tagColor = "Purple",
                birthDate = baseDate.minusYears(7),
                gender = Gender.FEMALE,
                classification = Classification.COW,
                colorMarkings = "Black Angus",
                pastureId = pastureIds[1], // South Meadow
                status = Status.ACTIVE,
                isWatched = true
            ),
            Cow(
                name = "Molly",
                tagNumber = "C004",
                tagColor = "Gold",
                birthDate = baseDate.minusYears(6),
                gender = Gender.FEMALE,
                classification = Classification.COW,
                colorMarkings = "Red with white face",
                pastureId = pastureIds[0], // North Field
                status = Status.ACTIVE
            ),
            Cow(
                name = "Ruby",
                tagNumber = "C005",
                tagColor = "Maroon",
                birthDate = baseDate.minusYears(5),
                gender = Gender.FEMALE,
                classification = Classification.COW,
                colorMarkings = "Solid red",
                pastureId = pastureIds[1], // South Meadow
                status = Status.DECEASED // Deceased cow to show historical data
            ),
            Cow(
                name = "Pearl",
                tagNumber = "C006",
                tagColor = "White",
                birthDate = baseDate.minusYears(7),
                gender = Gender.FEMALE,
                classification = Classification.COW,
                colorMarkings = "White with black spots",
                pastureId = pastureIds[0], // North Field
                status = Status.ACTIVE,
                isWatched = true
            ),
            
            // Second Generation Cows (daughters of foundation cows)
            Cow(
                name = "Luna",
                tagNumber = "C007",
                tagColor = "Pink",
                birthDate = baseDate.minusYears(4),
                gender = Gender.FEMALE,
                classification = Classification.COW,
                colorMarkings = "Red with white markings",
                pastureId = pastureIds[1], // South Meadow
                status = Status.ACTIVE,
                motherId = 4, // Bessie's daughter
                fatherId = 1 // Thunder's daughter
            ),
            Cow(
                name = "Star",
                tagNumber = "C008",
                tagColor = "Orange",
                birthDate = baseDate.minusYears(3),
                gender = Gender.FEMALE,
                classification = Classification.COW,
                colorMarkings = "Charolais cream colored",
                pastureId = pastureIds[1], // South Meadow
                status = Status.ACTIVE,
                motherId = 5, // Daisy's daughter
                fatherId = 2 // Storm's daughter
            ),
            Cow(
                name = "Grace",
                tagNumber = "C009",
                tagColor = "Teal",
                birthDate = baseDate.minusYears(3),
                gender = Gender.FEMALE,
                classification = Classification.COW,
                colorMarkings = "Black with white stripe",
                pastureId = pastureIds[0], // North Field
                status = Status.ACTIVE,
                motherId = 6, // Rosie's daughter
                fatherId = 1 // Thunder's daughter
            ),
            
            // Current Heifers (ready to breed)
            Cow(
                name = "Hope",
                tagNumber = "H001",
                tagColor = "Lime",
                birthDate = baseDate.minusYears(2),
                gender = Gender.FEMALE,
                classification = Classification.HEIFER,
                colorMarkings = "Brown with white face",
                pastureId = pastureIds[1], // South Meadow
                status = Status.ACTIVE,
                motherId = 7, // Molly's daughter
                fatherId = 2 // Storm's daughter
            ),
            Cow(
                name = "Faith",
                tagNumber = "H002",
                tagColor = "Coral",
                birthDate = baseDate.minusYears(2),
                gender = Gender.FEMALE,
                classification = Classification.HEIFER,
                colorMarkings = "Red and white spotted",
                pastureId = pastureIds[1], // South Meadow
                status = Status.ACTIVE,
                motherId = 9, // Pearl's daughter
                fatherId = 1 // Thunder's daughter
            ),
            Cow(
                name = "Joy",
                tagNumber = "H003",
                tagColor = "Violet",
                birthDate = baseDate.minusMonths(20),
                gender = Gender.FEMALE,
                classification = Classification.HEIFER,
                colorMarkings = "Black Angus solid",
                pastureId = pastureIds[1], // South Meadow
                status = Status.ACTIVE,
                motherId = 10, // Luna's daughter
                fatherId = 2 // Storm's daughter
            ),
            
            // Steers (castrated males for beef)
            Cow(
                name = "Max",
                tagNumber = "S001",
                tagColor = "Brown",
                birthDate = baseDate.minusYears(2),
                gender = Gender.MALE,
                classification = Classification.STEER,
                colorMarkings = "Black with white stripe",
                pastureId = pastureIds[3], // West Lot
                status = Status.ACTIVE,
                motherId = 4, // Bessie's son
                fatherId = 1 // Thunder's son
            ),
            Cow(
                name = "Duke",
                tagNumber = "S002",
                tagColor = "Navy",
                birthDate = baseDate.minusYears(2),
                gender = Gender.MALE,
                classification = Classification.STEER,
                colorMarkings = "Red with white markings",
                pastureId = pastureIds[3], // West Lot
                status = Status.ACTIVE,
                motherId = 5, // Daisy's son
                fatherId = 2 // Storm's son
            ),
            Cow(
                name = "Rex",
                tagNumber = "S003",
                tagColor = "Gray",
                birthDate = baseDate.minusMonths(18),
                gender = Gender.MALE,
                classification = Classification.STEER,
                colorMarkings = "Charolais cream",
                pastureId = pastureIds[3], // West Lot
                status = Status.SOLD, // Sold for beef
                motherId = 6, // Rosie's son
                fatherId = 1 // Thunder's son
            ),
            
            // Current Calves (recent births)
            Cow(
                name = "Buddy",
                tagNumber = "K001",
                tagColor = "Tan",
                birthDate = baseDate.plusMonths(3),
                gender = Gender.MALE,
                classification = Classification.CALF,
                colorMarkings = "Brown and white spotted",
                pastureId = pastureIds[0], // North Field with mother
                status = Status.ACTIVE,
                motherId = 10, // Luna's calf
                fatherId = 2 // Storm's calf
            ),
            Cow(
                name = "Bella",
                tagNumber = "K002",
                tagColor = "Black",
                birthDate = baseDate.plusMonths(1),
                gender = Gender.FEMALE,
                classification = Classification.CALF,
                colorMarkings = "Solid black",
                pastureId = pastureIds[0], // North Field with mother
                status = Status.ACTIVE,
                motherId = 11, // Star's calf
                fatherId = 1 // Thunder's calf
            ),
            Cow(
                name = "Charlie",
                tagNumber = "K003",
                tagColor = "Yellow",
                birthDate = baseDate.plusMonths(2),
                gender = Gender.MALE,
                classification = Classification.CALF,
                colorMarkings = "Red with white face",
                pastureId = pastureIds[1], // South Meadow with mother
                status = Status.ACTIVE,
                motherId = 12, // Grace's calf
                fatherId = 2 // Storm's calf
            ),
            Cow(
                name = "Rosebud",
                tagNumber = "K004",
                tagColor = "Rose",
                birthDate = LocalDate.now().minusMonths(2),
                gender = Gender.FEMALE,
                classification = Classification.CALF,
                colorMarkings = "Brown Jersey coloring",
                pastureId = pastureIds[0], // North Field with mother
                status = Status.ACTIVE,
                motherId = 4, // Bessie's new calf
                fatherId = 2 // Storm's calf
            ),
            Cow(
                name = "Scout",
                tagNumber = "K005",
                tagColor = "Khaki",
                birthDate = LocalDate.now().minusMonths(1),
                gender = Gender.MALE,
                classification = Classification.CALF,
                colorMarkings = "Black with white markings",
                pastureId = pastureIds[1], // South Meadow with mother
                status = Status.ACTIVE,
                motherId = 9, // Pearl's calf
                fatherId = 1 // Thunder's calf
            ),
            
            // Yearlings (older calves, approaching breeding age)
            Cow(
                name = "Spirit",
                tagNumber = "Y001",
                tagColor = "Mint",
                birthDate = baseDate.minusMonths(14),
                gender = Gender.FEMALE,
                classification = Classification.CALF,
                colorMarkings = "White with black spots",
                pastureId = pastureIds[1], // South Meadow
                status = Status.ACTIVE,
                motherId = 7, // Molly's daughter
                fatherId = 1 // Thunder's daughter
            ),
            Cow(
                name = "Ranger",
                tagNumber = "Y002",
                tagColor = "Forest",
                birthDate = baseDate.minusMonths(16),
                gender = Gender.MALE,
                classification = Classification.CALF,
                colorMarkings = "Red Angus solid",
                pastureId = pastureIds[3], // West Lot (will be castrated)
                status = Status.ACTIVE,
                motherId = 10, // Luna's son
                fatherId = 2 // Storm's son
            )
        )
        
        val cowIds = mutableListOf<Long>()
        cows.forEach { cow ->
            val id = insertCow(cow)
            cowIds.add(id)
        }
        
        // Update parent relationships with actual IDs
        // The parent IDs in the cow objects refer to array indices, we need to convert them to actual database IDs
        for (i in cows.indices) {
            val cow = cows[i]
            val cowId = cowIds[i]
            
            var needsUpdate = false
            var updatedCow = getCowById(cowId)
            
            if (cow.motherId != null && cow.motherId!! > 0 && cow.motherId!! <= cowIds.size) {
                val actualMotherId = cowIds[cow.motherId!!.toInt() - 1] // Convert 1-based to 0-based index
                updatedCow = updatedCow?.copy(motherId = actualMotherId)
                needsUpdate = true
            }
            
            if (cow.fatherId != null && cow.fatherId!! > 0 && cow.fatherId!! <= cowIds.size) {
                val actualFatherId = cowIds[cow.fatherId!!.toInt() - 1] // Convert 1-based to 0-based index
                updatedCow = updatedCow?.copy(fatherId = actualFatherId)
                needsUpdate = true
            }
            
            if (needsUpdate && updatedCow != null) {
                updateCow(updatedCow)
            }
        }
        
        return cowIds
    }

    private suspend fun createSampleActivities(cowIds: List<Long>, pastureIds: List<String>) {
        val baseDate = LocalDate.now()
        val activities = mutableListOf<Activity>()
        
        // Birth activities for calves (including recent births)
        activities.add(Activity(
            cowId = cowIds[19], // Buddy
            date = baseDate.minusMonths(9), // 9 months ago when born
            activityType = ActivityType.BIRTH,
            notes = "Healthy male calf born to Luna",
            details = "Birth weight: 85 lbs, no complications",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[20], // Bella
            date = baseDate.minusMonths(11), // 11 months ago when born
            activityType = ActivityType.BIRTH,
            notes = "Female calf born to Star",
            details = "Birth weight: 78 lbs, assisted birth",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[21], // Charlie
            date = baseDate.minusMonths(10), // 10 months ago when born
            activityType = ActivityType.BIRTH,
            notes = "Male calf born to Grace",
            details = "Birth weight: 82 lbs, natural birth",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[22], // Rosebud
            date = baseDate.minusMonths(2), // Recent birth
            activityType = ActivityType.BIRTH,
            notes = "Female calf born to Bessie",
            details = "Birth weight: 75 lbs, healthy delivery",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[23], // Scout
            date = baseDate.minusMonths(1), // Very recent birth
            activityType = ActivityType.BIRTH,
            notes = "Male calf born to Pearl",
            details = "Birth weight: 88 lbs, strong calf",
            groupId = UUID.randomUUID().toString()
        ))
        
        // Weaning activities (older calves)
        activities.add(Activity(
            cowId = cowIds[24], // Spirit
            date = baseDate.minusMonths(6),
            activityType = ActivityType.WEANED,
            notes = "Weaned from Molly at 8 months",
            details = "Smooth transition, good weight gain",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[25], // Ranger
            date = baseDate.minusMonths(8),
            activityType = ActivityType.WEANED,
            notes = "Weaned from Luna at 8 months",
            details = "Will be castrated for beef production",
            groupId = UUID.randomUUID().toString()
        ))
        
        // Castration activities
        activities.add(Activity(
            cowId = cowIds[16], // Max
            date = baseDate.minusMonths(18),
            activityType = ActivityType.CASTRATED,
            notes = "Castrated for beef production",
            details = "Procedure went well, good recovery",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[17], // Duke
            date = baseDate.minusMonths(18),
            activityType = ActivityType.CASTRATED,
            notes = "Castrated for beef production",
            details = "No complications, healing well",
            groupId = UUID.randomUUID().toString()
        ))
        
        // Move activities (group moves)
        val springMoveGroupId = UUID.randomUUID().toString()
        activities.add(Activity(
            cowId = cowIds[3], // Bessie
            date = baseDate.minusMonths(3),
            activityType = ActivityType.MOVED,
            fromPastureId = pastureIds[1],
            toPastureId = pastureIds[0],
            notes = "Spring pasture rotation",
            details = "Moved to North Field for better grass",
            groupId = springMoveGroupId
        ))
        
        activities.add(Activity(
            cowId = cowIds[4], // Daisy
            date = baseDate.minusMonths(3),
            activityType = ActivityType.MOVED,
            fromPastureId = pastureIds[1],
            toPastureId = pastureIds[0],
            notes = "Spring pasture rotation",
            details = "Moved with Bessie to North Field",
            groupId = springMoveGroupId
        ))
        
        // Health/Work activities for various animals
        activities.add(Activity(
            cowId = cowIds[3], // Bessie
            date = baseDate.minusWeeks(2),
            activityType = ActivityType.WORKED,
            notes = "Annual health check and vaccinations",
            details = "All vaccinations up to date, excellent condition",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[0], // Thunder (bull)
            date = baseDate.minusMonths(1),
            activityType = ActivityType.WORKED,
            notes = "Breeding soundness exam",
            details = "Passed all tests, cleared for breeding",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[1], // Storm (bull)
            date = baseDate.minusMonths(1),
            activityType = ActivityType.WORKED,
            notes = "Breeding soundness exam",
            details = "Excellent condition, high fertility",
            groupId = UUID.randomUUID().toString()
        ))
        
        // Activities for SOLD animals (to demonstrate activities persist)
        activities.add(Activity(
            cowId = cowIds[2], // Titan (SOLD bull)
            date = baseDate.minusMonths(6),
            activityType = ActivityType.WORKED,
            notes = "Pre-sale health check",
            details = "Cleared for sale, excellent breeding record",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[2], // Titan (SOLD bull)
            date = baseDate.minusMonths(5),
            activityType = ActivityType.SOLD,
            notes = "Sold to Johnson Ranch",
            details = "Excellent breeding bull, $8,500 sale price",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[18], // Rex (SOLD steer)
            date = baseDate.minusMonths(4),
            activityType = ActivityType.WORKED,
            notes = "Pre-market health check",
            details = "Ready for market, good weight",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[18], // Rex (SOLD steer)
            date = baseDate.minusMonths(3),
            activityType = ActivityType.SOLD,
            notes = "Sold to local processor",
            details = "Market weight achieved, $1,850 sale price",
            groupId = UUID.randomUUID().toString()
        ))
        
        // Activities for DECEASED animal (to demonstrate activities persist)
        activities.add(Activity(
            cowId = cowIds[7], // Ruby (DECEASED cow)
            date = baseDate.minusMonths(8),
            activityType = ActivityType.WORKED,
            notes = "Health check - showing signs of illness",
            details = "Started treatment for respiratory infection",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[7], // Ruby (DECEASED cow)
            date = baseDate.minusMonths(7),
            activityType = ActivityType.WORKED,
            notes = "Follow-up treatment",
            details = "Condition worsening despite treatment",
            groupId = UUID.randomUUID().toString()
        ))
        
        // Breeding-related work activities (AI, breeding checks, etc.)
        activities.add(Activity(
            cowId = cowIds[3], // Bessie
            date = baseDate.minusMonths(11),
            activityType = ActivityType.WORKED,
            notes = "Artificial insemination with Storm semen",
            details = "AI procedure completed, good timing in cycle",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[8], // Pearl
            date = baseDate.minusMonths(10),
            activityType = ActivityType.WORKED,
            notes = "Natural breeding with Thunder",
            details = "Breeding observed, excellent genetics pairing",
            groupId = UUID.randomUUID().toString()
        ))
        
        // Recent activities for heifers (first breeding)
        activities.add(Activity(
            cowId = cowIds[13], // Hope
            date = baseDate.minusWeeks(3),
            activityType = ActivityType.WORKED,
            notes = "First breeding - AI with Storm semen",
            details = "Heifer ready for breeding, good size and condition",
            groupId = UUID.randomUUID().toString()
        ))
        
        activities.add(Activity(
            cowId = cowIds[14], // Faith
            date = baseDate.minusWeeks(1),
            activityType = ActivityType.WORKED,
            notes = "First breeding - natural service with Thunder",
            details = "Excellent heifer, good breeding prospect",
            groupId = UUID.randomUUID().toString()
        ))
        
        // Insert all activities
        activities.forEach { activity ->
            insertActivity(activity)
        }
    }

    private suspend fun createSampleNotes() {
        noteDao?.let { dao ->
            val notes = listOf(
                Note(
                    title = "Pasture Rotation Plan",
                    text = "Plan to rotate cattle between North Field and South Meadow every 3 months to prevent overgrazing and maintain grass health.",
                    timestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days ago
                ),
                Note(
                    title = "Breeding Schedule",
                    text = "Thunder will be bred with Bessie and Daisy in spring. Storm will be bred with Rosie and Luna. Expecting calves in late winter/early spring.",
                    timestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000) // 3 days ago
                ),
                Note(
                    title = "Feed Inventory",
                    text = "Current hay supply should last through winter. Need to order mineral supplements and check salt lick supplies in West Lot.",
                    timestamp = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000) // 1 day ago
                ),
                Note(
                    title = "Veterinary Visit",
                    text = "Dr. Johnson scheduled for next month to do annual health checks and vaccinations for the entire herd. Need to prepare holding area.",
                    timestamp = System.currentTimeMillis() - (12 * 60 * 60 * 1000) // 12 hours ago
                )
            )
            
            notes.forEach { note ->
                dao.insert(note)
            }
        }
    }

    suspend fun deleteSampleData() {
        if (!isSampleDataInstalled()) return
        
        // Delete sample pastures (this will cascade to remove cows from these pastures)
        val samplePastureIds = listOf("sample-pasture-1", "sample-pasture-2", "sample-pasture-3", "sample-pasture-4")
        samplePastureIds.forEach { pastureId ->
            getPastureByIdSuspend(pastureId)?.let { pasture ->
                deletePasture(pasture)
            }
        }
        
        // Delete sample cows (by tag number pattern)
        val sampleTagPrefixes = listOf("B00", "C00", "H00", "S00", "K00")
        val allCows = getAllCows().firstOrNull() ?: emptyList()
        allCows.filter { cow ->
            cow.tagNumber?.let { tag ->
                sampleTagPrefixes.any { prefix -> tag.startsWith(prefix) }
            } ?: false
        }.forEach { cow ->
            deleteCow(cow)
        }
        
        // Delete sample notes (by title pattern)
        noteDao?.let { dao ->
            val sampleNoteTitles = listOf("Pasture Rotation Plan", "Breeding Schedule", "Feed Inventory", "Veterinary Visit")
            val allNotes = dao.getAllNotes().firstOrNull() ?: emptyList()
            allNotes.filter { note ->
                sampleNoteTitles.contains(note.title)
            }.forEach { note ->
                dao.delete(note)
            }
        }
        
        // Mark sample data as not installed
        insertOrUpdateSetting(
            Settings(
                SettingsKeys.SAMPLE_DATA_INSTALLED,
                "false"
            )
        )
    }

    suspend fun deleteAllData() {
        cowDao.deleteAllCows()
        pastureDao.deleteAllPastures()
        activityDao.deleteAllActivities()
        noteDao?.deleteAllNotes()
        
        // Reset sample data flag
        insertOrUpdateSetting(
            Settings(
                SettingsKeys.SAMPLE_DATA_INSTALLED,
                "false"
            )
        )
        
        // Reinitialize default data
        initializeDefaultData()
    }
    
    // Individual delete all methods for data merge operations
    suspend fun deleteAllCows() = cowDao.deleteAllCows()
    suspend fun deleteAllPastures() = pastureDao.deleteAllPastures()
    suspend fun deleteAllActivities() = activityDao.deleteAllActivities()
    suspend fun deleteAllNotes() = noteDao?.deleteAllNotes()

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
    suspend fun getAllCowsSync(): List<Cow> {
        return try {
            getAllCows().first()
        } catch (e: Exception) {
            println("Error getting cows for sync: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAllActivitiesSync(): List<Activity> {
        return try {
            getAllActivities().first()
        } catch (e: Exception) {
            println("Error getting activities for sync: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAllPasturesSync(): List<Pasture> {
        return try {
            getAllPastures().first()
        } catch (e: Exception) {
            println("Error getting pastures for sync: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAllNotesSync(): List<Note> {
        return try {
            noteDao?.getAllNotes()?.first() ?: emptyList()
        } catch (e: Exception) {
            println("Error getting notes for sync: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun insertNote(note: Note) = noteDao?.insert(note)
    // Update methods for sync
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
        // Ensure every default color exists; re-insert any missing defaults by name (case-insensitive)
        val existing = tagColorDao?.getAllTagColorsSync() ?: emptyList()
        val existingNames = existing.map { it.name.lowercase() }.toSet()
        val defaults = TagColor.getDefaultColors()
        val missing = defaults.filter { it.name.lowercase() !in existingNames }
        if (missing.isNotEmpty()) {
            insertTagColors(missing)
        }
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
        // Ensure every default activity type exists; re-insert any missing defaults by name (case-insensitive)
        val existing = activityTypeConfigDao?.getAllActivityTypesSync() ?: emptyList()
        val existingNames = existing.map { it.name.lowercase() }.toSet()
        val defaults = ActivityTypeConfig.getDefaultActivityTypes()
        val missing = defaults.filter { it.name.lowercase() !in existingNames }
        if (missing.isNotEmpty()) {
            insertActivityTypes(missing)
        }
    }
    
    suspend fun restoreDefaultActivityTypes() {
        // Delete all existing activity types (both custom and default)
        activityTypeConfigDao?.deleteAllActivityTypes()
        // Insert the default activity types
        insertActivityTypes(ActivityTypeConfig.getDefaultActivityTypes())
    }
}
