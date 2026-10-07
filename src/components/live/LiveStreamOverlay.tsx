import React, { useState } from 'react';
import { useLiveStream, UseLiveStreamOptions } from '../../hooks/useLiveStream';

interface LiveStreamOverlayProps extends UseLiveStreamOptions {
  hostName: string;
  hostId?: string;
  onRequireAuth?: () => void;
}

const LIVE_GIFTS_CATALOG = [
  { type: 'Rose d’Or', icon: '🌹', cost: 10 },
  { type: 'Cœur Diamant', icon: '💎', cost: 50 },
  { type: 'Couronne Royale PANU', icon: '👑', cost: 250 },
];

/**
 * Surcouche UI modulaire à placer directement dans le conteneur du lecteur vidéo existant :
 * - Badge "EN DIRECT" dynamique et clignotant
 * - Compteur de spectateurs connectés en temps réel (Supabase Presence)
 * - Overlay de Chat en direct au-dessus de la vidéo
 * - Bouton "Cadeau" relié aux transactions de jetons en temps réel
 */
export const LiveStreamOverlay: React.FC<LiveStreamOverlayProps> = ({
  hostName,
  hostId,
  onRequireAuth,
  ...streamOptions
}) => {
  const {
    videoRef,
    isLive,
    viewerCount,
    messages,
    recentGifts,
    tokenBalance,
    error,
    sendChatMessage,
    sendGiftTransaction,
  } = useLiveStream(streamOptions);

  const [chatInput, setChatInput] = useState('');
  const [showGiftDrawer, setShowGiftDrawer] = useState(false);
  const [isSendingGift, setIsSendingGift] = useState(false);

  const handleSendChat = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!streamOptions.currentUser) {
      onRequireAuth?.();
      return;
    }
    if (!chatInput.trim()) return;
    await sendChatMessage(chatInput);
    setChatInput('');
  };

  const handleSendGift = async (gift: { type: string; icon: string; cost: number }) => {
    if (!streamOptions.currentUser) {
      onRequireAuth?.();
      return;
    }
    try {
      setIsSendingGift(true);
      await sendGiftTransaction({
        receiverId: hostId,
        receiverName: hostName,
        giftType: gift.type,
        giftIcon: gift.icon,
        creditsCost: gift.cost,
      });
      setShowGiftDrawer(false);
    } catch {
      // L'erreur est déjà remontée dans `error` par useLiveStream
    } finally {
      setIsSendingGift(false);
    }
  };

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%', backgroundColor: '#0D0E12', overflow: 'hidden' }}>
      {/* Lecteur Vidéo WebRTC / HLS */}
      <video
        ref={videoRef}
        playsInline
        autoPlay
        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
      />

      {/* 1. BADGE "EN DIRECT" CLIGNOTANT & COMPTEUR DE SPECTATEURS TEMPS RÉEL */}
      <div
        style={{
          position: 'absolute',
          top: 16,
          left: 16,
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          zIndex: 10,
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            backgroundColor: isLive ? '#FF2E4C' : '#555',
            color: '#FFF',
            padding: '6px 12px',
            borderRadius: 999,
            fontWeight: 800,
            fontSize: 12,
            letterSpacing: 0.6,
            boxShadow: '0 4px 14px rgba(255, 46, 76, 0.45)',
          }}
        >
          <span
            style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              backgroundColor: '#FFF',
              animation: isLive ? 'panuPulse 1.2s infinite ease-in-out' : 'none',
            }}
          />
          <span>EN DIRECT</span>
        </div>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            backgroundColor: 'rgba(0, 0, 0, 0.65)',
            border: '1px solid rgba(229, 169, 60, 0.45)',
            color: '#FFF',
            padding: '6px 12px',
            borderRadius: 999,
            fontWeight: 700,
            fontSize: 12,
          }}
        >
          <span>👁️ {viewerCount} spectateur{viewerCount > 1 ? 's' : ''}</span>
        </div>
      </div>

      {/* Animation des Cadeaux reçus en temps réel */}
      <div
        style={{
          position: 'absolute',
          top: 68,
          left: 16,
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
          zIndex: 10,
          pointerEvents: 'none',
        }}
      >
        {recentGifts.map((g) => (
          <div
            key={g.id}
            style={{
              backgroundColor: 'rgba(24, 25, 32, 0.88)',
              border: '1px solid #E5A93C',
              borderRadius: 12,
              padding: '6px 12px',
              color: '#FFF',
              fontSize: 12,
              fontWeight: 700,
            }}
          >
            <span style={{ fontSize: 16, marginRight: 6 }}>{g.giftIcon}</span>
            <strong style={{ color: '#E5A93C' }}>{g.senderName}</strong> a offert{' '}
            <span>{g.giftType}</span> ({g.creditsAmount} jetons)
          </div>
        ))}
      </div>

      {/* 2. OVERLAY DE CHAT EN DIRECT + BOUTON CADEAU */}
      <div
        style={{
          position: 'absolute',
          bottom: 16,
          left: 16,
          right: 16,
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
          zIndex: 10,
        }}
      >
        {/* Liste défilante des messages en direct */}
        <div
          style={{
            maxHeight: 200,
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: 6,
            paddingRight: 8,
          }}
        >
          {messages.map((msg) => (
            <div
              key={msg.id}
              style={{
                alignSelf: 'flex-start',
                backgroundColor: 'rgba(0, 0, 0, 0.58)',
                padding: '6px 10px',
                borderRadius: 10,
                color: '#FFF',
                fontSize: 13,
              }}
            >
              <strong style={{ color: '#E5A93C', marginRight: 6 }}>{msg.username}:</strong>
              <span>{msg.message}</span>
            </div>
          ))}
        </div>

        {error && (
          <div style={{ color: '#FF6B81', fontSize: 12, fontWeight: 600 }}>{error}</div>
        )}

        {/* Tiroir de sélection de Cadeau connecté aux transactions de jetons */}
        {showGiftDrawer && (
          <div
            style={{
              backgroundColor: 'rgba(18, 19, 24, 0.96)',
              border: '1px solid #E5A93C',
              borderRadius: 14,
              padding: 12,
              display: 'flex',
              justifyContent: 'space-around',
              alignItems: 'center',
            }}
          >
            {LIVE_GIFTS_CATALOG.map((gift) => (
              <button
                key={gift.type}
                disabled={isSendingGift}
                onClick={() => handleSendGift(gift)}
                style={{
                  background: 'rgba(229, 169, 60, 0.14)',
                  border: '1px solid rgba(229, 169, 60, 0.5)',
                  borderRadius: 10,
                  padding: '8px 12px',
                  color: '#FFF',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 4,
                }}
              >
                <span style={{ fontSize: 22 }}>{gift.icon}</span>
                <span style={{ fontSize: 11, fontWeight: 700 }}>{gift.type}</span>
                <span style={{ fontSize: 10, color: '#E5A93C' }}>🪙 {gift.cost} jetons</span>
              </button>
            ))}
          </div>
        )}

        {/* Barre de saisie Chat + Bouton Cadeau */}
        <form onSubmit={handleSendChat} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <input
            type="text"
            value={chatInput}
            onChange={(e) => setChatInput(e.target.value)}
            placeholder="Envoyer un message en direct..."
            style={{
              flex: 1,
              backgroundColor: 'rgba(0, 0, 0, 0.65)',
              border: '1px solid rgba(255, 255, 255, 0.25)',
              borderRadius: 999,
              padding: '10px 16px',
              color: '#FFF',
              fontSize: 13,
              outline: 'none',
            }}
          />
          <button
            type="submit"
            style={{
              backgroundColor: '#E5A93C',
              color: '#0D0E12',
              border: 'none',
              borderRadius: 999,
              padding: '10px 16px',
              fontWeight: 800,
              fontSize: 13,
              cursor: 'pointer',
            }}
          >
            Envoyer
          </button>
          <button
            type="button"
            onClick={() => setShowGiftDrawer((prev) => !prev)}
            style={{
              backgroundColor: 'rgba(229, 169, 60, 0.2)',
              border: '1px solid #E5A93C',
              color: '#E5A93C',
              borderRadius: 999,
              padding: '10px 14px',
              fontWeight: 800,
              fontSize: 13,
              cursor: 'pointer',
            }}
          >
            🎁 Cadeau {tokenBalance !== null ? `(${tokenBalance})` : ''}
          </button>
        </form>
      </div>

      <style>{`
        @keyframes panuPulse {
          0%, 100% { opacity: 1; transform: scale(1); }
          50% { opacity: 0.25; transform: scale(0.85); }
        }
      `}</style>
    </div>
  );
};
