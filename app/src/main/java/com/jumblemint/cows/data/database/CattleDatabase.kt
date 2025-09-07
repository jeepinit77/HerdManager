package com.jumblemint.cows.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jumblemint.cows.data.dao.ActivityDao
import com.jumblemint.cows.data.dao.CowDao
import com.jumblemint.cows.data.dao.NoteDao // Ensure this path is correct for your NoteDao
import com.jumblemint.cows.data.dao.PastureDao
import com.jumblemint.cows.data.dao.SettingsDao
import com.jumblemint.cows.data.dao.UserDao
import com.jumblemint.cows.data.dao.HerdDao
import com.jumblemint.cows.data.dao.HerdMemberDao
import com.jumblemint.cows.data.dao.TagColorDao
import com.jumblemint.cows.data.dao.ActivityTypeConfigDao
import com.jumblemint.cows.data.model.* // Assuming all models are here

// Modified the @Database annotation to include exportSchema = false
@Database(entities = [Cow::class, Pasture::class, Activity::class, Settings::class, Note::class, User::class, Herd::class, HerdMember::class, TagColor::class, ActivityTypeConfig::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CattleDatabase : RoomDatabase() {
    abstract fun cowDao(): CowDao
    abstract fun pastureDao(): PastureDao
    abstract fun activityDao(): ActivityDao
    abstract fun settingsDao(): SettingsDao
    abstract fun noteDao(): NoteDao
    abstract fun userDao(): UserDao
    abstract fun herdDao(): HerdDao
    abstract fun herdMemberDao(): HerdMemberDao
    abstract fun tagColorDao(): TagColorDao
    abstract fun activityTypeConfigDao(): ActivityTypeConfigDao

    companion object {
        @Volatile
        private var INSTANCE: CattleDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add groupId column to activities table
                database.execSQL("ALTER TABLE activities ADD COLUMN groupId TEXT")
                // Create index on groupId
                database.execSQL("CREATE INDEX index_activities_groupId ON activities(groupId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create users table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        uid TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        displayName TEXT,
                        photoUrl TEXT,
                        createdAt INTEGER NOT NULL,
                        lastSyncAt INTEGER NOT NULL
                    )
                """)

                // Create herds table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS herds (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT,
                        ownerId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                """)

                // Create herd_members table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS herd_members (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        herdId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        joinedAt INTEGER NOT NULL,
                        invitedBy TEXT,
                        isActive INTEGER NOT NULL,
                        FOREIGN KEY(herdId) REFERENCES herds(id) ON DELETE CASCADE,
                        FOREIGN KEY(userId) REFERENCES users(uid) ON DELETE CASCADE
                    )
                """)

                // Create indices for herd_members
                database.execSQL("CREATE INDEX index_herd_members_herdId ON herd_members(herdId)")
                database.execSQL("CREATE INDEX index_herd_members_userId ON herd_members(userId)")
                database.execSQL("CREATE UNIQUE INDEX index_herd_members_herdId_userId ON herd_members(herdId, userId)")

                // Add sync fields to existing tables
                database.execSQL("ALTER TABLE cows ADD COLUMN herdId TEXT")
                database.execSQL("ALTER TABLE cows ADD COLUMN firestoreId TEXT")
                database.execSQL("ALTER TABLE cows ADD COLUMN lastSyncAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE cows ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE cows ADD COLUMN createdBy TEXT")
                database.execSQL("ALTER TABLE cows ADD COLUMN updatedBy TEXT")

                database.execSQL("ALTER TABLE activities ADD COLUMN herdId TEXT")
                database.execSQL("ALTER TABLE activities ADD COLUMN firestoreId TEXT")
                database.execSQL("ALTER TABLE activities ADD COLUMN lastSyncAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE activities ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE activities ADD COLUMN createdBy TEXT")
                database.execSQL("ALTER TABLE activities ADD COLUMN updatedBy TEXT")

                database.execSQL("ALTER TABLE pastures ADD COLUMN herdId TEXT")
                database.execSQL("ALTER TABLE pastures ADD COLUMN firestoreId TEXT")
                database.execSQL("ALTER TABLE pastures ADD COLUMN lastSyncAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE pastures ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE pastures ADD COLUMN createdBy TEXT")
                database.execSQL("ALTER TABLE pastures ADD COLUMN updatedBy TEXT")

                database.execSQL("ALTER TABLE notes ADD COLUMN herdId TEXT")
                database.execSQL("ALTER TABLE notes ADD COLUMN firestoreId TEXT")
                database.execSQL("ALTER TABLE notes ADD COLUMN lastSyncAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN createdBy TEXT")
                database.execSQL("ALTER TABLE notes ADD COLUMN updatedBy TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create tag_colors table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS tag_colors (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        colorValue INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        firestoreId TEXT,
                        lastSyncAt INTEGER,
                        updatedBy TEXT
                    )
                """)

                // Create activity_type_configs table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_type_configs (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        description TEXT,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        firestoreId TEXT,
                        lastSyncAt INTEGER,
                        updatedBy TEXT
                    )
                """)

                // Add sync fields to settings table
                database.execSQL("ALTER TABLE settings ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE settings ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE settings ADD COLUMN firestoreId TEXT")
                database.execSQL("ALTER TABLE settings ADD COLUMN lastSyncAt INTEGER")
                database.execSQL("ALTER TABLE settings ADD COLUMN updatedBy TEXT")
            }
        }

        fun getDatabase(context: Context): CattleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CattleDatabase::class.java,
                    "cattle_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
