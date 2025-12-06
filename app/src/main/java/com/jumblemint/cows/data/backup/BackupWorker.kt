package com.jumblemint.cows.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jumblemint.cows.data.preferences.BackupPreferences
import kotlinx.coroutines.flow.first

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val backupManager = BackupManager(applicationContext)
        val backupPreferences = BackupPreferences(applicationContext)
        
        val localEnabled = backupPreferences.localBackupEnabled.first()
        val localUriString = backupPreferences.localBackupUri.first()
        val customUri = if (localEnabled && localUriString != null) Uri.parse(localUriString) else null
        
        // Local Backup (Internal + Custom location if configured)
        val result = backupManager.createLocalBackup(customUri)
        
        return if (result.isSuccess) {
            val file = result.getOrThrow()
            backupPreferences.updateLastBackupTimestamp(System.currentTimeMillis())
            
            // Google Drive Backup if enabled
            val driveEnabled = backupPreferences.googleDriveBackupEnabled.first()
            if (driveEnabled) {
                try {
                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(applicationContext)
                    if (account != null && com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(account, com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))) {
                         backupManager.createDriveBackup(account, file)
                    } else {
                        // Cannot backup to drive
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            Result.success()
        } else {
            Result.failure()
        }
    }
}
