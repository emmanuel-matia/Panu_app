plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    kotlin("plugin.serialization") version "2.1.0"
}

android {
    namespace = "com.example"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.example"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val envGeminiKey = (project.findProperty("GEMINI_API_KEY") as String?) ?: System.getenv("GEMINI_API_KEY") ?: ""
        val envAnthropicKey = (project.findProperty("ANTHROPIC_API_KEY") as String?) ?: System.getenv("ANTHROPIC_API_KEY") ?: ""
        val envFalKey = (project.findProperty("FAL_KEY") as String?) ?: System.getenv("FAL_KEY") ?: ""
        val envReplicateKey = (project.findProperty("REPLICATE_API_KEY") as String?) ?: System.getenv("REPLICATE_API_KEY") ?: ""
        val envLivekitUrl = (project.findProperty("NEXT_PUBLIC_LIVEKIT_WS_URL") as String?) ?: System.getenv("NEXT_PUBLIC_LIVEKIT_WS_URL") ?: "wss://panu-live-demo.livekit.cloud"
        val envSupabaseUrl = (project.findProperty("SUPABASE_URL") as String?) ?: System.getenv("SUPABASE_URL") ?: "https://xscnbjmiinznzepxzcvn.supabase.co"
        val envSupabaseKey = (project.findProperty("SUPABASE_ANON_KEY") as String?) ?: System.getenv("SUPABASE_ANON_KEY") ?: "sb_publishable_oiSjeHoC_1HnBPIUbfs_4g_dmwCUtyQ"

        buildConfigField("String", "GEMINI_API_KEY", "\"$envGeminiKey\"")
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"$envAnthropicKey\"")
        buildConfigField("String", "FAL_KEY", "\"$envFalKey\"")
        buildConfigField("String", "REPLICATE_API_KEY", "\"$envReplicateKey\"")
        buildConfigField("String", "NEXT_PUBLIC_LIVEKIT_WS_URL", "\"$envLivekitUrl\"")
        buildConfigField("String", "SUPABASE_URL", "\"$envSupabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$envSupabaseKey\"")
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
