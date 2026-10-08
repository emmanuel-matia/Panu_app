package com.example.data.model

// 1. Modèle de Notification en Temps Réel
data class PanuNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val notificationType: String = "system", // auth, gift, order, like, comment, verification, system
    val isRead: Boolean = false,
    val createdAt: String = "À l'instant"
)

// 2. Modèle de Calque & Projet Studio Graphique PANU (table canvas_projects)
enum class CanvasLayerType(val labelFr: String) {
    TEXT("Texte"),
    IMAGE("Image"),
    STICKER("Sticker"),
    SHAPE("Forme")
}

data class CanvasLayer(
    val id: String,
    val type: CanvasLayerType,
    val content: String, // Texte, émoji sticker, URL image ou nom de forme
    val x: Float = 80f,
    val y: Float = 120f,
    val scale: Float = 1f,
    val colorHex: String = "#E5A93C",
    val fontSizeSp: Float = 24f,
    val isBold: Boolean = true
)

data class CanvasProject(
    val id: String,
    val userId: String,
    val title: String,
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1920,
    val backgroundHex: String = "#121214",
    val layers: List<CanvasLayer> = emptyList(),
    val exportFormat: String = "PNG", // PNG, JPG, MP4
    val previewUrl: String? = null,
    val updatedAt: String = "Maintenant"
)

// 3. Modèle de Template Vidéo Officiel PANU (table video_templates)
enum class VideoStylePreset(val labelFr: String, val badge: String, val promptEnhancer: String) {
    CINEMATIC(
        "Cinématographique",
        "🎬 Cinéma 8K",
        "Cinematic 8K shot, ultra-realistic, golden hour lighting, shot on 35mm lens, photorealistic, professional color grading"
    ),
    MODERN_ADS(
        "Publicité & Commercial",
        "📢 Commercial",
        "Modern luxury commercial advert, clean studio lighting, crisp product showcase, 4K UHD, engaging pacing"
    ),
    ANIMATION_3D(
        "Animation 3D",
        "🧊 3D Studio",
        "High quality 3D stylized render, smooth lighting, vibrant textures, studio look"
    ),
    ANIME(
        "Anime",
        "⚡ Anime",
        "Dynamic high-end animation style, sharp line art, fluid motion, vivid colors"
    ),
    REALISTIC(
        "Réaliste & Documentaire",
        "📹 Réaliste",
        "Documentary realism, natural daylight, authentic atmosphere, natural textures"
    )
}

data class PanuTemplateScene(
    val index: Int,
    val name: String,
    val durationSeconds: Int = 5,
    val defaultText: String = "",
    val visualPrompt: String = "",
    val transition: String = "Fondu enchaîné"
)

data class PanuTemplateField(
    val key: String,
    val label: String,
    val placeholder: String = "",
    val isMultiline: Boolean = false,
    val isImage: Boolean = false
)

data class PanuTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // 🔥 Tendances, 📱 TikTok / Shorts / Reels, 🎬 Cinéma, 📢 Publicité, 🛍️ Produits & commerces, 🎵 Musique, 😂 Humour, 🙏 Église & événements, 🇨🇩 Afrique / RDC, 🎓 Formation, 💼 Entreprise
    val formatLabel: String = "Vertical (9:16)",
    val aspectRatio: String = "9:16", // 9:16, 16:9, 1:1
    val durationSeconds: Int = 15,
    val durationLabel: String = "15\"",
    val previewVideoUrl: String = "",
    val thumbnailUrl: String = "",
    val scenes: List<PanuTemplateScene> = emptyList(),
    val editableFields: List<PanuTemplateField> = emptyList(),
    val musicTrack: String? = null,
    val transitionEffect: String = "Fondu enchaîné cinématique",
    val animationEffect: String = "Zoom dynamique",
    val visualFilter: String = "Contraste chaud & Or",
    val basePrompt: String = "",
    val recommendedEngine: String = "MiniMax/Hailuo", // MiniMax/Hailuo, Kling, Luma, Flux/BFL
    val stylePreset: String = "Cinématographique"
)

data class ViralVideoTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val stylePreset: String,
    val promptTemplate: String,
    val previewVideoUrl: String,
    val thumbnailUrl: String,
    val durationSeconds: Int = 15,
    val ogTitle: String = "",
    val ogImageUrl: String = "",
    val formatLabel: String = "Vertical (9:16)",
    val aspectRatio: String = "9:16",
    val recommendedEngine: String = "MiniMax/Hailuo"
)

// 4. Modèle de Cadeau Virtuel Live (table live_gifts)
data class LiveGiftCatalogItem(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val creditsCost: Int,
    val description: String
)

data class LiveGiftTransaction(
    val id: String,
    val streamId: String,
    val senderId: String,
    val senderName: String,
    val receiverName: String,
    val giftType: String,
    val giftIcon: String,
    val creditsAmount: Int,
    val message: String = "",
    val createdAt: String = "À l'instant"
)

// 5. Modèle de Séquence Publicitaire, Booster, Mobile Money & Fonds Créateurs
data class MonetizedAdSequence(
    val id: String,
    val campaignTitle: String,
    val advertiserName: String,
    val durationSeconds: Int,
    val rewardFcfa: Double,
    val rewardCredits: Int,
    val bannerUrl: String,
    val ctaText: String,
    val adNetwork: String = "Google AdMob • Audience Network"
)

data class CreditPackOffer(
    val id: String,
    val name: String,
    val creditsAmount: Int,
    val priceFcfa: Int,
    val badge: String,
    val perks: List<String>,
    val unlocksNoWatermarkHd: Boolean = true
)

enum class PaymentProvider(val labelFr: String, val badgeEmoji: String, val feeNote: String) {
    ORANGE_MONEY("Orange Money", "🟠", "Validation instantanée USSD / Mobile"),
    MPESA("M-Pesa", "🟢", "Paiement mobile instantané sécurisé"),
    AIRTEL_MONEY("Airtel Money", "🔴", "Mobile Money Afrique Centrale & Est"),
    BANK_CARD("Carte Bancaire (Visa / Mastercard)", "💳", "Paiement 3D Secure International")
}

data class VisibilityBoostPlan(
    val id: String,
    val title: String,
    val creditsCost: Int,
    val priceFcfa: Int,
    val estimatedExtraViews: Int,
    val estimatedNewFollowers: Int,
    val durationLabel: String
)

data class CreatorRemunerationStats(
    val userId: String,
    val totalViews: Long = 18450,
    val totalShares: Long = 2190,
    val giftsReceivedCredits: Int = 680,
    val adImpressionsCount: Int = 342,
    val adRevenueFcfa: Double = 21500.0,
    val viewsAndSharesBonusFcfa: Double = 45200.0,
    val creatorFundCommissionPercent: Int = 55,
    val creatorFundShareFcfa: Double = 28400.0,
    val boostedVideosCount: Int = 2,
    val boostedViewsGained: Long = 12500,
    val totalAvailableFcfa: Double = 95100.0
)

// 6. Modèle d'Attestation de Carte de Membre / Personnel (table member_cards & RPC verify_member_card)
data class MemberCardVerification(
    val cardNumber: String,
    val holderFullName: String,
    val holderRole: String,
    val companyName: String,
    val department: String,
    val avatarUrl: String? = null,
    val nfcUid: String? = null,
    val status: String, // "Actif" ou "Invalide"
    val issuedAt: String = "2026",
    val expiresAt: String = "2029",
    val isAuthentic: Boolean = status.equals("Actif", ignoreCase = true)
)

// 7. Modèle de Flux Direct / Match Réel Supabase (table lives)
data class LiveMatchStream(
    val id: String,
    val title: String,
    val tournament: String,
    val category: String, // "Football", "Basketball", "Combat", "Créateur"
    val scoreOrStatus: String,
    val viewersCount: String,
    val thumbnailUrl: String,
    val videoStreamUrl: String,
    val streamerName: String,
    val isLive: Boolean = true
)
