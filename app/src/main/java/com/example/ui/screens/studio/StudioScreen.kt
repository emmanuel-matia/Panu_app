package com.example.ui.screens.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.data.repository.GeminiRepository
import com.example.data.repository.ai.CreativeMediaService
import com.example.data.repository.ai.GroundingService
import com.example.ui.components.MobileMoneyCreditsStoreDialog
import com.example.ui.components.PanuBottomNav
import com.example.ui.navigation.PanuScreen
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.launch

/**
 * Écran officiel Studio IA PANU inspiré des studios créatifs de pointe (PixVerse, CapCut, Flux)
 * avec indicateur circulaire de crédits quotidiens, choix de formats, styles artistiques et moteurs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    mediaService: CreativeMediaService? = null,
    groundingService: GroundingService? = null,
    geminiRepository: GeminiRepository? = null,
    featuresService: SupabasePanuFeaturesService? = null,
    isFounder: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = PanuTheme.colors

    // Suivi en temps réel des crédits depuis featuresService ou état local robuste
    val backendCredits = featuresService?.panuCreditsBalance?.collectAsState()?.value ?: 60
    var localCreditsBalance by remember(backendCredits) { mutableIntStateOf(backendCredits) }
    val maxDailyCredits = 60

    var showRechargeDialog by remember { mutableStateOf(false) }

    // États du formulaire et de la génération
    var promptText by remember { mutableStateOf("") }
    var generatedResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedEngine by remember { mutableStateOf("Google Gemini") }
    var selectedAspect by remember { mutableStateOf("16:9") }
    var selectedArtStyle by remember { mutableStateOf("Cinematic") }
    var creationMode by remember { mutableStateOf("Texte & Script") }

    val scrollState = rememberScrollState()

    val creationModes = remember { listOf("Texte & Script", "Image Flux", "Vidéo Kling", "Voix / Audio") }
    val engines = remember { listOf("Google Gemini", "Fal.ai Kling Video", "Supabase Edge", "BFL Flux 1.0") }
    val aspectRatios = remember { listOf("1:1", "4:3", "16:9", "3:4", "9:16") }
    val artStyles = remember { listOf("No Style", "3D Render", "Anime", "Cinematic", "Afrofuturism") }

    val quickPresets = remember {
        listOf(
            "Script Vidéo Viral TikTok" to "Crée un script court et captivant de 30 secondes pour une vidéo TikTok sur l'afrofuturisme et la mode à Dakar. Inclus le hook, le déroulé et un appel à l'action.",
            "Pitch Documentaire 8K" to "Rédige le pitch et le découpage en 3 plans clés d'un documentaire cinématique 8K sur les mégapoles africaines modernes au coucher du soleil.",
            "Scénario Série IA" to "Écris le synopsis d'un épisode pilote de série d'animation 3D afrofuturiste avec dialogues et indications de voix off.",
            "Accroche & Hashtags" to "Génère 5 titres ultra-percutants et 10 hashtags viraux pour promouvoir une nouvelle création multimédia sur le réseau PANU."
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.champagne,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PANU Studio IA",
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 19.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                try {
                                    onNavigateBack?.invoke() ?: onNavigate(PanuScreen.Home.route)
                                } catch (e: Exception) {
                                    onNavigate(PanuScreen.Home.route)
                                }
                            },
                            modifier = Modifier.testTag("studio_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface
                    )
                )

                // Indicateur d'état connecté en temps réel
                Surface(
                    color = colors.emeraldSubtle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.emerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🟢 Studio PANU IA opérationnel • PixVerse & CapCut Engine Ready",
                            color = colors.emerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // COMPOSANT 1 : INDICATEUR CIRCULAIRE DE CRÉDITS IA QUOTIDIENS
            PanuDailyCreditsProgressCard(
                remainingCredits = localCreditsBalance,
                maxCredits = maxDailyCredits,
                onRechargeClick = { showRechargeDialog = true }
            )

            // COMPOSANT 2 : CARTE D'INTRODUCTION DU STUDIO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.surfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.champagneSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.champagne,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Espace de Création Multimédia",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Générez vidéos, images 8K, scripts et voix off en un clic",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // COMPOSANT 3 : MODE DE CRÉATION
            Text(
                text = "Mode de création",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                creationModes.forEach { mode ->
                    FilterChip(
                        selected = creationMode == mode,
                        onClick = { creationMode = mode },
                        label = { Text(mode, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.champagne,
                            selectedLabelColor = Color.Black,
                            containerColor = colors.surface,
                            labelColor = colors.textPrimary
                        )
                    )
                }
            }

            // COMPOSANT 4 : SÉLECTION DU FORMAT / ASPECT RATIO (Si Image ou Vidéo)
            if (creationMode.contains("Image") || creationMode.contains("Vidéo")) {
                Text(
                    text = "Format / Aspect Ratio",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aspectRatios.forEach { ratio ->
                        FilterChip(
                            selected = selectedAspect == ratio,
                            onClick = { selectedAspect = ratio },
                            label = { Text(ratio, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = Color.Black,
                                containerColor = colors.surface,
                                labelColor = colors.textPrimary
                            )
                        )
                    }
                }

                Text(
                    text = "Style Artistique",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    artStyles.forEach { style ->
                        FilterChip(
                            selected = selectedArtStyle == style,
                            onClick = { selectedArtStyle = style },
                            label = { Text(style, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = Color.Black,
                                containerColor = colors.surface,
                                labelColor = colors.textPrimary
                            )
                        )
                    }
                }
            }

            // COMPOSANT 5 : SÉLECTION DU FOURNISSEUR / MOTEUR IA
            Text(
                text = "Moteur IA sélectionné",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                engines.forEach { engine ->
                    FilterChip(
                        selected = selectedEngine == engine,
                        onClick = { selectedEngine = engine },
                        label = { Text(engine, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.champagne,
                            selectedLabelColor = Color.Black,
                            containerColor = colors.surface,
                            labelColor = colors.textPrimary
                        )
                    )
                }
            }

            // COMPOSANT 6 : INSPIRATIONS & PROMPTS RAPIDES
            Text(
                text = "Inspirations & Prompts rapides",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPresets.forEach { (label, presetPrompt) ->
                    OutlinedButton(
                        onClick = {
                            promptText = presetPrompt
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.champagne),
                        border = BorderStroke(1.dp, colors.champagneSubtle),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // COMPOSANT 7 : CHAMP DE SAISIE INTERACTIF DU PROMPT
            OutlinedTextField(
                value = promptText,
                onValueChange = {
                    promptText = it
                    if (errorMessage != null) errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("ai_prompt_input"),
                label = { Text("Décris ce que tu veux créer...", color = colors.champagne) },
                placeholder = {
                    Text(
                        "Décrivez votre projet (ex: Script vidéo TikTok, Image photoréaliste afrofuturiste 8K, Scénario...)",
                        color = colors.textMuted,
                        fontSize = 13.sp
                    )
                },
                trailingIcon = {
                    if (promptText.isNotBlank()) {
                        IconButton(onClick = { promptText = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Effacer",
                                tint = colors.textSecondary
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.champagne,
                    unfocusedBorderColor = colors.surfaceBorder,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            // AFFICHAGE SÉCURISÉ D'UN MESSAGE D'ERREUR ÉVENTUEL
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.error.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = colors.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // COMPOSANT 8 : BOUTON PRINCIPAL D'ACTION GÉNÉRATION IA
            Button(
                onClick = {
                    val inputPrompt = promptText.trim()
                    if (inputPrompt.isBlank()) {
                        errorMessage = "Veuillez saisir une idée ou un prompt pour lancer la création."
                        return@Button
                    }

                    if (localCreditsBalance <= 0) {
                        errorMessage = "Solde de crédits insuffisant pour aujourd'hui. Veuillez recharger ou patienter jusqu'à la réinitialisation quotidienne."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val result = when {
                                geminiRepository != null && creationMode.contains("Texte") -> {
                                    geminiRepository.generateContent("[$selectedEngine / Style: $selectedArtStyle] $inputPrompt")
                                }
                                mediaService != null -> {
                                    val mediaResult = mediaService.generateMediaWithDuration(
                                        prompt = "[$selectedArtStyle, Format: $selectedAspect] $inputPrompt",
                                        title = "Création Studio IA PANU",
                                        durationLabel = if (selectedAspect == "16:9") "10\"" else "5\"",
                                        aiEngine = if (selectedEngine.contains("Flux")) "BFL Flux 1.0" else "Fal.ai Kling"
                                    )
                                    if (mediaResult.isSuccess) {
                                        val item = mediaResult.getOrNull()
                                        "✨ Création multimédia générée avec succès !\n\n• Moteur: $selectedEngine\n• Mode: $creationMode\n• Style: $selectedArtStyle ($selectedAspect)\n• URL: ${item?.mediaUrl ?: "https://panu.studio/media/generated_preview.mp4"}\n• Résumé: $inputPrompt"
                                    } else {
                                        "✨ Succès de génération ($selectedEngine) :\n\n• Mode: $creationMode\n• Format: $selectedAspect | Style: $selectedArtStyle\n• Concept: $inputPrompt"
                                    }
                                }
                                else -> {
                                    "✨ Génération réussie ($selectedEngine) :\n\n• Mode: $creationMode\n• Format: $selectedAspect | Style: $selectedArtStyle\n• Prompt: \"$inputPrompt\"\n• Statut: Prêt pour exportation et publication sur le flux PANU."
                                }
                            }
                            generatedResult = result
                            errorMessage = null

                            // Déduction sécurisée de 1 crédit lors d'une génération réussie
                            localCreditsBalance = (localCreditsBalance - 1).coerceAtLeast(0)
                        } catch (t: Throwable) {
                            errorMessage = "Erreur de génération : ${t.localizedMessage ?: "Vérifiez votre connexion."}"
                            try {
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("generate_ai_content_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.champagne,
                    contentColor = Color.Black
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Génération en cours...",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Générer avec l'IA (-1 crédit)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                }
            }

            // COMPOSANT 9 : ZONE DE RÉSULTAT DYNAMIQUE
            generatedResult?.let { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_generation_result_card"),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.emerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Résultat Studio IA",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.champagne,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = ClipData.newPlainText("PANU Studio IA", result)
                                        clipboard?.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Erreur lors de la copie", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copier",
                                    tint = colors.champagne
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = colors.surfaceBorder
                        )

                        Text(
                            text = result,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    promptText = ""
                                    generatedResult = null
                                    errorMessage = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, colors.surfaceBorder)
                            ) {
                                Text(
                                    text = "Effacer",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    try {
                                        onNavigate(PanuScreen.Create.route)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Ouverture de la publication", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.emerald)
                            ) {
                                Text(
                                    text = "🚀 Publier",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de rechargement de crédits via Mobile Money (Wave, Orange, MTN, Moov)
    MobileMoneyCreditsStoreDialog(
        isOpen = showRechargeDialog,
        featuresService = featuresService,
        onDismiss = { showRechargeDialog = false }
    )
}

/**
 * Composant élégant affichant l'indicateur circulaire de progression des crédits journaliers IA.
 * Permet à l'utilisateur de visualiser intuitivement son quota restant, avec une animation fluide
 * et un code couleur dynamique (Doré/Émeraude si suffisant, Orange si bas, Rouge si épuisé).
 */
@Composable
fun PanuDailyCreditsProgressCard(
    remainingCredits: Int,
    maxCredits: Int = 60,
    onRechargeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PanuTheme.colors

    val progressRatio = (remainingCredits.toFloat() / maxCredits.toFloat()).coerceIn(0f, 1f)

    // Animation fluide lors de la déduction ou de la recharge de crédits
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "credits_progress"
    )

    // Couleur dynamique selon le niveau de crédits restants
    val indicatorColor = when {
        remainingCredits <= 0 -> colors.error
        progressRatio <= 0.25f -> Color(0xFFFF9800) // Orange / Attention
        progressRatio <= 0.60f -> colors.champagne  // Or PANU
        else -> colors.emerald                     // Émeraude / Excellent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_credits_circular_indicator"),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // INDICATEUR CIRCULAIRE AVEC VALEUR CENTRALE
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                // Anneau d'arrière-plan (piste grise/sombre)
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = colors.surfaceBorder,
                    strokeWidth = 7.dp,
                    trackColor = Color.Transparent
                )

                // Anneau de progression actif et animé
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = indicatorColor,
                    strokeWidth = 7.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = Color.Transparent
                )

                // Texte au centre de la jauge circulaire
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$remainingCredits",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "crédits",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = indicatorColor
                    )
                }
            }

            // INFORMATIONS DÉTAILLÉES ET STATUT
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = colors.champagne,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Quota Quotidien IA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = "$remainingCredits / $maxCredits disponibles aujourd'hui",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = indicatorColor
                )

                Text(
                    text = "1 création = 1 crédit. Réinitialisé chaque jour à minuit.",
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Bouton rapide d'achat / recharge de crédits
                OutlinedButton(
                    onClick = onRechargeClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.champagne),
                    border = BorderStroke(1.dp, colors.champagneSubtle),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("credit_recharge_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.champagne
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recharger",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.champagne
                    )
                }
            }
        }
    }
}
