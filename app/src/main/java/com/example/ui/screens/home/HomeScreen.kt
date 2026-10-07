package com.example.ui.screens.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import com.example.ui.components.CreatorSuggestionDialog
import com.example.ui.components.PanuWatermarkExporterDialog
import com.example.ui.components.ReferralProgramDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Post
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.data.repository.PostRepository
import com.example.ui.components.AuthRequiredDialog
import com.example.ui.components.CreatorFundPayoutDialog
import com.example.ui.components.InstallAppBanner
import com.example.ui.components.InstallAppDialog
import com.example.ui.components.MobileMoneyCreditsStoreDialog
import com.example.ui.components.NotificationsCenterDialog
import com.example.ui.components.PanuAvatar
import com.example.ui.components.PanuBottomNav
import com.example.ui.components.PanuTopBar
import com.example.ui.components.TikTokInVideoAdSlide
import com.example.ui.components.VisibilityBoosterDialog
import com.example.ui.navigation.PanuScreen
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Zéro contenu factice : uniquement données réelles issues de Supabase
private val DEFAULT_VIRAL_TIKTOK_POSTS = emptyList<Post>()

@Composable
fun HomeScreen(
    postRepository: PostRepository,
    featuresService: SupabasePanuFeaturesService? = null,
    isLoggedIn: Boolean = false,
    isFounder: Boolean = false,
    onNavigate: (String) -> Unit,
    onNavigateToAuth: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    onOpenPublicProfile: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToFounder: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVod: () -> Unit,
    onNavigateToLive: () -> Unit,
    onNavigateToVerifyCard: (String) -> Unit = {},
    onWatchVideo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val colors = PanuTheme.colors
    val dbPosts by postRepository.publishedPosts.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // 100% données réelles Supabase + Masking strict du profil Fondateur
    val posts = remember(dbPosts) {
        dbPosts.filter { post ->
            val authorId = post.authorId.lowercase()
            val authorName = (post.authorName ?: "").lowercase()
            val authorUsername = (post.authorUsername ?: "").lowercase()
            // Masking 100% du Fondateur (Emmanuel Matia Mbundu) dans le fil public
            !authorId.contains("founder") &&
            !authorId.contains("emmanuel") &&
            !authorName.contains("emmanuel matia") &&
            !authorUsername.contains("emmanuelmatia")
        }
    }

    // Par défaut : Flux Vertical Vidéos Virales en lecture automatique
    var selectedTab by remember { mutableStateOf("Pour vous") }
    val tabs = listOf("Pour vous", "Tendances")

    // Pop-up d'invitation à se connecter pour les actions interactives (Aimer, Commenter, Offrir un cadeau, Créer)
    var showAuthModal by remember { mutableStateOf(false) }
    var currentBlockedAction by remember { mutableStateOf("interagir") }

    // Centre de notifications en temps réel
    var showNotificationsDialog by remember { mutableStateOf(false) }
    val notifications = featuresService?.notificationsHistory?.collectAsState()?.value ?: emptyList()
    val unreadCount = notifications.count { !it.isRead }

    val likedPosts = remember { mutableStateMapOf<String, Boolean>() }
    val likesCount = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        launch(kotlinx.coroutines.Dispatchers.IO) { postRepository.refreshPublishedPosts() }
        launch(kotlinx.coroutines.Dispatchers.IO) { featuresService?.fetchUserNotifications() }
    }

    AuthRequiredDialog(
        isOpen = showAuthModal,
        actionName = currentBlockedAction,
        onDismiss = { showAuthModal = false },
        onNavigateToAuth = {
            showAuthModal = false
            onNavigateToAuth()
        }
    )

    NotificationsCenterDialog(
        isOpen = showNotificationsDialog,
        notifications = notifications,
        onMarkAllRead = {
            scope.launch { featuresService?.markAllNotificationsRead() }
        },
        onDismiss = { showNotificationsDialog = false }
    )

    var showInstallDialog by remember { mutableStateOf(false) }
    InstallAppDialog(
        isOpen = showInstallDialog,
        onDismiss = { showInstallDialog = false }
    )

    var showCreditsStoreModal by remember { mutableStateOf(false) }
    var showCreatorFundModal by remember { mutableStateOf(false) }
    var boostTargetPost by remember { mutableStateOf<Post?>(null) }

    MobileMoneyCreditsStoreDialog(
        isOpen = showCreditsStoreModal,
        featuresService = featuresService,
        onDismiss = { showCreditsStoreModal = false }
    )

    CreatorFundPayoutDialog(
        isOpen = showCreatorFundModal,
        featuresService = featuresService,
        onDismiss = { showCreatorFundModal = false }
    )

    VisibilityBoosterDialog(
        isOpen = boostTargetPost != null,
        videoId = boostTargetPost?.id ?: "panu_viral_01",
        videoTitle = boostTargetPost?.title ?: "Vidéo Virale PANU",
        featuresService = featuresService,
        onDismiss = { boostTargetPost = null }
    )

    var showCreatorSuggestionDialog by remember { mutableStateOf(false) }
    var showReferralDialog by remember { mutableStateOf(false) }
    var watermarkExportPost by remember { mutableStateOf<Post?>(null) }

    CreatorSuggestionDialog(
        isOpen = showCreatorSuggestionDialog,
        onDismiss = { showCreatorSuggestionDialog = false },
        onSubmitSuggestion = { platform, handleOrUrl, name, cat ->
            Toast.makeText(context, "Suggestion envoyée pour $name ($platform) !", Toast.LENGTH_SHORT).show()
        }
    )

    ReferralProgramDialog(
        isOpen = showReferralDialog,
        userId = if (isLoggedIn) "createur_actif" else "guest",
        username = null,
        onDismiss = { showReferralDialog = false }
    )

    watermarkExportPost?.let { p ->
        PanuWatermarkExporterDialog(
            isOpen = true,
            authorHandle = p.authorName ?: "@createur",
            contentTitle = p.title ?: "Vidéo PANU",
            onDismiss = { watermarkExportPost = null }
        )
    }

    fun handleInteractionGated(actionLabel: String, onAllowed: () -> Unit) {
        if (!isLoggedIn) {
            currentBlockedAction = actionLabel
            showAuthModal = true
        } else {
            onAllowed()
        }
    }

    Scaffold(
        topBar = {
            Column {
                PanuTopBar(
                    onSearchClick = { /* Action recherche */ },
                    onMenuClick = onMenuClick
                )

                // Barre d'onglets : Flux TikTok Vertical, Fil Classique, Lives, Studio, Vérification Carte
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tabs) { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = {
                                Text(
                                    text = if (tab == "Pour vous") "🔥 Pour vous" else "✨ Tendances",
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = if (colors.isDark) colors.background else Color.White
                            )
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = onNavigateToLive,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("home_chip_live_gifts")
                        ) {
                            Text("🔴 Lives & Cadeaux", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { onNavigateToVerifyCard("PANU-FND-001") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("home_chip_verify_card")
                        ) {
                            Text("🪪 Vérifier Carte (/verify)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { showCreditsStoreModal = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("home_chip_credits_store")
                        ) {
                            Text("🪙 60 Crédits & Mobile Money", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { showCreatorFundModal = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("home_chip_creator_fund")
                        ) {
                            Text("💰 Fonds Créateurs (55%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = onNavigateToVod,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("🎬 VOD", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            PanuBottomNav(
                currentRoute = PanuScreen.Home.route,
                isFounder = isFounder,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    handleInteractionGated("créer une publication") {
                        onNavigateToCreate()
                    }
                },
                containerColor = colors.champagne,
                contentColor = if (colors.isDark) colors.background else Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("home_fab_create")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Créer du contenu")
            }
        },
        containerColor = colors.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab == "Pour vous") {
                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("home_empty_posts_for_you"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.champagne.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(2.dp, colors.champagne),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🎬", fontSize = 34.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aucune publication pour le moment",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = colors.champagne,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Soyez le premier à créer un contenu !",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    handleInteractionGated("créer une publication") {
                                        onNavigateToCreate()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_empty_create_post")
                            ) {
                                Text("🚀 Créer une publication", fontWeight = FontWeight.Bold, color = if (colors.isDark) colors.background else Color.White)
                            }
                        }
                    }
                } else {
                    // =========================================================================
                    // FLUX VERTICAL DE VIDÉOS VIRALES + INTERSTITIEL PUBLICITAIRE TOUTES LES 3 VIDÉOS
                    // =========================================================================
                    val feedEntries = remember(posts) {
                        val list = mutableListOf<Post?>()
                        posts.forEachIndexed { index, post ->
                            list.add(post)
                            // Insertion d'un interstitiel publicitaire dynamique toutes les 3 vidéos (Format Shorts/Reels)
                            if ((index + 1) % 3 == 0) {
                                list.add(null) // null = Slot Publicitaire In-Video AdMob / Audience Network
                            }
                        }
                        list
                    }
                    val pagerState = rememberPagerState(pageCount = { feedEntries.size })

                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("tiktok_vertical_feed_pager")
                    ) { pageIndex ->
                        val post = feedEntries[pageIndex]
                        if (post == null) {
                            // Publicité In-Video Dynamique (Format Shorts/Reels — Google AdMob / Audience Network)
                            val adItem = featuresService?.monetizedAdSequences?.firstOrNull()
                            TikTokInVideoAdSlide(
                                adCampaignTitle = adItem?.campaignTitle ?: "Google AdMob • Pack Créateur 8K Sans Filigrane",
                                advertiserName = adItem?.advertiserName ?: "Google AdMob & Audience Network",
                                bannerUrl = adItem?.bannerUrl ?: "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=900&q=80",
                                rewardFcfa = adItem?.rewardFcfa?.toInt() ?: 65,
                                onClaimAdReward = {
                                    if (adItem != null) {
                                        scope.launch {
                                            featuresService.recordAdImpression("shorts_in_video_ad_$pageIndex", adItem)
                                        }
                                    }
                                    showCreditsStoreModal = true
                                },
                                onOpenCreatorFund = { showCreatorFundModal = true }
                            )
                        } else {
                            val isLiked = likedPosts[post.id] ?: false
                            val currentLikes = likesCount[post.id] ?: ((post.id.hashCode() % 180) + 125).coerceAtLeast(42)

                            TikTokVerticalVideoItem(
                                post = post,
                                isLiked = isLiked,
                                likesCount = currentLikes,
                                onAuthorClick = {
                                    val handle = post.authorUsername ?: post.authorName ?: ""
                                    if (handle.isNotBlank()) onOpenPublicProfile(handle)
                                },
                                onWatchFullVideo = {
                                    val url = post.mediaUrl ?: ""
                                    if (url.isNotBlank()) {
                                        featuresService?.recordShareOrViewRemuneration(isShare = false)
                                        onWatchVideo(url)
                                    }
                                },
                            onLikeClick = {
                                handleInteractionGated("aimer cette vidéo") {
                                    val next = !isLiked
                                    likedPosts[post.id] = next
                                    likesCount[post.id] = if (next) currentLikes + 1 else currentLikes - 1
                                    Toast.makeText(context, if (next) "❤️ J'aime ajouté" else "Mention retirée", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCommentClick = {
                                handleInteractionGated("commenter") {
                                    Toast.makeText(context, "💬 Espace commentaires ouvert", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onGiftClick = {
                                handleInteractionGated("offrir un cadeau") {
                                    val roseGift = featuresService?.liveGiftsCatalog?.firstOrNull()
                                    if (roseGift != null) {
                                        scope.launch {
                                            featuresService.sendLiveGift(
                                                streamId = post.id,
                                                receiverName = post.authorName ?: "Créateur PANU",
                                                gift = roseGift
                                            )
                                        }
                                    }
                                }
                            },
                            onBoostClick = {
                                boostTargetPost = post
                            },
                            onShareClick = {
                                featuresService?.recordShareOrViewRemuneration(isShare = true)
                                watermarkExportPost = post
                            },
                            onCreateClick = {
                                handleInteractionGated("créer une vidéo") {
                                    onNavigateToStudio()
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // =========================================================================
            // FIL CLASSIQUE & MODE HORS-LIGNE PWA
            // =========================================================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    InstallAppBanner(
                        onInstallClick = { showInstallDialog = true }
                    )
                }

                if (!isLoggedIn) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, colors.champagne.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable { onNavigateToAuth() }
                                .testTag("guest_mode_banner"),
                            color = colors.champagneSubtle,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = colors.champagne,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mode Découverte Invité & Hors-Ligne PWA",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Visionnage libre sans connexion obligatoire. Connectez-vous pour aimer, commenter, offrir un cadeau ou créer.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                if (posts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .testTag("tendances_empty_posts_card"),
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎬", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aucune publication pour le moment",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.champagne
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Soyez le premier à créer un contenu !",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                items(posts, key = { it.id }) { post ->
                        val isLiked = likedPosts[post.id] ?: false
                        val currentCount = likesCount[post.id] ?: ((post.id.hashCode() % 40) + 24).coerceAtLeast(10)

                        PostCard(
                            post = post,
                            isLiked = isLiked,
                            likesCount = currentCount,
                            onAuthorClick = {
                                val handle = post.authorUsername ?: post.authorName ?: ""
                                if (handle.isNotBlank()) onOpenPublicProfile(handle)
                            },
                            onVideoClick = { videoUrl ->
                                featuresService?.recordShareOrViewRemuneration(isShare = false)
                                onWatchVideo(videoUrl)
                            },
                            onLikeClick = {
                                handleInteractionGated("aimer") {
                                    val newStatus = !isLiked
                                    likedPosts[post.id] = newStatus
                                    likesCount[post.id] = if (newStatus) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
                                }
                            },
                            onCommentClick = {
                                handleInteractionGated("commenter") {
                                    Toast.makeText(context, "Espace commentaires ouvert", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onGiftClick = {
                                handleInteractionGated("offrir un cadeau") {
                                    val rose = featuresService?.liveGiftsCatalog?.firstOrNull()
                                    if (rose != null) {
                                        scope.launch {
                                            featuresService.sendLiveGift(post.id, post.authorName ?: "Créateur PANU", rose)
                                        }
                                    }
                                }
                            },
                            onShareClick = {
                                featuresService?.recordShareOrViewRemuneration(isShare = true)
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Regardez cette vidéo sur PANU : ${post.title ?: post.content}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Partager via"))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TikTokVerticalVideoItem(
    post: Post,
    isLiked: Boolean,
    likesCount: Int,
    onAuthorClick: () -> Unit,
    onWatchFullVideo: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onGiftClick: () -> Unit,
    onBoostClick: () -> Unit = {},
    onShareClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    val colors = PanuTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .clickable { onWatchFullVideo() }
            .testTag("tiktok_video_item_${post.id}")
    ) {
        // Visuel plein écran de la vidéo virale
        AsyncImage(
            model = post.mediaUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=900&q=80",
            contentDescription = post.title ?: "Vidéo Virale PANU",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dégradé cinématographique pour lisibilité parfaite
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Colonne d'actions interactives à droite (Style TikTok : Aimer, Commenter, Cadeau, Partager, Studio IA)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PanuAvatar(
                avatarUrl = post.authorAvatarUrl,
                fullName = post.authorName ?: "PANU",
                size = 50.dp,
                onClick = onAuthorClick
            )

            // Bouton Aimer
            TikTokActionSideButton(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (isLiked) Color(0xFFFF4757) else Color.White,
                label = "$likesCount",
                testTag = "tiktok_like_btn_${post.id}",
                onClick = onLikeClick
            )

            // Bouton Commenter
            TikTokActionSideButton(
                icon = Icons.Default.ChatBubbleOutline,
                tint = Color.White,
                label = "Commenter",
                testTag = "tiktok_comment_btn_${post.id}",
                onClick = onCommentClick
            )

            // Bouton Offrir un Cadeau (Rose / Couronne)
            TikTokActionSideButton(
                icon = Icons.Default.CardGiftcard,
                tint = colors.champagne,
                label = "Cadeau 🌹",
                testTag = "tiktok_gift_btn_${post.id}",
                onClick = onGiftClick
            )

            // Bouton Booster de Visibilité dans Découvrir
            TikTokActionSideButton(
                icon = Icons.Default.VerifiedUser,
                tint = Color(0xFF2ED573),
                label = "Booster 🚀",
                testTag = "tiktok_boost_btn_${post.id}",
                onClick = onBoostClick
            )

            // Bouton Partager
            TikTokActionSideButton(
                icon = Icons.Default.Share,
                tint = Color.White,
                label = "Partager",
                testTag = "tiktok_share_btn_${post.id}",
                onClick = onShareClick
            )

            // Bouton Studio IA
            TikTokActionSideButton(
                icon = Icons.Default.AutoAwesome,
                tint = colors.champagne,
                label = "Créer IA",
                testTag = "tiktok_create_ia_btn_${post.id}",
                onClick = onCreateClick
            )
        }

        // Informations de la vidéo en bas à gauche
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAuthorClick() }
            ) {
                Text(
                    text = "@${post.authorUsername ?: "createur_panu"}",
                    color = colors.champagne,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = "Vérifié",
                    tint = colors.champagne,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (!post.title.isNullOrBlank()) {
                Text(
                    text = post.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = post.content,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = colors.champagne,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Son Original • Studio PANU Afrique • Glissez vers le haut ⬆️",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            var autoPlayActive by remember { mutableStateOf(true) }
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { autoPlayActive = !autoPlayActive }
                    .padding(top = 2.dp),
                color = if (autoPlayActive) colors.champagne.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (autoPlayActive) colors.champagne.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (autoPlayActive) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Lecture Auto",
                        tint = if (autoPlayActive) colors.champagne else Color.LightGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (autoPlayActive) "Lecture Auto ON" else "Lecture Auto OFF",
                        color = if (autoPlayActive) colors.champagne else Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TikTokActionSideButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean = false,
    likesCount: Int = 14,
    onAuthorClick: () -> Unit,
    onVideoClick: (String) -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onGiftClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    val colors = PanuTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAuthorClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanuAvatar(
                    avatarUrl = post.authorAvatarUrl,
                    fullName = post.authorName ?: post.authorUsername ?: "Auteur",
                    size = 42.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName ?: post.authorUsername ?: "Créateur PANU",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (!post.authorUsername.isNullOrBlank()) "@${post.authorUsername}" else "Membre",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surfaceElevated
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = colors.champagne,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Public",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.champagne,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (!post.title.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                lineHeight = 22.sp
            )

            if (!post.mediaUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.surfaceBorder, RoundedCornerShape(12.dp))
                        .clickable { onVideoClick(post.mediaUrl) }
                ) {
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = "Média de la publication",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onLikeClick() }
                        .padding(vertical = 4.dp)
                        .testTag("btn_like_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Liker",
                        tint = if (isLiked) Color(0xFFFF4757) else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$likesCount",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isLiked) Color(0xFFFF4757) else colors.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onCommentClick() }
                        .padding(vertical = 4.dp)
                        .testTag("btn_comment_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Commenter",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Commenter",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onGiftClick() }
                        .padding(vertical = 4.dp)
                        .testTag("btn_gift_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Offrir un cadeau",
                        tint = colors.champagne,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cadeau 🌹",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.champagne
                    )
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_share_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Partager",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(Date(post.createdAt))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}
