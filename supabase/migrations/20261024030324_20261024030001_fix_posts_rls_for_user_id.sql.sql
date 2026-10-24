/*
# Fix posts RLS policies to support user_id column

## Problem
Existing INSERT/UPDATE/DELETE policies check `auth.uid() = author_id`,
but the frontend now inserts with `user_id` instead of `author_id`.
This causes INSERT failures because the WITH CHECK fails.

## Changes
1. Add INSERT policy that accepts `auth.uid() = user_id`
2. Add UPDATE policy that accepts `auth.uid() = user_id`
3. Add DELETE policy that accepts `auth.uid() = user_id`
4. Keep existing policies (they still work for author_id)
5. Grant INSERT/UPDATE/DELETE to authenticated (already granted, but ensure)
*/

GRANT INSERT, UPDATE, DELETE ON posts TO authenticated;

DROP POLICY IF EXISTS "Users can insert posts with user_id" ON posts;
CREATE POLICY "Users can insert posts with user_id"
ON posts FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update posts with user_id" ON posts;
CREATE POLICY "Users can update posts with user_id"
ON posts FOR UPDATE
TO authenticated
USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete posts with user_id" ON posts;
CREATE POLICY "Users can delete posts with user_id"
ON posts FOR DELETE
TO authenticated
USING (auth.uid() = user_id);
