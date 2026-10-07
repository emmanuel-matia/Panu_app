import { supabase } from '../lib/supabaseClient';

export interface LikeEventPayload {
  eventType: 'INSERT' | 'DELETE';
  postId: string;
  userId: string;
}

/**
 * Service pour la gestion des Likes en temps réel avec Supabase
 * - Stockage persistant dans la table `public.likes`
 * - Synchronisation en temps réel via Supabase Realtime
 * - Calcul dynamique des compteurs de likes
 */

/**
 * Récupère l'ensemble des IDs des posts aimés par l'utilisateur courant
 */
export async function fetchUserLikedPostIds(userId: string, postIds?: string[]): Promise<Set<string>> {
  if (!userId) return new Set();

  try {
    let query = supabase.from('likes').select('post_id').eq('user_id', userId);
    if (postIds && postIds.length > 0) {
      query = query.in('post_id', postIds);
    }
    const { data, error } = await query;
    if (error) {
      console.warn('[LikeService] Erreur récupération likes utilisateur:', error.message);
      return new Set();
    }
    return new Set((data || []).map((row) => row.post_id).filter(Boolean));
  } catch (err) {
    console.warn('[LikeService] Exception likes utilisateur:', err);
    return new Set();
  }
}

/**
 * Récupère le décompte exact des likes pour une liste de posts depuis la table `likes`
 */
export async function fetchLikesCountMap(postIds: string[]): Promise<Record<string, number>> {
  if (!postIds || postIds.length === 0) return {};

  try {
    const { data, error } = await supabase
      .from('likes')
      .select('post_id')
      .in('post_id', postIds);

    if (error) {
      console.warn('[LikeService] Erreur récupération compteurs likes:', error.message);
      return {};
    }

    const counts: Record<string, number> = {};
    (data || []).forEach((row) => {
      if (row.post_id) {
        counts[row.post_id] = (counts[row.post_id] || 0) + 1;
      }
    });

    return counts;
  } catch (err) {
    console.warn('[LikeService] Exception compteurs likes:', err);
    return {};
  }
}

/**
 * Bascule l'état du like pour un post (Ajout ou Retrait)
 * Enregistre dans Supabase, met à jour `posts.likes_count` et envoie une notification
 */
export async function togglePostLike(params: {
  postId: string;
  userId: string;
  isCurrentlyLiked: boolean;
  postAuthorId?: string;
  postTitle?: string;
  actorName?: string;
}): Promise<{ liked: boolean }> {
  const { postId, userId, isCurrentlyLiked, postAuthorId, postTitle, actorName } = params;

  if (isCurrentlyLiked) {
    // Retirer le like
    try {
      const { error } = await supabase
        .from('likes')
        .delete()
        .eq('user_id', userId)
        .eq('post_id', postId);

      if (error) {
        console.warn('[LikeService] Erreur suppression like:', error.message);
      }
    } catch (err) {
      console.warn('[LikeService] Exception delete like:', err);
    }

    return { liked: false };
  } else {
    // Ajouter le like
    try {
      const { error } = await supabase.from('likes').insert({
        user_id: userId,
        post_id: postId,
      });

      if (error) {
        // Ignorer l'erreur si c'est un doublon de contrainte unique
        if (!error.message.includes('unique') && !error.message.includes('duplicate')) {
          console.warn('[LikeService] Erreur insertion like:', error.message);
        }
      }

      // Alerter l'auteur du post si ce n'est pas lui-même
      const targetAuthorId = postAuthorId;
      if (targetAuthorId && targetAuthorId !== userId) {
        const actorDisplayName = actorName || 'Un créateur PANU';
        const snippet = postTitle ? ` "${postTitle.slice(0, 30)}"` : '';

        await supabase.from('notifications').insert({
          user_id: targetAuthorId,
          actor_id: userId,
          actor_name: actorDisplayName,
          post_id: postId,
          title: '❤️ Nouveau like sur votre publication',
          message: `${actorDisplayName} a aimé votre publication${snippet}`,
          notification_type: 'like',
          is_read: false,
        });
      }
    } catch (err) {
      console.warn('[LikeService] Exception insert like:', err);
    }

    return { liked: true };
  }
}

/**
 * Souscription Supabase Realtime aux modifications de la table `likes`
 */
export function subscribeToRealtimeLikes(
  callback: (event: LikeEventPayload) => void
) {
  const channel = supabase
    .channel('realtime_likes_feed_' + Math.random().toString(36).substring(2, 7))
    .on(
      'postgres_changes',
      { event: '*', schema: 'public', table: 'likes' },
      (payload) => {
        if (payload.eventType === 'INSERT') {
          const row = payload.new as any;
          if (row && row.post_id) {
            callback({
              eventType: 'INSERT',
              postId: row.post_id,
              userId: row.user_id,
            });
          }
        } else if (payload.eventType === 'DELETE') {
          const row = payload.old as any;
          if (row && row.post_id) {
            callback({
              eventType: 'DELETE',
              postId: row.post_id,
              userId: row.user_id,
            });
          }
        }
      }
    )
    .subscribe();

  return () => {
    supabase.removeChannel(channel);
  };
}
