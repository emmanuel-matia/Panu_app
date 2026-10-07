import { supabase } from '../lib/supabaseClient';

export interface FollowEvent {
  followerId: string;
  followingId: string;
  eventType: 'INSERT' | 'DELETE';
}

/**
 * Alterne l'état d'abonnement (Suivre / Ne plus suivre)
 */
export async function toggleFollow(followerId: string, followingId: string): Promise<boolean> {
  if (!followerId || !followingId || followerId === followingId) return false;

  // Vérifier si l'abonnement existe
  const { data: existing } = await supabase
    .from('follows')
    .select('id')
    .eq('follower_id', followerId)
    .eq('following_id', followingId)
    .single();

  if (existing) {
    // Se désabonner
    const { error } = await supabase
      .from('follows')
      .delete()
      .eq('follower_id', followerId)
      .eq('following_id', followingId);
    return !error;
  } else {
    // S'abonner
    const { error } = await supabase
      .from('follows')
      .insert({
        follower_id: followerId,
        following_id: followingId,
      });
    return !error;
  }
}

/**
 * Récupère la liste des IDs des créateurs suivis par un utilisateur
 */
export async function fetchFollowingIds(userId: string): Promise<Set<string>> {
  if (!userId) return new Set();
  const { data, error } = await supabase
    .from('follows')
    .select('following_id')
    .eq('follower_id', userId);

  if (error || !data) return new Set();
  return new Set(data.map((f) => f.following_id));
}

/**
 * Souscription Realtime aux abonnements
 */
export function subscribeToRealtimeFollows(callback: (event: FollowEvent) => void) {
  const channel = supabase
    .channel('public:follows')
    .on(
      'postgres_changes',
      { event: '*', schema: 'public', table: 'follows' },
      (payload) => {
        if (payload.eventType === 'INSERT') {
          callback({
            followerId: payload.new.follower_id,
            followingId: payload.new.following_id,
            eventType: 'INSERT',
          });
        } else if (payload.eventType === 'DELETE') {
          callback({
            followerId: payload.old.follower_id,
            followingId: payload.old.following_id,
            eventType: 'DELETE',
          });
        }
      }
    )
    .subscribe();

  return () => {
    supabase.removeChannel(channel);
  };
}
