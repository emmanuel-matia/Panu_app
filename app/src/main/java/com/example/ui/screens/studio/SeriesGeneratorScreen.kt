package com.example.ui.screens.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.example.data.repository.GeminiRepository
import kotlinx.coroutines.launch

// Modèle de série / film / documentaire tendance (inclut les Templates Sponsorisés en tête de liste)
data class SeriesTemplate(
    val title: String,
    val genre: String,
    val visualStyle: String,
    val universe: String,
    val character: String,
    val icon: String,
    val isSponsoredBrand: Boolean = false,
    val sponsorBadge: String? = null
)

val TRENDING_TEMPLATES = listOf(
    SeriesTemplate(
        title = "Faites apparaître votre produit dans un décor 3D",
        genre = "Sponsorisé • Partenariat Marque",
        visualStyle = "Rendu 3D Photoréaliste 8K, éclairage Golden Hour, caméra orbitale fluide 35mm",
        universe = "Décor architectural 3D de luxe africain mettant en valeur le produit d'une marque partenaire au centre du cadre",
        character = "Ambassadeur élégant présentant le produit phare avec reflets dorés et placement sponsor naturel",
        icon = "⭐",
        isSponsoredBrand = true,
        sponsorBadge = "SPONSORISÉ • EN TÊTE DE LISTE"
    ),
    SeriesTemplate(
        title = "Affiche de concert / événement sponsorisée",
        genre = "Sponsorisé • Événement & Festival",
        visualStyle = "Cinématographique 8K, drones dynamiques, pyrotechnie dorée, ambiance stade survolté",
        universe = "Grand concert ou événement culturel sponsorisé dans une métropole africaine illuminée",
        character = "Artiste vedette sur scène devant une foule en liesse avec écrans géants aux couleurs du sponsor",
        icon = "🎤",
        isSponsoredBrand = true,
        sponsorBadge = "SPONSORISÉ • PARTENARIAT MARQUE"
    ),
    SeriesTemplate(
        title = "Métropole Africaine au Coucher du Soleil (Documentaire 8K)",
        genre = "Documentaire IA 8K",
        visualStyle = GeminiRepository.READY_TO_TEST_CINEMATIC_VIDEO_PROMPT,
        universe = "Survol aérien en drone d'une métropole africaine moderne vibrante à l'heure dorée (Golden Hour)",
        character = "Narration documentaire immersive 35mm explorant l'architecture hyper-détaillée et l'innovation",
        icon = "🎬",
        isSponsoredBrand = false,
        sponsorBadge = "EXEMPLE PRÊT À TESTER"
    ),
    SeriesTemplate(
        title = "Les Ombres du Sahel",
        genre = "Film & Légendes",
        visualStyle = "Cinématographique 8K, éclairage chaud coucher de soleil, grain 35mm, photoréaliste",
        universe = "Un royaume sahélien ancien mystérieux où la sagesse ancestrale rencontre la technologie des étoiles",
        character = "Amina, 24 ans, exploratrice avec manteau indigo et boussole holographique dorée",
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
fun SeriesGeneratorScreen(repository: GeminiRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Films/Séries/Docs IA, 1: Modèles Sponsorisés, 2: Académie

    var selectedGenre by remember { mutableStateOf("Documentaire 8K") }
    val genres = listOf("Documentaire 8K", "Film Cinéma IA", "Série Épisodique IA")

    // États du concept pré-remplis avec le Prompt Précis de Création Vidéo (Exemple Prêt à Tester)
    var title by remember { mutableStateOf("L'Horizon d'Or : Métropole Africaine") }
    var visualStyle by remember { mutableStateOf(GeminiRepository.READY_TO_TEST_CINEMATIC_VIDEO_PROMPT) }
    var universe by remember { mutableStateOf("Survol dynamique en drone d'une métropole africaine moderne vibrante au coucher du soleil (Golden Hour), architecture hyper-détaillée") }
    var character by remember { mutableStateOf("Narration documentaire immersive 35mm & placement sponsor stratégique") }

    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var activeExercise by remember { mutableStateOf<AcademyExercise?>(null) }
    var exerciseAnswer by remember { mutableStateOf("") }
    var exerciseFeedback by remember { mutableStateOf<String?>(null) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
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
                            text = "Script • Storyboard 3 Scènes • Prompts 8K Midjourney/Runway/Luma • Emplacement Sponsor",
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
                        text = { Text("Modèles & Sponsors", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
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
                        title = title,
                        onTitleChange = { title = it },
                        visualStyle = visualStyle,
                        onVisualStyleChange = { visualStyle = it },
                        universe = universe,
                        onUniverseChange = { universe = it },
                        character = character,
                        onCharacterChange = { character = it },
                        isLoading = isLoading,
                        resultText = resultText,
                        onLoadReadyToTestPrompt = {
                            selectedGenre = "Documentaire 8K"
                            title = "Métropole Africaine 8K au Coucher du Soleil"
                            visualStyle = GeminiRepository.READY_TO_TEST_CINEMATIC_VIDEO_PROMPT
                            universe = "Survol dynamique en drone d'une métropole africaine moderne vibrante au coucher du soleil, éclairage Golden Hour, architecture hyper-détaillée"
                            character = "Narration documentaire immersive tournée au 35mm, 30 FPS"
                            Toast.makeText(context, "✅ Prompt Vidéo Cinématique 8K chargé !", Toast.LENGTH_SHORT).show()
                        },
                        onCopyPrompt8K = {
                            copyToClipboard("Prompt Vidéo Cinématique 8K", GeminiRepository.READY_TO_TEST_CINEMATIC_VIDEO_PROMPT)
                        },
                        onCopySystemPrompt = {
                            copyToClipboard("Prompt Système Cinéma IA", GeminiRepository.SYSTEM_PROMPT_CINEMA_PRODUCER)
                        },
                        onGenerate = {
                            if (title.isBlank() && universe.isBlank()) {
                                Toast.makeText(context, "Veuillez renseigner un thème ou une idée", Toast.LENGTH_SHORT).show()
                                return@SeriesCreationTab
                            }
                            isLoading = true
                            scope.launch {
                                val fullConcept = """
                                    Titre / Idée : $title
                                    Genre choisi : $selectedGenre
                                    Thème & Intrigue : $universe
                                    Sujet / Narration : $character
                                    Prompt Visuel 8K de référence : $visualStyle
                                """.trimIndent()
                                resultText = repository.generateScenario(fullConcept, selectedGenre)
                                isLoading = false
                            }
                        },
                        onCopy = { textToCopy ->
                            copyToClipboard("Dossier Réalisateur IA", textToCopy)
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
                            selectedTab = 0
                            Toast.makeText(context, "Modèle « ${t.title} » chargé dans le Réalisateur IA !", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "Écrivez votre prompt ou concept", Toast.LENGTH_SHORT).show()
                                return@AcademyTab
                            }
                            exerciseFeedback = "✅ Excellent travail ! Votre proposition respecte la structure '${exercise.title}' avec emplacement sponsor optimisé."
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
    title: String,
    onTitleChange: (String) -> Unit,
    visualStyle: String,
    onVisualStyleChange: (String) -> Unit,
    universe: String,
    onUniverseChange: (String) -> Unit,
    character: String,
    onCharacterChange: (String) -> Unit,
    isLoading: Boolean,
    resultText: String,
    onLoadReadyToTestPrompt: () -> Unit,
    onCopyPrompt8K: () -> Unit,
    onCopySystemPrompt: () -> Unit,
    onGenerate: () -> Unit,
    onCopy: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var showSystemPromptDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // CARTE A : Prompt Système Backend Officiel (Réalisateur & Producteur de Cinéma IA)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5A93C).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFE5A93C))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "A. Prompt Système : Réalisateur & Producteur IA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    TextButton(onClick = { showSystemPromptDetails = !showSystemPromptDetails }) {
                        Text(if (showSystemPromptDetails) "Masquer" else "Voir le Prompt")
                    }
                }
                Text(
                    text = "Génère automatiquement : 1. Concept & Synopsis • 2. Structure en 3 Scènes (Introduction, Climax, Conclusion + Voix Off) • 3. Prompts 8K Photoréalistes (Midjourney/Runway/Luma) • 4. Emplacement Sponsor/Publicité.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showSystemPromptDetails) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = GeminiRepository.SYSTEM_PROMPT_CINEMA_PRODUCER,
                                fontSize = 11.sp,
                                lineHeight = 17.sp
                            )
                            OutlinedButton(
                                onClick = onCopySystemPrompt,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier le Prompt Système Backend", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // CARTE B : Prompt Précis de Création Vidéo 8K (Exemple Prêt à Tester)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2ED573).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF2ED573))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "B. Prompt Vidéo Cinématique 8K (Prêt à Tester)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "\"${GeminiRepository.READY_TO_TEST_CINEMATIC_VIDEO_PROMPT}\"",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onLoadReadyToTestPrompt,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_load_8k_cinematic_prompt"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ED573))
                    ) {
                        Text("Appliquer ce Prompt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    OutlinedButton(
                        onClick = onCopyPrompt8K,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copier 8K", fontSize = 11.sp)
                    }
                }
            }
        }

        // Formulaire de production
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
                Text("Genre de la Production IA :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    label = { Text("Titre du Film, de la Série ou du Documentaire") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = universe,
                    onValueChange = onUniverseChange,
                    modifier = Modifier.fillMaxWidth().testTag("series_universe_input"),
                    label = { Text("Idée, Thème & Intrigue Principale") },
                    minLines = 2
                )

                OutlinedTextField(
                    value = character,
                    onValueChange = onCharacterChange,
                    modifier = Modifier.fillMaxWidth().testTag("series_character_input"),
                    label = { Text("Sujet, Personnage & Indications Voix Off") },
                    minLines = 2
                )

                OutlinedTextField(
                    value = visualStyle,
                    onValueChange = onVisualStyleChange,
                    modifier = Modifier.fillMaxWidth().testTag("series_style_input"),
                    label = { Text("Prompt Visuel 8K (Midjourney / Runway / Luma)") },
                    minLines = 3
                )

                Button(
                    onClick = onGenerate,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_series_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5A93C))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Production du Script, Storyboard & Emplacement Sponsor...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Générer Script, 3 Scènes 8K & Emplacement Sponsor", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (resultText.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5A93C))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2ED573))
                            Text("Storyboard 3 Scènes, Prompts 8K & Emplacement Sponsor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        IconButton(onClick = { onCopy(resultText) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copier")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡 Formule Gagnante", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(activeExercise.formula, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("⚡ Conseils d'Experts Viraux :", fontWeight = FontWeight.SemiBold)
                    activeExercise.proTips.forEach { tip ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = MaterialTheme.colorScheme.primary)
                            Text(tip, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎯 Exercice Pratique", fontWeight = FontWeight.Bold)
                    Text(activeExercise.challengePrompt, style = MaterialTheme.typography.bodyMedium)

                    OutlinedTextField(
                        value = userAnswer,
                        onValueChange = onAnswerChange,
                        modifier = Modifier.fillMaxWidth().testTag("exercise_answer_input"),
                        label = { Text("Votre proposition de scène / prompt") },
                        minLines = 4
                    )

                    Button(
                        onClick = { onSubmitAnswer(activeExercise) },
                        modifier = Modifier.fillMaxWidth().testTag("exercise_submit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Valider l'exercice")
                    }

                    feedback?.let { fb ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2ED573).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text(
                                text = fb,
                                modifier = Modifier.padding(12.dp),
                                color = Color(0xFF009432),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
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
                Text(
                    text = "Académie de Réalisation & Monétisation IA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Maîtrisez le découpage en 3 scènes, les prompts 8K Midjourney/Runway/Luma et l'intégration de placements sponsorisés.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
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
                text = "Templates Sponsorisés (Partenariats Marques) & Tendances",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Les marques partenaires apparaissent en tête de liste. Sélectionnez un modèle sponsorisé ou documentaire 8K en 1 clic.",
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = if (template.isSponsoredBrand)
                    androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE5A93C))
                else
                    null
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (template.sponsorBadge != null) {
                        Surface(
                            color = if (template.isSponsoredBrand) Color(0xFFE5A93C) else Color(0xFF2ED573),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = template.sponsorBadge,
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

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
                            Text("Utiliser", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("👤 Sujet / Placement : ${template.character}", style = MaterialTheme.typography.bodySmall)
                    Text("🌍 Décor / Univers : ${template.universe}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
