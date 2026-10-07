package com.example.data.remote

import com.example.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SupabaseClient(private val sessionManager: SessionManager) {

    // Pool de connexions HTTP/2 persistant (Keep-Alive) et parallélisme élevé pour connexions lentes
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .dispatcher(
            Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 16
            }
        )
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .retryOnConnectionFailure(true)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Cache mémoire ultra-rapide (Stale-While-Revalidate) pour réponses GET instantanées sur réseau lent
    private data class CachedEntry(val code: Int, val body: String, val timestampMs: Long)
    private val getCache = ConcurrentHashMap<String, CachedEntry>()
    private val cacheTtlMs = 15_000L // 15s de fraîcheur immédiate + repli hors-ligne/réseau lent

    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Pré-chauffage asynchrone DNS + TCP + TLS dès l'ouverture pour éliminer la latence du 1er appel
        warmupScope.launch {
            try {
                if (isConfigured) {
                    val req = Request.Builder()
                        .url("$currentUrl/rest/v1/")
                        .head()
                        .addHeader("apikey", currentAnonKey)
                        .build()
                    httpClient.newCall(req).execute().close()
                }
            } catch (_: Exception) {
                // Ignoré silencieusement en mode hors-ligne
            }
        }
    }

    val currentUrl: String
        get() {
            var raw = sessionManager.supabaseUrl.value.trim().removeSurrounding("\"")
            if (raw.isBlank()) return ""
            if (raw.startsWith("http://")) {
                raw = "https://" + raw.removePrefix("http://")
            } else if (!raw.startsWith("https://")) {
                raw = "https://$raw"
            }
            return raw.removeSuffix("/")
        }

    val currentAnonKey: String
        get() = sessionManager.supabaseAnonKey.value.trim().removeSurrounding("\"")

    val isConfigured: Boolean
        get() = currentUrl.isNotBlank() &&
                !currentUrl.contains("your-project.supabase.co") &&
                currentAnonKey.isNotBlank() &&
                !currentAnonKey.contains("placeholder") &&
                !currentAnonKey.contains("your-anon-key") &&
                !currentAnonKey.contains("YOUR_SUPABASE")

    private fun buildRequest(
        endpoint: String,
        method: String,
        body: RequestBody? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        includeAnonBearer: Boolean = true
    ): Request {
        val cleanEndpoint = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        val fullUrl = if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            if (endpoint.startsWith("http://")) "https://" + endpoint.removePrefix("http://") else endpoint
        } else {
            "$currentUrl$cleanEndpoint"
        }
        val builder = Request.Builder()
            .url(fullUrl)
            .addHeader("apikey", currentAnonKey)
            .addHeader("Connection", "keep-alive")

        val token = sessionManager.getAuthToken()?.trim()?.removeSurrounding("\"")
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        } else if (includeAnonBearer && !currentAnonKey.startsWith("sb_publishable_")) {
            builder.addHeader("Authorization", "Bearer $currentAnonKey")
        }

        extraHeaders.forEach { (k, v) -> builder.addHeader(k, v) }

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post(body ?: "".toRequestBody(jsonMediaType))
            "PUT" -> builder.put(body ?: "".toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch(body ?: "".toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete(body)
        }

        return builder.build()
    }

    suspend fun execute(
        endpoint: String,
        method: String = "GET",
        jsonBody: String? = null,
        headers: Map<String, String> = emptyMap()
    ): SupabaseResponse = withContext(Dispatchers.IO) {
        val upperMethod = method.uppercase()
        val isGet = upperMethod == "GET"
        val cacheKey = if (isGet) "$currentUrl|$endpoint" else ""

        // Si une écriture a lieu, invalider le cache mémoire associé pour garantir la fraîcheur
        if (!isGet) {
            getCache.clear()
        } else {
            val cached = getCache[cacheKey]
            val now = System.currentTimeMillis()
            if (cached != null && (now - cached.timestampMs) < cacheTtlMs) {
                return@withContext SupabaseResponse.Success(cached.code, cached.body)
            }
        }

        val body = jsonBody?.toRequestBody(jsonMediaType)
        val request = buildRequest(endpoint, upperMethod, body, headers, includeAnonBearer = true)

        try {
            var response: Response = httpClient.newCall(request).execute()
            var responseBody = response.body?.string() ?: ""

            // Fallback intelligent si la passerelle attend Authorization: Bearer avec sb_publishable_
            if (response.code == 401 &&
                sessionManager.getAuthToken().isNullOrBlank() &&
                currentAnonKey.startsWith("sb_publishable_") &&
                !headers.containsKey("Authorization")
            ) {
                response.close()
                val retryHeaders = headers.toMutableMap().apply {
                    put("Authorization", "Bearer $currentAnonKey")
                }
                val retryReq = buildRequest(
                    endpoint,
                    upperMethod,
                    jsonBody?.toRequestBody(jsonMediaType),
                    retryHeaders,
                    includeAnonBearer = false
                )
                response = httpClient.newCall(retryReq).execute()
                responseBody = response.body?.string() ?: ""
            }

            if (response.isSuccessful) {
                if (isGet) {
                    getCache[cacheKey] = CachedEntry(response.code, responseBody, System.currentTimeMillis())
                }
                SupabaseResponse.Success(response.code, responseBody)
            } else {
                // En cas d'erreur serveur temporaire sur un GET, renvoyer la dernière donnée connue en cache
                val fallbackCached = if (isGet) getCache[cacheKey] else null
                if (fallbackCached != null) {
                    SupabaseResponse.Success(fallbackCached.code, fallbackCached.body)
                } else {
                    SupabaseResponse.Error(response.code, parseErrorMessage(responseBody, response.code))
                }
            }
        } catch (e: IOException) {
            // Repli instantané sur le cache mémoire si la connexion est lente ou interrompue
            val fallbackCached = if (isGet) getCache[cacheKey] else null
            if (fallbackCached != null) {
                SupabaseResponse.Success(fallbackCached.code, fallbackCached.body)
            } else {
                SupabaseResponse.NetworkError("Connexion lente ou hors-ligne (${e.localizedMessage ?: "réseau"})")
            }
        } catch (e: Exception) {
            val fallbackCached = if (isGet) getCache[cacheKey] else null
            if (fallbackCached != null) {
                SupabaseResponse.Success(fallbackCached.code, fallbackCached.body)
            } else {
                SupabaseResponse.Error(500, "Erreur inattendue : ${e.localizedMessage}")
            }
        }
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            val json = JSONObject(body)
            when {
                json.has("error_description") && json.getString("error_description").isNotBlank() ->
                    json.getString("error_description")
                json.has("msg") && json.getString("msg").isNotBlank() ->
                    json.getString("msg")
                json.has("message") && json.getString("message").isNotBlank() ->
                    json.getString("message")
                json.has("error") && json.getString("error").isNotBlank() ->
                    json.getString("error")
                json.has("hint") && json.getString("hint").isNotBlank() ->
                    "${json.optString("message", "Erreur")}: ${json.getString("hint")}"
                else -> if (body.isNotBlank()) body.take(250) else "Erreur HTTP $code"
            }
        } catch (e: Exception) {
            if (body.isNotBlank()) body.take(250) else "Erreur serveur ($code)"
        }
    }
}

sealed class SupabaseResponse {
    data class Success(val code: Int, val body: String) : SupabaseResponse() {
        fun asJsonObject(): JSONObject? = try { JSONObject(body) } catch (e: Exception) { null }
        fun asJsonArray(): JSONArray? = try { JSONArray(body) } catch (e: Exception) { null }
    }
    data class Error(val code: Int, val message: String) : SupabaseResponse()
    data class NetworkError(val message: String) : SupabaseResponse()
}
