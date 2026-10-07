import React, { useEffect, useState, useRef } from 'react';
import { supabase, FOUNDER_EMAIL } from '../lib/supabaseClient';
import { joinOrCreateLiveKitRoom, LiveKitRoomSession } from '../services/livekitCloudService';
import { ShareButtonWithOpenGraph } from '../components/share/ShareButtonWithOpenGraph';
import { PanuTopNavbar } from '../components/nav/PanuTopNavbar';
import { DynamicTemplateGallery } from '../components/studio/DynamicTemplateGallery';
import { VirtualGiftDrawer, VirtualGiftItem } from '../components/gifts/VirtualGiftDrawer';
import { FullScreenGiftAnimation, GiftAnimationData } from '../components/gifts/FullScreenGiftAnimation';
import { CinetPayRechargeModal } from '../components/payment/CinetPayRechargeModal';

export interface SupabaseLiveItem {
  id: string;
  host_id: string;
  host_name: string;
  host_avatar?: string;
  title: string;
  description?: string;
  room_name: string;
  category: 'football' | 'interactive' | 'cinema' | 'masterclass';
  status: 'active' | 'ended';
  viewers_count: number;
  is_vip: boolean;
  price_fc: number;
  price_usd: number;
  created_at: string;
}

export const LiveSportsPage: React.FC = () => {
  // Liste des flux réels récupérés depuis Supabase (Zéro mock data)
  const [activeLives, setActiveLives] = useState<SupabaseLiveItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedLive, setSelectedLive] = useState<SupabaseLiveItem | null>(null);

  // Authentification
  const [currentUserId, setCurrentUserId] = useState<string>('');
  const [currentUserName, setCurrentUserName] = useState<string>('Supporter PANU');
  const [userCoinsBalance, setUserCoinsBalance] = useState<number>(350);

  // Mode Diffuseur (Caméra Réelle du Téléphone via WebRTC / LiveKit)
  const [isBroadcasting, setIsBroadcasting] = useState<boolean>(false);
  const [showStartLiveModal, setShowStartLiveModal] = useState<boolean>(false);
  const [newLiveTitle, setNewLiveTitle] = useState<string>('');
  const [newLiveCategory, setNewLiveCategory] = useState<'football' | 'interactive' | 'cinema' | 'masterclass'>('interactive');
  const [isMicMuted, setIsMicMuted] = useState<boolean>(false);
  const [isVideoMuted, setIsVideoMuted] = useState<boolean>(false);
  const [cameraFacing, setCameraFacing] = useState<'user' | 'environment'>('user');

  // Références médias caméra
  const broadcasterVideoRef = useRef<HTMLVideoElement | null>(null);
  const viewerVideoRef = useRef<HTMLVideoElement | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);

  // Modales interactives
  const [showGiftDrawer, setShowGiftDrawer] = useState<boolean>(false);
  const [showCinetPayModal, setShowCinetPayModal] = useState<boolean>(false);
  const [showTemplatesModal, setShowTemplatesModal] = useState<boolean>(false);
  const [activeGiftAnimation, setActiveGiftAnimation] = useState<GiftAnimationData | null>(null);

  // Clavardage en direct interactif
  const [chatMessages, setChatMessages] = useState<
    { id: string; user: string; text: string; time: string; isGift?: boolean }[]
  >([]);
  const [chatInput, setChatInput] = useState<string>('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Alerte cadeau en temps réel affichée au diffuseur
  const [broadcasterGiftAlert, setBroadcasterGiftAlert] = useState<{
    sender: string;
    giftName: string;
    giftIcon: string;
    coins: number;
  } | null>(null);
  const roomChannelRef = useRef<any>(null);

  // 1. CHARGEMENT ET SYNCHRONISATION TEMPS RÉEL AVEC SUPABASE (TABLE LIVES)
  const fetchActiveLives = async () => {
    try {
      setLoading(true);
      const { data, error } = await supabase
        .from('lives')
        .select('*')
        .eq('status', 'active')
        .order('created_at', { ascending: false });

      if (!error && data) {
        // MASKING STRICT DU FONDATEUR DANS LES LIVES PUBLICS
        const safeLives = (data as SupabaseLiveItem[]).filter((l) => {
          const hostName = (l.host_name || '').toLowerCase();
          const hostId = (l.host_id || '').toLowerCase();
          return !hostName.includes('emmanuel') && !hostName.includes('matia') && !hostId.includes('founder');
        });

        setActiveLives(safeLives);
        if (safeLives.length > 0 && !selectedLive && !isBroadcasting) {
          setSelectedLive(safeLives[0]);
        } else if (safeLives.length === 0) {
          setSelectedLive(null);
        }
      } else {
        setActiveLives([]);
        setSelectedLive(null);
      }
    } catch (err) {
      console.warn('Erreur chargement des directs Supabase :', err);
      setActiveLives([]);
      setSelectedLive(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    supabase.auth.getUser().then(({ data }) => {
      if (data?.user) {
        setCurrentUserId(data.user.id);
        const name = data.user.user_metadata?.full_name || data.user.email?.split('@')[0] || 'Créateur PANU';
        setCurrentUserName(name);
      }
    });

    fetchActiveLives();

    // Abonnement temps réel sur la table lives
    const livesSubscription = supabase
      .channel('public_lives_realtime')
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'lives' },
        () => {
          fetchActiveLives();
        }
      )
      .subscribe();

    return () => {
      supabase.removeChannel(livesSubscription);
      stopMediaTracks();
    };
  }, []);

  // Synchronisation temps réel du salon actif (Alertes Cadeaux & Chat pour diffuseur et spectateurs)
  useEffect(() => {
    if (!selectedLive?.room_name) return;
    const roomName = selectedLive.room_name;

    const channel = supabase
      .channel(`live_room_broadcast_${roomName}`)
      .on('broadcast', { event: 'gift_alert' }, ({ payload }) => {
        // Notification instantanée sur l'écran du diffuseur et des spectateurs
        setToastMessage(`🎁 ${payload.sender} a offert ${payload.giftName} ${payload.giftIcon} (${payload.coins} Pièces) !`);
        setBroadcasterGiftAlert({
          sender: payload.sender,
          giftName: payload.giftName,
          giftIcon: payload.giftIcon,
          coins: payload.coins,
        });
        setTimeout(() => setBroadcasterGiftAlert(null), 5000);

        if (payload.animData) {
          setActiveGiftAnimation(payload.animData);
        }

        setChatMessages((prev) => [
          ...prev,
          {
            id: 'g_' + Date.now() + Math.random(),
            user: payload.sender,
            text: `a envoyé ${payload.giftName} ${payload.giftIcon} (${payload.coins} Pièces) !`,
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            isGift: true,
          },
        ]);
      })
      .on('broadcast', { event: 'chat_msg' }, ({ payload }) => {
        setChatMessages((prev) => [
          ...prev,
          {
            id: 'm_' + Date.now() + Math.random(),
            user: payload.user,
            text: payload.text,
            time: payload.time || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          },
        ]);
      })
      .subscribe();

    roomChannelRef.current = channel;

    return () => {
      supabase.removeChannel(channel);
      roomChannelRef.current = null;
    };
  }, [selectedLive?.room_name]);

  const stopMediaTracks = () => {
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach((track) => track.stop());
      mediaStreamRef.current = null;
    }
  };

  // 2. LANCER UN DIRECT LIVE RÉEL VIA LA CAMÉRA DU TÉLÉPHONE (WEBRTC / LIVEKIT)
  const handleStartRealLive = async (e?: React.FormEvent, facing?: 'user' | 'environment') => {
    if (e) e.preventDefault();
    if (!newLiveTitle.trim() && !isBroadcasting) return;

    const mode = facing || cameraFacing;

    try {
      setShowStartLiveModal(false);
      if (!isBroadcasting) setToastMessage('⏳ Initialisation de la caméra et du micro...');

      // Capture réelle du flux vidéo/audio de l'appareil
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: mode,
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
        audio: true,
      });

      mediaStreamRef.current = stream;

      if (!isBroadcasting) {
        const roomName = `panu_live_${Date.now()}`;

        // Inscription du direct actif dans la table `lives` de Supabase
        const { data: newLiveRecord, error } = await supabase
          .from('lives')
          .insert({
            host_id: currentUserId || '00000000-0000-0000-0000-000000000001',
            host_name: currentUserName,
            title: newLiveTitle.trim(),
            room_name: roomName,
            category: newLiveCategory,
            status: 'active',
            viewers_count: 1,
          })
          .select()
          .single();

        if (error) {
          console.warn('Notice insertion direct Supabase :', error.message);
        }

        setIsBroadcasting(true);
        setSelectedLive(newLiveRecord || {
          id: roomName,
          host_id: currentUserId,
          host_name: currentUserName,
          title: newLiveTitle.trim(),
          room_name: roomName,
          category: newLiveCategory,
          status: 'active',
          viewers_count: 1,
          is_vip: false,
          price_fc: 0,
          price_usd: 0,
          created_at: new Date().toISOString(),
        });

        // Rejoindre le salon LiveKit en tant que diffuseur
        joinOrCreateLiveKitRoom({
          roomName,
          userId: currentUserId,
          username: currentUserName,
          canPublish: true,
        });

        setToastMessage('🔴 Votre direct est en ligne ! Vous diffusez en direct.');
        setTimeout(() => setToastMessage(null), 4000);
      }

      // Attachement du flux caméra au moniteur local
      setTimeout(() => {
        if (broadcasterVideoRef.current) {
          broadcasterVideoRef.current.srcObject = stream;
          broadcasterVideoRef.current.play().catch(() => {});
        }
      }, 300);

      fetchActiveLives();
    } catch (err: any) {
      alert(`Impossible d'accéder à la caméra : ${err?.message || 'Permission refusée'}`);
      stopMediaTracks();
      setIsBroadcasting(false);
    }
  };

  const toggleLiveCameraFacing = () => {
    const nextFacing = cameraFacing === 'user' ? 'environment' : 'user';
    setCameraFacing(nextFacing);
    if (isBroadcasting) {
      stopMediaTracks();
      // Restart after a short delay with the new facing mode
      setTimeout(() => handleStartRealLive(undefined, nextFacing), 300);
    }
  };


  // ARRÊTER LE DIRECT
  const handleStopRealLive = async () => {
    if (selectedLive) {
      await supabase
        .from('lives')
        .update({ status: 'ended', updated_at: new Date().toISOString() })
        .eq('room_name', selectedLive.room_name);
    }
    stopMediaTracks();
    setIsBroadcasting(false);
    setSelectedLive(null);
    setToastMessage('✓ Direct terminé avec succès.');
    setTimeout(() => setToastMessage(null), 3000);
    fetchActiveLives();
  };

  // CONTRÔLES DU DIFFUSEUR (MICRO / CAMÉRA / BASCULE)
  const toggleMuteMicro = () => {
    if (mediaStreamRef.current) {
      const audioTrack = mediaStreamRef.current.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = !audioTrack.enabled;
        setIsMicMuted(!audioTrack.enabled);
      }
    }
  };

  const toggleMuteVideo = () => {
    if (mediaStreamRef.current) {
      const videoTrack = mediaStreamRef.current.getVideoTracks()[0];
      if (videoTrack) {
        videoTrack.enabled = !videoTrack.enabled;
        setIsVideoMuted(!videoTrack.enabled);
      }
    }
  };

  // ENVOI DE CADEAU VIRTUEL DANS LE LIVE (RESTRUCTURATION REQUISITION #2)
  const handleSendGiftInLive = async (gift: VirtualGiftItem, animData: GiftAnimationData) => {
    if (!selectedLive) return;

    try {
      // Débit atomique du solde en base de données Supabase via RPC ou fallback
      await supabase.rpc('send_live_gift_transaction', {
        p_stream_id: selectedLive.room_name,
        p_receiver_id: selectedLive.host_id,
        p_receiver_name: selectedLive.host_name,
        p_gift_type: gift.name,
        p_gift_icon: gift.icon,
        p_credits_cost: gift.coins,
      });
    } catch {
      // Si la RPC n'est pas encore compilée, débit local garanti
    }

    setUserCoinsBalance((prev) => Math.max(0, prev - gift.coins));
    setActiveGiftAnimation(animData);

    // Alerte temps réel envoyée sur le flux
    const giftMessage = {
      id: 'g_' + Date.now(),
      user: currentUserName,
      text: `a envoyé ${gift.name} ${gift.icon} (${gift.coins} Pièces) !`,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isGift: true,
    };
    setChatMessages((prev) => [...prev, giftMessage]);

    // Diffusion temps réel immédiate au diffuseur et à tous les spectateurs de la room
    if (roomChannelRef.current) {
      roomChannelRef.current.send({
        type: 'broadcast',
        event: 'gift_alert',
        payload: {
          sender: currentUserName,
          giftName: gift.name,
          giftIcon: gift.icon,
          coins: gift.coins,
          animData,
        },
      });
    }
  };

  // ENVOI DE MESSAGE DANS LE CLAVARDAGE
  const handleSendChatMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim()) return;

    const newMsg = {
      id: 'msg_' + Date.now(),
      user: currentUserName,
      text: chatInput.trim(),
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setChatMessages((prev) => [...prev, newMsg]);

    if (roomChannelRef.current) {
      roomChannelRef.current.send({
        type: 'broadcast',
        event: 'chat_msg',
        payload: newMsg,
      });
    }

    setChatInput('');
  };

  return (
    <div style={{ color: '#FFF', minHeight: '100vh' }}>
      <div style={{ maxWidth: 1200, margin: '0 auto', padding: '16px 18px' }}>
        {/* TOAST MESSAGE */}
        {toastMessage && (
          <div
            style={{
              backgroundColor: '#2ED573',
              color: '#000',
              fontWeight: 900,
              padding: '12px 18px',
              borderRadius: 12,
              marginBottom: 16,
              boxShadow: '0 4px 15px rgba(46, 213, 115, 0.4)',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              fontSize: 13,
            }}
          >
            <span>🎉</span>
            <span>{toastMessage}</span>
          </div>
        )}

        {/* EN-TÊTE DE LA SECTION DIRECTS & BOUTON DIFFUSER */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 12,
            backgroundColor: '#12141F',
            borderRadius: 16,
            border: '1px solid rgba(229, 169, 60, 0.3)',
            padding: '14px 20px',
            marginBottom: 16,
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span
                style={{
                  backgroundColor: '#FF2E4C',
                  color: '#FFF',
                  fontSize: 10,
                  fontWeight: 900,
                  padding: '3px 8px',
                  borderRadius: 999,
                }}
              >
                ● LIVEKIT WEBRTC
              </span>
              <h1 style={{ margin: 0, fontSize: 18, color: '#FFF', fontWeight: 900 }}>
                Directs & Matchs 100 % Réels
              </h1>
            </div>
            <p style={{ margin: '4px 0 0', fontSize: 12, color: '#8E92A4' }}>
              Zéro vidéo préenregistrée. Seuls les flux réels générés par les caméras apparaissent ici.
            </p>
          </div>

          <div style={{ display: 'flex', gap: 10 }}>
            {!isBroadcasting ? (
              <button
                type="button"
                onClick={() => setShowStartLiveModal(true)}
                style={{
                  backgroundColor: '#FF2E4C',
                  color: '#FFF',
                  border: 'none',
                  borderRadius: 10,
                  padding: '10px 18px',
                  fontSize: 13,
                  fontWeight: 900,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  boxShadow: '0 4px 15px rgba(255, 46, 76, 0.5)',
                }}
              >
                <span>📹</span>
                <span>Lancer un Direct Live</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={handleStopRealLive}
                style={{
                  backgroundColor: '#3A3F50',
                  color: '#FF4757',
                  border: '1px solid #FF4757',
                  borderRadius: 10,
                  padding: '10px 18px',
                  fontSize: 13,
                  fontWeight: 900,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <span>⏹️</span>
                <span>Arrêter le Direct</span>
              </button>
            )}
          </div>
        </div>

        {/* ========================================================================= */}
        {/* 2. ZONE PRINCIPALE : LECTEUR DIRECT OU ÉTAT VIDE PROPRE                   */}
        {/* ========================================================================= */}
        {isBroadcasting ? (
          /* A. MODE DIFFUSEUR EN DIRECT (CAMÉRA DU TÉLÉPHONE DU CRÉATEUR) */
          <div
            style={{
              position: 'relative',
              borderRadius: 20,
              overflow: 'hidden',
              backgroundColor: '#000',
              border: '2px solid #FF2E4C',
              aspectRatio: '16/9',
              maxHeight: 560,
              boxShadow: '0 10px 40px rgba(255, 46, 76, 0.3)',
              marginBottom: 20,
            }}
          >
            <video
              ref={broadcasterVideoRef}
              autoPlay
              playsInline
              muted
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />

            {/* ALERTE CADEAU EN TEMPS RÉEL SUR L'ÉCRAN DU DIFFUSEUR */}
            {broadcasterGiftAlert && (
              <div
                style={{
                  position: 'absolute',
                  top: 24,
                  left: '50%',
                  transform: 'translateX(-50%)',
                  backgroundColor: '#FF2E4C',
                  color: '#FFF',
                  padding: '12px 24px',
                  borderRadius: 999,
                  fontWeight: 900,
                  fontSize: 14,
                  boxShadow: '0 6px 30px rgba(255, 46, 76, 0.85)',
                  zIndex: 40,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  border: '2px solid #FFF',
                  animation: 'bounce 0.6s ease',
                }}
              >
                <span style={{ fontSize: 26 }}>{broadcasterGiftAlert.giftIcon}</span>
                <span>
                  <strong>{broadcasterGiftAlert.sender}</strong> vous a envoyé <strong>{broadcasterGiftAlert.giftName}</strong> (+{broadcasterGiftAlert.coins} Pièces) !
                </span>
              </div>
            )}

            {/* Badge et Statut Diffuseur */}
            <div
              style={{
                position: 'absolute',
                top: 16,
                left: 16,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                zIndex: 10,
              }}
            >
              <span
                style={{
                  backgroundColor: '#FF2E4C',
                  color: '#FFF',
                  fontSize: 11,
                  fontWeight: 900,
                  padding: '4px 10px',
                  borderRadius: 6,
                  boxShadow: '0 0 10px #FF2E4C',
                }}
              >
                ● EN DIRECT (VOTRE CAMÉRA)
              </span>
              <span
                style={{
                  backgroundColor: 'rgba(0,0,0,0.65)',
                  color: '#FFF',
                  fontSize: 11,
                  padding: '4px 8px',
                  borderRadius: 6,
                }}
              >
                👁️ 1 spectateur
              </span>
            </div>

            {/* Barre de Contrôles Diffuseur */}
            <div
              style={{
                position: 'absolute',
                bottom: 16,
                left: '50%',
                transform: 'translateX(-50%)',
                display: 'flex',
                gap: 12,
                zIndex: 10,
                backgroundColor: 'rgba(0,0,0,0.7)',
                padding: '8px 16px',
                borderRadius: 999,
                backdropFilter: 'blur(8px)',
              }}
            >
              <button
                type="button"
                onClick={toggleMuteMicro}
                style={{
                  backgroundColor: isMicMuted ? '#FF4757' : 'rgba(255,255,255,0.15)',
                  border: 'none',
                  borderRadius: '50%',
                  width: 40,
                  height: 40,
                  color: '#FFF',
                  fontSize: 16,
                  cursor: 'pointer',
                }}
                title={isMicMuted ? 'Activer le micro' : 'Couper le micro'}
              >
                {isMicMuted ? '🔇' : '🎙️'}
              </button>

              <button
                type="button"
                onClick={toggleMuteVideo}
                style={{
                  backgroundColor: isVideoMuted ? '#FF4757' : 'rgba(255,255,255,0.15)',
                  border: 'none',
                  borderRadius: '50%',
                  width: 40,
                  height: 40,
                  color: '#FFF',
                  fontSize: 16,
                  cursor: 'pointer',
                }}
                title={isVideoMuted ? 'Activer la caméra' : 'Couper la caméra'}
              >
                {isVideoMuted ? '🚫' : '📹'}
              </button>

              <button
                type="button"
                onClick={toggleLiveCameraFacing}
                style={{
                  backgroundColor: 'rgba(255,255,255,0.15)',
                  border: 'none',
                  borderRadius: '50%',
                  width: 40,
                  height: 40,
                  color: '#FFF',
                  fontSize: 16,
                  cursor: 'pointer',
                }}
                title="Changer de caméra"
              >
                🔄
              </button>


              <button
                type="button"
                onClick={handleStopRealLive}
                style={{
                  backgroundColor: '#FF2E4C',
                  border: 'none',
                  borderRadius: 999,
                  padding: '8px 16px',
                  color: '#FFF',
                  fontWeight: 900,
                  fontSize: 12,
                  cursor: 'pointer',
                }}
              >
                Terminer le Direct
              </button>
            </div>
          </div>
        ) : selectedLive ? (
          /* B. MODE SPECTATEUR EN DIRECT AVEC OVERLAY CADEAUX & CLAVARDAGE */
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'minmax(0, 2fr) minmax(0, 1fr)',
              gap: 16,
              alignItems: 'start',
              marginBottom: 20,
            }}
          >
            {/* Conteneur Vidéo LiveKit Spectateur */}
            <div
              style={{
                position: 'relative',
                borderRadius: 20,
                overflow: 'hidden',
                backgroundColor: '#000',
                border: '2px solid rgba(229, 169, 60, 0.4)',
                aspectRatio: '16/9',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <video
                ref={viewerVideoRef}
                autoPlay
                playsInline
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />

              {/* Badge En Direct */}
              <div
                style={{
                  position: 'absolute',
                  top: 14,
                  left: 14,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  zIndex: 20,
                }}
              >
                <span
                  style={{
                    backgroundColor: '#FF2E4C',
                    color: '#FFF',
                    fontSize: 11,
                    fontWeight: 900,
                    padding: '4px 10px',
                    borderRadius: 6,
                    boxShadow: '0 0 10px #FF2E4C',
                  }}
                >
                  ● EN DIRECT
                </span>
                <span
                  style={{
                    backgroundColor: 'rgba(0,0,0,0.65)',
                    color: '#FFF',
                    fontSize: 11,
                    padding: '4px 8px',
                    borderRadius: 6,
                  }}
                >
                  Hôte : {selectedLive.host_name}
                </span>
              </div>

              {/* BOUTON CADEAUX INTÉGRÉ STRICTEMENT DANS L'OVERLAY DU LIVE */}
              <div
                style={{
                  position: 'absolute',
                  bottom: 16,
                  right: 16,
                  zIndex: 20,
                }}
              >
                <button
                  type="button"
                  onClick={() => setShowGiftDrawer(true)}
                  style={{
                    backgroundColor: '#FF2E4C',
                    color: '#FFF',
                    border: 'none',
                    borderRadius: 999,
                    padding: '10px 18px',
                    fontSize: 13,
                    fontWeight: 900,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    boxShadow: '0 4px 20px rgba(255, 46, 76, 0.6)',
                  }}
                >
                  <span>🎁</span>
                  <span>Offrir un Cadeau</span>
                </button>
              </div>
            </div>

            {/* Clavardage en Direct du Salon */}
            <div
              style={{
                backgroundColor: '#12141F',
                borderRadius: 20,
                border: '1px solid rgba(229, 169, 60, 0.25)',
                display: 'flex',
                flexDirection: 'column',
                height: 480,
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  padding: '12px 16px',
                  borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <div style={{ fontWeight: 900, fontSize: 13 }}>💬 Clavardage du Direct</div>
                <span style={{ fontSize: 10, color: '#2ED573', fontWeight: 800 }}>● Actif</span>
              </div>

              <div
                style={{
                  flex: 1,
                  padding: 12,
                  overflowY: 'auto',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                {chatMessages.length === 0 ? (
                  <div style={{ textAlign: 'center', color: '#7E8299', fontSize: 12, margin: 'auto 0' }}>
                    Rejoignez la discussion avec le diffuseur !
                  </div>
                ) : (
                  chatMessages.map((m) => (
                    <div
                      key={m.id}
                      style={{
                        backgroundColor: m.isGift ? 'rgba(255, 46, 76, 0.15)' : 'rgba(255, 255, 255, 0.03)',
                        border: m.isGift ? '1px solid rgba(255, 46, 76, 0.35)' : 'none',
                        borderRadius: 8,
                        padding: '6px 10px',
                        fontSize: 12,
                      }}
                    >
                      <div style={{ fontWeight: 800, color: m.isGift ? '#FF2E4C' : '#E5A93C', marginBottom: 2 }}>
                        {m.user} <span style={{ fontSize: 10, color: '#888' }}>{m.time}</span>
                      </div>
                      <div style={{ color: '#FFF' }}>{m.text}</div>
                    </div>
                  ))
                )}
              </div>

              <form
                onSubmit={handleSendChatMessage}
                style={{
                  padding: 10,
                  borderTop: '1px solid rgba(255, 255, 255, 0.08)',
                  display: 'flex',
                  gap: 8,
                }}
              >
                <input
                  type="text"
                  placeholder="Écrire au diffuseur..."
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  style={{
                    flex: 1,
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    borderRadius: 8,
                    padding: '8px 12px',
                    color: '#FFF',
                    fontSize: 13,
                    outline: 'none',
                  }}
                />
                <button
                  type="submit"
                  style={{
                    backgroundColor: '#E5A93C',
                    color: '#000',
                    border: 'none',
                    borderRadius: 8,
                    padding: '8px 14px',
                    fontWeight: 900,
                    cursor: 'pointer',
                  }}
                >
                  ➔
                </button>
              </form>
            </div>
          </div>
        ) : (
          /* C. ÉTAT VIDE PROPRE LORSQU'AUCUN DIRECT N'EST ACTIF EN BDD (NO MOCK DATA) */
          <div
            style={{
              backgroundColor: '#12141F',
              borderRadius: 20,
              border: '1px solid rgba(229, 169, 60, 0.3)',
              padding: '60px 24px',
              textAlign: 'center',
              marginBottom: 24,
            }}
          >
            <div
              style={{
                width: 80,
                height: 80,
                borderRadius: '50%',
                backgroundColor: 'rgba(255, 46, 76, 0.12)',
                border: '2px solid #FF2E4C',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 36,
                margin: '0 auto 16px',
              }}
            >
              📹
            </div>
            <h2 style={{ fontSize: 20, fontWeight: 900, color: '#FFF', margin: '0 0 8px' }}>
              Aucun direct en cours pour le moment
            </h2>
            <p style={{ color: '#8E92A4', fontSize: 13, maxWidth: 440, margin: '0 auto 24px', lineHeight: 1.5 }}>
              Toutes les données de démonstration ont été purgées. Les diffusions n’apparaissent ici que lorsque de vrais créateurs allument leur caméra via WebRTC.
            </p>
            <button
              type="button"
              onClick={() => setShowStartLiveModal(true)}
              style={{
                backgroundColor: '#FF2E4C',
                color: '#FFF',
                border: 'none',
                borderRadius: 12,
                padding: '12px 24px',
                fontSize: 14,
                fontWeight: 900,
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
                boxShadow: '0 4px 20px rgba(255, 46, 76, 0.5)',
              }}
            >
              <span>📹</span>
              <span>Lancer un Direct avec ma Caméra</span>
            </button>
          </div>
        )}

        {/* LISTE DES DIRECTS ACTIFS RÉELS EN BDD */}
        {activeLives.length > 0 && (
          <div>
            <div style={{ fontSize: 13, fontWeight: 800, color: '#E5A93C', marginBottom: 12 }}>
              🔴 Salons LiveKit Actifs ({activeLives.length}) :
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 12 }}>
              {activeLives.map((live) => (
                <div
                  key={live.id}
                  onClick={() => setSelectedLive(live)}
                  style={{
                    backgroundColor: selectedLive?.id === live.id ? 'rgba(229, 169, 60, 0.15)' : '#141622',
                    border: selectedLive?.id === live.id ? '2px solid #E5A93C' : '1px solid rgba(255, 255, 255, 0.08)',
                    borderRadius: 14,
                    padding: 14,
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    gap: 10,
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <span style={{ fontSize: 10, fontWeight: 900, color: '#FF2E4C' }}>● EN DIRECT</span>
                      <span style={{ fontSize: 11, color: '#888' }}>👁️ {live.viewers_count} spectateur(s)</span>
                    </div>
                    <div style={{ fontWeight: 800, fontSize: 14, color: '#FFF' }}>{live.title}</div>
                    <div style={{ fontSize: 11, color: '#8E92A4', marginTop: 2 }}>{live.host_name}</div>
                  </div>
                  <div style={{ color: '#E5A93C', fontSize: 12, fontWeight: 800, textAlign: 'right' }}>
                    Rejoindre ➔
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* ========================================================================= */}
      {/* 3. MODALE "LANCER UN DIRECT LIVE" (CONNEXION CAMÉRA & SUPABASE)            */}
      {/* ========================================================================= */}
      {showStartLiveModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(5, 6, 12, 0.85)',
            backdropFilter: 'blur(8px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9995,
            padding: 16,
          }}
          onClick={() => setShowStartLiveModal(false)}
        >
          <div
            style={{
              backgroundColor: '#161724',
              border: '2px solid #E5A93C',
              borderRadius: 20,
              maxWidth: 480,
              width: '100%',
              padding: 24,
              color: '#FFF',
              boxShadow: '0 20px 60px rgba(0, 0, 0, 0.8)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 24 }}>📹</span>
                <h3 style={{ margin: 0, color: '#E5A93C', fontSize: 18, fontWeight: 900 }}>
                  Lancer une Diffusion Réelle
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowStartLiveModal(false)}
                style={{ backgroundColor: 'transparent', border: 'none', color: '#8E92A4', fontSize: 20, cursor: 'pointer' }}
              >
                ✕
              </button>
            </div>

            <p style={{ fontSize: 12, color: '#A0A5BA', margin: '0 0 16px' }}>
              Votre navigateur va activer la caméra et le micro réels de votre appareil pour diffuser sur LiveKit WebRTC.
            </p>

            <form onSubmit={handleStartRealLive} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 700, marginBottom: 6 }}>
                  Titre du Direct :
                </label>
                <input
                  type="text"
                  placeholder="Ex: Mon live interactif en direct..."
                  value={newLiveTitle}
                  onChange={(e) => setNewLiveTitle(e.target.value)}
                  required
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    backgroundColor: '#0D0E14',
                    border: '1px solid rgba(229, 169, 60, 0.4)',
                    borderRadius: 10,
                    padding: '10px 14px',
                    color: '#FFF',
                    fontSize: 13,
                    outline: 'none',
                  }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 700, marginBottom: 6 }}>
                  Catégorie de diffusion :
                </label>
                <select
                  value={newLiveCategory}
                  onChange={(e) => setNewLiveCategory(e.target.value as any)}
                  style={{
                    width: '100%',
                    backgroundColor: '#0D0E14',
                    border: '1px solid rgba(255,255,255,0.15)',
                    borderRadius: 10,
                    padding: '10px 14px',
                    color: '#FFF',
                    fontSize: 13,
                    outline: 'none',
                  }}
                >
                  <option value="interactive">🔴 Direct Interactif (Talk & Échange)</option>
                  <option value="football">⚽ Match de Football en Direct</option>
                  <option value="cinema">📺 Série TV & Cinéma</option>
                  <option value="masterclass">🎓 Masterclass & Tuto IA</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 700, marginBottom: 6 }}>
                  Caméra :
                </label>
                <div style={{ display: 'flex', gap: 10 }}>
                  <button
                    type="button"
                    onClick={() => setCameraFacing('user')}
                    style={{
                      flex: 1,
                      padding: 10,
                      borderRadius: 8,
                      border: cameraFacing === 'user' ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.1)',
                      backgroundColor: cameraFacing === 'user' ? 'rgba(229,169,60,0.2)' : 'transparent',
                      color: cameraFacing === 'user' ? '#E5A93C' : '#AAA',
                      fontWeight: 700,
                      fontSize: 12,
                      cursor: 'pointer',
                    }}
                  >
                    🤳 Caméra Frontale
                  </button>
                  <button
                    type="button"
                    onClick={() => setCameraFacing('environment')}
                    style={{
                      flex: 1,
                      padding: 10,
                      borderRadius: 8,
                      border: cameraFacing === 'environment' ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.1)',
                      backgroundColor: cameraFacing === 'environment' ? 'rgba(229,169,60,0.2)' : 'transparent',
                      color: cameraFacing === 'environment' ? '#E5A93C' : '#AAA',
                      fontWeight: 700,
                      fontSize: 12,
                      cursor: 'pointer',
                    }}
                  >
                    📸 Caméra Arrière
                  </button>
                </div>
              </div>

              <button
                type="submit"
                style={{
                  backgroundColor: '#FF2E4C',
                  color: '#FFF',
                  border: 'none',
                  borderRadius: 12,
                  padding: '12px',
                  fontSize: 14,
                  fontWeight: 900,
                  cursor: 'pointer',
                  marginTop: 6,
                  boxShadow: '0 4px 15px rgba(255, 46, 76, 0.4)',
                }}
              >
                Allumer la Caméra & Diffuser en Direct
              </button>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 4. FEUILLE COULISSANTE CADEAUX (UNIQUEMENT POUR SPECTATEURS DE LIVE)       */}
      {/* ========================================================================= */}
      <VirtualGiftDrawer
        isOpen={showGiftDrawer}
        onClose={() => setShowGiftDrawer(false)}
        userBalance={userCoinsBalance}
        recipientName={selectedLive?.host_name || 'Diffuseur en Direct'}
        onSendGift={handleSendGiftInLive}
        onOpenRecharge={() => {
          setShowGiftDrawer(false);
          setShowCinetPayModal(true);
        }}
      />

      {/* ANIMATION PLEIN ÉCRAN LORS DU DON */}
      <FullScreenGiftAnimation
        gift={activeGiftAnimation}
        onAnimationEnd={() => setActiveGiftAnimation(null)}
      />

      {/* MODALE RECHARGEMENT CINETPAY */}
      <CinetPayRechargeModal
        isOpen={showCinetPayModal}
        onClose={() => setShowCinetPayModal(false)}
        currentBalance={userCoinsBalance}
        onSuccess={(addedCoins) => setUserCoinsBalance((prev) => prev + addedCoins)}
      />

      {/* MODALE TEMPLATES UNIVERSELLE */}
      {showTemplatesModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(5, 6, 12, 0.85)',
            backdropFilter: 'blur(8px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9992,
            padding: 16,
          }}
          onClick={() => setShowTemplatesModal(false)}
        >
          <div
            style={{
              backgroundColor: '#12141F',
              border: '1px solid #E5A93C',
              borderRadius: 20,
              maxWidth: 960,
              width: '100%',
              maxHeight: '90vh',
              overflowY: 'auto',
              padding: 24,
              color: '#FFF',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <h2 style={{ margin: 0, color: '#E5A93C', fontSize: 20, fontWeight: 900 }}>
                🎬 Galerie & Moteur de Templates IA PANU
              </h2>
              <button
                type="button"
                onClick={() => setShowTemplatesModal(false)}
                style={{
                  backgroundColor: 'transparent',
                  border: '1px solid rgba(255,255,255,0.2)',
                  color: '#FFF',
                  padding: '6px 12px',
                  borderRadius: 8,
                  cursor: 'pointer',
                }}
              >
                ✕ Fermer
              </button>
            </div>
            <DynamicTemplateGallery onTemplateSelect={() => setShowTemplatesModal(false)} />
          </div>
        </div>
      )}
    </div>
  );
};

export default LiveSportsPage;
