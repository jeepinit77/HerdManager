package com.jumblemint.cows.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jumblemint.cows.data.dao.ActivityDao
import com.jumblemint.cows.data.dao.CowDao
import com.jumblemint.cows.data.dao.NoteDao // Ensure this path is correct for your NoteDao
import com.jumblemint.cows.data.dao.PastureDao
import com.jumblemint.cows.data.dao.SettingsDao
import com.jumblemint.cows.data.model.* // Assuming all models are here

// Modified the @Database annotation to include exportSchema = false
@Database(entities = [Cow::class, Pasture::class, Activity::class, Settings::class, Note::class], version = 1, exportSchema = false)
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

        fun getDatabase(context: Context): CattleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CattleDatabase::class.java,
                    "cattle_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
