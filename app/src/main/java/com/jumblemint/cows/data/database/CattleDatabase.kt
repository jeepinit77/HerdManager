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
import com.jumblemint.cows.data.model.* // Assuming all models are here

// Modified the @Database annotation to include exportSchema = false
@Database(entities = [Cow::class, Pasture::class, Activity::class, Settings::class, Note::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CattleDatabase : RoomDatabase() {
    abstract fun cowDao(): CowDao
    abstract fun pastureDao(): PastureDao
    abstract fun activityDao(): ActivityDao
    abstract fun settingsDao(): SettingsDao
    abstract fun noteDao(): NoteDao // <-- This is the added line

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

        fun getDatabase(context: Context): CattleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CattleDatabase::class.java,
                    "cattle_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
