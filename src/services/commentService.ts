import { supabase } from '../lib/supabaseClient';

export interface CommentItem {
  id: string;
  postId: string;
  userId: string;
  authorName: string;
  text: string;
  createdAt: string;
}

/**
 * Service pour la gestion des Commentaires en temps réel avec Supabase
 * - Table `public.comments`
 * - Synchronisation Realtime
 * - Création automatique de notifications pour l'auteur du post
 */

/**
 * Récupère les commentaires d'une liste de posts ou d'un post donné
 */
export async function fetchCommentsForPosts(postIds: string[]): Promise<Record<string, CommentItem[]>> {
  if (!postIds || postIds.length === 0) return {};

  try {
    const { data, error } = await supabase
      .from('comments')
      .select('*')
      .in('post_id', postIds)
      .order('created_at', { ascending: true });

    if (error) {
      console.warn('[CommentService] Erreur fetch comments:', error.message);
      return {};
    }

    const map: Record<string, CommentItem[]> = {};
    (data || []).forEach((row: any) => {
      const formatted: CommentItem = {
        id: row.id,
        postId: row.post_id,
        userId: row.user_id,
        authorName: row.author_name || 'Créateur PANU',
        text: row.content,
        createdAt: row.created_at ? formatRelativeTime(row.created_at) : 'À l’instant',
      };
      if (!map[row.post_id]) {
        map[row.post_id] = [];
      }
      map[row.post_id].push(formatted);
    });

    return map;
  } catch (err) {
    console.warn('[CommentService] Exception fetch comments:', err);
    return {};
  }
}

/**
 * Ajoute un nouveau commentaire dans `public.comments`
 * et alerte l'auteur de la publication par notification temps réel
 */
export async function addPostComment(params: {
  postId: string;
  userId: string;
  content: string;
  authorName?: string;
  postAuthorId?: string;
  postTitle?: string;
}): Promise<CommentItem | null> {
  const { postId, userId, content, authorName, postAuthorId, postTitle } = params;
  const cleanText = content.trim();
  if (!cleanText) return null;

  const displayName = authorName || 'Vous';

  try {
    const { data, error } = await supabase
      .from('comments')
      .insert({
        post_id: postId,
        user_id: userId,
        content: cleanText,
        author_name: displayName,
      })
      .select()
      .single();

    if (error) {
      console.warn('[CommentService] Erreur insertion commentaire:', error.message);
      // Fallback local
      return {
        id: `comm_${Date.now()}`,
        postId,
        userId,
        authorName: displayName,
        text: cleanText,
        createdAt: 'À l’instant',
      };
    }

    // Alerter l'auteur du post si ce n'est pas lui-même
    if (postAuthorId && postAuthorId !== userId) {
      const snippet = postTitle ? ` sur "${postTitle.slice(0, 25)}"` : '';
      await supabase.from('notifications').insert({
        user_id: postAuthorId,
        actor_id: userId,
        actor_name: displayName,
        post_id: postId,
        title: '💬 Nouveau commentaire',
        message: `${displayName} a commenté${snippet} : "${cleanText.slice(0, 45)}"`,
        notification_type: 'comment',
        is_read: false,
      });
    }

    return {
      id: data.id,
      postId: data.post_id,
      userId: data.user_id,
      authorName: data.author_name || displayName,
      text: data.content,
      createdAt: 'À l’instant',
    };
  } catch (err) {
    console.warn('[CommentService] Exception add comment:', err);
    return {
      id: `comm_${Date.now()}`,
      postId,
      userId,
      authorName: displayName,
      text: cleanText,
      createdAt: 'À l’instant',
    };
  }
}

/**
 * Souscription Supabase Realtime aux nouveaux commentaires
 */
export function subscribeToRealtimeComments(
  callback: (comment: CommentItem) => void
) {
  const channel = supabase
    .channel('realtime_comments_feed_' + Math.random().toString(36).substring(2, 7))
    .on(
      'postgres_changes',
      { event: 'INSERT', schema: 'public', table: 'comments' },
      (payload) => {
        const row = payload.new as any;
        if (row && row.post_id) {
          callback({
            id: row.id,
            postId: row.post_id,
            userId: row.user_id,
            authorName: row.author_name || 'Créateur PANU',
            text: row.content,
            createdAt: 'À l’instant',
          });
        }
      }
    )
    .subscribe();

  return () => {
    supabase.removeChannel(channel);
  };
}

function formatRelativeTime(dateStr: string): string {
  try {
    const diffMs = Date.now() - new Date(dateStr).getTime();
    const diffSec = Math.floor(diffMs / 1000);
    if (diffSec < 60) return 'À l’instant';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `Il y a ${diffMin} min`;
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) return `Il y a ${diffHours} h`;
    return new Date(dateStr).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
  } catch {
    return 'Récemment';
  }
}
