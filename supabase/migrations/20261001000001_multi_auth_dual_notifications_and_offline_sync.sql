-- ==============================================================================
-- PANU — AUTHENTIFICATION MULTI-MÉTHODES, NOTIFICATIONS DOUBLES (UTILISATEUR + FONDATEUR)
-- & SYNCHRONISATION HORS-LIGNE (`emmanuelmatia150@gmail.com`)
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.handle_new_user_dual_notifications()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_founder_id UUID;
    v_provider TEXT;
    v_method_label TEXT;
    v_user_display TEXT;
BEGIN
    -- Détection de la méthode d'inscription utilisée (Google, Apple, Facebook, WhatsApp, Téléphone, E-mail)
    v_provider := COALESCE(
        NEW.raw_app_meta_data->>'provider',
        NEW.raw_user_meta_data->>'auth_method',
        CASE WHEN NEW.phone IS NOT NULL AND NEW.phone <> '' THEN 'phone' ELSE 'email' END
    );

    v_method_label := CASE LOWER(v_provider)
        WHEN 'google' THEN 'Google (Gmail)'
        WHEN 'apple' THEN 'Apple ID'
        WHEN 'facebook' THEN 'Facebook'
        WHEN 'whatsapp' THEN 'WhatsApp OTP'
        WHEN 'phone' THEN 'Téléphone / SMS OTP'
        ELSE 'E-mail'
    END;

    v_user_display := COALESCE(
        NEW.raw_user_meta_data->>'full_name',
        NEW.email,
        NEW.phone,
        'Nouveau Membre PANU'
    );

    -- 1. Notification de bienvenue et de confirmation CÔTÉ UTILISATEUR (adaptée à la méthode utilisée)
    INSERT INTO public.notifications (user_id, title, message, notification_type, is_read, created_at)
    VALUES (
        NEW.id,
        'Bienvenue sur PANU • Confirmation ' || v_method_label || ' ✅',
        'Bonjour ' || v_user_display || ' ! Votre compte créé via ' || v_method_label || ' est confirmé. Vos +60 Crédits Gratuits et le Mode Studio Gratuit & Hors-ligne (Style CapCut) sont actifs.',
        'auth_confirmation',
        false,
        NOW()
    );

    -- 2. Alerte instantanée CÔTÉ FONDATEUR (emmanuelmatia150@gmail.com)
    SELECT id INTO v_founder_id
    FROM public.profiles
    WHERE LOWER(email) = 'emmanuelmatia150@gmail.com'
       OR role = 'founder'
    ORDER BY (CASE WHEN LOWER(email) = 'emmanuelmatia150@gmail.com' THEN 0 ELSE 1 END)
    LIMIT 1;

    IF v_founder_id IS NOT NULL AND v_founder_id <> NEW.id THEN
        INSERT INTO public.notifications (user_id, title, message, notification_type, is_read, created_at)
        VALUES (
            v_founder_id,
            '🔔 Nouvel inscrit sur PANU via ' || v_method_label,
            'Nouvel utilisateur inscrit : ' || v_user_display || ' (Méthode : ' || v_method_label || '). Auto-abonnement au compte Fondateur (emmanuelmatia150@gmail.com) effectué.',
            'founder_new_user_alert',
            false,
            NOW()
        );
    END IF;

    RETURN NEW;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created_dual_notifications ON auth.users;
CREATE TRIGGER on_auth_user_created_dual_notifications
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user_dual_notifications();
