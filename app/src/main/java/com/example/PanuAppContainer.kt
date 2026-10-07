package com.example

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.SupabaseAuthService
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseContentService
import com.example.data.remote.SupabaseCreditService
import com.example.data.remote.SupabaseFounderService
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.data.remote.SupabasePostService
import com.example.data.remote.SupabaseProfileService
import com.example.data.remote.SupabaseStorageService
import com.example.data.repository.AuthRepository
import com.example.data.repository.ContentRepository
import com.example.data.repository.CreditRepository
import com.example.data.repository.FounderRepository
import com.example.data.repository.GeminiRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ai.AIProvider
import com.example.data.repository.ai.CreativeMediaService
import com.example.data.repository.ai.GeminiAIProvider
import com.example.data.repository.ai.GroundingService
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PanuAppContainer(context: Context) {
    private val appCtx: Context = context.applicationContext

    init {
        // Configuration globale du cache d'images/miniatures Coil (Mémoire + Disque 100 Mo)
        // Garantit un affichage instantané des visuels même sur connexion très lente (2G/3G)
        val fastImageHttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        Coil.setImageLoader(
            ImageLoader.Builder(appCtx)
                .okHttpClient(fastImageHttpClient)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .memoryCache {
                    MemoryCache.Builder(appCtx)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(appCtx.cacheDir.resolve("panu_fast_image_cache"))
                        .maxSizeBytes(100L * 1024 * 1024) // 100 MB
                        .build()
                }
                .crossfade(120)
                .respectCacheHeaders(false)
                .build()
        )
    }

    // Initialisation paresseuse (by lazy) pour ouverture instantanée (< 5ms) du thread principal UI
    val sessionManager: SessionManager by lazy { SessionManager(appCtx) }
    val supabaseClient: SupabaseClient by lazy { SupabaseClient(sessionManager) }
    val database: AppDatabase by lazy { AppDatabase.getInstance(appCtx) }

    val authService: SupabaseAuthService by lazy { SupabaseAuthService(supabaseClient, sessionManager) }
    val profileService: SupabaseProfileService by lazy { SupabaseProfileService(supabaseClient) }
    val postService: SupabasePostService by lazy { SupabasePostService(supabaseClient) }
    val storageService: SupabaseStorageService by lazy { SupabaseStorageService(supabaseClient, appCtx) }
    val founderService: SupabaseFounderService by lazy { SupabaseFounderService(supabaseClient) }
    val creditService: SupabaseCreditService by lazy { SupabaseCreditService(supabaseClient) }
    val panuFeaturesService: SupabasePanuFeaturesService by lazy {
        SupabasePanuFeaturesService(
            client = supabaseClient,
            sessionManager = sessionManager,
            appContext = appCtx
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            authService = authService,
            profileService = profileService,
            profileDao = database.profileDao(),
            sessionManager = sessionManager
        )
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            profileService = profileService,
            storageService = storageService,
            profileDao = database.profileDao(),
            socialLinksDao = database.socialLinksDao(),
            sessionManager = sessionManager
        )
    }

    val postRepository: PostRepository by lazy {
        PostRepository(
            postService = postService,
            storageService = storageService,
            postDao = database.postDao()
        )
    }

    val founderRepository: FounderRepository by lazy {
        FounderRepository(
            founderService = founderService,
            profileDao = database.profileDao(),
            postDao = database.postDao(),
            creditDao = database.creditDao()
        )
    }

    val creditRepository: CreditRepository by lazy {
        CreditRepository(
            creditService = creditService,
            creditDao = database.creditDao()
        )
    }

    val contentRepository: ContentRepository by lazy {
        ContentRepository(
            contentService = SupabaseContentService(supabaseClient)
        )
    }

    val geminiRepository: GeminiRepository by lazy {
        GeminiRepository(
            apiService = RetrofitClient.geminiService,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    val aiProvider: AIProvider by lazy { GeminiAIProvider(sessionManager) }
    val creativeMediaService: CreativeMediaService by lazy { CreativeMediaService(sessionManager) }
    val groundingService: GroundingService by lazy { GroundingService(sessionManager) }
}
