-- Table bookmarks pour les publications enregistrées en favoris
create table if not exists public.bookmarks (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references auth.users(id) on delete cascade,
  post_id uuid references public.posts(id) on delete cascade,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  constraint unique_user_post_bookmark unique (user_id, post_id)
);

-- Activer RLS
alter table public.bookmarks enable row level security;

-- Politiques RLS
create policy "Users can view their own bookmarks"
  on public.bookmarks for select
  using (auth.uid() = user_id);

create policy "Users can insert their own bookmarks"
  on public.bookmarks for insert
  with check (auth.uid() = user_id);

create policy "Users can delete their own bookmarks"
  on public.bookmarks for delete
  using (auth.uid() = user_id);

-- Publication realtime
alter publication supabase_realtime add table public.bookmarks;
