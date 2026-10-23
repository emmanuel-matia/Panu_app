package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.data.repository.PostRepository
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanuCommentsBottomSheet(
    post: Post,
    postRepository: PostRepository,
    featuresService: SupabasePanuFeaturesService?,
    isLoggedIn: Boolean,
    onNavigateToAuth: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val comments = remember { mutableStateListOf<PostComment>() }
    var isLoading by remember { mutableStateOf(true) }
    var newCommentText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val likedComments = remember { mutableStateMapOf<String, Boolean>() }
    val commentLikesCount = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(post.id) {
        isLoading = true
        val loaded = postRepository.getComments(post.id)
        comments.clear()
        if (loaded.isNotEmpty()) {
            comments.addAll(loaded)
        } else {
            // Premier commentaire dynamique d'accueil pour la publication
            comments.add(
                PostComment(
                    id = "welcome_${post.id}",
                    postId = post.id,
                    authorId = post.authorId,
                    authorName = post.authorName ?: "Créateur PANU",
                    authorUsername = post.authorUsername ?: "createur_panu",
                    authorAvatarUrl = post.authorAvatarUrl,
                    content = "Bienvenue dans l'espace d'échange sur « ${post.title ?: "cette création"} » ! Donnez votre avis.",
                    createdAt = "Créateur"
                )
            )
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceElevated,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = RoundedCornerShape(2.dp),
                color = colors.textSecondary.copy(alpha = 0.4f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // En-tête
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Commentaires (${comments.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = colors.textPrimary
                    )
                    Text(
                        text = post.title ?: "Publication PANU",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.champagne,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = colors.textSecondary)
                }
            }

            HorizontalDivider(color = colors.surfaceBorder)

            // Liste des commentaires
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.champagne)
                }
            } else if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💬", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aucun commentaire pour l'instant",
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            "Soyez le premier à réagir à cette vidéo !",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        val isLiked = likedComments[comment.id] ?: false
                        val currentLikes = commentLikesCount[comment.id] ?: comment.likesCount

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            PanuAvatar(
                                avatarUrl = comment.authorAvatarUrl,
                                fullName = comment.authorName,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.authorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "@${comment.authorUsername ?: "membre"}",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${comment.createdAt}",
                                        fontSize = 10.sp,
                                        color = colors.textSecondary.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.content,
                                    fontSize = 13.sp,
                                    color = colors.textPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val next = !isLiked
                                    likedComments[comment.id] = next
                                    commentLikesCount[comment.id] = if (next) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Aimer",
                                    tint = if (isLiked) Color(0xFFFF4757) else colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.surfaceBorder)

            // Barre de saisie
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = {
                        Text(
                            "Ajouter un commentaire...",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.champagne,
                        unfocusedBorderColor = colors.surfaceBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = newCommentText.trim()
                        if (text.isBlank() || isSending) return@IconButton

                        if (!isLoggedIn) {
                            Toast.makeText(context, "Connectez-vous pour commenter.", Toast.LENGTH_SHORT).show()
                            onNavigateToAuth()
                            return@IconButton
                        }

                        val session = featuresService?.sessionManager
                        val authorId = session?.currentUserId?.value ?: "user_${System.currentTimeMillis()}"
                        val authorEmail = session?.currentUserEmail?.value ?: "user@panu.app"
                        val authorName = authorEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        val authorHandle = authorEmail.substringBefore("@").lowercase()

                        val newComment = PostComment(
                            id = UUID.randomUUID().toString(),
                            postId = post.id,
                            authorId = authorId,
                            authorName = authorName,
                            authorUsername = authorHandle,
                            authorAvatarUrl = null,
                            content = text,
                            likesCount = 0,
                            createdAt = "À l'instant"
                        )

                        isSending = true
                        comments.add(0, newComment)
                        newCommentText = ""

                        scope.launch {
                            val res = postRepository.addComment(newComment)
                            isSending = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Commentaire publié !", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = newCommentText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (newCommentText.isNotBlank()) colors.champagne else colors.champagneSubtle)
                        .testTag("btn_send_comment")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = if (newCommentText.isNotBlank()) Color.Black else colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
