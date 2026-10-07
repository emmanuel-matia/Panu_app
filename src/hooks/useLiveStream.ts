import { useEffect, useRef, useState, useCallback } from 'react';
import { RealtimeChannel } from '@supabase/supabase-js';
import { supabase } from '../lib/supabaseClient';

export interface LiveChatMessage {
  id: string;
  userId: string;
  username: string;
  avatarUrl?: string;
  message: string;
  createdAt: string;
}

export interface LiveGiftEvent {
  id: string;
  senderId: string;
  senderName: string;
  receiverName: string;
  giftType: string;
  giftIcon: string;
  creditsAmount: number;
  createdAt: string;
}

export interface UseLiveStreamOptions {
  streamId: string;
  /** URL du serveur WebRTC/LiveKit (ex: wss://panu-live.livekit.cloud) */
  livekitWsUrl?: string;
  /** Token d'accès LiveKit généré côté serveur pour ce salon */
  livekitToken?: string;
  /** Flux HLS/WebRTC de secours si le lecteur natif HTML5 est utilisé */
  fallbackStreamUrl?: string;
  currentUser?: {
    id: string;
    username: string;
    avatarUrl?: string;
  } | null;
}

/**
 * Hook modulaire `useLiveStream` (WebRTC / LiveKit + Supabase Realtime Presence, Chat & Cadeaux)
 * S'intègre directement sur le lecteur vidéo existant sans modifier l'architecture globale.
 */
export function useLiveStream({
  streamId,
  livekitWsUrl,
  livekitToken,
  fallbackStreamUrl,
  currentUser,
}: UseLiveStreamOptions) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const channelRef = useRef<RealtimeChannel | null>(null);

  const [isLive, setIsLive] = useState<boolean>(true);
  const [isConnecting, setIsConnecting] = useState<boolean>(true);
  const [viewerCount, setViewerCount] = useState<number>(1);
  const [messages, setMessages] = useState<LiveChatMessage[]>([]);
  const [recentGifts, setRecentGifts] = useState<LiveGiftEvent[]>([]);
  const [tokenBalance, setTokenBalance] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 1. Connexion au flux vidéo temps réel (LiveKit WebRTC SDK avec repli natif HLS/WebRTC)
  useEffect(() => {
    let isMounted = true;
    let livekitRoom: any = null;

    async function connectRealtimeVideo() {
      setIsConnecting(true);
      setError(null);

      try {
        // Si les identifiants LiveKit WebRTC sont fournis, attacher les pistes distantes à <video>
        if (livekitWsUrl && livekitToken) {
          const { Room, RoomEvent, Track } = await import('livekit-client');
          livekitRoom = new Room({
            adaptiveStream: true,
            dynacast: true,
          });

          livekitRoom.on(
            RoomEvent.TrackSubscribed,
            (track: any) => {
              if (
                (track.kind === Track.Kind.Video || track.kind === Track.Kind.Audio) &&
                videoRef.current
              ) {
                track.attach(videoRef.current);
                if (isMounted) {
                  setIsLive(true);
                  setIsConnecting(false);
                }
              }
            }
          );

          livekitRoom.on(RoomEvent.Disconnected, () => {
            if (isMounted) setIsLive(false);
          });

          await livekitRoom.connect(livekitWsUrl, livekitToken);
          if (isMounted) {
            setIsLive(true);
            setIsConnecting(false);
          }
          return;
        }

        // Repli direct sur le lecteur vidéo existant (flux low-latency / HLS)
        if (videoRef.current && fallbackStreamUrl) {
          videoRef.current.src = fallbackStreamUrl;
          await videoRef.current.play().catch(() => {
            // Autoplay muet si le navigateur bloque le son initial
            if (videoRef.current) {
              videoRef.current.muted = true;
              videoRef.current.play().catch(() => {});
            }
          });
        }

        if (isMounted) {
          setIsLive(true);
          setIsConnecting(false);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err?.message || 'Erreur de connexion au flux en direct');
          setIsConnecting(false);
        }
      }
    }

    connectRealtimeVideo();

    return () => {
      isMounted = false;
      if (livekitRoom) {
        livekitRoom.disconnect();
      }
    };
  }, [streamId, livekitWsUrl, livekitToken, fallbackStreamUrl]);

  // 2. Supabase Realtime : Compteur de spectateurs (Presence) + Chat en direct (Broadcast) + Cadeaux (Postgres Changes)
  useEffect(() => {
    if (!streamId) return;

    const presenceKey = currentUser?.id || `guest_${Math.random().toString(36).slice(2, 9)}`;

    const channel = supabase.channel(`panu_live_${streamId}`, {
      config: {
        presence: { key: presenceKey },
        broadcast: { self: true },
      },
    });

    // A. Synchronisation du nombre réel de spectateurs connectés via Presence
    channel.on('presence', { event: 'sync' }, () => {
      const presenceState = channel.presenceState();
      const connectedViewers = Object.keys(presenceState).length;
      setViewerCount(Math.max(1, connectedViewers));
    });

    // B. Réception des messages du Chat en direct (WebSockets faible latence)
    channel.on('broadcast', { event: 'chat_message' }, ({ payload }) => {
      if (payload) {
        setMessages((prev) => [...prev.slice(-49), payload as LiveChatMessage]);
      }
    });

    // C. Réception des cadeaux virtuels en temps réel (Broadcast + Table public.live_gifts)
    channel.on('broadcast', { event: 'live_gift' }, ({ payload }) => {
      if (payload) {
        const gift = payload as LiveGiftEvent;
        setRecentGifts((prev) => [gift, ...prev.slice(0, 4)]);
      }
    });

    channel.on(
      'postgres_changes',
      {
        event: 'INSERT',
        schema: 'public',
        table: 'live_gifts',
        filter: `stream_id=eq.${streamId}`,
      },
      (payload) => {
        const row = payload.new as any;
        const giftEvent: LiveGiftEvent = {
          id: row.id,
          senderId: row.sender_id,
          senderName: row.sender_name,
          receiverName: row.receiver_name,
          giftType: row.gift_type,
          giftIcon: row.gift_icon,
          creditsAmount: row.credits_amount,
          createdAt: row.created_at,
        };
        setRecentGifts((prev) => {
          if (prev.some((g) => g.id === giftEvent.id)) return prev;
          return [giftEvent, ...prev.slice(0, 4)];
        });
      }
    );

    channel.subscribe(async (status) => {
      if (status === 'SUBSCRIBED') {
        await channel.track({
          user_id: presenceKey,
          username: currentUser?.username || 'Spectateur',
          online_at: new Date().toISOString(),
        });
      }
    });

    channelRef.current = channel;

    return () => {
      channel.unsubscribe();
      channelRef.current = null;
    };
  }, [streamId, currentUser?.id, currentUser?.username]);

  // 3. Envoi d'un message dans le Chat en direct
  const sendChatMessage = useCallback(
    async (text: string) => {
      const cleanText = text.trim();
      if (!cleanText || !channelRef.current) return false;

      const chatMsg: LiveChatMessage = {
        id: `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
        userId: currentUser?.id || 'guest',
        username: currentUser?.username || 'Membre PANU',
        avatarUrl: currentUser?.avatarUrl,
        message: cleanText,
        createdAt: new Date().toISOString(),
      };

      await channelRef.current.send({
        type: 'broadcast',
        event: 'chat_message',
        payload: chatMsg,
      });

      return true;
    },
    [currentUser]
  );

  // 4. Envoi d'un Cadeau avec transaction de jetons atomique sur Supabase
  const sendGiftTransaction = useCallback(
    async (params: {
      receiverId?: string;
      receiverName: string;
      giftType: string;
      giftIcon: string;
      creditsCost: number;
      message?: string;
    }) => {
      setError(null);

      const { data, error: rpcError } = await supabase.rpc('send_live_gift_transaction', {
        p_stream_id: streamId,
        p_receiver_id: params.receiverId || null,
        p_receiver_name: params.receiverName,
        p_gift_type: params.giftType,
        p_gift_icon: params.giftIcon,
        p_credits_cost: params.creditsCost,
        p_message: params.message || null,
      });

      if (rpcError) {
        setError(rpcError.message);
        throw rpcError;
      }

      if (data && typeof data.remaining_balance === 'number') {
        setTokenBalance(data.remaining_balance);
      }

      // Diffusion instantanée aux spectateurs connectés en complément de postgres_changes
      if (channelRef.current && data?.gift_id) {
        const giftPayload: LiveGiftEvent = {
          id: data.gift_id,
          senderId: currentUser?.id || 'authenticated',
          senderName: data.sender_name || currentUser?.username || 'Membre PANU',
          receiverName: params.receiverName,
          giftType: params.giftType,
          giftIcon: params.giftIcon,
          creditsAmount: params.creditsCost,
          createdAt: new Date().toISOString(),
        };
        await channelRef.current.send({
          type: 'broadcast',
          event: 'live_gift',
          payload: giftPayload,
        });
      }

      return data;
    },
    [streamId, currentUser]
  );

  return {
    videoRef,
    isLive,
    isConnecting,
    viewerCount,
    messages,
    recentGifts,
    tokenBalance,
    error,
    sendChatMessage,
    sendGiftTransaction,
  };
}
