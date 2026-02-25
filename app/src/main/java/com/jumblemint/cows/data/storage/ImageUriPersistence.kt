package com.jumblemint.cows.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

class ImageUriPersistence(private val context: Context) {

    fun persistForLongTermAccess(uriStrings: List<String>): List<String> {
        return uriStrings.mapNotNull(::persistOne)
    }

    private fun persistOne(rawUri: String): String? {
        if (rawUri.isBlank()) return null

        val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return rawUri
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return rawUri

        val copied = runCatching {
            val extension = inferExtension(uri)
            val directory = File(context.filesDir, "shared_images").apply { mkdirs() }
            val destination = File(
                directory,
                "shared_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
            )

            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return rawUri

            Uri.fromFile(destination).toString()
        }.getOrNull()

        return copied ?: rawUri
    }

    private fun inferExtension(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        val fromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        if (!fromMime.isNullOrBlank()) return fromMime

        val fromUrl = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        if (!fromUrl.isNullOrBlank()) return fromUrl

        return "jpg"
    }
}
