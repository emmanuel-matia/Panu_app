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
    val saturation: Float = 0.0f,
    val transition: String = "Fondu enchaîné"
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
    val animation: String = "Fade",
    val textStylePreset: String = "Standard" // Standard, Banner, GoldTitle, Highlight, Neon, Minimal
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

/**
 * Convertit un PanuTemplate en un véritable StudioProject complet pré-rempli avec scènes,
 * calques de textes stylisés, musiques et ratios prêts à être montés dans l'éditeur.
 */
fun PanuTemplate.toStudioProject(): StudioProject {
    val ratio = when (aspectRatio) {
        "16:9" -> StudioAspectRatio.RATIO_16_9
        "1:1" -> StudioAspectRatio.RATIO_1_1
        "4:5" -> StudioAspectRatio.RATIO_4_5
        else -> StudioAspectRatio.RATIO_9_16
    }

    val generatedClips = if (scenes.isNotEmpty()) {
        scenes.mapIndexed { idx, sc ->
            val dur = maxOf(2f, sc.durationSeconds.toFloat())
            StudioMediaClip(
                id = "clip_${id}_${sc.index}_${idx}",
                uri = if (previewVideoUrl.isNotBlank()) Uri.parse(previewVideoUrl) else Uri.EMPTY,
                type = if (previewVideoUrl.endsWith(".mp4", ignoreCase = true)) StudioMediaType.VIDEO else StudioMediaType.IMAGE,
                name = sc.name,
                durationSeconds = dur,
                startTrimSeconds = 0f,
                endTrimSeconds = dur,
                transition = sc.transition
            )
        }
    } else {
        listOf(
            StudioMediaClip(
                id = "clip_${id}_0",
                uri = if (previewVideoUrl.isNotBlank()) Uri.parse(previewVideoUrl) else Uri.EMPTY,
                type = StudioMediaType.IMAGE,
                name = title,
                durationSeconds = durationSeconds.toFloat(),
                startTrimSeconds = 0f,
                endTrimSeconds = durationSeconds.toFloat()
            )
        )
    }

    val generatedTexts = mutableListOf<StudioTextOverlay>()
    // Titre d'accroche principal
    generatedTexts.add(
        StudioTextOverlay(
            id = "txt_title_${id}",
            text = title,
            startTimeSeconds = 0f,
            durationSeconds = minOf(4f, durationSeconds.toFloat()),
            fontSizeSp = 24f,
            colorHex = "#E5A93C",
            backgroundHex = "#CC000000",
            xPosRatio = 0.5f,
            yPosRatio = 0.22f,
            isTitle = true,
            textStylePreset = "GoldTitle"
        )
    )

    // Textes de chaque scène
    var cumulative = 0f
    scenes.forEachIndexed { idx, sc ->
        if (sc.defaultText.isNotBlank()) {
            generatedTexts.add(
                StudioTextOverlay(
                    id = "txt_scene_${id}_${idx}",
                    text = sc.defaultText,
                    startTimeSeconds = cumulative,
                    durationSeconds = maxOf(2f, sc.durationSeconds.toFloat()),
                    fontSizeSp = 18f,
                    colorHex = "#FFFFFF",
                    backgroundHex = "#9914151C",
                    xPosRatio = 0.5f,
                    yPosRatio = 0.76f,
                    isTitle = false,
                    textStylePreset = "Banner"
                )
            )
        }
        cumulative += sc.durationSeconds
    }

    val generatedAudio = if (!musicTrack.isNullOrBlank()) {
        listOf(
            StudioAudioTrack(
                id = "audio_${id}",
                uri = Uri.parse("https://assets.panu.app/audio/preset_${id}.mp3"),
                title = musicTrack,
                durationSeconds = durationSeconds.toFloat(),
                volume = 0.85f
            )
        )
    } else emptyList()

    return StudioProject(
        id = "proj_${System.currentTimeMillis()}",
        title = title,
        aspectRatio = ratio,
        clips = generatedClips,
        textOverlays = generatedTexts,
        audioTracks = generatedAudio,
        coverThumbnailUrl = thumbnailUrl.ifBlank { null }
    )
}
