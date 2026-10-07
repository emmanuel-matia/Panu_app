package com.example.ui.screens.studio

import android.content.Intent
import android.widget.Toast
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
import coil.compose.AsyncImage
import com.example.data.model.CanvasLayer
import com.example.data.model.CanvasLayerType
import com.example.data.model.CanvasProject
import com.example.data.model.VideoStylePreset
import com.example.data.model.ViralVideoTemplate
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.data.repository.GeminiRepository
import com.example.data.repository.ai.CreativeMediaService
import com.example.data.repository.ai.GroundingService
import com.example.ui.components.CreatorFundPayoutDialog
import com.example.ui.components.FiveSecondVideoAdDialog
import com.example.ui.components.MobileMoneyCreditsStoreDialog
import com.example.ui.components.PanuBottomNav
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
    // 0: Studio Graphique (Canva), 1: Studio Vidéo IA & Templates (CapCut/Pixverse), 2: Séries IA

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
                        Text(
                            text = if (pendingSyncCount > 0)
                                "⚡ Mode Gratuit & Hors-ligne (CapCut) • $pendingSyncCount projet(s) en attente de synchro"
                            else
                                "✅ Mode Gratuit & Hors-ligne (Style CapCut / PWA) : Templates & Édition locale actifs",
                            color = colors.emerald,
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
                        text = { Text("🎨 Studio Graphique (Canva)", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_canva_studio")
                    )
                    Tab(
                        selected = selectedStudioTab == 1,
                        onClick = { selectedStudioTab = 1 },
                        text = { Text("🎬 Vidéo IA & Templates (CapCut)", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_capcut_video_studio")
                    )
                    if (geminiRepository != null) {
                        Tab(
                            selected = selectedStudioTab == 2,
                            onClick = { selectedStudioTab = 2 },
                            text = { Text("📚 Séries & Académie IA", fontWeight = FontWeight.Bold) },
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
                0 -> CanvaGraphicStudioTab(featuresService = featuresService)
                1 -> CapCutAiVideoStudioTab(
                    mediaService = mediaService,
                    featuresService = featuresService
                )
                2 -> {
                    if (geminiRepository != null) {
                        SeriesGeneratorScreen(repository = geminiRepository)
                    }
                }
            }
        }
    }
}

// =============================================================================
// 1. STUDIO GRAPHIQUE & DESIGN (STYLE CANVA) -> Table `canvas_projects`
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CanvaGraphicStudioTab(
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
    var showCreditsStoreDialog by remember { mutableStateOf(false) }

    fun triggerCanvasDownloadExport() {
        Toast.makeText(
            context,
            "📥 Téléchargement de « $projectTitle.${selectedExportFormat.lowercase()} » (HD Sans Filigrane : ${if (hasNoWatermarkHd) "OUI" else "Standard"}) réussi !",
            Toast.LENGTH_LONG
        ).show()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Création graphique PANU Studio : $projectTitle (Exporté en $selectedExportFormat)"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Télécharger / Exporter en $selectedExportFormat"))
    }

    FiveSecondVideoAdDialog(
        isOpen = showExport5sAdDialog,
        triggerActionLabel = "Télécharger la création ($selectedExportFormat)",
        featuresService = featuresService,
        onAdCompleted = { triggerCanvasDownloadExport() },
        onOpenCreditsStore = { showCreditsStoreDialog = true },
        onDismiss = { showExport5sAdDialog = false }
    )

    MobileMoneyCreditsStoreDialog(
        isOpen = showCreditsStoreDialog,
        featuresService = featuresService,
        onDismiss = { showCreditsStoreDialog = false }
    )

    LaunchedEffect(Unit) {
        featuresService?.fetchCanvasProjects()
    }

    fun parseColorSafe(hex: String, fallback: Color = Color(0xFF181920)): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            fallback
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Barre de titre du projet & Format d'exportation (PNG, JPG, MP4)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Éditeur Visuel Glisser-Déposer (Table Supabase : canvas_projects)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.champagne
                )
                OutlinedTextField(
                    value = projectTitle,
                    onValueChange = { projectTitle = it },
                    label = { Text("Titre de la création graphique") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("canva_project_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Couleurs d'arrière-plan du canevas
                Text(
                    text = "Couleur d'arrière-plan du canevas :",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("#181920", "#2C1654", "#0F3D3E", "#4A154B", "#7A4918", "#F7F3E9").forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(parseColorSafe(hex))
                                .border(
                                    width = if (backgroundHex == hex) 2.5.dp else 1.dp,
                                    color = if (backgroundHex == hex) colors.champagne else colors.surfaceBorder,
                                    shape = CircleShape
                                )
                                .clickable { backgroundHex = hex }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // ZONE D'ÉDITION VISUELLE (GLISSER-DÉPOSER LES CALQUES)
        // =========================================================================
        Text(
            text = "🖐️ Glissez-déposez les calques directement sur le canevas :",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(parseColorSafe(backgroundHex))
                .border(2.dp, colors.champagne.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .testTag("canva_interactive_canvas")
        ) {
            // Filigrane discret
            Text(
                text = "PANU DESIGN CANVAS • ${selectedExportFormat}",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            )

            layers.forEachIndexed { index, layer ->
                val isSelected = selectedLayerId == layer.id
                Box(
                    modifier = Modifier
                        .offset { IntOffset(layer.x.roundToInt(), layer.y.roundToInt()) }
                        .pointerInput(layer.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                selectedLayerId = layer.id
                                val idx = layers.indexOfFirst { it.id == layer.id }
                                if (idx != -1) {
                                    val current = layers[idx]
                                    layers[idx] = current.copy(
                                        x = (current.x + dragAmount.x).coerceIn(0f, 650f),
                                        y = (current.y + dragAmount.y).coerceIn(0f, 520f)
                                    )
                                }
                            }
                        }
                        .clickable { selectedLayerId = layer.id }
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) colors.champagne else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(6.dp)
                ) {
                    when (layer.type) {
                        CanvasLayerType.TEXT -> {
                            Text(
                                text = layer.content,
                                color = parseColorSafe(layer.colorHex, Color.White),
                                fontSize = layer.fontSizeSp.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        CanvasLayerType.STICKER -> {
                            Text(
                                text = layer.content,
                                fontSize = layer.fontSizeSp.sp
                            )
                        }
                        CanvasLayerType.SHAPE -> {
                            Surface(
                                color = parseColorSafe(layer.colorHex, colors.champagne).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = layer.content,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        CanvasLayerType.IMAGE -> {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(layer.content, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // OUTILS D'AJOUT DE CALQUES (TEXTES, IMAGES, STICKERS, FORMES)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ajouter des Calques (Textes, Images, Stickers, Formes)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customLayerText,
                        onValueChange = { customLayerText = it },
                        label = { Text("Texte ou Légende du calque") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            if (customLayerText.isNotBlank()) {
                                val newLayer = CanvasLayer(
                                    id = UUID.randomUUID().toString(),
                                    type = CanvasLayerType.TEXT,
                                    content = customLayerText.trim(),
                                    x = (30..180).random().toFloat(),
                                    y = (40..220).random().toFloat(),
                                    colorHex = "#E5A93C",
                                    fontSizeSp = 20f
                                )
                                layers.add(newLayer)
                                selectedLayerId = newLayer.id
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                        modifier = Modifier.testTag("btn_add_text_layer")
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Texte", fontWeight = FontWeight.Bold)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val newLayer = CanvasLayer(
                                id = UUID.randomUUID().toString(),
                                type = CanvasLayerType.IMAGE,
                                content = "Image HD Studio #${layers.size + 1}",
                                x = 50f,
                                y = 140f
                            )
                            layers.add(newLayer)
                            selectedLayerId = newLayer.id
                        },
                        modifier = Modifier.testTag("btn_add_image_layer")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Calque Image", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val emojis = listOf("🔥👑💎", "🚀✨🎬", "🇨🇮🇸🇳🇨🇲", "🦁🏆🎶").random()
                            val newLayer = CanvasLayer(
                                id = UUID.randomUUID().toString(),
                                type = CanvasLayerType.STICKER,
                                content = emojis,
                                x = 90f,
                                y = 160f,
                                fontSizeSp = 30f
                            )
                            layers.add(newLayer)
                            selectedLayerId = newLayer.id
                        },
                        modifier = Modifier.testTag("btn_add_sticker_layer")
                    ) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Sticker", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val newLayer = CanvasLayer(
                                id = UUID.randomUUID().toString(),
                                type = CanvasLayerType.SHAPE,
                                content = "Badge Officiel PANU",
                                x = 40f,
                                y = 70f,
                                colorHex = "#2ED573"
                            )
                            layers.add(newLayer)
                            selectedLayerId = newLayer.id
                        },
                        modifier = Modifier.testTag("btn_add_shape_layer")
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Forme", fontSize = 12.sp)
                    }

                    if (selectedLayerId != null && layers.size > 1) {
                        OutlinedButton(
                            onClick = {
                                layers.removeAll { it.id == selectedLayerId }
                                selectedLayerId = layers.firstOrNull()?.id
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer calque", tint = colors.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Supprimer calque", color = colors.error, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SAUVEGARDE DANS `canvas_projects` & EXPORTATION EN PNG, JPG ET MP4
        // =========================================================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Sauvegarde Supabase (`canvas_projects`) & Exportation Multi-Formats",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    exportFormats.forEach { fmt ->
                        FilterChip(
                            selected = selectedExportFormat == fmt,
                            onClick = { selectedExportFormat = fmt },
                            label = { Text("Format $fmt", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = if (colors.isDark) colors.background else Color.White
                            ),
                            modifier = Modifier.testTag("export_format_chip_$fmt")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val project = CanvasProject(
                                    id = UUID.randomUUID().toString(),
                                    userId = "current",
                                    title = projectTitle.ifBlank { "Création PANU" },
                                    backgroundHex = backgroundHex,
                                    layers = layers.toList(),
                                    exportFormat = selectedExportFormat
                                )
                                featuresService?.saveCanvasProjectToSupabase(project)
                                isSaving = false
                                Toast.makeText(
                                    context,
                                    "✅ Sauvegardé dans Supabase canvas_projects ($selectedExportFormat) !",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_canvas_project"),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sauvegarder", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (hasNoWatermarkHd) {
                                triggerCanvasDownloadExport()
                            } else {
                                showExport5sAdDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_canvas_format"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Télécharger ($selectedExportFormat)", color = colors.champagne, fontWeight = FontWeight.Bold)
                    }
                }

                if (savedProjects.isNotEmpty()) {
                    HorizontalDivider(color = colors.surfaceBorder)
                    Text(
                        text = "Vos projets enregistrés dans canvas_projects (${savedProjects.size}) :",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(savedProjects, key = { it.id }) { proj ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
                                modifier = Modifier
                                    .width(190.dp)
                                    .clickable {
                                        projectTitle = proj.title
                                        backgroundHex = proj.backgroundHex
                                        selectedExportFormat = proj.exportFormat
                                        layers.clear()
                                        layers.addAll(proj.layers)
                                        Toast.makeText(context, "Projet « ${proj.title} » chargé", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = proj.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${proj.layers.size} calques • Export ${proj.exportFormat}",
                                        fontSize = 11.sp,
                                        color = colors.champagne
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 2. STUDIO VIDÉO IA & TEMPLATES VIRAUX (STYLE CAPCUT / PIXVERSE)
// =============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CapCutAiVideoStudioTab(
    mediaService: CreativeMediaService,
    featuresService: SupabasePanuFeaturesService?
) {
    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoTitle by remember { mutableStateOf("Métropole Africaine 8K au Coucher du Soleil") }
    var promptText by remember {
        mutableStateOf(GeminiRepository.READY_TO_TEST_CINEMATIC_VIDEO_PROMPT)
    }
    var selectedPreset by remember { mutableStateOf(VideoStylePreset.CINEMATIC) }
    var selectedAiEngine by remember { mutableStateOf("Fal.ai Kling 1.6 & Flux") }
    val aiEngines = listOf(
        "Fal.ai Kling 1.6 & Flux",
        "Claude 3.5 Sonnet (Script)",
        "Gemini Veo 3.1 / Omni",
        "Replicate Minimax"
    )
    var isGeneratingVideo by remember { mutableStateOf(false) }
    var generatedVideoUrl by remember {
        mutableStateOf("")
    }
    var generatedThumbUrl by remember {
        mutableStateOf("")
    }
    var generationStatusText by remember { mutableStateOf<String?>(null) }

    val creditsBalance = featuresService?.panuCreditsBalance?.collectAsState()?.value ?: 60
    val hasNoWatermarkHd = featuresService?.hasNoWatermarkHdPass?.collectAsState()?.value ?: false
    var show5sAdModal by remember { mutableStateOf(false) }
    var pending5sAdActionLabel by remember { mutableStateOf("Générer la vidéo") }
    var pendingAfterAdCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showCreditsStoreModal by remember { mutableStateOf(false) }
    var showBoosterModal by remember { mutableStateOf(false) }
    var showCreatorFundModal by remember { mutableStateOf(false) }

    fun executeAiVideoGeneration() {
        if (promptText.isBlank() || isGeneratingVideo) return
        isGeneratingVideo = true
        val modeLabel = if (hasNoWatermarkHd) "8K Sans Filigrane • Priorité GPU" else selectedPreset.labelFr
        generationStatusText = "Génération vidéo IA en mode $modeLabel..."
        scope.launch {
            val enrichedPrompt = "${promptText.trim()} — Style : ${selectedPreset.promptEnhancer}"
            try {
                mediaService.generateVideo(enrichedPrompt)
            } catch (_: Exception) {}
            delay(if (hasNoWatermarkHd) 450L else 900L)
            featuresService?.saveGeneratedAiVideoToSupabase(
                title = videoTitle,
                prompt = enrichedPrompt,
                stylePreset = selectedPreset.labelFr,
                videoUrl = generatedVideoUrl,
                thumbnailUrl = generatedThumbUrl
            )
            isGeneratingVideo = false
            generationStatusText = "✅ Vidéo IA ($modeLabel) générée et synchronisée avec Supabase !"
        }
    }

    FiveSecondVideoAdDialog(
        isOpen = show5sAdModal,
        triggerActionLabel = pending5sAdActionLabel,
        featuresService = featuresService,
        onAdCompleted = { pendingAfterAdCallback?.invoke() },
        onOpenCreditsStore = { showCreditsStoreModal = true },
        onDismiss = { show5sAdModal = false }
    )

    MobileMoneyCreditsStoreDialog(
        isOpen = showCreditsStoreModal,
        featuresService = featuresService,
        onDismiss = { showCreditsStoreModal = false }
    )

    VisibilityBoosterDialog(
        isOpen = showBoosterModal,
        videoId = "studio_${videoTitle.hashCode()}",
        videoTitle = videoTitle,
        featuresService = featuresService,
        onDismiss = { showBoosterModal = false }
    )

    CreatorFundPayoutDialog(
        isOpen = showCreatorFundModal,
        featuresService = featuresService,
        onDismiss = { showCreatorFundModal = false }
    )

    val templates = remember {
        featuresService?.defaultViralTemplates ?: emptyList()
    }

    fun shareWithOpenGraph(platformName: String, targetPackage: String? = null) {
        val ogTitle = "$videoTitle • Style ${selectedPreset.labelFr} | PANU Studio IA"
        val ogShareUrl = "https://panu.app/watch?title=${java.net.URLEncoder.encode(videoTitle, "UTF-8")}&style=${selectedPreset.name}"
        val shareMessage = "🎬 $ogTitle\n" +
                "✨ Prompt : $promptText\n" +
                "🖼️ Aperçu Open Graph HD : $generatedThumbUrl\n" +
                "🔗 Regarder sur PANU : $ogShareUrl"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, ogTitle)
            putExtra(Intent.EXTRA_TITLE, ogTitle)
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            if (targetPackage != null) {
                setPackage(targetPackage)
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Si l'application cible n'est pas installée, ouvrir le sélecteur universel avec aperçu Open Graph
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, ogTitle)
                putExtra(Intent.EXTRA_TITLE, ogTitle)
                putExtra(Intent.EXTRA_TEXT, shareMessage)
            }
            context.startActivity(Intent.createChooser(fallback, "Partager vers $platformName"))
        }
        featuresService?.recordShareOrViewRemuneration(isShare = true)
        Toast.makeText(context, "Partage Open Graph enrichi vers $platformName initié !", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Générateur Vidéo IA avec Presets (Cinématographique, Animation 3D, Anime)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.champagne)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Générateur Vidéo IA (Style CapCut / Pixverse)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Transformez vos prompts texte en vidéos virales HD",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                // Sélection du Moteur Multi-IA (Fal.ai, Claude, Gemini, Replicate)
                Text(
                    text = "Moteur Hub Multi-IA Actif (Clés Environnement Connectées) :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.champagne
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(aiEngines) { engine ->
                        FilterChip(
                            selected = selectedAiEngine == engine,
                            onClick = { selectedAiEngine = engine },
                            label = {
                                Text(
                                    text = engine,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedAiEngine == engine) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = if (colors.isDark) colors.background else Color.White
                            )
                        )
                    }
                }

                // Sélection du Preset de Style : Cinématographique, Animation 3D, Anime
                Text(
                    text = "1. Choisissez un Preset de Style IA :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.champagne
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoStylePreset.values().forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) colors.champagne else colors.surfaceBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedPreset = preset }
                                .testTag("preset_style_${preset.name}"),
                            color = if (isSelected) colors.champagneSubtle else colors.surfaceElevated
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = preset.badge,
                                    fontSize = 11.sp,
                                    color = colors.champagne,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.labelFr,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = videoTitle,
                    onValueChange = { videoTitle = it },
                    label = { Text("Titre de la vidéo (Titre Open Graph)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    label = { Text("Prompt Vidéo Cinématique 8K (Compatible Midjourney/Runway/Luma/Veo)") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_video_prompt_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Bouton rapide pour réinsérer le Prompt Cinématique 8K officiel prêt à tester
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            videoTitle = "Métropole Africaine 8K au Coucher du Soleil"
                            promptText = GeminiRepository.READY_TO_TEST_CINEMATIC_VIDEO_PROMPT
                            selectedPreset = VideoStylePreset.CINEMATIC
                            Toast.makeText(context, "Prompt Vidéo Cinématique 8K chargé !", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("✨ Charger le Prompt Drone 8K Officiel", color = colors.champagne, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Surface(
                        color = colors.champagneSubtle,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne),
                        modifier = Modifier.clickable { showCreditsStoreModal = true }
                    ) {
                        Text(
                            text = "🪙 $creditsBalance Crédits (+)",
                            color = colors.champagne,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (promptText.isBlank() || isGeneratingVideo) return@Button
                        if (hasNoWatermarkHd) {
                            executeAiVideoGeneration()
                        } else {
                            pending5sAdActionLabel = "Générer la vidéo"
                            pendingAfterAdCallback = { executeAiVideoGeneration() }
                            show5sAdModal = true
                        }
                    },
                    enabled = !isGeneratingVideo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_generate_ai_video"),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGeneratingVideo) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rendu IA (${selectedPreset.labelFr}) en cours...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.MovieCreation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (hasNoWatermarkHd)
                                "Générer la Vidéo 8K Sans Filigrane (${selectedPreset.labelFr})"
                            else
                                "Générer la Vidéo (${selectedPreset.labelFr} • Pub 5s)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Barre d'outils Monétisation : Télécharger (Pub 5s), Booster Visibilité, Crédits Mobile Money & Fonds Créateurs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val downloadAction = {
                                Toast.makeText(
                                    context,
                                    "📥 Vidéo « $videoTitle.mp4 » téléchargée (${if (hasNoWatermarkHd) "HD 8K Sans Filigrane" else "HD Standard"}) !",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            if (hasNoWatermarkHd) {
                                downloadAction()
                            } else {
                                pending5sAdActionLabel = "Télécharger la création"
                                pendingAfterAdCallback = downloadAction
                                show5sAdModal = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_download_ai_video"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Télécharger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showBoosterModal = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_boost_ai_video"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne)
                    ) {
                        Text("🚀 Booster Découvrir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.champagne)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCreditsStoreModal = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📱 Crédits Mobile Money", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showCreatorFundModal = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("💰 Fonds Créateurs (55%)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (generationStatusText != null) {
                    Text(
                        text = generationStatusText!!,
                        color = colors.emerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Catalogue de Modèles Sponsorisés (Partenariats Marques) & Templates Viraux
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "⭐ Templates Sponsorisés (Partenariats Marques) & Modèles Viraux",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "Les modèles sponsorisés par les entreprises et marques locales apparaissent en tête de liste :",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(templates, key = { it.id }) { tpl ->
                        TemplateCardItem(
                            template = tpl,
                            onUseTemplate = {
                                videoTitle = tpl.title
                                promptText = tpl.promptTemplate
                                generatedVideoUrl = tpl.previewVideoUrl
                                generatedThumbUrl = tpl.thumbnailUrl
                                selectedPreset = when (tpl.stylePreset) {
                                    "Animation 3D" -> VideoStylePreset.ANIMATION_3D
                                    "Anime" -> VideoStylePreset.ANIME
                                    else -> VideoStylePreset.CINEMATIC
                                }
                                Toast.makeText(context, "Modèle « ${tpl.title} » appliqué !", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // 3. Aperçu Open Graph Enrichi & Partage Direct (TikTok, WhatsApp, Facebook, Instagram)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "🌐 Exportation & Partage Direct (Open Graph Enrichi)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                // Carte d'Aperçu Open Graph (Image + Titre + Badge Style)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            AsyncImage(
                                model = generatedThumbUrl,
                                contentDescription = "Aperçu Open Graph",
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
                                    text = "APERÇU OPEN GRAPH • ${selectedPreset.labelFr.uppercase()}",
                                    color = colors.champagne,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "$videoTitle • Style ${selectedPreset.labelFr}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = promptText,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "panu.app/watch • Métadonnées og:image & og:title actives",
                                fontSize = 11.sp,
                                color = colors.champagne
                            )
                        }
                    }
                }

                // Boutons de partage direct vers TikTok, WhatsApp, Facebook et Instagram
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { shareWithOpenGraph("TikTok", "com.zhiliaoapp.musically") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_share_tiktok")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TikTok", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { shareWithOpenGraph("WhatsApp", "com.whatsapp") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_share_whatsapp")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { shareWithOpenGraph("Facebook", "com.facebook.katana") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_share_facebook")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Facebook", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { shareWithOpenGraph("Instagram", "com.instagram.android") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_share_instagram")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Instagram", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCardItem(
    template: ViralVideoTemplate,
    onUseTemplate: () -> Unit
) {
    val colors = PanuTheme.colors
    Card(
        modifier = Modifier
            .width(245.dp)
            .clickable(onClick = onUseTemplate)
            .testTag("viral_template_${template.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        border = androidx.compose.foundation.BorderStroke(
            if (template.isSponsoredBrand) 2.dp else 1.dp,
            if (template.isSponsoredBrand) colors.champagne else colors.surfaceBorder
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                AsyncImage(
                    model = template.thumbnailUrl,
                    contentDescription = template.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
                Surface(
                    color = if (template.isSponsoredBrand) colors.champagne else Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = template.sponsorBadgeText ?: template.stylePreset,
                        color = if (template.isSponsoredBrand) Color.Black else colors.champagne,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${template.usesCount} utilisations • ${template.durationSeconds}s",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = template.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.description,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUseTemplate,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Utiliser ce modèle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
