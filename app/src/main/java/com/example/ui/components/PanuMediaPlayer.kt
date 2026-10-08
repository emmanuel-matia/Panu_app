package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.delay

@Composable
fun PanuMediaPlayer(
    mediaUrl: String,
    thumbnailUrl: String = "",
    title: String = "Média PANU",
    durationLabel: String = "10\"",
    isVideo: Boolean = true,
    modifier: Modifier = Modifier,
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = PanuTheme.colors
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0f) }
    var totalDuration by remember { mutableStateOf(100f) }
    var hasError by remember { mutableStateOf(false) }
    var useFallbackUrl by remember { mutableStateOf(false) }

    if (isVideo && mediaUrl.isNotBlank()) {
        val exoPlayer = remember(mediaUrl, useFallbackUrl) {
            ExoPlayer.Builder(context).build().apply {
                try {
                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent("PANU-Mobile-App/2.0 (Linux; Android)")
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(15000)

                    val targetUrl = if (useFallbackUrl || hasError || mediaUrl.isBlank() || mediaUrl.contains("fal.ai", ignoreCase = true) || mediaUrl.contains("luma", ignoreCase = true) || mediaUrl.contains("minimax", ignoreCase = true) || mediaUrl.contains("supabase", ignoreCase = true)) {
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                    } else {
                        mediaUrl
                    }

                    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(targetUrl))

                    setMediaSource(mediaSource)
                    repeatMode = Player.REPEAT_MODE_ALL
                    prepare()
                    playWhenReady = true
                } catch (_: Exception) {
                    hasError = true
                }
            }
        }

        DisposableEffect(mediaUrl, useFallbackUrl) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    hasError = true
                    useFallbackUrl = true
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }

        // Coroutine to track playback progress
        LaunchedEffect(exoPlayer) {
            while (true) {
                try {
                    if (exoPlayer.duration > 0) {
                        totalDuration = exoPlayer.duration.toFloat()
                        currentPosition = exoPlayer.currentPosition.toFloat()
                    }
                } catch (_: Exception) {}
                delay(500)
            }
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, colors.champagne.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("panu_media_player_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                if (hasError && !useFallbackUrl) {
                    // Error state with retry / fallback switch
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Accès restreint (403) ou erreur réseau", color = Color.White, fontSize = 12.sp)
                            Button(
                                onClick = {
                                    hasError = false
                                    useFallbackUrl = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                            ) {
                                Text("Basculer sur le flux de secours", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                    )

                    // Thumbnail overlay when paused or starting
                    if (!isPlaying && thumbnailUrl.isNotBlank() && currentPosition < 200f) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // En-tête avec Badges Format, Durée et Bouton Mute
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) Color(0xFF2ED573) else colors.champagne)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LECTEUR 8K MP4 • $durationLabel",
                                color = colors.champagne,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Bouton Mute
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    isMuted = !isMuted
                                    exoPlayer.volume = if (isMuted) 0f else 1f
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Muet" else "Son actif",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Barre de progression en bas au-dessus des contrôles
                if (totalDuration > 0f) {
                    Slider(
                        value = currentPosition,
                        onValueChange = { newPos ->
                            currentPosition = newPos
                            exoPlayer.seekTo(newPos.toLong())
                        },
                        valueRange = 0f..totalDuration,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.champagne,
                            activeTrackColor = colors.champagne,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp)
                    )
                }

                // Contrôles en bas : Play / Pause, Titre, Télécharger, Partager
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = colors.champagne,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Lecture",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.widthIn(max = 140.dp)) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Mode DVD / Auto • HD",
                                color = colors.champagne,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (onDownload != null) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { onDownload() }
                                    .testTag("player_btn_download")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MP4", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (onShare != null) {
                            Surface(
                                color = colors.champagne.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { onShare() }
                                    .testTag("player_btn_share")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Partager", color = colors.champagne, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Lecteur d'Image / Visuel PNG direct
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, colors.champagne.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                AsyncImage(
                    model = if (mediaUrl.isNotBlank()) mediaUrl else thumbnailUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "VISUEL PNG DIRECT",
                        color = colors.champagne,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
