package com.example.data.catalog

import com.example.data.model.PanuTemplate
import com.example.data.model.PanuTemplateField
import com.example.data.model.PanuTemplateScene

/**
 * Catalogue complet de modèles de création et montage vidéo professionnels PANU.
 * Organisé rigoureusement selon les 17 catégories requises par la plateforme.
 * Chaque modèle dispose d'une véritable structure : scènes avec durées, médias de remplacement,
 * calques de textes éditables, transitions, musiques recommandées et formats d'export réels.
 */
object PanuVideoTemplateCatalog {

    val CATEGORIES = listOf(
        "Tous",
        "Publicités commerciales",
        "Vidéos TikTok et vidéos verticales",
        "Shorts YouTube",
        "Publications vidéo Facebook",
        "Anniversaires et événements",
        "Mariages et invitations",
        "Motivation et citations animées",
        "Actualités et reportages",
        "Clips musicaux",
        "Vidéos de produits",
        "Présentation d'entreprises",
        "Beauté, coiffure et mode",
        "Agriculture et commerce",
        "Vidéos éducatives et formations",
        "Diaporamas photo",
        "Vidéos avec narration et voix IA",
        "Présentations professionnelles"
    )

    val ALL_TEMPLATES: List<PanuTemplate> = listOf(
        // 1. Publicités commerciales
        PanuTemplate(
            id = "tpl_pub_commerciale",
            title = "Spot Publicitaire Commercial & Promo",
            description = "Annonce percutante pour lancer un produit ou une promotion commerciale.",
            category = "Publicités commerciales",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 15,
            durationLabel = "15\"",
            scenes = listOf(
                PanuTemplateScene(1, "Accroche Promo & Offre Spéciale", 5, "⚡ OFFRE EXCEPTIONNELLE DU MOIS", transition = "Zoom dynamique"),
                PanuTemplateScene(2, "Présentation du Produit Vedette", 5, "Qualité Certifiée • Jusqu'à -30%", transition = "Glissement"),
                PanuTemplateScene(3, "Appel à l'action WhatsApp & Magasin", 5, "Commandez vite au +243 81 000 0000", transition = "Fondu enchaîné")
            ),
            editableFields = listOf(
                PanuTemplateField("title", "Titre de la promo", "Ex: Grande Vente Flash"),
                PanuTemplateField("discount", "Réduction / Remise", "Ex: Jusqu'à -40% de réduction"),
                PanuTemplateField("contact", "Contact WhatsApp / Ville", "Ex: +243 81 000 0000 • Kinshasa Gombe")
            ),
            musicTrack = "Afrobeats Commercial Energique",
            transitionEffect = "Zoom dynamique",
            animationEffect = "High Light Banner",
            stylePreset = "Publicité & Commercial",
            basePrompt = "Spot publicitaire dynamique et ultra-lumineux pour commerce moderne en Afrique, gros plans produits 4K, typographie percutante"
        ),

        // 2. Vidéos TikTok et vidéos verticales
        PanuTemplate(
            id = "tpl_tiktok_vertical",
            title = "TikTok / Reels Hook 3 Secondes",
            description = "Format vertical explosif calibré pour capter l'attention dès la première seconde.",
            category = "Vidéos TikTok et vidéos verticales",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 12,
            durationLabel = "12\"",
            scenes = listOf(
                PanuTemplateScene(1, "Hook visuel choc", 3, "STOP SCROLL ! Tu savais ça ?", transition = "Coupure nette"),
                PanuTemplateScene(2, "Démonstration rapide", 5, "Voici le secret que personne ne te dit !", transition = "Zoom dynamique"),
                PanuTemplateScene(3, "Appel à l'abonnement", 4, "Abonne-toi & like pour la partie 2 !", transition = "Glissement")
            ),
            editableFields = listOf(
                PanuTemplateField("hook", "Question choc d'accroche", "Ex: 3 astuces qui changent tout"),
                PanuTemplateField("solution", "Révélation rapide", "Ex: Utilise PANU Studio sur ton téléphone")
            ),
            musicTrack = "Trend Viral Beat 128BPM",
            transitionEffect = "Glissement rapide",
            stylePreset = "Cinématographique",
            basePrompt = "Format court vertical ultra-dynamique, cadrage serré expressif, typographie animée moderne, rythme soutenu"
        ),

        // 3. Shorts YouTube
        PanuTemplate(
            id = "tpl_yt_shorts",
            title = "YouTube Short Éducatif & Top 3",
            description = "Short avec décompte visuel dynamique et transitions rapides.",
            category = "Shorts YouTube",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 15,
            durationLabel = "15\"",
            scenes = listOf(
                PanuTemplateScene(1, "Introduction Top 3", 4, "TOP 3 DES MEILLEURS CONSEILS", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Point Clé #1 & #2", 6, "1. Pratique quotidienne • 2. Cohérence", transition = "Zoom dynamique"),
                PanuTemplateScene(3, "Conseil Ultime & Abonne-toi", 5, "Le N°3 fait toute la différence !", transition = "Glissement")
            ),
            editableFields = listOf(
                PanuTemplateField("subject", "Sujet du Top", "Ex: 3 erreurs à éviter absolument"),
                PanuTemplateField("counsel", "Conseil vedette", "Ex: Toujours planifier son montage avant de tourner")
            ),
            musicTrack = "Lo-Fi Focus & Rythme Moderne",
            transitionEffect = "Zoom dynamique",
            stylePreset = "Cinématographique"
        ),

        // 4. Publications vidéo Facebook
        PanuTemplate(
            id = "tpl_fb_video",
            title = "Vidéo Facebook Carrée avec Sous-Titres",
            description = "Format carré 1:1 idéal pour le fil d'actualité Facebook avec barres de texte lisibles sans son.",
            category = "Publications vidéo Facebook",
            formatLabel = "Carré (1:1)",
            aspectRatio = "1:1",
            durationSeconds = 20,
            durationLabel = "20\"",
            scenes = listOf(
                PanuTemplateScene(1, "Titre Bandeau Supérieur", 6, "À REGARDER JUSQU'À LA FIN ⬇️", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Contenu Principal & Témoignage", 8, "Une expérience qui a tout transformé", transition = "Fondu enchaîné"),
                PanuTemplateScene(3, "Partage & Commentaires", 6, "Partagez la vidéo avec vos proches !", transition = "Glissement")
            ),
            editableFields = listOf(
                PanuTemplateField("banner_title", "Titre du bandeau Facebook", "Ex: UNE HISTOIRE VRAIE QUI INSPIRE"),
                PanuTemplateField("cta_footer", "Bandeau inférieur", "Ex: Laissez votre avis en commentaire !")
            ),
            musicTrack = "Ambiance Narrative Inspirante",
            transitionEffect = "Fondu enchaîné",
            stylePreset = "Réaliste & Documentaire"
        ),

        // 5. Anniversaires et événements
        PanuTemplate(
            id = "tpl_anniversaire_event",
            title = "Joyeux Anniversaire & Célébration",
            description = "Diaporama festif avec confettis, cadres dorés et vœux chaleureux.",
            category = "Anniversaires et événements",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 15,
            durationLabel = "15\"",
            scenes = listOf(
                PanuTemplateScene(1, "Ouverture Festive & Vœux", 5, "🎉 JOYEUX ANNIVERSAIRE !", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Photos Souvenirs & Moments Clés", 5, "Que cette nouvelle année soit bénie", transition = "Zoom dynamique"),
                PanuTemplateScene(3, "Message d'Amour & Célébration", 5, "Plein de joie, santé et succès ❤️", transition = "Fondu enchaîné")
            ),
            editableFields = listOf(
                PanuTemplateField("name", "Prénom de la personne", "Ex: Merveille / Christian"),
                PanuTemplateField("wishes", "Message personnalisé", "Ex: 25 ans de grâce et d'élégance")
            ),
            musicTrack = "Célébration Festive & Joyeuse",
            transitionEffect = "Fondu enchaîné",
            stylePreset = "Cinématographique"
        ),

        // 6. Mariages et invitations
        PanuTemplate(
            id = "tpl_mariage_invitation",
            title = "Invitation de Mariage Dorée & Élégante",
            description = "Faire-part vidéo raffiné avec typographie dorée, alliance et programme.",
            category = "Mariages et invitations",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 20,
            durationLabel = "20\"",
            scenes = listOf(
                PanuTemplateScene(1, "Annonce des Époux", 6, "💍 Michel & Sarah s'unissent", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Date, Cérémonie & Lieu", 8, "Samedi 15 Novembre • 14h00", transition = "Fondu enchaîné"),
                PanuTemplateScene(3, "Soirée & R.S.V.P.", 6, "Votre présence fera notre bonheur", transition = "Zoom dynamique")
            ),
            editableFields = listOf(
                PanuTemplateField("couples", "Noms des mariés", "Ex: David & Grâce"),
                PanuTemplateField("date_location", "Date et Salle de réception", "Ex: 24 Décembre • Fleuve Congo Hotel"),
                PanuTemplateField("rsvp", "Contact R.S.V.P", "Ex: +243 82 111 2222")
            ),
            musicTrack = "Mélodie Romantique Piano & Violon",
            transitionEffect = "Fondu enchaîné cinématique",
            stylePreset = "Cinématographique"
        ),

        // 7. Motivation et citations animées
        PanuTemplate(
            id = "tpl_motivation_quote",
            title = "Motivation Quotidienne & Citation Forte",
            description = "Texte typographique animé avec fond sombre et musique percutante.",
            category = "Motivation et citations animées",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 12,
            durationLabel = "12\"",
            scenes = listOf(
                PanuTemplateScene(1, "Introduction Réflexion", 4, "La seule limite est dans ton esprit.", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Montée en puissance", 4, "N'abandonne jamais tes rêves.", transition = "Zoom dynamique"),
                PanuTemplateScene(3, "Déclic & Action", 4, "Commence aujourd'hui, pas demain.", transition = "Coupure nette")
            ),
            editableFields = listOf(
                PanuTemplateField("quote", "Citation ou phrase de motivation", "Ex: Travaille en silence, laisse le succès faire le bruit"),
                PanuTemplateField("author", "Auteur / Signature", "Ex: Sagesse Africaine / Mentor")
            ),
            musicTrack = "Motivation Épique & Bruits Sourds",
            transitionEffect = "Zoom dynamique",
            stylePreset = "Cinématographique"
        ),

        // 8. Actualités et reportages
        PanuTemplate(
            id = "tpl_actualites_news",
            title = "Flash Info & Reportage Express",
            description = "Format journal télévisé avec bandeau déroulant, titre choc et transition news.",
            category = "Actualités et reportages",
            formatLabel = "Horizontal (16:9)",
            aspectRatio = "16:9",
            durationSeconds = 25,
            durationLabel = "25\"",
            scenes = listOf(
                PanuTemplateScene(1, "Générique Flash Info", 5, "🔴 FLASH INFO PANU • KINSHASA", transition = "Glissement"),
                PanuTemplateScene(2, "Développement & Images du Terrain", 12, "Les faits marquants de la journée", transition = "Fondu enchaîné"),
                PanuTemplateScene(3, "Synthèse & Suivi de l'Information", 8, "Retrouvez l'intégralité du direct", transition = "Glissement")
            ),
            editableFields = listOf(
                PanuTemplateField("headline", "Gros titre de l'actualité", "Ex: NOUVEAUX INVESTISSEMENTS À KINSHASA"),
                PanuTemplateField("journalist", "Nom du reporter / Journaliste", "Ex: Reportage par Emmanuel Matia")
            ),
            musicTrack = "Générique Télévisé Actualité 24h",
            transitionEffect = "Glissement",
            stylePreset = "Réaliste & Documentaire"
        ),

        // 9. Clips musicaux
        PanuTemplate(
            id = "tpl_clip_musical",
            title = "Clip Musical Rythme & Performance",
            description = "Montage synchronisé sur les beats musicaux avec effets de lumières néons.",
            category = "Clips musicaux",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 15,
            durationLabel = "15\"",
            scenes = listOf(
                PanuTemplateScene(1, "Intro Beat & Cadrage Artiste", 5, "Nouveau Titre Disponible", transition = "Zoom dynamique"),
                PanuTemplateScene(2, "Refrain & Chorégraphie", 5, "Énergie maximale de scène", transition = "Glissement"),
                PanuTemplateScene(3, "Outro Plateformes Streaming", 5, "Écoutez sur YouTube & Spotify", transition = "Fondu enchaîné")
            ),
            editableFields = listOf(
                PanuTemplateField("artist", "Nom de l'artiste", "Ex: Artiste PANU"),
                PanuTemplateField("song", "Titre du morceau", "Ex: Bolingo Na Ngaï"),
                PanuTemplateField("streaming", "Plateformes d'écoute", "Ex: YouTube, Spotify, Apple Music")
            ),
            musicTrack = "Rumba Urbaine & Afrobeats 8K",
            transitionEffect = "Zoom dynamique",
            stylePreset = "Cinématographique"
        ),

        // 10. Vidéos de produits
        PanuTemplate(
            id = "tpl_produit_showcase",
            title = "Showcase Produit & Déballage 4K",
            description = "Mise en valeur esthétique d'un article : zoom sur finitions, prix et bénéfices.",
            category = "Vidéos de produits",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 15,
            durationLabel = "15\"",
            scenes = listOf(
                PanuTemplateScene(1, "Accroche Produit & Design", 5, "Découvrez la nouvelle génération", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Caractéristiques Clés & Qualité", 5, "Finitions haut de gamme & Garantie", transition = "Zoom dynamique"),
                PanuTemplateScene(3, "Prix & Livraison Express", 5, "Stock limité • Commandez maintenant", transition = "Glissement")
            ),
            editableFields = listOf(
                PanuTemplateField("product_name", "Nom du produit", "Ex: Montre Connectée Pro V3"),
                PanuTemplateField("price", "Prix en FCFA ou USD", "Ex: 45$ • Livraison gratuite à Kinshasa"),
                PanuTemplateField("order_phone", "Numéro de commande", "Ex: +243 89 000 0000")
            ),
            musicTrack = "Électro Pop Produit & Moderne",
            transitionEffect = "Zoom dynamique",
            stylePreset = "Publicité & Commercial"
        ),

        // 11. Présentation d'entreprises
        PanuTemplate(
            id = "tpl_presentation_entreprise",
            title = "Présentation Corporate & Équipe",
            description = "Film institutionnel présentant la mission, l'équipe et les réalisations d'une société.",
            category = "Présentation d'entreprises",
            formatLabel = "Horizontal (16:9)",
            aspectRatio = "16:9",
            durationSeconds = 25,
            durationLabel = "25\"",
            scenes = listOf(
                PanuTemplateScene(1, "Mission & Vision d'Entreprise", 7, "Construire l'avenir avec passion", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Nos Métiers & Savoir-faire", 10, "Une équipe d'experts dévoués", transition = "Fondu enchaîné"),
                PanuTemplateScene(3, "Confiance & Partenariats", 8, "Votre partenaire de référence en RDC", transition = "Zoom dynamique")
            ),
            editableFields = listOf(
                PanuTemplateField("company", "Nom de l'entreprise", "Ex: Congo Solutions SA"),
                PanuTemplateField("slogan", "Slogan institutionnel", "Ex: L'innovation au service de votre réussite"),
                PanuTemplateField("website", "Site web ou adresse", "Ex: www.congosolutions.cd")
            ),
            musicTrack = "Corporate Élégant & Confiance",
            transitionEffect = "Fondu enchaîné",
            stylePreset = "Cinématographique"
        ),

        // 12. Beauté, coiffure et mode
        PanuTemplate(
            id = "tpl_beaute_coiffure_mode",
            title = "Salon de Beauté, Coiffure & Mode",
            description = "Ambiance chic et glamour pour salons de coiffure, stylistes et maquilleurs.",
            category = "Beauté, coiffure et mode",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 15,
            durationLabel = "15\"",
            scenes = listOf(
                PanuTemplateScene(1, "Avant / Après & Nouveau Look", 5, "Sublimez votre beauté naturelle", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Prestations Coiffure & Soins", 5, "Coiffure tendance • Manucure • Spa", transition = "Zoom dynamique"),
                PanuTemplateScene(3, "Prise de Rendez-vous", 5, "Réservez votre séance aujourd'hui !", transition = "Glissement")
            ),
            editableFields = listOf(
                PanuTemplateField("salon_name", "Nom du salon ou styliste", "Ex: Glamour K-Style Kinshasa"),
                PanuTemplateField("services", "Prestations phares", "Ex: Braids, Tissages, Make-up de mariage"),
                PanuTemplateField("address", "Adresse & Contact", "Ex: Huileries • WhatsApp: +243 81 222 3333")
            ),
            musicTrack = "Chill R&B & Ambiance Glamour",
            transitionEffect = "Fondu enchaîné",
            stylePreset = "Publicité & Commercial"
        ),

        // 13. Agriculture et commerce
        PanuTemplate(
            id = "tpl_agriculture_commerce",
            title = "Agriculture Locale & Terroir Africain",
            description = "Valorisation des récoltes, produits frais locaux, élevage et commerce de gros.",
            category = "Agriculture et commerce",
            formatLabel = "Horizontal (16:9)",
            aspectRatio = "16:9",
            durationSeconds = 20,
            durationLabel = "20\"",
            scenes = listOf(
                PanuTemplateScene(1, "Terre Nourricière & Récolte Fraîche", 6, "Produits 100% naturels du terroir", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Qualité & Vente en Gros ou Détail", 8, "De nos champs à votre table", transition = "Fondu enchaîné"),
                PanuTemplateScene(3, "Commandes & Points de Distribution", 6, "Commandes directes auprès des producteurs", transition = "Glissement")
            ),
            editableFields = listOf(
                PanuTemplateField("farm_name", "Nom de la ferme ou coopérative", "Ex: Coopérative Agricole du Kongo Central"),
                PanuTemplateField("products", "Produits disponibles", "Ex: Manioc, Légumes frais, Poissons, Huile de palme"),
                PanuTemplateField("distribution", "Contact approvisionnement", "Ex: Marché de la Liberté • +243 85 444 5555")
            ),
            musicTrack = "Percussions Africaines & Chaleur Locale",
            transitionEffect = "Fondu enchaîné",
            stylePreset = "Réaliste & Documentaire"
        ),

        // 14. Vidéos éducatives et formations
        PanuTemplate(
            id = "tpl_education_formation",
            title = "Capsule Éducative & Formation Pro",
            description = "Présentation pédagogique d'un concept clé avec schémas, astuces et synthèse.",
            category = "Vidéos éducatives et formations",
            formatLabel = "Horizontal (16:9)",
            aspectRatio = "16:9",
            durationSeconds = 20,
            durationLabel = "20\"",
            scenes = listOf(
                PanuTemplateScene(1, "Problématique & Question Clé", 5, "Comment maîtriser ce sujet rapidement ?", transition = "Glissement"),
                PanuTemplateScene(2, "Explication Pratique & Exemple", 10, "La méthode étape par étape", transition = "Fondu enchaîné"),
                PanuTemplateScene(3, "Synthèse & Inscription au Cours", 5, "Inscrivez-vous à la formation complète !", transition = "Zoom dynamique")
            ),
            editableFields = listOf(
                PanuTemplateField("course_title", "Intitulé du cours", "Ex: Initiation au Montage Vidéo Mobile"),
                PanuTemplateField("trainer", "Nom du formateur", "Ex: Académie Créative PANU"),
                PanuTemplateField("link", "Lien d'inscription", "Ex: Formation certifiante 100% pratique")
            ),
            musicTrack = "Inspiration Douce & Apprentissage",
            transitionEffect = "Glissement",
            stylePreset = "Cinématographique"
        ),

        // 15. Diaporamas photo
        PanuTemplate(
            id = "tpl_diaporama_photo",
            title = "Diaporama Photo Souvenirs & Album",
            description = "Défilement fluide de photos de famille, voyages ou portfolio avec fondus croisés doux.",
            category = "Diaporamas photo",
            formatLabel = "Horizontal (16:9)",
            aspectRatio = "16:9",
            durationSeconds = 20,
            durationLabel = "20\"",
            scenes = listOf(
                PanuTemplateScene(1, "Photo Souvenir #1", 5, "Les plus beaux instants partagés", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Photo Souvenir #2", 5, "Des souvenirs gravés à jamais", transition = "Fondu enchaîné"),
                PanuTemplateScene(3, "Photo Souvenir #3", 5, "Une aventure inoubliable", transition = "Fondu enchaîné"),
                PanuTemplateScene(4, "Clôture de l'Album", 5, "Merci pour ces précieux moments", transition = "Fondu enchaîné")
            ),
            editableFields = listOf(
                PanuTemplateField("album_title", "Titre de l'album", "Ex: Nos Vacances à Moanda 2026"),
                PanuTemplateField("caption", "Légende de fin", "Ex: Avec tout notre amour pour la famille")
            ),
            musicTrack = "Piano Nostalgique & Cordes",
            transitionEffect = "Fondu enchaîné doux",
            stylePreset = "Cinématographique"
        ),

        // 16. Vidéos avec narration et voix IA
        PanuTemplate(
            id = "tpl_narration_voix_ia",
            title = "Vidéo Documentaire avec Voix Off IA",
            description = "Storytelling immersif avec voix narrative, séquences de contextualisation et sous-titres.",
            category = "Vidéos avec narration et voix IA",
            formatLabel = "Vertical (9:16)",
            aspectRatio = "9:16",
            durationSeconds = 18,
            durationLabel = "18\"",
            scenes = listOf(
                PanuTemplateScene(1, "Introduction de l'Histoire", 6, "Au cœur d'une cité millénaire...", transition = "Fondu enchaîné"),
                PanuTemplateScene(2, "Développement & Révélations", 6, "Les secrets transmis de génération en génération", transition = "Zoom dynamique"),
                PanuTemplateScene(3, "Conclusion & Morale", 6, "Une leçon d'héritage pour notre époque", transition = "Fondu enchaîné")
            ),
            editableFields = listOf(
                PanuTemplateField("story_title", "Titre de l'histoire", "Ex: L'Épopée du Royaume Kongo"),
                PanuTemplateField("narration_script", "Texte de la narration vocale", "Ex: Écoutez l'histoire fascinante des bâtisseurs africains", isMultiline = true)
            ),
            musicTrack = "Ambiance Cinématographique Grand Angle",
            transitionEffect = "Fondu enchaîné",
            stylePreset = "Cinématographique"
        ),

        // 17. Présentations professionnelles
        PanuTemplate(
            id = "tpl_presentation_professionnelle",
            title = "Pitch Professionnel & Rapport Annuel",
            description = "Présentation vidéo nette pour investisseurs, rapports de projets ou bilan d'activités.",
            category = "Présentations professionnelles",
            formatLabel = "Horizontal (16:9)",
            aspectRatio = "16:9",
            durationSeconds = 24,
            durationLabel = "24\"",
            scenes = listOf(
                PanuTemplateScene(1, "Rapport d'Impact & Chiffres Clés", 8, "📊 Bilan d'Activités & Croissance", transition = "Glissement"),
                PanuTemplateScene(2, "Objectifs Atteints & Projets Réalisés", 8, "+150% d'impact sur le terrain", transition = "Fondu enchaîné"),
                PanuTemplateScene(3, "Perspectives Stratégiques & Remerciements", 8, "Merci à tous nos partenaires", transition = "Zoom dynamique")
            ),
            editableFields = listOf(
                PanuTemplateField("project_title", "Titre du projet / Rapport", "Ex: RAPPORT D'IMPACT PANU 2026"),
                PanuTemplateField("key_metrics", "Chiffres marquants", "Ex: Plus de 50 000 créateurs accompagnés"),
                PanuTemplateField("conclusion", "Message de clôture", "Ex: Ensemble, accélérons l'écosystème numérique")
            ),
            musicTrack = "Corporate Tech & Dynamique",
            transitionEffect = "Glissement",
            stylePreset = "Cinématographique"
        )
    )
}
