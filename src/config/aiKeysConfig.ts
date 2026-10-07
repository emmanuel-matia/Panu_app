/**
 * 1.1 GESTIONNAIRE DE CLÉS API UNIVERSEL (BACKEND / EDGE / SSR)
 * Centralise la détection et la rotation des clés API pour Gemini, Claude (Anthropic),
 * Replicate et Fal.ai sans exposer les secrets côté client.
 */

export type AIProviderId = 'gemini' | 'claude' | 'fal' | 'replicate';

export type AITaskType =
  | 'viral_script'   // Scripts TikTok & Reels, copywriting, hooks
  | 'poster_image'   // Miniatures, Posters Canva, Vouchers/Cadeaux
  | 'viral_video'    // Clips CapCut / PeerVerse / Veo 3.1
  | 'voiceover_tts'; // Audio & Voix-off

export interface AIProviderStatus {
  provider: AIProviderId;
  configured: boolean;
  apiKey?: string;
  defaultModels: Record<AITaskType, string>;
}

/**
 * Lit une variable d'environnement de manière compatible Node.js (Next.js) et Deno (Supabase Edge Functions)
 */
function readServerEnv(key: string): string {
  // Support Deno (Supabase Edge Functions)
  if (typeof globalThis !== 'undefined' && 'Deno' in globalThis) {
    try {
      const val = (globalThis as any).Deno.env.get(key);
      if (val && !val.startsWith('YOUR_')) return val.trim();
    } catch {
      // Ignorer si permission env restreinte
    }
  }
  // Support Node.js / Next.js SSR
  if (typeof process !== 'undefined' && process.env) {
    const val = process.env[key];
    if (val && !val.startsWith('YOUR_')) return val.trim();
  }
  return '';
}

export function getUniversalAIConfig(): Record<AIProviderId, AIProviderStatus> {
  const geminiKey = readServerEnv('GEMINI_API_KEY');
  const anthropicKey = readServerEnv('ANTHROPIC_API_KEY');
  const falKey = readServerEnv('FAL_KEY');
  const replicateKey = readServerEnv('REPLICATE_API_KEY');

  return {
    gemini: {
      provider: 'gemini',
      configured: Boolean(geminiKey),
      apiKey: geminiKey,
      defaultModels: {
        viral_script: 'gemini-3-flash-preview',
        poster_image: 'gemini-2.5-flash-image', // Compatible gemini-3.1-flash-lite-image
        viral_video: 'veo-3.1-fast-generate-preview', // Compatible gemini-omni-1.1-flash / veo-3.1
        voiceover_tts: 'gemini-2.5-flash-preview-tts',
      },
    },
    claude: {
      provider: 'claude',
      configured: Boolean(anthropicKey),
      apiKey: anthropicKey,
      defaultModels: {
        viral_script: 'claude-3-5-sonnet-latest',
        poster_image: 'gemini-2.5-flash-image',
        viral_video: 'veo-3.1-fast-generate-preview',
        voiceover_tts: 'gemini-2.5-flash-preview-tts',
      },
    },
    fal: {
      provider: 'fal',
      configured: Boolean(falKey),
      apiKey: falKey,
      defaultModels: {
        viral_script: 'gemini-3-flash-preview',
        poster_image: 'fal-ai/flux/dev',
        viral_video: 'fal-ai/kling-video/v1.6/standard/text-to-video',
        voiceover_tts: 'fal-ai/f5-tts',
      },
    },
    replicate: {
      provider: 'replicate',
      configured: Boolean(replicateKey),
      apiKey: replicateKey,
      defaultModels: {
        viral_script: 'meta/meta-llama-3-70b-instruct',
        poster_image: 'black-forest-labs/flux-schnell',
        viral_video: 'minimax/video-01',
        voiceover_tts: 'lucataco/xtts-v2',
      },
    },
  };
}

/**
 * Sélectionne automatiquement le meilleur moteur IA disponible selon la tâche et la préférence
 */
export function resolveOptimalProvider(
  task: AITaskType,
  preferredProvider?: AIProviderId
): AIProviderStatus {
  const config = getUniversalAIConfig();

  if (preferredProvider && config[preferredProvider]?.configured) {
    return config[preferredProvider];
  }

  // Ordre de priorité intelligent par type de tâche
  const priorityOrder: Record<AITaskType, AIProviderId[]> = {
    viral_script: ['claude', 'gemini', 'replicate'],
    poster_image: ['gemini', 'fal', 'replicate'],
    viral_video: ['gemini', 'fal', 'replicate'],
    voiceover_tts: ['gemini', 'fal', 'replicate'],
  };

  for (const candidate of priorityOrder[task]) {
    if (config[candidate].configured) {
      return config[candidate];
    }
  }

  // Repli par défaut sur Gemini
  return config.gemini;
}
