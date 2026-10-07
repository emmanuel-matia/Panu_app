-- ==============================================================================
-- PANU — MODULE 1 & 2 : AUTO-ABONNEMENT FONDATEUR, ISOLATION RLS & TRANSACTIONS LIVE
-- Compatible avec le schéma existant (auth.users, public.profiles, public.follows, public.live_gifts)
-- ==============================================================================

-- 1. SCHÉMA PRIVÉ POUR ISOLER LES RÔLES PRIVILÉGIÉS (SÉPARATION STRICTE ADMIN/FONDATEUR)
-- Empêche toute exposition ou manipulation du statut fondateur depuis le client (PostgREST)
CREATE SCHEMA IF NOT EXISTS private;
REVOKE ALL ON SCHEMA private FROM PUBLIC, anon, authenticated;
GRANT USAGE ON SCHEMA private TO postgres, service_role;

-- Fonction sécurisée (SECURITY DEFINER) vérifiant le rôle Fondateur/Admin côté serveur uniquement
CREATE OR REPLACE FUNCTION public.is_founder_or_admin(check_user_id UUID DEFAULT auth.uid())
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_role TEXT;
    v_email TEXT;
BEGIN
    IF check_user_id IS NULL THEN
        RETURN FALSE;
    END IF;

    SELECT role::TEXT, LOWER(TRIM(email))
    INTO v_role, v_email
    FROM public.profiles
    WHERE id = check_user_id
    LIMIT 1;

    RETURN (v_role IN ('founder', 'admin') OR v_email = 'emmanuelmatia150@gmail.com');
END;
$$;

REVOKE ALL ON FUNCTION public.is_founder_or_admin(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_founder_or_admin(UUID) TO authenticated, service_role;

-- 2. FONCTION & TRIGGER D'AUTO-ABONNEMENT AU COMPTE FONDATEUR DÈS L'INSCRIPTION
-- S'exécute automatiquement après création d'un profil ou d'un utilisateur sans jamais bloquer l'inscription
CREATE OR REPLACE FUNCTION public.handle_auto_follow_founder()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    -- Possibilité de fixer l'UUID exact du fondateur ou de le résoudre dynamiquement via son email/rôle officiel
    v_founder_id UUID;
    v_founder_email CONSTANT TEXT := 'emmanuelmatia150@gmail.com';
BEGIN
    -- Résolution sécurisée de l'ID_FONDATEUR officiel (aucun abonné fictif)
    SELECT id INTO v_founder_id
    FROM public.profiles
    WHERE LOWER(TRIM(email)) = v_founder_email
       OR role::TEXT = 'founder'
    ORDER BY created_at ASC
    LIMIT 1;

    -- Auto-abonnement immédiat si le compte fondateur existe et n'est pas l'utilisateur lui-même
    IF v_founder_id IS NOT NULL AND NEW.id <> v_founder_id THEN
        INSERT INTO public.follows (follower_id, following_id, created_at)
        VALUES (NEW.id, v_founder_id, NOW())
        ON CONFLICT (follower_id, following_id) DO NOTHING;
    END IF;

    RETURN NEW;
EXCEPTION
    WHEN OTHERS THEN
        -- Garantit que toute erreur éventuelle sur follows ne bloque JAMAIS l'inscription
        RAISE WARNING '[PANU Auto-Follow] Ignoré pour user % : %', NEW.id, SQLERRM;
        RETURN NEW;
END;
$$;

-- Attachement du Trigger sur public.profiles (déclenché dès qu'un profil est inséré par auth.users)
DROP TRIGGER IF EXISTS trg_auto_follow_founder_on_signup ON public.profiles;
CREATE TRIGGER trg_auto_follow_founder_on_signup
AFTER INSERT ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.handle_auto_follow_founder();

-- 3. VERROUILLAGE RLS STRICT : PROTECTION DU RÔLE FONDATEUR/ADMIN SUR PUBLIC.PROFILES
CREATE OR REPLACE FUNCTION public.enforce_profile_role_immutability()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    -- Lors d'une insertion par le client, forcer 'user' sauf pour l'email officiel du fondateur
    IF TG_OP = 'INSERT' THEN
        IF LOWER(TRIM(COALESCE(NEW.email, ''))) = 'emmanuelmatia150@gmail.com' THEN
            NEW.role := 'founder'::user_role;
        ELSE
            NEW.role := 'user'::user_role;
        END IF;
        RETURN NEW;
    END IF;

    -- Lors d'une mise à jour, interdire toute modification de la colonne 'role' ou de l'email du fondateur
    IF TG_OP = 'UPDATE' THEN
        IF OLD.role::TEXT = 'founder' AND NEW.role::TEXT <> 'founder' THEN
            RAISE EXCEPTION 'Opération interdite : Le statut du compte Fondateur est immuable.';
        END IF;

        IF NEW.role IS DISTINCT FROM OLD.role AND current_setting('role', true) <> 'service_role' THEN
            IF NOT public.is_founder_or_admin(auth.uid()) THEN
                RAISE EXCEPTION 'Accès refusé : Modification de rôle non autorisée depuis le client.';
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_profile_role_immutability ON public.profiles;
CREATE TRIGGER trg_enforce_profile_role_immutability
BEFORE INSERT OR UPDATE ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.enforce_profile_role_immutability();

-- Politiques RLS sur public.follows : empêcher la suppression forcée de l'abonnement par un tiers
ALTER TABLE public.follows ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Follows lecture publique" ON public.follows;
CREATE POLICY "Follows lecture publique"
ON public.follows FOR SELECT
USING (true);

DROP POLICY IF EXISTS "Follows insertion authentifiee" ON public.follows;
CREATE POLICY "Follows insertion authentifiee"
ON public.follows FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = follower_id);

-- 4. RPC ATOMIQUE POUR LES CADEAUX EN DIRECT (TRANSACTIONS DE JETONS TEMPS RÉEL)
CREATE OR REPLACE FUNCTION public.send_live_gift_transaction(
    p_stream_id TEXT,
    p_receiver_id UUID,
    p_receiver_name TEXT,
    p_gift_type TEXT,
    p_gift_icon TEXT,
    p_credits_cost INTEGER,
    p_message TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_sender_id UUID := auth.uid();
    v_sender_name TEXT;
    v_current_balance INTEGER;
    v_new_balance INTEGER;
    v_gift_id UUID;
BEGIN
    IF v_sender_id IS NULL THEN
        RAISE EXCEPTION 'Authentification requise pour offrir un cadeau en direct.';
    END IF;

    IF p_credits_cost <= 0 THEN
        RAISE EXCEPTION 'Montant de jetons invalide.';
    END IF;

    -- Verrouillage de la ligne de crédits de l'expéditeur (FOR UPDATE évite les doubles dépenses)
    SELECT balance INTO v_current_balance
    FROM public.ai_credits
    WHERE user_id = v_sender_id
    FOR UPDATE;

    IF v_current_balance IS NULL OR v_current_balance < p_credits_cost THEN
        RAISE EXCEPTION 'Solde de jetons PANU insuffisant (% disponibles, % requis).', COALESCE(v_current_balance, 0), p_credits_cost;
    END IF;

    -- Récupération du nom de l'expéditeur
    SELECT COALESCE(full_name, username, 'Membre PANU')
    INTO v_sender_name
    FROM public.profiles
    WHERE id = v_sender_id;

    -- 1. Débit atomique du portefeuille de l'expéditeur
    UPDATE public.ai_credits
    SET balance = balance - p_credits_cost,
        used_credits = used_credits + p_credits_cost,
        updated_at = NOW()
    WHERE user_id = v_sender_id
    RETURNING balance INTO v_new_balance;

    -- 2. Crédit du portefeuille créateur (si p_receiver_id est fourni)
    IF p_receiver_id IS NOT NULL THEN
        INSERT INTO public.creator_earnings (user_id, gifts_received_credits, total_available_fcfa, updated_at)
        VALUES (p_receiver_id, p_credits_cost, p_credits_cost * 10, NOW())
        ON CONFLICT (user_id) DO UPDATE
        SET gifts_received_credits = public.creator_earnings.gifts_received_credits + EXCLUDED.gifts_received_credits,
            total_available_fcfa = public.creator_earnings.total_available_fcfa + (p_credits_cost * 10),
            updated_at = NOW();
    END IF;

    -- 3. Enregistrement dans public.live_gifts (déclenche l'événement Supabase Realtime pour tous les spectateurs)
    INSERT INTO public.live_gifts (
        stream_id,
        sender_id,
        sender_name,
        receiver_name,
        gift_type,
        gift_icon,
        credits_amount,
        message
    )
    VALUES (
        p_stream_id,
        v_sender_id,
        COALESCE(v_sender_name, 'Membre PANU'),
        p_receiver_name,
        p_gift_type,
        p_gift_icon,
        p_credits_cost,
        p_message
    )
    RETURNING id INTO v_gift_id;

    RETURN jsonb_build_object(
        'success', true,
        'gift_id', v_gift_id,
        'remaining_balance', v_new_balance,
        'sender_name', COALESCE(v_sender_name, 'Membre PANU')
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.send_live_gift_transaction(TEXT, UUID, TEXT, TEXT, TEXT, INTEGER, TEXT) TO authenticated;
