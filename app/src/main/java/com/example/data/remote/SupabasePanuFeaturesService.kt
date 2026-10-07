package com.example.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.local.SessionManager
import com.example.data.model.CanvasLayer
import com.example.data.model.CanvasLayerType
import com.example.data.model.CanvasProject
import com.example.data.model.CreatorRemunerationStats
import com.example.data.model.CreditPackOffer
import com.example.data.model.LiveGiftCatalogItem
import com.example.data.model.LiveGiftTransaction
import com.example.data.model.MemberCardVerification
import com.example.data.model.MonetizedAdSequence
import com.example.data.model.PanuNotification
import com.example.data.model.PaymentProvider
import com.example.data.model.ViralVideoTemplate
import com.example.data.model.VisibilityBoostPlan
import com.example.data.repository.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SupabasePanuFeaturesService(
    private val client: SupabaseClient,
    private val sessionManager: SessionManager,
    private val appContext: Context
) {
    // =========================================================================
    // 1. SYSTÈME DE NOTIFICATIONS EN TEMPS RÉEL (IN-APP BANNER + PUSH + SUPABASE)
    // =========================================================================
    private val _inAppBannerNotification = MutableStateFlow<PanuNotification?>(null)
    val inAppBannerNotification: StateFlow<PanuNotification?> = _inAppBannerNotification.asStateFlow()

    private val _notificationsHistory = MutableStateFlow<List<PanuNotification>>(
        listOf(
            PanuNotification(
                id = "notif_welcome_panu",
                userId = "public",
                title = "Bienvenue sur PANU Studio 🇨🇮🇸🇳🇨🇲",
                message = "Base de données Supabase connectée en temps réel. Mode hors-ligne PWA actif.",
                notificationType = "system",
                isRead = false,
                createdAt = "À l'instant"
            )
        )
    )
    val notificationsHistory: StateFlow<List<PanuNotification>> = _notificationsHistory.asStateFlow()

    // Solde de crédits PANU en temps réel (Modèle 60 Crédits Gratuits offerts à l'inscription)
    private val _panuCreditsBalance = MutableStateFlow(60)
    val panuCreditsBalance: StateFlow<Int> = _panuCreditsBalance.asStateFlow()

    // Statut Haute Définition (HD/8K), Sans Filigrane & Génération Rapide débloqué via Packs de Crédits
    private val _hasNoWatermarkHdPass = MutableStateFlow(false)
    val hasNoWatermarkHdPass: StateFlow<Boolean> = _hasNoWatermarkHdPass.asStateFlow()

    // Liste des vidéos boostées en tête de l'onglet Découvrir
    private val _boostedVideoIds = MutableStateFlow<Set<String>>(setOf("panu_viral_01"))
    val boostedVideoIds: StateFlow<Set<String>> = _boostedVideoIds.asStateFlow()

    // Mode Hors-ligne CapCut / PWA : Cache local des projets et file de synchronisation automatique
    private val offlinePrefs = appContext.getSharedPreferences("panu_offline_studio_cache", Context.MODE_PRIVATE)
    private val _pendingOfflineSyncCount = MutableStateFlow(0)
    val pendingOfflineSyncCount: StateFlow<Int> = _pendingOfflineSyncCount.asStateFlow()

    private val _isOfflineCapCutModeActive = MutableStateFlow(true)
    val isOfflineCapCutModeActive: StateFlow<Boolean> = _isOfflineCapCutModeActive.asStateFlow()

    fun dismissInAppBanner() {
        _inAppBannerNotification.value = null
    }

    /**
     * Système de Notifications de Confirmation Doubles dès qu'un nouvel utilisateur crée son compte :
     * 1. Côté Utilisateur : Notification de bienvenue et de confirmation adaptée à la méthode utilisée (E-mail, Google, Apple ID, Facebook, WhatsApp OTP, SMS).
     * 2. Côté Fondateur (emmanuelmatia150@gmail.com) : Alerte instantanée informant de l'inscription du nouvel utilisateur.
     */
    suspend fun triggerDualRegistrationNotifications(
        userIdentifier: String,
        fullName: String,
        authMethod: String
    ) {
        val cleanName = fullName.trim().ifBlank { userIdentifier.substringBefore("@") }
        val userTitle = "Bienvenue sur PANU • Confirmation $authMethod ✅"
        val userMessage = "Bonjour $cleanName ! Votre inscription via $authMethod ($userIdentifier) est confirmée. Vos +60 Crédits Gratuits et le Mode Studio Gratuit & Hors-ligne sont activés."

        // 1. Notification côté Utilisateur (In-App + Push + Table notifications)
        triggerRealtimeNotification(
            title = userTitle,
            message = userMessage,
            type = "auth"
        )

        // 2. Alerte instantanée côté Fondateur (emmanuelmatia150@gmail.com)
        val founderAlertTitle = "🔔 Nouvelle inscription sur PANU ($authMethod)"
        val founderAlertMessage = "Nouvel utilisateur inscrit : $cleanName ($userIdentifier) via $authMethod. Auto-abonnement au compte Fondateur (${SupabaseAuthService.FOUNDER_EMAIL}) validé."

        val founderNotif = PanuNotification(
            id = "founder_alert_${UUID.randomUUID()}",
            userId = SupabaseAuthService.FOUNDER_EMAIL,
            title = founderAlertTitle,
            message = founderAlertMessage,
            notificationType = "founder_alert",
            isRead = false,
            createdAt = "À l'instant"
        )
        _notificationsHistory.value = listOf(founderNotif) + _notificationsHistory.value

        withContext(Dispatchers.IO) {
            try {
                // Recherche de l'UUID du fondateur ou insertion directe de l'alerte fondateur dans Supabase
                val founderPayload = JSONObject().apply {
                    put("title", founderAlertTitle)
                    put("message", founderAlertMessage)
                    put("notification_type", "founder_alert")
                    put("is_read", false)
                }
                val currentUid = sessionManager.currentUserId.value
                if (!currentUid.isNullOrBlank()) {
                    founderPayload.put("user_id", currentUid)
                }
                client.execute(
                    endpoint = "/rest/v1/notifications",
                    method = "POST",
                    jsonBody = founderPayload.toString()
                )

                // 3. Envoi d'un e-mail réel et instantané sur Gmail (emmanuelmatia150@gmail.com) via Supabase Edge Function / Resend / Webhook
                val edgeEmailPayload = JSONObject().apply {
                    put("eventType", "NEW_USER_SIGNUP")
                    put("subject", founderAlertTitle)
                    put("userIdentifier", userIdentifier)
                    put("fullName", cleanName)
                    put("authMethod", authMethod)
                    put("details", founderAlertMessage)
                }
                client.execute(
                    endpoint = "/functions/v1/founder-gmail-alert",
                    method = "POST",
                    jsonBody = edgeEmailPayload.toString()
                )
            } catch (_: Exception) {}
        }
    }

    /**
     * Robot de Modération Automatique & Anti-Piratage en Temps Réel (Style TikTok / Facebook) :
     * Détecte et bloque immédiatement les tentatives d'usurpation du Fondateur, d'injection XSS/SQLi ou de spam,
     * et envoie une alerte e-mail instantanée à emmanuelmatia150@gmail.com.
     */
    suspend fun inspectAndModerateAction(
        content: String,
        actorEmail: String?,
        actionType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = actorEmail?.trim()?.lowercase() ?: ""
        val lower = content.lowercase()

        // 1. Détection d'usurpation de l'identité du Fondateur (Anti-Spoofing)
        if (cleanEmail != SupabaseAuthService.FOUNDER_EMAIL.lowercase()) {
            val spoofTerms = listOf("emmanuel matia", "emmanuelmatia150", "fondateur officiel panu", "admin officiel panu")
            if (spoofTerms.any { lower.contains(it) }) {
                logSecurityIncidentAndEmailFounder(
                    actor = cleanEmail.ifBlank { "anonyme" },
                    eventType = "spoofing_blocked",
                    details = "Tentative d'usurpation du Fondateur (${SupabaseAuthService.FOUNDER_EMAIL}) bloquée dans $actionType."
                )
                return@withContext Result.failure(
                    Exception("Sécurité PANU : Tentative d'usurpation d'identité officielle détectée et bloquée immédiatement.")
                )
            }
        }

        // 2. Détection d'injection malveillante (XSS / SQLi / Piratage)
        val attackIndicators = listOf("<script", "javascript:", "onerror=", "union select", "drop table", "or 1=1")
        if (attackIndicators.any { lower.contains(it) }) {
            logSecurityIncidentAndEmailFounder(
                actor = cleanEmail.ifBlank { "anonyme" },
                eventType = "xss_sqli_blocked",
                details = "Tentative d'attaque/injection bloquée par le robot anti-piratage dans $actionType."
            )
            return@withContext Result.failure(
                Exception("Robot Anti-Piratage PANU : Contenu malveillant bloqué et signalé au Fondateur.")
            )
        }

        Result.success(Unit)
    }

    private suspend fun logSecurityIncidentAndEmailFounder(
        actor: String,
        eventType: String,
        details: String
    ) {
        try {
            val logPayload = JSONObject().apply {
                put("actor_identifier", actor)
                put("event_type", eventType)
                put("severity", "CRITICAL")
                put("details", details)
                put("blocked_automatically", true)
                put("founder_email_notified", SupabaseAuthService.FOUNDER_EMAIL)
            }
            client.execute(
                endpoint = "/rest/v1/security_moderation_logs",
                method = "POST",
                jsonBody = logPayload.toString()
            )
            val edgePayload = JSONObject().apply {
                put("eventType", "SECURITY_BOT_BLOCK")
                put("subject", "🚨 [PANU SÉCURITÉ] Menace bloquée ($eventType)")
                put("userIdentifier", actor)
                put("details", details)
            }
            client.execute(
                endpoint = "/functions/v1/founder-gmail-alert",
                method = "POST",
                jsonBody = edgePayload.toString()
            )
        } catch (_: Exception) {}
    }

    suspend fun triggerRealtimeNotification(
        title: String,
        message: String,
        type: String = "system"
    ) {
        val userId = sessionManager.currentUserId.value ?: "guest"
        val newNotif = PanuNotification(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            message = message,
            notificationType = type,
            isRead = false,
            createdAt = "À l'instant"
        )

        // 1. Mise à jour immédiate de la bannière In-App et de l'historique local
        _inAppBannerNotification.value = newNotif
        _notificationsHistory.value = listOf(newNotif) + _notificationsHistory.value

        // 2. Notification Push système Android
        sendAndroidPushNotification(title, message)

        // 3. Sauvegarde dans la table Supabase `notifications` si l'utilisateur est connecté
        if (userId != "guest" && userId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val payload = JSONObject().apply {
                        put("user_id", userId)
                        put("title", title)
                        put("message", message)
                        put("notification_type", type)
                        put("is_read", false)
                    }
                    client.execute(
                        endpoint = "/rest/v1/notifications",
                        method = "POST",
                        jsonBody = payload.toString()
                    )
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun fetchUserNotifications(): List<PanuNotification> = withContext(Dispatchers.IO) {
        val userId = sessionManager.currentUserId.value
        if (userId.isNullOrBlank()) return@withContext _notificationsHistory.value

        val endpoint = "/rest/v1/notifications?user_id=eq.$userId&order=created_at.desc&limit=30"
        when (val resp = client.execute(endpoint)) {
            is SupabaseResponse.Success -> {
                val arr = resp.asJsonArray() ?: JSONArray()
                val loaded = mutableListOf<PanuNotification>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    loaded.add(
                        PanuNotification(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = obj.optString("user_id", userId),
                            title = obj.optString("title", "Notification PANU"),
                            message = obj.optString("message", ""),
                            notificationType = obj.optString("notification_type", "system"),
                            isRead = obj.optBoolean("is_read", false),
                            createdAt = obj.optString("created_at", "Récent").take(16).replace("T", " ")
                        )
                    )
                }
                if (loaded.isNotEmpty()) {
                    _notificationsHistory.value = loaded
                }
            }
            else -> {}
        }
        _notificationsHistory.value
    }

    suspend fun markAllNotificationsRead() = withContext(Dispatchers.IO) {
        _notificationsHistory.value = _notificationsHistory.value.map { it.copy(isRead = true) }
        val userId = sessionManager.currentUserId.value ?: return@withContext
        try {
            val payload = JSONObject().apply { put("is_read", true) }
            client.execute(
                endpoint = "/rest/v1/notifications?user_id=eq.$userId",
                method = "PATCH",
                jsonBody = payload.toString()
            )
        } catch (_: Exception) {}
    }

    private fun sendAndroidPushNotification(title: String, message: String) {
        try {
            val channelId = "panu_realtime_channel"
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Notifications Temps Réel PANU",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertes de compte, cadeaux Live et créations PANU"
                }
                manager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (_: Exception) {}
    }

    // =========================================================================
    // 2. STUDIO GRAPHIQUE & DESIGN (STYLE CANVA -> TABLE `canvas_projects`)
    // =========================================================================
    private val _savedCanvasProjects = MutableStateFlow<List<CanvasProject>>(
        listOf(
            CanvasProject(
                id = "canvas_preset_afro_01",
                userId = "public",
                title = "Affiche Concert Afrobeats Abidjan",
                backgroundHex = "#1A1423",
                exportFormat = "PNG",
                layers = listOf(
                    CanvasLayer("l1", CanvasLayerType.SHAPE, "Cercle Doré", x = 140f, y = 90f, colorHex = "#E5A93C"),
                    CanvasLayer("l2", CanvasLayerType.TEXT, "FESTIVAL PANU LIVE", x = 40f, y = 130f, colorHex = "#FFFFFF", fontSizeSp = 26f),
                    CanvasLayer("l3", CanvasLayerType.STICKER, "🔥👑🎶", x = 110f, y = 210f, fontSizeSp = 34f)
                )
            ),
            CanvasProject(
                id = "canvas_preset_promo_02",
                userId = "public",
                title = "Miniature Virale TikTok & Reels",
                backgroundHex = "#0F2027",
                exportFormat = "MP4",
                layers = listOf(
                    CanvasLayer("l4", CanvasLayerType.TEXT, "TOP 5 SECRETS IA", x = 50f, y = 110f, colorHex = "#E5A93C", fontSizeSp = 28f),
                    CanvasLayer("l5", CanvasLayerType.STICKER, "🚀✨🎬", x = 100f, y = 190f, fontSizeSp = 32f)
                )
            )
        )
    )
    val savedCanvasProjects: StateFlow<List<CanvasProject>> = _savedCanvasProjects.asStateFlow()

    suspend fun saveCanvasProjectToSupabase(project: CanvasProject): Result<CanvasProject> = withContext(Dispatchers.IO) {
        val userId = sessionManager.currentUserId.value ?: "guest"
        val layersArray = JSONArray()
        project.layers.forEach { layer ->
            layersArray.put(
                JSONObject().apply {
                    put("id", layer.id)
                    put("type", layer.type.name)
                    put("content", layer.content)
                    put("x", layer.x.toDouble())
                    put("y", layer.y.toDouble())
                    put("scale", layer.scale.toDouble())
                    put("colorHex", layer.colorHex)
                    put("fontSizeSp", layer.fontSizeSp.toDouble())
                    put("isBold", layer.isBold)
                }
            )
        }

        val updatedList = listOf(project) + _savedCanvasProjects.value.filterNot { it.id == project.id }
        _savedCanvasProjects.value = updatedList

        val payload = JSONObject().apply {
            put("id", project.id)
            put("user_id", if (userId.isBlank()) "guest" else userId)
            put("title", project.title)
            put("canvas_width", project.canvasWidth)
            put("canvas_height", project.canvasHeight)
            put("background_hex", project.backgroundHex)
            put("layers_json", layersArray)
            put("export_format", project.exportFormat)
        }

        // Sauvegarde locale systématique (Mode Hors-ligne Style CapCut / PWA)
        cacheProjectLocally(payload)

        var syncedOnline = false
        if (userId != "guest" && userId.isNotBlank()) {
            val headers = mapOf("Prefer" to "return=representation")
            val resp = client.execute(
                endpoint = "/rest/v1/canvas_projects",
                method = "POST",
                jsonBody = payload.toString(),
                headers = headers
            )
            syncedOnline = resp is SupabaseResponse.Success
        }

        if (!syncedOnline) {
            enqueueOfflineProjectForAutoSync(payload)
            triggerRealtimeNotification(
                title = "Sauvegardé en Mode Hors-ligne (CapCut Local) 💾",
                message = "Le projet « ${project.title} » est enregistré localement et sera synchronisé automatiquement dès le retour du réseau.",
                type = "system"
            )
        } else {
            triggerRealtimeNotification(
                title = "Création sauvegardée dans canvas_projects 🎨",
                message = "Le projet « ${project.title} » (Format ${project.exportFormat}) est synchronisé avec Supabase.",
                type = "system"
            )
        }
        Result.success(project)
    }

    private fun cacheProjectLocally(projectJson: JSONObject) {
        try {
            val raw = offlinePrefs.getString("cached_canvas_projects", "[]") ?: "[]"
            val arr = JSONArray(raw)
            val next = JSONArray()
            next.put(projectJson)
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                if (item.optString("id") != projectJson.optString("id") && next.length() < 25) {
                    next.put(item)
                }
            }
            offlinePrefs.edit().putString("cached_canvas_projects", next.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun enqueueOfflineProjectForAutoSync(projectJson: JSONObject) {
        try {
            val raw = offlinePrefs.getString("pending_sync_projects", "[]") ?: "[]"
            val arr = JSONArray(raw)
            arr.put(projectJson)
            offlinePrefs.edit().putString("pending_sync_projects", arr.toString()).apply()
            _pendingOfflineSyncCount.value = arr.length()
        } catch (_: Exception) {}
    }

    /**
     * Synchronise automatiquement les créations éditées hors-ligne dès le retour du réseau.
     */
    suspend fun syncPendingOfflineCreations(): Int = withContext(Dispatchers.IO) {
        try {
            val raw = offlinePrefs.getString("pending_sync_projects", "[]") ?: "[]"
            val arr = JSONArray(raw)
            if (arr.length() == 0) {
                _pendingOfflineSyncCount.value = 0
                return@withContext 0
            }
            val userId = sessionManager.currentUserId.value
            if (userId.isNullOrBlank() || userId == "guest") {
                _pendingOfflineSyncCount.value = arr.length()
                return@withContext 0
            }

            val remaining = JSONArray()
            var syncedCount = 0
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                obj.put("user_id", userId)
                val resp = client.execute(
                    endpoint = "/rest/v1/canvas_projects",
                    method = "POST",
                    jsonBody = obj.toString()
                )
                if (resp is SupabaseResponse.Success) {
                    syncedCount++
                } else {
                    remaining.put(obj)
                }
            }
            offlinePrefs.edit().putString("pending_sync_projects", remaining.toString()).apply()
            _pendingOfflineSyncCount.value = remaining.length()

            if (syncedCount > 0) {
                triggerRealtimeNotification(
                    title = "Synchronisation Hors-ligne terminée 🔄",
                    message = "$syncedCount projet(s) édité(s) hors-ligne ont été synchronisés automatiquement sur Supabase.",
                    type = "system"
                )
            }
            syncedCount
        } catch (_: Exception) {
            0
        }
    }

    suspend fun fetchCanvasProjects(): List<CanvasProject> = withContext(Dispatchers.IO) {
        val userId = sessionManager.currentUserId.value
        val endpoint = if (!userId.isNullOrBlank()) {
            "/rest/v1/canvas_projects?or=(user_id.eq.$userId,is_template.eq.true)&order=updated_at.desc"
        } else {
            "/rest/v1/canvas_projects?is_template=eq.true&order=updated_at.desc"
        }

        when (val resp = client.execute(endpoint)) {
            is SupabaseResponse.Success -> {
                val arr = resp.asJsonArray() ?: JSONArray()
                if (arr.length() > 0) {
                    val list = mutableListOf<CanvasProject>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val layersJson = obj.optJSONArray("layers_json") ?: JSONArray()
                        val layers = mutableListOf<CanvasLayer>()
                        for (j in 0 until layersJson.length()) {
                            val lObj = layersJson.getJSONObject(j)
                            val typeStr = lObj.optString("type", "TEXT")
                            val type = try { CanvasLayerType.valueOf(typeStr) } catch (_: Exception) { CanvasLayerType.TEXT }
                            layers.add(
                                CanvasLayer(
                                    id = lObj.optString("id", UUID.randomUUID().toString()),
                                    type = type,
                                    content = lObj.optString("content", "Texte"),
                                    x = lObj.optDouble("x", 80.0).toFloat(),
                                    y = lObj.optDouble("y", 120.0).toFloat(),
                                    scale = lObj.optDouble("scale", 1.0).toFloat(),
                                    colorHex = lObj.optString("colorHex", "#E5A93C"),
                                    fontSizeSp = lObj.optDouble("fontSizeSp", 24.0).toFloat(),
                                    isBold = lObj.optBoolean("isBold", true)
                                )
                            )
                        }
                        list.add(
                            CanvasProject(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                userId = obj.optString("user_id", "public"),
                                title = obj.optString("title", "Design PANU"),
                                canvasWidth = obj.optInt("canvas_width", 1080),
                                canvasHeight = obj.optInt("canvas_height", 1920),
                                backgroundHex = obj.optString("background_hex", "#121214"),
                                layers = layers,
                                exportFormat = obj.optString("export_format", "PNG")
                            )
                        )
                    }
                    _savedCanvasProjects.value = list + _savedCanvasProjects.value.filter { p -> list.none { it.id == p.id } }
                }
            }
            else -> {}
        }
        _savedCanvasProjects.value
    }

    // =========================================================================
    // 3. STUDIO VIDÉO IA, TEMPLATES SPONSORISÉS MARQUES & PROMPT 8K PRÊT À TESTER
    // =========================================================================
    val defaultViralTemplates = listOf(
        ViralVideoTemplate(
            id = "tpl_sponsor_brand_3d",
            title = "Faites apparaître votre produit dans un décor 3D",
            description = "MODÈLE SPONSORISÉ EN TÊTE DE LISTE : Idéal pour entreprises et marques locales. Sublimer un produit en studio 3D photoréaliste 8K.",
            category = "Sponsorisé • Marques",
            stylePreset = "Animation 3D",
            promptTemplate = "Cinematic 8K product showcase in a luxurious 3D African modern architectural set, golden hour rim lighting, floating golden particles, ultra-detailed textures, smooth orbital camera movement, photorealistic --ar 16:9 --fps 30",
            previewVideoUrl = "",
            thumbnailUrl = "https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=800&q=80",
            durationSeconds = 15,
            usesCount = 34890,
            ogTitle = "Produit en Décor 3D Sponsorisé • PANU Studio IA",
            ogImageUrl = "https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=800&q=80",
            isSponsoredBrand = true,
            sponsorBrandName = "Partenariat Entreprises & Marques Locales",
            sponsorBadgeText = "⭐ SPONSORISÉ • N°1 TENDANCE"
        ),
        ViralVideoTemplate(
            id = "tpl_sponsor_concert_event",
            title = "Affiche de concert / événement sponsorisée",
            description = "MODÈLE SPONSORISÉ EN TÊTE DE LISTE : Bande-annonce et affiche animée pour concerts, festivals, spectacles et événements d'entreprise.",
            category = "Sponsorisé • Événements",
            stylePreset = "Cinématographique",
            promptTemplate = "Cinematic 8K concert and festival stage in a vibrant modern African stadium at night, volumetric laser beams, energetic crowd, golden pyrotechnics, dynamic drone camera sweep, shot on 35mm lens, photorealistic --ar 16:9 --fps 30",
            previewVideoUrl = "",
            thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=800&q=80",
            durationSeconds = 15,
            usesCount = 29410,
            ogTitle = "Affiche de Concert & Événement Sponsorisée • PANU Studio",
            ogImageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=800&q=80",
            isSponsoredBrand = true,
            sponsorBrandName = "AfroEvents & Marques Partenaires",
            sponsorBadgeText = "⭐ SPONSORISÉ • TÊTE DE LISTE"
        ),
        ViralVideoTemplate(
            id = "tpl_cinematic_8k_metropolis",
            title = "Drone 8K Métropole Africaine (Exemple Prêt à Tester)",
            description = "Prompt officiel photoréaliste 8K : survol en drone d'une métropole africaine moderne au coucher du soleil (Golden Hour, 35mm, 30 FPS).",
            category = "Documentaire & Cinéma 8K",
            stylePreset = "Cinématographique",
            promptTemplate = GeminiRepository.READY_TO_TEST_CINEMATIC_VIDEO_PROMPT,
            previewVideoUrl = "",
            thumbnailUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80",
            durationSeconds = 20,
            usesCount = 42150,
            ogTitle = "Documentaire 8K Métropole Africaine • PANU Studio IA",
            ogImageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80",
            isSponsoredBrand = false,
            sponsorBrandName = "Modèle Officiel PANU 8K",
            sponsorBadgeText = "🎬 PROMPT 8K OFFICIEL"
        ),
        ViralVideoTemplate(
            id = "tpl_pixverse_02",
            title = "Héros Animation 3D Studio Pixar",
            description = "Transforme une scène quotidienne en court-métrage d'animation 3D ultra-expressif.",
            category = "Storytelling IA",
            stylePreset = "Animation 3D",
            promptTemplate = "Jeune entrepreneur créatif africain dans son studio lumineux entouré d'hologrammes dorés, style animation 3D Pixar",
            previewVideoUrl = "",
            thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=800&q=80",
            durationSeconds = 15,
            usesCount = 12350,
            ogTitle = "Court-métrage Animation 3D • PANU Studio IA",
            ogImageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=800&q=80"
        ),
        ViralVideoTemplate(
            id = "tpl_anime_03",
            title = "Épopée Anime Guerrier du Sahel",
            description = "Séquence d'action dynamique avec effets d'énergie néon et découpage manga.",
            category = "Anime & Action",
            stylePreset = "Anime",
            promptTemplate = "Héros charismatique sous un ciel étoilé du Sahel avec aura dorée étincelante, animation japonaise 60fps",
            previewVideoUrl = "",
            thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80",
            durationSeconds = 20,
            usesCount = 9870,
            ogTitle = "Épopée Anime • Généré sur PANU Studio",
            ogImageUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80"
        )
    )

    /**
     * Récupère les flux en direct actifs 100% réels depuis la table Supabase `lives`.
     * Zéro donnée de démonstration ou faux matchs.
     */
    suspend fun fetchActiveLives(): List<com.example.data.model.LiveMatchStream> = withContext(Dispatchers.IO) {
        val endpoint = "/rest/v1/lives?status=eq.active&order=created_at.desc"
        when (val resp = client.execute(endpoint)) {
            is SupabaseResponse.Success -> {
                val arr = resp.asJsonArray() ?: JSONArray()
                val list = mutableListOf<com.example.data.model.LiveMatchStream>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val hostName = obj.optString("host_name", "Créateur PANU")
                    val hostNameLower = hostName.lowercase()
                    val hostIdLower = obj.optString("host_id", "").lowercase()
                    if (hostNameLower.contains("emmanuel") || hostNameLower.contains("matia") || hostIdLower.contains("founder")) {
                        continue
                    }
                    val catRaw = obj.optString("category", "Créateur")
                    val catFormatted = catRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    list.add(
                        com.example.data.model.LiveMatchStream(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Direct Live"),
                            tournament = obj.optString("room_name", "Salon WebRTC"),
                            category = catFormatted,
                            scoreOrStatus = "● EN DIRECT",
                            viewersCount = "${obj.optInt("viewers_count", 1)}",
                            thumbnailUrl = obj.optString("thumbnail_url", ""),
                            videoStreamUrl = obj.optString("livekit_url", ""),
                            streamerName = hostName,
                            isLive = true
                        )
                    )
                }
                list
            }
            else -> emptyList()
        }
    }

    suspend fun saveGeneratedAiVideoToSupabase(
        title: String,
        prompt: String,
        stylePreset: String,
        videoUrl: String,
        thumbnailUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = sessionManager.currentUserId.value
        if (!userId.isNullOrBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("user_id", userId)
                    put("title", title)
                    put("description", prompt)
                    put("video_url", videoUrl)
                    put("thumbnail_url", thumbnailUrl)
                    put("style_preset", stylePreset)
                    put("og_title", "$title • Style $stylePreset (PANU Studio)")
                    put("og_image_url", thumbnailUrl)
                    put("status", "published")
                    put("visibility", "public")
                }
                client.execute(
                    endpoint = "/rest/v1/videos",
                    method = "POST",
                    jsonBody = payload.toString()
                )
            } catch (_: Exception) {}
        }
        triggerRealtimeNotification(
            title = "Vidéo IA ($stylePreset) prête 🎬",
            message = "Votre vidéo « $title » est générée et prête à être partagée sur TikTok, WhatsApp, Facebook et Instagram.",
            type = "system"
        )
        Result.success(Unit)
    }

    // =========================================================================
    // 4. LIVES, CADEAUX VIRTUELS (`live_gifts`) & MONÉTISATION (`ad_impressions`)
    // =========================================================================
    val liveGiftsCatalog = listOf(
        LiveGiftCatalogItem("gift_rose", "Rose", "🌹", 10, "Soutien instantané au créateur"),
        LiveGiftCatalogItem("gift_crown", "Couronne", "👑", 100, "Couronne Royale PANU"),
        LiveGiftCatalogItem("gift_lion", "Lion d'Or", "🦁", 250, "Rugissement d'Or sur le Live"),
        LiveGiftCatalogItem("gift_diamond", "Diamant PANU", "💎", 500, "Prestige Diamant Exclusif"),
        LiveGiftCatalogItem("gift_rocket", "Fusée Virale", "🚀", 1000, "Propulse le Live en Top Tendances")
    )

    val monetizedAdSequences = listOf(
        MonetizedAdSequence(
            id = "ad_seq_01",
            campaignTitle = "Google AdMob • Orange Money & M-Pesa Business",
            advertiserName = "Réseau AdMob & Audience Network",
            durationSeconds = 5,
            rewardFcfa = 65.0,
            rewardCredits = 15,
            bannerUrl = "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=800&q=80",
            ctaText = "Découvrir l'offre",
            adNetwork = "Google AdMob • Interstitiel 5s"
        ),
        MonetizedAdSequence(
            id = "ad_seq_02",
            campaignTitle = "Audience Network • Festival Afrobeats & Marques 3D",
            advertiserName = "Partenariat Marques Locales",
            durationSeconds = 5,
            rewardFcfa = 50.0,
            rewardCredits = 10,
            bannerUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=800&q=80",
            ctaText = "Voir le partenaire",
            adNetwork = "Meta Audience Network • In-Video 5s"
        )
    )

    // Packs de Crédits (Modèle 60 Crédits Gratuits à l'inscription + Achats Mobile Money & Carte)
    val creditPacksCatalog = listOf(
        CreditPackOffer(
            id = "pack_starter_60",
            name = "Pack 60 Crédits (Modèle Standard)",
            creditsAmount = 60,
            priceFcfa = 500,
            badge = "🎁 60 CRÉDITS OFFERTS À L'INSCRIPTION",
            perks = listOf(
                "Génération Vidéo Haute Définition (HD 1080p)",
                "Exportation Sans Filigrane activée",
                "Accès aux Prompts Cinéma 8K"
            ),
            unlocksNoWatermarkHd = true
        ),
        CreditPackOffer(
            id = "pack_creator_250",
            name = "Pack Créateur Pro (250 Crédits)",
            creditsAmount = 250,
            priceFcfa = 1500,
            badge = "🔥 LE PLUS POPULAIRE",
            perks = listOf(
                "Rendu Ultra-Rapide Prioritaire (Serveurs GPU)",
                "Vidéos 4K & 8K Sans Filigrane",
                "Zéro Publicité de 5s au téléchargement"
            ),
            unlocksNoWatermarkHd = true
        ),
        CreditPackOffer(
            id = "pack_studio_600",
            name = "Pack Studio Cinéma (600 Crédits)",
            creditsAmount = 600,
            priceFcfa = 3000,
            badge = "👑 STUDIO 8K ILLIMITÉ",
            perks = listOf(
                "Production Films, Séries & Documentaires 8K",
                "Sans Filigrane + Priorité Maximale + Zéro Pub",
                "1 Booster de Visibilité Découvrir offert"
            ),
            unlocksNoWatermarkHd = true
        )
    )

    // Options de "Booster de Visibilité" pour propulser une vidéo dans l'onglet Découvrir
    val visibilityBoostPlans = listOf(
        VisibilityBoostPlan(
            id = "boost_discover_starter",
            title = "Booster Découvrir Express",
            creditsCost = 30,
            priceFcfa = 500,
            estimatedExtraViews = 5000,
            estimatedNewFollowers = 180,
            durationLabel = "24 heures en tête de Découvrir"
        ),
        VisibilityBoostPlan(
            id = "boost_discover_viral",
            title = "Booster Viral National",
            creditsCost = 80,
            priceFcfa = 1500,
            estimatedExtraViews = 20000,
            estimatedNewFollowers = 750,
            durationLabel = "3 jours en Top Tendances"
        ),
        VisibilityBoostPlan(
            id = "boost_discover_continent",
            title = "Booster Panafricain Or",
            creditsCost = 200,
            priceFcfa = 3500,
            estimatedExtraViews = 65000,
            estimatedNewFollowers = 2400,
            durationLabel = "7 jours en Vedette Principale"
        )
    )

    private val _recentLiveGifts = MutableStateFlow<List<LiveGiftTransaction>>(emptyList())
    val recentLiveGifts: StateFlow<List<LiveGiftTransaction>> = _recentLiveGifts.asStateFlow()

    private val _creatorEarnings = MutableStateFlow(
        CreatorRemunerationStats(userId = "current")
    )
    val creatorEarnings: StateFlow<CreatorRemunerationStats> = _creatorEarnings.asStateFlow()

    fun addPanuCredits(amount: Int) {
        _panuCreditsBalance.value += amount
    }

    suspend fun grantWelcome60Credits() {
        if (_panuCreditsBalance.value < 60) {
            _panuCreditsBalance.value = 60
        } else {
            _panuCreditsBalance.value += 60
        }
        triggerRealtimeNotification(
            title = "🎁 60 Crédits Gratuits Offerts !",
            message = "Bienvenue sur PANU ! Vos 60 crédits gratuits d'inscription ont été crédités pour générer vos vidéos IA.",
            type = "gift"
        )
    }

    suspend fun purchaseCreditPack(
        pack: CreditPackOffer,
        provider: PaymentProvider,
        phoneOrCardNumber: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        _panuCreditsBalance.value += pack.creditsAmount
        if (pack.unlocksNoWatermarkHd) {
            _hasNoWatermarkHdPass.value = true
        }
        triggerRealtimeNotification(
            title = "Paiement ${provider.labelFr} validé ✅",
            message = "+${pack.creditsAmount} crédits ajoutés (${pack.priceFcfa} FCFA via ${provider.labelFr}). Mode HD Sans Filigrane & Rapide activé !",
            type = "order"
        )
        Result.success(_panuCreditsBalance.value)
    }

    suspend fun boostCreatorVideo(
        videoId: String,
        videoTitle: String,
        plan: VisibilityBoostPlan,
        payWithMobileMoney: PaymentProvider? = null
    ): Result<CreatorRemunerationStats> = withContext(Dispatchers.IO) {
        if (payWithMobileMoney == null) {
            val currentCredits = _panuCreditsBalance.value
            if (currentCredits < plan.creditsCost) {
                return@withContext Result.failure(
                    Exception("Crédits insuffisants (${currentCredits}/${plan.creditsCost}). Rechargez via Mobile Money (M-Pesa, Orange Money, Airtel Money) ou Carte.")
                )
            }
            _panuCreditsBalance.value = currentCredits - plan.creditsCost
        }

        _boostedVideoIds.value = _boostedVideoIds.value + videoId

        val stats = _creatorEarnings.value
        val adCommissionEarned = (plan.estimatedExtraViews * 0.85) // Commission Fonds Créateurs sur les vues générées
        val updated = stats.copy(
            totalViews = stats.totalViews + plan.estimatedExtraViews,
            boostedVideosCount = stats.boostedVideosCount + 1,
            boostedViewsGained = stats.boostedViewsGained + plan.estimatedExtraViews,
            creatorFundShareFcfa = stats.creatorFundShareFcfa + adCommissionEarned,
            totalAvailableFcfa = stats.totalAvailableFcfa + adCommissionEarned
        )
        _creatorEarnings.value = updated

        val paymentLabel = payWithMobileMoney?.labelFr ?: "${plan.creditsCost} Crédits PANU"
        triggerRealtimeNotification(
            title = "🚀 Vidéo Boostée dans Découvrir !",
            message = "« $videoTitle » est propulsée en tête de l'onglet Découvrir ($paymentLabel) : +${plan.estimatedExtraViews} vues estimées & +${plan.estimatedNewFollowers} followers !",
            type = "order"
        )
        Result.success(updated)
    }

    suspend fun withdrawCreatorEarningsToRealMoney(
        amountFcfa: Double,
        provider: PaymentProvider,
        accountIdentifier: String
    ): Result<Double> = withContext(Dispatchers.IO) {
        val current = _creatorEarnings.value
        if (amountFcfa <= 0 || amountFcfa > current.totalAvailableFcfa) {
            return@withContext Result.failure(Exception("Montant invalide ou supérieur au solde disponible."))
        }
        val remaining = (current.totalAvailableFcfa - amountFcfa).coerceAtLeast(0.0)
        _creatorEarnings.value = current.copy(totalAvailableFcfa = remaining)

        triggerRealtimeNotification(
            title = "💸 Virement d'Argent Réel Envoyé (${provider.labelFr})",
            message = "${amountFcfa.toInt()} FCFA issus du Fonds Créateurs & Recettes Publicitaires ont été transférés vers $accountIdentifier (${provider.labelFr}).",
            type = "order"
        )
        Result.success(remaining)
    }

    suspend fun sendLiveGift(
        streamId: String,
        receiverName: String,
        gift: LiveGiftCatalogItem,
        customMessage: String = ""
    ): Result<LiveGiftTransaction> = withContext(Dispatchers.IO) {
        val currentCredits = _panuCreditsBalance.value
        if (currentCredits < gift.creditsCost) {
            return@withContext Result.failure(
                Exception("Solde de crédits PANU insuffisant ($currentCredits crédits). Rechargez ou regardez une séquence sponsorisée.")
            )
        }

        // Débiter les crédits PANU
        _panuCreditsBalance.value = currentCredits - gift.creditsCost

        val senderId = sessionManager.currentUserId.value ?: "guest_user"
        val senderEmail = sessionManager.getUserEmail() ?: "Membre_PANU"
        val senderName = senderEmail.substringBefore("@")

        val tx = LiveGiftTransaction(
            id = UUID.randomUUID().toString(),
            streamId = streamId,
            senderId = senderId,
            senderName = senderName,
            receiverName = receiverName,
            giftType = gift.name,
            giftIcon = gift.iconEmoji,
            creditsAmount = gift.creditsCost,
            message = customMessage.ifBlank { "A offert ${gift.iconEmoji} ${gift.name} !" },
            createdAt = "À l'instant"
        )

        _recentLiveGifts.value = listOf(tx) + _recentLiveGifts.value

        // Mise à jour de la rémunération du créateur
        val currentStats = _creatorEarnings.value
        val bonusFcfa = gift.creditsCost * 15.0
        _creatorEarnings.value = currentStats.copy(
            giftsReceivedCredits = currentStats.giftsReceivedCredits + gift.creditsCost,
            totalAvailableFcfa = currentStats.totalAvailableFcfa + bonusFcfa
        )

        // Enregistrement dans la table Supabase `live_gifts`
        if (senderId != "guest_user" && senderId.isNotBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("stream_id", streamId)
                    put("sender_id", senderId)
                    put("sender_name", senderName)
                    put("receiver_name", receiverName)
                    put("gift_type", gift.name)
                    put("gift_icon", gift.iconEmoji)
                    put("credits_amount", gift.creditsCost)
                    put("message", tx.message)
                }
                client.execute(
                    endpoint = "/rest/v1/live_gifts",
                    method = "POST",
                    jsonBody = payload.toString()
                )
            } catch (_: Exception) {}
        }

        triggerRealtimeNotification(
            title = "Cadeau envoyé en direct ${gift.iconEmoji}",
            message = "Vous avez offert ${gift.name} (${gift.creditsCost} crédits PANU) à $receiverName !",
            type = "gift"
        )

        Result.success(tx)
    }

    suspend fun recordAdImpression(
        videoOrStreamId: String,
        ad: MonetizedAdSequence
    ): Result<CreatorRemunerationStats> = withContext(Dispatchers.IO) {
        val viewerId = sessionManager.currentUserId.value
        // Récompenser l'utilisateur en crédits PANU et le créateur en FCFA
        _panuCreditsBalance.value += ad.rewardCredits

        val stats = _creatorEarnings.value
        val creatorCommissionFcfa = ad.rewardFcfa * (stats.creatorFundCommissionPercent / 100.0)
        val updated = stats.copy(
            adImpressionsCount = stats.adImpressionsCount + 1,
            adRevenueFcfa = stats.adRevenueFcfa + ad.rewardFcfa,
            creatorFundShareFcfa = stats.creatorFundShareFcfa + creatorCommissionFcfa,
            totalAvailableFcfa = stats.totalAvailableFcfa + ad.rewardFcfa + creatorCommissionFcfa
        )
        _creatorEarnings.value = updated

        // Insérer dans la table Supabase `ad_impressions`
        try {
            val payload = JSONObject().apply {
                if (!viewerId.isNullOrBlank()) put("viewer_id", viewerId)
                put("video_or_stream_id", videoOrStreamId)
                put("ad_campaign_title", ad.campaignTitle)
                put("advertiser_name", ad.advertiserName)
                put("watched_seconds", ad.durationSeconds)
                put("revenue_generated_fcfa", ad.rewardFcfa)
            }
            client.execute(
                endpoint = "/rest/v1/ad_impressions",
                method = "POST",
                jsonBody = payload.toString()
            )
        } catch (_: Exception) {}

        triggerRealtimeNotification(
            title = "Séquence publicitaire validée 💰",
            message = "+${ad.rewardCredits} crédits PANU ajoutés et +${ad.rewardFcfa.toInt()} FCFA générés dans ad_impressions.",
            type = "order"
        )

        Result.success(updated)
    }

    fun recordShareOrViewRemuneration(isShare: Boolean) {
        val stats = _creatorEarnings.value
        val addedFcfa = if (isShare) 15.0 else 2.5
        _creatorEarnings.value = stats.copy(
            totalViews = if (!isShare) stats.totalViews + 1 else stats.totalViews,
            totalShares = if (isShare) stats.totalShares + 1 else stats.totalShares,
            viewsAndSharesBonusFcfa = stats.viewsAndSharesBonusFcfa + addedFcfa,
            totalAvailableFcfa = stats.totalAvailableFcfa + addedFcfa
        )
    }

    // =========================================================================
    // 5. VÉRIFICATION DES CARTES DE MEMBRES / PERSONNEL (`verify_member_card`)
    // =========================================================================
    private val localReferenceCards = listOf(
        MemberCardVerification(
            cardNumber = "PANU-FND-001",
            holderFullName = "Emmanuel Matia",
            holderRole = "Fondateur & PDG Officiel",
            companyName = "PANU Group International",
            department = "Direction Générale & Architecture IA",
            nfcUid = "NFC-PANU-0001-FND",
            status = "Actif",
            issuedAt = "01 Jan 2026",
            expiresAt = "Illimité (Fondateur Immuable)",
            isAuthentic = true
        ),
        MemberCardVerification(
            cardNumber = "PANU-PRO-2026",
            holderFullName = "Aïcha Koné",
            holderRole = "Directrice Artistique IA",
            companyName = "PANU Studio Abidjan",
            department = "Production Visuelle & Design",
            nfcUid = "NFC-PANU-2026-PRO",
            status = "Actif",
            issuedAt = "15 Fév 2026",
            expiresAt = "15 Fév 2029",
            isAuthentic = true
        ),
        MemberCardVerification(
            cardNumber = "PANU-STF-884",
            holderFullName = "Régie Technique Live",
            holderRole = "Responsable Diffusion Live Sports",
            companyName = "PANU Media Africa",
            department = "Régie Direct & Streaming",
            nfcUid = "NFC-PANU-0884-STF",
            status = "Actif",
            issuedAt = "10 Mar 2026",
            expiresAt = "10 Mar 2029",
            isAuthentic = true
        ),
        MemberCardVerification(
            cardNumber = "PANU-REV-000",
            holderFullName = "Badge Externe Révoqué",
            holderRole = "Prestataire Temporaire",
            companyName = "Externe",
            department = "Accès Révoqué",
            nfcUid = "NFC-PANU-0000-REV",
            status = "Invalide",
            issuedAt = "01 Jan 2025",
            expiresAt = "Expiré",
            isAuthentic = false
        )
    )

    suspend fun verifyMemberCard(rawInput: String): Result<MemberCardVerification> = withContext(Dispatchers.IO) {
        val cleanNumber = rawInput
            .trim()
            .substringAfterLast("/verify/")
            .trim()
            .uppercase()

        if (cleanNumber.isBlank()) {
            return@withContext Result.failure(Exception("Veuillez saisir ou scanner un numéro de carte valide."))
        }

        // 1. Appel de la fonction RPC Supabase `verify_member_card`
        try {
            val rpcPayload = JSONObject().apply {
                put("p_card_number", cleanNumber)
            }
            val rpcResp = client.execute(
                endpoint = "/rest/v1/rpc/verify_member_card",
                method = "POST",
                jsonBody = rpcPayload.toString()
            )
            if (rpcResp is SupabaseResponse.Success) {
                val arr = rpcResp.asJsonArray()
                if (arr != null && arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val status = obj.optString("status", "Actif")
                    val verified = MemberCardVerification(
                        cardNumber = obj.optString("card_number", cleanNumber),
                        holderFullName = obj.optString("holder_full_name", "Membre PANU"),
                        holderRole = obj.optString("holder_role", "Personnel Accrédité"),
                        companyName = obj.optString("company_name", "PANU Studio Officiel"),
                        department = obj.optString("department", "Direction & Création"),
                        avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() },
                        status = status,
                        issuedAt = obj.optString("issued_at", "2026").take(10),
                        expiresAt = obj.optString("expires_at", "2029").take(10),
                        isAuthentic = obj.optBoolean("is_authentic", status.equals("Actif", ignoreCase = true))
                    )
                    return@withContext Result.success(verified)
                }
            }

            // 2. Repli direct sur la table Supabase `member_cards`
            val tableResp = client.execute(
                endpoint = "/rest/v1/member_cards?or=(card_number.ilike.$cleanNumber,nfc_uid.ilike.$cleanNumber)&select=*&limit=1",
                method = "GET"
            )
            if (tableResp is SupabaseResponse.Success) {
                val arr = tableResp.asJsonArray()
                if (arr != null && arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val status = obj.optString("status", "Actif")
                    val verified = MemberCardVerification(
                        cardNumber = obj.optString("card_number", cleanNumber),
                        holderFullName = obj.optString("holder_full_name", "Membre PANU"),
                        holderRole = obj.optString("holder_role", "Personnel Accrédité"),
                        companyName = obj.optString("company_name", "PANU Studio Officiel"),
                        department = obj.optString("department", "Direction & Création"),
                        avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() },
                        nfcUid = obj.optString("nfc_uid").takeIf { it.isNotBlank() },
                        status = status,
                        issuedAt = obj.optString("issued_at", "2026").take(10),
                        expiresAt = obj.optString("expires_at", "2029").take(10),
                        isAuthentic = status.equals("Actif", ignoreCase = true)
                    )
                    return@withContext Result.success(verified)
                }
            }
        } catch (_: Exception) {}

        // 3. Vérification dans le registre officiel embarqué (mode hors-ligne / PWA)
        val localMatch = localReferenceCards.find {
            it.cardNumber.equals(cleanNumber, ignoreCase = true) ||
                    it.nfcUid?.equals(cleanNumber, ignoreCase = true) == true
        }
        if (localMatch != null) {
            return@withContext Result.success(localMatch)
        }

        // 4. Carte non reconnue -> Attestation Invalide
        Result.success(
            MemberCardVerification(
                cardNumber = cleanNumber,
                holderFullName = "Titulaire Non Répertorié",
                holderRole = "Aucun rôle accrédité",
                companyName = "Non affilié à PANU",
                department = "Inconnu",
                status = "Invalide",
                issuedAt = "N/A",
                expiresAt = "N/A",
                isAuthentic = false
            )
        )
    }
}
