package com.jumblemint.cows.data.import

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.jumblemint.cows.data.export.ExportData
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

class DataImporter(
    private val context: Context,
    private val repository: CattleRepository
) {
    
    private val gson = Gson()
    
    suspend fun importFromJson(uri: Uri): ImportResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
                ?: return ImportResult.Error("Could not read file")
            
            val exportData = gson.fromJson(jsonString, ExportData::class.java)
            
            var imported = 0
            var skipped = 0
            
            // Import pastures first (skip duplicates)
            exportData.pastures.forEach { pastureExport ->
                try {
                    val existing = repository.getPastureByIdSuspend(pastureExport.id)
                    if (existing == null) {
                        val pasture = Pasture(
                            id = pastureExport.id,
                            name = pastureExport.name,
                            description = pastureExport.description,
                            sizeAcres = pastureExport.sizeAcres
                        )
                        repository.insertPasture(pasture)
                        imported++
                    } else {
                        skipped++
                    }
                } catch (e: Exception) {
                    skipped++
                }
            }
            
            // Import cows (skip duplicates)
            exportData.cows.forEach { cowExport ->
                try {
                    val existing = repository.getCowById(cowExport.id)
                    if (existing == null) {
                        val cow = Cow(
                            id = 0, // Let Room auto-generate new ID
                            name = cowExport.name,
                            tagNumber = cowExport.tagNumber,
                            tagColor = cowExport.tagColor,
                            birthDate = cowExport.birthDate?.let { LocalDate.parse(it) },
                            gender = Gender.valueOf(cowExport.gender),
                            classification = Classification.valueOf(cowExport.classification),
                            colorMarkings = cowExport.colorMarkings,
                            motherId = cowExport.motherId,
                            fatherId = cowExport.fatherId,
                            pastureId = cowExport.pastureId,
                            status = Status.valueOf(cowExport.status),
                            isWatched = cowExport.isWatched,
                            createdAt = cowExport.createdAt.let { LocalDate.parse(it) },
                            updatedAt = cowExport.updatedAt?.let { LocalDate.parse(it) }
                        )
                        repository.insertCow(cow)
                        imported++
                    } else {
                        skipped++
                    }
                } catch (e: Exception) {
                    skipped++
                }
            }
            
            // Import activities (skip duplicates)
            exportData.activities.forEach { activityExport ->
                try {
                    val existing = repository.getActivityById(activityExport.id)
                    if (existing == null) {
                        val activity = Activity(
                            id = 0, // Let Room auto-generate new ID
                            cowId = activityExport.cowId,
                            date = LocalDate.parse(activityExport.date),
                            activityType = ActivityType.valueOf(activityExport.activityType),
                            notes = activityExport.notes,
                            details = activityExport.details,
                            fromPastureId = activityExport.fromPastureId,
                            toPastureId = activityExport.toPastureId,
                            groupId = activityExport.groupId,
                            cowIds = activityExport.cowIds
                        )
                        repository.insertActivity(activity)
                        imported++
                    } else {
                        skipped++
                    }
                } catch (e: Exception) {
                    skipped++
                }
            }
            
            // Import notes (skip duplicates)
            exportData.notes.forEach { noteExport ->
                try {
                    val note = Note(
                        id = 0, // Let Room auto-generate new ID
                        title = noteExport.title,
                        text = noteExport.text,
                        timestamp = noteExport.timestamp
                    )
                    repository.insertNote(note)
                    imported++
                } catch (e: Exception) {
                    skipped++
                }
            }
            
            val message = if (skipped > 0) {
                "Imported $imported items, skipped $skipped duplicates"
            } else {
                "Successfully imported $imported items"
            }
            ImportResult.Success(imported, message)
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    suspend fun importFromCsv(uri: Uri): ImportResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            var imported = 0
            var skipped = 0
            var currentSection = ""
            var isFirstLineOfSection = true
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    when {
                        line.startsWith("=== COWS ===") -> {
                            currentSection = "COWS"
                            isFirstLineOfSection = true
                        }
                        line.startsWith("=== PASTURES ===") -> {
                            currentSection = "PASTURES"
                            isFirstLineOfSection = true
                        }
                        line.startsWith("=== ACTIVITIES ===") -> {
                            currentSection = "ACTIVITIES"
                            isFirstLineOfSection = true
                        }
                        line.startsWith("=== NOTES ===") -> {
                            currentSection = "NOTES"
                            isFirstLineOfSection = true
                        }
                        line.isBlank() -> {
                            // Skip empty lines
                        }
                        else -> {
                            if (isFirstLineOfSection) {
                                // Skip header line
                                isFirstLineOfSection = false
                            } else {
                                when (currentSection) {
                                    "COWS" -> {
                                        parseCowCsvLine(line)?.let { cow ->
                                            try {
                                                repository.insertCow(cow)
                                                imported++
                                            } catch (e: Exception) {
                                                skipped++
                                            }
                                        }
                                    }
                                    "PASTURES" -> {
                                        parsePastureCsvLine(line)?.let { pasture ->
                                            try {
                                                val existing = repository.getPastureByIdSuspend(pasture.id)
                                                if (existing == null) {
                                                    repository.insertPasture(pasture)
                                                    imported++
                                                } else {
                                                    skipped++
                                                }
                                            } catch (e: Exception) {
                                                skipped++
                                            }
                                        }
                                    }
                                    "ACTIVITIES" -> {
                                        parseActivityCsvLine(line)?.let { activity ->
                                            try {
                                                repository.insertActivity(activity)
                                                imported++
                                            } catch (e: Exception) {
                                                skipped++
                                            }
                                        }
                                    }
                                    "NOTES" -> {
                                        parseNoteCsvLine(line)?.let { note ->
                                            try {
                                                repository.insertNote(note)
                                                imported++
                                            } catch (e: Exception) {
                                                skipped++
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            val message = if (skipped > 0) {
                "Imported $imported items, skipped $skipped duplicates/errors"
            } else {
                "Successfully imported $imported items"
            }
            ImportResult.Success(imported, message)
        } catch (e: Exception) {
            ImportResult.Error("CSV import failed: ${e.message}")
        }
    }
    
    private fun parseCowCsvLine(line: String): Cow? {
        return try {
            val parts = parseCsvLine(line)
            if (parts.size < 15) return null
            
            Cow(
                id = 0, // Let Room auto-generate new ID
                name = parts[1].takeIf { it.isNotBlank() },
                tagNumber = parts[2].takeIf { it.isNotBlank() },
                tagColor = parts[3].takeIf { it.isNotBlank() },
                birthDate = parts[4].takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                gender = Gender.valueOf(parts[5]),
                classification = Classification.valueOf(parts[6]),
                colorMarkings = parts[7].takeIf { it.isNotBlank() },
                motherId = parts[8].toLongOrNull(),
                fatherId = parts[9].toLongOrNull(),
                pastureId = parts[10].takeIf { it.isNotBlank() },
                status = Status.valueOf(parts[11]),
                isWatched = parts[12].toBoolean(),
                createdAt = parts[13].takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                updatedAt = parts[14].takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parsePastureCsvLine(line: String): Pasture? {
        return try {
            val parts = parseCsvLine(line)
            if (parts.size < 4) return null
            
            Pasture(
                id = parts[0],
                name = parts[1],
                description = parts[2].takeIf { it.isNotBlank() },
                sizeAcres = parts[3].toDoubleOrNull()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseActivityCsvLine(line: String): Activity? {
        return try {
            val parts = parseCsvLine(line)
            if (parts.size < 10) return null
            
            Activity(
                id = 0, // Let Room auto-generate new ID
                cowId = parts[1].toLong(),
                date = LocalDate.parse(parts[2]),
                activityType = ActivityType.valueOf(parts[3]),
                notes = parts[4].takeIf { it.isNotBlank() },
                details = parts[5].takeIf { it.isNotBlank() },
                fromPastureId = parts[6].takeIf { it.isNotBlank() },
                toPastureId = parts[7].takeIf { it.isNotBlank() },
                groupId = parts[8].takeIf { it.isNotBlank() },
                cowIds = parts[9].split(";").mapNotNull { it.toLongOrNull() }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseNoteCsvLine(line: String): Note? {
        return try {
            val parts = parseCsvLine(line)
            if (parts.size < 4) return null
            
            Note(
                id = 0, // Let Room auto-generate new ID
                title = parts[1],
                text = parts[2],
                timestamp = parts[3].toLong()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && !inQuotes -> inQuotes = true
                char == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}

sealed class ImportResult {
    data class Success(val itemsImported: Int, val message: String? = null) : ImportResult()
    data class Error(val message: String) : ImportResult()
}