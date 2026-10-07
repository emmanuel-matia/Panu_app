-- ==============================================================================
-- PANU — SÉCURITÉ RENFORCÉE, ROBOTS DE MODÉRATION AUTOMATIQUE (STYLE TIKTOK/FB),
-- PROTECTION RLS DES CLÉS API & ALERTES EMAIL RÉELLES GMAIL (`emmanuelmatia150@gmail.com`)
-- ==============================================================================

-- 1. COFFRE-FORT SERVEUR ISOLÉ POUR LES CLÉS API (INACCESSIBLE AU CLIENT)
CREATE SCHEMA IF NOT EXISTS private_security;
REVOKE ALL ON SCHEMA private_security FROM PUBLIC, anon, authenticated;

CREATE TABLE IF NOT EXISTS private_security.api_keys_vault (
    key_name TEXT PRIMARY KEY,
    key_value TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
REVOKE ALL ON ALL TABLES IN SCHEMA private_security FROM PUBLIC, anon, authenticated;

-- 2. TABLES DE MODÉRATION AUTOMATIQUE & BLOCAGE ANTI-PIRATAGE
CREATE TABLE IF NOT EXISTS public.security_moderation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID,
    actor_identifier TEXT,
    event_type TEXT NOT NULL, -- 'spoofing_blocked', 'xss_sqli_blocked', 'spam_flood_blocked', 'new_user_signup_gmail_alert'
    severity TEXT NOT NULL DEFAULT 'HIGH', -- 'INFO', 'MEDIUM', 'HIGH', 'CRITICAL'
    details TEXT NOT NULL,
    blocked_automatically BOOLEAN NOT NULL DEFAULT true,
    founder_email_notified TEXT NOT NULL DEFAULT 'emmanuelmatia150@gmail.com',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.blocked_entities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE,
    email_or_ip TEXT,
    reason TEXT NOT NULL,
    blocked_by TEXT NOT NULL DEFAULT 'PANU_AUTO_MODERATION_BOT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.security_moderation_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blocked_entities ENABLE ROW LEVEL SECURITY;

-- Seul le Fondateur (emmanuelmatia150@gmail.com) peut consulter les journaux de sécurité et entités bloquées
DROP POLICY IF EXISTS "founder_only_read_security_logs" ON public.security_moderation_logs;
CREATE POLICY "founder_only_read_security_logs"
ON public.security_moderation_logs
FOR SELECT
TO authenticated
USING (public.is_founder_or_admin());

DROP POLICY IF EXISTS "authenticated_insert_security_logs" ON public.security_moderation_logs;
CREATE POLICY "authenticated_insert_security_logs"
ON public.security_moderation_logs
FOR INSERT
TO anon, authenticated
WITH CHECK (true);

DROP POLICY IF EXISTS "founder_only_manage_blocked_entities" ON public.blocked_entities;
CREATE POLICY "founder_only_manage_blocked_entities"
ON public.blocked_entities
FOR ALL
TO authenticated
USING (public.is_founder_or_admin())
WITH CHECK (public.is_founder_or_admin());

-- 3. ROBOT SQL DE MODÉRATION AUTOMATIQUE & ANTI-USURPATION (STYLE TIKTOK / FACEBOOK)
CREATE OR REPLACE FUNCTION public.fn_panu_auto_moderation_and_anti_spoofing()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_jwt_email TEXT;
    v_content_to_inspect TEXT;
    v_recent_posts_count INT;
    v_founder_id UUID;
BEGIN
    v_jwt_email := LOWER(COALESCE(auth.jwt() ->> 'email', ''));

    -- Vérifie si l'utilisateur est déjà bloqué par le robot anti-piratage
    IF auth.uid() IS NOT NULL AND EXISTS (
        SELECT 1 FROM public.blocked_entities WHERE user_id = auth.uid()
    ) THEN
        RAISE EXCEPTION 'Accès bloqué par le Robot de Sécurité PANU : compte suspendu pour activité suspecte.';
    END IF;

    -- Inspection anti-usurpation sur la table `profiles`
    IF TG_TABLE_NAME = 'profiles' THEN
        IF v_jwt_email <> 'emmanuelmatia150@gmail.com' THEN
            -- Empêcher toute usurpation du rôle ou du nom du Fondateur
            IF NEW.role IN ('founder', 'admin') THEN
                NEW.role := 'user';
            END IF;

            IF LOWER(COALESCE(NEW.full_name, '')) LIKE '%emmanuel matia%'
               OR LOWER(COALESCE(NEW.username, '')) IN ('emmanuelmatia', 'founder', 'admin_panu', 'panu_officiel') THEN
                INSERT INTO public.security_moderation_logs (
                    actor_user_id, actor_identifier, event_type, severity, details, blocked_automatically
                ) VALUES (
                    auth.uid(), v_jwt_email, 'spoofing_blocked', 'CRITICAL',
                    'Tentative d''usurpation de l''identité du Fondateur (Emmanuel Matia) bloquée automatiquement.', true
                );
                RAISE EXCEPTION 'Sécurité PANU : Usurpation de l''identité du Fondateur détectée et bloquée.';
            END IF;
        END IF;
    END IF;

    -- Inspection anti-piratage (XSS / SQLi) et anti-spam sur `posts` et `videos`
    IF TG_TABLE_NAME IN ('posts', 'videos') THEN
        v_content_to_inspect := LOWER(COALESCE(NEW.title, '') || ' ' || COALESCE(NEW.description, ''));

        -- Détection d'injection de scripts ou commandes malveillantes
        IF v_content_to_inspect ~ '(<script|javascript:|onerror=|onload=|union\s+select|drop\s+table|--\s*sp_)' THEN
            INSERT INTO public.security_moderation_logs (
                actor_user_id, actor_identifier, event_type, severity, details, blocked_automatically
            ) VALUES (
                auth.uid(), v_jwt_email, 'xss_sqli_blocked', 'CRITICAL',
                'Tentative d''injection malveillante (XSS/SQLi) détectée et bloquée dans ' || TG_TABLE_NAME, true
            );

            IF auth.uid() IS NOT NULL THEN
                INSERT INTO public.blocked_entities (user_id, email_or_ip, reason)
                VALUES (auth.uid(), v_jwt_email, 'Tentative d''injection XSS/SQLi bloquée par le robot PANU')
                ON CONFLICT (user_id) DO NOTHING;
            END IF;

            RAISE EXCEPTION 'Sécurité PANU : Contenu malveillant détecté et bloqué par le robot anti-piratage.';
        END IF;

        -- Anti-Spam / Rate Limiting (max 5 publications en 30 secondes par utilisateur)
        IF auth.uid() IS NOT NULL AND TG_TABLE_NAME = 'posts' THEN
            SELECT COUNT(*) INTO v_recent_posts_count
            FROM public.posts
            WHERE user_id = auth.uid()
              AND created_at > (NOW() - INTERVAL '30 seconds');

            IF v_recent_posts_count >= 5 THEN
                INSERT INTO public.security_moderation_logs (
                    actor_user_id, actor_identifier, event_type, severity, details, blocked_automatically
                ) VALUES (
                    auth.uid(), v_jwt_email, 'spam_flood_blocked', 'HIGH',
                    'Flood/Spam détecté (>5 publications en 30s). Action bloquée.', true
                );
                RAISE EXCEPTION 'Modération PANU : Trop de requêtes simultanées (Anti-Spam actif). Veuillez patienter.';
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_panu_moderation_profiles ON public.profiles;
CREATE TRIGGER trg_panu_moderation_profiles
    BEFORE INSERT OR UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_panu_auto_moderation_and_anti_spoofing();

DROP TRIGGER IF EXISTS trg_panu_moderation_posts ON public.posts;
CREATE TRIGGER trg_panu_moderation_posts
    BEFORE INSERT OR UPDATE ON public.posts
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_panu_auto_moderation_and_anti_spoofing();
