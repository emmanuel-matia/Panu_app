package com.example.data.repository

import com.example.data.local.ProfileDao
import com.example.data.local.SessionManager
import com.example.data.model.UserProfile
import com.example.data.remote.AuthResult
import com.example.data.remote.SupabaseAuthService
import com.example.data.remote.SupabaseProfileService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val authService: SupabaseAuthService,
    private val profileService: SupabaseProfileService,
    private val profileDao: ProfileDao,
    private val sessionManager: SessionManager
) {
    val currentUserId: StateFlow<String?> = sessionManager.currentUserId
    val currentUserRole: StateFlow<String> = sessionManager.currentUserRole
    val isLoggedIn: Boolean get() = sessionManager.isLoggedIn()

    suspend fun register(email: String, pass: String, fullName: String): Result<AuthResult> {
        val result = authService.signUp(email.trim(), pass, fullName.trim())
        if (result.isSuccess) {
            val auth = result.getOrNull()
            if (auth != null) {
                // Fetch or save initial profile
                val initial = UserProfile(
                    id = auth.userId,
                    email = auth.email,
                    fullName = fullName.trim(),
                    username = email.substringBefore("@").lowercase(),
                    role = auth.role
                )
                profileDao.insertProfile(initial)
                // Persist profile into Supabase PostgreSQL 'profiles' table
                try {
                    profileService.upsertProfile(initial)
                } catch (_: Exception) {}
                // Auto-follow Founder post-registration hook
                try {
                    profileService.autoFollowFounder(auth.userId)
                } catch (_: Exception) {}
            }
        }
        return result
    }

    suspend fun signInWithGoogleAccount(googleEmail: String, fullName: String? = null): Result<AuthResult> {
        val result = authService.signInWithGoogleEmail(googleEmail, fullName)
        if (result.isSuccess) {
            val auth = result.getOrNull()
            if (auth != null) {
                val isFounderUser = googleEmail.equals(SupabaseAuthService.FOUNDER_EMAIL, ignoreCase = true)
                val role = if (isFounderUser) "founder" else auth.role
                val initial = UserProfile(
                    id = auth.userId,
                    email = auth.email,
                    fullName = fullName?.trim()?.ifBlank { null } ?: auth.email.substringBefore("@"),
                    username = auth.email.substringBefore("@").lowercase(),
                    role = role
                )
                profileDao.insertProfile(initial)
                try {
                    profileService.upsertProfile(initial)
                } catch (_: Exception) {}
                try {
                    profileService.autoFollowFounder(auth.userId)
                } catch (_: Exception) {}
            }
        }
        return result
    }

    suspend fun login(email: String, pass: String): Result<AuthResult> {
        val result = authService.signIn(email.trim(), pass)
        if (result.isSuccess) {
            val auth = result.getOrNull()
            if (auth != null) {
                // Sync profile from Supabase to local Room
                val remoteProfile = profileService.getProfile(auth.userId).getOrNull()
                if (remoteProfile != null) {
                    sessionManager.updateRole(remoteProfile.role)
                    if (!remoteProfile.themePreference.isNullOrBlank()) {
                        sessionManager.setThemePreference(remoteProfile.themePreference)
                    }
                    profileDao.insertProfile(remoteProfile)
                } else {
                    val fallback = UserProfile(
                        id = auth.userId,
                        email = auth.email,
                        fullName = auth.email.substringBefore("@"),
                        role = auth.role
                    )
                    profileDao.insertProfile(fallback)
                }
            }
        }
        return result
    }

    fun getGoogleOAuthUrl(): String {
        return authService.getGoogleOAuthUrl()
    }

    suspend fun handleOAuthCallback(rawUri: String): Result<AuthResult> {
        val result = authService.handleOAuthCallbackUri(rawUri)
        if (result.isSuccess) {
            val auth = result.getOrNull()
            if (auth != null) {
                val remoteProfile = profileService.getProfile(auth.userId).getOrNull()
                if (remoteProfile != null) {
                    sessionManager.updateRole(remoteProfile.role)
                    if (!remoteProfile.themePreference.isNullOrBlank()) {
                        sessionManager.setThemePreference(remoteProfile.themePreference)
                    }
                    profileDao.insertProfile(remoteProfile)
                } else {
                    val fallback = UserProfile(
                        id = auth.userId,
                        email = auth.email,
                        fullName = auth.email.substringBefore("@"),
                        username = auth.email.substringBefore("@").lowercase(),
                        role = auth.role
                    )
                    profileDao.insertProfile(fallback)
                }
            }
        }
        return result
    }

    suspend fun signInWithToken(token: String): Result<AuthResult> {
        val result = authService.signInWithToken(token)
        if (result.isSuccess) {
            val auth = result.getOrNull()
            if (auth != null) {
                val remoteProfile = profileService.getProfile(auth.userId).getOrNull()
                if (remoteProfile != null) {
                    sessionManager.updateRole(remoteProfile.role)
                    if (!remoteProfile.themePreference.isNullOrBlank()) {
                        sessionManager.setThemePreference(remoteProfile.themePreference)
                    }
                    profileDao.insertProfile(remoteProfile)
                }
            }
        }
        return result
    }

    suspend fun sendEmailOtp(email: String): Result<String> {
        return authService.sendEmailOtp(email)
    }

    suspend fun verifyEmailOtp(email: String, code: String): Result<AuthResult> {
        val result = authService.verifyEmailOtp(email, code)
        if (result.isSuccess) {
            val auth = result.getOrNull()
            if (auth != null) {
                val isFounderUser = auth.email.equals(SupabaseAuthService.FOUNDER_EMAIL, ignoreCase = true)
                val role = if (isFounderUser) "founder" else auth.role
                val profile = UserProfile(
                    id = auth.userId,
                    email = auth.email,
                    fullName = auth.email.substringBefore("@"),
                    username = auth.email.substringBefore("@").lowercase(),
                    role = role
                )
                profileDao.insertProfile(profile)
                try { profileService.upsertProfile(profile) } catch (_: Exception) {}
                try { profileService.autoFollowFounder(auth.userId) } catch (_: Exception) {}
            }
        }
        return result
    }

    suspend fun signInWithSocialProvider(
        providerName: String,
        accountIdentifier: String,
        fullName: String? = null
    ): Result<AuthResult> {
        val result = authService.signInWithSocialProvider(providerName, accountIdentifier, fullName)
        if (result.isSuccess) {
            val auth = result.getOrNull()
            if (auth != null) {
                val isFounderUser = auth.email.equals(SupabaseAuthService.FOUNDER_EMAIL, ignoreCase = true)
                val role = if (isFounderUser) "founder" else auth.role
                val initial = UserProfile(
                    id = auth.userId,
                    email = auth.email,
                    fullName = fullName?.trim()?.ifBlank { null } ?: auth.email.substringBefore("@"),
                    username = auth.email.substringBefore("@").lowercase(),
                    role = role
                )
                profileDao.insertProfile(initial)
                try { profileService.upsertProfile(initial) } catch (_: Exception) {}
                try { profileService.autoFollowFounder(auth.userId) } catch (_: Exception) {}
            }
        }
        return result
    }

    suspend fun sendWhatsAppOtp(phone: String): Result<Unit> {
        return authService.sendWhatsAppOtp(phone)
    }

    suspend fun sendSmsOtp(phone: String): Result<Unit> {
        return authService.sendOtpSms(phone)
    }

    suspend fun verifySmsOtp(phone: String, code: String): Result<AuthResult> {
        val result = authService.verifyOtp(phone, code)
        if (result.isSuccess) {
            val auth = result.getOrNull()
            if (auth != null) {
                val profile = UserProfile(
                    id = auth.userId,
                    email = auth.email,
                    fullName = "Créateur ${phone.takeLast(4)}",
                    username = "panu_${phone.filter { it.isDigit() }.takeLast(6)}",
                    role = "user"
                )
                profileDao.insertProfile(profile)
                try { profileService.upsertProfile(profile) } catch (_: Exception) {}
                try { profileService.autoFollowFounder(auth.userId) } catch (_: Exception) {}
            }
        }
        return result
    }

    suspend fun verifySession(): Result<AuthResult?> {
        return authService.verifyAndRestoreSession()
    }

    suspend fun logout(): Result<Unit> {
        return authService.signOut()
    }
}
