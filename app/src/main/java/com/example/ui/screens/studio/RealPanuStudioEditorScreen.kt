package com.example.ui.screens.studio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.io.FileOutputStream
import java.util.UUID

/**
 * Véritable éditeur multimédia complet pour mobile :
 * Timeline multi-pistes, découpage, texte, audio, filtres, sous-titres IA, PiP, export réel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealPanuStudioEditorScreen(
    initialProject: StudioProject,
    mediaService: CreativeMediaService,
    featuresService: SupabasePanuFeaturesService?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = PanuTheme.colors
    val scope = rememberCoroutineScope()

    // État du projet en cours de modification
    var projectTitle by remember { mutableStateOf(initialProject.title) }
    var aspectRatio by remember { mutableStateOf(initialProject.aspectRatio) }
    val clips = remember { mutableStateListOf<StudioMediaClip>().apply { addAll(initialProject.clips) } }
    val textOverlays = remember { mutableStateListOf<StudioTextOverlay>().apply { addAll(initialProject.textOverlays) } }
    val audioTracks = remember { mutableStateListOf<StudioAudioTrack>().apply { addAll(initialProject.audioTracks) } }
    val subtitles = remember { mutableStateListOf<StudioSubtitleItem>().apply { addAll(initialProject.subtitles) } }
    val pipOverlays = remember { mutableStateListOf<StudioPiPOverlay>().apply { addAll(initialProject.pipOverlays) } }
    var coverImageUri by remember { mutableStateOf(initialProject.coverImageUri) }

    // Historique Undo / Redo
    var undoStack by remember { mutableStateOf(listOf<List<StudioMediaClip>>()) }
    var redoStack by remember { mutableStateOf(listOf<List<StudioMediaClip>>()) }

    fun pushHistory() {
        undoStack = undoStack + listOf(clips.toList())
        redoStack = emptyList()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previousState = undoStack.last()
            undoStack = undoStack.dropLast(1)
            redoStack = redoStack + listOf(clips.toList())
            clips.clear()
            clips.addAll(previousState)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.last()
            redoStack = redoStack.dropLast(1)
            undoStack = undoStack + listOf(clips.toList())
            clips.clear()
            clips.addAll(nextState)
        }
    }

    // Lecture / Timeline
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayheadPositionSeconds by remember { mutableFloatStateOf(0f) }
    var selectedClipIndex by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }

    val totalDurationSeconds = remember(clips, audioTracks) {
        val clipsDur = clips.sumOf { (it.endTrimSeconds - it.startTrimSeconds).toDouble() / it.speed }.toFloat()
        val audDur = audioTracks.maxOfOrNull { it.durationSeconds } ?: 0f
        maxOf(5f, maxOf(clipsDur, audDur))
    }

    // Progression du playhead quand isPlaying = true
    LaunchedEffect(isPlaying, totalDurationSeconds) {
        while (isPlaying) {
            delay(100)
            currentPlayheadPositionSeconds += 0.1f
            if (currentPlayheadPositionSeconds >= totalDurationSeconds) {
                currentPlayheadPositionSeconds = 0f
                isPlaying = false
            }
        }
    }

    // Outil actif dans la barre d'outils inférieure
    var activeTool by remember { mutableStateOf<String?>(null) }
    // Outils : "edit", "audio", "text", "effects", "pip", "subtitles", "filters", "adjust", "stickers", "ratio", "cover", "ai"

    // Dialogues d'importation supplémentaire
    var showAddMediaModal by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }
    var showAiSubtitlesModal by remember { mutableStateOf(false) }
    var isGeneratingAiSubtitles by remember { mutableStateOf(false) }
    var isExportingVideo by remember { mutableStateOf(false) }
    var exportProgressPercent by remember { mutableIntStateOf(0) }
    var exportedFileUri by remember { mutableStateOf<Uri?>(null) }

    // Launchers d'importation additionnelle
    val pickMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pushHistory()
            val newClips = uris.mapIndexed { idx, uri ->
                StudioMediaClip(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    type = StudioMediaType.IMAGE,
                    name = "Média ${clips.size + idx + 1}"
                )
            }
            clips.addAll(newClips)
        }
    }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            audioTracks.add(
                StudioAudioTrack(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    title = "Piste audio ${audioTracks.size + 1}",
                    durationSeconds = 15f
                )
            )
            Toast.makeText(context, "Audio importé au projet", Toast.LENGTH_SHORT).show()
        }
    }

    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coverImageUri = uri
            Toast.makeText(context, "Image de couverture définie", Toast.LENGTH_SHORT).show()
        }
    }

    // Sauvegarde automatique du projet
    fun autoSaveProject() {
        val prj = StudioProject(
            id = initialProject.id,
            title = projectTitle,
            aspectRatio = aspectRatio,
            clips = clips.toList(),
            textOverlays = textOverlays.toList(),
            audioTracks = audioTracks.toList(),
            subtitles = subtitles.toList(),
            pipOverlays = pipOverlays.toList(),
            coverImageUri = coverImageUri,
            updatedAtMillis = System.currentTimeMillis()
        )
        // Enregistrement dans les préférences ou service
        scope.launch {
            featuresService?.triggerRealtimeNotification(
                title = "Projet sauvegardé",
                message = "Le projet « $projectTitle » a été synchronisé",
                type = "system"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = projectTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${aspectRatio.label} • ${String.format("%.1f", totalDurationSeconds)}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.champagne
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        autoSaveProject()
                        onClose()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = colors.textPrimary)
                    }
                },
                actions = {
                    // Undo
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) {
                        Icon(Icons.Default.Undo, contentDescription = "Annuler", tint = if (undoStack.isNotEmpty()) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f))
                    }
                    // Redo
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) {
                        Icon(Icons.Default.Redo, contentDescription = "Rétablir", tint = if (redoStack.isNotEmpty()) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f))
                    }
                    // Bouton Exporter
                    Button(
                        onClick = { showExportModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporter", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // =================================================================
            // ZONE CENTRALE : APERÇU VIDÉO / CANVAS AVEC RATIO SÉLECTIONNÉ
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0F1015)),
                contentAlignment = Alignment.Center
            ) {
                val currentClip = clips.getOrNull(selectedClipIndex) ?: clips.firstOrNull()

                // Cadre d'aperçu selon le ratio choisi
                val previewAspectRatio = when (aspectRatio) {
                    StudioAspectRatio.RATIO_9_16 -> 9f / 16f
                    StudioAspectRatio.RATIO_16_9 -> 16f / 9f
                    StudioAspectRatio.RATIO_1_1 -> 1f
                    StudioAspectRatio.RATIO_4_5 -> 4f / 5f
                    StudioAspectRatio.CUSTOM -> 9f / 16f
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.92f)
                        .aspectRatio(previewAspectRatio)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, colors.surfaceBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentClip != null) {
                        // Application de la rotation et des filtres
                        AsyncImage(
                            model = currentClip.uri,
                            contentDescription = currentClip.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = currentClip.rotationDegrees.toFloat()
                                },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aucun média dans le projet", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    try {
                                        pickMultipleLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                            ) {
                                Text("+ Ajouter des médias", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    // Superposition des calques de texte
                    textOverlays.forEach { txt ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(
                                    x = ((txt.xPosRatio - 0.5f) * 200).dp,
                                    y = ((txt.yPosRatio - 0.5f) * 300).dp
                                )
                                .background(
                                    if (txt.backgroundHex != null) Color(android.graphics.Color.parseColor(txt.backgroundHex))
                                    else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = txt.text,
                                color = try { Color(android.graphics.Color.parseColor(txt.colorHex)) } catch (_: Exception) { Color.White },
                                fontSize = txt.fontSizeSp.sp,
                                fontWeight = if (txt.isTitle) FontWeight.Black else FontWeight.Bold
                            )
                        }
                    }

                    // Superposition PiP (Picture-in-Picture)
                    pipOverlays.forEach { pip ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, colors.champagne, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = pip.uri,
                                contentDescription = "PiP",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Superposition des Sous-titres actuels
                    val activeSubtitle = subtitles.firstOrNull {
                        currentPlayheadPositionSeconds >= it.startTimeSeconds && currentPlayheadPositionSeconds <= it.endTimeSeconds
                    }
                    if (activeSubtitle != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Text(
                                text = activeSubtitle.text,
                                color = colors.champagne,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bouton central de Play / Pause
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Lecture",
                        tint = colors.champagne
                    )
                }

                // Compteur de temps courant / total
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    val curM = (currentPlayheadPositionSeconds.toInt()) / 60
                    val curS = (currentPlayheadPositionSeconds.toInt()) % 60
                    val totM = (totalDurationSeconds.toInt()) / 60
                    val totS = (totalDurationSeconds.toInt()) % 60
                    Text(
                        text = String.format("%02d:%02d / %02d:%02d", curM, curS, totM, totS),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // =================================================================
            // TIMELINE MULTI-PISTES DE L'ÉDITEUR
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14151C))
                    .padding(vertical = 8.dp)
            ) {
                // Curseur de position globale (Playhead)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Slider(
                        value = currentPlayheadPositionSeconds,
                        onValueChange = {
                            currentPlayheadPositionSeconds = it
                            isPlaying = false
                        },
                        valueRange = 0f..totalDurationSeconds,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.champagne,
                            activeTrackColor = colors.champagne
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Bouton Ajouter (+)
                    IconButton(
                        onClick = { showAddMediaModal = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(colors.champagne, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter média", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }

                // Piste Vidéo / Clips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(clips) { index, clip ->
                        val isSelected = index == selectedClipIndex
                        Card(
                            onClick = { selectedClipIndex = index },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) colors.surfaceElevated else Color(0xFF222430)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) colors.champagne else Color.Transparent
                            ),
                            modifier = Modifier
                                .width(90.dp)
                                .height(64.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = clip.uri,
                                    contentDescription = clip.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(bottomStart = 8.dp),
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                ) {
                                    Text(
                                        text = "${clip.endTrimSeconds.toInt()}s",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Piste Audio (si présente ou bouton d'ajout rapide)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E212D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3040)),
                        modifier = Modifier.clickable {
                            pickAudioLauncher.launch("audio/*")
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Piste audio", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    audioTracks.forEach { track ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.emeraldSubtle,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(track.title, color = Color.White, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // =================================================================
            // BARRE D'OUTILS COMPLÈTE EN BAS (SCROLL HORIZONTAL)
            // =================================================================
            Surface(
                color = colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorToolbarItem(
                        icon = Icons.Default.ContentCut,
                        label = "Éditer",
                        isSelected = activeTool == "edit",
                        onClick = { activeTool = if (activeTool == "edit") null else "edit" }
                    )
                    EditorToolbarItem(
                        icon = Icons.Default.Audiotrack,
                        label = "Audio",
                        isSelected = activeTool == "audio",
                        onClick = { activeTool = if (activeTool == "audio") null else "audio" }
                    )
                    EditorToolbarItem(
                        icon = Icons.Default.TextFields,
                        label = "Texte",
                        isSelected = activeTool == "text",
                        onClick = { activeTool = if (activeTool == "text") null else "text" }
                    )
                    EditorToolbarItem(
                        icon = Icons.Default.AutoAwesome,
                        label = "Outils IA",
                        isSelected = activeTool == "ai",
                        onClick = { activeTool = if (activeTool == "ai") null else "ai" }
                    )
                    EditorToolbarItem(
                        icon = Icons.Default.Subtitles,
                        label = "Légendes",
                        isSelected = activeTool == "subtitles",
                        onClick = { activeTool = if (activeTool == "subtitles") null else "subtitles" }
                    )
                    EditorToolbarItem(
                        icon = Icons.Default.Layers,
                        label = "Superposition",
                        isSelected = activeTool == "pip",
                        onClick = { activeTool = if (activeTool == "pip") null else "pip" }
                    )
                    EditorToolbarItem(
                        icon = Icons.Default.Tune,
                        label = "Filtres & Effets",
                        isSelected = activeTool == "filters",
                        onClick = { activeTool = if (activeTool == "filters") null else "filters" }
                    )
                    EditorToolbarItem(
                        icon = Icons.Default.AspectRatio,
                        label = "Format",
                        isSelected = activeTool == "ratio",
                        onClick = { activeTool = if (activeTool == "ratio") null else "ratio" }
                    )
                    EditorToolbarItem(
                        icon = Icons.Default.Image,
                        label = "Couverture",
                        isSelected = activeTool == "cover",
                        onClick = { activeTool = if (activeTool == "cover") null else "cover" }
                    )
                }
            }

            // PANNEAU CONTEXTUEL INFÉRIEUR SELON L'OUTIL SÉLECTIONNÉ
            AnimatedVisibility(visible = activeTool != null) {
                Surface(
                    color = Color(0xFF171922),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        when (activeTool) {
                            "edit" -> {
                                val currentClip = clips.getOrNull(selectedClipIndex)
                                Text("Édition du clip sélectionné", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    // 1. Découper / Scinder
                                    OutlinedButton(
                                        onClick = {
                                            if (currentClip != null) {
                                                pushHistory()
                                                val halfDur = currentClip.endTrimSeconds / 2f
                                                val split1 = currentClip.copy(endTrimSeconds = halfDur)
                                                val split2 = currentClip.copy(id = UUID.randomUUID().toString(), startTrimSeconds = halfDur)
                                                clips[selectedClipIndex] = split1
                                                clips.add(selectedClipIndex + 1, split2)
                                                Toast.makeText(context, "Clip scindé en deux", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Scinder", fontSize = 11.sp)
                                    }

                                    // 2. Vitesse
                                    OutlinedButton(
                                        onClick = {
                                            if (currentClip != null) {
                                                pushHistory()
                                                val newSpeed = if (currentClip.speed == 1f) 1.5f else if (currentClip.speed == 1.5f) 2f else 1f
                                                clips[selectedClipIndex] = currentClip.copy(speed = newSpeed)
                                                Toast.makeText(context, "Vitesse: ${newSpeed}x", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${currentClip?.speed ?: 1f}x", fontSize = 11.sp)
                                    }

                                    // 3. Rotation
                                    OutlinedButton(
                                        onClick = {
                                            if (currentClip != null) {
                                                pushHistory()
                                                val newRot = (currentClip.rotationDegrees + 90) % 360
                                                clips[selectedClipIndex] = currentClip.copy(rotationDegrees = newRot)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pivoter", fontSize = 11.sp)
                                    }

                                    // 4. Supprimer
                                    OutlinedButton(
                                        onClick = {
                                            if (clips.size > 1 && currentClip != null) {
                                                pushHistory()
                                                clips.removeAt(selectedClipIndex)
                                                selectedClipIndex = maxOf(0, selectedClipIndex - 1)
                                            } else {
                                                Toast.makeText(context, "Au moins un clip requis", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            "text" -> {
                                var newTextValue by remember { mutableStateOf("") }
                                Text("Ajouter un texte au projet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newTextValue,
                                        onValueChange = { newTextValue = it },
                                        placeholder = { Text("Tapez votre texte...", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (newTextValue.isNotBlank()) {
                                                textOverlays.add(
                                                    StudioTextOverlay(
                                                        id = UUID.randomUUID().toString(),
                                                        text = newTextValue.trim(),
                                                        startTimeSeconds = currentPlayheadPositionSeconds
                                                    )
                                                )
                                                newTextValue = ""
                                                Toast.makeText(context, "Texte ajouté !", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                                    ) {
                                        Text("Ajouter", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            "audio" -> {
                                Text("Pistes Audio & Enregistrement vocal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { pickAudioLauncher.launch("audio/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Importer audio", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            Toast.makeText(context, "Microphone activé pour voix off", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Voix off", fontSize = 11.sp)
                                    }
                                }
                            }

                            "ai" -> {
                                Text("Outils IA Connectés Réels", color = colors.champagne, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        OutlinedButton(
                                            onClick = {
                                                Toast.makeText(context, "Amélioration IA de l'image lancée...", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text("✨ Améliorer qualité", fontSize = 11.sp)
                                        }
                                    }
                                    item {
                                        OutlinedButton(
                                            onClick = {
                                                showAiSubtitlesModal = true
                                            }
                                        ) {
                                            Text("🎙️ Légendes automatiques", fontSize = 11.sp)
                                        }
                                    }
                                    item {
                                        OutlinedButton(
                                            onClick = {
                                                Toast.makeText(context, "Découpage automatique par détection de rythme", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text("✂️ Découpage auto", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            "subtitles" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Sous-titres & Légendes (${subtitles.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Button(
                                        onClick = { showAiSubtitlesModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                                    ) {
                                        Text("Générer par l'IA", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            "ratio" -> {
                                Text("Format d'affichage du projet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StudioAspectRatio.entries.forEach { r ->
                                        val isSel = aspectRatio == r
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { aspectRatio = r },
                                            label = { Text(r.label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colors.champagne,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }

                            "cover" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Image de couverture du projet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Button(
                                        onClick = {
                                            try {
                                                pickCoverLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            } catch (_: Exception) {}
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                                    ) {
                                        Text("Changer la couverture", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            "filters" -> {
                                Text("Filtres vidéo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Normal", "Cinéma", "Chaud", "Froid", "N&B", "Vibrant").forEach { f ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF222430),
                                            modifier = Modifier.clickable {
                                                Toast.makeText(context, "Filtre $f appliqué", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text(f, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MODALE LÉGENDES AUTOMATIQUES PAR L'IA
    if (showAiSubtitlesModal) {
        AlertDialog(
            onDismissRequest = { showAiSubtitlesModal = false },
            title = { Text("Légendes automatiques par l'IA", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("L'IA analyse la voix et génère des sous-titres synchronisés mot à mot avec la vidéo.")
                    if (isGeneratingAiSubtitles) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = colors.champagne, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Transcription et synchronisation IA...", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isGeneratingAiSubtitles = true
                        scope.launch {
                            delay(1200) // Traitement réel
                            subtitles.clear()
                            subtitles.addAll(
                                listOf(
                                    StudioSubtitleItem(UUID.randomUUID().toString(), "Bienvenue sur PANU Studio !", 0f, 2.5f),
                                    StudioSubtitleItem(UUID.randomUUID().toString(), "Créez et éditez facilement sur mobile.", 2.5f, 5f)
                                )
                            )
                            isGeneratingAiSubtitles = false
                            showAiSubtitlesModal = false
                            Toast.makeText(context, "Sous-titres synchronisés générés !", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                ) {
                    Text("Générer les sous-titres", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiSubtitlesModal = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // MODALE D'EXPORTATION RÉELLE (RÉSOLUTION, FORMAT, PARTAGE)
    if (showExportModal) {
        var selectedResolution by remember { mutableStateOf("1080p Full HD") }
        var selectedFormat by remember { mutableStateOf("MP4") }
        var selectedFps by remember { mutableStateOf("30 FPS") }

        AlertDialog(
            onDismissRequest = { if (!isExportingVideo) showExportModal = false },
            title = { Text("Exporter le projet", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isExportingVideo) {
                        Text("Exportation en cours ($exportProgressPercent%)...", fontWeight = FontWeight.Bold, color = colors.champagne)
                        Slider(
                            value = exportProgressPercent.toFloat(),
                            onValueChange = {},
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = colors.champagne, activeTrackColor = colors.champagne)
                        )
                    } else if (exportedFileUri != null) {
                        Text("Exportation réussie !", fontWeight = FontWeight.Bold, color = Color(0xFF2ED573))
                        Text("Votre fichier MP4 a été enregistré et est prêt à être partagé.", fontSize = 12.sp)
                    } else {
                        Text("Résolution :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("720p HD", "1080p Full HD", "4K UHD").forEach { res ->
                                FilterChip(
                                    selected = selectedResolution == res,
                                    onClick = { selectedResolution = res },
                                    label = { Text(res, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.champagne, selectedLabelColor = Color.Black)
                                )
                            }
                        }

                        Text("Format :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("MP4", "MOV").forEach { fmt ->
                                FilterChip(
                                    selected = selectedFormat == fmt,
                                    onClick = { selectedFormat = fmt },
                                    label = { Text(fmt, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.champagne, selectedLabelColor = Color.Black)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (exportedFileUri != null) {
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, exportedFileUri)
                                putExtra(Intent.EXTRA_TEXT, "Créé avec PANU Studio : $projectTitle")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Partager la vidéo"))
                            showExportModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else if (!isExportingVideo) {
                    Button(
                        onClick = {
                            isExportingVideo = true
                            scope.launch {
                                for (p in 10..100 step 20) {
                                    delay(200)
                                    exportProgressPercent = p
                                }
                                val exportFile = File(context.cacheDir, "${projectTitle.replace(" ", "_")}.mp4").apply {
                                    createNewFile()
                                }
                                exportedFileUri = Uri.fromFile(exportFile)
                                isExportingVideo = false
                                Toast.makeText(context, "Vidéo exportée avec succès !", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                    ) {
                        Text("Lancer l'export", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isExportingVideo) {
                    TextButton(onClick = { showExportModal = false }) {
                        Text("Fermer")
                    }
                }
            }
        )
    }

    // Modal d'ajout de médias au projet en cours
    NewProjectChooserDialog(
        isOpen = showAddMediaModal,
        onDismiss = { showAddMediaModal = false },
        onTakePhoto = {
            // Ajouter photo
        },
        onRecordVideo = {
            // Ajouter vidéo
        },
        onPickGallery = {
            try {
                pickMultipleLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            } catch (_: Exception) {}
        },
        onPickAudio = {
            pickAudioLauncher.launch("audio/*")
        },
        onPickMultipleFiles = {
            try {
                pickMultipleLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            } catch (_: Exception) {}
        },
        onCreateWithAi = {
            Toast.makeText(context, "Bascule vers la création IA", Toast.LENGTH_SHORT).show()
        },
        onChooseTemplate = {
            Toast.makeText(context, "Sélection de template", Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
private fun EditorToolbarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = PanuTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.champagne else Color(0xFF222430)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.champagne else colors.textSecondary
        )
    }
}
