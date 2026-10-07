-- ==============================================================================
-- PROJET PANU — AUDIT COMPLET & SCHÉMA SQL FINAL ANTI-CONTRADICTION (SUPABASE)
-- Langue officielle : Français
-- Inclut :
-- 1. Authentification & Rôle Fondateur immuable (emmanuelmatia150@gmail.com)
-- 2. Auto-abonnement discret de chaque nouveau membre au compte du Fondateur
-- 3. Flux d'Accueil Dynamique (posts, videos, likes, comments, follows)
-- 4. Système de Notifications en temps réel (notifications)
-- 5. Studio Graphique & Design Style Canva (canvas_projects : PNG, JPG, MP4)
-- 6. Studio Vidéo IA & Templates Viraux Style CapCut/Pixverse (video_templates)
-- 7. Lives, Cadeaux Virtuels & Monétisation (live_gifts, ad_impressions, creator_earnings)
-- 8. Vérification des Cartes de Membres/Personnel (member_cards & RPC verify_member_card)
-- ==============================================================================

-- TYPE RÔLE UTILISATEUR (SI NON EXISTANT)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE public.user_role AS ENUM ('founder', 'admin', 'business', 'creator', 'user');
    END IF;
END $$;

-- 1. TABLE PROFILES (UTILISATEURS & CRÉATEURS)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    phone TEXT,
    username TEXT UNIQUE,
    full_name TEXT,
    bio TEXT,
    avatar_url TEXT,
    location TEXT,
    category TEXT DEFAULT 'Créateur de contenu',
    role public.user_role NOT NULL DEFAULT 'user'::public.user_role,
    theme_preference TEXT DEFAULT 'light',
    is_verified BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. TABLE FOLLOWS (ABONNEMENTS & AUTO-FOLLOW FONDATEUR)
CREATE TABLE IF NOT EXISTS public.follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_panu_follow UNIQUE (follower_id, following_id)
);

-- 3. TABLE POSTS & VIDEOS (FLUX VERTICAL STYLE TIKTOK & VOD)
CREATE TABLE IF NOT EXISTS public.posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT,
    content TEXT NOT NULL,
    media_url TEXT,
    media_type TEXT NOT NULL DEFAULT 'video',
    status TEXT NOT NULL DEFAULT 'published' CHECK (status IN ('draft', 'published', 'archived')),
    visibility TEXT NOT NULL DEFAULT 'public' CHECK (visibility IN ('public', 'private', 'unlisted')),
    views_count BIGINT NOT NULL DEFAULT 0,
    shares_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
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

-- 4. TABLES LIKES & COMMENTS
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

-- 5. TABLE NOTIFICATIONS (SYSTÈME TEMPS RÉEL & HISTORIQUE UTILISATEUR)
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    notification_type TEXT NOT NULL DEFAULT 'system' CHECK (notification_type IN ('auth', 'gift', 'order', 'like', 'comment', 'system', 'verification')),
    is_read BOOLEAN NOT NULL DEFAULT false,
    action_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications(user_id, created_at DESC);

-- 6. TABLE CANVAS_PROJECTS (STUDIO GRAPHIQUE & DESIGN STYLE CANVA)
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

CREATE INDEX IF NOT EXISTS idx_canvas_projects_user ON public.canvas_projects(user_id, updated_at DESC);

-- 7. TABLE VIDEO_TEMPLATES (MODÈLES VIRAUX STYLE CAPCUT / PIXVERSE)
CREATE TABLE IF NOT EXISTS public.video_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Tendances TikTok',
    style_preset TEXT NOT NULL DEFAULT 'Cinématographique' CHECK (style_preset IN ('Cinématographique', 'Animation 3D', 'Anime', 'Afro-Futurisme', 'Publicité Produit')),
    prompt_template TEXT NOT NULL,
    preview_video_url TEXT NOT NULL,
    thumbnail_url TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 15,
    uses_count BIGINT NOT NULL DEFAULT 0,
    is_featured BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. TABLE AI_CREDITS (CRÉDITS PANU POUR CADEAUX LIVES & GÉNÉRATIONS IA)
CREATE TABLE IF NOT EXISTS public.ai_credits (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    balance INTEGER NOT NULL DEFAULT 250,
    free_credits INTEGER NOT NULL DEFAULT 250,
    purchased_credits INTEGER NOT NULL DEFAULT 0,
    used_credits INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. TABLE LIVE_GIFTS (CADEAUX VIRTUELS PENDANT LES LIVES : ROSES, COURONNES, ETC.)
CREATE TABLE IF NOT EXISTS public.live_gifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id TEXT NOT NULL,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    sender_name TEXT NOT NULL,
    receiver_name TEXT NOT NULL,
    gift_type TEXT NOT NULL CHECK (gift_type IN ('Rose', 'Couronne', 'Lion d''Or', 'Diamant PANU', 'Fusée Virale')),
    gift_icon TEXT NOT NULL,
    credits_amount INTEGER NOT NULL CHECK (credits_amount > 0),
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_live_gifts_stream ON public.live_gifts(stream_id, created_at DESC);

-- 10. TABLES AD_IMPRESSIONS & CREATOR_EARNINGS (MONÉTISATION & RÉMUNÉRATION CRÉATEURS)
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

-- 11. TABLE MEMBER_CARDS & FONCTION RPC VERIFY_MEMBER_CARD (CARTES QR CODE & NFC)
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

CREATE INDEX IF NOT EXISTS idx_member_cards_number ON public.member_cards(card_number);

-- Insertion des cartes officielles de référence (Fondateur + Personnel)
INSERT INTO public.member_cards (
    card_number, holder_full_name, holder_role, company_name, department, nfc_uid, qr_payload, status
) VALUES
(
    'PANU-FND-001',
    'Emmanuel Matia',
    'Fondateur & PDG',
    'PANU Group International',
    'Direction Générale',
    'NFC-PANU-0001-FND',
    'https://panu.app/verify/PANU-FND-001',
    'Actif'
),
(
    'PANU-PRO-2026',
    'Aïcha Koné',
    'Directrice Artistique IA',
    'PANU Studio Abidjan',
    'Production Visuelle & Design',
    'NFC-PANU-2026-PRO',
    'https://panu.app/verify/PANU-PRO-2026',
    'Actif'
),
(
    'PANU-STF-884',
    'Régie Technique Live',
    'Responsable Diffusion Live Sports',
    'PANU Media Africa',
    'Régie Direct & Streaming',
    'NFC-PANU-0884-STF',
    'https://panu.app/verify/PANU-STF-884',
    'Actif'
),
(
    'PANU-REV-000',
    'Badge Test Expiré',
    'Prestataire Externe',
    'PANU Externe',
    'Accès Temporaire',
    'NFC-PANU-0000-REV',
    'https://panu.app/verify/PANU-REV-000',
    'Invalide'
)
ON CONFLICT (card_number) DO NOTHING;

-- Fonction RPC officielle `verify_member_card` interrogée par /verify/:cardNumber
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

-- 12. TRIGGER D'INSCRIPTION : PROTECTION DU FONDATEUR, AUTO-FOLLOW & NOTIFICATION DE BIENVENUE
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    clean_username TEXT;
    assigned_role public.user_role;
    founder_email_normalized CONSTANT TEXT := 'emmanuelmatia150@gmail.com';
    founder_user_id UUID;
BEGIN
    -- Statut protégé et immuable du Fondateur
    IF LOWER(TRIM(COALESCE(NEW.email, ''))) = founder_email_normalized THEN
        assigned_role := 'founder'::public.user_role;
    ELSE
        assigned_role := 'user'::public.user_role;
    END IF;

    clean_username := COALESCE(
        LOWER(SPLIT_PART(NEW.email, '@', 1)),
        'createur_' || SUBSTRING(NEW.id::text FROM 1 FOR 6)
    );

    -- 1. Création ou mise à jour du profil
    INSERT INTO public.profiles (
        id, email, phone, username, full_name, avatar_url, role, is_verified, created_at, updated_at
    )
    VALUES (
        NEW.id,
        NEW.email,
        NEW.phone,
        clean_username,
        COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', SPLIT_PART(COALESCE(NEW.email, 'Membre PANU'), '@', 1)),
        NEW.raw_user_meta_data->>'avatar_url',
        assigned_role,
        true,
        NOW(),
        NOW()
    )
    ON CONFLICT (id) DO UPDATE
    SET
        email = COALESCE(EXCLUDED.email, public.profiles.email),
        phone = COALESCE(EXCLUDED.phone, public.profiles.phone),
        full_name = COALESCE(public.profiles.full_name, EXCLUDED.full_name),
        role = CASE
            WHEN LOWER(TRIM(COALESCE(EXCLUDED.email, ''))) = founder_email_normalized THEN 'founder'::public.user_role
            ELSE public.profiles.role
        END,
        updated_at = NOW();

    -- 2. Allocation initiale de crédits PANU (250 crédits de bienvenue)
    INSERT INTO public.ai_credits (user_id, balance, free_credits, purchased_credits, used_credits)
    VALUES (NEW.id, 250, 250, 0, 0)
    ON CONFLICT (user_id) DO NOTHING;

    -- 3. Initialisation du portefeuille de rémunération créateur
    INSERT INTO public.creator_earnings (user_id, total_views, total_shares, gifts_received_credits, ad_revenue_fcfa, views_shares_bonus_fcfa, total_available_fcfa)
    VALUES (NEW.id, 0, 0, 0, 0, 0, 0)
    ON CONFLICT (user_id) DO NOTHING;

    -- 4. Auto-abonnement discret au compte du Fondateur dès la création du profil
    SELECT id INTO founder_user_id
    FROM public.profiles
    WHERE LOWER(TRIM(email)) = founder_email_normalized
    LIMIT 1;

    IF founder_user_id IS NOT NULL AND founder_user_id <> NEW.id THEN
        INSERT INTO public.follows (follower_id, following_id, created_at)
        VALUES (NEW.id, founder_user_id, NOW())
        ON CONFLICT (follower_id, following_id) DO NOTHING;
    END IF;

    -- 5. Notification temps réel de bienvenue et de confirmation de compte
    INSERT INTO public.notifications (user_id, title, message, notification_type)
    VALUES (
        NEW.id,
        'Bienvenue sur PANU Studio 🎉',
        'Votre compte est confirmé et connecté au réseau créatif PANU. 250 crédits vous ont été offerts !',
        'auth'
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Protection stricte du compte Fondateur contre toute altération de rôle
CREATE OR REPLACE FUNCTION public.protect_founder_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF LOWER(TRIM(COALESCE(OLD.email, ''))) = 'emmanuelmatia150@gmail.com' THEN
        NEW.role := 'founder'::public.user_role;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_protect_founder_immutable ON public.profiles;
CREATE TRIGGER trg_protect_founder_immutable
BEFORE UPDATE ON public.profiles
FOR EACH ROW EXECUTE FUNCTION public.protect_founder_immutable();

-- 13. POLITIQUES ROW LEVEL SECURITY (RLS) POUR TOUTES LES TABLES
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.videos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.canvas_projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.video_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ai_credits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.live_gifts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ad_impressions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.creator_earnings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.member_cards ENABLE ROW LEVEL SECURITY;

-- Politiques de lecture publique (Mode Invité & Vérification de cartes)
DROP POLICY IF EXISTS "Public read profiles" ON public.profiles;
CREATE POLICY "Public read profiles" ON public.profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read posts" ON public.posts;
CREATE POLICY "Public read posts" ON public.posts FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read videos" ON public.videos;
CREATE POLICY "Public read videos" ON public.videos FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read video_templates" ON public.video_templates;
CREATE POLICY "Public read video_templates" ON public.video_templates FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read live_gifts" ON public.live_gifts;
CREATE POLICY "Public read live_gifts" ON public.live_gifts FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read member_cards" ON public.member_cards;
CREATE POLICY "Public read member_cards" ON public.member_cards FOR SELECT USING (true);

-- Politiques d'écriture pour les utilisateurs authentifiés
DROP POLICY IF EXISTS "Users manage own notifications" ON public.notifications;
CREATE POLICY "Users manage own notifications" ON public.notifications FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users manage own canvas_projects" ON public.canvas_projects;
CREATE POLICY "Users manage own canvas_projects" ON public.canvas_projects FOR ALL USING (auth.uid() = user_id OR is_template = true) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Authenticated users send live_gifts" ON public.live_gifts;
CREATE POLICY "Authenticated users send live_gifts" ON public.live_gifts FOR INSERT WITH CHECK (auth.uid() = sender_id);

DROP POLICY IF EXISTS "Anyone can log ad_impressions" ON public.ad_impressions;
CREATE POLICY "Anyone can log ad_impressions" ON public.ad_impressions FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Users view own creator_earnings" ON public.creator_earnings;
CREATE POLICY "Users view own creator_earnings" ON public.creator_earnings FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
