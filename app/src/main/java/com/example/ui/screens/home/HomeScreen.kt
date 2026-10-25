package com.example.ui.screens.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PanuGold
import com.example.ui.components.FullCreativeStudioDialog

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit = {},
    onOpenMessages: () -> Unit = {},
    onOpenCreateStudio: () -> Unit = {}
) {
    var activeHeaderTab by remember { mutableStateOf("Pour toi") }
    val headerTabs = listOf("Suivis", "Découvrir", "Amis", "Pour toi")
    var showCreateStudioDialog by remember { mutableStateOf(false) }
    var showMessagesDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Interactive dynamic counters state per video index
    val likesState = remember { mutableStateMapOf<Int, Int>(0 to 0, 1 to 0, 2 to 0) }
    val likedState = remember { mutableStateMapOf<Int, Boolean>() }
    val followedState = remember { mutableStateMapOf<Int, Boolean>() }
    val commentsState = remember { mutableStateMapOf<Int, Int>(0 to 0, 1 to 0, 2 to 0) }
    val bookmarksState = remember { mutableStateMapOf<Int, Int>(0 to 0, 1 to 0, 2 to 0) }
    val bookmarkedState = remember { mutableStateMapOf<Int, Boolean>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C12))
    ) {
        // Immersive video feed
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            itemsIndexed(
                listOf(
                    mapOf("author" to "@PANU_Studio", "title" to "Remixe cette vidéo ou booste ton contenu ! 🚀", "audio" to "Morceau original - PANU Studio"),
                    mapOf("author" to "@AfroDance_Kinshasa", "title" to "Chorégraphie Freestyle & Énergie Urbaine 🌟", "audio" to "Afrobeat Energy - PANU"),
                    mapOf("author" to "@PixVerse_Creative", "title" to "Cinématique Nuit Urbaine générée par IA 🎬", "audio" to "Cyber Afro Vibe - PANU")
                )
            ) { index, video ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(720.dp)
                        .background(Color(0xFF13151D))
                ) {
                    // Video immersive background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1E2230), Color(0xFF0B0C12), Color.Black)
                                )
                            )
                    )

                    // Right action column
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Creator Avatar with follow "+" button
                        Box(modifier = Modifier.size(48.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(PanuGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("P", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            val isFollowed = followedState[index] == true
                            if (!isFollowed) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.BottomCenter)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .clickable { followedState[index] = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Suivre", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.BottomCenter)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Suivi", tint = Color.Black, modifier = Modifier.size(10.dp))
                                }
                            }
                        }

                        // Like button & dynamic count
                        val isLiked = likedState[index] == true
                        val likeCount = likesState[index] ?: 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = {
                                val newLiked = !isLiked
                                likedState[index] = newLiked
                                likesState[index] = if (newLiked) likeCount + 1 else Math.max(0, likeCount - 1)
                            }) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color.Red else Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(likeCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Comments button & count
                        val commentCount = commentsState[index] ?: 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { showMessagesDialog = true }) {
                                Icon(Icons.Default.Comment, contentDescription = "Commentaires", tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                            Text(commentCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Favorites button & count
                        val isBookmarked = bookmarkedState[index] == true
                        val bookmarkCount = bookmarksState[index] ?: 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = {
                                val newBm = !isBookmarked
                                bookmarkedState[index] = newBm
                                bookmarksState[index] = if (newBm) bookmarkCount + 1 else Math.max(0, bookmarkCount - 1)
                            }) {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = "Favoris",
                                    tint = if (isBookmarked) PanuGold else Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(bookmarkCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Share, contentDescription = "Partager", tint = Color.White, modifier = Modifier.size(30.dp))
                        }

                        // Remix button
                        IconButton(onClick = { onOpenCreateStudio() }) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PanuGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Remix", tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Rocket / Boost button
                        IconButton(onClick = { showCreateStudioDialog = true }) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = "Booster", tint = PanuGold, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Vinyl record icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray)
                                .clickable { onOpenCreateStudio() },
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

                    // Bottom-left info (Author handle, title, audio)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .padding(end = 80.dp, bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = video["author"] ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
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
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = video["title"] ?: "",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = PanuGold, modifier = Modifier.size(14.dp))
                            Text(
                                text = video["audio"] ?: "",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Top Header overlay (LIVE, Suivis, Amis, Pour toi, Découvrir + Search & Messages)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LIVE badge
                Surface(
                    onClick = { onNavigate("live") },
                    color = Color.Red.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.Red)
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

                // Header Tabs: Suivis, Découvrir, Amis, Pour toi
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headerTabs.forEach { tab ->
                        val isSelected = activeHeaderTab == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { activeHeaderTab = tab }
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
                                        .width(16.dp)
                                        .height(2.dp)
                                        .background(PanuGold, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }

                // Right icons: Message button & Search icon
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showMessagesDialog = true }) {
                        Icon(Icons.Default.Message, contentDescription = "Messages", tint = PanuGold, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            if (isSearchActive) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
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
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
                )
            }
        }

        // Bottom Navigation Bar (5 fixed items: Accueil, Templates, Grand "+" bouton central, Studio IA, Profil)
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
                    modifier = Modifier.clickable { }
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Accueil", tint = Color.White, modifier = Modifier.size(24.dp))
                    Text("Accueil", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Templates
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showCreateStudioDialog = true }
                ) {
                    Icon(Icons.Default.Movie, contentDescription = "Templates", tint = PanuGold, modifier = Modifier.size(24.dp))
                    Text("Templates", color = PanuGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 3. Central Grand "+" Button (Créer)
                Button(
                    onClick = { showCreateStudioDialog = true },
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanuGold),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Créer", tint = Color.Black, modifier = Modifier.size(32.dp))
                }

                // 4. Studio IA
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigate("studio") }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Studio IA", tint = Color.Gray, modifier = Modifier.size(24.dp))
                    Text("Studio IA", color = Color.Gray, fontSize = 10.sp)
                }

                // 5. Profil
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigate("profile") }
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profil", tint = Color.Gray, modifier = Modifier.size(24.dp))
                    Text("Profil", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }

        // Creative Studio Dialog (triggered by grand "+" button)
        FullCreativeStudioDialog(
            isOpen = showCreateStudioDialog,
            userId = "current_user_id", // Placeholder
            authorName = "@PANU_Studio", // Placeholder
            postRepository = null, // Placeholder - should ideally be injected
            onDismiss = { showCreateStudioDialog = false },
            onPostCreated = { showCreateStudioDialog = false }
        )

        // Messages & Notifications Dialog
        if (showMessagesDialog) {
            AlertDialog(
                onDismissRequest = { showMessagesDialog = false },
                containerColor = Color(0xFF13151D),
                title = { Text("Messages & Notifications", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = Color(0xFF1E2230), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Nouveau commentaire de @Aminata", color = PanuGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Superbe vidéo sur le futur du cinéma !", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMessagesDialog = false }) {
                        Text("OK", color = PanuGold, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
