import { supabase, FOUNDER_EMAIL } from '../lib/supabaseClient';

export interface LiveKitRoomSession {
  roomName: string;
  wsUrl: string;
  participantToken: string;
  participantIdentity: string;
  isFounderHost: boolean;
}

/**
 * Service de connexion et de gestion des salons LiveKit Cloud (WebRTC Faible Latence)
 * Récupère les variables d'environnement (`NEXT_PUBLIC_LIVEKIT_WS_URL`, `LIVEKIT_API_KEY`)
 * ou génère un jeton de session via la Edge Function Supabase `livekit-token`.
 */
export async function joinOrCreateLiveKitRoom(params: {
  roomName: string;
  userId?: string;
  userEmail?: string;
  username?: string;
  canPublish?: boolean;
}): Promise<LiveKitRoomSession> {
  const env = (import.meta as unknown as { env?: Record<string, string> }).env || {};
  const wsUrl =
    env.NEXT_PUBLIC_LIVEKIT_WS_URL ||
    env.VITE_LIVEKIT_WS_URL ||
    'wss://panu-cloud.livekit.cloud';

  const isFounderHost =
    (params.userEmail || '').trim().toLowerCase() === FOUNDER_EMAIL.toLowerCase();

  const identity = params.userId || `panu_${Math.random().toString(36).slice(2, 8)}`;

  // Appel à la fonction Edge Supabase si déployée pour signer le JWT LiveKit Cloud
  try {
    const { data, error } = await supabase.functions.invoke('livekit-token', {
      body: {
        roomName: params.roomName,
        identity,
        name: params.username || (isFounderHost ? 'Fondateur PANU' : 'Membre PANU'),
        canPublish: Boolean(params.canPublish || isFounderHost),
      },
    });

    if (!error && data?.token) {
      return {
        roomName: params.roomName,
        wsUrl: data.wsUrl || wsUrl,
        participantToken: data.token,
        participantIdentity: identity,
        isFounderHost,
      };
    }
  } catch {
    // Repli gracieux si la fonction Edge n'est pas encore appelée
  }

  return {
    roomName: params.roomName,
    wsUrl,
    participantToken: '',
    participantIdentity: identity,
    isFounderHost,
  };
}
