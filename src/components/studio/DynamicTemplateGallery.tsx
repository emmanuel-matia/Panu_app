import React, { useState, useEffect, useRef, useCallback } from 'react';
import { supabase } from '../../lib/supabaseClient';
import {
  generateWithMultiAiHub,
  publishGeneratedTemplateToPanu,
  MultiAiGenerationResult,
} from '../../services/multiAiService';
import { AIProviderId, AITaskType } from '../../config/aiKeysConfig';

export type TemplateCategoryKey =
  | 'parallel_life'
  | 'portrait_studio'
  | 'dance_party'
  | 'product_marketing'
  | 'birthday'
  | 'star_style'
  | 'trending_others'
  | 'canva_poster'
  | 'capcut_clip'
  | 'tiktok_script'
  | 'voucher_gift';

export interface StudioTemplateItem {
  id: string;
  category: TemplateCategoryKey;
  categoryLabel: string;
  title: string;
  badge: string;
  task: AITaskType;
  recommendedProvider: AIProviderId;
  recommendedModel: string;
  aiEngine: string;
  stylePreset: string;
  aspectRatio: string;
  soundDesignTrack: string;
  defaultPrompt: string;
  previewImage: string;
  explicitInstructions: string[];
}

export const PANU_DYNAMIC_TEMPLATES: StudioTemplateItem[] = [
  // 1. VIE PARALLÈLE
  {
    id: 'parallel_marionnette',
    category: 'parallel_life',
    categoryLabel: 'Vie Parallèle',
    title: 'Marionnette Étrange',
    badge: '🎭 Vie Parallèle • Strange Puppet',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur Puppet-Motion IA',
    stylePreset: 'Surréalisme & Mouvements Fluides',
    aspectRatio: '9:16',
    soundDesignTrack: 'Ambiance mystérieuse & sons boisés',
    defaultPrompt: 'Animation surréaliste d’une marionnette étrange prenant vie, mouvements fluides et cinématographiques.',
    previewImage: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Génération de mouvements organiques.', 'Effet de texture boisée réaliste.'],
  },
  {
    id: 'parallel_tracas',
    category: 'parallel_life',
    categoryLabel: 'Vie Parallèle',
    title: 'Studio Petits Tracas',
    badge: '🏚️ Studio Petits Tracas',
    task: 'viral_video',
    recommendedProvider: 'fal',
    recommendedModel: 'kling-v1.6',
    aiEngine: 'Moteur Tracas-Life IA',
    stylePreset: 'Lo-fi & Nostalgie',
    aspectRatio: '9:16',
    soundDesignTrack: 'Musique Lo-fi relaxante',
    defaultPrompt: 'Une scène de vie quotidienne avec des petits tracas transformés en art visuel.',
    previewImage: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Filtre Lo-fi chaud.', 'Animation subtile des objets.'],
  },
  {
    id: 'parallel_charme',
    category: 'parallel_life',
    categoryLabel: 'Vie Parallèle',
    title: 'Mon Petit Charme Perso',
    badge: '✨ Mon Petit Charme Perso',
    task: 'poster_image',
    recommendedProvider: 'gemini',
    recommendedModel: 'gemini-2.5-flash-image',
    aiEngine: 'Moteur Glow-Up IA',
    stylePreset: 'Éclat & Magie',
    aspectRatio: '4:5',
    soundDesignTrack: 'N/A',
    defaultPrompt: 'Portrait avec un éclat magique et des particules lumineuses.',
    previewImage: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Ajout de particules lumineuses.', 'Lissage de peau IA.'],
  },

  // 2. STUDIO DE PORTRAIT
  {
    id: 'portrait_coastal',
    category: 'portrait_studio',
    categoryLabel: 'Studio de Portrait',
    title: 'Coastal Cruisin\'',
    badge: '🌊 Coastal Cruisin\'',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur Ocean-Drive IA',
    stylePreset: 'Vacances & Été',
    aspectRatio: '9:16',
    soundDesignTrack: 'Sons de vagues et moteur de voiture classique',
    defaultPrompt: 'Portrait de croisière côtière en voiture décapotable au coucher du soleil.',
    previewImage: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Éclairage Golden Hour.', 'Vent dans les cheveux réaliste.'],
  },
  {
    id: 'portrait_rainy',
    category: 'portrait_studio',
    categoryLabel: 'Studio de Portrait',
    title: 'Rainy Day Drive',
    badge: '🌧️ Rainy Day Drive',
    task: 'viral_video',
    recommendedProvider: 'fal',
    recommendedModel: 'kling-v1.6',
    aiEngine: 'Moteur Rain-Visuals IA',
    stylePreset: 'Mélancolie & Urbain',
    aspectRatio: '9:16',
    soundDesignTrack: 'Pluie sur le pare-brise et jazz doux',
    defaultPrompt: 'Portrait à travers une vitre de voiture sous la pluie, reflets néons.',
    previewImage: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Gouttes de pluie sur l\'objectif.', 'Reflets de néons dynamiques.'],
  },
  {
    id: 'portrait_cockpit',
    category: 'portrait_studio',
    categoryLabel: 'Studio de Portrait',
    title: 'The Cockpit',
    badge: '✈️ The Cockpit',
    task: 'poster_image',
    recommendedProvider: 'gemini',
    recommendedModel: 'gemini-2.5-flash-image',
    aiEngine: 'Moteur Aviation-Glow IA',
    stylePreset: 'Technologique & Futuriste',
    aspectRatio: '4:5',
    soundDesignTrack: 'N/A',
    defaultPrompt: 'Portrait dans un cockpit d\'avion futuriste avec éclairage bleu et orange.',
    previewImage: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Interface holographique.', 'Éclairage de cockpit réaliste.'],
  },

  // 3. SOIRÉE DANSANTE
  {
    id: 'dance_mj',
    category: 'dance_party',
    categoryLabel: 'Soirée Dansante',
    title: 'Danse MJ',
    badge: '🕺 Danse MJ',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur Moonwalk-Motion IA',
    stylePreset: 'Iconique & Énergique',
    aspectRatio: '9:16',
    soundDesignTrack: 'Beat style Michael Jackson',
    defaultPrompt: 'Mouvements de danse iconiques inspirés par Michael Jackson dans un studio moderne.',
    previewImage: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Tracking des articulations.', 'Fumée de scène et projecteurs.'],
  },
  {
    id: 'dance_imitation',
    category: 'dance_party',
    categoryLabel: 'Soirée Dansante',
    title: 'Redance Dance Imitation',
    badge: '💃 Redance Dance Imitation',
    task: 'viral_video',
    recommendedProvider: 'fal',
    recommendedModel: 'kling-v1.6',
    aiEngine: 'Moteur Sync-Dance IA',
    stylePreset: 'Tendance TikTok',
    aspectRatio: '9:16',
    soundDesignTrack: 'Musique tendance virale',
    defaultPrompt: 'Imitation parfaite d\'une danse virale avec synchronisation fluide.',
    previewImage: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Synchronisation au tempo.', 'Effets de traînée lumineuse.'],
  },
  {
    id: 'dance_hot',
    category: 'dance_party',
    categoryLabel: 'Soirée Dansante',
    title: 'Hot Dance Moment',
    badge: '🔥 Hot Dance Moment',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur Heat-Dance IA',
    stylePreset: 'Vibrant & Festif',
    aspectRatio: '9:16',
    soundDesignTrack: 'Afrobeats énergique',
    defaultPrompt: 'Moment de danse intense dans une fête africaine avec lumières chaudes.',
    previewImage: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Ambiance de fête immersive.', 'Vibrations de caméra rythmées.'],
  },

  // 4. MARKETING PRODUIT
  {
    id: 'marketing_traversee',
    category: 'product_marketing',
    categoryLabel: 'Marketing Produit',
    title: 'Traversée Mondiale',
    badge: '🌍 Traversée Mondiale',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur Travel-Ad IA',
    stylePreset: 'International & Dynamique',
    aspectRatio: '9:16',
    soundDesignTrack: 'Voix-off corporate et musique entraînante',
    defaultPrompt: 'Présentation de produit voyageant à travers différents paysages mondiaux.',
    previewImage: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Transitions géographiques.', 'Lissage de produit 3D.'],
  },
  {
    id: 'marketing_post_ete',
    category: 'product_marketing',
    categoryLabel: 'Marketing Produit',
    title: 'Post-Été',
    badge: '☀️ Post-Été',
    task: 'poster_image',
    recommendedProvider: 'gemini',
    recommendedModel: 'gemini-2.5-flash-image',
    aiEngine: 'Moteur Summer-Marketing IA',
    stylePreset: 'Solaire & Rafraîchissant',
    aspectRatio: '1:1',
    soundDesignTrack: 'N/A',
    defaultPrompt: 'Affiche publicitaire pour produit estival avec effets de soleil et d\'eau.',
    previewImage: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Reflets d\'eau réalistes.', 'Typographie publicitaire moderne.'],
  },
  {
    id: 'marketing_brandverse',
    category: 'product_marketing',
    categoryLabel: 'Marketing Produit',
    title: 'Portail Brandverse',
    badge: '🌀 Portail Brandverse',
    task: 'viral_video',
    recommendedProvider: 'fal',
    recommendedModel: 'kling-v1.6',
    aiEngine: 'Moteur Portal-AI',
    stylePreset: 'Futuriste & Captivant',
    aspectRatio: '9:16',
    soundDesignTrack: 'Sons de portail spatial et basse profonde',
    defaultPrompt: 'Un portail s\'ouvrant pour révéler l\'univers d\'une marque.',
    previewImage: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Effet de portail dimensionnel.', 'Révélation de logo spectaculaire.'],
  },

  // 5. JOYEUX ANNIVERSAIRE
  {
    id: 'birthday_miroir',
    category: 'birthday',
    categoryLabel: 'Joyeux Anniversaire',
    title: 'Miroir d\'Anniversaire',
    badge: '🪞 Miroir d\'Anniversaire',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur Mirror-Glow IA',
    stylePreset: 'Élégant & Brillant',
    aspectRatio: '9:16',
    soundDesignTrack: 'Happy Birthday remix',
    defaultPrompt: 'Portrait d\'anniversaire devant un miroir avec confettis numériques.',
    previewImage: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Confettis dorés réalistes.', 'Glow autour du sujet.'],
  },
  {
    id: 'birthday_flight',
    category: 'birthday',
    categoryLabel: 'Joyeux Anniversaire',
    title: 'Wish Flight',
    badge: '🎈 Wish Flight',
    task: 'viral_video',
    recommendedProvider: 'fal',
    recommendedModel: 'kling-v1.6',
    aiEngine: 'Moteur Balloon-Rise IA',
    stylePreset: 'Rêveur & Aérien',
    aspectRatio: '9:16',
    soundDesignTrack: 'Musique orchestrale inspirante',
    defaultPrompt: 'Envol de ballons colorés emportant des vœux d\'anniversaire.',
    previewImage: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Mouvement ascendant fluide.', 'Reflets de lumière sur les ballons.'],
  },
  {
    id: 'birthday_surprise',
    category: 'birthday',
    categoryLabel: 'Joyeux Anniversaire',
    title: 'Surprise d\'Anniversaire',
    badge: '🎁 Surprise d\'Anniversaire',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur Party-Pop IA',
    stylePreset: 'Festif & Coloré',
    aspectRatio: '9:16',
    soundDesignTrack: 'Sons de fête et acclamations',
    defaultPrompt: 'Moment de surprise d\'anniversaire avec explosion de couleurs.',
    previewImage: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Explosion de couleurs vive.', 'Capture des expressions de joie.'],
  },

  // 6. STYLE DE STAR
  {
    id: 'star_tenue',
    category: 'star_style',
    categoryLabel: 'Style de Star',
    title: 'Change Ma Tenue !',
    badge: '👗 Change Ma Tenue !',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur Outfit-Swap IA',
    stylePreset: 'Mode & Transformation',
    aspectRatio: '9:16',
    soundDesignTrack: 'Beat de défilé de mode',
    defaultPrompt: 'Changement de tenue instantané et stylé en marchant vers la caméra.',
    previewImage: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Transformation de vêtements fluide.', 'Éclairage de podium.'],
  },
  {
    id: 'star_milliardaire',
    category: 'star_style',
    categoryLabel: 'Style de Star',
    title: 'La Révélation du Milliardaire Caché',
    badge: '💰 Milliardaire Caché',
    task: 'viral_video',
    recommendedProvider: 'fal',
    recommendedModel: 'kling-v1.6',
    aiEngine: 'Moteur Rich-Vibe IA',
    stylePreset: 'Luxe & Mystère',
    aspectRatio: '9:16',
    soundDesignTrack: 'Musique de suspense et de luxe',
    defaultPrompt: 'Transformation d\'un look ordinaire en un look de milliardaire élégant.',
    previewImage: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Passage au noir et blanc puis couleur.', 'Accessoires de luxe ajoutés par IA.'],
  },
  {
    id: 'star_couture',
    category: 'star_style',
    categoryLabel: 'Style de Star',
    title: 'Couture AI',
    badge: '🧵 Couture AI',
    task: 'poster_image',
    recommendedProvider: 'gemini',
    recommendedModel: 'gemini-2.5-flash-image',
    aiEngine: 'Moteur Fashion-Design IA',
    stylePreset: 'Haute Couture',
    aspectRatio: '4:5',
    soundDesignTrack: 'N/A',
    defaultPrompt: 'Conception de vêtement haute couture africaine moderne.',
    previewImage: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Textures de tissus riches (pagne, soie).', 'Modèle de mannequin IA.'],
  },

  // 7. TENDANCE & AUTRES
  {
    id: 'trending_hotel',
    category: 'trending_others',
    categoryLabel: 'Tendance & Autres',
    title: 'Hall d\'hôtel',
    badge: '🏨 Hall d\'hôtel',
    task: 'poster_image',
    recommendedProvider: 'gemini',
    recommendedModel: 'gemini-2.5-flash-image',
    aiEngine: 'Moteur Interior-Luxury IA',
    stylePreset: 'Architectural & Luxe',
    aspectRatio: '16:9',
    soundDesignTrack: 'N/A',
    defaultPrompt: 'Visualisation d\'un hall d\'hôtel de luxe moderne à Kinshasa.',
    previewImage: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Rendu réaliste des matériaux (marbre, verre).', 'Éclairage d\'ambiance tamisé.'],
  },
  {
    id: 'trending_oeufs',
    category: 'trending_others',
    categoryLabel: 'Tendance & Autres',
    title: 'Jeu de Chasse aux Œufs de Cité',
    badge: '🥚 Chasse aux Œufs',
    task: 'viral_video',
    recommendedProvider: 'fal',
    recommendedModel: 'kling-v1.6',
    aiEngine: 'Moteur Game-Play IA',
    stylePreset: 'Gamification & Fun',
    aspectRatio: '9:16',
    soundDesignTrack: 'Sons de jeu vidéo 8-bit',
    defaultPrompt: 'Une chasse aux œufs interactive dans une cité africaine colorée.',
    previewImage: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Interface de jeu vidéo superposée.', 'Animation de personnages 2D/3D.'],
  },
  {
    id: 'trending_coeur',
    category: 'trending_others',
    categoryLabel: 'Tendance & Autres',
    title: 'Arc de Cœur Brisé',
    badge: '💔 Arc de Cœur Brisé',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur Emotional-Motion IA',
    stylePreset: 'Émotionnel & Dramatique',
    aspectRatio: '9:16',
    soundDesignTrack: 'Musique triste et pluie',
    defaultPrompt: 'Expression d\'une émotion forte de cœur brisé avec effets visuels dramatiques.',
    previewImage: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Effets de particules brisées.', 'Colorimétrie froide.'],
  },
  {
    id: 'trending_heros',
    category: 'trending_others',
    categoryLabel: 'Tendance & Autres',
    title: 'Petits Héros',
    badge: '🦸 Petits Héros',
    task: 'poster_image',
    recommendedProvider: 'gemini',
    recommendedModel: 'gemini-2.5-flash-image',
    aiEngine: 'Moteur Superhero-Kid IA',
    stylePreset: 'Inspirant & Épique',
    aspectRatio: '1:1',
    soundDesignTrack: 'N/A',
    defaultPrompt: 'Enfants africains transformés en super-héros épiques.',
    previewImage: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Costumes de super-héros personnalisés.', 'Arrière-plan de ville épique.'],
  },

  // ANCIENS TEMPLATES (GARDÉS POUR COMPATIBILITÉ)
  {
    id: 'tpl_panu_afro_viral',
    category: 'capcut_clip',
    categoryLabel: 'Templates Vidéo PANU',
    title: 'Vidéo Dynamique PANU : Effet Vitesse & Transitions',
    badge: '🎬 Vidéo IA PANU',
    task: 'viral_video',
    recommendedProvider: 'gemini',
    recommendedModel: 'veo-3.1',
    aiEngine: 'Moteur IA Vidéo PANU',
    stylePreset: 'Cinematic High-Energy 4K (Kinshasa / Lagos Pulse)',
    aspectRatio: '9:16 (Format Vertical TikTok / Reels / Shorts)',
    soundDesignTrack: 'Afro-House 124 BPM avec transition basse percutante',
    defaultPrompt: 'Dynamique clip viral 9:16, découpage rapide avec zoom avant/arrière fluide sur les temps forts, transitions lumineuses dorées et colorimétrie contrastée.',
    previewImage: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=400&q=80',
    explicitInstructions: ['Application d’un effet Speed-Ramp.', 'Étalonnage colorimétrique chaud.'],
  },
];

interface DynamicTemplateGalleryProps {
  currentUserId?: string | null;
  onRequireAuth?: () => void;
  onPublishedSuccess?: (post: any) => void;
  onTemplateSelect?: (template: StudioTemplateItem) => void;
}

/**
 * MOTEUR D'EXÉCUTION DE TEMPLATES VIDÉO & INSTRUCTIONS IA
 * Chaque template contient ses consignes explicites (style, montage Studio PANU, effets).
 * L'IA lit et applique automatiquement toutes ces consignes sur les médias de l'utilisateur.
 */
export const DynamicTemplateGallery: React.FC<DynamicTemplateGalleryProps> = ({
  currentUserId,
  onRequireAuth,
  onPublishedSuccess,
  onTemplateSelect,
}) => {
  const [activeCategory, setActiveCategory] = useState<TemplateCategoryKey | 'all'>('all');
  const [selectedTemplate, setSelectedTemplate] = useState<StudioTemplateItem>(
    PANU_DYNAMIC_TEMPLATES[0]
  );
  const [customPrompt, setCustomPrompt] = useState<string>(PANU_DYNAMIC_TEMPLATES[0].defaultPrompt);
  const [selectedProvider, setSelectedProvider] = useState<AIProviderId>(
    PANU_DYNAMIC_TEMPLATES[0].recommendedProvider
  );
  const [userMediaUrl, setUserMediaUrl] = useState<string>('');

  // Identifiant utilisateur authentifié garanti
  const [localUserId, setLocalUserId] = useState<string>(currentUserId || '');

  // Canvas interactif pour le montage visuel Studio PANU
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [canvasTitle, setCanvasTitle] = useState<string>(PANU_DYNAMIC_TEMPLATES[0].title);
  const [canvasSubtitle, setCanvasSubtitle] = useState<string>(PANU_DYNAMIC_TEMPLATES[0].defaultPrompt);
  const [canvasBadge, setCanvasBadge] = useState<string>(PANU_DYNAMIC_TEMPLATES[0].badge);
  const [canvasFilter, setCanvasFilter] = useState<'gold_afro' | 'pixverse_cinema' | 'capcut_speed' | 'vintage_35mm' | 'none'>('gold_afro');
  const [uploadedUserMedia, setUploadedUserMedia] = useState<string | null>(null);
  const [isMotionPreview, setIsMotionPreview] = useState<boolean>(false);
  const [publishSuccessMessage, setPublishSuccessMessage] = useState<string | null>(null);
  const motionTick = useRef<number>(0);
  const animationFrameRef = useRef<number | null>(null);

  // Synchronisation utilisateur
  useEffect(() => {
    if (!currentUserId) {
      supabase.auth.getUser().then(({ data }) => {
        if (data?.user?.id) {
          setLocalUserId(data.user.id);
        }
      });
    } else {
      setLocalUserId(currentUserId);
    }
  }, [currentUserId]);

  // États du pipeline d'exécution IA
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionStep, setExecutionStep] = useState<number>(0);
  const [executionStepText, setExecutionStepText] = useState<string>('');
  const [executionProgress, setExecutionProgress] = useState<number>(0);
  const [isPublishing, setIsPublishing] = useState(false);
  const [result, setResult] = useState<MultiAiGenerationResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const categories: { key: TemplateCategoryKey | 'all'; label: string }[] = [
    { key: 'all', label: '✨ Tous' },
    { key: 'parallel_life', label: '🎭 Vie Parallèle' },
    { key: 'portrait_studio', label: '📸 Studio Portrait' },
    { key: 'dance_party', label: '🕺 Soirée Dansante' },
    { key: 'product_marketing', label: '🛍️ Marketing' },
    { key: 'birthday', label: '🎂 Anniversaire' },
    { key: 'star_style', label: '👗 Style de Star' },
    { key: 'trending_others', label: '🔥 Tendances' },
    { key: 'capcut_clip', label: '🎬 Vidéos PANU' },
  ];

  const filteredTemplates =
    activeCategory === 'all'
      ? PANU_DYNAMIC_TEMPLATES
      : PANU_DYNAMIC_TEMPLATES.filter((t) => t.category === activeCategory);

  const handleSelectTemplate = (tpl: StudioTemplateItem) => {
    setSelectedTemplate(tpl);
    setCustomPrompt(tpl.defaultPrompt);
    setCanvasTitle(tpl.title);
    setCanvasSubtitle(tpl.defaultPrompt);
    setCanvasBadge(tpl.badge);
    setSelectedProvider(tpl.recommendedProvider);
    setResult(null);
    setError(null);
    setPublishSuccessMessage(null);
    setExecutionProgress(0);
    setExecutionStep(0);
    onTemplateSelect?.(tpl);
  };

  // RENDU DU CANVAS WEB INTERACTIF EN TEMPS RÉEL (STUDIO PANU)
  const renderInteractiveCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Dimensions adaptatives selon le ratio du template
    const isVertical = selectedTemplate.aspectRatio.includes('9:16');
    const isSquare = selectedTemplate.aspectRatio.includes('1:1') || selectedTemplate.aspectRatio.includes('4:5');
    const width = isVertical ? 540 : isSquare ? 640 : 800;
    const height = isVertical ? 960 : isSquare ? 800 : 450;

    canvas.width = width;
    canvas.height = height;

    // 1. Fond sombre de base
    ctx.fillStyle = '#0B0C12';
    ctx.fillRect(0, 0, width, height);

    // 2. Arrière-plan média
    const imgSource = uploadedUserMedia || userMediaUrl.trim() || selectedTemplate.previewImage;
    const bgImg = new Image();
    bgImg.crossOrigin = 'anonymous';
    bgImg.src = imgSource;

    const drawContent = () => {
      ctx.save();
      // Animation de caméra (Zoom & Mouvement dynamique Studio PANU)
      if (isMotionPreview) {
        motionTick.current += 1;
        const zoom = 1 + Math.sin(motionTick.current * 0.04) * 0.06;
        const panX = Math.cos(motionTick.current * 0.03) * 15;
        ctx.translate(width / 2 + panX, height / 2);
        ctx.scale(zoom, zoom);
        ctx.translate(-width / 2, -height / 2);
      }

      try {
        // Couverture adaptée (Cover scale)
        const scale = Math.max(width / bgImg.width, height / bgImg.height);
        const nw = bgImg.width * scale;
        const nh = bgImg.height * scale;
        const nx = (width - nw) / 2;
        const ny = (height - nh) / 2;
        ctx.drawImage(bgImg, nx, ny, nw, nh);
      } catch {
        // Fallback dégradé abstrait si l'image est bloquée CORS
        const grad = ctx.createLinearGradient(0, 0, width, height);
        grad.addColorStop(0, '#1E202B');
        grad.addColorStop(0.5, '#E5A93C');
        grad.addColorStop(1, '#0B0C12');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, width, height);
      }
      ctx.restore();

      // 3. Application des filtres visuels interactifs
      if (canvasFilter === 'gold_afro') {
        const goldOverlay = ctx.createRadialGradient(width / 2, height / 2, width * 0.2, width / 2, height / 2, width);
        goldOverlay.addColorStop(0, 'rgba(229, 169, 60, 0.2)');
        goldOverlay.addColorStop(1, 'rgba(0, 0, 0, 0.7)');
        ctx.fillStyle = goldOverlay;
        ctx.fillRect(0, 0, width, height);
      } else if (canvasFilter === 'pixverse_cinema') {
        ctx.fillStyle = 'rgba(10, 20, 38, 0.35)';
        ctx.fillRect(0, 0, width, height);
        // Vignettage cinéma
        const cinGrad = ctx.createRadialGradient(width / 2, height / 2, width * 0.3, width / 2, height / 2, width * 0.8);
        cinGrad.addColorStop(0, 'rgba(0,0,0,0)');
        cinGrad.addColorStop(1, 'rgba(0,0,0,0.85)');
        ctx.fillStyle = cinGrad;
        ctx.fillRect(0, 0, width, height);
      } else if (canvasFilter === 'capcut_speed') {
        // Effet de vitesse (Speed-lines & flash)
        ctx.fillStyle = 'rgba(0, 0, 0, 0.4)';
        ctx.fillRect(0, 0, width, height);
        ctx.strokeStyle = 'rgba(229, 169, 60, 0.3)';
        ctx.lineWidth = 4;
        ctx.beginPath();
        ctx.moveTo(0, height * 0.3);
        ctx.lineTo(width, height * 0.35);
        ctx.moveTo(0, height * 0.7);
        ctx.lineTo(width, height * 0.75);
        ctx.stroke();
      } else if (canvasFilter === 'vintage_35mm') {
        ctx.fillStyle = 'rgba(120, 80, 40, 0.18)';
        ctx.fillRect(0, 0, width, height);
      }

      // 4. Calque Badge / Sticker supérieur
      if (canvasBadge) {
        ctx.fillStyle = 'rgba(0, 0, 0, 0.75)';
        ctx.strokeStyle = '#E5A93C';
        ctx.lineWidth = 2;
        const badgeWidth = Math.min(width - 40, 280);
        ctx.beginPath();
        ctx.roundRect(24, 30, badgeWidth, 38, 19);
        ctx.fill();
        ctx.stroke();

        ctx.fillStyle = '#E5A93C';
        ctx.font = 'bold 15px sans-serif';
        ctx.fillText(canvasBadge.slice(0, 32), 40, 55);
      }

      // 5. Calque Titre principal avec ombres portées
      if (canvasTitle) {
        ctx.fillStyle = '#FFF';
        ctx.font = '900 30px sans-serif';
        ctx.shadowColor = 'rgba(0, 0, 0, 0.9)';
        ctx.shadowBlur = 10;
        ctx.shadowOffsetX = 3;
        ctx.shadowOffsetY = 3;

        // Découpage du titre sur 2 lignes
        const words = canvasTitle.split(' ');
        const mid = Math.ceil(words.length / 2);
        const line1 = words.slice(0, mid).join(' ');
        const line2 = words.slice(mid).join(' ');

        const titleY = isVertical ? height * 0.65 : height * 0.6;
        ctx.fillText(line1, 24, titleY);
        if (line2) {
          ctx.fillText(line2, 24, titleY + 38);
        }
      }

      // 6. Calque Sous-titre / Hook d'accroche
      if (canvasSubtitle) {
        ctx.shadowBlur = 0;
        ctx.shadowOffsetX = 0;
        ctx.shadowOffsetY = 0;

        ctx.fillStyle = 'rgba(0, 0, 0, 0.75)';
        const subY = isVertical ? height - 120 : height - 80;
        ctx.beginPath();
        ctx.roundRect(20, subY, width - 40, 54, 12);
        ctx.fill();

        ctx.fillStyle = '#F0F0F0';
        ctx.font = '600 14px sans-serif';
        ctx.fillText(canvasSubtitle.slice(0, 65) + (canvasSubtitle.length > 65 ? '...' : ''), 34, subY + 32);
      }

      // 7. Filigrane officiel PANU STUDIO (4K)
      ctx.fillStyle = 'rgba(229, 169, 60, 0.85)';
      ctx.font = 'bold 11px sans-serif';
      ctx.fillText('⚡ PANU STUDIO IA • 4K RENDER', width - 200, height - 20);
    };

    if (bgImg.complete) {
      drawContent();
    } else {
      bgImg.onload = drawContent;
      bgImg.onerror = drawContent;
    }
  }, [selectedTemplate, canvasTitle, canvasSubtitle, canvasBadge, canvasFilter, uploadedUserMedia, userMediaUrl, isMotionPreview]);

  // Boucle d'animation fluide si prévisualisation de mouvement activée
  useEffect(() => {
    renderInteractiveCanvas();

    if (isMotionPreview) {
      const loop = () => {
        renderInteractiveCanvas();
        animationFrameRef.current = requestAnimationFrame(loop);
      };
      animationFrameRef.current = requestAnimationFrame(loop);
    }

    return () => {
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    };
  }, [renderInteractiveCanvas, isMotionPreview]);

  /**
   * EXÉCUTION AUTOMATIQUE DES CONSIGNES DU TEMPLATE PAR L'IA
   * L'IA lit les consignes explicites associées au template et les applique successivement.
   */
  const handleExecuteTemplateInstructions = async () => {
    if (isExecuting) return;
    setIsExecuting(true);
    setError(null);
    setExecutionProgress(5);
    setExecutionStep(1);
    setExecutionStepText(`1/4 : Lecture des consignes du template (${selectedTemplate.aiEngine})...`);

    try {
      // Étape 1 : Lecture et compilation des consignes du template
      await new Promise((r) => setTimeout(r, 600));
      setExecutionProgress(30);
      setExecutionStep(2);
      setExecutionStepText(
        `2/4 : Application du style "${selectedTemplate.stylePreset}" sur le média...`
      );

      // Étape 2 : Montage & pipeline d'effets visuels
      await new Promise((r) => setTimeout(r, 800));
      setExecutionProgress(65);
      setExecutionStep(3);
      setExecutionStepText(
        `3/4 : Exécution du rendu CapCut / PixVerse (${selectedTemplate.aspectRatio})...`
      );

      // Étape 3 : Synthèse avec le moteur IA sélectionné
      const fullExecutionPrompt = `[CONSIGNES IA TEMPLATE : ${selectedTemplate.title}]
Style requis : ${selectedTemplate.stylePreset}
Moteur de rendu : ${selectedTemplate.aiEngine}
Format et ratio : ${selectedTemplate.aspectRatio}
Consignes explicites exécutées :
${selectedTemplate.explicitInstructions.map((ins, i) => `${i + 1}. ${ins}`).join('\n')}
Média source utilisateur : ${userMediaUrl.trim() || 'Médias officiels PANU Studio'}
Prompt créatif utilisateur : ${customPrompt.trim()}`;

      const res = await generateWithMultiAiHub({
        task: selectedTemplate.task,
        prompt: fullExecutionPrompt,
        templateCategory: selectedTemplate.category,
        preferredProvider: selectedProvider,
        modelOverride: selectedTemplate.recommendedModel,
      });

      setExecutionProgress(100);
      setExecutionStep(4);
      setExecutionStepText('4/4 : Rendu final terminé avec succès ! Prêt pour publication.');
      setResult(res);
    } catch (err: any) {
      setError(err?.message || 'Erreur lors de l’exécution des consignes du template IA.');
    } finally {
      setIsExecuting(false);
    }
  };

  const handlePublishToPanu = async () => {
    const activeUid = localUserId || currentUserId;
    if (!activeUid) {
      onRequireAuth?.();
      return;
    }

    const canvas = canvasRef.current;
    setIsPublishing(true);
    setError(null);
    setPublishSuccessMessage(null);

    const publishWithBlob = async (blob?: Blob) => {
      try {
        const published = await publishGeneratedTemplateToPanu({
          userId: activeUid,
          title: canvasTitle || selectedTemplate.title,
          content: canvasSubtitle || customPrompt,
          mediaUrl: result?.mediaUrl || selectedTemplate.previewImage,
          blob,
          category: selectedTemplate.category,
        });

        setPublishSuccessMessage(
          `🎉 Votre création interactive "${canvasTitle || selectedTemplate.title}" a été publiée avec succès sur PANU !`
        );
        onPublishedSuccess?.(published);
      } catch (err: any) {
        setError(err?.message || 'Erreur lors de la publication sur PANU');
      } finally {
        setIsPublishing(false);
      }
    };

    if (canvas) {
      canvas.toBlob((blob) => {
        publishWithBlob(blob || undefined);
      }, 'image/png');
    } else {
      publishWithBlob();
    }
  };

  return (
    <div style={{ backgroundColor: '#121318', color: '#FFF', padding: 20, borderRadius: 16 }}>
      {/* EN-TÊTE DU MOTEUR DE TEMPLATES */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10, marginBottom: 16 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 24 }}>🎬</span>
            <h2 style={{ margin: 0, color: '#E5A93C', fontSize: 19 }}>
              Moteur de Templates & Instructions IA
            </h2>
          </div>
          <p style={{ margin: '4px 0 0', fontSize: 12, color: '#AAA' }}>
            Sélectionnez un modèle : l'IA lit et applique automatiquement les consignes de style, montage CapCut / PixVerse et effets.
          </p>
        </div>

        <div style={{ backgroundColor: '#1E202B', border: '1px solid rgba(229,169,60,0.3)', padding: '4px 10px', borderRadius: 999, fontSize: 11, color: '#E5A93C', fontWeight: 800 }}>
          ⚡ CapCut • PixVerse • Gemini • Claude
        </div>
      </div>

      {/* FILTRES PAR CATÉGORIES */}
      <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 12, marginBottom: 12 }}>
        {categories.map((cat) => (
          <button
            key={cat.key}
            type="button"
            onClick={() => setActiveCategory(cat.key)}
            style={{
              backgroundColor: activeCategory === cat.key ? '#E5A93C' : '#1E2029',
              color: activeCategory === cat.key ? '#000' : '#FFF',
              border: '1px solid rgba(229, 169, 60, 0.4)',
              borderRadius: 999,
              padding: '7px 14px',
              fontWeight: 700,
              fontSize: 12,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            {cat.label}
          </button>
        ))}
      </div>

      {/* GRILLE DES TEMPLATES */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
          gap: 12,
          marginBottom: 20,
        }}
      >
        {filteredTemplates.map((tpl) => {
          const isSelected = selectedTemplate.id === tpl.id;
          return (
            <div
              key={tpl.id}
              onClick={() => handleSelectTemplate(tpl)}
              style={{
                backgroundColor: '#181922',
                border: isSelected ? '2px solid #E5A93C' : '1px solid rgba(255,255,255,0.12)',
                borderRadius: 12,
                overflow: 'hidden',
                cursor: 'pointer',
                transition: 'all 0.2s',
                boxShadow: isSelected ? '0 0 14px rgba(229, 169, 60, 0.35)' : 'none',
              }}
            >
              <div style={{ position: 'relative' }}>
                <img
                  src={tpl.previewImage}
                  alt={tpl.title}
                  style={{ width: '100%', height: 125, objectFit: 'cover' }}
                />
                <span
                  style={{
                    position: 'absolute',
                    top: 8,
                    left: 8,
                    backgroundColor: 'rgba(0,0,0,0.75)',
                    color: '#E5A93C',
                    fontSize: 10,
                    fontWeight: 800,
                    padding: '2px 6px',
                    borderRadius: 4,
                  }}
                >
                  {tpl.aspectRatio.split(' ')[0]}
                </span>
              </div>
              <div style={{ padding: 12 }}>
                <span style={{ fontSize: 10, color: '#E5A93C', fontWeight: 800 }}>{tpl.badge}</span>
                <h4 style={{ margin: '6px 0 4px', fontSize: 13, lineHeight: 1.3 }}>{tpl.title}</h4>
                <p style={{ margin: 0, fontSize: 11, color: '#888' }}>{tpl.aiEngine}</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* PANNEAU DES CONSIGNES EXPLICITES & EXÉCUTION DU TEMPLATE SÉLECTIONNÉ */}
      <div
        style={{
          backgroundColor: '#181922',
          border: '1px solid rgba(229, 169, 60, 0.45)',
          borderRadius: 14,
          padding: 18,
          display: 'flex',
          flexDirection: 'column',
          gap: 14,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ color: '#E5A93C', fontWeight: 900, fontSize: 15 }}>
                📋 Consignes Explicites du Template :
              </span>
              <span style={{ fontWeight: 800, fontSize: 14, color: '#FFF' }}>
                {selectedTemplate.title}
              </span>
            </div>
            <p style={{ margin: '4px 0 0', fontSize: 12, color: '#AAA' }}>
              Moteur : <strong style={{ color: '#2ED573' }}>{selectedTemplate.aiEngine}</strong> • Style :{' '}
              <strong style={{ color: '#E5A93C' }}>{selectedTemplate.stylePreset}</strong>
            </p>
          </div>

          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            <span style={{ fontSize: 11, color: '#888' }}>Moteur IA :</span>
            {(['gemini', 'claude', 'fal'] as AIProviderId[]).map((prov) => (
              <button
                key={prov}
                type="button"
                onClick={() => setSelectedProvider(prov)}
                style={{
                  backgroundColor: selectedProvider === prov ? '#E5A93C' : '#0D0E12',
                  color: selectedProvider === prov ? '#000' : '#CCC',
                  border: '1px solid rgba(229, 169, 60, 0.35)',
                  borderRadius: 6,
                  padding: '4px 8px',
                  fontSize: 11,
                  fontWeight: 800,
                  cursor: 'pointer',
                  textTransform: 'uppercase',
                }}
              >
                {prov}
              </button>
            ))}
          </div>
        </div>

        {/* LISTE DES CONSIGNES EXPLICITES QUE L'IA VA LIRE ET EXÉCUTER */}
        <div
          style={{
            backgroundColor: '#0D0E12',
            border: '1px solid rgba(255,255,255,0.08)',
            borderRadius: 10,
            padding: 12,
          }}
        >
          <div style={{ fontSize: 12, color: '#E5A93C', fontWeight: 800, marginBottom: 8 }}>
            ⚙️ Programme d’exécution automatique par l'IA :
          </div>
          <div style={{ display: 'grid', gap: 6 }}>
            {selectedTemplate.explicitInstructions.map((instruction, index) => (
              <div
                key={index}
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 8,
                  fontSize: 12,
                  color: '#DDD',
                }}
              >
                <span
                  style={{
                    backgroundColor: 'rgba(229, 169, 60, 0.2)',
                    color: '#E5A93C',
                    width: 20,
                    height: 20,
                    borderRadius: 999,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 11,
                    fontWeight: 800,
                    flexShrink: 0,
                  }}
                >
                  {index + 1}
                </span>
                <span>{instruction}</span>
              </div>
            ))}
          </div>
        </div>

        {/* ============================================================================== */}
        {/* MODÈLE INTERACTIF RÉEL STYLE CAPCUT / CANVA (CANVAS WEB AVEC OUTILS DE MONTAGE) */}
        {/* ============================================================================== */}
        <div
          style={{
            backgroundColor: '#0D0E14',
            border: '2px solid rgba(229, 169, 60, 0.4)',
            borderRadius: 16,
            padding: 16,
            display: 'flex',
            flexDirection: 'column',
            gap: 16,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 20 }}>🎨</span>
                <h3 style={{ margin: 0, fontSize: 16, color: '#E5A93C', fontWeight: 900 }}>
                  Studio Montage & Canvas Interactif (Style CapCut / Canva)
                </h3>
              </div>
              <p style={{ margin: '4px 0 0', fontSize: 12, color: '#A0A5BA' }}>
                Édition directe en temps réel : modifiez les calques de texte, appliquez les filtres et animez le rendu.
              </p>
            </div>

            {/* Bouton de prévisualisation d'animation cinématique */}
            <button
              type="button"
              onClick={() => setIsMotionPreview(!isMotionPreview)}
              style={{
                backgroundColor: isMotionPreview ? '#FF4757' : 'rgba(229, 169, 60, 0.2)',
                border: isMotionPreview ? '1px solid #FF4757' : '1px solid #E5A93C',
                color: isMotionPreview ? '#FFF' : '#E5A93C',
                borderRadius: 10,
                padding: '8px 14px',
                fontSize: 12,
                fontWeight: 800,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              <span>{isMotionPreview ? '⏸️ Arrêter l’Animation' : '▶️ Animation CapCut / Pixverse'}</span>
            </button>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
              gap: 18,
              alignItems: 'start',
            }}
          >
            {/* A. Zone Canvas de Prévisualisation Temps Réel */}
            <div
              style={{
                backgroundColor: '#06070B',
                borderRadius: 14,
                padding: 12,
                border: '1px solid rgba(255, 255, 255, 0.1)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <div
                style={{
                  position: 'relative',
                  width: '100%',
                  maxWidth: 320,
                  borderRadius: 12,
                  overflow: 'hidden',
                  boxShadow: '0 8px 30px rgba(0,0,0,0.8)',
                  border: '1px solid rgba(229, 169, 60, 0.3)',
                  aspectRatio: selectedTemplate.aspectRatio.includes('9:16')
                    ? '9/16'
                    : selectedTemplate.aspectRatio.includes('16:9')
                    ? '16/9'
                    : '4/5',
                }}
              >
                <canvas
                  ref={canvasRef}
                  style={{
                    width: '100%',
                    height: '100%',
                    display: 'block',
                    objectFit: 'contain',
                  }}
                />
              </div>

              <div style={{ marginTop: 8, fontSize: 11, color: '#888', textAlign: 'center' }}>
                Format : {selectedTemplate.aspectRatio.split(' ')[0]} • Qualité Exportation HD
              </div>
            </div>

            {/* B. Panneau des Outils de Montage & Calques Interactifs */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {/* 1. Titre Principal */}
              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 700, color: '#DDD', marginBottom: 4 }}>
                  🔤 Titre du Template (Calque Haut) :
                </label>
                <input
                  type="text"
                  value={canvasTitle}
                  onChange={(e) => setCanvasTitle(e.target.value)}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    backgroundColor: '#161822',
                    border: '1px solid rgba(255,255,255,0.15)',
                    borderRadius: 8,
                    padding: '8px 12px',
                    color: '#FFF',
                    fontSize: 13,
                  }}
                />
              </div>

              {/* 2. Sous-Titre / Hook */}
              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 700, color: '#DDD', marginBottom: 4 }}>
                  💬 Sous-Titre & Hook d'Accroche :
                </label>
                <textarea
                  rows={2}
                  value={canvasSubtitle}
                  onChange={(e) => setCanvasSubtitle(e.target.value)}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    backgroundColor: '#161822',
                    border: '1px solid rgba(255,255,255,0.15)',
                    borderRadius: 8,
                    padding: '8px 12px',
                    color: '#FFF',
                    fontSize: 13,
                  }}
                />
              </div>

              {/* 3. Badge Sticker */}
              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 700, color: '#DDD', marginBottom: 4 }}>
                  🏷️ Badge Visuel (Sticker) :
                </label>
                <input
                  type="text"
                  value={canvasBadge}
                  onChange={(e) => setCanvasBadge(e.target.value)}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    backgroundColor: '#161822',
                    border: '1px solid rgba(255,255,255,0.15)',
                    borderRadius: 8,
                    padding: '8px 12px',
                    color: '#FFF',
                    fontSize: 13,
                  }}
                />
              </div>

              {/* 4. Filtre Visuel / Étalonnage */}
              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 700, color: '#DDD', marginBottom: 6 }}>
                  ✨ Filtre Visuel Style PixVerse / CapCut :
                </label>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                  {[
                    { id: 'gold_afro', label: '✨ Or Afro & Néon' },
                    { id: 'pixverse_cinema', label: '🎬 PixVerse Cinéma' },
                    { id: 'capcut_speed', label: '⚡ Speed-Ramp' },
                    { id: 'vintage_35mm', label: '🎞️ Grain 35mm' },
                    { id: 'none', label: 'Original' },
                  ].map((f) => (
                    <button
                      key={f.id}
                      type="button"
                      onClick={() => setCanvasFilter(f.id as any)}
                      style={{
                        padding: '6px 10px',
                        borderRadius: 8,
                        fontSize: 11,
                        fontWeight: 700,
                        cursor: 'pointer',
                        border: canvasFilter === f.id ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.1)',
                        backgroundColor: canvasFilter === f.id ? 'rgba(229,169,60,0.25)' : 'rgba(255,255,255,0.04)',
                        color: canvasFilter === f.id ? '#E5A93C' : '#AAA',
                      }}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* 5. Importer son propre média source */}
              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 700, color: '#DDD', marginBottom: 6 }}>
                  📁 Remplacer par votre Média Local (Photo / Capture vidéo) :
                </label>
                <label
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 8,
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                    border: '1px dashed rgba(229, 169, 60, 0.4)',
                    borderRadius: 10,
                    padding: '10px 14px',
                    fontSize: 12,
                    color: '#E5A93C',
                    cursor: 'pointer',
                    fontWeight: 700,
                  }}
                >
                  <span>📷 Choisir une photo ou vidéo depuis l'appareil</span>
                  <input
                    type="file"
                    accept="image/*,video/*"
                    onChange={(e) => {
                      if (e.target.files && e.target.files[0]) {
                        const file = e.target.files[0];
                        const url = URL.createObjectURL(file);
                        setUploadedUserMedia(url);
                      }
                    }}
                    style={{ display: 'none' }}
                  />
                </label>
              </div>
            </div>
          </div>
        </div>

        {/* Message de confirmation d'enregistrement Supabase */}
        {publishSuccessMessage && (
          <div
            style={{
              backgroundColor: 'rgba(46, 213, 115, 0.15)',
              border: '2px solid #2ED573',
              borderRadius: 12,
              padding: '14px 18px',
              color: '#2ED573',
              fontSize: 13,
              fontWeight: 800,
              display: 'flex',
              alignItems: 'center',
              gap: 10,
            }}
          >
            <span>✅</span>
            <span>{publishSuccessMessage}</span>
          </div>
        )}

        {/* ATTACHER UN MÉDIA UTILISATEUR OPTIONNEL */}
        <div style={{ display: 'grid', gap: 6 }}>
          <label style={{ fontSize: 12, fontWeight: 700, color: '#BBB' }}>
            📸 Votre média source (Photo, vidéo ou lien à transformer selon les consignes) :
          </label>
          <input
            type="text"
            value={userMediaUrl}
            onChange={(e) => setUserMediaUrl(e.target.value)}
            placeholder="URL de votre image/vidéo (ou laissez vide pour utiliser la banque PANU)..."
            style={{
              width: '100%',
              backgroundColor: '#0D0E12',
              border: '1px solid rgba(255,255,255,0.18)',
              borderRadius: 8,
              padding: 9,
              color: '#FFF',
              fontSize: 13,
            }}
          />
        </div>

        {/* PROMPT CRÉATIF OU SUJET PERSONNALISÉ */}
        <div style={{ display: 'grid', gap: 6 }}>
          <label style={{ fontSize: 12, fontWeight: 700, color: '#BBB' }}>
            ✏️ Consigne personnalisée ou texte de l'utilisateur :
          </label>
          <textarea
            rows={3}
            value={customPrompt}
            onChange={(e) => setCustomPrompt(e.target.value)}
            placeholder="Détaillez le sujet de votre vidéo ou affiche..."
            style={{
              width: '100%',
              backgroundColor: '#0D0E12',
              border: '1px solid rgba(255,255,255,0.18)',
              borderRadius: 8,
              padding: 10,
              color: '#FFF',
              fontSize: 13,
            }}
          />
        </div>

        {/* PROGRESSION DE L'EXÉCUTION IA */}
        {isExecuting && (
          <div style={{ backgroundColor: '#0D0E12', padding: 14, borderRadius: 10, border: '1px solid #E5A93C' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 6 }}>
              <span style={{ color: '#E5A93C', fontWeight: 800 }}>{executionStepText}</span>
              <span style={{ color: '#FFF', fontWeight: 900 }}>{executionProgress}%</span>
            </div>
            <div style={{ width: '100%', height: 8, backgroundColor: '#222', borderRadius: 999, overflow: 'hidden' }}>
              <div
                style={{
                  width: `${executionProgress}%`,
                  height: '100%',
                  backgroundColor: '#E5A93C',
                  transition: 'width 0.4s ease',
                }}
              />
            </div>
          </div>
        )}

        {error && (
          <div style={{ backgroundColor: 'rgba(255,71,87,0.15)', border: '1px solid #FF4757', color: '#FF6B81', padding: 10, borderRadius: 8, fontSize: 12 }}>
            ⚠️ {error}
          </div>
        )}

        {/* RÉSULTAT OBTENU APRÈS EXÉCUTION */}
        {result && (
          <div style={{ backgroundColor: '#0D0E12', padding: 16, borderRadius: 12, border: '2px solid #2ED573' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#2ED573', fontWeight: 800, fontSize: 13, marginBottom: 8 }}>
              <span>✓</span>
              <span>Consignes IA exécutées avec succès via {result.provider.toUpperCase()} ({result.model})</span>
            </div>

            {result.outputText && (
              <p style={{ margin: '0 0 10px', fontSize: 13, color: '#EEE', whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>
                {result.outputText}
              </p>
            )}

            {result.mediaUrl && (
              <div style={{ borderRadius: 10, overflow: 'hidden', border: '1px solid rgba(255,255,255,0.15)', maxHeight: 320 }}>
                <img
                  src={result.mediaUrl}
                  alt="Rendu IA Template"
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
              </div>
            )}
          </div>
        )}

        {/* BOUTONS D'ACTION : EXÉCUTER LES CONSIGNES & PUBLIER */}
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button
            type="button"
            disabled={isExecuting}
            onClick={handleExecuteTemplateInstructions}
            style={{
              flex: 1,
              minWidth: 240,
              backgroundColor: '#E5A93C',
              color: '#000',
              border: 'none',
              borderRadius: 10,
              padding: '13px 18px',
              fontWeight: 900,
              fontSize: 14,
              cursor: isExecuting ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              boxShadow: '0 4px 12px rgba(229, 169, 60, 0.35)',
            }}
          >
            <span>⚡</span>
            <span>
              {isExecuting
                ? 'Exécution IA des Consignes en cours...'
                : `Exécuter les Consignes du Template (${selectedTemplate.aiEngine.split(' ')[0]})`}
            </span>
          </button>

          <button
            type="button"
            disabled={isPublishing}
            onClick={handlePublishToPanu}
            style={{
              backgroundColor: '#2ED573',
              color: '#000',
              border: 'none',
              borderRadius: 10,
              padding: '13px 20px',
              fontWeight: 900,
              fontSize: 14,
              cursor: isPublishing ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              boxShadow: '0 4px 12px rgba(46, 213, 115, 0.3)',
            }}
          >
            <span>🚀</span>
            <span>{isPublishing ? 'Publication...' : 'Publier sur PANU'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default DynamicTemplateGallery;
