package com.example.ui.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File

/**
 * Gestionnaire natif de téléchargement et de partage de médias pour PANU.
 * Enregistre les fichiers réels (.mp4, .mp3, .png, .jpg) directement
 * dans le stockage public de l'appareil Android via DownloadManager.
 */
object MediaDownloadManager {

    enum class MediaType(val extension: String, val mimeType: String, val subDir: String) {
        VIDEO("mp4", "video/mp4", Environment.DIRECTORY_MOVIES),
        IMAGE("png", "image/png", Environment.DIRECTORY_PICTURES),
        AUDIO("mp3", "audio/mpeg", Environment.DIRECTORY_MUSIC)
    }

    /**
     * Lance le téléchargement réel d'un média dans le stockage du téléphone.
     */
    fun downloadMedia(
        context: Context,
        url: String,
        title: String,
        mediaType: MediaType
    ): Result<Long> {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || !cleanUrl.startsWith("http", ignoreCase = true)) {
            Toast.makeText(context, "Impossible de télécharger : aucun fichier réel disponible.", Toast.LENGTH_SHORT).show()
            return Result.failure(IllegalArgumentException("URL invalide ou absente"))
        }

        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return Result.failure(IllegalStateException("DownloadManager indisponible sur cet appareil"))

            val safeTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(35).ifBlank { "PANU_Media" }
            val fileName = "PANU_${safeTitle}_${System.currentTimeMillis()}.${mediaType.extension}"

            val request = DownloadManager.Request(Uri.parse(cleanUrl))
                .setTitle("PANU : $title")
                .setDescription("Téléchargement du fichier multimédia PANU...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(mediaType.subDir, "PANU/$fileName")
                .setMimeType(mediaType.mimeType)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(
                context,
                "📥 Téléchargement démarré : $fileName\nEnregistré dans le dossier ${mediaType.subDir}/PANU",
                Toast.LENGTH_LONG
            ).show()
            Result.success(downloadId)
        } catch (e: Exception) {
            Toast.makeText(context, "Échec du téléchargement : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            Result.failure(e)
        }
    }

    /**
     * Partage le lien ou le fichier média vers d'autres applications (WhatsApp, Telegram, etc.)
     */
    fun shareMedia(
        context: Context,
        url: String,
        title: String,
        description: String = ""
    ) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            Toast.makeText(context, "Aucun média disponible à partager.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val shareText = if (description.isNotBlank()) {
                "🎬 Découvre ma création sur PANU : « $title »\n$description\n$cleanUrl"
            } else {
                "🎬 Créé avec le Studio PANU : « $title »\n$cleanUrl"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, "Partager avec PANU"))
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible de partager : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
