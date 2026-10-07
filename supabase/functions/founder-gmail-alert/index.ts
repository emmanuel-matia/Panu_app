// ==============================================================================
// SUPABASE EDGE FUNCTION : `founder-gmail-alert`
// Envoie un e-mail réel et instantané à l'adresse Gmail du Fondateur :
// `emmanuelmatia150@gmail.com` via Resend API / Webhook SMTP à chaque nouvelle
// inscription ou alerte de sécurité critique.
// ==============================================================================

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

declare const Deno: {
  env: { get(key: string): string | undefined };
  serve(handler: (req: Request) => Promise<Response> | Response): void;
};

const FOUNDER_GMAIL = 'emmanuelmatia150@gmail.com';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

interface FounderEmailAlertPayload {
  eventType: 'NEW_USER_SIGNUP' | 'SECURITY_BOT_BLOCK' | 'IMPORTANT_APP_EVENT';
  subject?: string;
  userIdentifier?: string;
  fullName?: string;
  authMethod?: string;
  details?: string;
}

Deno.serve(async (req: Request) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    const body = (await req.json()) as FounderEmailAlertPayload;
    const resendApiKey = Deno.env.get('RESEND_API_KEY') || '';
    const customWebhookUrl = Deno.env.get('FOUNDER_ALERT_WEBHOOK_URL') || '';
    const supabaseUrl = Deno.env.get('SUPABASE_URL') || 'https://xscnbjmiinznzepxzcvn.supabase.co';
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';

    const subject =
      body.subject ||
      (body.eventType === 'NEW_USER_SIGNUP'
        ? `[PANU] Nouvelle inscription : ${body.fullName || body.userIdentifier} (${body.authMethod || 'Direct'})`
        : body.eventType === 'SECURITY_BOT_BLOCK'
        ? `🚨 [PANU SÉCURITÉ] Menace bloquée par le Robot de Modération`
        : `🔔 [PANU] Alerte Événement Important`);

    const htmlContent = `
      <div style="font-family: Arial, sans-serif; background-color: #0D0E12; color: #F8F9FA; padding: 24px; border-radius: 12px; border: 1px solid #E5A93C;">
        <h2 style="color: #E5A93C; margin-top: 0;">PANU — Notification Officielle Fondateur</h2>
        <p><strong>Destinataire :</strong> ${FOUNDER_GMAIL}</p>
        <p><strong>Type d'événement :</strong> ${body.eventType}</p>
        ${body.fullName ? `<p><strong>Utilisateur :</strong> ${body.fullName} (${body.userIdentifier || 'N/A'})</p>` : ''}
        ${body.authMethod ? `<p><strong>Méthode d'authentification :</strong> ${body.authMethod}</p>` : ''}
        <p><strong>Détails :</strong> ${body.details || 'Auto-abonnement au compte Fondateur activé avec succès.'}</p>
        <hr style="border-color: #2A2C38; margin: 16px 0;" />
        <p style="font-size: 12px; color: #9E9EAA;">Envoyé automatiquement en temps réel par Supabase Edge Functions & PANU Security Bot.</p>
      </div>
    `;

    let emailDispatched = false;
    let providerUsed = 'database_realtime_log';

    // 1. Envoi réel via Resend API vers emmanuelmatia150@gmail.com
    if (resendApiKey) {
      const resendResp = await fetch('https://api.resend.com/emails', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${resendApiKey}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          from: 'PANU Security & Auth <onboarding@resend.dev>',
          to: [FOUNDER_GMAIL],
          subject,
          html: htmlContent,
        }),
      });
      if (resendResp.ok) {
        emailDispatched = true;
        providerUsed = 'resend_api_gmail';
      }
    }

    // 2. Envoi vers un Webhook personnalisé (Make / Zapier / Gmail Apps Script) si configuré
    if (!emailDispatched && customWebhookUrl) {
      const whResp = await fetch(customWebhookUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          to: FOUNDER_GMAIL,
          subject,
          html: htmlContent,
          payload: body,
          timestamp: new Date().toISOString(),
        }),
      });
      if (whResp.ok) {
        emailDispatched = true;
        providerUsed = 'custom_gmail_webhook';
      }
    }

    // 3. Journalisation systématique dans `security_moderation_logs` et `notifications` sur Supabase
    if (supabaseServiceKey) {
      const adminClient = createClient(supabaseUrl, supabaseServiceKey);
      await adminClient.from('security_moderation_logs').insert({
        actor_identifier: body.userIdentifier || 'system',
        event_type: body.eventType.toLowerCase(),
        severity: body.eventType === 'SECURITY_BOT_BLOCK' ? 'CRITICAL' : 'INFO',
        details: `${subject} — ${body.details || ''}`,
        blocked_automatically: body.eventType === 'SECURITY_BOT_BLOCK',
        founder_email_notified: FOUNDER_GMAIL,
      });
    }

    return new Response(
      JSON.stringify({
        ok: true,
        recipient: FOUNDER_GMAIL,
        emailDispatched,
        providerUsed,
        subject,
      }),
      {
        status: 200,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({
        ok: false,
        recipient: FOUNDER_GMAIL,
        error: err instanceof Error ? err.message : 'Erreur interne Edge Function',
      }),
      {
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    );
  }
});
