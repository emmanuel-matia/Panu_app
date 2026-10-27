package com.example.ui.screens.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.SessionManager
import com.example.data.model.Post
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.data.repository.PostRepository
import com.example.data.repository.ProfileRepository
import com.example.ui.components.FullCreativeStudioDialog
import com.example.ui.components.PanuAvatar
import com.example.ui.components.PanuCommentsBottomSheet
import com.example.ui.theme.PanuGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val DEFAULT_STARTER_POSTS = listOf(
    Post(
        id = "panu_starter_01",
        authorId = "creator_panu_studio",
        authorName = "PANU Studio",
        authorUsername = "PANU_Studio",
        title = "Remixe cette vidéo ou booste ton contenu ! 🚀",
        content = "Le studio créatif intelligent pensé pour les créateurs africains. Imaginez. Créez. Publiez.",
        mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        mediaType = "video",
        status = "published",
        visibility = "public",
        likesCount = 124
    ),
    Post(
        id = "panu_starter_02",
        authorId = "creator_afro_dance",
        authorName = "Afro Dance Kinshasa",
        authorUsername = "AfroDance_Kinshasa",
        title = "Chorégraphie Freestyle & Énergie Urbaine 🌟",
        content = "Nouvelle session Afrobeat Freestyle filmée en plein air au cœur de la ville.",
        mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        mediaType = "video",
        status = "published",
        visibility = "public",
        likesCount = 89
    ),
    Post(
        id = "panu_starter_03",
        authorId = "creator_pixverse",
        authorName = "PixVerse Creative",
        authorUsername = "PixVerse_Creative",
        title = "Cinématique Nuit Urbaine générée par IA 🎬",
        content = "Rendu ultra-réaliste généré avec nos moteurs vidéo IA PANU & MiniMax.",
        mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        mediaType = "video",
        status = "published",
        visibility = "public",
        likesCount = 215
    )
)

@Composable
fun HomeScreen(
    postRepository: PostRepository? = null,
    profileRepository: ProfileRepository? = null,
    sessionManager: SessionManager? = null,
    featuresService: SupabasePanuFeaturesService? = null,
    onNavigate: (String) -> Unit = {},
    onOpenMessages: () -> Unit = {},
    onOpenCreateStudio: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeHeaderTab by remember { mutableStateOf("Pour toi") }
    val headerTabs = listOf("Suivis", "Découvrir", "Amis", "Pour toi")

    var showCreateStudioDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<Post?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // User session
    val currentUserId = sessionManager?.currentUserId?.collectAsState()?.value ?: "user_panu_creator"
    val currentUserEmail = sessionManager?.currentUserEmail?.collectAsState()?.value ?: "createur@panu.app"
    val authorDisplayName = remember(currentUserEmail) {
        currentUserEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
    }

    // Dynamic states per post ID for immediate UI responsiveness & persistence
    val likedState = remember { mutableStateMapOf<String, Boolean>() }
    val likesCountState = remember { mutableStateMapOf<String, Int>() }
    val followedState = remember { mutableStateMapOf<String, Boolean>() }
    val bookmarkedState = remember { mutableStateMapOf<String, Boolean>() }
    val bookmarksCountState = remember { mutableStateMapOf<String, Int>() }

    // Collect published posts from Room database
    val dbPosts by (postRepository?.publishedPosts ?: emptyFlow()).collectAsState(initial = emptyList())

    // Initial seeding & remote refresh (runs asynchronously in Dispatchers.IO to never block the UI)
    LaunchedEffect(Unit) {
        if (postRepository != null) {
            withContext(Dispatchers.IO) {
                try {
                    DEFAULT_STARTER_POSTS.forEach { post ->
                        try {
                            postRepository.savePost(post)
                        } catch (_: Exception) {}
                    }
                    postRepository.refreshPublishedPosts()
                } catch (_: Exception) {}
            }
        }
    }

    // Filter posts according to active tab and search query - instant fallback to starter posts if DB empty
    val displayPosts = remember(dbPosts, activeHeaderTab, searchQuery, followedState) {
        val baseList = if (dbPosts.isNotEmpty()) dbPosts else DEFAULT_STARTER_POSTS

        val filteredByTab = when (activeHeaderTab) {
            "Suivis" -> baseList.filter { post ->
                followedState[post.authorId] == true ||
                        (profileRepository?.isUserFollowed(post.authorId) == true)
            }
            "Découvrir" -> baseList.sortedByDescending { it.likesCount }
            "Amis" -> baseList.filterIndexed { idx, _ -> idx % 2 == 0 }
            else -> baseList // "Pour toi"
        }

        if (searchQuery.isBlank()) {
            filteredByTab
        } else {
            val q = searchQuery.trim().lowercase()
            filteredByTab.filter { post ->
                post.title?.lowercase()?.contains(q) == true ||
                        post.content.lowercase().contains(q) ||
                        post.authorName?.lowercase()?.contains(q) == true ||
                        post.authorUsername?.lowercase()?.contains(q) == true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C12))
    ) {
        // Immersive video feed
        if (displayPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = if (activeHeaderTab == "Suivis") Icons.Default.PersonAdd else Icons.Default.Movie,
                        contentDescription = null,
                        tint = PanuGold,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = if (activeHeaderTab == "Suivis")
                            "Vous ne suivez encore aucun créateur"
                        else
                            "Aucune publication trouvée",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (activeHeaderTab == "Suivis")
                            "Touchez l'icône « + » sous le profil d'un créateur dans l'onglet « Pour toi » pour voir son contenu ici."
                        else
                            "Créez ou importez votre première création multimédia dans PANU Studio !",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = {
                            if (activeHeaderTab == "Suivis") {
                                activeHeaderTab = "Pour toi"
                            } else {
                                showCreateStudioDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PanuGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (activeHeaderTab == "Suivis") "Découvrir des créateurs" else "✨ Créer une publication",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 76.dp)
            ) {
                itemsIndexed(displayPosts, key = { _, post -> post.id }) { index, post ->
                    val isLiked = likedState[post.id] ?: false
                    val likeCount = likesCountState[post.id] ?: post.likesCount
                    val isFollowed = followedState[post.authorId] ?: (profileRepository?.isUserFollowed(post.authorId) ?: false)
                    val isBookmarked = bookmarkedState[post.id] ?: false
                    val bookmarkCount = bookmarksCountState[post.id] ?: 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(720.dp)
                            .background(Color(0xFF13151D))
                    ) {
                        // Background gradient / video cover representation
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF1E2230),
                                            Color(0xFF0F1118),
                                            Color(0xFF07080C)
                                        )
                                    )
                                )
                        )

                        // If mediaUrl is an image or video thumbnail, show AsyncImage cover
                        if (!post.mediaUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = post.mediaUrl,
                                contentDescription = post.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        // Tap on video to open full player if video
                                        if (post.mediaType == "video" || post.mediaUrl?.endsWith(".mp4") == true) {
                                            onNavigate("vod_player/${java.net.URLEncoder.encode(post.mediaUrl, "UTF-8")}")
                                        }
                                    },
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }

                        // Top gradient overlay for text readability
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                                    )
                                )
                        )

                        // Bottom gradient overlay for info readability
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                    )
                                )
                        )

                        // Right action column (Follow, Like, Comments, Bookmark, Share, Remix, Boost)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Creator Avatar with Follow "+" / "✓" toggle
                            Box(modifier = Modifier.size(50.dp)) {
                                PanuAvatar(
                                    avatarUrl = post.authorAvatarUrl,
                                    fullName = post.authorName ?: post.authorUsername ?: "PANU",
                                    size = 46.dp,
                                    borderWidth = 2.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .clickable {
                                            onNavigate("public_profile/${post.authorUsername ?: post.authorId}")
                                        }
                                )

                                if (!isFollowed) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.BottomCenter)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                            .clickable {
                                                scope.launch {
                                                    val res = profileRepository?.toggleFollow(currentUserId, post.authorId)
                                                    val nextState = if (res != null && res.isSuccess) res.getOrDefault(!isFollowed) else !isFollowed
                                                    followedState[post.authorId] = nextState
                                                    Toast.makeText(
                                                        context,
                                                        if (nextState) "Abonné à ${post.authorName ?: post.authorUsername} !" else "Désabonné",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            .testTag("feed_follow_btn_${post.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Suivre",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.BottomCenter)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .clickable {
                                                scope.launch {
                                                    val res = profileRepository?.toggleFollow(currentUserId, post.authorId)
                                                    val nextState = if (res != null && res.isSuccess) res.getOrDefault(!isFollowed) else !isFollowed
                                                    followedState[post.authorId] = nextState
                                                    Toast.makeText(context, "Désabonné", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .testTag("feed_unfollow_btn_${post.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Abonné",
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            // 2. Like button & dynamic count connected to database
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        val newLiked = !isLiked
                                        likedState[post.id] = newLiked
                                        val newCount = if (newLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
                                        likesCountState[post.id] = newCount
                                        scope.launch {
                                            postRepository?.updateLikes(post.id, newCount)
                                            postRepository?.updatePost(post.copy(likesCount = newCount))
                                        }
                                    },
                                    modifier = Modifier.testTag("feed_like_btn_${post.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "J'aime",
                                        tint = if (isLiked) Color.Red else Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = likeCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 3. Comments button: opens real connected PanuCommentsBottomSheet
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { selectedPostForComments = post },
                                    modifier = Modifier.testTag("feed_comments_btn_${post.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Comment,
                                        contentDescription = "Commentaires",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "Avis",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 4. Favorites (Bookmark) button
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        val newBm = !isBookmarked
                                        bookmarkedState[post.id] = newBm
                                        val nextBCount = if (newBm) bookmarkCount + 1 else (bookmarkCount - 1).coerceAtLeast(0)
                                        bookmarksCountState[post.id] = nextBCount
                                        Toast.makeText(
                                            context,
                                            if (newBm) "Ajouté à vos favoris !" else "Retiré des favoris",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.testTag("feed_bookmark_btn_${post.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = "Favoris",
                                        tint = if (isBookmarked) PanuGold else Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = bookmarkCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 5. Share button (Native Android Share Intent)
                            IconButton(
                                onClick = {
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, post.title ?: "Découvre cette vidéo sur PANU !")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "🎬 Regarde « ${post.title ?: "cette création"} » par ${post.authorName ?: post.authorUsername} sur PANU Studio : ${post.mediaUrl ?: "https://panu.app/post/${post.id}"}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Partage : ${post.title}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("feed_share_btn_${post.id}")
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Partager",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // 6. Remix button (opens creation tools with this post's audio/theme)
                            IconButton(
                                onClick = {
                                    showCreateStudioDialog = true
                                },
                                modifier = Modifier.testTag("feed_remix_btn_${post.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PanuGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Shuffle,
                                        contentDescription = "Remixer",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // 7. Booster / Rocket button
                            IconButton(
                                onClick = {
                                    onNavigate("studio")
                                },
                                modifier = Modifier.testTag("feed_boost_btn_${post.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.RocketLaunch,
                                        contentDescription = "Booster",
                                        tint = PanuGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // 8. Vinyl record icon -> Navigate to Studio
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray)
                                    .clickable { onNavigate("studio") },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(PanuGold)
                                )
                            }
                        }

                        // Bottom-left info (Author handle, title, audio note)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                                .padding(end = 84.dp, bottom = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "@${post.authorUsername ?: post.authorName ?: "panu"}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.clickable {
                                        onNavigate("public_profile/${post.authorUsername ?: post.authorId}")
                                    }
                                )
                                Surface(
                                    color = PanuGold,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Créateur",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = post.title ?: post.content,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable { onNavigate("studio") }
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = PanuGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Morceau original • ${post.authorName ?: "PANU Studio"}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Header overlay (LIVE, Suivis, Découvrir, Amis, Pour toi + Search & Messages)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.88f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. LIVE badge -> Navigates directly to LiveSportsScreen
                Surface(
                    onClick = { onNavigate("live") },
                    color = Color.Red.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.Red),
                    modifier = Modifier.testTag("feed_live_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Text("LIVE", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // 2. Header Tabs: Suivis, Découvrir, Amis, Pour toi
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headerTabs.forEach { tab ->
                        val isSelected = activeHeaderTab == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { activeHeaderTab = tab }
                                .testTag("tab_header_$tab")
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (isSelected) 15.sp else 14.sp
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(2.5.dp)
                                        .background(PanuGold, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }

                // 3. Right icons: Notifications/Messages & Search
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.testTag("feed_messages_btn")
                    ) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = "Messages",
                            tint = PanuGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("feed_search_btn")
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Expandable Search Field
            if (isSearchActive) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher vidéos, créateurs, musique...", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feed_search_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E2230),
                        unfocusedContainerColor = Color(0xFF1E2230),
                        focusedBorderColor = PanuGold,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer", tint = Color.Gray)
                            }
                        }
                    }
                )
            }
        }

        // Bottom Navigation Bar (5 fixed interactive items: Accueil, Templates, Grand "+" bouton central, Studio IA, Profil)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = Color(0xFF0B0C12),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 1. Accueil
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            activeHeaderTab = "Pour toi"
                            searchQuery = ""
                            isSearchActive = false
                        }
                        .testTag("nav_bottom_home")
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Accueil",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Accueil", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Templates (Navigates to Studio with templates)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNavigate("studio") }
                        .testTag("nav_bottom_templates")
                ) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = "Templates",
                        tint = PanuGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Templates", color = PanuGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 3. Central Grand "+" Button (Créer) -> Opens real FullCreativeStudioDialog
                Button(
                    onClick = { showCreateStudioDialog = true },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("nav_bottom_create_plus"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanuGold),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Créer",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 4. Studio IA -> Navigates to StudioScreen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNavigate("studio") }
                        .testTag("nav_bottom_studio")
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Studio IA",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Studio IA", color = Color.Gray, fontSize = 10.sp)
                }

                // 5. Profil -> Navigates to ProfileScreen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNavigate("profile") }
                        .testTag("nav_bottom_profile")
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profil",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Profil", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }

        // Full Creative Studio Dialog (Connected to real PostRepository)
        FullCreativeStudioDialog(
            isOpen = showCreateStudioDialog,
            userId = currentUserId,
            authorName = authorDisplayName,
            postRepository = postRepository,
            onDismiss = { showCreateStudioDialog = false },
            onPostCreated = { newPost ->
                showCreateStudioDialog = false
                scope.launch {
                    postRepository?.savePost(newPost)
                    Toast.makeText(context, "Publication créée avec succès !", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Real Comments Bottom Sheet (Connected to database comments)
        selectedPostForComments?.let { post ->
            PanuCommentsBottomSheet(
                post = post,
                postRepository = postRepository ?: PostRepository(
                    postService = com.example.data.remote.SupabasePostService(com.example.data.remote.SupabaseClient(SessionManager(context))),
                    storageService = com.example.data.remote.SupabaseStorageService(com.example.data.remote.SupabaseClient(SessionManager(context)), context),
                    postDao = com.example.data.local.AppDatabase.getInstance(context).postDao()
                ),
                featuresService = featuresService,
                isLoggedIn = sessionManager?.isLoggedIn() == true,
                onNavigateToAuth = {
                    selectedPostForComments = null
                    onNavigate("login")
                },
                onDismiss = { selectedPostForComments = null }
            )
        }

        // Notifications & Messages Dialog
        if (showNotificationsDialog) {
            val notificationsList = featuresService?.notificationsHistory?.collectAsState()?.value ?: emptyList()
            AlertDialog(
                onDismissRequest = { showNotificationsDialog = false },
                containerColor = Color(0xFF13151D),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notifications & Messages", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showNotificationsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.Gray)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (notificationsList.isEmpty()) {
                            Text("Aucune nouvelle notification.", color = Color.Gray, fontSize = 13.sp)
                        } else {
                            notificationsList.take(6).forEach { notif ->
                                Surface(
                                    color = Color(0xFF1E2230),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(notif.title, color = PanuGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(notif.message, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNotificationsDialog = false }) {
                        Text("Fermer", color = PanuGold, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
