-- ==============================================================================
-- PANU — MODULE FLUX EN DIRECT 100 % RÉELS & STORAGE (LIVEKIT WEBRTC & REALTIME)
-- ==============================================================================

-- 1. TABLE LIVES (DIFFUSIONS WEBRTC RÉELLES PAR CAMÉRA DU TÉLÉPHONE)
CREATE TABLE IF NOT EXISTS public.lives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    host_name TEXT NOT NULL,
    host_avatar TEXT,
    title TEXT NOT NULL,
    description TEXT,
    room_name TEXT NOT NULL UNIQUE,
    category TEXT NOT NULL DEFAULT 'interactive' CHECK (category IN ('football', 'interactive', 'cinema', 'masterclass')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'ended')),
    viewers_count INTEGER NOT NULL DEFAULT 1,
    is_vip BOOLEAN NOT NULL DEFAULT false,
    price_fc INTEGER NOT NULL DEFAULT 0,
    price_usd NUMERIC NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lives_status ON public.lives(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_lives_host ON public.lives(host_id);

-- RLS sur public.lives
ALTER TABLE public.lives ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Lives lecture publique" ON public.lives;
CREATE POLICY "Lives lecture publique"
ON public.lives FOR SELECT
USING (true);

DROP POLICY IF EXISTS "Lives insertion authentifiee" ON public.lives;
CREATE POLICY "Lives insertion authentifiee"
ON public.lives FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = host_id);

DROP POLICY IF EXISTS "Lives mise a jour par l'hote" ON public.lives;
CREATE POLICY "Lives mise a jour par l'hote"
ON public.lives FOR UPDATE
TO authenticated
USING (auth.uid() = host_id OR public.is_founder_or_admin(auth.uid()))
WITH CHECK (auth.uid() = host_id OR public.is_founder_or_admin(auth.uid()));

DROP POLICY IF EXISTS "Lives suppression par l'hote" ON public.lives;
CREATE POLICY "Lives suppression par l'hote"
ON public.lives FOR DELETE
TO authenticated
USING (auth.uid() = host_id OR public.is_founder_or_admin(auth.uid()));

-- Publication Realtime pour public.lives
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.lives;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;
