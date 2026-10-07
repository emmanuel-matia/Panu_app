-- Ajouter la colonne views_count à la table posts
alter table public.posts 
add column if not exists views_count integer default 0;

-- Mise à jour pour incrémentation sécurisée (via fonction RPC)
create or replace function increment_post_views(post_id uuid)
returns void as $$
begin
  update public.posts
  set views_count = views_count + 1
  where id = post_id;
end;
$$ language plpgsql security definer;
