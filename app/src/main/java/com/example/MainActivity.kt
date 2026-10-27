package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.navigation.PanuScreen
import com.example.ui.screens.activity.ActivityScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.create.CreatePostScreen
import com.example.ui.screens.founder.FounderDashboardScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.live.LiveSportsScreen
import com.example.ui.screens.profile.EditProfileScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.publicprofile.PublicProfileScreen
import com.example.ui.screens.settings.SupabaseSettingsScreen
import com.example.ui.screens.studio.StudioScreen
import com.example.ui.screens.verify.VerifyMemberCardScreen
import com.example.ui.screens.vod.VideoPlayerScreen
import com.example.ui.theme.PanuTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var containerInstance: PanuAppContainer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, ">>> PANU ENTRY POINT: MainActivity.onCreate() REACHED <<<")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "PANU lifecycle: onCreate initialized, all services lazy loaded, reaching Compose rendering stage")

        val container = try {
            PanuAppContainer(applicationContext).also { containerInstance = it }
        } catch (_: Throwable) {
            PanuAppContainer(applicationContext)
        }

        setContent {
            Log.d(TAG, "PANU lifecycle: setContent active - rendering PanuTheme and PanuAppNavHost")
            val currentTheme by container.sessionManager.themePreference.collectAsState()
            val isDark = when (currentTheme) {
                "light" -> false
                else -> true
            }

            PanuTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PanuTheme.colors.background
                ) {
                    PanuAppNavHost(container = container)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure warm state when app returns to foreground
    }
}

@Composable
fun PanuAppNavHost(container: PanuAppContainer) {
    var currentRoute by remember { mutableStateOf(PanuScreen.Home.route) }
    var selectedUsername by remember { mutableStateOf("") }
    var selectedPlayerUrl by remember { mutableStateOf("") }
    var selectedCardNumber by remember { mutableStateOf("PANU-FND-001") }

    val currentRole by container.sessionManager.currentUserRole.collectAsState()
    val currentUserEmail by container.sessionManager.currentUserEmail.collectAsState()
    val isFounder = remember(currentRole, currentUserEmail) {
        currentUserEmail?.trim()?.equals("emmanuelmatia150@gmail.com", ignoreCase = true) == true ||
                currentRole.equals("founder", ignoreCase = true)
    }

    // Intercept back navigation to return smoothly to Home
    BackHandler(enabled = currentRoute != PanuScreen.Home.route) {
        currentRoute = PanuScreen.Home.route
    }

    fun navigateTo(route: String) {
        Log.d("PanuNav", "Navigating to route: $route")
        when {
            route.startsWith("verify/") -> {
                selectedCardNumber = try {
                    java.net.URLDecoder.decode(route.removePrefix("verify/"), "UTF-8")
                } catch (_: Exception) {
                    route.removePrefix("verify/")
                }
                currentRoute = "verify"
            }
            route.startsWith("public_profile/") -> {
                selectedUsername = route.removePrefix("public_profile/")
                currentRoute = route
            }
            route.startsWith("vod_player/") -> {
                selectedPlayerUrl = try {
                    java.net.URLDecoder.decode(route.removePrefix("vod_player/"), "UTF-8")
                } catch (_: Exception) {
                    route.removePrefix("vod_player/")
                }
                currentRoute = "vod_player"
            }
            route == "live" || route == PanuScreen.LiveSports.route -> {
                currentRoute = PanuScreen.LiveSports.route
            }
            route == "studio" || route == PanuScreen.Studio.route -> {
                currentRoute = PanuScreen.Studio.route
            }
            route == "profile" || route == PanuScreen.Profile.route -> {
                currentRoute = PanuScreen.Profile.route
            }
            route == "home" || route == PanuScreen.Home.route -> {
                currentRoute = PanuScreen.Home.route
            }
            route == "create" || route == PanuScreen.Create.route -> {
                currentRoute = PanuScreen.Create.route
            }
            route == "creations" || route == "activity" || route == PanuScreen.Creations.route || route == PanuScreen.Activity.route -> {
                currentRoute = PanuScreen.Creations.route
            }
            route == "settings" || route == PanuScreen.Settings.route -> {
                currentRoute = PanuScreen.Settings.route
            }
            else -> {
                currentRoute = route
            }
        }
    }

    when {
        currentRoute == PanuScreen.Home.route -> {
            HomeScreen(
                postRepository = container.postRepository,
                profileRepository = container.profileRepository,
                sessionManager = container.sessionManager,
                featuresService = container.panuFeaturesService,
                onNavigate = ::navigateTo,
                onOpenCreateStudio = { navigateTo(PanuScreen.Studio.route) }
            )
        }
        currentRoute == PanuScreen.Studio.route -> {
            StudioScreen(
                mediaService = container.creativeMediaService,
                groundingService = container.groundingService,
                geminiRepository = container.geminiRepository,
                featuresService = container.panuFeaturesService,
                isFounder = isFounder,
                onNavigate = ::navigateTo,
                onNavigateBack = { currentRoute = PanuScreen.Home.route }
            )
        }
        currentRoute == PanuScreen.Create.route -> {
            CreatePostScreen(
                postRepository = container.postRepository,
                profileRepository = container.profileRepository,
                sessionManager = container.sessionManager,
                isFounder = isFounder,
                onNavigate = ::navigateTo,
                onNavigateToHome = { currentRoute = PanuScreen.Home.route },
                onNavigateToFounder = { currentRoute = PanuScreen.Founder.route },
                onNavigateToSettings = { currentRoute = PanuScreen.Settings.route }
            )
        }
        currentRoute == PanuScreen.Creations.route || currentRoute == PanuScreen.Activity.route -> {
            ActivityScreen(
                postRepository = container.postRepository,
                sessionManager = container.sessionManager,
                isFounder = isFounder,
                onNavigate = ::navigateTo,
                featuresService = container.panuFeaturesService,
                onNavigateToCreate = { currentRoute = PanuScreen.Create.route },
                onNavigateToFounder = { currentRoute = PanuScreen.Founder.route },
                onNavigateToSettings = { currentRoute = PanuScreen.Settings.route }
            )
        }
        currentRoute == PanuScreen.Profile.route -> {
            ProfileScreen(
                profileRepository = container.profileRepository,
                authRepository = container.authRepository,
                sessionManager = container.sessionManager,
                isFounder = isFounder,
                onNavigate = ::navigateTo,
                onOpenPublicProfile = { handle ->
                    selectedUsername = handle
                    currentRoute = "public_profile/$handle"
                },
                onLogout = { currentRoute = PanuScreen.Home.route },
                onNavigateToFounder = { currentRoute = PanuScreen.Founder.route },
                onNavigateToSettings = { currentRoute = PanuScreen.Settings.route }
            )
        }
        currentRoute == PanuScreen.EditProfile.route -> {
            EditProfileScreen(
                profileRepository = container.profileRepository,
                sessionManager = container.sessionManager,
                isFounder = isFounder,
                onNavigateBack = { currentRoute = PanuScreen.Profile.route },
                onSavedSuccess = { currentRoute = PanuScreen.Profile.route },
                onNavigateToSettings = { currentRoute = PanuScreen.Settings.route }
            )
        }
        currentRoute == "verify" -> {
            VerifyMemberCardScreen(
                initialCardNumber = selectedCardNumber,
                featuresService = container.panuFeaturesService,
                onNavigateBack = { currentRoute = PanuScreen.Home.route }
            )
        }
        currentRoute.startsWith("public_profile/") -> {
            PublicProfileScreen(
                username = selectedUsername.ifBlank { "panu" },
                profileRepository = container.profileRepository,
                postRepository = container.postRepository,
                onNavigateBack = { currentRoute = PanuScreen.Home.route }
            )
        }
        currentRoute == PanuScreen.LiveSports.route -> {
            LiveSportsScreen(
                onNavigateToPlayer = { url ->
                    selectedPlayerUrl = url
                    currentRoute = "vod_player"
                },
                featuresService = container.panuFeaturesService
            )
        }
        currentRoute == "vod_player" -> {
            VideoPlayerScreen(
                videoUrl = selectedPlayerUrl,
                onNavigateBack = { currentRoute = PanuScreen.Home.route }
            )
        }
        currentRoute == PanuScreen.Settings.route -> {
            SupabaseSettingsScreen(
                sessionManager = container.sessionManager,
                supabaseClient = container.supabaseClient,
                profileRepository = container.profileRepository,
                onNavigateBack = { currentRoute = PanuScreen.Profile.route },
                onNavigateToFounder = { currentRoute = PanuScreen.Founder.route }
            )
        }
        currentRoute == PanuScreen.Login.route -> {
            LoginScreen(
                authRepository = container.authRepository,
                profileRepository = container.profileRepository,
                featuresService = container.panuFeaturesService,
                initialTab = 1,
                onLoginSuccess = { currentRoute = PanuScreen.Profile.route },
                onNavigateToForgotPassword = { currentRoute = PanuScreen.ForgotPassword.route },
                onNavigateToSettings = { currentRoute = PanuScreen.Settings.route },
                onContinueAsGuest = { currentRoute = PanuScreen.Home.route }
            )
        }
        currentRoute == PanuScreen.Register.route -> {
            RegisterScreen(
                authRepository = container.authRepository,
                onRegisterSuccess = { currentRoute = PanuScreen.Profile.route },
                onNavigateToLogin = { currentRoute = PanuScreen.Login.route },
                onNavigateToSettings = { currentRoute = PanuScreen.Settings.route }
            )
        }
        currentRoute == PanuScreen.ForgotPassword.route -> {
            ForgotPasswordScreen(
                authRepository = container.authRepository,
                onNavigateBack = { currentRoute = PanuScreen.Login.route }
            )
        }
        currentRoute.startsWith("founder") -> {
            FounderDashboardScreen(
                founderRepository = container.founderRepository,
                sessionManager = container.sessionManager,
                onNavigateBack = { currentRoute = PanuScreen.Home.route },
                onNavigateToSettings = { currentRoute = PanuScreen.Settings.route }
            )
        }
        else -> {
            HomeScreen(
                postRepository = container.postRepository,
                profileRepository = container.profileRepository,
                sessionManager = container.sessionManager,
                featuresService = container.panuFeaturesService,
                onNavigate = ::navigateTo,
                onOpenCreateStudio = { navigateTo(PanuScreen.Studio.route) }
            )
        }
    }
}
