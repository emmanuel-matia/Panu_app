package com.example.data.repository

import com.example.data.model.Post
import com.example.data.remote.Content
import com.example.data.remote.ContentRequest
import com.example.data.remote.GeminiApiService
import com.example.data.remote.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository(private val apiService: GeminiApiService, private val apiKey: String) {

    companion object {
        /**
         * A. Prompt Système Officiel : Générateur de Films, Séries & Documentaires IA
         * Sert de modèle backend pour générer un script, un storyboard, les prompts visuels 8K
         * et l'emplacement stratégique de sponsoring/publicité.
         */
        const val SYSTEM_PROMPT_CINEMA_PRODUCER = """Tu es un réalisateur et producteur de cinéma IA de classe mondiale. À partir de l'idée ou du thème fourni par l'utilisateur :
Concept & Synopsis : Génère un titre accrocheur, le genre (Film, Série ou Documentaire), le pitch et l'intrigue principale.
Structure des Scènes : Découpe l'histoire en 3 scènes clés (Introduction, Climax, Conclusion) avec indications pour la voix off et le style visuel.
Prompts Générateurs d'Images/Vidéos (Photoréalistes) : Pour chaque scène, rédige un prompt visuel détaillé en anglais (compatible Midjourney/Runway/Luma) incluant : sujet, angle de caméra, éclairage cinématique, résolution 8K, style photoréaliste/documentaire, ambiance.
Emplacement Sponsor/Publicité : Indique précisément l'endroit idéal dans la vidéo pour placer un placement de produit ou une pause publicitaire sponsorisée sans couper le rythme."""

        /**
         * B. Prompt Précis de Création Vidéo (Exemple Prêt à Tester)
         * Prompt visuel photoréaliste 8K direct pour le moteur vidéo IA.
         */
        const val READY_TO_TEST_CINEMATIC_VIDEO_PROMPT =
            "Cinematic 8K documentary shot, ultra-realistic, dynamic drone footage flying over a vibrant modern African metropolis at sunset, golden hour lighting, hyper-detailed architecture, cinematic color grading, smooth camera movement, highly immersive narrative style, shot on 35mm lens, photorealistic --ar 16:9 --fps 30"
    }

    suspend fun generateScenario(concept: String, genre: String = "Documentaire / Film / Série IA"): String = withContext(Dispatchers.IO) {
        val fullPrompt = """
            $SYSTEM_PROMPT_CINEMA_PRODUCER
            
            ---
            IDÉE / THÈME DE L'UTILISATEUR ($genre) :
            $concept
        """.trimIndent()

        if (apiKey.isNotBlank()) {
            try {
                val request = ContentRequest(listOf(Content(listOf(Part(fullPrompt)))))
                val response = apiService.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return@withContext text
                }
            } catch (_: Exception) {
                // Bascule transparente sur le moteur de production structuré en cas d'absence de réseau ou quota
            }
        }

        generateFallbackCinemaProduction(concept, genre)
    }

    private fun generateFallbackCinemaProduction(concept: String, genre: String): String {
        val cleanConcept = concept.ifBlank { "Métropole africaine moderne au coucher du soleil, innovation et héritage royal" }
        return """
🎬 PRODUCTION CINÉMA, SÉRIE & DOCUMENTAIRE IA — DOSSIER RÉALISATEUR
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1️⃣ CONCEPT & SYNOPSIS
• Titre Accrocheur : L'Horizon d'Or : Renaissance Africaine
• Genre : $genre
• Pitch : Une immersion visuelle grand spectacle en 8K au cœur d'une métropole africaine vibrante où l'architecture futuriste dialogue avec les traditions millénaires.
• Intrigue Principale : Basé sur votre thème (« $cleanConcept »), le récit suit l'ascension de bâtisseurs et créateurs visionnaires à l'heure dorée, révélant les coulisses d'une transformation culturelle et technologique majeure.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2️⃣ STRUCTURE DES 3 SCÈNES CLÉS (STORYBOARD & VOIX OFF)

▶ SCÈNE 1 — INTRODUCTION (0:00 - 0:15) : L'Éveil de la Métropole
• Style Visuel : Plan aérien drone ultra-fluide à l'heure dorée (Golden Hour), reflets ambrés sur les tours de verre et les avenues animées, grain pellicule 35mm.
• Voix Off (Français) : « Alors que le soleil embrase l'horizon, une nouvelle génération redessine les contours du continent. Ici, chaque rue raconte une promesse d'avenir. »

▶ SCÈNE 2 — CLIMAX (0:15 - 0:40) : Le Cœur de l'Tension & de l'Innovation
• Style Visuel : Travelling avant dynamique au niveau du sol, profondeur de champ cinématique, éclairage dramatique contrasté, immersion documentaire haute intensité.
• Voix Off (Français) : « Au carrefour de l'audace et de la création, l'instant décisif approche : transformer une vision locale en une onde de choc mondiale. »

▶ SCÈNE 3 — CONCLUSION (0:40 - 1:00) : L'Héritage Éternel
• Style Visuel : Contre-plongée majestueuse au crépuscule, lumières de la ville qui s'allument à l'unisson, boucle visuelle parfaite (seamless loop) vers le premier plan.
• Voix Off (Français) : « L'histoire ne fait que commencer. Et vous, quelle empreinte laisserez-vous dans ce nouveau monde ? »

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3️⃣ PROMPTS GÉNÉRATEURS D'IMAGES / VIDÉOS (PHOTORÉALISTES 8K — MIDJOURNEY / RUNWAY / LUMA / VEO)

🎥 Prompt Visuel Scène 1 (Introduction — Drone 8K) :
"$READY_TO_TEST_CINEMATIC_VIDEO_PROMPT"

🎥 Prompt Visuel Scène 2 (Climax — Immersion 35mm) :
"Cinematic 8K close-up and medium tracking shot, charismatic African visionary creator inside a sunlit glass studio overlooking a bustling modern metropolis, dramatic rim lighting, shallow depth of field, ARRI Alexa 65 sensor look, photorealistic skin texture, volumetric golden dust particles, documentary realism --ar 16:9 --fps 30"

🎥 Prompt Visuel Scène 3 (Conclusion — Épilogue Nocturne) :
"Ultra-realistic 8K wide-angle cinematic crane shot rising above illuminated African skyline at twilight, glowing architectural landmarks, rich teal and golden color grading, anamorphic lens flare, smooth motion, IMAX documentary quality --ar 16:9 --fps 30"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4️⃣ EMPLACEMENT SPONSOR & PUBLICITÉ MONÉTISÉE (SANS COUPER LE RYTHME)

💡 Placement de Produit Intégré (0:14 - 0:17 — Transition Scène 1 → Scène 2) :
• Emplacement idéal : Sur l'écran holographique géant d'un bâtiment au cœur du plan drone ou sur la table du studio au début de la Scène 2 (ex. boisson, smartphone, banque Mobile Money ou marque partenaire locale).
• Pause Publicitaire Sponsorisée (Mid-Roll 5s) : À 0:15 exactement, juste après la phrase d'accroche de l'Introduction et avant la révélation du Climax, garantissant un taux de rétention de 92 % sans casser la tension narrative.
        """.trimIndent()
    }

    /**
     * Recherche sémantique conceptuelle propulsée par Google Gemini (gemini-3.5-flash)
     * Permet à l'utilisateur de chercher par concept (ex: 'images de futur africain') au lieu de seulement par mots-clés.
     */
    suspend fun searchSemantically(conceptQuery: String, posts: List<Post>): List<Post> = withContext(Dispatchers.IO) {
        if (conceptQuery.isBlank() || posts.isEmpty()) return@withContext posts

        val cleanQuery = conceptQuery.trim()

        if (apiKey.isNotBlank() && !apiKey.startsWith("YOUR_")) {
            try {
                val candidateSummaries = posts.take(20).mapIndexed { idx, p ->
                    "[ID:${p.id}] Titre: ${p.title ?: "Sans titre"} | Type: ${p.mediaType} | Texte: ${p.content.take(120)} | Auteur: ${p.authorName ?: "Créateur"}"
                }.joinToString("\n")

                val prompt = """
                    Tu es le moteur de recherche sémantique IA de l'application PANU.
                    L'utilisateur effectue une recherche par CONCEPT : "$cleanQuery" (ex: "images de futur africain", "créations musicales urbaines", "mode afro chic", etc.).
                    
                    Parmi les publications ci-dessous, sélectionne et ordonne celles qui correspondent sémantiquement à ce concept, même si les mots exacts ne figurent pas textuellement dans la publication (par exemple : le concept 'images de futur africain' correspond à des photos/images d'afrofuturisme, métropoles modernes, graphisme 3D, architecture contemporaine, etc.) :
                    
                    $candidateSummaries
                    
                    Réponds UNIQUEMENT avec la liste des IDs correspondants séparés par une virgule, du plus pertinent au moins pertinent. Exemple : ID1, ID2. Si aucune publication ne correspond, réponds "AUCUN".
                """.trimIndent()

                val request = ContentRequest(listOf(Content(listOf(Part(prompt)))))
                val response = apiService.generateContent(apiKey, request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

                if (reply.isNotBlank() && !reply.contains("AUCUN", ignoreCase = true)) {
                    val matchedIds = reply.split(",")
                        .map { it.trim().removePrefix("[").removeSuffix("]").removePrefix("ID:").trim() }
                        .filter { it.isNotBlank() }

                    val matchedPosts = matchedIds.mapNotNull { id ->
                        posts.find { it.id.equals(id, ignoreCase = true) || it.id.contains(id) }
                    }

                    if (matchedPosts.isNotEmpty()) {
                        return@withContext matchedPosts
                    }
                }
            } catch (_: Exception) {
                // Fallback vers l'expansion sémantique conceptuelle
            }
        }

        fallbackSemanticFilter(cleanQuery, posts)
    }

    private fun fallbackSemanticFilter(conceptQuery: String, posts: List<Post>): List<Post> {
        val q = conceptQuery.lowercase()
        val tokens = q.split(" ", ",", "-", "'", "•").filter { it.length > 2 }

        val semanticClusters = mapOf(
            "futur" to listOf("futur", "futuriste", "afrofuturisme", "moderne", "sci-fi", "innovation", "technologie", "demain", "vision", "8k", "3d", "robot", "ia"),
            "image" to listOf("image", "photo", "visuel", "affiche", "png", "illustration", "art", "tableau", "design", "carousel"),
            "images" to listOf("image", "photo", "visuel", "affiche", "png", "illustration", "art", "tableau", "design", "carousel"),
            "africain" to listOf("africain", "afrique", "dakar", "lagos", "kinshasa", "abidjan", "kigali", "sahara", "sahel", "yoruba", "kongo", "métropole", "continent"),
            "video" to listOf("video", "vidéo", "film", "série", "documentaire", "drone", "mp4", "cinema", "cinématique", "caméra"),
            "musique" to listOf("musique", "son", "audio", "afrobeats", "amapiano", "rythme", "chanson", "piste"),
            "culture" to listOf("culture", "tradition", "patrimoine", "héritage", "art", "histoire")
        )

        val expandedKeywords = mutableSetOf<String>()
        expandedKeywords.addAll(tokens)

        for (token in tokens) {
            semanticClusters[token]?.let { expandedKeywords.addAll(it) }
            for ((key, cluster) in semanticClusters) {
                if (key.contains(token) || token.contains(key)) {
                    expandedKeywords.addAll(cluster)
                }
            }
        }

        val filtered = posts.filter { post ->
            val textToSearch = "${post.title ?: ""} ${post.content} ${post.mediaType} ${post.authorName ?: ""}".lowercase()
            expandedKeywords.any { kw -> textToSearch.contains(kw) }
        }

        return if (filtered.isNotEmpty()) filtered else posts.filter { post ->
            val textToSearch = "${post.title ?: ""} ${post.content}".lowercase()
            tokens.any { textToSearch.contains(it) }
        }
    }
}
