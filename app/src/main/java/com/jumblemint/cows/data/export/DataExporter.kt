package com.jumblemint.cows.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Note
import com.jumblemint.cows.data.model.Pasture
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ExportData(
    val exportDate: String,
    val cows: List<CowExport>,
    val pastures: List<PastureExport>,
    val activities: List<ActivityExport>,
    val notes: List<NoteExport>
)

data class CowExport(
    val id: Long,
    val name: String?,
    val tagNumber: String?,
    val tagColor: String?,
    val birthDate: String?,
    val gender: String,
    val classification: String,
    val colorMarkings: String?,
    val motherId: Long?,
    val fatherId: Long?,
    val pastureId: String?,
    val status: String,
    val isWatched: Boolean,
    val createdAt: String,
    val updatedAt: String?
)

data class PastureExport(
    val id: String,
    val name: String,
    val description: String?,
    val sizeAcres: Double?,
    val createdAt: String,
    val updatedAt: String?
)

data class ActivityExport(
    val id: Long,
    val cowId: Long,
    val date: String,
    val activityType: String,
    val notes: String?,
    val details: String?,
    val fromPastureId: String?,
    val toPastureId: String?,
    val groupId: String?,
    val cowIds: List<Long>,
    val createdAt: String
)

data class NoteExport(
    val id: Long,
    val title: String,
    val text: String,
    val timestamp: Long
)

class DataExporter(private val context: Context) {
    
    private val gson = GsonBuilder().setPrettyPrinting().create()
    
    suspend fun exportToJson(
        cows: List<Cow>,
        pastures: List<Pasture>,
        activities: List<Activity>,
        notes: List<Note>
    ): File {
        val exportData = ExportData(
            exportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            cows = cows.map { it.toExport() },
            pastures = pastures.map { it.toExport() },
            activities = activities.map { it.toExport() },
            notes = notes.map { it.toExport() }
        )
        
        val jsonString = gson.toJson(exportData)
        val fileName = generateFileName("json")
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(jsonString)
        return file
    }
    
    suspend fun exportToCsv(
        cows: List<Cow>,
        pastures: List<Pasture>,
        activities: List<Activity>,
        notes: List<Note>
    ): File {
        val fileName = generateFileName("csv")
        val file = File(context.getExternalFilesDir(null), fileName)
        
        val csvContent = buildString {
            // Cows CSV
            appendLine("=== COWS ===")
            appendLine("ID,Name,Tag Number,Tag Color,Birth Date,Gender,Classification,Color Markings,Mother ID,Father ID,Pasture ID,Status,Watched,Created At,Updated At")
            cows.forEach { cow ->
                appendLine("${cow.id},\"${cow.name ?: ""}\",\"${cow.tagNumber ?: ""}\",\"${cow.tagColor ?: ""}\",\"${cow.birthDate?.toString() ?: ""}\",\"${cow.gender}\",\"${cow.classification}\",\"${cow.colorMarkings ?: ""}\",\"${cow.motherId ?: ""}\",\"${cow.fatherId ?: ""}\",\"${cow.pastureId ?: ""}\",\"${cow.status}\",\"${cow.isWatched}\",\"${cow.createdAt?.toString() ?: LocalDate.now()}\",\"${cow.updatedAt?.toString() ?: ""}\"")
            }
            
            appendLine()
            appendLine("=== PASTURES ===")
            appendLine("ID,Name,Description,Size Acres,Created At,Updated At")
            pastures.forEach { pasture ->
                appendLine("\"${pasture.id}\",\"${pasture.name}\",\"${pasture.description ?: ""}\",\"${pasture.sizeAcres ?: ""}\",\"${LocalDate.now()}\",\"\"")
            }
            
            appendLine()
            appendLine("=== ACTIVITIES ===")
            appendLine("ID,Cow ID,Date,Activity Type,Notes,Details,From Pasture,To Pasture,Group ID,Cow IDs,Created At")
            activities.forEach { activity ->
                appendLine("${activity.id},${activity.cowId},\"${activity.date}\",\"${activity.activityType}\",\"${activity.notes ?: ""}\",\"${activity.details ?: ""}\",\"${activity.fromPastureId ?: ""}\",\"${activity.toPastureId ?: ""}\",\"${activity.groupId ?: ""}\",\"${activity.cowIds.joinToString(";")}\",\"${activity.date}\"")
            }
            
            appendLine()
            appendLine("=== NOTES ===")
            appendLine("ID,Title,Text,Timestamp")
            notes.forEach { note ->
                appendLine("${note.id},\"${note.title}\",\"${note.text}\",${note.timestamp}")
            }
        }
        
        file.writeText(csvContent)
        return file
    }
    
    private fun generateFileName(extension: String): String {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val time = LocalTime.now()
        val timeCode = String.format("%03d", (time.toSecondOfDay() * 999) / 86399)
        return "HerdManagerExport_${date}_${timeCode}.${extension}"
    }
    
    fun getFileName(format: String): String {
        return generateFileName(format.lowercase())
    }
}

private fun Cow.toExport() = CowExport(
    id = id,
    name = name,
    tagNumber = tagNumber,
    tagColor = tagColor,
    birthDate = birthDate?.toString(),
    gender = gender.name,
    classification = classification.name,
    colorMarkings = colorMarkings,
    motherId = motherId,
    fatherId = fatherId,
    pastureId = pastureId,
    status = status.name,
    isWatched = isWatched,
    createdAt = createdAt?.toString() ?: LocalDate.now().toString(),
    updatedAt = updatedAt?.toString()
)

private fun Pasture.toExport() = PastureExport(
    id = id,
    name = name,
    description = description,
    sizeAcres = sizeAcres,
    createdAt = LocalDate.now().toString(),
    updatedAt = null
)

private fun Activity.toExport() = ActivityExport(
    id = id,
    cowId = cowId,
    date = date.toString(),
    activityType = activityType.name,
    notes = notes,
    details = details,
    fromPastureId = fromPastureId,
    toPastureId = toPastureId,
    groupId = groupId,
    cowIds = cowIds,
    createdAt = date.toString()
)

private fun Note.toExport() = NoteExport(
    id = id,
    title = title,
    text = text,
    timestamp = timestamp
)