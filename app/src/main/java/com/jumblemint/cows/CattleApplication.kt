package com.jumblemint.cows

import android.app.Application
import com.jumblemint.cows.auth.AuthService
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CattleApplication : Application() {
    
    // Application scope for one-time initialization
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Database
    val database by lazy { CattleDatabase.getDatabase(this) }
    
    // Repository
    val repository by lazy {
        CattleRepository(
            cowDao = database.cowDao(),
            pastureDao = database.pastureDao(),
            activityDao = database.activityDao(),
            settingsDao = database.settingsDao(),
            noteDao = database.noteDao(),
            userDao = database.userDao(),
            herdDao = database.herdDao(),
            herdMemberDao = database.herdMemberDao(),
            tagColorDao = database.tagColorDao(),
            activityTypeConfigDao = database.activityTypeConfigDao(),
            breedDao = database.breedDao()
        )
    }
    
    // Auth Service
    val authService by lazy { AuthService(this) }
    
    // Sync Service
    val syncService by lazy { SyncService(repository) }

    // Sync Orchestrator (lifecycle-aware binder for screens)
    val syncOrchestrator by lazy { com.jumblemint.cows.sync.SyncOrchestrator(syncService, authService) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize default data once when the app starts
        applicationScope.launch {
            repository.initializeDefaultData()
        }
    }
}