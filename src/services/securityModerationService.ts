import { supabase, FOUNDER_EMAIL } from '../lib/supabaseClient';

export interface ModerationCheckResult {
  allowed: boolean;
  reason?: string;
  threatCategory?: 'SPOOFING' | 'XSS_SQLI_INJECTION' | 'SPAM_FLOOD' | 'ABUSE';
}

const recentActionTimestamps: number[] = [];

/**
 * 1. ROBOT DE MODÉRATION AUTOMATIQUE & ANTI-PIRATAGE (STYLE TIKTOK / FACEBOOK)
 * Analyse en temps réel chaque saisie (profil, commentaire, vidéo, chat en direct)
 * et bloque immédiatement toute tentative d'usurpation, de spam ou d'injection.
 */
export async function inspectAndModerateUserAction(params: {
  actorEmail?: string | null;
  actorName?: string | null;
  content: string;
  actionType: 'signup' | 'profile_update' | 'post_publish' | 'live_chat';
}): Promise<ModerationCheckResult> {
  const cleanEmail = (params.actorEmail || '').trim().toLowerCase();
  const cleanName = (params.actorName || '').trim().toLowerCase();
  const lowerContent = (params.content || '').toLowerCase();

  // A. Détection d'usurpation du Fondateur (Anti-Spoofing)
  if (cleanEmail !== FOUNDER_EMAIL.toLowerCase()) {
    const founderSpoofPatterns = ['emmanuel matia', 'emmanuelmatia150', 'fondateur officiel panu', 'admin officiel panu'];
    if (founderSpoofPatterns.some((pattern) => cleanName.includes(pattern) || lowerContent.includes(pattern))) {
      await logAndAlertSecurityThreat({
        actorIdentifier: cleanEmail || 'anonyme',
        threatCategory: 'SPOOFING',
        details: `Tentative d'usurpation du compte Fondateur (${FOUNDER_EMAIL}) bloquée dans ${params.actionType}.`,
      });
      return {
        allowed: false,
        threatCategory: 'SPOOFING',
        reason: 'Sécurité PANU : Tentative d’usurpation d’identité officielle détectée et bloquée immédiatement.',
      };
    }
  }

  // B. Détection d'attaques XSS / Injection SQL / Piratage
  const hackingRegex = /(<script|javascript:|onerror\s*=|onload\s*=|union\s+select|drop\s+table|insert\s+into\s+auth\.|--\s*sp_)/i;
  if (hackingRegex.test(params.content) || hackingRegex.test(params.actorName || '')) {
    await logAndAlertSecurityThreat({
      actorIdentifier: cleanEmail || 'anonyme',
      threatCategory: 'XSS_SQLI_INJECTION',
      details: `Payload d'attaque XSS/SQLi détecté et bloqué automatiquement lors de ${params.actionType}.`,
    });
    return {
      allowed: false,
      threatCategory: 'XSS_SQLI_INJECTION',
      reason: 'Robot Anti-Piratage PANU : Requête malveillante bloquée et signalée au Fondateur.',
    };
  }

  // C. Anti-Spam & Flood (Style TikTok / Facebook : max 6 actions en 15 secondes)
  const now = Date.now();
  while (recentActionTimestamps.length > 0 && now - recentActionTimestamps[0] > 15000) {
    recentActionTimestamps.shift();
  }
  recentActionTimestamps.push(now);

  if (recentActionTimestamps.length > 6) {
    await logAndAlertSecurityThreat({
      actorIdentifier: cleanEmail || 'anonyme',
      threatCategory: 'SPAM_FLOOD',
      details: `Rafale de spam détectée (${recentActionTimestamps.length} requêtes en <15s) sur ${params.actionType}.`,
    });
    return {
      allowed: false,
      threatCategory: 'SPAM_FLOOD',
      reason: 'Modération Automatique PANU : Activité de spam détectée. Veuillez patienter quelques secondes.',
    };
  }

  return { allowed: true };
}

/**
 * 2. ENVOI D'ALERTE EMAIL RÉELLE ET INSTANTANÉE SUR GMAIL (`emmanuelmatia150@gmail.com`)
 * Connecté à la Supabase Edge Function `founder-gmail-alert` (Resend / Webhook SMTP).
 */
export async function sendInstantGmailAlertToFounder(payload: {
  eventType: 'NEW_USER_SIGNUP' | 'SECURITY_BOT_BLOCK' | 'IMPORTANT_APP_EVENT';
  subject?: string;
  userIdentifier?: string;
  fullName?: string;
  authMethod?: string;
  details: string;
}): Promise<boolean> {
  try {
    const { data, error } = await supabase.functions.invoke('founder-gmail-alert', {
      body: {
        ...payload,
        founderEmail: FOUNDER_EMAIL,
      },
    });
    if (!error && data?.ok) {
      return true;
    }
  } catch {}

  // Enregistrement de secours dans la table temps réel `security_moderation_logs`
  try {
    await supabase.from('security_moderation_logs').insert({
      actor_identifier: payload.userIdentifier || 'system',
      event_type: payload.eventType.toLowerCase(),
      severity: payload.eventType === 'SECURITY_BOT_BLOCK' ? 'CRITICAL' : 'INFO',
      details: payload.details,
      blocked_automatically: payload.eventType === 'SECURITY_BOT_BLOCK',
      founder_email_notified: FOUNDER_EMAIL,
    });
  } catch {}

  return false;
}

async function logAndAlertSecurityThreat(params: {
  actorIdentifier: string;
  threatCategory: 'SPOOFING' | 'XSS_SQLI_INJECTION' | 'SPAM_FLOOD' | 'ABUSE';
  details: string;
}): Promise<void> {
  await sendInstantGmailAlertToFounder({
    eventType: 'SECURITY_BOT_BLOCK',
    subject: `🚨 [PANU SÉCURITÉ] Menace ${params.threatCategory} bloquée`,
    userIdentifier: params.actorIdentifier,
    details: params.details,
  });
}
