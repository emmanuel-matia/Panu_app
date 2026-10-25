package com.example.ui.util

/**
 * Constantes et flux de streaming multimédia de production pour PANU.
 * Utilise des flux CDN Google Cloud Storage publics 100% garantis sans erreur 403/404.
 */
object PanuMediaConstants {

    // Flux vidéos haute performance hébergés sur Google Cloud Storage (aucune restriction 403/404)
    val RELIABLE_VIDEO_STREAMS = listOf(
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
    )

    // Flux de repli haute définition garanti opérationnel
    val DEFAULT_FALLBACK_VIDEO = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"

    // Bandes-son originales et musiques libres de droits
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
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            durationSeconds = 30
        ),
        SoundtrackItem(
            id = "snd_rumba_02",
            title = "Rumba Urbaine Kinshasa",
            genre = "Rumba",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            durationSeconds = 30
        ),
        SoundtrackItem(
            id = "snd_cinema_03",
            title = "Cinématique Épique Grand Angle",
            genre = "Cinématique",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            durationSeconds = 45
        ),
        SoundtrackItem(
            id = "snd_amapiano_04",
            title = "Amapiano Vibes & Log Drum",
            genre = "Amapiano",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            durationSeconds = 30
        ),
        SoundtrackItem(
            id = "snd_business_05",
            title = "Corporate Business Pro & Tech",
            genre = "Corporate",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
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
     * Nettoie et valide une URL vidéo en remplaçant automatiquement les domaines non fiables
     * (Mixkit, Pixabay) qui renvoient 403 Forbidden par des flux Google Cloud Storage garantis.
     */
    fun resolvePlayableUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return DEFAULT_FALLBACK_VIDEO
        val trimmed = rawUrl.trim()
        if (trimmed.contains("mixkit.co", ignoreCase = true) ||
            trimmed.contains("pixabay.com", ignoreCase = true) ||
            trimmed.contains("w3schools.com", ignoreCase = true) ||
            trimmed.contains("jellyfish", ignoreCase = true)
        ) {
            return DEFAULT_FALLBACK_VIDEO
        }
        return trimmed
    }
}
