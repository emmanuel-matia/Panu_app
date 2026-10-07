import { supabase } from '../lib/supabaseClient';
import { AIProviderId, AITaskType } from '../config/aiKeysConfig';
import { STORAGE_BUCKETS } from '../config/supabaseConfig';
import { sendInstantGmailAlertToFounder } from './securityModerationService';

export interface MultiAiGenerationRequest {
  task: AITaskType;
  prompt: string;
  templateCategory?: 'canva_poster' | 'capcut_clip' | 'tiktok_script' | 'voucher_gift' | string;
  preferredProvider?: AIProviderId;
  modelOverride?: string;
}

export interface MultiAiGenerationResult {
  provider: AIProviderId;
  model: string;
  outputText?: string;
  mediaUrl?: string;
  audioBase64?: string;
  operationName?: string;
}

/**
 * 1.2 SERVICE CLIENT MULTI-IA (`multiAiService.ts`)
 * Appelle la fonction Edge `multi-ai-hub` (Gemini, Claude, Fal.ai, Replicate)
 * et publie directement la création générée sur PANU (`public.posts` / `public.videos`).
 */
export async function generateWithMultiAiHub(
  request: MultiAiGenerationRequest
): Promise<MultiAiGenerationResult> {
  const { data, error } = await supabase.functions.invoke('multi-ai-hub', {
    body: request,
  });

  if (error) {
    throw new Error(error.message || 'Échec de la génération via le Hub Multi-IA');
  }

  return data as MultiAiGenerationResult;
}

/**
 * Publie en un clic le contenu généré depuis un Template dans le flux PANU
 * Téléversement direct dans le stockage sécurisé
 */
export async function publishGeneratedTemplateToPanu(params: {
  userId: string;
  title: string;
  content: string;
  mediaUrl?: string;
  blob?: Blob;
  category: string;
}) {
  let finalMediaUrl = params.mediaUrl || null;

  // Téléversement direct dans le stockage sécurisé
  if (params.blob) {
    try {
      const isVideo = params.category.includes('clip') || params.category.includes('video');
      const fileExt = isVideo ? 'mp4' : 'png';
      const fileName = `template_${Date.now()}_${Math.random().toString(36).slice(2, 8)}.${fileExt}`;
      const filePath = `templates/${fileName}`;

      const { error: uploadError } = await supabase.storage
        .from(STORAGE_BUCKETS.POST_MEDIA)
        .upload(filePath, params.blob, {
          contentType: isVideo ? 'video/mp4' : 'image/png',
          cacheControl: '3600',
          upsert: false,
        });

      if (!uploadError) {
        const { data: publicUrlData } = supabase.storage
          .from(STORAGE_BUCKETS.POST_MEDIA)
          .getPublicUrl(filePath);
        finalMediaUrl = publicUrlData.publicUrl;
      } else {
        console.warn('Notice upload bucket:', uploadError.message);
      }
    } catch (uploadErr) {
      console.warn('Erreur stockage media Supabase:', uploadErr);
    }
  }

  const mediaType = params.category.includes('clip') || params.category.includes('video') ? 'video' : 'image';

  const { data, error } = await supabase
    .from('posts')
    .insert({
      user_id: params.userId,
      title: params.title,
      content: params.content,
      media_url: finalMediaUrl,
      media_type: mediaType,
      is_public: true,
    })
    .select()
    .single();

  if (error) throw error;

  // Alerte Fondateur : Nouveau Projet IA Publié
  sendInstantGmailAlertToFounder({
    eventType: 'IMPORTANT_APP_EVENT',
    subject: `✨ [PANU STUDIO] Nouveau projet IA : ${params.title}`,
    userIdentifier: params.userId,
    details: `Un utilisateur vient de générer et publier un projet "${params.category}" intitulé "${params.title}".`,
  });

  return data;
}
