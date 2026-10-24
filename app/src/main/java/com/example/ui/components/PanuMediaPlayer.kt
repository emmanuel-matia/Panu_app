@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ui.theme.PanuTheme
import com.example.ui.util.PanuMediaConstants
import kotlinx.coroutines.delay

@Composable
fun PanuMediaPlayer(
    mediaUrl: String,
    thumbnailUrl: String = "",
    title: String = "Média PANU",
    durationLabel: String = "10\"",
    isVideo: Boolean = true,
    mediaType: String = "", // "video", "image", "audio"
    modifier: Modifier = Modifier,
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = PanuTheme.colors
    val haptic = LocalHapticFeedback.current

    val resolvedType = remember(mediaUrl, mediaType, isVideo) {
        val lowerType = mediaType.lowercase()
        when {
            lowerType.contains("image") || lowerType.contains("photo") -> "image"
            lowerType.contains("audio") || lowerType.contains("music") || lowerType.contains("song") -> "audio"
            lowerType.contains("video") -> "video"
            mediaUrl.endsWith(".jpg", true) || mediaUrl.endsWith(".png", true) || mediaUrl.endsWith(".jpeg", true) || mediaUrl.endsWith(".webp", true) -> "image"
            mediaUrl.endsWith(".mp3", true) || mediaUrl.endsWith(".wav", true) || mediaUrl.endsWith(".m4a", true) || mediaUrl.endsWith(".aac", true) -> "audio"
            else -> if (isVideo) "video" else "image"
        }
    }

    // =========================================================================
    // 1. GESTION DES PHOTOS / IMAGES
    // =========================================================================
    if (resolvedType == "image" && mediaUrl.isNotBlank()) {
        var showFullImageDialog by remember { mutableStateOf(false) }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, colors.champagne.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("panu_image_player_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showFullImageDialog = true
                    }
            ) {
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Badge Photo
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, colors.champagne.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "PHOTO HD • $title", color = colors.champagne, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Légende & Actions en bas (sans aucun contrôle vidéo)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (onDownload != null) {
                            Surface(
                                color = colors.champagne,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { onDownload() }
                                    .testTag("image_btn_download")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Enregistrer", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFullImageDialog) {
            Dialog(onDismissRequest = { showFullImageDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { showFullImageDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }
            }
        }
        return
    }

    // =========================================================================
    // 2. GESTION DES FICHIERS AUDIO / CHANSONS
    // =========================================================================
    if (resolvedType == "audio" && mediaUrl.isNotBlank()) {
        var isAudioPlaying by remember { mutableStateOf(false) }
        var isAudioMuted by remember { mutableStateOf(false) }

        val audioPlayer = remember(mediaUrl) {
            ExoPlayer.Builder(context).build().apply {
                try {
                    val mediaSource = ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
                        .createMediaSource(MediaItem.fromUri(mediaUrl))
                    setMediaSource(mediaSource)
                    repeatMode = Player.REPEAT_MODE_ALL
                    prepare()
                } catch (_: Exception) {}
            }
        }

        DisposableEffect(mediaUrl) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isAudioPlaying = playing
                }
            }
            audioPlayer.addListener(listener)
            onDispose {
                audioPlayer.removeListener(listener)
                audioPlayer.release()
            }
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, colors.champagne.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("panu_audio_player_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = colors.champagne,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (audioPlayer.isPlaying) {
                                audioPlayer.pause()
                            } else {
                                audioPlayer.play()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAudioPlaying) "Pause" else "Lecture Audio",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "LECTEUR AUDIO PANU", color = colors.champagne, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = title, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(text = "Audio Haute Fidélité • $durationLabel", color = colors.textSecondary, fontSize = 11.sp)
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isAudioMuted = !isAudioMuted
                        audioPlayer.volume = if (isAudioMuted) 0f else 1f
                    }
                ) {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = colors.textPrimary
                    )
                }
            }
        }
        return
    }

    // =========================================================================
    // 3. GESTION DES VIDÉOS (EXOPLAYER + PLAYERVIEW)
    // =========================================================================
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0f) }
    var totalDuration by remember { mutableStateOf(100f) }
    var hasError by remember { mutableStateOf(false) }
    var useFallbackUrl by remember { mutableStateOf(false) }

    if (mediaUrl.isNotBlank()) {
        val exoPlayer = remember(mediaUrl, useFallbackUrl) {
            ExoPlayer.Builder(context).build().apply {
                try {
                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 PANU/2.0")
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(20000)
                        .setReadTimeoutMs(20000)

                    val targetUrl = if (useFallbackUrl) {
                        PanuMediaConstants.DEFAULT_FALLBACK_VIDEO
                    } else {
                        PanuMediaConstants.resolvePlayableUrl(mediaUrl)
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
                    if (!useFallbackUrl) {
                        useFallbackUrl = true
                        hasError = false
                    } else {
                        hasError = true
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }

        // Coroutine pour suivre la progression de la vidéo
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
                            Text("Fichier vidéo indisponible ou accès restreint", color = Color.White, fontSize = 12.sp)
                            Button(
                                onClick = {
                                    hasError = false
                                    useFallbackUrl = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                            ) {
                                Text("Basculer sur la vidéo de secours", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                    )

                    if (!isPlaying && thumbnailUrl.isNotBlank() && currentPosition < 200f) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // En-tête avec Badges Format et Mute
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
                        border = BorderStroke(1.dp, colors.champagne.copy(alpha = 0.6f))
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

                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

                // Barre de progression
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

                // Contrôles en bas
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
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
    }
}
