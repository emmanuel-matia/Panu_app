-- ==============================================================================
-- PANU — POLITIQUES RLS LECTURE PUBLIQUE POUR POSTS, CREATIONS ET LIVES
-- ==============================================================================

-- 1. TABLE CREATIONS (STUDIO & TEMPLATES CRÉATIFS)
CREATE TABLE IF NOT EXISTS public.creations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    caption TEXT,
    media_url TEXT,
    media_type TEXT NOT NULL DEFAULT 'image',
    category TEXT DEFAULT 'studio',
    status TEXT NOT NULL DEFAULT 'published',
    visibility TEXT NOT NULL DEFAULT 'public',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Profil public par défaut pour publications anonymes ou sans session
INSERT INTO public.profiles (id, email, username, full_name, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'guest@panu.app', 'createur_panu', 'Créateur PANU', 'creator')
ON CONFLICT (id) DO NOTHING;

-- 2. POLITIQUES RLS SUR POSTS
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read posts" ON public.posts;
DROP POLICY IF EXISTS "Published public posts are viewable by everyone" ON public.posts;
CREATE POLICY "Public read posts"
ON public.posts FOR SELECT
USING (true);

DROP POLICY IF EXISTS "Users can insert their own posts" ON public.posts;
DROP POLICY IF EXISTS "Posts insertion" ON public.posts;
CREATE POLICY "Users can insert their own posts"
ON public.posts FOR INSERT
WITH CHECK (true);

DROP POLICY IF EXISTS "Users can update their own posts" ON public.posts;
CREATE POLICY "Users can update their own posts"
ON public.posts FOR UPDATE
USING (true);

-- 3. POLITIQUES RLS SUR CREATIONS
ALTER TABLE public.creations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read creations" ON public.creations;
CREATE POLICY "Public read creations"
ON public.creations FOR SELECT
USING (true);

DROP POLICY IF EXISTS "Users can insert their own creations" ON public.creations;
CREATE POLICY "Users can insert their own creations"
ON public.creations FOR INSERT
WITH CHECK (true);

-- 4. POLITIQUES RLS SUR LIVES
ALTER TABLE public.lives ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Lives lecture publique" ON public.lives;
CREATE POLICY "Lives lecture publique"
ON public.lives FOR SELECT
USING (true);

DROP POLICY IF EXISTS "Lives insertion libre ou authentifiee" ON public.lives;
CREATE POLICY "Lives insertion libre ou authentifiee"
ON public.lives FOR INSERT
WITH CHECK (true);

DROP POLICY IF EXISTS "Lives mise a jour" ON public.lives;
CREATE POLICY "Lives mise a jour"
ON public.lives FOR UPDATE
USING (true);

-- 5. CONFIGURATION DU BUCKET STORAGE post-media
INSERT INTO storage.buckets (id, name, public)
VALUES ('post-media', 'post-media', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- 6. PUBLICATION REALTIME POUR POSTS, CREATIONS ET LIVES
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.posts;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.creations;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.lives;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;
