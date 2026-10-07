import React, { useEffect, useState } from 'react';

export interface GiftAnimationData {
  giftType: string;
  icon: string;
  animationType: 'rose_shower' | 'rocket_launch' | 'crown_descent' | 'lion_roar' | 'fireworks_burst' | 'heart_explosion';
  senderName: string;
}

interface FullScreenGiftAnimationProps {
  gift: GiftAnimationData | null;
  onAnimationEnd: () => void;
}

export const FullScreenGiftAnimation: React.FC<FullScreenGiftAnimationProps> = ({
  gift,
  onAnimationEnd,
}) => {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (gift) {
      setVisible(true);
      const timer = setTimeout(() => {
        setVisible(false);
        onAnimationEnd();
      }, 3500);
      return () => clearTimeout(timer);
    }
  }, [gift, onAnimationEnd]);

  if (!gift || !visible) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 9999,
        pointerEvents: 'none',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        backdropFilter: 'blur(2px)',
        backgroundColor: 'rgba(0,0,0,0.25)',
      }}
    >
      <style>{`
        @keyframes rocketFly {
          0% { transform: translateY(120vh) scale(0.6) rotate(-15deg); opacity: 0; }
          20% { opacity: 1; }
          50% { transform: translateY(0vh) scale(1.4) rotate(0deg); }
          80% { opacity: 1; }
          100% { transform: translateY(-120vh) scale(1.8) rotate(15deg); opacity: 0; }
        }
        @keyframes crownDrop {
          0% { transform: translateY(-80vh) scale(0.2) rotate(-30deg); opacity: 0; }
          40% { transform: translateY(10px) scale(1.3) rotate(5deg); opacity: 1; }
          60% { transform: translateY(-10px) scale(1.1) rotate(-2deg); }
          80% { transform: translateY(0) scale(1.2); opacity: 1; filter: drop-shadow(0 0 40px #FFD700); }
          100% { transform: scale(1.5); opacity: 0; }
        }
        @keyframes lionPulse {
          0% { transform: scale(0.3); opacity: 0; filter: drop-shadow(0 0 10px #FF9900); }
          40% { transform: scale(1.4); opacity: 1; filter: drop-shadow(0 0 60px #FF7700); }
          70% { transform: scale(1.25) rotate(-3deg); }
          100% { transform: scale(2); opacity: 0; }
        }
        @keyframes roseRain {
          0% { transform: translateY(-50px) rotate(0deg); opacity: 0; }
          20% { opacity: 1; }
          80% { opacity: 1; }
          100% { transform: translateY(100vh) rotate(360deg); opacity: 0; }
        }
        @keyframes heartBurst {
          0% { transform: scale(0.2); opacity: 0; }
          50% { transform: scale(1.5); opacity: 1; filter: drop-shadow(0 0 50px #FF2E4C); }
          100% { transform: scale(2.2); opacity: 0; }
        }
        @keyframes bannerSlide {
          0% { transform: translateY(60px) scale(0.85); opacity: 0; }
          25% { transform: translateY(0) scale(1.05); opacity: 1; }
          75% { transform: translateY(0) scale(1); opacity: 1; }
          100% { transform: translateY(-40px) scale(0.9); opacity: 0; }
        }
      `}</style>

      {/* 1. ANIMATION SELON LE TYPE DU CADEAU */}
      {gift.animationType === 'rocket_launch' && (
        <div style={{ animation: 'rocketFly 3s cubic-bezier(0.25, 1, 0.5, 1) forwards' }}>
          <div style={{ fontSize: 130, filter: 'drop-shadow(0 0 50px #FF4757)' }}>🚀</div>
          <div style={{ textAlign: 'center', fontSize: 32 }}>🔥💨✨</div>
        </div>
      )}

      {gift.animationType === 'crown_descent' && (
        <div style={{ animation: 'crownDrop 3.2s ease-out forwards', textAlign: 'center' }}>
          <div style={{ fontSize: 140, filter: 'drop-shadow(0 0 60px #FFD700)' }}>👑</div>
          <div style={{ color: '#FFD700', fontSize: 24, fontWeight: 900, textShadow: '0 2px 20px rgba(255,215,0,0.8)' }}>
            HONNEUR ROYAL PANU
          </div>
        </div>
      )}

      {gift.animationType === 'lion_roar' && (
        <div style={{ animation: 'lionPulse 3.2s ease-out forwards', textAlign: 'center' }}>
          <div style={{ fontSize: 140, filter: 'drop-shadow(0 0 70px #FF8C00)' }}>🦁</div>
          <div style={{ color: '#FF9900', fontSize: 26, fontWeight: 900, textTransform: 'uppercase', letterSpacing: 2 }}>
            ROAR D’AFRIQUE !
          </div>
        </div>
      )}

      {gift.animationType === 'rose_shower' && (
        <div style={{ position: 'absolute', inset: 0, overflow: 'hidden' }}>
          {[...Array(14)].map((_, i) => (
            <div
              key={i}
              style={{
                position: 'absolute',
                top: `${(i * -15) - 30}px`,
                left: `${(i * 7.2) % 94}%`,
                fontSize: `${28 + (i % 4) * 14}px`,
                animation: `roseRain ${2.2 + (i % 3) * 0.4}s ease-in forwards`,
                animationDelay: `${(i * 0.12)}s`,
                filter: 'drop-shadow(0 2px 10px rgba(255, 46, 76, 0.6))',
              }}
            >
              {i % 2 === 0 ? '🌹' : '✨'}
            </div>
          ))}
        </div>
      )}

      {gift.animationType === 'heart_explosion' && (
        <div style={{ animation: 'heartBurst 3s ease-out forwards', textAlign: 'center' }}>
          <div style={{ fontSize: 150, filter: 'drop-shadow(0 0 60px #FF2E4C)' }}>💖</div>
          <div style={{ color: '#FF4757', fontSize: 24, fontWeight: 900, textShadow: '0 2px 20px rgba(255,46,76,0.9)' }}>
            AMOUR & SOUTIEN INFINI
          </div>
        </div>
      )}

      {gift.animationType === 'fireworks_burst' && (
        <div style={{ animation: 'heartBurst 3s ease-out forwards', textAlign: 'center' }}>
          <div style={{ fontSize: 140, filter: 'drop-shadow(0 0 60px #2ED573)' }}>🏎️✨</div>
          <div style={{ color: '#2ED573', fontSize: 24, fontWeight: 900, textShadow: '0 2px 20px rgba(46,213,115,0.9)' }}>
            SUPERCAR DÉVERROUILLÉE !
          </div>
        </div>
      )}

      {/* BANNIÈRE CENTRALE DE PROJECTION AVEC LE NOM DE L'ENVOYEUR */}
      <div
        style={{
          marginTop: 24,
          backgroundColor: 'rgba(18, 19, 26, 0.92)',
          border: '2px solid #E5A93C',
          borderRadius: 999,
          padding: '12px 28px',
          display: 'flex',
          alignItems: 'center',
          gap: 14,
          boxShadow: '0 10px 40px rgba(229, 169, 60, 0.5), 0 0 20px rgba(0,0,0,0.8)',
          animation: 'bannerSlide 3.2s ease-in-out forwards',
          zIndex: 10,
        }}
      >
        <span style={{ fontSize: 36 }}>{gift.icon}</span>
        <div>
          <div style={{ color: '#E5A93C', fontWeight: 900, fontSize: 16 }}>
            {gift.senderName || 'Un Spectateur Généreux'}
          </div>
          <div style={{ color: '#FFF', fontSize: 13, fontWeight: 600 }}>
            a envoyé <span style={{ color: '#FFD700', fontWeight: 800 }}>{gift.giftType}</span> !
          </div>
        </div>
      </div>
    </div>
  );
};
