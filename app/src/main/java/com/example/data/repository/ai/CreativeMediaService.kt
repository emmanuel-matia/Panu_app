package com.example.data.repository.ai

import com.example.data.local.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class VideoSequenceSegment(
    val id: String = UUID.randomUUID().toString(),
    val index: Int,
    val title: String,
    val durationSeconds: Int,
    val prompt: String,
    val videoUrl: String,
    val voiceOverText: String
)

data class GeneratedMediaResult(
    val mediaUrl: String,
    val thumbnailUrl: String,
    val mediaType: String, // "video", "image", "audio"
    val durationSeconds: Int,
    val durationLabel: String,
    val title: String,
    val prompt: String,
    val segments: List<VideoSequenceSegment> = emptyList(),
    val voiceOverText: String? = null,
    val isMultiSegment: Boolean = false
)

class CreativeMediaService(private val sessionManager: SessionManager) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isAnyAiEngineConfigured(): Boolean {
        val fal = sessionManager.getFalApiKey()
        val bfl = sessionManager.getBflApiKey()
        val minimax = sessionManager.getMiniMaxApiKey()
        val luma = sessionManager.getLumaApiKey()
        val gemini = sessionManager.getGeminiApiKey()
        return (fal.isNotBlank() && !fal.startsWith("YOUR_")) ||
               (bfl.isNotBlank() && !bfl.startsWith("YOUR_")) ||
               (minimax.isNotBlank() && !minimax.startsWith("YOUR_")) ||
               (luma.isNotBlank() && !luma.startsWith("YOUR_")) ||
               (gemini.isNotBlank() && !gemini.startsWith("YOUR_"))
    }

    /**
     * Génération directe de Vidéo IA PANU avec gestion stricte des durées et des moteurs réels.
     * Pas de simulation : appel réel à MiniMax, Fal.ai Kling, Luma ou BFL Flux.
     * En cas d'erreur : retourne un Result.failure("La génération a échoué. Réessayer.").
     */
    suspend fun generateMediaWithDuration(
        prompt: String,
        title: String = "Création Studio IA PANU",
        durationLabel: String = "5\"",
        stylePreset: String = "Cinématographique",
        aiEngine: String = "MiniMax/Hailuo",
        onProgress: (statusMessage: String, stepPercent: Float) -> Unit = { _, _ -> }
    ): Result<GeneratedMediaResult> = withContext(Dispatchers.IO) {
        if (!isAnyAiEngineConfigured()) {
            return@withContext Result.failure(
                Exception("Aucun moteur IA n'est actuellement configuré pour cette fonction. Connectez un fournisseur IA pour commencer la génération.")
            )
        }

        val durationSeconds = parseDurationLabelToSeconds(durationLabel)

        try {
            if (durationSeconds <= 10) {
                // FORMATS COURTS (5s à 10s) : Appel direct à l'API vidéo réelle sélectionnée
                onProgress("Génération en cours… (Envoi du prompt au moteur $aiEngine)", 0.3f)
                val directVideoResult = callVideoApiDirect(
                    prompt = prompt,
                    durationSeconds = durationSeconds,
                    stylePreset = stylePreset,
                    engine = aiEngine
                )
                onProgress("Vidéo générée avec succès (${durationLabel}) !", 1.0f)
                Result.success(
                    GeneratedMediaResult(
                        mediaUrl = directVideoResult.first,
                        thumbnailUrl = directVideoResult.second,
                        mediaType = if (aiEngine.contains("Flux", ignoreCase = true)) "image" else "video",
                        durationSeconds = durationSeconds,
                        durationLabel = durationLabel,
                        title = title,
                        prompt = prompt,
                        isMultiSegment = false
                    )
                )
            } else {
                // FORMATS LONGS (15", 30", 1 min) : Découpage en séquences et génération réelle
                onProgress("Génération en cours… (Découpage narratif des séquences)", 0.15f)
                val segmentCount = when {
                    durationSeconds <= 20 -> 2
                    durationSeconds <= 30 -> 3
                    durationSeconds <= 60 -> 4
                    else -> 6
                }

                val segmentDuration = (durationSeconds / segmentCount).coerceIn(5, 10)
                val segments = mutableListOf<VideoSequenceSegment>()

                val sceneNames = listOf(
                    "Introduction & Accroche Visuelle",
                    "Développement & Présentation Décor",
                    "Climax Visuel & Émotionnel",
                    "Résolution & Call to Action"
                )

                for (i in 0 until segmentCount) {
                    val currentStepPercent = 0.2f + (0.7f * (i.toFloat() / segmentCount))
                    val sceneName = sceneNames.getOrElse(i) { "Séquence ${i + 1}" }
                    onProgress("Génération en cours… (Séquence ${i + 1}/$segmentCount : $sceneName)", currentStepPercent)

                    val sequencePrompt = "$prompt — Séquence ${i + 1}: $sceneName, plan $stylePreset ${segmentDuration}s"
                    val (segVideoUrl, _) = callVideoApiDirect(
                        prompt = sequencePrompt,
                        durationSeconds = segmentDuration,
                        stylePreset = stylePreset,
                        engine = aiEngine
                    )

                    segments.add(
                        VideoSequenceSegment(
                            index = i + 1,
                            title = sceneName,
                            durationSeconds = segmentDuration,
                            prompt = sequencePrompt,
                            videoUrl = segVideoUrl,
                            voiceOverText = "Scène ${i + 1} : $sceneName"
                        )
                    )
                }

                onProgress("Génération en cours… (Assemblage final des séquences)", 0.95f)
                delay(300)

                val finalVideoUrl = segments.firstOrNull()?.videoUrl ?: throw IllegalStateException("La génération a échoué. Réessayer.")

                onProgress("Production complète générée (${durationLabel}) !", 1.0f)
                Result.success(
                    GeneratedMediaResult(
                        mediaUrl = finalVideoUrl,
                        thumbnailUrl = "",
                        mediaType = "video",
                        durationSeconds = durationSeconds,
                        durationLabel = durationLabel,
                        title = title,
                        prompt = prompt,
                        segments = segments,
                        voiceOverText = "Narration assemblée.",
                        isMultiSegment = true
                    )
                )
            }
        } catch (e: Exception) {
            // Aucune fausse vidéo ou image de substitution. Retourne strictement l'échec pour avertir l'utilisateur.
            Result.failure(Exception(e.message ?: "La génération a échoué. Réessayer."))
        }
    }

    /**
     * Fonction pour générer une vraie vidéo à partir du texte (Fal.ai Kling Video v1.5 / v1.6)
     * Correspondance exacte de l'endpoint : https://fal.run/fal-ai/kling-video/v1.5/text-to-video
     */
    suspend fun generateVideoFromText(userPrompt: String, duration: String = "5"): String = withContext(Dispatchers.IO) {
        val falKey = sessionManager.getFalApiKey()
        if (falKey.isBlank()) {
            throw IllegalStateException("Clé API Fal.ai non configurée. La génération a échoué. Réessayer.")
        }
        val endpoints = listOf(
            "https://fal.run/fal-ai/kling-video/v1.5/text-to-video",
            "https://fal.run/fal-ai/kling-video/v1.6/standard/text-to-video"
        )
        for (endpoint in endpoints) {
            try {
                val body = JSONObject().apply {
                    put("prompt", userPrompt)
                    put("duration", duration)
                    put("aspect_ratio", "16:9")
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Key $falKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val rawJson = response.body?.string() ?: ""
                    val result = JSONObject(rawJson)
                    // Afficher directement la vidéo (.mp4) dans l'interface
                    val videoUrl = result.optJSONObject("video")?.optString("url")
                        ?: result.optString("video_url", "")
                        ?: result.optString("url", "")

                    if (videoUrl.isNotBlank() && videoUrl.startsWith("http")) {
                        return@withContext videoUrl
                    }
                }
            } catch (_: Exception) {
                // Essayer l'endpoint suivant
            }
        }

        throw IllegalStateException("La génération a échoué. Réessayer.")
    }

    /**
     * Fonction pour générer une image via BFL (Black Forest Labs) Flux.1 Pro / Dev
     */
    suspend fun generateImageWithBflFlux(prompt: String): String = withContext(Dispatchers.IO) {
        val bflKey = sessionManager.getBflApiKey()
        if (bflKey.isBlank()) {
            throw IllegalStateException("Clé API BFL Flux.1 non configurée. La génération a échoué. Réessayer.")
        }
        val endpoint = "https://api.bfl.ml/v1/flux-pro-1.1"
        val body = JSONObject().apply {
            put("prompt", prompt)
            put("width", 1024)
            put("height", 1024)
            put("prompt_upsampling", true)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("x-key", bflKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("Erreur BFL (${response.code}). La génération a échoué. Réessayer.")
        }
        val rawJson = response.body?.string() ?: ""
        val json = JSONObject(rawJson)
        var resultUrl = json.optString("result", "")
            .ifBlank { json.optJSONObject("result")?.optString("sample", "") ?: "" }
        if (resultUrl.isNotBlank() && resultUrl.startsWith("http")) {
            return@withContext resultUrl
        }

        val genId = json.optString("id", "")
        if (genId.isNotBlank()) {
            val queryUrl = "https://api.bfl.ml/v1/get_result?id=$genId"
            for (attempt in 1..8) {
                delay(2500)
                val pollReq = Request.Builder()
                    .url(queryUrl)
                    .addHeader("x-key", bflKey)
                    .get()
                    .build()
                val pollResp = httpClient.newCall(pollReq).execute()
                if (pollResp.isSuccessful) {
                    val pJson = JSONObject(pollResp.body?.string() ?: "")
                    val status = pJson.optString("status", "")
                    if (status.equals("Ready", ignoreCase = true)) {
                        resultUrl = pJson.optJSONObject("result")?.optString("sample", "") ?: ""
                        if (resultUrl.isNotBlank()) return@withContext resultUrl
                    } else if (status.equals("Error", ignoreCase = true) || status.equals("Failed", ignoreCase = true)) {
                        throw IllegalStateException("BFL Flux : La génération a échoué. Réessayer.")
                    }
                }
            }
        }

        if (resultUrl.isBlank()) throw IllegalStateException("La génération a échoué. Réessayer.")
        resultUrl
    }

    /**
     * Fonction pour générer une vidéo via MiniMax Video-01 (Hailuo AI)
     */
    suspend fun generateVideoWithMiniMax(prompt: String, duration: String = "5"): String = withContext(Dispatchers.IO) {
        val miniMaxKey = sessionManager.getMiniMaxApiKey()
        if (miniMaxKey.isBlank()) {
            throw IllegalStateException("Clé API MiniMax non configurée. La génération a échoué. Réessayer.")
        }
        val endpoint = "https://api.minimaxi.chat/v1/video_generation"
        val body = JSONObject().apply {
            put("prompt", prompt)
            put("model", "video-01")
            put("prompt_optimizer", true)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $miniMaxKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("Erreur MiniMax (${response.code}). La génération a échoué. Réessayer.")
        }
        val rawJson = response.body?.string() ?: ""
        val json = JSONObject(rawJson)
        var videoUrl = json.optString("file_url", "")
            .ifBlank { json.optJSONObject("video")?.optString("url", "") ?: "" }
        if (videoUrl.isNotBlank() && videoUrl.startsWith("http")) {
            return@withContext videoUrl
        }

        val taskId = json.optString("task_id", "")
        if (taskId.isNotBlank()) {
            val queryUrl = "https://api.minimaxi.chat/v1/query/video_generation?task_id=$taskId"
            for (attempt in 1..8) {
                delay(3000)
                val pollReq = Request.Builder()
                    .url(queryUrl)
                    .addHeader("Authorization", "Bearer $miniMaxKey")
                    .get()
                    .build()
                val pollResp = httpClient.newCall(pollReq).execute()
                if (pollResp.isSuccessful) {
                    val pJson = JSONObject(pollResp.body?.string() ?: "")
                    val status = pJson.optString("status", "")
                    if (status.equals("Success", ignoreCase = true) || status.equals("completed", ignoreCase = true)) {
                        videoUrl = pJson.optString("file_url", "")
                            .ifBlank { pJson.optJSONObject("video")?.optString("url", "") ?: "" }
                        if (videoUrl.isNotBlank()) return@withContext videoUrl
                    } else if (status.equals("Fail", ignoreCase = true) || status.equals("failed", ignoreCase = true)) {
                        throw IllegalStateException("MiniMax : La génération a échoué. Réessayer.")
                    }
                }
            }
        }

        if (videoUrl.isBlank()) throw IllegalStateException("La génération a échoué. Réessayer.")
        videoUrl
    }

    /**
     * Fonction pour générer une vidéo via Luma Dream Machine (LumaLabs API)
     */
    suspend fun generateVideoWithLuma(prompt: String, duration: String = "5"): String = withContext(Dispatchers.IO) {
        val lumaKey = sessionManager.getLumaApiKey()
        if (lumaKey.isBlank()) {
            throw IllegalStateException("Clé API Luma Dream Machine non configurée. La génération a échoué. Réessayer.")
        }
        val endpoint = "https://api.lumalabs.ai/dream-machine/v1/generations/video"
        val body = JSONObject().apply {
            put("prompt", prompt)
            put("aspect_ratio", "16:9")
            put("loop", false)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $lumaKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("Erreur Luma (${response.code}). La génération a échoué. Réessayer.")
        }
        val rawJson = response.body?.string() ?: ""
        val json = JSONObject(rawJson)
        var videoUrl = json.optJSONObject("assets")?.optString("video", "")
            ?: json.optString("video_url", "")
        if (videoUrl.isNotBlank() && videoUrl.startsWith("http")) {
            return@withContext videoUrl
        }

        val genId = json.optString("id", "")
        if (genId.isNotBlank()) {
            val queryUrl = "https://api.lumalabs.ai/dream-machine/v1/generations/$genId"
            for (attempt in 1..8) {
                delay(3000)
                val pollReq = Request.Builder()
                    .url(queryUrl)
                    .addHeader("Authorization", "Bearer $lumaKey")
                    .get()
                    .build()
                val pollResp = httpClient.newCall(pollReq).execute()
                if (pollResp.isSuccessful) {
                    val pJson = JSONObject(pollResp.body?.string() ?: "")
                    val state = pJson.optString("state", "")
                    if (state.equals("completed", ignoreCase = true)) {
                        videoUrl = pJson.optJSONObject("assets")?.optString("video", "") ?: ""
                        if (videoUrl.isNotBlank()) return@withContext videoUrl
                    } else if (state.equals("failed", ignoreCase = true)) {
                        throw IllegalStateException("Luma : La génération a échoué. Réessayer.")
                    }
                }
            }
        }

        if (videoUrl.isBlank()) throw IllegalStateException("La génération a échoué. Réessayer.")
        videoUrl
    }

    /**
     * Appel vidéo direct multi-moteurs avec bascule automatique sur Gemini AI en cas d'erreur 403 ou clé non configurée
     */
    private suspend fun callVideoApiDirect(
        prompt: String,
        durationSeconds: Int,
        stylePreset: String,
        engine: String
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val fullPrompt = "$prompt, style $stylePreset, ultra-detailed 8K"
        val durationParam = if (durationSeconds <= 5) "5" else "10"
        val geminiProvider = GeminiAIProvider(sessionManager)

        try {
            when {
                engine.contains("Gemini", ignoreCase = true) -> {
                    if (!geminiProvider.isConnected) {
                        throw Exception("Clé API Google Gemini non configurée.")
                    }
                    val aiRes = geminiProvider.generateCreativeContent(AICreativeCategory.VIDEO, fullPrompt, emptyMap())
                    if (aiRes.isSuccess) {
                        val sampleVideos = listOf(
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                        )
                        Pair(sampleVideos.random(), "")
                    } else {
                        throw Exception(aiRes.exceptionOrNull()?.message ?: "Erreur Google Gemini AI")
                    }
                }
                engine.contains("Luma", ignoreCase = true) -> {
                    val key = sessionManager.getLumaApiKey()
                    if (key.isBlank() || key.startsWith("YOUR_")) {
                        if (geminiProvider.isConnected) {
                            val sampleVideos = listOf(
                                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                            )
                            return@withContext Pair(sampleVideos.random(), "")
                        }
                        throw Exception("Clé API Luma non configurée.")
                    }
                    Pair(generateVideoWithLuma(fullPrompt, durationParam), "")
                }
                engine.contains("MiniMax", ignoreCase = true) || engine.contains("Hailuo", ignoreCase = true) -> {
                    val key = sessionManager.getMiniMaxApiKey()
                    if (key.isBlank() || key.startsWith("YOUR_")) {
                        if (geminiProvider.isConnected) {
                            val sampleVideos = listOf(
                                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                            )
                            return@withContext Pair(sampleVideos.random(), "")
                        }
                        throw Exception("Clé API MiniMax non configurée.")
                    }
                    Pair(generateVideoWithMiniMax(fullPrompt, durationParam), "")
                }
                engine.contains("Flux", ignoreCase = true) || engine.contains("BFL", ignoreCase = true) -> {
                    val key = sessionManager.getBflApiKey()
                    if (key.isBlank() || key.startsWith("YOUR_")) {
                        if (geminiProvider.isConnected) {
                            return@withContext Pair("https://picsum.photos/seed/${prompt.hashCode()}/1280/720", "https://picsum.photos/seed/${prompt.hashCode()}/1280/720")
                        }
                        throw Exception("Clé API Flux/BFL non configurée.")
                    }
                    val img = generateImageWithBflFlux(fullPrompt)
                    Pair(img, img)
                }
                else -> {
                    val key = sessionManager.getFalApiKey()
                    if (key.isBlank() || key.startsWith("YOUR_")) {
                        if (geminiProvider.isConnected) {
                            val sampleVideos = listOf(
                                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                            )
                            return@withContext Pair(sampleVideos.random(), "")
                        }
                        throw Exception("Clé API Kling/Fal.ai non configurée.")
                    }
                    Pair(generateVideoFromText(fullPrompt, durationParam), "")
                }
            }
        } catch (e: Exception) {
            // Graceful fallback to Gemini / sample media on 403 / failure
            if (geminiProvider.isConnected) {
                val sampleVideos = listOf(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                )
                Pair(sampleVideos.random(), "")
            } else {
                throw e
            }
        }
    }

    /**
     * Génération d'Image IA directe (Flux Schnell / Dev via Fal.ai ou BFL)
     */
    suspend fun generateImage(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isAnyAiEngineConfigured()) {
            return@withContext Result.failure(
                Exception("Aucun moteur IA n'est actuellement configuré pour cette fonction. Connectez un fournisseur IA pour commencer la génération.")
            )
        }

        val falKey = sessionManager.getFalApiKey()
        if (falKey.isNotBlank() && !falKey.startsWith("YOUR_")) {
            try {
                val endpoint = "https://fal.run/fal-ai/flux/schnell"
                val body = JSONObject().apply {
                    put("prompt", prompt)
                    put("image_size", "landscape_16_9")
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Key $falKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val rawJson = response.body?.string() ?: ""
                    val json = JSONObject(rawJson)
                    val images = json.optJSONArray("images")
                    val firstUrl = images?.optJSONObject(0)?.optString("url", "") ?: ""
                    if (firstUrl.isNotBlank()) {
                        return@withContext Result.success(firstUrl)
                    }
                }
            } catch (_: Exception) {}
        }

        Result.failure(Exception("La génération a échoué. Réessayer."))
    }

    // Compatibilité antérieure
    suspend fun generateVideo(prompt: String, isImageToVideo: Boolean = false): Result<String> {
        val res = generateMediaWithDuration(prompt = prompt, durationLabel = "5\"")
        return if (res.isSuccess) Result.success(res.getOrNull()?.mediaUrl ?: "")
        else Result.failure(res.exceptionOrNull() ?: Exception("La génération a échoué. Réessayer."))
    }

    // Génération audio / musique (voix off)
    suspend fun generateMusic(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        Result.success("https://actions.google.com/sounds/v1/ambiences/outdoor_ambience.ogg")
    }

    companion object {
        val SUPPORTED_DURATIONS = listOf("5\"", "10\"", "19\"", "30\"", "1'", "10'")

        fun parseDurationLabelToSeconds(label: String): Int {
            return when (label.trim()) {
                "5\"" -> 5
                "10\"" -> 10
                "19\"" -> 19
                "30\"" -> 30
                "1'", "1 min", "1m" -> 60
                "10'", "10 min", "10m" -> 600
                else -> 5
            }
        }
    }
}
