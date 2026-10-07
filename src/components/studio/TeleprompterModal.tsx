import React, { useState, useEffect, useRef } from 'react';

export interface TeleprompterModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const TeleprompterModal: React.FC<TeleprompterModalProps> = ({ isOpen, onClose }) => {
  const [script, setScript] = useState<string>(
    `Bienvenue sur PANU Studio !\n\nAujourd'hui, je vous présente les coulisses de notre nouvelle production vidéo.\n\nRestez bien jusqu'à la fin de la vidéo, car j'ai une surprise exclusive pour toute la communauté !\n\nN'oubliez pas de liker, vous abonner et partager cette vidéo sur vos réseaux.`
  );
  const [isScrolling, setIsScrolling] = useState<boolean>(false);
  const [scrollSpeed, setScrollSpeed] = useState<number>(2); // 1 à 5
  const [fontSize, setFontSize] = useState<number>(26); // 18 à 48px
  const [isMirrored, setIsMirrored] = useState<boolean>(false);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let animationFrameId: number;

    const scrollStep = () => {
      if (isScrolling && scrollContainerRef.current) {
        scrollContainerRef.current.scrollTop += scrollSpeed * 0.75;
        // Si fin atteinte
        if (
          scrollContainerRef.current.scrollTop + scrollContainerRef.current.clientHeight >=
          scrollContainerRef.current.scrollHeight - 5
        ) {
          setIsScrolling(false);
        }
      }
      if (isScrolling) {
        animationFrameId = requestAnimationFrame(scrollStep);
      }
    };

    if (isScrolling) {
      animationFrameId = requestAnimationFrame(scrollStep);
    }
    return () => cancelAnimationFrame(animationFrameId);
  }, [isScrolling, scrollSpeed]);

  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.92)',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        padding: 20,
        color: '#FFF',
      }}
    >
      {/* Barre supérieure */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 24 }}>📜</span>
          <h2 style={{ margin: 0, fontSize: 18, color: '#E5A93C' }}>
            Téléprompteur Studio Face Caméra
          </h2>
        </div>
        <button
          type="button"
          onClick={onClose}
          style={{
            background: 'transparent',
            border: 'none',
            color: '#AAA',
            fontSize: 24,
            cursor: 'pointer',
          }}
        >
          ✕
        </button>
      </div>

      {/* Barre de réglages vitesse, taille, miroir */}
      <div
        style={{
          display: 'flex',
          gap: 16,
          alignItems: 'center',
          flexWrap: 'wrap',
          backgroundColor: '#161922',
          padding: '10px 16px',
          borderRadius: 12,
          marginBottom: 16,
          border: '1px solid #282C3D',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 12, color: '#AAA' }}>Vitesse :</span>
          <input
            type="range"
            min={1}
            max={6}
            step={0.5}
            value={scrollSpeed}
            onChange={(e) => setScrollSpeed(Number(e.target.value))}
            style={{ width: 90, accentColor: '#E5A93C' }}
          />
          <span style={{ fontSize: 12, fontWeight: 700, color: '#E5A93C' }}>{scrollSpeed}x</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 12, color: '#AAA' }}>Taille texte :</span>
          <input
            type="range"
            min={18}
            max={44}
            value={fontSize}
            onChange={(e) => setFontSize(Number(e.target.value))}
            style={{ width: 90, accentColor: '#E5A93C' }}
          />
          <span style={{ fontSize: 12, fontWeight: 700 }}>{fontSize}px</span>
        </div>

        <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={isMirrored}
            onChange={(e) => setIsMirrored(e.target.checked)}
            style={{ accentColor: '#E5A93C' }}
          />
          Miroir (Glace prompteur)
        </label>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
          <button
            type="button"
            onClick={() => {
              if (scrollContainerRef.current) scrollContainerRef.current.scrollTop = 0;
              setIsScrolling(false);
            }}
            style={{
              backgroundColor: '#2A2E40',
              color: '#FFF',
              border: 'none',
              padding: '6px 12px',
              borderRadius: 6,
              cursor: 'pointer',
              fontSize: 12,
            }}
          >
            ⏮ Début
          </button>
          <button
            type="button"
            onClick={() => setIsScrolling(!isScrolling)}
            style={{
              backgroundColor: isScrolling ? '#FF4757' : '#2ED573',
              color: '#000',
              fontWeight: 800,
              border: 'none',
              padding: '6px 16px',
              borderRadius: 6,
              cursor: 'pointer',
              fontSize: 12,
            }}
          >
            {isScrolling ? '⏸ Pause' : '▶ Défiler'}
          </button>
        </div>
      </div>

      {/* Zone de défilement du script */}
      <div
        ref={scrollContainerRef}
        style={{
          flex: 1,
          overflowY: 'auto',
          backgroundColor: '#0A0B0E',
          borderRadius: 14,
          padding: '40px 60px',
          border: '1px solid #232738',
          position: 'relative',
          transform: isMirrored ? 'scaleX(-1)' : 'none',
        }}
      >
        {/* Ligne repère de lecture au milieu */}
        <div
          style={{
            position: 'sticky',
            top: '40%',
            left: 0,
            right: 0,
            height: 2,
            backgroundColor: 'rgba(229, 169, 60, 0.4)',
            pointerEvents: 'none',
            zIndex: 10,
          }}
        />

        <textarea
          value={script}
          onChange={(e) => setScript(e.target.value)}
          style={{
            width: '100%',
            minHeight: '120%',
            background: 'transparent',
            border: 'none',
            color: '#FFF',
            fontSize: `${fontSize}px`,
            lineHeight: 1.7,
            fontFamily: 'system-ui, sans-serif',
            resize: 'none',
            outline: 'none',
            textAlign: 'center',
          }}
        />
      </div>
    </div>
  );
};
