import { supabase } from '../lib/supabaseClient';

export interface AppNotification {
  id: string;
  userId: string;
  actorId?: string;
  actorName?: string;
  postId?: string;
  title: string;
  message: string;
  notificationType: 'like' | 'comment' | 'follow' | 'gift' | 'system' | 'auth';
  isRead: boolean;
  createdAt: string;
}

/**
 * Service pour la gestion des Notifications en temps réel avec Supabase
 * - Table `public.notifications`
 * - Écoute Realtime sur `user_id` de l'utilisateur connecté
 * - Alertes instantanées pour chaque Like ou Commentaire
 */

/**
 * Récupère les notifications de l'utilisateur connecté triées par date décroissante
 */
export async function fetchUserNotifications(userId: string): Promise<AppNotification[]> {
  if (!userId) return [];

  try {
    const { data, error } = await supabase
      .from('notifications')
      .select('*')
      .eq('user_id', userId)
      .order('created_at', { ascending: false })
      .limit(50);

    if (error) {
      console.warn('[NotificationService] Erreur fetch notifications:', error.message);
      return [];
    }

    return (data || []).map((row: any) => ({
      id: row.id,
      userId: row.user_id,
      actorId: row.actor_id,
      actorName: row.actor_name,
      postId: row.post_id,
      title: row.title,
      message: row.message,
      notificationType: row.notification_type || 'like',
      isRead: !!row.is_read,
      createdAt: row.created_at || new Date().toISOString(),
    }));
  } catch (err) {
    console.warn('[NotificationService] Exception fetch notifications:', err);
    return [];
  }
}

/**
 * Marque une notification spécifique comme lue
 */
export async function markNotificationAsRead(notificationId: string): Promise<boolean> {
  try {
    const { error } = await supabase
      .from('notifications')
      .update({ is_read: true })
      .eq('id', notificationId);

    if (error) {
      console.warn('[NotificationService] Erreur mark as read:', error.message);
      return false;
    }
    return true;
  } catch {
    return false;
  }
}

/**
 * Marque toutes les notifications de l'utilisateur comme lues
 */
export async function markAllNotificationsAsRead(userId: string): Promise<boolean> {
  if (!userId) return false;

  try {
    const { error } = await supabase
      .from('notifications')
      .update({ is_read: true })
      .eq('user_id', userId)
      .eq('is_read', false);

    if (error) {
      console.warn('[NotificationService] Erreur mark all as read:', error.message);
      return false;
    }
    return true;
  } catch {
    return false;
  }
}

/**
 * Souscription en temps réel via Supabase Realtime aux nouvelles notifications d'un utilisateur
 */
export function subscribeToUserNotifications(
  userId: string,
  onNewNotification: (notification: AppNotification) => void
) {
  if (!userId) return () => {};

  const channel = supabase
    .channel(`user_notifications_${userId}_${Math.random().toString(36).substring(2, 7)}`)
    .on(
      'postgres_changes',
      {
        event: 'INSERT',
        schema: 'public',
        table: 'notifications',
        filter: `user_id=eq.${userId}`,
      },
      (payload) => {
        const row = payload.new as any;
        if (row) {
          const notif: AppNotification = {
            id: row.id,
            userId: row.user_id,
            actorId: row.actor_id,
            actorName: row.actor_name,
            postId: row.post_id,
            title: row.title,
            message: row.message,
            notificationType: row.notification_type || 'like',
            isRead: !!row.is_read,
            createdAt: row.created_at || new Date().toISOString(),
          };
          onNewNotification(notif);
        }
      }
    )
    .subscribe();

  return () => {
    supabase.removeChannel(channel);
  };
}
