package com.example.ui.screens.studio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.CanvasLayer
import com.example.data.model.CanvasLayerType
import com.example.data.model.CanvasProject
import com.example.data.model.PanuTemplate
import com.example.data.model.PanuTemplateField
import com.example.data.model.StudioAudioTrack
import com.example.data.model.StudioMediaClip
import com.example.data.model.StudioMediaType
import com.example.data.model.StudioProject
import com.example.data.model.VideoStylePreset
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.data.repository.GeminiRepository
import com.example.data.repository.ai.CreativeMediaService
import com.example.data.repository.ai.GroundingService
import com.example.data.repository.ai.VideoSequenceSegment
import com.example.ui.components.CreatorFundPayoutDialog
import com.example.ui.components.FiveSecondVideoAdDialog
import com.example.ui.components.MobileMoneyCreditsStoreDialog
import com.example.ui.components.PanuBottomNav
import com.example.ui.components.PanuMediaPlayer
import com.example.ui.components.PanuTopBar
import com.example.ui.components.VisibilityBoosterDialog
import com.example.ui.navigation.PanuScreen
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun StudioScreen(
    mediaService: CreativeMediaService,
    groundingService: GroundingService,
    geminiRepository: GeminiRepository? = null,
    featuresService: SupabasePanuFeaturesService? = null,
    isFounder: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val colors = PanuTheme.colors
    val scope = rememberCoroutineScope()
    val pendingSyncCount = featuresService?.pendingOfflineSyncCount?.collectAsState()?.value ?: 0
    var selectedStudioTab by remember { mutableIntStateOf(0) }
    // 0: Studio Vidéo PANU, 1: Templates PANU, 2: Création Graphique PANU, 3: Films & Séries IA

    var selectedTemplateForCustomization by remember { mutableStateOf<PanuTemplate?>(null) }

    Scaffold(
        topBar = {
            Column {
                PanuTopBar(
                    canNavigateBack = onNavigateBack != null,
                    onNavigateBack = { onNavigateBack?.invoke() }
                )
                Surface(
                    color = colors.emeraldSubtle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAiConfigured = mediaService.isAnyAiEngineConfigured()
                        Text(
                            text = if (pendingSyncCount > 0)
                                "⚡ Mode Hors-ligne PANU • $pendingSyncCount création(s) en attente"
                            else if (isAiConfigured)
                                "🟢 IA connectée • Studio PANU actif"
                            else
                                "🟡 Configuration requise • Ajoutez votre clé API IA",
                            color = if (isAiConfigured || pendingSyncCount > 0) colors.emerald else Color(0xFFFFB300),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (pendingSyncCount > 0) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        featuresService?.syncPendingOfflineCreations()
                                    }
                                }
                            ) {
                                Text("Synchroniser", color = colors.champagne, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                ScrollableTabRow(
                    selectedTabIndex = selectedStudioTab,
                    containerColor = colors.surface,
                    contentColor = colors.champagne,
                    edgePadding = 12.dp
                ) {
                    Tab(
                        selected = selectedStudioTab == 0,
                        onClick = { selectedStudioTab = 0 },
                        text = { Text("🎬 Studio Vidéo PANU", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_video_studio")
                    )
                    Tab(
                        selected = selectedStudioTab == 1,
                        onClick = { selectedStudioTab = 1 },
                        text = { Text("📋 Templates PANU", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.ViewModule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_templates_panu")
                    )
                    Tab(
                        selected = selectedStudioTab == 2,
                        onClick = { selectedStudioTab = 2 },
                        text = { Text("🎨 Création IA PANU", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_graphic_studio")
                    )
                    if (geminiRepository != null) {
                        Tab(
                            selected = selectedStudioTab == 3,
                            onClick = { selectedStudioTab = 3 },
                            text = { Text("📽️ Films & Séries IA", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.MovieFilter, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_series_studio")
                        )
                    }
                }
            }
        },
        bottomBar = {
            PanuBottomNav(
                currentRoute = PanuScreen.Studio.route,
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
            when (selectedStudioTab) {
                0 -> PanuAiVideoStudioTab(
                    mediaService = mediaService,
                    featuresService = featuresService,
                    onNavigateToTemplates = { selectedStudioTab = 1 },
                    onNavigateToCreations = { onNavigate(PanuScreen.Creations.route) }
                )
                1 -> PanuTemplatesCatalogTab(
                    featuresService = featuresService,
                    onSelectTemplate = { tpl ->
                        selectedTemplateForCustomization = tpl
                    },
                    onCreateWithAi = { selectedStudioTab = 0 }
                )
                2 -> PanuGraphicStudioTab(featuresService = featuresService)
                3 -> {
                    if (geminiRepository != null) {
                        SeriesGeneratorScreen(
                            repository = geminiRepository,
                            mediaService = mediaService,
                            featuresService = featuresService
                        )
                    }
                }
            }

            // Modal / Dialog de Personnalisation Réelle du Template PANU
            selectedTemplateForCustomization?.let { template ->
                PanuTemplateCustomizerDialog(
                    template = template,
                    mediaService = mediaService,
                    featuresService = featuresService,
                    onDismiss = { selectedTemplateForCustomization = null },
                    onSuccess = { generatedVideoUrl ->
                        selectedTemplateForCustomization = null
                        selectedStudioTab = 0 // Switch to studio video player to preview
                    }
                )
            }
        }
    }
}

// =============================================================================
// 1. STUDIO VIDÉO IA PANU (MOTEURS MINIMAX, KLING, LUMA, FLUX)
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanuAiVideoStudioTab(
    mediaService: CreativeMediaService,
    featuresService: SupabasePanuFeaturesService?,
    onNavigateToTemplates: () -> Unit,
    onNavigateToCreations: () -> Unit
) {
    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoTitle by remember { mutableStateOf("") }
    var promptText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(VideoStylePreset.CINEMATIC) }
    var selectedAiEngine by remember { mutableStateOf("Google Gemini AI") }
    val aiEngines = listOf("Google Gemini AI", "MiniMax/Hailuo", "Kling", "Luma", "Flux/BFL")
    var selectedDuration by remember { mutableStateOf("10\"") }
    var selectedAspectRatio by remember { mutableStateOf("9:16") }

    var isGeneratingVideo by remember { mutableStateOf(false) }
    var generatedVideoUrl by remember { mutableStateOf<String?>(null) }
    var generatedThumbUrl by remember { mutableStateOf<String?>(null) }
    var generationStatusText by remember { mutableStateOf<String?>(null) }
    var generationError by remember { mutableStateOf<String?>(null) }

    // Nouveaux états et launchers pour le Module Nouveau Projet & Éditeur Mobile Réel
    var showNewProjectChooser by remember { mutableStateOf(false) }
    var activeCameraMode by remember { mutableStateOf<Boolean?>(null) }
    var activeEditorProject by remember { mutableStateOf<StudioProject?>(null) }
    var pendingCameraAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingCameraAction?.invoke()
        } else {
            Toast.makeText(context, "Permission caméra requise", Toast.LENGTH_SHORT).show()
        }
        pendingCameraAction = null
    }

    fun launchCameraChecked(isRecording: Boolean) {
        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasCam) {
            activeCameraMode = isRecording
        } else {
            pendingCameraAction = { activeCameraMode = isRecording }
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val clipsList = uris.mapIndexed { idx, uri ->
                StudioMediaClip(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    type = StudioMediaType.IMAGE,
                    name = "Média ${idx + 1}"
                )
            }
            activeEditorProject = StudioProject(
                id = UUID.randomUUID().toString(),
                title = "Projet Galerie PANU",
                clips = clipsList
            )
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val audioTrack = StudioAudioTrack(
                id = UUID.randomUUID().toString(),
                uri = uri,
                title = "Piste audio importée"
            )
            activeEditorProject = StudioProject(
                id = UUID.randomUUID().toString(),
                title = "Projet Audio PANU",
                audioTracks = listOf(audioTrack)
            )
        }
    }

    if (activeEditorProject != null) {
        RealPanuStudioEditorScreen(
            initialProject = activeEditorProject!!,
            mediaService = mediaService,
            featuresService = featuresService,
            onClose = { activeEditorProject = null }
        )
        return
    }

    if (activeCameraMode != null) {
        RealCameraStudioModule(
            isRecordingMode = activeCameraMode!!,
            onMediaCaptured = { uri, type ->
                activeCameraMode = null
                activeEditorProject = StudioProject(
                    id = UUID.randomUUID().toString(),
                    title = "Projet Capture Caméra",
                    clips = listOf(
                        StudioMediaClip(
                            id = UUID.randomUUID().toString(),
                            uri = uri,
                            type = type,
                            name = "Capture"
                        )
                    )
                )
            },
            onClose = { activeCameraMode = null }
        )
        return
    }

    NewProjectChooserDialog(
        isOpen = showNewProjectChooser,
        onDismiss = { showNewProjectChooser = false },
        onTakePhoto = { launchCameraChecked(false) },
        onRecordVideo = { launchCameraChecked(true) },
        onPickGallery = {
            try {
                galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            } catch (_: Exception) {}
        },
        onPickAudio = {
            audioPickerLauncher.launch("audio/*")
        },
        onPickMultipleFiles = {
            try {
                galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            } catch (_: Exception) {}
        },
        onCreateWithAi = {
            showNewProjectChooser = false
        },
        onChooseTemplate = {
            onNavigateToTemplates()
        }
    )

    val creditsBalance = featuresService?.panuCreditsBalance?.collectAsState()?.value ?: 60
    var showCreditsStoreModal by remember { mutableStateOf(false) }
    var showBoosterModal by remember { mutableStateOf(false) }

    fun executeAiVideoGeneration() {
        val targetPrompt = promptText.trim()
        if (targetPrompt.isBlank() || isGeneratingVideo) return

        isGeneratingVideo = true
        generationError = null
        val cleanTitle = if (videoTitle.isNotBlank()) videoTitle.trim() else "Vidéo IA PANU"
        generationStatusText = "Génération en cours… (Envoi du prompt au moteur $selectedAiEngine)"

        scope.launch {
            val enrichedPrompt = "$targetPrompt, style ${selectedPreset.labelFr}, ratio $selectedAspectRatio"
            val result = mediaService.generateMediaWithDuration(
                prompt = enrichedPrompt,
                title = cleanTitle,
                durationLabel = selectedDuration,
                stylePreset = selectedPreset.labelFr,
                aiEngine = selectedAiEngine,
                onProgress = { statusMsg, _ ->
                    generationStatusText = statusMsg
                }
            )

            result.fold(
                onSuccess = { media ->
                    generatedVideoUrl = media.mediaUrl
                    generatedThumbUrl = media.thumbnailUrl
                    generationStatusText = null
                    isGeneratingVideo = false

                    // Enregistrement systématique dans la base de données et dans « Mes créations »
                    try {
                        featuresService?.saveGeneratedAiVideoToSupabase(
                            title = cleanTitle,
                            prompt = enrichedPrompt,
                            stylePreset = selectedPreset.labelFr,
                            videoUrl = media.mediaUrl,
                            thumbnailUrl = media.thumbnailUrl
                        )
                        val canvasProject = CanvasProject(
                            id = UUID.randomUUID().toString(),
                            userId = featuresService?.sessionManager?.currentUserId?.value ?: "user_panu",
                            title = cleanTitle,
                            exportFormat = "MP4",
                            previewUrl = media.mediaUrl,
                            layers = listOf(
                                CanvasLayer(
                                    id = "layer_vid_1",
                                    type = CanvasLayerType.IMAGE,
                                    content = media.mediaUrl,
                                    x = 0f,
                                    y = 0f
                                ),
                                CanvasLayer(
                                    id = "layer_txt_1",
                                    type = CanvasLayerType.TEXT,
                                    content = cleanTitle,
                                    colorHex = "#FFFFFF",
                                    fontSizeSp = 28f
                                )
                            )
                        )
                        featuresService?.saveCanvasProject(canvasProject)
                    } catch (_: Exception) {}

                    Toast.makeText(context, "Vidéo générée et ajoutée dans « Mes créations » !", Toast.LENGTH_SHORT).show()
                },
                onFailure = { err ->
                    isGeneratingVideo = false
                    generationStatusText = null
                    generationError = err.message ?: "La génération a échoué. Réessayer."
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // ➕ BOUTON NOUVEAU PROJET MULTIMÉDIA (MODERNE & COMPLET)
        // =====================================================================
        Button(
            onClick = { showNewProjectChooser = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("btn_nouveau_projet_multimedia"),
            colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("➕ Nouveau projet multimédia", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }

        // =====================================================================
        // CARTE ATTRACTIVE VIDÉO IA PANU (MINIMAX / HAILUO)
        // =====================================================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.champagne.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_panu_minimax_video_ai")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.champagne.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎬", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vidéo IA PANU",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = colors.textPrimary,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Transformez votre idée en vidéo",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.champagne,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        color = colors.champagneSubtle,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne),
                        modifier = Modifier.clickable { showCreditsStoreModal = true }
                    ) {
                        Text(
                            text = "🪙 $creditsBalance",
                            color = colors.champagne,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Écrivez simplement ce que vous voulez créer :",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.textSecondary
                )

                OutlinedTextField(
                    value = promptText,
                    onValueChange = {
                        promptText = it
                        generationError = null
                    },
                    placeholder = {
                        Text(
                            "Une publicité moderne pour une entreprise congolaise…",
                            color = colors.textSecondary.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_video_prompt_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.champagne,
                        unfocusedBorderColor = colors.surfaceBorder
                    )
                )

                OutlinedTextField(
                    value = videoTitle,
                    onValueChange = { videoTitle = it },
                    label = { Text("Titre de la création (optionnel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Sélecteur de Moteur Vidéo Technique
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Moteur vidéo :",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.champagne
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aiEngines.forEach { eng ->
                            val isSel = selectedAiEngine == eng
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedAiEngine = eng },
                                label = {
                                    Text(
                                        text = if (isSel) "● $eng" else "○ $eng",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.champagne,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                // Ratio & Durée
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Format / Ratio :",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("9:16", "16:9", "1:1").forEach { ratio ->
                                val isSel = selectedAspectRatio == ratio
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) colors.champagne else colors.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) colors.champagne else colors.surfaceBorder),
                                    modifier = Modifier.clickable { selectedAspectRatio = ratio }
                                ) {
                                    Text(
                                        text = ratio,
                                        color = if (isSel) Color.Black else colors.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Durée :",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("5\"", "10\"", "15\"", "30\"").forEach { dur ->
                                val isSel = selectedDuration == dur
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) colors.champagne else colors.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) colors.champagne else colors.surfaceBorder),
                                    modifier = Modifier.clickable { selectedDuration = dur }
                                ) {
                                    Text(
                                        text = dur,
                                        color = if (isSel) Color.Black else colors.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bouton [✨ Générer la vidéo]
                Button(
                    onClick = { executeAiVideoGeneration() },
                    enabled = !isGeneratingVideo && promptText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_generate_ai_video"),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGeneratingVideo) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Génération en cours…", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✨ Générer la vidéo", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }

                // Affichage d'état en cours
                if (isGeneratingVideo && generationStatusText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = colors.champagne)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = generationStatusText ?: "Génération en cours…",
                            fontSize = 12.sp,
                            color = colors.champagne,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Affichage d'erreur avec bouton Réessayer (zéro simulation)
                if (generationError != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF381A1A), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = generationError ?: "La génération a échoué. Réessayer.",
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { executeAiVideoGeneration() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Réessayer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // =====================================================================
        // SECTION LECTEUR MÉDIA RÉEL (VISIBLE UNIQUEMENT QUAND CONTENU EXISTE)
        // =====================================================================
        if (generatedVideoUrl != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🎬 Votre Création IA Réelle (Aperçu Direct)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )

                    PanuMediaPlayer(
                        mediaUrl = generatedVideoUrl!!,
                        thumbnailUrl = generatedThumbUrl ?: "",
                        title = if (videoTitle.isNotBlank()) videoTitle else "Création IA",
                        durationLabel = selectedDuration,
                        isVideo = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToCreations,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Voir dans « Mes créations »", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { showBoosterModal = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚀 Booster", color = colors.champagne, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Raccourci vers les Templates PANU
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
            modifier = Modifier.clickable { onNavigateToTemplates() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📋 Découvrir les Templates PANU",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Publicité, Musique, Humour, RDC, Événements prêts à générer",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.champagne)
            }
        }
    }

    MobileMoneyCreditsStoreDialog(
        isOpen = showCreditsStoreModal,
        featuresService = featuresService,
        onDismiss = { showCreditsStoreModal = false }
    )

    VisibilityBoosterDialog(
        isOpen = showBoosterModal,
        videoId = "studio_${videoTitle.hashCode()}",
        videoTitle = if (videoTitle.isNotBlank()) videoTitle else "Création IA PANU",
        featuresService = featuresService,
        onDismiss = { showBoosterModal = false }
    )
}

// =============================================================================
// 2. PAGE TEMPLATES PANU (ORGANISÉE PAR CATÉGORIES SANS FAUX COMPTEURS)
// =============================================================================
@Composable
private fun PanuTemplatesCatalogTab(
    featuresService: SupabasePanuFeaturesService?,
    onSelectTemplate: (PanuTemplate) -> Unit,
    onCreateWithAi: () -> Unit
) {
    val colors = PanuTheme.colors
    val scope = rememberCoroutineScope()

    val categories = listOf(
        "Tous",
        "🔥 Tendances",
        "📱 TikTok / Shorts / Reels",
        "🎬 Cinéma",
        "📢 Publicité",
        "🛍️ Produits & commerces",
        "🎵 Musique",
        "😂 Humour",
        "🙏 Église & événements",
        "🇨🇩 Afrique / RDC",
        "🎓 Formation",
        "💼 Entreprise"
    )

    var selectedCategory by remember { mutableStateOf("Tous") }
    var templatesList by remember { mutableStateOf<List<PanuTemplate>>(emptyList()) }
    var isLoadingTemplates by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val list = featuresService?.fetchPanuTemplates() ?: emptyList()
        templatesList = list
        isLoadingTemplates = false
    }

    val filteredTemplates = remember(templatesList, selectedCategory) {
        if (selectedCategory == "Tous") templatesList
        else templatesList.filter { it.category.contains(selectedCategory.removePrefix("🔥 ").removePrefix("📱 ").removePrefix("🎬 ").removePrefix("📢 ").removePrefix("🛍️ ").removePrefix("🎵 ").removePrefix("😂 ").removePrefix("🙏 ").removePrefix("🇨🇩 ").removePrefix("🎓 ").removePrefix("💼 "), ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // En-tête de la page Templates PANU
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Templates PANU",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = colors.textPrimary
                )
                Text(
                    text = "Structures vidéo prêtes à être alimentées par l'IA",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Button(
                onClick = onCreateWithAi,
                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Créer avec l'IA", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        // Barre horizontale des 11 catégories
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSel = selectedCategory == cat
                FilterChip(
                    selected = isSel,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.champagne,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        if (isLoadingTemplates) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.champagne)
            }
        } else if (filteredTemplates.isEmpty()) {
            // État vide propre demandé par l'utilisateur
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("📋", fontSize = 48.sp)
                    Text(
                        text = "Aucun template disponible pour le moment",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Commencez dès maintenant en générant votre vidéo directement avec l'IA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Button(
                        onClick = onCreateWithAi,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Créer avec l'IA", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Liste des vraies cartes de template
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTemplates, key = { it.id }) { template ->
                    RealPanuTemplateCard(
                        template = template,
                        onUseTemplate = { onSelectTemplate(template) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RealPanuTemplateCard(
    template: PanuTemplate,
    onUseTemplate: () -> Unit
) {
    val colors = PanuTheme.colors

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUseTemplate() }
            .testTag("template_card_${template.id}")
    ) {
        Column {
            // Bandeau supérieur visuel du Template
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.surfaceElevated,
                                Color(0xFF1F1A28)
                            )
                        )
                    )
            ) {
                if (template.previewVideoUrl.isNotBlank() && template.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = template.thumbnailUrl,
                        contentDescription = template.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Overlay sombre élégant
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Badge de Catégorie
                Surface(
                    color = colors.champagne,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = template.category,
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Format & Durée (Pas de compteur fictif !)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = template.formatLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "⏱️ ${template.durationSeconds}s",
                            color = colors.champagne,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Corps de la carte
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = template.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = template.description,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (template.scenes.isNotEmpty()) {
                    Text(
                        text = "Scènes : ${template.scenes.joinToString(" → ") { it.name }}",
                        fontSize = 11.sp,
                        color = colors.champagne.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onUseTemplate,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_use_template_${template.id}")
                ) {
                    Text("Utiliser ce template", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

// =============================================================================
// MODAL DE PERSONNALISATION RÉELLE D'UN TEMPLATE PANU
// =============================================================================
@Composable
private fun PanuTemplateCustomizerDialog(
    template: PanuTemplate,
    mediaService: CreativeMediaService,
    featuresService: SupabasePanuFeaturesService?,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Formulaire dynamique basé sur les champs éditables du template
    val fieldValues = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            template.editableFields.forEach { f ->
                add(f.key to "")
            }
        }
    }

    var selectedEngine by remember { mutableStateOf(template.recommendedEngine) }
    val engines = listOf("MiniMax/Hailuo", "Kling", "Luma", "Flux/BFL")
    var isGenerating by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isGenerating) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.champagne),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Personnaliser le template",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${template.title} (${template.formatLabel} • ${template.durationSeconds}s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.champagne,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isGenerating) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = colors.textSecondary)
                    }
                }

                HorizontalDivider(color = colors.surfaceBorder)

                Text(
                    text = "Renseignez vos informations pour générer votre vidéo avec l'IA :",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                // Champs de personnalisation (Entreprise, Produit, WhatsApp, Ville, Langue, etc.)
                template.editableFields.forEachIndexed { index, field ->
                    val currentVal = fieldValues.getOrNull(index)?.second ?: ""
                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { newVal ->
                            fieldValues[index] = field.key to newVal
                            errorMessage = null
                        },
                        label = { Text(field.label) },
                        placeholder = { Text(field.placeholder, fontSize = 11.sp) },
                        singleLine = !field.isMultiline,
                        minLines = if (field.isMultiline) 2 else 1,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Choix du moteur technique
                Text(
                    text = "Moteur de génération IA :",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.champagne
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    engines.forEach { eng ->
                        val isSel = selectedEngine == eng
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedEngine = eng },
                            label = { Text(eng, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // Bouton Déclencheur Réel
                Button(
                    onClick = {
                        isGenerating = true
                        errorMessage = null
                        statusText = "Génération en cours… (Envoi au moteur $selectedEngine)"

                        scope.launch {
                            // Construction du prompt structuré avec les données de l'utilisateur
                            val userDetailsString = fieldValues.joinToString(", ") { "${it.first}: ${it.second.ifBlank { "Standard" }}" }
                            val compiledPrompt = "${template.basePrompt} — Détails personnalisés : $userDetailsString, format ${template.aspectRatio}"

                            val result = mediaService.generateMediaWithDuration(
                                prompt = compiledPrompt,
                                title = template.title,
                                durationLabel = "${template.durationSeconds}\"",
                                stylePreset = template.stylePreset,
                                aiEngine = selectedEngine,
                                onProgress = { msg, _ -> statusText = msg }
                            )

                            result.fold(
                                onSuccess = { media ->
                                    isGenerating = false
                                    // Sauvegarde persistante dans les créations
                                    try {
                                        featuresService?.saveGeneratedAiVideoToSupabase(
                                            title = template.title,
                                            prompt = compiledPrompt,
                                            stylePreset = template.stylePreset,
                                            videoUrl = media.mediaUrl,
                                            thumbnailUrl = media.thumbnailUrl
                                        )
                                        val canvasProj = CanvasProject(
                                            id = UUID.randomUUID().toString(),
                                            userId = featuresService?.sessionManager?.currentUserId?.value ?: "user_panu",
                                            title = template.title,
                                            exportFormat = "MP4",
                                            previewUrl = media.mediaUrl
                                        )
                                        featuresService?.saveCanvasProject(canvasProj)
                                    } catch (_: Exception) {}

                                    Toast.makeText(context, "Vidéo « ${template.title} » générée et enregistrée !", Toast.LENGTH_SHORT).show()
                                    onSuccess(media.mediaUrl)
                                },
                                onFailure = { err ->
                                    isGenerating = false
                                    statusText = null
                                    errorMessage = err.message ?: "La génération a échoué. Réessayer."
                                }
                            )
                        }
                    },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Génération en cours…", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Text("✨ Générer avec l'IA PANU", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }

                if (isGenerating && statusText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = colors.champagne)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText ?: "Génération en cours…",
                            fontSize = 11.sp,
                            color = colors.champagne
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "La génération a échoué. Réessayer.",
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =============================================================================
// 3. STUDIO GRAPHIQUE PANU (TABLE `canvas_projects`)
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanuGraphicStudioTab(
    featuresService: SupabasePanuFeaturesService?
) {
    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var projectTitle by remember { mutableStateOf("Affiche Officielle PANU") }
    var backgroundHex by remember { mutableStateOf("#181920") }
    var selectedExportFormat by remember { mutableStateOf("PNG") }
    val exportFormats = listOf("PNG", "JPG", "MP4")

    val layers = remember {
        mutableStateListOf(
            CanvasLayer(
                id = "layer_shape_1",
                type = CanvasLayerType.SHAPE,
                content = "Bannière Or Royale",
                x = 24f,
                y = 28f,
                colorHex = "#E5A93C"
            ),
            CanvasLayer(
                id = "layer_text_1",
                type = CanvasLayerType.TEXT,
                content = "STUDIO CRÉATIF PANU",
                x = 32f,
                y = 95f,
                colorHex = "#FFFFFF",
                fontSizeSp = 22f
            ),
            CanvasLayer(
                id = "layer_sticker_1",
                type = CanvasLayerType.STICKER,
                content = "🔥👑🎬✨",
                x = 80f,
                y = 165f,
                fontSizeSp = 30f
            )
        )
    }

    var selectedLayerId by remember { mutableStateOf<String?>(layers.firstOrNull()?.id) }
    var customLayerText by remember { mutableStateOf("Nouveau Texte PANU") }
    var isSaving by remember { mutableStateOf(false) }

    val savedProjects = featuresService?.savedCanvasProjects?.collectAsState()?.value ?: emptyList()
    val hasNoWatermarkHd = featuresService?.hasNoWatermarkHdPass?.collectAsState()?.value ?: false
    var showExport5sAdDialog by remember { mutableStateOf(false) }

    fun triggerCanvasDownloadExport() {
        Toast.makeText(
            context,
            "📥 Téléchargement de « $projectTitle.${selectedExportFormat.lowercase()} » réussi !",
            Toast.LENGTH_LONG
        ).show()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Créé avec le Studio PANU : $projectTitle")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Partager le design"))
    }

    FiveSecondVideoAdDialog(
        isOpen = showExport5sAdDialog,
        triggerActionLabel = "Exporter le visuel",
        featuresService = featuresService,
        onAdCompleted = { triggerCanvasDownloadExport() },
        onOpenCreditsStore = { },
        onDismiss = { showExport5sAdDialog = false }
    )

    LaunchedEffect(Unit) {
        featuresService?.fetchCanvasProjects()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brush, contentDescription = null, tint = colors.champagne)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Studio Graphique & Design PANU",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Composition visuelle multi-calques vectorielle & exports haute fidélité",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = projectTitle,
                    onValueChange = { projectTitle = it },
                    label = { Text("Nom du Projet Graphique") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Sélecteur de couleur d'arrière-plan
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fond :", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    val bgColors = listOf("#181920", "#000000", "#1E1233", "#0B251F", "#3B1F0B", "#FFFFFF")
                    bgColors.forEach { hex ->
                        val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Black }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (backgroundHex == hex) 2.dp else 1.dp,
                                    color = if (backgroundHex == hex) colors.champagne else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { backgroundHex = hex }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                // Zone de Canvas Interactif
                val canvasParsedBg = try { Color(android.graphics.Color.parseColor(backgroundHex)) } catch (_: Exception) { Color.Black }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(canvasParsedBg)
                        .border(1.dp, colors.surfaceBorder, RoundedCornerShape(12.dp))
                        .testTag("interactive_canvas_preview")
                ) {
                    layers.forEach { layer ->
                        val isSelected = layer.id == selectedLayerId
                        val layerColor = try { Color(android.graphics.Color.parseColor(layer.colorHex)) } catch (_: Exception) { Color.White }
                        var offsetX by remember(layer.id) { mutableStateOf(layer.x) }
                        var offsetY by remember(layer.id) { mutableStateOf(layer.y) }

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                                .pointerInput(layer.id) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, 600f)
                                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, 500f)
                                        val idx = layers.indexOfFirst { it.id == layer.id }
                                        if (idx != -1) {
                                            layers[idx] = layers[idx].copy(x = offsetX, y = offsetY)
                                        }
                                    }
                                }
                                .clickable { selectedLayerId = layer.id }
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) colors.champagne else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        ) {
                            when (layer.type) {
                                CanvasLayerType.TEXT -> {
                                    Text(
                                        text = layer.content,
                                        color = layerColor,
                                        fontSize = layer.fontSizeSp.sp,
                                        fontWeight = if (layer.isBold) FontWeight.Black else FontWeight.Normal
                                    )
                                }
                                CanvasLayerType.STICKER -> {
                                    Text(
                                        text = layer.content,
                                        fontSize = layer.fontSizeSp.sp
                                    )
                                }
                                CanvasLayerType.SHAPE -> {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 180.dp, height = 26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(layerColor.copy(alpha = 0.85f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = layer.content,
                                            fontSize = 11.sp,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                CanvasLayerType.IMAGE -> {
                                    AsyncImage(
                                        model = layer.content,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }

                // Outils d'ajout de calques
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val newLayer = CanvasLayer(
                                id = "layer_${UUID.randomUUID().toString().take(6)}",
                                type = CanvasLayerType.TEXT,
                                content = customLayerText.ifBlank { "Nouveau Texte PANU" },
                                x = 40f,
                                y = 100f
                            )
                            layers.add(newLayer)
                            selectedLayerId = newLayer.id
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Texte", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val newLayer = CanvasLayer(
                                id = "layer_${UUID.randomUUID().toString().take(6)}",
                                type = CanvasLayerType.STICKER,
                                content = "🔥✨👑💎",
                                x = 60f,
                                y = 140f
                            )
                            layers.add(newLayer)
                            selectedLayerId = newLayer.id
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Sticker", fontSize = 11.sp)
                    }
                }

                // Actions de Sauvegarde et d'Exportation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            isSaving = true
                            scope.launch {
                                val project = CanvasProject(
                                    id = UUID.randomUUID().toString(),
                                    userId = featuresService?.sessionManager?.currentUserId?.value ?: "user_panu",
                                    title = projectTitle,
                                    backgroundHex = backgroundHex,
                                    layers = layers.toList(),
                                    exportFormat = selectedExportFormat
                                )
                                featuresService?.saveCanvasProjectToSupabase(project)
                                isSaving = false
                                Toast.makeText(context, "Projet sauvegardé dans « Mes créations » !", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sauvegarder", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (hasNoWatermarkHd) {
                                triggerCanvasDownloadExport()
                            } else {
                                showExport5sAdDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporter ($selectedExportFormat)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
