package com.example.data.model

import android.net.Uri

/**
 * Modèles complets de données pour le Studio de Création Multimédia Réel PANU
 */

enum class StudioMediaType(val labelFr: String) {
    IMAGE("Photo"),
    VIDEO("Vidéo"),
    AUDIO("Audio")
}

data class StudioMediaClip(
    val id: String,
    val uri: Uri,
    val type: StudioMediaType,
    val name: String,
    val durationSeconds: Float = 5.0f,
    val startTrimSeconds: Float = 0.0f,
    val endTrimSeconds: Float = 5.0f,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val filterName: String = "Normal",
    val brightness: Float = 0.0f,
    val contrast: Float = 0.0f,
    val saturation: Float = 0.0f
)

data class StudioTextOverlay(
    val id: String,
    val text: String,
    val startTimeSeconds: Float = 0.0f,
    val durationSeconds: Float = 5.0f,
    val fontSizeSp: Float = 22.0f,
    val colorHex: String = "#FFFFFF",
    val backgroundHex: String? = null,
    val xPosRatio: Float = 0.5f, // 0.0 à 1.0 centré
    val yPosRatio: Float = 0.7f,
    val isTitle: Boolean = false,
    val fontStyle: String = "SansSerif",
    val animation: String = "Fade"
)

data class StudioAudioTrack(
    val id: String,
    val uri: Uri,
    val title: String,
    val durationSeconds: Float = 15.0f,
    val startTrimSeconds: Float = 0.0f,
    val volume: Float = 1.0f,
    val isVoiceOver: Boolean = false,
    val fadeIn: Boolean = false,
    val fadeOut: Boolean = false
)

data class StudioSubtitleItem(
    val id: String,
    val text: String,
    val startTimeSeconds: Float,
    val endTimeSeconds: Float
)

data class StudioPiPOverlay(
    val id: String,
    val uri: Uri,
    val type: StudioMediaType,
    val xPosRatio: Float = 0.75f,
    val yPosRatio: Float = 0.25f,
    val scale: Float = 0.35f,
    val opacity: Float = 1.0f
)

enum class StudioAspectRatio(val label: String, val ratioWidth: Float, val ratioHeight: Float) {
    RATIO_9_16("9:16", 9f, 16f),
    RATIO_16_9("16:9", 16f, 9f),
    RATIO_1_1("1:1", 1f, 1f),
    RATIO_4_5("4:5", 4f, 5f),
    CUSTOM("Libre", 1f, 1f)
}

data class StudioProject(
    val id: String,
    val title: String = "Nouveau Projet PANU",
    val aspectRatio: StudioAspectRatio = StudioAspectRatio.RATIO_9_16,
    val clips: List<StudioMediaClip> = emptyList(),
    val textOverlays: List<StudioTextOverlay> = emptyList(),
    val audioTracks: List<StudioAudioTrack> = emptyList(),
    val subtitles: List<StudioSubtitleItem> = emptyList(),
    val pipOverlays: List<StudioPiPOverlay> = emptyList(),
    val coverImageUri: Uri? = null,
    val coverThumbnailUrl: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val activeFilter: String = "Normal"
) {
    val totalDurationSeconds: Float
        get() {
            val clipsDuration = clips.sumOf { (it.endTrimSeconds - it.startTrimSeconds).toDouble() / it.speed }.toFloat()
            val audioDuration = audioTracks.maxOfOrNull { it.durationSeconds } ?: 0f
            return maxOf(5.0f, maxOf(clipsDuration, audioDuration))
        }
}
