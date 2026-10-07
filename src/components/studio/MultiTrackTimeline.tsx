import React, { useState, useRef } from 'react';

export interface TimelineClip {
  id: string;
  title: string;
  startTime: number; // en secondes
  duration: number; // en secondes
  color?: string;
  thumbnailUrl?: string;
}

export interface MultiTrackTimelineProps {
  currentTime: number;
  totalDuration: number;
  isPlaying: boolean;
  onSeek: (time: number) => void;
  onTogglePlay: () => void;
  onSplitCurrentClip?: () => void;
}

/**
 * 1.1 TIMELINE INTERACTIVE MULTI-PISTES (STYLE CAPCUT / PREMIERE PRO PWA)
 * - Piste 1 : Vidéo principale (Main video track)
 * - Piste 2 : Superposition (PIP / Overlay)
 * - Piste 3 : Contenu Audio (Musique, SFX, Voix-off)
 * - Piste 4 : Incrustation de Texte & Légendes
 */
export const MultiTrackTimeline: React.FC<MultiTrackTimelineProps> = ({
  currentTime,
  totalDuration,
  isPlaying,
  onSeek,
  onTogglePlay,
  onSplitCurrentClip,
}) => {
  const [zoomLevel, setZoomLevel] = useState<number>(1); // 0.5x à 3x
  const timelineRef = useRef<HTMLDivElement>(null);

  // Clips initiaux pour démo interactive
  const [mainVideoClips, setMainVideoClips] = useState<TimelineClip[]>([
    { id: 'v1', title: 'Intro Afro-Beats', startTime: 0, duration: 18, color: '#3A4A7C' },
    { id: 'v2', title: 'Scène Métropole 4K', startTime: 18, duration: 24, color: '#4A3A7C' },
    { id: 'v3', title: 'Outro PANU Studio', startTime: 42, duration: 18, color: '#3A7C5E' },
  ]);

  const [overlayClips] = useState<TimelineClip[]>([
    { id: 'ov1', title: 'B-Roll Drone', startTime: 8, duration: 12, color: '#8E44AD' },
    { id: 'ov2', title: 'Logo Animé', startTime: 35, duration: 10, color: '#9B59B6' },
  ]);

  const [audioClips] = useState<TimelineClip[]>([
    { id: 'a1', title: 'Beat Amapiano Hit (120 BPM)', startTime: 0, duration: 50, color: '#27AE60' },
    { id: 'a2', title: 'Voix-off Studio IA', startTime: 5, duration: 25, color: '#2ECC71' },
  ]);

  const [textClips] = useState<TimelineClip[]>([
    { id: 't1', title: '✨ Titre Viral 3D', startTime: 2, duration: 6, color: '#E5A93C' },
    { id: 't2', title: '💬 Sous-titres Auto (Karaoké)', startTime: 10, duration: 30, color: '#F39C12' },
  ]);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    const ms = Math.floor((seconds % 1) * 10);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}.${ms}`;
  };

  const handleTimelineClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!timelineRef.current) return;
    const rect = timelineRef.current.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const percentage = Math.max(0, Math.min(1, clickX / rect.width));
    onSeek(percentage * totalDuration);
  };

  const playheadPercent = (currentTime / totalDuration) * 100;

  return (
    <div
      style={{
        backgroundColor: '#0D0E15',
        border: '1px solid #252838',
        borderRadius: 16,
        padding: 14,
        color: '#FFF',
        display: 'flex',
        flexDirection: 'column',
        gap: 10,
      }}
    >
      {/* Barre de contrôle supérieure de la Timeline */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
        {/* Contrôles de lecture */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <button
            type="button"
            onClick={() => onSeek(0)}
            title="Revenir au début"
            style={{
              backgroundColor: '#1C1F2E',
              border: '1px solid #2C3044',
              borderRadius: 8,
              padding: '6px 10px',
              color: '#FFF',
              cursor: 'pointer',
              fontSize: 12,
            }}
          >
            ⏮️
          </button>
          <button
            type="button"
            onClick={onTogglePlay}
            style={{
              backgroundColor: '#E5A93C',
              color: '#000',
              border: 'none',
              borderRadius: 8,
              padding: '6px 14px',
              fontWeight: 800,
              fontSize: 13,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            {isPlaying ? '⏸️ Pause' : '▶️ Lecture'}
          </button>
          <button
            type="button"
            onClick={onSplitCurrentClip}
            title="Scinder le clip à la tête de lecture"
            style={{
              backgroundColor: '#1C1F2E',
              border: '1px solid #2C3044',
              borderRadius: 8,
              padding: '6px 10px',
              color: '#FFF',
              cursor: 'pointer',
              fontSize: 12,
              fontWeight: 700,
            }}
          >
            ✂️ Split
          </button>
        </div>

        {/* Compteur de temps */}
        <div
          style={{
            fontFamily: 'monospace',
            fontSize: 13,
            fontWeight: 800,
            backgroundColor: '#151722',
            padding: '4px 12px',
            borderRadius: 6,
            border: '1px solid #252838',
            color: '#2ED573',
          }}
        >
          {formatTime(currentTime)} / {formatTime(totalDuration)}
        </div>

        {/* Zoom Timeline */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, color: '#888' }}>
          <span>Zoom</span>
          <button
            type="button"
            onClick={() => setZoomLevel((z) => Math.max(0.6, z - 0.2))}
            style={{ background: '#1C1F2E', border: '1px solid #2C3044', color: '#FFF', borderRadius: 4, padding: '2px 8px', cursor: 'pointer' }}
          >
            -
          </button>
          <span style={{ color: '#E5A93C', fontWeight: 700 }}>{Math.round(zoomLevel * 100)}%</span>
          <button
            type="button"
            onClick={() => setZoomLevel((z) => Math.min(2.5, z + 0.2))}
            style={{ background: '#1C1F2E', border: '1px solid #2C3044', color: '#FFF', borderRadius: 4, padding: '2px 8px', cursor: 'pointer' }}
          >
            +
          </button>
        </div>
      </div>

      {/* Zone des Pistes Multiples avec Tête de Lecture */}
      <div
        ref={timelineRef}
        onClick={handleTimelineClick}
        style={{
          position: 'relative',
          backgroundColor: '#12141F',
          borderRadius: 10,
          border: '1px solid #222636',
          padding: '10px 0',
          cursor: 'pointer',
          userSelect: 'none',
          overflowX: 'hidden',
        }}
      >
        {/* Tête de Lecture (Playhead) */}
        <div
          style={{
            position: 'absolute',
            top: 0,
            bottom: 0,
            left: `${playheadPercent}%`,
            width: 2,
            backgroundColor: '#E5A93C',
            zIndex: 10,
            pointerEvents: 'none',
            boxShadow: '0 0 8px #E5A93C',
          }}
        >
          <div
            style={{
              position: 'absolute',
              top: 0,
              left: -5,
              width: 12,
              height: 12,
              backgroundColor: '#E5A93C',
              transform: 'rotate(45deg)',
              borderRadius: 2,
            }}
          />
        </div>

        {/* Graduations Temporelles */}
        <div style={{ height: 18, borderBottom: '1px solid #1C2030', display: 'flex', justifyContent: 'space-between', padding: '0 8px', fontSize: 10, color: '#555A6E' }}>
          <span>00:00</span>
          <span>00:15</span>
          <span>00:30</span>
          <span>00:45</span>
          <span>01:00</span>
        </div>

        {/* PISTE 1 : VIDÉO PRINCIPALE */}
        <div style={{ margin: '8px 0', padding: '0 8px' }}>
          <div style={{ fontSize: 10, fontWeight: 700, color: '#7E8599', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
            <span>🎬 PISTE VIDÉO PRINCIPALE</span>
          </div>
          <div style={{ position: 'relative', height: 42, backgroundColor: '#1A1D2B', borderRadius: 6, overflow: 'hidden' }}>
            {mainVideoClips.map((c) => {
              const leftPercent = (c.startTime / totalDuration) * 100;
              const widthPercent = (c.duration / totalDuration) * 100;
              return (
                <div
                  key={c.id}
                  style={{
                    position: 'absolute',
                    left: `${leftPercent}%`,
                    width: `${widthPercent}%`,
                    top: 2,
                    bottom: 2,
                    backgroundColor: c.color,
                    borderRadius: 4,
                    border: '1px solid rgba(255,255,255,0.2)',
                    display: 'flex',
                    alignItems: 'center',
                    padding: '0 8px',
                    fontSize: 11,
                    fontWeight: 700,
                    overflow: 'hidden',
                    whiteSpace: 'nowrap',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {c.title}
                </div>
              );
            })}
          </div>
        </div>

        {/* PISTE 2 : SUPERPOSITION (PIP / OVERLAY) */}
        <div style={{ margin: '8px 0', padding: '0 8px' }}>
          <div style={{ fontSize: 10, fontWeight: 700, color: '#7E8599', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
            <span>🪟 SUPERPOSITION (PIP / B-ROLL)</span>
          </div>
          <div style={{ position: 'relative', height: 32, backgroundColor: '#161926', borderRadius: 6, overflow: 'hidden' }}>
            {overlayClips.map((c) => {
              const leftPercent = (c.startTime / totalDuration) * 100;
              const widthPercent = (c.duration / totalDuration) * 100;
              return (
                <div
                  key={c.id}
                  style={{
                    position: 'absolute',
                    left: `${leftPercent}%`,
                    width: `${widthPercent}%`,
                    top: 2,
                    bottom: 2,
                    backgroundColor: c.color,
                    borderRadius: 4,
                    display: 'flex',
                    alignItems: 'center',
                    padding: '0 6px',
                    fontSize: 10,
                    fontWeight: 700,
                    overflow: 'hidden',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {c.title}
                </div>
              );
            })}
          </div>
        </div>

        {/* PISTE 3 : AUDIO (MUSIQUE & VOIX-OFF) */}
        <div style={{ margin: '8px 0', padding: '0 8px' }}>
          <div style={{ fontSize: 10, fontWeight: 700, color: '#7E8599', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
            <span>🎵 AUDIO & VOIX-OFF</span>
          </div>
          <div style={{ position: 'relative', height: 32, backgroundColor: '#141824', borderRadius: 6, overflow: 'hidden' }}>
            {audioClips.map((c) => {
              const leftPercent = (c.startTime / totalDuration) * 100;
              const widthPercent = (c.duration / totalDuration) * 100;
              return (
                <div
                  key={c.id}
                  style={{
                    position: 'absolute',
                    left: `${leftPercent}%`,
                    width: `${widthPercent}%`,
                    top: 2,
                    bottom: 2,
                    backgroundColor: c.color,
                    borderRadius: 4,
                    display: 'flex',
                    alignItems: 'center',
                    padding: '0 6px',
                    fontSize: 10,
                    fontWeight: 700,
                    overflow: 'hidden',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {c.title}
                </div>
              );
            })}
          </div>
        </div>

        {/* PISTE 4 : TEXTE & SOUS-TITRES */}
        <div style={{ margin: '8px 0', padding: '0 8px' }}>
          <div style={{ fontSize: 10, fontWeight: 700, color: '#7E8599', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
            <span>💬 TEXTE & SOUS-TITRES IA</span>
          </div>
          <div style={{ position: 'relative', height: 28, backgroundColor: '#121520', borderRadius: 6, overflow: 'hidden' }}>
            {textClips.map((c) => {
              const leftPercent = (c.startTime / totalDuration) * 100;
              const widthPercent = (c.duration / totalDuration) * 100;
              return (
                <div
                  key={c.id}
                  style={{
                    position: 'absolute',
                    left: `${leftPercent}%`,
                    width: `${widthPercent}%`,
                    top: 2,
                    bottom: 2,
                    backgroundColor: c.color,
                    color: '#000',
                    borderRadius: 4,
                    display: 'flex',
                    alignItems: 'center',
                    padding: '0 6px',
                    fontSize: 10,
                    fontWeight: 800,
                    overflow: 'hidden',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {c.title}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};
