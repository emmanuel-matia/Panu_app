package com.example.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class Part(val text: String)

@Serializable
data class Content(val role: String? = null, val parts: List<Part>)

@Serializable
data class SystemInstruction(val parts: List<Part>)

@Serializable
data class ContentRequest(
    val system_instruction: SystemInstruction? = null,
    val contents: List<Content>
)

@Serializable
data class Candidate(val content: Content)

@Serializable
data class ContentResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: ContentRequest
    ): ContentResponse
}
