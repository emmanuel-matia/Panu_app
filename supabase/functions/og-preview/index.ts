// ==============================================================================
// SUPABASE EDGE FUNCTION : `og-preview`
// Sert les balises Open Graph (<meta property="og:title">, og:image, og:description)
// pour les routes /watch/[id] et /live/[id] aux robots WhatsApp, Facebook, Telegram
// ==============================================================================

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') ?? 'https://xscnbjmiinznzepxzcvn.supabase.co';
const SUPABASE_ANON_KEY = Deno.env.get('SUPABASE_ANON_KEY') ?? '';
const APP_ORIGIN = Deno.env.get('PUBLIC_APP_URL') ?? 'https://panu.app';

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  // Supporte ?type=watch&id=123 ou chemin direct /watch/123 et /live/123
  const pathParts = url.pathname.split('/').filter(Boolean);
  const type = url.searchParams.get('type') || (pathParts.includes('live') ? 'live' : 'watch');
  const contentId = url.searchParams.get('id') || pathParts[pathParts.length - 1] || '';

  const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

  let title = 'PANU — Plateforme Numérique Africaine';
  let description = 'Découvrez les vidéos virales, films IA et lives en direct sur PANU.';
  let imageUrl = `${APP_ORIGIN}/og-default-poster.jpg`;
  const canonicalUrl = `${APP_ORIGIN}/${type}/${encodeURIComponent(contentId)}`;

  if (contentId) {
    // Recherche des métadonnées réelles de la vidéo ou du live dans Supabase
    const { data: video } = await supabase
      .from('videos')
      .select('title, description, thumbnail_url, og_title, og_image_url')
      .eq('id', contentId)
      .maybeSingle();

    if (video) {
      title = video.og_title || video.title || title;
      description = video.description || description;
      imageUrl = video.og_image_url || video.thumbnail_url || imageUrl;
    } else {
      const { data: post } = await supabase
        .from('posts')
        .select('title, content, media_url')
        .eq('id', contentId)
        .maybeSingle();

      if (post) {
        title = post.title || 'Publication PANU';
        description = post.content || description;
        imageUrl = post.media_url || imageUrl;
      }
    }
  }

  const safeTitle = escapeHtml(title);
  const safeDesc = escapeHtml(description);
  const safeImage = escapeHtml(imageUrl);
  const safeUrl = escapeHtml(canonicalUrl);

  const html = `<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8" />
  <title>${safeTitle}</title>
  <meta name="description" content="${safeDesc}" />

  <!-- Balises Open Graph pour WhatsApp, Facebook, Telegram -->
  <meta property="og:site_name" content="PANU" />
  <meta property="og:locale" content="fr_FR" />
  <meta property="og:type" content="${type === 'live' ? 'video.other' : 'video.movie'}" />
  <meta property="og:url" content="${safeUrl}" />
  <meta property="og:title" content="${safeTitle}" />
  <meta property="og:description" content="${safeDesc}" />
  <meta property="og:image" content="${safeImage}" />
  <meta property="og:image:secure_url" content="${safeImage}" />
  <meta property="og:image:width" content="1200" />
  <meta property="og:image:height" content="630" />

  <!-- Balises Twitter / X Card -->
  <meta name="twitter:card" content="summary_large_image" />
  <meta name="twitter:title" content="${safeTitle}" />
  <meta name="twitter:description" content="${safeDesc}" />
  <meta name="twitter:image" content="${safeImage}" />

  <!-- Redirection immédiate pour les utilisateurs humains vers le lecteur de l'application -->
  <meta http-equiv="refresh" content="0;url=${safeUrl}" />
</head>
<body style="background:#0D0E12;color:#FFF;font-family:sans-serif;text-align:center;padding:40px;">
  <h1>${safeTitle}</h1>
  <p>${safeDesc}</p>
  <script>window.location.replace(${JSON.stringify(canonicalUrl)});</script>
</body>
</html>`;

  return new Response(html, {
    status: 200,
    headers: {
      'Content-Type': 'text/html; charset=utf-8',
      'Cache-Control': 'public, max-age=60, s-maxage=300',
    },
  });
});
