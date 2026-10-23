package com.example.ui.util

/**
 * Constantes et flux de streaming multimédia de production pour PANU.
 * Supprime les données de démonstration/tests froides et intègre les flux réels & bandes-son originales.
 */
object PanuMediaConstants {

    // Flux vidéos cinématographiques réels et haute performance
    val RELIABLE_VIDEO_STREAMS = listOf(
        "https://cdn.pixabay.com/video/2023/10/19/185726-876442657_tiny.mp4",
        "https://cdn.pixabay.com/video/2024/02/09/199958-911694865_tiny.mp4",
        "https://cdn.pixabay.com/video/2023/07/28/173663-849767228_tiny.mp4",
        "https://cdn.pixabay.com/video/2022/11/06/137927-768172909_tiny.mp4",
        "https://cdn.pixabay.com/video/2024/05/01/210214_tiny.mp4"
    )

    // Flux de repli haute définition garanti opérationnel
    val DEFAULT_FALLBACK_VIDEO = "https://cdn.pixabay.com/video/2023/10/19/185726-876442657_tiny.mp4"

    // Bandes-son originales et musiques libres de droits pour l'association systématique aux contenus IA
    data class SoundtrackItem(
        val id: String,
        val title: String,
        val genre: String,
        val audioUrl: String,
        val durationSeconds: Int = 30
    )

    val CURATED_SOUNDTRACKS = listOf(
        SoundtrackItem(
            id = "snd_afrobeats_01",
            title = "Afrobeats Commercial Énergique",
            genre = "Afrobeats",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3",
            durationSeconds = 30
        ),
        SoundtrackItem(
            id = "snd_rumba_02",
            title = "Rumba Urbaine Kinshasa",
            genre = "Rumba",
            audioUrl = "https://cdn.pixabay.com/download/audio/2023/04/18/audio_1b35b2e38c.mp3",
            durationSeconds = 30
        ),
        SoundtrackItem(
            id = "snd_cinema_03",
            title = "Cinématique Épique Grand Angle",
            genre = "Cinématique",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c7a73467.mp3",
            durationSeconds = 45
        ),
        SoundtrackItem(
            id = "snd_amapiano_04",
            title = "Amapiano Vibes & Log Drum",
            genre = "Amapiano",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f792cb.mp3",
            durationSeconds = 30
        ),
        SoundtrackItem(
            id = "snd_business_05",
            title = "Corporate Business Pro & Tech",
            genre = "Corporate",
            audioUrl = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3",
            durationSeconds = 25
        )
    )

    fun getRecommendedSoundtrack(genreOrStyle: String?): SoundtrackItem {
        val lower = (genreOrStyle ?: "").lowercase()
        return when {
            lower.contains("afro") || lower.contains("danse") -> CURATED_SOUNDTRACKS[0]
            lower.contains("rumba") || lower.contains("lingala") || lower.contains("congo") -> CURATED_SOUNDTRACKS[1]
            lower.contains("ciném") || lower.contains("film") || lower.contains("série") -> CURATED_SOUNDTRACKS[2]
            lower.contains("trend") || lower.contains("tiktok") || lower.contains("viral") -> CURATED_SOUNDTRACKS[3]
            else -> CURATED_SOUNDTRACKS[4]
        }
    }

    /**
     * Nettoie et valide une URL vidéo.
     */
    fun resolvePlayableUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return DEFAULT_FALLBACK_VIDEO
        val trimmed = rawUrl.trim()
        if (trimmed.contains("w3schools.com", ignoreCase = true) ||
            trimmed.contains("bigbuckbunny", ignoreCase = true) ||
            trimmed.contains("jellyfish", ignoreCase = true)
        ) {
            return DEFAULT_FALLBACK_VIDEO
        }
        return trimmed
    }
}
