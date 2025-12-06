package com.jumblemint.cows.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.jumblemint.cows.data.database.CattleDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {

    private val dbName = "cattle_database"
    private val backupDirName = "backups"

    suspend fun createLocalBackup(customLocalUri: Uri? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dbPath = context.getDatabasePath(dbName)
            if (!dbPath.exists()) {
                return@withContext Result.failure(Exception("Database file not found"))
            }

            val backupDir = File(context.getExternalFilesDir(null), backupDirName)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "cattle_backup_$timeStamp.db"
            val backupFile = File(backupDir, fileName)

            FileInputStream(dbPath).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Verify and Checkpoint
            if (backupFile.exists() && backupFile.length() > 0) {
                // Force a checkpoint to ensure WAL is merged
                CattleDatabase.getDatabase(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")

                // Re-copy after checkpoint to be safe
                FileInputStream(dbPath).use { input ->
                    FileOutputStream(backupFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Copy to custom location if provided
                if (customLocalUri != null) {
                    try {
                        val targetDir = DocumentFile.fromTreeUri(context, customLocalUri)
                        if (targetDir != null && targetDir.canWrite()) {
                            val targetFile = targetDir.createFile("application/x-sqlite3", fileName)
                            if (targetFile != null) {
                                context.contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                                    FileInputStream(backupFile).use { input ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Don't fail the whole backup if custom copy fails, but maybe log it?
                        // Proceed returning the internal file so Drive backup can still happen
                    }
                }

                Result.success(backupFile)
            } else {
                Result.failure(Exception("Backup file created but empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromLocalBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbPath = context.getDatabasePath(dbName)

            // Close database before overwriting?
            // In a running app, this is tricky. We should probably verify the file first.
            // Ideally we restart the app or re-initialize the DB.

            CattleDatabase.getDatabase(context).close()

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbPath).use { output ->
                    input.copyTo(output)
                }
            }

            // Re-open/Check?
             CattleDatabase.getDatabase(context).openHelper.writableDatabase

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDriveBackup(account: GoogleSignInAccount, file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("Cattle App").build()

            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = file.name
            // Folder ID? For now root or app folder.
            // fileMetadata.parents = listOf("appDataFolder") // if using appDataFolder

            val mediaContent = FileContent("application/x-sqlite3", file)

            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            Result.success(uploadedFile.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriveBackups(account: GoogleSignInAccount): Result<List<com.google.api.services.drive.model.File>> = withContext(Dispatchers.IO) {
        try {
             val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("Cattle App").build()

            val result = driveService.files().list()
                .setQ("name contains 'cattle_backup_' and trashed = false")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name, createdTime, size)")
                .execute()

            Result.success(result.files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromDriveBackup(fileId: String, account: GoogleSignInAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("Cattle App").build()

            // Download file
            val tempFile = File(context.cacheDir, "restore_temp.db")
            val outputStream = FileOutputStream(tempFile)
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            // Restore from temp file
            restoreFromLocalBackup(Uri.fromFile(tempFile))

            // tempFile.delete()

            // Result.success(Unit) // Handled by restoreFromLocalBackup
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
