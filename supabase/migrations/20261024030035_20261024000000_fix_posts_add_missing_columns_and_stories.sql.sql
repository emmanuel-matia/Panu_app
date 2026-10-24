/*
# Fix posts table: add user_id, missing columns, and create stories table

## Changes
1. Posts: add user_id column (backfilled from author_id), is_public, likes_count, comments_count, views_count
2. Stories: new table with 24h auto-expiry, RLS, public read for non-expired
3. Security: RLS on stories, grants for anon/authenticated, public read policy on posts
*/

-- 1. Add user_id column to posts (front-end uses user_id, DB has author_id)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'user_id') THEN
    ALTER TABLE posts ADD COLUMN user_id uuid;
    UPDATE posts SET user_id = author_id WHERE user_id IS NULL;
    ALTER TABLE posts ALTER COLUMN user_id SET NOT NULL;
  END IF;
END $$;

-- 2. Add missing columns to posts
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'is_public') THEN
    ALTER TABLE posts ADD COLUMN is_public boolean NOT NULL DEFAULT true;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'likes_count') THEN
    ALTER TABLE posts ADD COLUMN likes_count integer NOT NULL DEFAULT 0;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'comments_count') THEN
    ALTER TABLE posts ADD COLUMN comments_count integer NOT NULL DEFAULT 0;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'posts' AND column_name = 'views_count') THEN
    ALTER TABLE posts ADD COLUMN views_count integer NOT NULL DEFAULT 0;
  END IF;
END $$;

-- 3. Create stories table
CREATE TABLE IF NOT EXISTS stories (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
  media_url text NOT NULL,
  media_type text NOT NULL DEFAULT 'image',
  caption text,
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL DEFAULT (now() + interval '24 hours')
);

ALTER TABLE stories ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON stories TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON stories TO authenticated;

DROP POLICY IF EXISTS "Anyone can view non-expired stories" ON stories;
CREATE POLICY "Anyone can view non-expired stories"
ON stories FOR SELECT
TO anon, authenticated
USING (expires_at > now());

DROP POLICY IF EXISTS "Users can insert own stories" ON stories;
CREATE POLICY "Users can insert own stories"
ON stories FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own stories" ON stories;
CREATE POLICY "Users can update own stories"
ON stories FOR UPDATE
TO authenticated
USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own stories" ON stories;
CREATE POLICY "Users can delete own stories"
ON stories FOR DELETE
TO authenticated
USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_stories_user ON stories (user_id);
CREATE INDEX IF NOT EXISTS idx_stories_expires ON stories (expires_at);

-- 4. Public read policy on posts
DROP POLICY IF EXISTS "Anyone can view public posts" ON posts;
CREATE POLICY "Anyone can view public posts"
ON posts FOR SELECT
TO anon, authenticated
USING (is_public = true);

-- Ensure anon and authenticated have table-level grants
GRANT SELECT ON posts TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON posts TO authenticated;
