-- ==============================================================================
-- MIGRATION SUPABASE : GESTION DES LIKES EN TEMPS RÉEL & SYSTÈME DE NOTIFICATIONS
-- 1. Table `public.likes` avec contrainte unique (user_id, post_id)
-- 2. Table `public.comments` pour les commentaires réels
-- 3. Table `public.notifications` pour les alertes de likes et commentaires
-- 4. Publication Realtime pour synchronisation instantanée sans rechargement
-- 5. Triggers d'incrémentation automatique des compteurs et de notification
-- ==============================================================================

-- 1. COLONNES DE COMPTEURS DANS `posts`
ALTER TABLE IF EXISTS public.posts ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE IF EXISTS public.posts ADD COLUMN IF NOT EXISTS author_id UUID;
ALTER TABLE IF EXISTS public.posts ADD COLUMN IF NOT EXISTS likes_count BIGINT DEFAULT 0;
ALTER TABLE IF EXISTS public.posts ADD COLUMN IF NOT EXISTS comments_count BIGINT DEFAULT 0;
ALTER TABLE IF EXISTS public.posts ADD COLUMN IF NOT EXISTS is_public BOOLEAN DEFAULT true;

-- Synchronisation des identifiants d'auteurs
UPDATE public.posts SET user_id = author_id WHERE user_id IS NULL AND author_id IS NOT NULL;
UPDATE public.posts SET author_id = user_id WHERE author_id IS NULL AND user_id IS NOT NULL;

-- 2. TABLE `likes`
CREATE TABLE IF NOT EXISTS public.likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_id UUID REFERENCES public.posts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_likes_user_post ON public.likes(user_id, post_id);
CREATE INDEX IF NOT EXISTS idx_likes_post_id ON public.likes(post_id);
CREATE INDEX IF NOT EXISTS idx_likes_user_id ON public.likes(user_id);

-- 3. TABLE `comments`
CREATE TABLE IF NOT EXISTS public.comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_id UUID REFERENCES public.posts(id) ON DELETE CASCADE,
    author_name TEXT,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comments_post_id ON public.comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON public.comments(user_id);

-- 4. TABLE `notifications`
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    actor_id UUID,
    actor_name TEXT,
    post_id UUID,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    notification_type TEXT NOT NULL DEFAULT 'like',
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON public.notifications(user_id, is_read) WHERE is_read = false;

-- 5. POLITIQUES RLS
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tout le monde peut voir les likes" ON public.likes;
CREATE POLICY "Tout le monde peut voir les likes" ON public.likes FOR SELECT USING (true);
DROP POLICY IF EXISTS "Tout le monde peut liker" ON public.likes;
CREATE POLICY "Tout le monde peut liker" ON public.likes FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "L'utilisateur peut retirer son like" ON public.likes;
CREATE POLICY "L'utilisateur peut retirer son like" ON public.likes FOR DELETE USING (true);

ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tout le monde peut voir les commentaires" ON public.comments;
CREATE POLICY "Tout le monde peut voir les commentaires" ON public.comments FOR SELECT USING (true);
DROP POLICY IF EXISTS "Tout le monde peut commenter" ON public.comments;
CREATE POLICY "Tout le monde peut commenter" ON public.comments FOR INSERT WITH CHECK (true);

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Lecture des notifications" ON public.notifications;
CREATE POLICY "Lecture des notifications" ON public.notifications FOR SELECT USING (true);
DROP POLICY IF EXISTS "Insertion des notifications" ON public.notifications;
CREATE POLICY "Insertion des notifications" ON public.notifications FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Mise a jour des notifications" ON public.notifications;
CREATE POLICY "Mise a jour des notifications" ON public.notifications FOR UPDATE USING (true);

-- 6. TRIGGERS POUR LA GESTION AUTOMATIQUE DES COMPTEURS DE LIKES ET COMMENTAIRES
CREATE OR REPLACE FUNCTION public.sync_likes_count()
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.posts SET likes_count = COALESCE(likes_count, 0) + 1 WHERE id = NEW.post_id;
    RETURN NEW;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.posts SET likes_count = GREATEST(COALESCE(likes_count, 1) - 1, 0) WHERE id = OLD.post_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_sync_likes_count ON public.likes;
CREATE TRIGGER trigger_sync_likes_count
AFTER INSERT OR DELETE ON public.likes
FOR EACH ROW EXECUTE FUNCTION public.sync_likes_count();

CREATE OR REPLACE FUNCTION public.sync_comments_count()
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.posts SET comments_count = COALESCE(comments_count, 0) + 1 WHERE id = NEW.post_id;
    RETURN NEW;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.posts SET comments_count = GREATEST(COALESCE(comments_count, 1) - 1, 0) WHERE id = OLD.post_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_sync_comments_count ON public.comments;
CREATE TRIGGER trigger_sync_comments_count
AFTER INSERT OR DELETE ON public.comments
FOR EACH ROW EXECUTE FUNCTION public.sync_comments_count();

-- 7. TRIGGER AUTOMATIQUE POUR CRÉER UNE NOTIFICATION SUR LIKE OU COMMENTAIRE
CREATE OR REPLACE FUNCTION public.trigger_post_interaction_notification()
RETURNS TRIGGER AS $$
DECLARE
  target_user_id UUID;
  post_title_str TEXT;
  actor_label TEXT;
BEGIN
  IF (TG_OP = 'INSERT') THEN
    SELECT COALESCE(user_id, author_id), title INTO target_user_id, post_title_str
    FROM public.posts
    WHERE id = NEW.post_id;

    -- Ne pas notifier l'auteur de sa propre action
    IF target_user_id IS NOT NULL AND target_user_id <> NEW.user_id THEN
      SELECT COALESCE(full_name, username, 'Un créateur') INTO actor_label
      FROM public.profiles
      WHERE id = NEW.user_id;

      IF actor_label IS NULL THEN
        actor_label := COALESCE(NEW.author_name, 'Un utilisateur');
      END IF;

      IF (TG_TABLE_NAME = 'likes') THEN
        INSERT INTO public.notifications (user_id, actor_id, actor_name, post_id, title, message, notification_type, is_read)
        VALUES (
          target_user_id,
          NEW.user_id,
          actor_label,
          NEW.post_id,
          '❤️ Nouveau like',
          actor_label || ' a aimé votre publication' || CASE WHEN post_title_str IS NOT NULL THEN ' "' || SUBSTRING(post_title_str FROM 1 FOR 30) || '"' ELSE '' END,
          'like',
          false
        );
      ELSIF (TG_TABLE_NAME = 'comments') THEN
        INSERT INTO public.notifications (user_id, actor_id, actor_name, post_id, title, message, notification_type, is_read)
        VALUES (
          target_user_id,
          NEW.user_id,
          actor_label,
          NEW.post_id,
          '💬 Nouveau commentaire',
          actor_label || ' a commenté : "' || SUBSTRING(NEW.content FROM 1 FOR 40) || '"',
          'comment',
          false
        );
      END IF;
    END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_on_like ON public.likes;
CREATE TRIGGER trigger_notify_on_like
AFTER INSERT ON public.likes
FOR EACH ROW EXECUTE FUNCTION public.trigger_post_interaction_notification();

DROP TRIGGER IF EXISTS trigger_notify_on_comment ON public.comments;
CREATE TRIGGER trigger_notify_on_comment
AFTER INSERT ON public.comments
FOR EACH ROW EXECUTE FUNCTION public.trigger_post_interaction_notification();

-- 8. PUBLICATION SUPABASE REALTIME
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.likes;
EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.comments;
EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.posts;
EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL;
END $$;
