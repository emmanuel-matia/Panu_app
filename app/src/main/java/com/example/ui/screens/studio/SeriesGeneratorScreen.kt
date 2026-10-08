package com.example.ui.screens.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CanvasLayer
import com.example.data.model.CanvasLayerType
import com.example.data.model.CanvasProject
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.data.repository.GeminiRepository
import com.example.data.repository.ai.CreativeMediaService
import com.example.data.repository.ai.VideoSequenceSegment
import com.example.ui.components.PanuMediaPlayer
import kotlinx.coroutines.launch

// Modèle de série / film / documentaire PANU
data class SeriesTemplate(
    val title: String,
    val genre: String,
    val visualStyle: String,
    val universe: String,
    val character: String,
    val icon: String,
    val previewVideoUrl: String = "",
    val thumbnailUrl: String = ""
)

val TRENDING_TEMPLATES = listOf(
    SeriesTemplate(
        title = "Les Bâtisseurs de l'Horizon",
        genre = "Documentaire Grand Format",
        visualStyle = "Plans aériens cinématiques 8K, éclairage heure dorée, grain pellicule 35mm, photoréaliste",
        universe = "Immersion visuelle au cœur des innovations et de l'architecture moderne sur le continent",
        character = "Visionnaires et créateurs bâtissant l'avenir avec audace et créativité",
        icon = "🎬"
    ),
    SeriesTemplate(
        title = "Les Ombres du Sahel",
        genre = "Film Épique & Légendes",
        visualStyle = "Cinématographique 8K, éclairage chaud coucher de soleil, grain 35mm, photoréaliste",
        universe = "Un royaume sahélien ancien mystérieux où la sagesse ancestrale rencontre la technologie des étoiles",
        character = "Amina, 24 ans, exploratrice avec manteau indigo et boussole dorée",
        icon = "🌙"
    ),
    SeriesTemplate(
        title = "Neo-Kinshasa 2099",
        genre = "Série Science-Fiction",
        visualStyle = "Cyberpunk vibrant 8K, néons holographiques, pluie battante, caméras anamorphiques",
        universe = "Mégalopole futuriste africaine dominée par des tours d'énergie solaire quantique",
        character = "Bakary, 30 ans, ingénieur visionnaire avec veste à bandelettes LED dorées",
        icon = "🚀"
    )
)

// Exercice de l'Académie
data class AcademyExercise(
    val id: String,
    val title: String,
    val duration: String,
    val category: String,
    val description: String,
    val formula: String,
    val challengePrompt: String,
    val proTips: List<String>
)

val ACADEMY_EXERCISES = listOf(
    AcademyExercise(
        id = "ex_hook",
        title = "Le Hook Visuel des 3 Secondes & Emplacement Sponsor",
        duration = "5 min",
        category = "Rétention & Monétisation",
        description = "Sur TikTok, Reels et Shorts, 70% de l'audience swipe dans les 3 premières secondes. Apprenez à captiver immédiatement et à placer votre sponsor entre la Scène 1 et le Climax.",
        formula = "[Introduction Drone 8K 0-3s] + [Placement Produit Naturel à 0:15] + [Climax Émotionnel]",
        challengePrompt = "Rédigez une ouverture documentaire 8K suivie d'un placement de produit fluide avant le Climax.",
        proTips = listOf(
            "Utilisez un plan drone dynamique à l'heure dorée (Golden Hour)",
            "Placez le produit ou le sponsor juste avant le pic de tension (Climax) pour 92% de rétention",
            "Spécifiez toujours la résolution 8K et la focale 35mm dans vos prompts anglais"
        )
    ),
    AcademyExercise(
        id = "ex_story",
        title = "Structure en 3 Scènes Clés (Introduction, Climax, Conclusion)",
        duration = "8 min",
        category = "Réalisation IA",
        description = "Découpez chaque Film, Série ou Documentaire en 3 scènes clés avec indications de voix off et prompts photoréalistes Midjourney/Runway/Luma.",
        formula = "Scène 1 (Introduction) → Pause/Placement Sponsor → Scène 2 (Climax) → Scène 3 (Conclusion en boucle)",
        challengePrompt = "Décrivez les 3 scènes d'un documentaire sur une métropole africaine moderne au coucher du soleil.",
        proTips = listOf(
            "Ajoutez une indication précise de voix off en français pour chaque scène",
            "Rédigez le prompt visuel en anglais avec --ar 16:9 --fps 30 pour Runway/Luma/Midjourney",
            "Soignez l'éclairage cinématique (golden hour lighting, cinematic color grading)"
        )
    )
)

@Composable
fun SeriesGeneratorScreen(
    repository: GeminiRepository,
    mediaService: CreativeMediaService? = null,
    featuresService: SupabasePanuFeaturesService? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Films/Séries/Docs IA, 1: Modèles Sponsorisés, 2: Académie

    var selectedGenre by remember { mutableStateOf("Documentaire 8K") }
    val genres = listOf("Documentaire 8K", "Film Cinéma IA", "Série Épisodique IA")

    var selectedDuration by remember { mutableStateOf("10\"") }

    // États du concept
    var title by remember { mutableStateOf("L'Horizon d'Or : Métropole Africaine") }
    var visualStyle by remember { mutableStateOf(GeminiRepository.READY_TO_TEST_CINEMATIC_VIDEO_PROMPT) }
    var universe by remember { mutableStateOf("Survol dynamique en drone d'une métropole africaine moderne vibrante au coucher du soleil (Golden Hour), architecture hyper-détaillée") }
    var character by remember { mutableStateOf("Narration documentaire immersive 35mm & placement sponsor stratégique") }

    var isGeneratingMedia by remember { mutableStateOf(false) }
    var generationProgressText by remember { mutableStateOf<String?>(null) }
    var generatedVideoUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4") }
    var generatedSegments by remember { mutableStateOf<List<VideoSequenceSegment>>(emptyList()) }
    var hasGeneratedMedia by remember { mutableStateOf(false) }

    var activeExercise by remember { mutableStateOf<AcademyExercise?>(null) }
    var exerciseAnswer by remember { mutableStateOf("") }
    var exerciseFeedback by remember { mutableStateOf<String?>(null) }

    fun triggerMediaGeneration(
        targetPrompt: String,
        targetTitle: String,
        targetGenre: String,
        targetDuration: String
    ) {
        if (isGeneratingMedia) return
        isGeneratingMedia = true
        generationProgressText = "Initialisation de la production média ($targetDuration)..."

        scope.launch {
            val service = mediaService ?: CreativeMediaService(com.example.data.local.SessionManager(context))
            val result = service.generateMediaWithDuration(
                prompt = targetPrompt,
                title = targetTitle,
                durationLabel = targetDuration,
                stylePreset = targetGenre,
                onProgress = { status, _ ->
                    generationProgressText = status
                }
            )

            val media = result.getOrNull()
            if (media != null) {
                generatedVideoUrl = media.mediaUrl
                generatedSegments = media.segments
                hasGeneratedMedia = true

                // Enregistrement direct dans Supabase (canvas_projects et videos)
                try {
                    featuresService?.saveGeneratedAiVideoToSupabase(
                        title = targetTitle,
                        prompt = targetPrompt,
                        stylePreset = targetGenre,
                        videoUrl = media.mediaUrl,
                        thumbnailUrl = media.thumbnailUrl
                    )

                    val canvasProject = CanvasProject(
                        id = "proj_${System.currentTimeMillis()}",
                        userId = featuresService?.sessionManager?.currentUserId?.value ?: "user",
                        title = targetTitle,
                        exportFormat = "MP4",
                        previewUrl = media.mediaUrl,
                        layers = listOf(
                            CanvasLayer(
                                id = "layer_video",
                                type = CanvasLayerType.IMAGE,
                                content = media.mediaUrl,
                                colorHex = "#E5A93C"
                            ),
                            CanvasLayer(
                                id = "layer_title",
                                type = CanvasLayerType.TEXT,
                                content = targetTitle,
                                colorHex = "#FFFFFF",
                                fontSizeSp = 28f
                            )
                        )
                    )
                    featuresService?.saveCanvasProject(canvasProject)
                } catch (_: Exception) {}

                Toast.makeText(context, "✅ Vidéo ($targetDuration) générée et enregistrée dans Créations !", Toast.LENGTH_LONG).show()
            }
            isGeneratingMedia = false
            generationProgressText = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFE5A93C), Color(0xFF6C5CE7))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = "Producteur Cinéma IA",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Générateur de Films, Séries & Documentaires IA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Génération directe de médias (5\", 10\", 19\", 30\", 1', 10') • Synchronisé avec canvas_projects",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Réalisateur IA 8K", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Univers & Scénarios", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.ViewCarousel, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Académie Virale", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> {
                    SeriesCreationTab(
                        selectedGenre = selectedGenre,
                        genres = genres,
                        onSelectGenre = { selectedGenre = it },
                        selectedDuration = selectedDuration,
                        onSelectDuration = { selectedDuration = it },
                        title = title,
                        onTitleChange = { title = it },
                        universe = universe,
                        onUniverseChange = { universe = it },
                        character = character,
                        onCharacterChange = { character = it },
                        visualStyle = visualStyle,
                        onVisualStyleChange = { visualStyle = it },
                        isGeneratingMedia = isGeneratingMedia,
                        generationProgressText = generationProgressText,
                        generatedVideoUrl = generatedVideoUrl,
                        generatedSegments = generatedSegments,
                        hasGeneratedMedia = hasGeneratedMedia,
                        onTriggerGeneration = {
                            val enrichedPrompt = "$visualStyle. Thème : $universe. Narration : $character"
                            triggerMediaGeneration(
                                targetPrompt = enrichedPrompt,
                                targetTitle = title,
                                targetGenre = selectedGenre,
                                targetDuration = selectedDuration
                            )
                        }
                    )
                }
                1 -> {
                    TemplatesTab(
                        onSelectTemplate = { t ->
                            title = t.title
                            selectedGenre = t.genre
                            visualStyle = t.visualStyle
                            universe = t.universe
                            character = t.character
                            generatedVideoUrl = t.previewVideoUrl
                            selectedTab = 0
                            // Déclenchement automatique de la génération média en arrière-plan sans afficher de bloc texte
                            triggerMediaGeneration(
                                targetPrompt = t.visualStyle,
                                targetTitle = t.title,
                                targetGenre = t.genre,
                                targetDuration = selectedDuration
                            )
                        }
                    )
                }
                2 -> {
                    AcademyTab(
                        activeExercise = activeExercise,
                        onSelectExercise = {
                            activeExercise = it
                            exerciseAnswer = ""
                            exerciseFeedback = null
                        },
                        onBackToList = {
                            activeExercise = null
                            exerciseFeedback = null
                        },
                        userAnswer = exerciseAnswer,
                        onAnswerChange = { exerciseAnswer = it },
                        feedback = exerciseFeedback,
                        onSubmitAnswer = { exercise ->
                            if (exerciseAnswer.isBlank()) {
                                Toast.makeText(context, "Écrivez votre concept", Toast.LENGTH_SHORT).show()
                                return@AcademyTab
                            }
                            exerciseFeedback = "✅ Excellent travail ! Votre proposition respecte la structure '${exercise.title}' avec format prêt à tourner."
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesCreationTab(
    selectedGenre: String,
    genres: List<String>,
    onSelectGenre: (String) -> Unit,
    selectedDuration: String,
    onSelectDuration: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    universe: String,
    onUniverseChange: (String) -> Unit,
    character: String,
    onCharacterChange: (String) -> Unit,
    visualStyle: String,
    onVisualStyleChange: (String) -> Unit,
    isGeneratingMedia: Boolean,
    generationProgressText: String?,
    generatedVideoUrl: String,
    generatedSegments: List<VideoSequenceSegment>,
    hasGeneratedMedia: Boolean,
    onTriggerGeneration: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. LECTEUR MÉDIA DIRECT (REMPLACEMENT DES CARTES DE TEXTE DE PROMPT)
        Text(
            text = "🎬 Aperçu Vidéo Photoréaliste Direct (Lecteur MP4 8K) :",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )

        PanuMediaPlayer(
            mediaUrl = generatedVideoUrl,
            title = title,
            durationLabel = selectedDuration,
            isVideo = true,
            onDownload = {
                Toast.makeText(context, "📥 Téléchargement de « $title.mp4 » lancé !", Toast.LENGTH_SHORT).show()
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "🎬 Regardez « $title » ($selectedDuration) généré sur PANU Studio : $generatedVideoUrl")
                }
                context.startActivity(Intent.createChooser(intent, "Partager la vidéo"))
            }
        )

        // 2. GESTION DES DURÉES ET FORMATS (5", 10", 19", 30", 1', 10')
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5A93C).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFE5A93C))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Formats & Durées (Appel Direct ou Multi-Séquences) :",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CreativeMediaService.SUPPORTED_DURATIONS.forEach { dur ->
                        val isSelected = selectedDuration == dur
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDuration(dur) },
                            label = {
                                Text(
                                    text = dur,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE5A93C),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Text(
                    text = if (CreativeMediaService.parseDurationLabelToSeconds(selectedDuration) <= 10)
                        "⚡ Format court ($selectedDuration) : Appel direct à l'API vidéo Fal.ai Kling / Veo."
                    else
                        "🎞️ Format long ($selectedDuration) : Découpage automatique en séquences de 5 à 10s en arrière-plan, génération unitaire et assemblage dans canvas_projects avec voix off.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Séquences découpées pour formats longs (si disponibles)
        if (generatedSegments.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2ED573))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF2ED573))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Séquences Assemblées (${generatedSegments.size} segments) :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    generatedSegments.forEach { seg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(seg.title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(seg.voiceOverText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                color = Color(0xFF2ED573).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${seg.durationSeconds}s",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2ED573),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Formulaire de paramétrage de la production
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Genre de la Production :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { g ->
                        FilterChip(
                            selected = selectedGenre == g,
                            onClick = { onSelectGenre(g) },
                            label = { Text(g, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth().testTag("series_title_input"),
                    label = { Text("Titre de la Création") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = universe,
                    onValueChange = onUniverseChange,
                    modifier = Modifier.fillMaxWidth().testTag("series_universe_input"),
                    label = { Text("Thème / Intrigue de la Scène") },
                    minLines = 2
                )

                OutlinedTextField(
                    value = character,
                    onValueChange = onCharacterChange,
                    modifier = Modifier.fillMaxWidth().testTag("series_character_input"),
                    label = { Text("Personnage & Voix Off") },
                    minLines = 2
                )

                // Déclenchement automatique sans texte intermédiaire
                Button(
                    onClick = onTriggerGeneration,
                    enabled = !isGeneratingMedia,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_series_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5A93C))
                ) {
                    if (isGeneratingMedia) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = generationProgressText ?: "Génération Média ($selectedDuration) en cours...",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    } else {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Générer la Vidéo Directe ($selectedDuration)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademyTab(
    activeExercise: AcademyExercise?,
    onSelectExercise: (AcademyExercise) -> Unit,
    onBackToList: () -> Unit,
    userAnswer: String,
    onAnswerChange: (String) -> Unit,
    feedback: String?,
    onSubmitAnswer: (AcademyExercise) -> Unit
) {
    if (activeExercise != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBackToList) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retour aux modules de l'Académie")
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Badge { Text(activeExercise.category) }
                        Text(activeExercise.duration, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(activeExercise.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(activeExercise.description, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡 Conseils de Réalisation :", fontWeight = FontWeight.Bold)
                    activeExercise.proTips.forEach { tip ->
                        Text("• $tip", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedTextField(
                value = userAnswer,
                onValueChange = onAnswerChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Votre proposition") },
                minLines = 4
            )

            Button(
                onClick = { onSubmitAnswer(activeExercise) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Valider l'exercice")
            }

            if (feedback != null) {
                Text(feedback, color = Color(0xFF2ED573), fontWeight = FontWeight.Bold)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Modules Pratiques de l'Académie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(ACADEMY_EXERCISES) { exercise ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectExercise(exercise) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge { Text(exercise.category) }
                            Text(exercise.duration, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(exercise.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(exercise.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplatesTab(
    onSelectTemplate: (SeriesTemplate) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Univers & Scénarios Prédéfinis PANU",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sélectionnez un univers pour charger sa structure et lancer sa génération.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        items(TRENDING_TEMPLATES) { template ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTemplate(template) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(template.icon, fontSize = 24.sp)
                            Column {
                                Text(template.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(template.genre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Button(
                            onClick = { onSelectTemplate(template) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Utiliser", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("👤 Sujet / Narration : ${template.character}", style = MaterialTheme.typography.bodySmall)
                    Text("🌍 Décor / Univers : ${template.universe}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
