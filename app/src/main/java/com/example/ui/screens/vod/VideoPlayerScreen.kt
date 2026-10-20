package com.example.ui.screens.vod

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.example.ui.util.PanuMediaConstants

@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var hasError by remember { mutableStateOf(false) }
    var useFallback by remember { mutableStateOf(false) }

    val resolvedUrl = remember(videoUrl, useFallback) {
        if (useFallback) {
            PanuMediaConstants.DEFAULT_FALLBACK_VIDEO
        } else {
            PanuMediaConstants.resolvePlayableUrl(videoUrl)
        }
    }

    val exoPlayer = remember(resolvedUrl) {
        ExoPlayer.Builder(context).build().apply {
            try {
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 PANU/2.0")
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(20000)
                    .setReadTimeoutMs(20000)

                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(resolvedUrl))

                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            } catch (_: Exception) {
                hasError = true
            }
        }
    }

    DisposableEffect(resolvedUrl) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!useFallback) {
                    useFallback = true
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

    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var showSettings by remember { mutableStateOf(false) }

    fun handleBack() {
        if (onNavigateBack != null) {
            onNavigateBack()
        } else {
            (activity as? androidx.activity.ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    BackHandler {
        handleBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = !isLocked
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.useController = !isLocked }
        )

        // Overlay de contrôles avancés
        if (!isLocked) {
            // Bouton Retour en haut à gauche
            IconButton(
                onClick = { handleBack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            Row(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                // Bouton PiP
                IconButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                        activity?.enterPictureInPictureMode(params)
                    }
                }) {
                    Text("PiP", color = Color.White)
                }

                // Sélecteur de vitesse
                TextButton(onClick = {
                    playbackSpeed = if (playbackSpeed >= 2f) 0.5f else playbackSpeed + 0.5f
                    exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                }) {
                    Text("${playbackSpeed}x", color = Color.White)
                }

                // Bouton Paramètres
                IconButton(onClick = { showSettings = true }) {
                    Text("⚙️", color = Color.White)
                    DropdownMenu(expanded = showSettings, onDismissRequest = { showSettings = false }) {
                        DropdownMenuItem(text = { Text("Qualité HD Automatique") }, onClick = { showSettings = false })
                        DropdownMenuItem(text = { Text("Piste Audio Stéréo") }, onClick = { showSettings = false })
                        DropdownMenuItem(
                            text = { Text("Flux de secours") },
                            onClick = {
                                showSettings = false
                                useFallback = true
                            }
                        )
                    }
                }
            }
        }

        // Bouton de verrouillage
        IconButton(
            onClick = { isLocked = !isLocked },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Text(if (isLocked) "🔒" else "🔓", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
