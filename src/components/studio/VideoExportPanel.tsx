import React, { useState, useMemo } from 'react';

export type VideoResolution = '480p' | '720p' | '1080p' | '2K' | '4K';
export type VideoFramerate = 24 | 25 | 30 | 50 | 60;
export type VideoCodec = 'H.264 (MP4)' | 'H.265 (HEVC)' | 'ProRes 422' | 'VP9 / WebM';

export interface VideoExportSettings {
  resolution: VideoResolution;
  framerate: VideoFramerate;
  bitrateMbps: number;
  codec: VideoCodec;
  enableAiUltraHd: boolean;
  enableOpticalFlow: boolean;
  enableAiDenoise: boolean;
  durationSeconds: number;
}

export interface VideoExportPanelProps {
  isOpen?: boolean;
  videoDurationSeconds?: number;
  onClose?: () => void;
  onStartExport?: (settings: VideoExportSettings) => void;
}

/**
 * 2. MODULE D'EXPORTATION & AMÉLIORATION PAR IA (STYLE CAPCUT PRO)
 * - Résolution : 480p, 720p, 1080p, 2K, 4K
 * - Framerate : 24, 25, 30, 50, 60 fps
 * - Bitrate variable : 5 Mbit/s à 100 Mbit/s avec slider
 * - Calcul dynamique de la taille estimée du fichier avant export
 * - Toggles IA : Ultra HD par IA (Super-résolution) et Flux Optique (Interpolation fluide)
 */
export const VideoExportPanel: React.FC<VideoExportPanelProps> = ({
  isOpen = true,
  videoDurationSeconds = 60,
  onClose,
  onStartExport,
}) => {
  const [resolution, setResolution] = useState<VideoResolution>('1080p');
  const [framerate, setFramerate] = useState<VideoFramerate>(30);
  const [bitrateMbps, setBitrateMbps] = useState<number>(20);
  const [codec, setCodec] = useState<VideoCodec>('H.264 (MP4)');
  const [enableAiUltraHd, setEnableAiUltraHd] = useState<boolean>(false);
  const [enableOpticalFlow, setEnableOpticalFlow] = useState<boolean>(false);
  const [enableAiDenoise, setEnableAiDenoise] = useState<boolean>(false);

  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [exportProgress, setExportProgress] = useState<number>(0);
  const [exportStatusText, setExportStatusText] = useState<string>('');

  // Recommandation automatique de bitrate selon la résolution
  const recommendedBitrate = useMemo(() => {
    switch (resolution) {
      case '480p': return 6;
      case '720p': return 12;
      case '1080p': return 20;
      case '2K': return 35;
      case '4K': return 60;
      default: return 20;
    }
  }, [resolution]);

  // Calcul dynamique de la taille estimée en Mégaoctets (Mo) et Gigaoctets (Go)
  const estimatedFileSize = useMemo(() => {
    // Débit vidéo : (Mbit/s / 8) * Durée(s)
    const videoSizeMB = (bitrateMbps / 8) * videoDurationSeconds;
    // Débit audio stéréo standard 320 kbps : (0.320 Mbit/s / 8) * Durée(s)
    const audioSizeMB = (0.32 / 8) * videoDurationSeconds;
    // Métadonnées & index MP4 (~2%)
    const totalMB = (videoSizeMB + audioSizeMB) * 1.02;

    if (totalMB >= 1024) {
      return `${(totalMB / 1024).toFixed(2)} Go`;
    }
    return `${totalMB.toFixed(1)} Mo`;
  }, [bitrateMbps, videoDurationSeconds]);

  // Estimation du temps d'encodage selon les options IA activées
  const estimatedEncodingTime = useMemo(() => {
    let speedFactor = 1.0;
    if (enableAiUltraHd) speedFactor += 1.8;
    if (enableOpticalFlow) speedFactor += 1.2;
    if (enableAiDenoise) speedFactor += 0.8;
    if (resolution === '4K') speedFactor *= 2.2;
    if (resolution === '2K') speedFactor *= 1.5;

    const seconds = Math.round(videoDurationSeconds * speedFactor * 0.4);
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return m > 0 ? `${m} min ${s} s` : `${s} secondes`;
  }, [videoDurationSeconds, enableAiUltraHd, enableOpticalFlow, enableAiDenoise, resolution]);

  const handleStartExport = () => {
    const settings: VideoExportSettings = {
      resolution,
      framerate,
      bitrateMbps,
      codec,
      enableAiUltraHd,
      enableOpticalFlow,
      enableAiDenoise,
      durationSeconds: videoDurationSeconds,
    };

    setIsExporting(true);
    setExportProgress(5);
    setExportStatusText(
      enableAiUltraHd
        ? '🤖 Initialisation de l’Upscaling Ultra HD par l’IA...'
        : '🎬 Rendu des calques et compression vidéo...'
    );

    // Simulation progressive du rendu professionnel WebCodecs / FFmpeg.wasm
    const interval = setInterval(() => {
      setExportProgress((prev) => {
        if (prev >= 95) {
          clearInterval(interval);
          setTimeout(() => {
            setIsExporting(false);
            setExportProgress(100);
            setExportStatusText('✅ Vidéo exportée avec succès !');
            onStartExport?.(settings);
          }, 600);
          return 98;
        }
        if (prev > 30 && enableOpticalFlow && prev < 55) {
          setExportStatusText('⚡ Interpolation des trames par flux optique (60 FPS)...');
        } else if (prev > 60 && prev < 85) {
          setExportStatusText(`📦 Multiplexage ${codec} & Finalisation MP4...`);
        }
        return prev + Math.floor(Math.random() * 8 + 4);
      });
    }, 280);
  };

  const resolutions: { key: VideoResolution; label: string; desc: string }[] = [
    { key: '480p', label: '480p SD', desc: '854×480 • Fichier très léger' },
    { key: '720p', label: '720p HD', desc: '1280×720 • Standard Web & PWA' },
    { key: '1080p', label: '1080p FHD', desc: '1920×1080 • Recommandé TikTok/Reels' },
    { key: '2K', label: '2K QHD', desc: '2560×1440 • Haute définition écran Retina' },
    { key: '4K', label: '4K UHD', desc: '3840×2160 • Qualité Cinéma Ultra 8K Ready' },
  ];

  const framerates: VideoFramerate[] = [24, 25, 30, 50, 60];

  return (
    <div
      style={{
        backgroundColor: '#12141D',
        borderRadius: 18,
        border: '1px solid #2B2F42',
        color: '#FFF',
        padding: 24,
        boxShadow: '0 16px 40px rgba(0,0,0,0.6)',
        maxWidth: 620,
        width: '100%',
      }}
    >
      {/* En-tête */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 20 }}>🚀</span>
            <h2 style={{ margin: 0, fontSize: 18, color: '#E5A93C', fontWeight: 800 }}>
              Exportation & Amélioration Vidéo IA
            </h2>
          </div>
          <p style={{ margin: '4px 0 0', fontSize: 12, color: '#888F9E' }}>
            Paramètres d'encodage studio style CapCut • Durée du projet : {videoDurationSeconds}s
          </p>
        </div>
        {onClose && (
          <button
            type="button"
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#888F9E',
              fontSize: 22,
              cursor: 'pointer',
            }}
          >
            ✕
          </button>
        )}
      </div>

      {/* 1. Résolution */}
      <div style={{ marginBottom: 18 }}>
        <label style={{ display: 'block', fontSize: 13, fontWeight: 700, color: '#CCC', marginBottom: 8 }}>
          📐 Résolution Vidéo
        </label>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(100px, 1fr))', gap: 8 }}>
          {resolutions.map((r) => {
            const isSelected = resolution === r.key;
            return (
              <button
                key={r.key}
                type="button"
                onClick={() => {
                  setResolution(r.key);
                  setBitrateMbps(
                    r.key === '480p' ? 6 : r.key === '720p' ? 12 : r.key === '1080p' ? 20 : r.key === '2K' ? 35 : 60
                  );
                }}
                style={{
                  backgroundColor: isSelected ? 'rgba(229, 169, 60, 0.18)' : '#191C28',
                  border: isSelected ? '2px solid #E5A93C' : '1px solid #2B2F42',
                  borderRadius: 10,
                  padding: '10px 8px',
                  color: isSelected ? '#E5A93C' : '#FFF',
                  fontWeight: isSelected ? 800 : 600,
                  fontSize: 12,
                  cursor: 'pointer',
                  textAlign: 'center',
                }}
              >
                <div>{r.label}</div>
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Fréquence d'images (FPS) */}
      <div style={{ marginBottom: 18 }}>
        <label style={{ display: 'block', fontSize: 13, fontWeight: 700, color: '#CCC', marginBottom: 8 }}>
          ⏱️ Fréquence d'Images (Framerate)
        </label>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {framerates.map((fps) => {
            const isSelected = framerate === fps;
            return (
              <button
                key={fps}
                type="button"
                onClick={() => setFramerate(fps)}
                style={{
                  flex: 1,
                  minWidth: 60,
                  backgroundColor: isSelected ? '#E5A93C' : '#191C28',
                  border: isSelected ? '1px solid #E5A93C' : '1px solid #2B2F42',
                  borderRadius: 8,
                  padding: '8px 10px',
                  color: isSelected ? '#000' : '#FFF',
                  fontWeight: 800,
                  fontSize: 13,
                  cursor: 'pointer',
                }}
              >
                {fps} fps
              </button>
            );
          })}
        </div>
      </div>

      {/* 3. Débit Binaire (Bitrate Slider) & Calculateur Dynamique */}
      <div style={{ marginBottom: 20, backgroundColor: '#181B26', padding: 14, borderRadius: 12, border: '1px solid #282C3D' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <label style={{ fontSize: 13, fontWeight: 700, color: '#FFF' }}>
            📊 Débit Binaire (Bitrate) : <span style={{ color: '#E5A93C' }}>{bitrateMbps} Mbit/s</span>
          </label>
          <span style={{ fontSize: 11, color: '#888F9E' }}>
            Recommandé pour {resolution} : <strong>{recommendedBitrate} Mbit/s</strong>
          </span>
        </div>
        <input
          type="range"
          min={5}
          max={100}
          step={1}
          value={bitrateMbps}
          onChange={(e) => setBitrateMbps(Number(e.target.value))}
          style={{
            width: '100%',
            accentColor: '#E5A93C',
            cursor: 'pointer',
          }}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, color: '#6A7182', marginTop: 4 }}>
          <span>5 Mbit/s (Ultra compact)</span>
          <span>50 Mbit/s (Haute fidélité)</span>
          <span>100 Mbit/s (Master Studio)</span>
        </div>

        {/* Encadré d'estimation dynamique en temps réel */}
        <div
          style={{
            marginTop: 14,
            padding: 10,
            borderRadius: 8,
            backgroundColor: 'rgba(229, 169, 60, 0.08)',
            border: '1px solid rgba(229, 169, 60, 0.25)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <div>
            <div style={{ fontSize: 11, color: '#888F9E' }}>Taille finale estimée du fichier :</div>
            <div style={{ fontSize: 18, fontWeight: 900, color: '#2ED573', marginTop: 2 }}>
              💾 {estimatedFileSize}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 11, color: '#888F9E' }}>Temps d'encodage estimé :</div>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#E5A93C', marginTop: 2 }}>
              ⏳ ~{estimatedEncodingTime}
            </div>
          </div>
        </div>
      </div>

      {/* 4. Options d'Amélioration par IA */}
      <div style={{ marginBottom: 22 }}>
        <label style={{ display: 'block', fontSize: 13, fontWeight: 700, color: '#CCC', marginBottom: 10 }}>
          ✨ Amélioration et Traitement par l'IA
        </label>
        <div style={{ display: 'grid', gap: 10 }}>
          {/* Toggle 1 : Ultra HD par l'IA */}
          <div
            onClick={() => setEnableAiUltraHd(!enableAiUltraHd)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: 12,
              borderRadius: 10,
              backgroundColor: enableAiUltraHd ? 'rgba(46, 213, 115, 0.12)' : '#181B26',
              border: enableAiUltraHd ? '1px solid #2ED573' : '1px solid #2B2F42',
              cursor: 'pointer',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <span style={{ fontSize: 20 }}>🔬</span>
              <div>
                <div style={{ fontSize: 13, fontWeight: 700, color: enableAiUltraHd ? '#2ED573' : '#FFF' }}>
                  Ultra HD par l'IA (Super-résolution)
                </div>
                <div style={{ fontSize: 11, color: '#888F9E' }}>
                  Rehausse les détails et reconstruit les textures fines via modèle neural
                </div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={enableAiUltraHd}
              onChange={() => {}}
              style={{ accentColor: '#2ED573', transform: 'scale(1.2)' }}
            />
          </div>

          {/* Toggle 2 : Flux optique */}
          <div
            onClick={() => setEnableOpticalFlow(!enableOpticalFlow)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: 12,
              borderRadius: 10,
              backgroundColor: enableOpticalFlow ? 'rgba(229, 169, 60, 0.12)' : '#181B26',
              border: enableOpticalFlow ? '1px solid #E5A93C' : '1px solid #2B2F42',
              cursor: 'pointer',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <span style={{ fontSize: 20 }}>⚡</span>
              <div>
                <div style={{ fontSize: 13, fontWeight: 700, color: enableOpticalFlow ? '#E5A93C' : '#FFF' }}>
                  Flux Optique (Interpolation fluide 60 FPS)
                </div>
                <div style={{ fontSize: 11, color: '#888F9E' }}>
                  Génère des images intermédiaires pour des mouvements ultra fluides sans saccades
                </div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={enableOpticalFlow}
              onChange={() => {}}
              style={{ accentColor: '#E5A93C', transform: 'scale(1.2)' }}
            />
          </div>
        </div>
      </div>

      {/* Barre de progression pendant l'exportation */}
      {isExporting && (
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 6 }}>
            <span style={{ color: '#E5A93C', fontWeight: 700 }}>{exportStatusText}</span>
            <span style={{ fontWeight: 800 }}>{exportProgress}%</span>
          </div>
          <div style={{ width: '100%', height: 8, backgroundColor: '#202434', borderRadius: 999, overflow: 'hidden' }}>
            <div
              style={{
                width: `${exportProgress}%`,
                height: '100%',
                backgroundColor: '#2ED573',
                transition: 'width 0.25s ease',
              }}
            />
          </div>
        </div>
      )}

      {/* Bouton d'Action */}
      <button
        type="button"
        disabled={isExporting}
        onClick={handleStartExport}
        style={{
          width: '100%',
          backgroundColor: isExporting ? '#333748' : '#E5A93C',
          color: isExporting ? '#888' : '#000',
          border: 'none',
          borderRadius: 12,
          padding: '14px 20px',
          fontWeight: 800,
          fontSize: 14,
          cursor: isExporting ? 'not-allowed' : 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 10,
          transition: 'all 0.2s ease',
        }}
      >
        {isExporting ? (
          <>⏳ Encodage en cours ({exportProgress}%)...</>
        ) : (
          <>
            <span>🚀 Exporter le Projet en {resolution}</span>
            <span style={{ opacity: 0.8, fontSize: 12 }}>({estimatedFileSize})</span>
          </>
        )}
      </button>
    </div>
  );
};
