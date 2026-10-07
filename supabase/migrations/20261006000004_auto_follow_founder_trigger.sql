-- ==============================================================================
-- PANU — AUTO-ABONNEMENT AU COMPTE FONDATEUR (EMMANUEL MATIA)
-- Trigger SQL pour garantir l'abonnement automatique dès la création du profil
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.auto_follow_founder()
RETURNS TRIGGER AS $$
DECLARE
    founder_uuid UUID;
BEGIN
    -- Recherche de l'ID du compte fondateur par son pseudo (Emmanuel MATIA)
    SELECT id INTO founder_uuid 
    FROM public.profiles 
    WHERE username = 'Emmanuel MATIA' 
       OR full_name = 'Emmanuel MATIA'
       OR email = 'emmanuelmatia150@gmail.com'
    ORDER BY created_at ASC
    LIMIT 1;
        
    -- Si le compte fondateur existe et n'est pas le nouvel utilisateur lui-même
    IF founder_uuid IS NOT NULL AND founder_uuid != NEW.id THEN
        INSERT INTO public.follows (follower_id, following_id, created_at)
        VALUES (NEW.id, founder_uuid, NOW())
        ON CONFLICT (follower_id, following_id) DO NOTHING;
    END IF;
        
    RETURN NEW;
EXCEPTION
    WHEN OTHERS THEN
        -- Garantit que toute erreur sur l'auto-follow ne bloque JAMAIS la création du profil
        RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Déclencheur après la création d'un profil
DROP TRIGGER IF EXISTS on_profile_created_follow_founder ON public.profiles;
CREATE TRIGGER on_profile_created_follow_founder
AFTER INSERT ON public.profiles
FOR EACH ROW EXECUTE FUNCTION public.auto_follow_founder();

-- Nettoyage de l'ancien trigger s'il existe
DROP TRIGGER IF EXISTS trg_auto_follow_founder_on_signup ON public.profiles;
