-- ==============================================================================
-- PROJET PANU — MODULE CATEGORIES, PROFILS, WALLETS RLS & CONVERSION MULTI-DEVISES
-- ==============================================================================

-- 0. EXTENSIONS POSTGRESQL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. TYPES ET ENUMS
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_type') THEN
        CREATE TYPE public.account_type AS ENUM ('creator', 'business');
    END IF;
END $$;

-- 2. TABLE DES CATÉGORIES & INSERTION DES 30 CATÉGORIES OFFICIELLES PANU
CREATE TABLE IF NOT EXISTS public.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    slug TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Insertion des 30 catégories spécifiées avec slug généré
INSERT INTO public.categories (name, slug) VALUES
    ('Blog personnel', 'blog-personnel'),
    ('Créateur de contenu', 'createur-de-contenu'),
    ('Entrepreneur & Business', 'entrepreneur-business'),
    ('Comédie & Humour', 'comedie-humour'),
    ('Musique & Artiste', 'musique-artiste'),
    ('Peinture & Arts visuels', 'peinture-arts-visuels'),
    ('Motivation & Développement personnel', 'motivation-developpement-personnel'),
    ('Séries & Films', 'series-films'),
    ('Danse & Chorégraphie', 'danse-choregraphie'),
    ('Créateur de Reels & Shorts', 'createur-de-reels-shorts'),
    ('Mode & Style de vie', 'mode-style-de-vie'),
    ('Beauté & Soins personnels', 'beaute-soins-personnels'),
    ('Cuisine & Gastronomie africaine', 'cuisine-gastronomie-africaine'),
    ('Technologie & Innovation', 'technologie-innovation'),
    ('Éducation & Tutoriels', 'education-tutoriels'),
    ('Sport & Fitness', 'sport-fitness'),
    ('Santé & Bien-être', 'sante-bien-etre'),
    ('Voyages & Découvertes', 'voyages-decouvertes'),
    ('Automobile & Engins', 'automobile-engins'),
    ('Jeux vidéo & Gaming', 'jeux-video-gaming'),
    ('Actualités & Média', 'actualites-media'),
    ('Politique & Débats', 'politique-debats'),
    ('Foi, Religion & Spiritualité', 'foi-religion-spiritualite'),
    ('Immobilier & Architecture', 'immobilier-architecture'),
    ('Finances personnelles & Crypto', 'finances-personnelles-crypto'),
    ('Coiffure & Esthétique', 'coiffure-esthetique'),
    ('Photographie & Vidéaste', 'photographie-videaste'),
    ('Culture & Traditions', 'culture-traditions'),
    ('Événementiel & Organisation', 'evenementiel-organisation'),
    ('Agro-business & Agriculture', 'agro-business-agriculture')
ON CONFLICT (name) DO UPDATE SET slug = EXCLUDED.slug;

-- 3. TABLE DES PROFILS UTILISATEURS
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE,
    full_name TEXT,
    avatar_url TEXT,
    bio TEXT,
    account_type public.account_type NOT NULL DEFAULT 'creator',
    is_admin BOOLEAN NOT NULL DEFAULT false,
    country_code VARCHAR(3) NOT NULL DEFAULT 'CD',
    currency VARCHAR(5) NOT NULL DEFAULT 'CDF',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Rétro-compatibilité : Si la table existait déjà, ajouter les colonnes manquantes
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS account_type public.account_type NOT NULL DEFAULT 'creator';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS country_code VARCHAR(3) NOT NULL DEFAULT 'CD';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS currency VARCHAR(5) NOT NULL DEFAULT 'CDF';

-- Protection permanente du compte Fondateur
UPDATE public.profiles
SET is_admin = true
WHERE LOWER(email) = 'emmanuelmatia150@gmail.com' OR role::text = 'founder';

-- 4. TABLE DE LIAISON 'USER_CATEGORIES' (MAX 4 CATÉGORIES PAR UTILISATEUR)
CREATE TABLE IF NOT EXISTS public.user_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES public.categories(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_category UNIQUE (user_id, category_id)
);

CREATE INDEX IF NOT EXISTS idx_user_categories_user ON public.user_categories(user_id);
CREATE INDEX IF NOT EXISTS idx_user_categories_cat ON public.user_categories(category_id);

-- Fonction de contrôle strict : maximum 4 catégories par profil
CREATE OR REPLACE FUNCTION public.check_user_categories_limit()
RETURNS TRIGGER AS $$
DECLARE
    current_count INT;
BEGIN
    SELECT COUNT(*) INTO current_count
    FROM public.user_categories
    WHERE user_id = NEW.user_id;

    IF current_count >= 4 THEN
        RAISE EXCEPTION 'Limite atteinte : un créateur ou business ne peut sélectionner que 4 catégories au maximum sur PANU (actuellement: %)', current_count;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_user_categories_limit ON public.user_categories;
CREATE TRIGGER trg_user_categories_limit
BEFORE INSERT ON public.user_categories
FOR EACH ROW
EXECUTE FUNCTION public.check_user_categories_limit();

-- 5. PORTEFEUILLE SÉCURISÉ DES GAINS & JETONS (TABLE 'USER_WALLETS')
CREATE TABLE IF NOT EXISTS public.user_wallets (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    tokens_balance BIGINT NOT NULL DEFAULT 0 CHECK (tokens_balance >= 0),
    gift_tokens_earned BIGINT NOT NULL DEFAULT 0 CHECK (gift_tokens_earned >= 0),
    total_withdrawn_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00 CHECK (total_withdrawn_amount >= 0),
    currency VARCHAR(5) NOT NULL DEFAULT 'CDF',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Trigger pour initialiser automatiquement le portefeuille à la création du profil
CREATE OR REPLACE FUNCTION public.handle_profile_wallet_init()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_wallets (user_id, tokens_balance, gift_tokens_earned, currency)
    VALUES (NEW.id, 0, 0, COALESCE(NEW.currency, 'CDF'))
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_init_user_wallet ON public.profiles;
CREATE TRIGGER trg_init_user_wallet
AFTER INSERT ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.handle_profile_wallet_init();

-- 6. SÉCURITÉ ROW LEVEL SECURITY (RLS) RENFORCÉE

-- Activation RLS
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_wallets ENABLE ROW LEVEL SECURITY;

-- Helper SQL vérifiant si l'utilisateur connecté est le Fondateur / Admin
CREATE OR REPLACE FUNCTION public.is_admin_user()
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
    SELECT COALESCE(
        (SELECT is_admin FROM public.profiles WHERE id = auth.uid()),
        (LOWER(COALESCE(auth.jwt()->>'email', '')) = 'emmanuelmatia150@gmail.com')
    );
$$;

-- RLS Categories : Tout le monde peut lire les catégories, seul l'admin peut modifier
DROP POLICY IF EXISTS "categories_read_all" ON public.categories;
CREATE POLICY "categories_read_all" ON public.categories FOR SELECT USING (true);

DROP POLICY IF EXISTS "categories_admin_all" ON public.categories;
CREATE POLICY "categories_admin_all" ON public.categories FOR ALL USING (public.is_admin_user());

-- RLS User Categories : Lecture publique, modification strictement par l'utilisateur propriétaire
DROP POLICY IF EXISTS "user_categories_select" ON public.user_categories;
CREATE POLICY "user_categories_select" ON public.user_categories FOR SELECT USING (true);

DROP POLICY IF EXISTS "user_categories_insert" ON public.user_categories;
CREATE POLICY "user_categories_insert" ON public.user_categories FOR INSERT TO authenticated 
WITH CHECK (auth.uid() = user_id OR public.is_admin_user());

DROP POLICY IF EXISTS "user_categories_delete" ON public.user_categories;
CREATE POLICY "user_categories_delete" ON public.user_categories FOR DELETE TO authenticated 
USING (auth.uid() = user_id OR public.is_admin_user());

-- RLS Profiles : Lecture publique, modification propriétaire ou admin
DROP POLICY IF EXISTS "profiles_select_public" ON public.profiles;
CREATE POLICY "profiles_select_public" ON public.profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "profiles_update_owner" ON public.profiles;
CREATE POLICY "profiles_update_owner" ON public.profiles FOR UPDATE TO authenticated 
USING (auth.uid() = id OR public.is_admin_user())
WITH CHECK (auth.uid() = id OR public.is_admin_user());

-- RLS Wallets : RÈGLE STRICTE -> L'utilisateur ne voit QUE son propre portefeuille, le Fondateur voit tout
DROP POLICY IF EXISTS "wallets_owner_or_admin_select" ON public.user_wallets;
CREATE POLICY "wallets_owner_or_admin_select" ON public.user_wallets FOR SELECT TO authenticated
USING (auth.uid() = user_id OR public.is_admin_user());

DROP POLICY IF EXISTS "wallets_owner_or_admin_update" ON public.user_wallets;
CREATE POLICY "wallets_owner_or_admin_update" ON public.user_wallets FOR UPDATE TO authenticated
USING (auth.uid() = user_id OR public.is_admin_user())
WITH CHECK (auth.uid() = user_id OR public.is_admin_user());

-- 7. FONCTION DE CONVERSION DES CADEAUX / JETONS EN ARGENT RÉEL (MULTI-DEVISES)
-- Taux de base unitaire : 1 Jeton Cadeau PANU = 0.01 USD (valeur de référence internationale)
-- Taux appliqués :
-- - CDF (Franc Congolais) : 1 USD = 2850 CDF -> 1 Jeton = 28.50 CDF
-- - XOF / XAF (Franc CFA) : 1 USD = 610 FCFA  -> 1 Jeton = 6.10 FCFA
-- - EUR (Euro)            : 1 USD = 0.92 EUR  -> 1 Jeton = 0.0092 EUR
-- - USD (Dollar US)       : 1 USD = 1.00 USD  -> 1 Jeton = 0.0100 USD

CREATE OR REPLACE FUNCTION public.convert_tokens_to_cash(
    p_tokens BIGINT,
    p_target_currency TEXT DEFAULT NULL,
    p_user_id UUID DEFAULT auth.uid()
)
RETURNS TABLE (
    tokens_count BIGINT,
    currency VARCHAR(5),
    unit_rate NUMERIC(12, 4),
    gross_amount NUMERIC(14, 2),
    platform_commission_percent NUMERIC(5, 2),
    net_creator_amount NUMERIC(14, 2),
    formatted_label TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_currency VARCHAR(5);
    v_rate NUMERIC(12, 4);
    v_gross NUMERIC(14, 2);
    v_commission_percent CONSTANT NUMERIC(5, 2) := 20.00; -- Commission standard plateforme 20%
    v_net NUMERIC(14, 2);
    v_symbol TEXT;
BEGIN
    -- 1. Résolution de la devise cible (paramètre explicite, sinon devise du profil utilisateur)
    IF p_target_currency IS NOT NULL AND TRIM(p_target_currency) <> '' THEN
        v_currency := UPPER(TRIM(p_target_currency));
    ELSE
        SELECT COALESCE(p.currency, 'CDF') INTO v_currency
        FROM public.profiles p
        WHERE p.id = p_user_id;

        IF v_currency IS NULL THEN
            v_currency := 'CDF';
        END IF;
    END IF;

    -- 2. Sélection du taux de conversion officiel selon la devise
    CASE v_currency
        WHEN 'CDF' THEN
            v_rate := 28.5000; -- 1 jeton = 28.5 Francs Congolais
            v_symbol := 'FC';
        WHEN 'XOF' THEN
            v_rate := 6.1000;  -- 1 jeton = 6.10 Francs CFA Ouest
            v_symbol := 'FCFA';
        WHEN 'XAF' THEN
            v_rate := 6.1000;  -- 1 jeton = 6.10 Francs CFA Centrale
            v_symbol := 'FCFA';
        WHEN 'EUR' THEN
            v_rate := 0.0092;  -- 1 jeton = ~0.0092 €
            v_symbol := '€';
        ELSE
            v_currency := 'USD';
            v_rate := 0.0100;  -- 1 jeton = 0.01 $
            v_symbol := '$';
    END CASE;

    -- 3. Calculs financiers
    v_gross := ROUND((p_tokens * v_rate)::NUMERIC, 2);
    v_net := ROUND((v_gross * (1.00 - (v_commission_percent / 100.00)))::NUMERIC, 2);

    -- 4. Retour des résultats
    RETURN QUERY
    SELECT
        p_tokens,
        v_currency,
        v_rate,
        v_gross,
        v_commission_percent,
        v_net,
        v_net || ' ' || v_symbol || ' (Net Créateur après ' || v_commission_percent || '% commission)';
END;
$$;

GRANT EXECUTE ON FUNCTION public.convert_tokens_to_cash(BIGINT, TEXT, UUID) TO authenticated, anon;
