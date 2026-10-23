package com.example.ui.screens.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material.icons.filled.Close
import com.example.data.repository.GeminiRepository
import com.example.data.repository.ProfileRepository
import com.example.ui.components.CreatorSuggestionDialog
import com.example.ui.components.PanuCommentsBottomSheet
import com.example.ui.components.PanuGiftDialog
import com.example.ui.components.PanuWatermarkExporterDialog
import com.example.ui.components.ReferralProgramDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.components.PanuMediaPlayer
import com.example.ui.components.PanuTopBar
import com.example.ui.components.SuggestedCreatorsStrip
import com.example.ui.components.SuggestedCreator
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
    profileRepository: ProfileRepository? = null,
    currentUserId: String? = null,
    featuresService: SupabasePanuFeaturesService? = null,
    geminiRepository: GeminiRepository? = null,
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

    // Recherche Sémantique IA Gemini (par Concept : ex. 'images de futur africain')
    var isSearchActive by remember { mutableStateOf(false) }
    var searchConceptQuery by remember { mutableStateOf("") }
    var isSearchingWithGemini by remember { mutableStateOf(false) }
    var semanticSearchResults by remember { mutableStateOf<List<Post>?>(null) }

    var selectedCategory by remember { mutableStateOf("Tous") }

    val displayedPosts = remember(posts, semanticSearchResults, selectedCategory) {
        val base = semanticSearchResults ?: posts
        if (selectedCategory == "Tous") {
            base
        } else {
            base.filter { post ->
                getPostCategory(post).equals(selectedCategory, ignoreCase = true)
            }
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

    // Abonnements dynamiques & interactions temps réel
    val followedCreators = remember { mutableStateMapOf<String, Boolean>() }
    var activeCommentPost by remember { mutableStateOf<Post?>(null) }
    var giftTargetPost by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(Unit) {
        launch(kotlinx.coroutines.Dispatchers.IO) { postRepository.refreshPublishedPosts() }
        launch(kotlinx.coroutines.Dispatchers.IO) { featuresService?.fetchUserNotifications() }
    }

    LaunchedEffect(currentUserId) {
        if (!currentUserId.isNullOrBlank()) {
            val ids = profileRepository?.fetchFollowedIds(currentUserId) ?: emptySet()
            ids.forEach { followedCreators[it] = true }
        }
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
        userId = if (isLoggedIn) (currentUserId ?: "createur_actif") else "guest",
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

    activeCommentPost?.let { p ->
        PanuCommentsBottomSheet(
            post = p,
            postRepository = postRepository,
            featuresService = featuresService,
            isLoggedIn = isLoggedIn,
            onNavigateToAuth = onNavigateToAuth,
            onDismiss = { activeCommentPost = null }
        )
    }

    giftTargetPost?.let { p ->
        PanuGiftDialog(
            isOpen = true,
            post = p,
            featuresService = featuresService,
            onOpenRechargeModal = {
                giftTargetPost = null
                showCreditsStoreModal = true
            },
            onDismiss = { giftTargetPost = null }
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

    fun handleToggleFollow(authorId: String, authorHandle: String) {
        handleInteractionGated("s'abonner à ce créateur") {
            val uid = currentUserId ?: "guest"
            val currentlyFollowed = followedCreators[authorId] == true
            followedCreators[authorId] = !currentlyFollowed
            scope.launch {
                val res = profileRepository?.toggleFollow(uid, authorId)
                if (res != null && res.isSuccess) {
                    val newStatus = res.getOrNull() == true
                    followedCreators[authorId] = newStatus
                    Toast.makeText(
                        context,
                        if (newStatus) "Abonné à @$authorHandle !" else "Désabonné de @$authorHandle",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    followedCreators[authorId] = currentlyFollowed
                    Toast.makeText(context, "Erreur lors de la mise à jour de l'abonnement", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun sharePost(post: Post) {
        featuresService?.recordShareOrViewRemuneration(isShare = true)
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Regardez cette vidéo sur PANU : ${post.title ?: "Création PANU"} — ${post.mediaUrl ?: "https://panu.app"}")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Partager la vidéo PANU")
            context.startActivity(shareIntent)
        } catch (_: Exception) {
            watermarkExportPost = post
        }
    }

    Scaffold(
        topBar = {
            Column {
                PanuTopBar(
                    featuresService = featuresService,
                    onCreditsClick = { showCreditsStoreModal = true },
                    onCreateClick = {
                        handleInteractionGated("créer une publication") {
                            onNavigateToCreate()
                        }
                    },
                    onSearchClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            searchConceptQuery = ""
                            semanticSearchResults = null
                        }
                    },
                    onMenuClick = onMenuClick
                )

                // Panneau de Recherche Sémantique IA Gemini (Par Concept)
                if (isSearchActive) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchConceptQuery,
                                    onValueChange = { newQ ->
                                        searchConceptQuery = newQ
                                        if (newQ.isBlank()) {
                                            semanticSearchResults = null
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_semantic_search_input"),
                                    placeholder = {
                                        Text(
                                            "Chercher par concept (ex: 'images de futur africain')...",
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "Gemini",
                                            tint = colors.champagne,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchConceptQuery.isNotBlank()) {
                                            IconButton(onClick = {
                                                searchConceptQuery = ""
                                                semanticSearchResults = null
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Effacer", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Button(
                                    onClick = {
                                        if (searchConceptQuery.isNotBlank() && !isSearchingWithGemini) {
                                            isSearchingWithGemini = true
                                            scope.launch {
                                                val results = geminiRepository?.searchSemantically(searchConceptQuery, posts) ?: emptyList()
                                                semanticSearchResults = results
                                                isSearchingWithGemini = false
                                                Toast.makeText(context, "✨ ${results.size} publication(s) trouvée(s) pour le concept !", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isSearchingWithGemini && searchConceptQuery.isNotBlank(),
                                    modifier = Modifier.testTag("home_btn_semantic_search")
                                ) {
                                    if (isSearchingWithGemini) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Chercher", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (colors.isDark) colors.background else Color.White)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (semanticSearchResults != null)
                                        "✨ ${semanticSearchResults!!.size} résultat(s) conceptuel(s) (Gemini 3.5 Flash)"
                                    else
                                        "💡 Recherche sémantique IA : explorez par concept au lieu de simples mots-clés",
                                    fontSize = 11.sp,
                                    color = colors.champagne,
                                    fontWeight = FontWeight.Medium
                                )

                                if (semanticSearchResults != null) {
                                    Text(
                                        text = "Tout réafficher",
                                        fontSize = 11.sp,
                                        color = colors.emerald,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            searchConceptQuery = ""
                                            semanticSearchResults = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Barre d'onglets du fil d'actualité : Pour vous, Tendances
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = {
                                Text(
                                    text = if (tab == "Pour vous") "🔥 Pour vous" else "✨ Tendances",
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = if (colors.isDark) colors.background else Color.White
                            )
                        )
                    }
                }

                // Filtres rapides de catégories dynamiques : Tous, Design, Vidéo, AI Art
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("Tous", "Design", "Vidéo", "AI Art")
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = when(cat) {
                                        "Design" -> "🎨 Design"
                                        "Vidéo" -> "🎬 Vidéo"
                                        "AI Art" -> "✨ AI Art"
                                        else -> "🌟 Tous"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = if (colors.isDark) colors.background else Color.White,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textPrimary
                            )
                        )
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
        containerColor = colors.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab == "Pour vous") {
                if (displayedPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 48.dp)
                            .testTag("home_empty_posts_for_you"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(32.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.champagne.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(2.dp, colors.champagne),
                                modifier = Modifier.size(88.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🎬", fontSize = 38.sp)
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Aucune publication pour le moment",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 19.sp,
                                    color = colors.champagne,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Soyez le premier à créer un contenu !",
                                    color = colors.textSecondary,
                                    fontSize = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            CrossPlatformInviteCard(
                                onInviteClick = { showReferralDialog = true },
                                onSuggestClick = { showCreatorSuggestionDialog = true }
                            )
                        }
                    }
                } else {
                    // =========================================================================
                    // FLUX VERTICAL DE VIDÉOS VIRALES + INTERSTITIEL PUBLICITAIRE TOUTES LES 3 VIDÉOS
                    // =========================================================================
                    val feedEntries = remember(displayedPosts) {
                        val list = mutableListOf<Post?>()
                        displayedPosts.forEachIndexed { index, post ->
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
                            val currentLikes = likesCount[post.id] ?: post.likesCount

                            TikTokVerticalVideoItem(
                                post = post,
                                isLiked = isLiked,
                                likesCount = currentLikes,
                                isFollowed = followedCreators[post.authorId] == true,
                                onAuthorClick = {
                                    val handle = post.authorUsername ?: post.authorName ?: ""
                                    if (handle.isNotBlank()) onOpenPublicProfile(handle)
                                },
                                onToggleFollow = {
                                    handleToggleFollow(post.authorId, post.authorUsername ?: post.authorName ?: "createur")
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
                                    scope.launch {
                                        val next = !isLiked
                                        likedPosts[post.id] = next
                                        val updatedLikes = if (next) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
                                        likesCount[post.id] = updatedLikes
                                        val res = postRepository.updateLikes(post.id, updatedLikes)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, if (next) "❤️ J'aime ajouté" else "Mention retirée", Toast.LENGTH_SHORT).show()
                                        } else {
                                            likedPosts[post.id] = isLiked
                                            likesCount[post.id] = currentLikes
                                            Toast.makeText(context, "Erreur de connexion lors de l'enregistrement du J'aime", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onCommentClick = {
                                handleInteractionGated("commenter") {
                                    activeCommentPost = post
                                }
                            },
                            onGiftClick = {
                                handleInteractionGated("offrir un cadeau") {
                                    giftTargetPost = post
                                }
                            },
                            onRemixClick = {
                                onNavigateToStudio()
                            },
                            onBoostClick = {
                                boostTargetPost = post
                            },
                            onShareClick = {
                                sharePost(post)
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
                item {
                    SuggestedCreatorsStrip(
                        creators = listOf(
                            SuggestedCreator("1", "Marie Art", "@marie_art"),
                            SuggestedCreator("2", "Jean Tech", "@jean_tech"),
                            SuggestedCreator("3", "Fatou Dance", "@fatou_dance")
                        ),
                        onNavigateToProfile = { handle -> onOpenPublicProfile(handle) },
                        onFollow = { handle -> handleToggleFollow(handle, handle) }
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

                if (displayedPosts.isEmpty()) {
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

                items(displayedPosts, key = { it.id }) { post ->
                        val isLiked = likedPosts[post.id] ?: false
                        val currentCount = likesCount[post.id] ?: post.likesCount

                        PostCard(
                            post = post,
                            isLiked = isLiked,
                            likesCount = currentCount,
                            isFollowed = followedCreators[post.authorId] == true,
                            onAuthorClick = {
                                val handle = post.authorUsername ?: post.authorName ?: ""
                                if (handle.isNotBlank()) onOpenPublicProfile(handle)
                            },
                            onToggleFollow = {
                                handleToggleFollow(post.authorId, post.authorUsername ?: post.authorName ?: "createur")
                            },
                            onVideoClick = { videoUrl ->
                                featuresService?.recordShareOrViewRemuneration(isShare = false)
                                onWatchVideo(videoUrl)
                            },
                            onLikeClick = {
                                handleInteractionGated("aimer") {
                                    scope.launch {
                                        val newStatus = !isLiked
                                        likedPosts[post.id] = newStatus
                                        val updatedCount = if (newStatus) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
                                        likesCount[post.id] = updatedCount
                                        val res = postRepository.updateLikes(post.id, updatedCount)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, if (newStatus) "❤️ J'aime ajouté" else "Mention retirée", Toast.LENGTH_SHORT).show()
                                        } else {
                                            likedPosts[post.id] = isLiked
                                            likesCount[post.id] = currentCount
                                            Toast.makeText(context, "Erreur de connexion lors de l'enregistrement du J'aime", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onCommentClick = {
                                handleInteractionGated("commenter") {
                                    activeCommentPost = post
                                }
                            },
                            onGiftClick = {
                                handleInteractionGated("offrir un cadeau") {
                                    giftTargetPost = post
                                }
                            },
                            onShareClick = {
                                sharePost(post)
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
    isFollowed: Boolean = false,
    onAuthorClick: () -> Unit,
    onToggleFollow: () -> Unit = {},
    onWatchFullVideo: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onGiftClick: () -> Unit,
    onRemixClick: () -> Unit = {},
    onBoostClick: () -> Unit = {},
    onShareClick: () -> Unit,
    onCreateClick: () -> Unit,
    onCategoryClick: (String) -> Unit = {}
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

        // Colonne d'actions interactives à droite (Style TikTok : Avatar+Follow, Aimer, Commenter, Cadeau, Remixer, Booster, Partager, Studio IA)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar avec badge "+" ou "✓" pour suivre en 1-clic
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                PanuAvatar(
                    avatarUrl = post.authorAvatarUrl,
                    fullName = post.authorName ?: "PANU",
                    size = 50.dp,
                    onClick = onAuthorClick
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isFollowed) Color(0xFF2ED573) else colors.champagne)
                        .border(1.5.dp, Color.Black, CircleShape)
                        .clickable { onToggleFollow() }
                        .testTag("avatar_follow_btn_${post.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (isFollowed) "Abonné" else "Suivre",
                        tint = if (isFollowed) Color.White else Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

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

            // Bouton Remixer / Utiliser le son & modèle IA
            TikTokActionSideButton(
                icon = Icons.Default.MusicNote,
                tint = colors.champagne,
                label = "Remixer",
                testTag = "tiktok_remix_btn_${post.id}",
                onClick = onRemixClick
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
                .fillMaxWidth(0.76f)
                .padding(start = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleFollow() }
                        .testTag("follow_pill_btn_${post.id}"),
                    color = if (isFollowed) Color.White.copy(alpha = 0.25f) else colors.champagne,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isFollowed) "Abonné ✓" else "+ Suivre",
                        color = if (isFollowed) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
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

            val postCategory = getPostCategory(post)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCategoryClick(postCategory) }
                    .testTag("category_badge_${post.id}"),
                color = colors.champagne.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when(postCategory) {
                        "Design" -> "🎨 Design"
                        "Vidéo" -> "🎬 Vidéo"
                        else -> "✨ AI Art"
                    },
                    color = colors.champagne,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

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
    likesCount: Int = post.likesCount,
    isFollowed: Boolean = false,
    onAuthorClick: () -> Unit,
    onToggleFollow: () -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onGiftClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {}
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
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleFollow() }
                        .testTag("postcard_follow_btn_${post.id}"),
                    color = if (isFollowed) colors.surfaceElevated else colors.champagne.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFollowed) colors.surfaceBorder else colors.champagne),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isFollowed) "Abonné ✓" else "+ Suivre",
                        color = if (isFollowed) colors.textSecondary else colors.champagne,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
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
            val postCategory = getPostCategory(post)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCategoryClick(postCategory) }
                    .testTag("postcard_category_badge_${post.id}"),
                color = colors.champagne.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when(postCategory) {
                        "Design" -> "🎨 Design"
                        "Vidéo" -> "🎬 Vidéo"
                        else -> "✨ AI Art"
                    },
                    color = colors.champagne,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                PanuMediaPlayer(
                    mediaUrl = post.mediaUrl,
                    title = post.title ?: "Publication PANU",
                    mediaType = post.mediaType,
                    isVideo = post.mediaType.equals("video", ignoreCase = true),
                    modifier = Modifier.fillMaxWidth()
                )
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

@Composable
private fun CrossPlatformInviteCard(
    onInviteClick: () -> Unit,
    onSuggestClick: () -> Unit
) {
    val colors = PanuTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.champagne)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌟", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inviter & Suivre vos créateurs préférés",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Importez les profils TikTok, Instagram, YouTube, X de vos créateurs et gagnez des bonus !",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSuggestClick,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("👥 Suggérer un créateur", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onInviteClick,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("🔗 Lien de parrainage", color = colors.champagne, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getPostCategory(post: Post): String {
    val text = (post.title ?: "") + " " + post.content + " " + post.mediaType
    return when {
        post.mediaType.equals("video", ignoreCase = true) -> "Vidéo"
        text.contains("art", ignoreCase = true) || text.contains("ai", ignoreCase = true) || text.contains("généré", ignoreCase = true) -> "AI Art"
        text.contains("design", ignoreCase = true) || text.contains("ui", ignoreCase = true) || text.contains("ux", ignoreCase = true) -> "Design"
        post.mediaType.equals("image", ignoreCase = true) -> "AI Art"
        else -> "Design"
    }
}
