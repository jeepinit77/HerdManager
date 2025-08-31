package com.jumblemint.cows.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SettingsDaoTest {

    private lateinit var settingsDao: SettingsDao
    private lateinit var database: CattleDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CattleDatabase::class.java
        ).build()
        settingsDao = database.settingsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertOrUpdateSetting_insertNewSetting_successful() = runTest {
        // Given
        val setting = Settings(key = "theme", value = "dark")

        // When
        settingsDao.insertOrUpdateSetting(setting)

        // Then
        val retrievedSetting = settingsDao.getSettingByKey("theme")
        assertNotNull(retrievedSetting)
        assertEquals("theme", retrievedSetting!!.key)
        assertEquals("dark", retrievedSetting.value)
    }

    @Test
    fun insertOrUpdateSetting_updateExistingSetting_successful() = runTest {
        // Given
        val originalSetting = Settings(key = "theme", value = "light")
        val updatedSetting = Settings(key = "theme", value = "dark")

        // When
        settingsDao.insertOrUpdateSetting(originalSetting)
        settingsDao.insertOrUpdateSetting(updatedSetting)

        // Then
        val retrievedSetting = settingsDao.getSettingByKey("theme")
        assertNotNull(retrievedSetting)
        assertEquals("theme", retrievedSetting!!.key)
        assertEquals("dark", retrievedSetting.value)
    }

    @Test
    fun getSettingByKey_existingKey_returnsSetting() = runTest {
        // Given
        val setting = Settings(key = "language", value = "english")
        settingsDao.insertOrUpdateSetting(setting)

        // When
        val result = settingsDao.getSettingByKey("language")

        // Then
        assertNotNull(result)
        assertEquals("language", result!!.key)
        assertEquals("english", result.value)
    }

    @Test
    fun getSettingByKey_nonExistentKey_returnsNull() = runTest {
        // Given - no settings inserted

        // When
        val result = settingsDao.getSettingByKey("nonexistent")

        // Then
        assertNull(result)
    }

    @Test
    fun getAllSettings_noSettings_returnsEmptyList() = runTest {
        // Given - no settings inserted

        // When
        val allSettings = settingsDao.getAllSettings().first()

        // Then
        assertTrue(allSettings.isEmpty())
    }

    @Test
    fun getAllSettings_multipleSettings_returnsAllSettings() = runTest {
        // Given
        val setting1 = Settings(key = "theme", value = "dark")
        val setting2 = Settings(key = "language", value = "english")
        val setting3 = Settings(key = "notifications", value = "enabled")

        settingsDao.insertOrUpdateSetting(setting1)
        settingsDao.insertOrUpdateSetting(setting2)
        settingsDao.insertOrUpdateSetting(setting3)

        // When
        val allSettings = settingsDao.getAllSettings().first()

        // Then
        assertEquals(3, allSettings.size)
        val keys = allSettings.map { it.key }.toSet()
        assertTrue(keys.contains("theme"))
        assertTrue(keys.contains("language"))
        assertTrue(keys.contains("notifications"))
    }

    @Test
    fun deleteSetting_existingSetting_removesFromDatabase() = runTest {
        // Given
        val setting = Settings(key = "temp_setting", value = "temporary")
        settingsDao.insertOrUpdateSetting(setting)

        // Verify setting exists
        var retrievedSetting = settingsDao.getSettingByKey("temp_setting")
        assertNotNull(retrievedSetting)

        // When
        settingsDao.deleteSetting(setting)

        // Then
        retrievedSetting = settingsDao.getSettingByKey("temp_setting")
        assertNull(retrievedSetting)
    }

    @Test
    fun deleteSetting_nonExistentSetting_noError() = runTest {
        // Given
        val nonExistentSetting = Settings(key = "nonexistent", value = "value")

        // When - should not throw exception
        settingsDao.deleteSetting(nonExistentSetting)

        // Then - verify no settings exist
        val allSettings = settingsDao.getAllSettings().first()
        assertTrue(allSettings.isEmpty())
    }
}