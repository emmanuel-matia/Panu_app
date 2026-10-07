-- ==============================================================================
-- PROJET PANU — AUDIT COMPLET & SCHÉMA SQL FINAL ANTI-CONTRADICTION (SUPABASE)
-- Copiez-collez ce script dans l'éditeur SQL de Supabase (https://xscnbjmiinznzepxzcvn.supabase.co)
-- ==============================================================================

-- 1. TABLES PRINCIPALES & MODULES MÉTIERS PANU
CREATE TABLE IF NOT EXISTS public.follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_panu_follow UNIQUE (follower_id, following_id)
);

CREATE TABLE IF NOT EXISTS public.videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    style_preset TEXT DEFAULT 'Cinématographique',
    duration_seconds INTEGER DEFAULT 15,
    views_count BIGINT DEFAULT 0,
    likes_count BIGINT DEFAULT 0,
    shares_count BIGINT DEFAULT 0,
    og_title TEXT,
    og_image_url TEXT,
    status TEXT NOT NULL DEFAULT 'published' CHECK (status IN ('draft', 'published', 'archived')),
    visibility TEXT NOT NULL DEFAULT 'public' CHECK (visibility IN ('public', 'private', 'unlisted')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    post_id UUID REFERENCES public.posts(id) ON DELETE CASCADE,
    video_id UUID REFERENCES public.videos(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    post_id UUID REFERENCES public.posts(id) ON DELETE CASCADE,
    video_id UUID REFERENCES public.videos(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    notification_type TEXT NOT NULL DEFAULT 'system',
    is_read BOOLEAN NOT NULL DEFAULT false,
    action_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.canvas_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    canvas_width INTEGER NOT NULL DEFAULT 1080,
    canvas_height INTEGER NOT NULL DEFAULT 1920,
    background_hex TEXT NOT NULL DEFAULT '#121214',
    layers_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    export_format TEXT NOT NULL DEFAULT 'PNG' CHECK (export_format IN ('PNG', 'JPG', 'MP4')),
    preview_url TEXT,
    is_template BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.video_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Tendances TikTok',
    style_preset TEXT NOT NULL DEFAULT 'Cinématographique',
    prompt_template TEXT NOT NULL,
    preview_video_url TEXT NOT NULL,
    thumbnail_url TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 15,
    uses_count BIGINT NOT NULL DEFAULT 0,
    is_featured BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.live_gifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id TEXT NOT NULL,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    sender_name TEXT NOT NULL,
    receiver_name TEXT NOT NULL,
    gift_type TEXT NOT NULL,
    gift_icon TEXT NOT NULL,
    credits_amount INTEGER NOT NULL CHECK (credits_amount > 0),
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.ad_impressions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    viewer_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    creator_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    video_or_stream_id TEXT NOT NULL,
    ad_campaign_title TEXT NOT NULL,
    advertiser_name TEXT NOT NULL,
    watched_seconds INTEGER NOT NULL DEFAULT 15,
    revenue_generated_fcfa NUMERIC(12, 2) NOT NULL DEFAULT 25.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.creator_earnings (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    total_views BIGINT NOT NULL DEFAULT 0,
    total_shares BIGINT NOT NULL DEFAULT 0,
    gifts_received_credits BIGINT NOT NULL DEFAULT 0,
    ad_revenue_fcfa NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    views_shares_bonus_fcfa NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_available_fcfa NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.member_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_number TEXT NOT NULL UNIQUE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    holder_full_name TEXT NOT NULL,
    holder_role TEXT NOT NULL,
    company_name TEXT NOT NULL DEFAULT 'PANU Studio Officiel',
    department TEXT DEFAULT 'Direction & Création',
    avatar_url TEXT,
    nfc_uid TEXT UNIQUE,
    qr_payload TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Actif' CHECK (status IN ('Actif', 'Invalide', 'Suspendu', 'Expiré')),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '3 years')
);

-- Fonction RPC `verify_member_card` pour la page publique /verify/:cardNumber
CREATE OR REPLACE FUNCTION public.verify_member_card(p_card_number TEXT)
RETURNS TABLE (
    card_number TEXT,
    holder_full_name TEXT,
    holder_role TEXT,
    company_name TEXT,
    department TEXT,
    avatar_url TEXT,
    status TEXT,
    issued_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    is_authentic BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        mc.card_number,
        mc.holder_full_name,
        mc.holder_role,
        mc.company_name,
        mc.department,
        mc.avatar_url,
        mc.status,
        mc.issued_at,
        mc.expires_at,
        (mc.status = 'Actif') AS is_authentic
    FROM public.member_cards mc
    WHERE UPPER(TRIM(mc.card_number)) = UPPER(TRIM(p_card_number))
       OR UPPER(TRIM(COALESCE(mc.nfc_uid, ''))) = UPPER(TRIM(p_card_number))
    LIMIT 1;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.verify_member_card(TEXT) TO anon, authenticated;
