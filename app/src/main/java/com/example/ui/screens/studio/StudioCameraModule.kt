package com.example.ui.screens.studio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.StudioAspectRatio
import com.example.data.model.StudioAudioTrack
import com.example.data.model.StudioMediaClip
import com.example.data.model.StudioMediaType
import com.example.data.model.StudioPiPOverlay
import com.example.data.model.StudioProject
import com.example.data.model.StudioSubtitleItem
import com.example.data.model.StudioTextOverlay
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.data.repository.ai.CreativeMediaService
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Dialogue de choix initial lorsqu'on clique sur « Nouveau projet »
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectChooserDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    onPickGallery: () -> Unit,
    onPickAudio: () -> Unit,
    onPickMultipleFiles: () -> Unit,
    onCreateWithAi: () -> Unit,
    onChooseTemplate: () -> Unit
) {
    if (!isOpen) return
    val colors = PanuTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nouveau projet",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Choisissez comment démarrer votre création",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = colors.textSecondary)
                }
            }

            // Options en grille ou liste d'actions
            val options = listOf(
                NewProjectOption(
                    title = "📷 Prendre une photo",
                    subtitle = "Appareil photo intégré avec flash et zoom",
                    action = { onDismiss(); onTakePhoto() }
                ),
                NewProjectOption(
                    title = "🎥 Enregistrer une vidéo",
                    subtitle = "Caméra avant/arrière, durée en direct",
                    action = { onDismiss(); onRecordVideo() }
                ),
                NewProjectOption(
                    title = "🖼️ Importer depuis la galerie",
                    subtitle = "Photos et vidéos locales du téléphone",
                    action = { onDismiss(); onPickGallery() }
                ),
                NewProjectOption(
                    title = "📁 Importer plusieurs fichiers",
                    subtitle = "Sélectionnez plusieurs médias en une fois",
                    action = { onDismiss(); onPickMultipleFiles() }
                ),
                NewProjectOption(
                    title = "🎵 Importer un fichier audio",
                    subtitle = "Musique de fond, sonorités ou voix",
                    action = { onDismiss(); onPickAudio() }
                ),
                NewProjectOption(
                    title = "✨ Créer avec l'IA",
                    subtitle = "Génération par MiniMax, Kling, Luma ou Flux",
                    action = { onDismiss(); onCreateWithAi() }
                ),
                NewProjectOption(
                    title = "📐 Choisir un modèle/template",
                    subtitle = "Templates prêts à l'emploi par catégorie",
                    action = { onDismiss(); onChooseTemplate() }
                )
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                items(options) { opt ->
                    Card(
                        onClick = opt.action,
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = opt.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Icon(Icons.Default.Add, contentDescription = null, tint = colors.champagne)
                        }
                    }
                }
            }
        }
    }
}

private data class NewProjectOption(
    val title: String,
    val subtitle: String,
    val action: () -> Unit
)

/**
 * 2 & 3. MODULE CAMÉRA ET ENREGISTREMENT VIDÉO RÉEL AVEC APERÇU ET ENREGISTREMENT DANS LE PROJET
 */
@Composable
fun RealCameraStudioModule(
    isRecordingMode: Boolean,
    onMediaCaptured: (Uri, StudioMediaType) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = PanuTheme.colors

    var cameraFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var isVideoPaused by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var capturedTempUri by remember { mutableStateOf<Uri?>(null) }
    var isReviewingCapture by remember { mutableStateOf(false) }
    var lastCapturedType by remember { mutableStateOf(StudioMediaType.IMAGE) }

    // Objets CameraX
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    // Minuterie de compte à rebours
    var countdownTimerSeconds by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Configuration de la caméra CameraX
    LaunchedEffect(cameraFacing, flashEnabled) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imgCap = ImageCapture.Builder()
                .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build()
            imageCapture = imgCap

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            val vidCap = VideoCapture.withOutput(recorder)
            videoCapture = vidCap

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imgCap,
                    vidCap
                )
                cameraControl = camera.cameraControl
                camera.cameraControl.setZoomRatio(zoomRatio)
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur caméra: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Incrémentation du compteur lors d'un enregistrement vidéo réel
    LaunchedEffect(isRecordingVideo, isVideoPaused) {
        while (isRecordingVideo && !isVideoPaused) {
            delay(1000)
            recordingDurationSeconds += 1
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!isReviewingCapture) {
            // Vue caméra réelle
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Barre supérieure de contrôles (Fermer, Flash, Changement de caméra, Minuterie)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }

                // Affichage durée d'enregistrement si vidéo en cours
                if (isRecordingVideo) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Red.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val mins = recordingDurationSeconds / 60
                            val secs = recordingDurationSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Flash
                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (flashEnabled) colors.champagne else Color.White
                        )
                    }

                    // Basculer Caméra avant / arrière
                    IconButton(
                        onClick = {
                            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Tourner la caméra", tint = Color.White)
                    }
                }
            }

            // Boutons de Zoom (1x, 2x)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                listOf(1f, 2f).forEach { z ->
                    val isSel = zoomRatio == z
                    Surface(
                        shape = CircleShape,
                        color = if (isSel) colors.champagne else Color.Transparent,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                zoomRatio = z
                                cameraControl?.setZoomRatio(z)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${z.toInt()}x",
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Barre inférieure de Déclenchement (Photo ou Vidéo)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecordingMode) {
                        // MODE ENREGISTREMENT VIDÉO RÉEL
                        if (isRecordingVideo) {
                            // Bouton Pause / Reprise
                            IconButton(
                                onClick = {
                                    if (isVideoPaused) {
                                        activeRecording?.resume()
                                        isVideoPaused = false
                                    } else {
                                        activeRecording?.pause()
                                        isVideoPaused = true
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isVideoPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White
                                )
                            }

                            // Bouton Arrêt de l'enregistrement vidéo
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                                    .background(Color.Red)
                                    .clickable {
                                        activeRecording?.stop()
                                        activeRecording = null
                                        isRecordingVideo = false
                                        isVideoPaused = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(Color.White, RoundedCornerShape(4.dp))
                                )
                            }

                            Spacer(modifier = Modifier.size(48.dp))
                        } else {
                            // Démarrage de l'enregistrement vidéo
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                                    .background(Color.Red)
                                    .clickable {
                                        val videoFile = File(context.cacheDir, "vid_${System.currentTimeMillis()}.mp4")
                                        val outputOptions = FileOutputOptions.Builder(videoFile).build()
                                        val recording = videoCapture?.output
                                            ?.prepareRecording(context, outputOptions)
                                            ?.apply {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                    withAudioEnabled()
                                                }
                                            }
                                            ?.start(ContextCompat.getMainExecutor(context)) { event ->
                                                when (event) {
                                                    is VideoRecordEvent.Finalize -> {
                                                        if (!event.hasError()) {
                                                            val savedUri = Uri.fromFile(videoFile)
                                                            capturedTempUri = savedUri
                                                            lastCapturedType = StudioMediaType.VIDEO
                                                            isReviewingCapture = true
                                                        } else {
                                                            Toast.makeText(context, "Erreur d'enregistrement", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        activeRecording = recording
                                        isRecordingVideo = true
                                        recordingDurationSeconds = 0
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    } else {
                        // MODE CAPTURE PHOTO RÉELLE
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .background(Color.White.copy(alpha = 0.3f))
                                .clickable {
                                    val photoFile = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    imageCapture?.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                val savedUri = Uri.fromFile(photoFile)
                                                capturedTempUri = savedUri
                                                lastCapturedType = StudioMediaType.IMAGE
                                                isReviewingCapture = true
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                Toast.makeText(context, "Erreur photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        } else {
            // APERÇU IMMÉDIAT AVEC OPTION DE REPRENDRE OU VALIDER DANS LE PROJET
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (lastCapturedType == StudioMediaType.IMAGE) "Aperçu de la photo" else "Aperçu de la vidéo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2028)),
                    contentAlignment = Alignment.Center
                ) {
                    if (lastCapturedType == StudioMediaType.IMAGE) {
                        AsyncImage(
                            model = capturedTempUri,
                            contentDescription = "Aperçu",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Aperçu de la vidéo capturée
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Vidéo capturée (${recordingDurationSeconds}s)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            capturedTempUri = null
                            isReviewingCapture = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reprendre")
                    }

                    Button(
                        onClick = {
                            capturedTempUri?.let { uri ->
                                onMediaCaptured(uri, lastCapturedType)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter au projet", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
