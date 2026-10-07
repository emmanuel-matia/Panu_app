// ==============================================================================
// SUPABASE EDGE FUNCTION : `multi-ai-hub`
// Connecteur Backend Multi-Modèles (Google Gemini, Anthropic Claude, Fal.ai, Replicate)
// Gère : Scripts TikTok/Reels, Posters/Miniatures Canva, Vidéos CapCut/PeerVerse, Voix-Off TTS
// ==============================================================================

const GEMINI_API_BASE = 'https://generativelanguage.googleapis.com/v1beta/models';
const ANTHROPIC_API_URL = 'https://api.anthropic.com/v1/messages';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

interface MultiAiRequest {
  task: 'viral_script' | 'poster_image' | 'viral_video' | 'voiceover_tts';
  prompt: string;
  templateCategory?: 'canva_poster' | 'capcut_clip' | 'tiktok_script' | 'voucher_gift';
  preferredProvider?: 'gemini' | 'claude' | 'fal' | 'replicate';
  modelOverride?: string;
}

Deno.serve(async (req: Request) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    const body = (await req.json()) as MultiAiRequest;
    const { task, prompt, preferredProvider, modelOverride } = body;

    const geminiKey = Deno.env.get('GEMINI_API_KEY') ?? '';
    const anthropicKey = Deno.env.get('ANTHROPIC_API_KEY') ?? '';
    const falKey = Deno.env.get('FAL_KEY') ?? '';
    const replicateKey = Deno.env.get('REPLICATE_API_KEY') ?? '';

    // ---------------------------------------------------------------------------
    // 1. GÉNÉRATION DE SCRIPTS VIRAUX / HOOKS TIKTOK & REELS (CLAUDE OU GEMINI)
    // ---------------------------------------------------------------------------
    if (task === 'viral_script') {
      if ((preferredProvider === 'claude' || !geminiKey) && anthropicKey) {
        const claudeRes = await fetch(ANTHROPIC_API_URL, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-api-key': anthropicKey,
            'anthropic-version': '2023-06-01',
          },
          body: JSON.stringify({
            model: modelOverride || 'claude-3-5-sonnet-latest',
            max_tokens: 1024,
            system:
              'Tu es le directeur créatif de PANU Studio. Rédige en français un script viral rythmé (Hook 0-3s, Corps visuel, Appel à l’action et hashtags) prêt à être tourné ou généré.',
            messages: [{ role: 'user', content: prompt }],
          }),
        });
        const claudeData = await claudeRes.json();
        return new Response(
          JSON.stringify({
            provider: 'claude',
            model: modelOverride || 'claude-3-5-sonnet-latest',
            outputText: claudeData?.content?.[0]?.text ?? '',
          }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
      }

      // Appel Gemini pour Script Viral
      const textModel = modelOverride || 'gemini-3-flash-preview';
      const geminiRes = await fetch(`${GEMINI_API_BASE}/${textModel}:generateContent?key=${geminiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [
            {
              role: 'user',
              parts: [
                {
                  text: `Tu es le moteur créatif de PANU Studio. Génère en français un contenu viral prêt à publier pour : ${prompt}`,
                },
              ],
            },
          ],
        }),
      });
      const geminiData = await geminiRes.json();
      const outputText =
        geminiData?.candidates?.[0]?.content?.parts?.[0]?.text ?? 'Script généré avec succès.';

      return new Response(
        JSON.stringify({ provider: 'gemini', model: textModel, outputText }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    // ---------------------------------------------------------------------------
    // 2. GÉNÉRATION D'IMAGES / MINIATURES / POSTERS CANVA / VOUCHERS
    // Modèles supportés : gemini-2.5-flash-image / gemini-3.1-flash-lite-image / Fal.ai
    // ---------------------------------------------------------------------------
    if (task === 'poster_image') {
      if (preferredProvider === 'fal' && falKey) {
        const falRes = await fetch('https://fal.run/fal-ai/flux/dev', {
          method: 'POST',
          headers: {
            Authorization: `Key ${falKey}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ prompt, image_size: 'portrait_16_9' }),
        });
        const falData = await falRes.json();
        return new Response(
          JSON.stringify({
            provider: 'fal',
            model: 'fal-ai/flux/dev',
            mediaUrl: falData?.images?.[0]?.url ?? null,
          }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
      }

      const imageModel = modelOverride || 'gemini-2.5-flash-image';
      const imgRes = await fetch(`${GEMINI_API_BASE}/${imageModel}:generateContent?key=${geminiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: { responseModalities: ['TEXT', 'IMAGE'] },
        }),
      });
      const imgData = await imgRes.json();
      const inlinePart = imgData?.candidates?.[0]?.content?.parts?.find(
        (p: any) => p.inlineData?.data
      );
      const base64DataUrl = inlinePart
        ? `data:${inlinePart.inlineData.mimeType || 'image/png'};base64,${inlinePart.inlineData.data}`
        : null;

      return new Response(
        JSON.stringify({
          provider: 'gemini',
          model: imageModel,
          mediaUrl: base64DataUrl,
        }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    // ---------------------------------------------------------------------------
    // 3. GÉNÉRATION DE VIDÉOS VIRALES (CAPCUT / PEERVERSE / VEO 3.1 / GEMINI OMNI)
    // Modèles supportés : veo-3.1-fast-generate-preview / gemini-omni-1.1-flash / Fal / Replicate
    // ---------------------------------------------------------------------------
    if (task === 'viral_video') {
      if (preferredProvider === 'fal' && falKey) {
        const falVideoRes = await fetch(
          'https://fal.run/fal-ai/kling-video/v1.6/standard/text-to-video',
          {
            method: 'POST',
            headers: {
              Authorization: `Key ${falKey}`,
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ prompt, duration: '5', aspect_ratio: '9:16' }),
          }
        );
        const falVideoData = await falVideoRes.json();
        return new Response(
          JSON.stringify({
            provider: 'fal',
            model: 'kling-v1.6',
            mediaUrl: falVideoData?.video?.url ?? null,
          }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
      }

      if (preferredProvider === 'replicate' && replicateKey) {
        const repRes = await fetch('https://api.replicate.com/v1/models/minimax/video-01/predictions', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${replicateKey}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ input: { prompt } }),
        });
        const repData = await repRes.json();
        return new Response(
          JSON.stringify({
            provider: 'replicate',
            model: 'minimax/video-01',
            predictionId: repData?.id,
            statusUrl: repData?.urls?.get,
          }),
          { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
      }

      const videoModel = modelOverride || 'veo-3.1-fast-generate-preview';
      const veoRes = await fetch(`${GEMINI_API_BASE}/${videoModel}:predictLongRunning?key=${geminiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          instances: [{ prompt }],
          parameters: { aspectRatio: '9:16', resolution: '720p' },
        }),
      });
      const veoData = await veoRes.json();

      return new Response(
        JSON.stringify({
          provider: 'gemini',
          model: videoModel,
          operationName: veoData?.name ?? null,
        }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    // ---------------------------------------------------------------------------
    // 4. GÉNÉRATION AUDIO / VOIX-OFF TTS (GEMINI 2.5 FLASH TTS)
    // ---------------------------------------------------------------------------
    if (task === 'voiceover_tts') {
      const ttsModel = modelOverride || 'gemini-2.5-flash-preview-tts';
      const ttsRes = await fetch(`${GEMINI_API_BASE}/${ttsModel}:generateContent?key=${geminiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: {
            responseModalities: ['AUDIO'],
            speechConfig: {
              voiceConfig: { prebuiltVoiceConfig: { voiceName: 'Kore' } },
            },
          },
        }),
      });
      const ttsData = await ttsRes.json();
      const audioPart = ttsData?.candidates?.[0]?.content?.parts?.[0]?.inlineData;

      return new Response(
        JSON.stringify({
          provider: 'gemini',
          model: ttsModel,
          audioBase64: audioPart?.data ?? null,
          mimeType: audioPart?.mimeType ?? 'audio/pcm',
        }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    return new Response(JSON.stringify({ error: 'Type de tâche IA non reconnu' }), {
      status: 400,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    });
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err?.message || 'Erreur serveur Multi-IA' }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    });
  }
});
