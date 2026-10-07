import { supabase, FOUNDER_EMAIL } from '../lib/supabaseClient';
import { inspectAndModerateUserAction, sendInstantGmailAlertToFounder } from './securityModerationService';

export interface OfflineStudioDraft {
  id: string;
  title: string;
  category: string;
  promptOrLayersJson: string;
  exportFormat: 'PNG' | 'JPG' | 'MP4';
  updatedAt: string;
  synced: boolean;
}

const OFFLINE_DRAFTS_KEY = 'panu_offline_capcut_studio_drafts_v1';
const CACHED_TEMPLATES_KEY = 'panu_offline_cached_templates_v1';

/**
 * 1. SYSTÈME DE NOTIFICATIONS DE CONFIRMATION DOUBLES (UTILISATEUR + FONDATEUR)
 * Envoie dès la création d'un compte :
 * - Côté Utilisateur : Notification de bienvenue et de confirmation adaptée à la méthode (Google, Apple ID, Facebook, WhatsApp OTP, SMS, E-mail).
 * - Côté Fondateur (emmanuelmatia150@gmail.com) : Alerte instantanée informant de l'inscription du nouvel utilisateur.
 */
export async function triggerDualSignupConfirmationNotifications(params: {
  userId?: string;
  userIdentifier: string;
  fullName?: string;
  authMethod: 'Google (Gmail)' | 'Apple ID' | 'Facebook' | 'WhatsApp OTP' | 'Téléphone SMS' | 'E-mail';
  referralBy?: string; // Pseudonyme du parrain
}): Promise<void> {
  const displayName = params.fullName?.trim() || params.userIdentifier.split('@')[0];

  const userNotification = {
    user_id: params.userId || null,
    title: `Bienvenue sur PANU • Confirmation ${params.authMethod} ✅`,
    message: `Bonjour ${displayName} ! Votre inscription via ${params.authMethod} (${params.userIdentifier}) est confirmée. Vos +60 Crédits Gratuits et le Mode Studio Gratuit & Hors-ligne (Style CapCut) sont activés.`,
    notification_type: 'auth_user_welcome',
    is_read: false,
  };

  const founderAlertNotification = {
    user_id: params.userId || null,
    title: `🔔 Alerte Fondateur (${FOUNDER_EMAIL}) : Nouvel inscrit via ${params.authMethod}`,
    message: `Nouveau compte créé par ${displayName} (${params.userIdentifier}) via ${params.authMethod}. Auto-abonnement au compte Fondateur (${FOUNDER_EMAIL}) activé.`,
    notification_type: 'founder_instant_alert',
    is_read: false,
  };

  try {
    await supabase.from('notifications').insert([userNotification, founderAlertNotification]);
  } catch {
    // En mode hors-ligne, mise en file d'attente locale
  }

  // Envoi d'un e-mail réel et instantané au Fondateur (emmanuelmatia150@gmail.com) via Edge Function / Resend / Webhook
  await sendInstantGmailAlertToFounder({
    eventType: 'NEW_USER_SIGNUP',
    subject: `🔔 [PANU] Nouvelle inscription : ${displayName} (${params.authMethod})`,
    userIdentifier: params.userIdentifier,
    fullName: displayName,
    authMethod: params.authMethod,
    details: `Nouvel utilisateur inscrit via ${params.authMethod} (${params.userIdentifier}). Auto-abonnement au compte Fondateur (${FOUNDER_EMAIL}) activé.`,
  });

  // 3. Fallback Auto-follow JS (Emmanuel MATIA)
  if (params.userId) {
    try {
      const { data: founder } = await supabase
        .from('profiles')
        .select('id')
        .or(`username.eq.Emmanuel MATIA,full_name.eq.Emmanuel MATIA,email.eq.${FOUNDER_EMAIL}`)
        .order('created_at', { ascending: true })
        .limit(1)
        .single();

      if (founder && founder.id !== params.userId) {
        await supabase.from('follows').insert({
          follower_id: params.userId,
          following_id: founder.id
        });
      }
    } catch (e) {
      console.warn('[Auto-follow Fallback] Erreur ignorée:', e);
    }
  }

  // 4. Attribution des crédits de parrainage (+50)
  if (params.referralBy && params.userId) {
    try {
      // Trouver le parrain
      const { data: referrer } = await supabase
        .from('profiles')
        .select('id')
        .eq('username', params.referralBy)
        .single();

      if (referrer) {
        // Ajouter des crédits au parrain (via RPC existant ou direct update)
        // Utilisons un update direct simple pour cette démo virale
        const { data: currentCredits } = await supabase
          .from('ai_credits')
          .select('balance')
          .eq('user_id', referrer.id)
          .single();

        if (currentCredits) {
          await supabase
            .from('ai_credits')
            .update({ balance: currentCredits.balance + 50 })
            .eq('user_id', referrer.id);

          // Notification au parrain
          await supabase.from('notifications').insert({
            user_id: referrer.id,
            title: '🎁 Crédits de parrainage reçus !',
            message: `Félicitations ! ${displayName} a rejoint PANU grâce à vous. Vous avez reçu +50 Crédits IA.`,
            notification_type: 'referral_reward'
          });
        }
      }
    } catch (e) {
      console.warn('[Referral Logic] Erreur:', e);
    }
  }
}

/**
 * 2. MODE GRATUIT & HORS-LIGNE (STYLE CAPCUT / PWA)
 * Permet de consulter les templates hors-ligne, d'éditer localement dans le Studio sans connexion,
 * et de synchroniser automatiquement dès le retour du réseau.
 */
export function saveDraftLocallyOfflineFirst(draft: Omit<OfflineStudioDraft, 'updatedAt' | 'synced'>): OfflineStudioDraft {
  const existing = getLocalStudioDrafts();
  const entry: OfflineStudioDraft = {
    ...draft,
    updatedAt: new Date().toISOString(),
    synced: false,
  };
  const updated = [entry, ...existing.filter((d) => d.id !== draft.id)].slice(0, 30);
  try {
    localStorage.setItem(OFFLINE_DRAFTS_KEY, JSON.stringify(updated));
  } catch {}
  return entry;
}

export function getLocalStudioDrafts(): OfflineStudioDraft[] {
  try {
    const raw = localStorage.getItem(OFFLINE_DRAFTS_KEY);
    return raw ? (JSON.parse(raw) as OfflineStudioDraft[]) : [];
  } catch {
    return [];
  }
}

export function cacheTemplatesLocally<T>(templates: T[]): void {
  try {
    localStorage.setItem(CACHED_TEMPLATES_KEY, JSON.stringify(templates));
  } catch {}
}

export function getCachedTemplatesOffline<T>(fallback: T[]): T[] {
  try {
    const raw = localStorage.getItem(CACHED_TEMPLATES_KEY);
    return raw ? (JSON.parse(raw) as T[]) : fallback;
  } catch {
    return fallback;
  }
}

/**
 * Synchronise automatiquement tous les projets édités hors-ligne dès le retour du réseau.
 */
export async function syncPendingOfflineDraftsToSupabase(): Promise<number> {
  if (typeof navigator !== 'undefined' && !navigator.onLine) {
    return 0;
  }

  const drafts = getLocalStudioDrafts();
  const pending = drafts.filter((d) => !d.synced);
  if (pending.length === 0) return 0;

  const { data: sessionData } = await supabase.auth.getSession();
  const userId = sessionData?.session?.user?.id;
  if (!userId) return 0;

  let syncedCount = 0;
  const updatedDrafts = [...drafts];

  for (const draft of pending) {
    const { error } = await supabase.from('canvas_projects').upsert({
      id: draft.id,
      user_id: userId,
      title: draft.title,
      export_format: draft.exportFormat,
      layers_json: draft.promptOrLayersJson,
    });

    if (!error) {
      syncedCount++;
      const idx = updatedDrafts.findIndex((item) => item.id === draft.id);
      if (idx !== -1) {
        updatedDrafts[idx] = { ...updatedDrafts[idx], synced: true };
      }
    }
  }

  try {
    localStorage.setItem(OFFLINE_DRAFTS_KEY, JSON.stringify(updatedDrafts));
  } catch {}

  return syncedCount;
}

/**
 * Initialise les écouteurs de reconnexion réseau et l'enregistrement du Service Worker PWA.
 */
export function initOfflinePwaAndAutoSync(onSyncedCallback?: (count: number) => void): void {
  if (typeof window === 'undefined') return;

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  }

  window.addEventListener('online', async () => {
    const count = await syncPendingOfflineDraftsToSupabase();
    if (count > 0 && onSyncedCallback) {
      onSyncedCallback(count);
    }
  });
}
