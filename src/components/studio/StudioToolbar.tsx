import React, { useState } from 'react';

export type StudioToolId =
  | 'cut'
  | 'filters'
  | 'adjust'
  | 'stickers'
  | 'aspect_ratio'
  | 'background'
  | 'cover'
  | 'subtitles'
  | 'teleprompter'
  | 'export';

export type AspectRatioType = '9:16' | '16:9' | '1:1' | '4:5' | '21:9';

export interface VideoAdjustments {
  brightness: number; // -50 to +50
  contrast: number; // -50 to +50
  saturation: number; // -50 to +50
  temperature: number; // -50 to +50
  vignette: number; // 0 to 100
  sharpness: number; // 0 to 100
}

export interface StudioToolbarProps {
  activeTool: StudioToolId | null;
  onSelectTool: (tool: StudioToolId | null) => void;
  aspectRatio: AspectRatioType;
  onChangeAspectRatio: (ratio: AspectRatioType) => void;
  adjustments: VideoAdjustments;
  onChangeAdjustments: (adjustments: VideoAdjustments) => void;
  activeFilter: string;
  onSelectFilter: (filterId: string) => void;
  backgroundType: 'blur' | 'color' | 'gradient' | 'image';
  backgroundColor: string;
  onChangeBackground: (type: 'blur' | 'color' | 'gradient' | 'image', value: string) => void;
  coverImageUrl?: string | null;
  onSelectCoverImage: () => void;
  onAutoGenerateSubtitles: () => void;
  isGeneratingSubtitles?: boolean;
  onOpenTeleprompter: () => void;
  onOpenExportPanel: () => void;
}

/**
 * 1.2 BARRE D'OUTILS DE MONTAGE STUDIO IA (STYLE CAPCUT / TIKTOK STUDIO)
 * Découpage (Cut), Filtres, Ajustements, Stickers, Format 9:16/16:9, Arrière-plan,
 * Image de couverture, Sous-titres automatiques & Téléprompteur.
 */
export const StudioToolbar: React.FC<StudioToolbarProps> = ({
  activeTool,
  onSelectTool,
  aspectRatio,
  onChangeAspectRatio,
  adjustments,
  onChangeAdjustments,
  activeFilter,
  onSelectFilter,
  backgroundType,
  backgroundColor,
  onChangeBackground,
  coverImageUrl,
  onSelectCoverImage,
  onAutoGenerateSubtitles,
  isGeneratingSubtitles = false,
  onOpenTeleprompter,
  onOpenExportPanel,
}) => {
  const [selectedStickerCategory, setSelectedStickerCategory] = useState<'reactions' | 'viral' | 'africa' | 'badges'>('viral');

  const tools = [
    { id: 'cut', label: 'Découper', icon: '✂️', desc: 'Scinder & Rogner' },
    { id: 'filters', label: 'Filtres', icon: '🎨', desc: 'Ambiances couleur' },
    { id: 'adjust', label: 'Ajuster', icon: '⚙️', desc: 'Lumière & Contraste' },
    { id: 'stickers', label: 'Stickers', icon: '✨', desc: 'Éléments animés' },
    { id: 'aspect_ratio', label: 'Format', icon: '📐', desc: `${aspectRatio}` },
    { id: 'background', label: 'Arrière-plan', icon: '🖼️', desc: 'Flou & Couleurs' },
    { id: 'cover', label: 'Couverture', icon: '🖼️', desc: 'Miniature vidéo' },
    { id: 'subtitles', label: 'Sous-titres', icon: '💬', desc: 'Sous-titres IA' },
    { id: 'teleprompter', label: 'Téléprompteur', icon: '📜', desc: 'Face caméra' },
  ];

  const aspectRatios: { key: AspectRatioType; label: string; desc: string }[] = [
    { key: '9:16', label: '9:16', desc: 'TikTok, Reels, Shorts' },
    { key: '16:9', label: '16:9', desc: 'YouTube, Web VOD' },
    { key: '1:1', label: '1:1', desc: 'Carré Instagram' },
    { key: '4:5', label: '4:5', desc: 'Feed Portrait' },
    { key: '21:9', label: '21:9', desc: 'Cinéma Écran Large' },
  ];

  const filters = [
    { id: 'none', name: 'Original', css: 'none' },
    { id: 'beauty_smooth', name: '✨ Lissage IA', css: 'contrast(1.05) brightness(1.08) saturate(1.15)' },
    { id: 'radiant_afro', name: '🌟 Teint Éclatant', css: 'sepia(0.2) saturate(1.35) contrast(1.1) brightness(1.04)' },
    { id: 'film_35mm', name: '🎞️ Grain 35mm', css: 'contrast(1.15) sepia(0.15) brightness(0.98)' },
    { id: 'studio_pro', name: '🎬 Studio Pro', css: 'contrast(1.25) saturate(1.2) brightness(1.02)' },
    { id: 'afropunk', name: 'AfroPunk', css: 'contrast(1.2) saturate(1.4) brightness(1.05)' },
    { id: 'golden_hour', name: 'Golden Hour', css: 'sepia(0.3) saturate(1.3) contrast(1.1)' },
    { id: 'cyberpunk', name: 'Cyberpunk', css: 'hue-rotate(290deg) contrast(1.3)' },
    { id: 'vintage_70s', name: 'Vintage 70s', css: 'sepia(0.5) contrast(0.9) brightness(1.1)' },
    { id: 'bw_film', name: 'Noir & Blanc', css: 'grayscale(1) contrast(1.2)' },
  ];

  const stickers = {
    viral: ['🔥', '⚡', '🚀', '👑', '👀', '💯', '💥', '🎯'],
    reactions: ['😂', '😍', '😱', '🤯', '👏', '🙌', '💃', '🕺'],
    africa: ['🌍', '🦁', '🥁', '🌴', '☀️', '🐆', '🎨', '🪘'],
    badges: ['NOUVEAU', 'VIRAL', 'LIVE', 'TOP 1', 'EXCLUSIF', 'PARTAGEZ'],
  };

  const backgroundColors = [
    '#000000', '#1A1C29', '#E5A93C', '#2ED573', '#FF4757', '#1E90FF', '#9B59B6', '#F1C40F'
  ];

  return (
    <div style={{ backgroundColor: '#141620', border: '1px solid #282C3D', borderRadius: 16, overflow: 'hidden' }}>
      {/* 1. Barre de navigation principale des outils (Scroll horizontal fluide) */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          padding: '10px 14px',
          overflowX: 'auto',
          borderBottom: '1px solid #232738',
          scrollbarWidth: 'none',
        }}
      >
        {tools.map((t) => {
          const isActive = activeTool === t.id;
          return (
            <button
              key={t.id}
              type="button"
              onClick={() => {
                if (t.id === 'teleprompter') {
                  onOpenTeleprompter();
                } else if (t.id === 'cover') {
                  onSelectCoverImage();
                } else {
                  onSelectTool(isActive ? null : (t.id as StudioToolId));
                }
              }}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                minWidth: 72,
                padding: '8px 10px',
                backgroundColor: isActive ? 'rgba(229, 169, 60, 0.16)' : '#191C28',
                border: isActive ? '1px solid #E5A93C' : '1px solid transparent',
                borderRadius: 10,
                color: isActive ? '#E5A93C' : '#DDD',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
                flexShrink: 0,
              }}
            >
              <span style={{ fontSize: 18 }}>{t.icon}</span>
              <span style={{ fontSize: 11, fontWeight: 700, marginTop: 4 }}>{t.label}</span>
            </button>
          );
        })}

        {/* Bouton Export Rapide */}
        <div style={{ marginLeft: 'auto', paddingLeft: 10, flexShrink: 0 }}>
          <button
            type="button"
            onClick={onOpenExportPanel}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              backgroundColor: '#E5A93C',
              color: '#000',
              border: 'none',
              borderRadius: 10,
              padding: '10px 16px',
              fontWeight: 800,
              fontSize: 12,
              cursor: 'pointer',
              boxShadow: '0 4px 12px rgba(229, 169, 60, 0.3)',
            }}
          >
            <span>🚀</span>
            <span>Exporter</span>
          </button>
        </div>
      </div>

      {/* 2. Tiroir contextuel selon l'outil actif */}
      {activeTool && (
        <div style={{ padding: 16, backgroundColor: '#0F1118', borderTop: '1px solid #1E2232' }}>
          {/* A. FORMAT / RATIO VIDÉO */}
          {activeTool === 'aspect_ratio' && (
            <div>
              <div style={{ fontSize: 12, fontWeight: 700, color: '#AAA', marginBottom: 10 }}>
                CHOISIR LE FORMAT VIDÉO (CANVAS DE RENDU) :
              </div>
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                {aspectRatios.map((ar) => (
                  <button
                    key={ar.key}
                    type="button"
                    onClick={() => onChangeAspectRatio(ar.key)}
                    style={{
                      backgroundColor: aspectRatio === ar.key ? '#E5A93C' : '#191C28',
                      color: aspectRatio === ar.key ? '#000' : '#FFF',
                      border: '1px solid #2B2F42',
                      borderRadius: 10,
                      padding: '10px 14px',
                      cursor: 'pointer',
                      fontWeight: 800,
                      fontSize: 12,
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                    }}
                  >
                    <span>{ar.label}</span>
                    <span style={{ fontSize: 10, opacity: 0.8 }}>({ar.desc})</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* B. FILTRES COLORIMÉTRIQUES */}
          {activeTool === 'filters' && (
            <div>
              <div style={{ fontSize: 12, fontWeight: 700, color: '#AAA', marginBottom: 10 }}>
                FILTRES D'AMBIANCE CHROMATIQUE :
              </div>
              <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 6 }}>
                {filters.map((f) => {
                  const isSelected = activeFilter === f.id;
                  return (
                    <button
                      key={f.id}
                      type="button"
                      onClick={() => onSelectFilter(f.id)}
                      style={{
                        minWidth: 100,
                        backgroundColor: isSelected ? 'rgba(229, 169, 60, 0.2)' : '#191C28',
                        border: isSelected ? '2px solid #E5A93C' : '1px solid #282C3D',
                        borderRadius: 10,
                        padding: 8,
                        color: isSelected ? '#E5A93C' : '#FFF',
                        cursor: 'pointer',
                        textAlign: 'center',
                      }}
                    >
                      <div
                        style={{
                          width: '100%',
                          height: 48,
                          borderRadius: 6,
                          backgroundColor: '#E5A93C',
                          filter: f.css,
                          marginBottom: 6,
                          backgroundImage: 'linear-gradient(45deg, #12c2e9, #c471ed, #f64f59)',
                        }}
                      />
                      <div style={{ fontSize: 11, fontWeight: 700 }}>{f.name}</div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* C. AJUSTEMENTS PRÉCIS (LUMINOSITÉ, CONTRASTE, SATURATION, ETC.) */}
          {activeTool === 'adjust' && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 14 }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#CCC', marginBottom: 4 }}>
                  <span>☀️ Luminosité</span>
                  <span style={{ color: '#E5A93C' }}>{adjustments.brightness}</span>
                </div>
                <input
                  type="range"
                  min={-50}
                  max={50}
                  value={adjustments.brightness}
                  onChange={(e) => onChangeAdjustments({ ...adjustments, brightness: Number(e.target.value) })}
                  style={{ width: '100%', accentColor: '#E5A93C' }}
                />
              </div>

              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#CCC', marginBottom: 4 }}>
                  <span>🌓 Contraste</span>
                  <span style={{ color: '#E5A93C' }}>{adjustments.contrast}</span>
                </div>
                <input
                  type="range"
                  min={-50}
                  max={50}
                  value={adjustments.contrast}
                  onChange={(e) => onChangeAdjustments({ ...adjustments, contrast: Number(e.target.value) })}
                  style={{ width: '100%', accentColor: '#E5A93C' }}
                />
              </div>

              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#CCC', marginBottom: 4 }}>
                  <span>🌈 Saturation</span>
                  <span style={{ color: '#E5A93C' }}>{adjustments.saturation}</span>
                </div>
                <input
                  type="range"
                  min={-50}
                  max={50}
                  value={adjustments.saturation}
                  onChange={(e) => onChangeAdjustments({ ...adjustments, saturation: Number(e.target.value) })}
                  style={{ width: '100%', accentColor: '#E5A93C' }}
                />
              </div>

              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#CCC', marginBottom: 4 }}>
                  <span>🌡️ Température</span>
                  <span style={{ color: '#E5A93C' }}>{adjustments.temperature}</span>
                </div>
                <input
                  type="range"
                  min={-50}
                  max={50}
                  value={adjustments.temperature}
                  onChange={(e) => onChangeAdjustments({ ...adjustments, temperature: Number(e.target.value) })}
                  style={{ width: '100%', accentColor: '#E5A93C' }}
                />
              </div>
            </div>
          )}

          {/* D. ARRIÈRE-PLAN (POUR FORMATS INCRUSTÉS ET FLOU GAUSSIEN) */}
          {activeTool === 'background' && (
            <div>
              <div style={{ fontSize: 12, fontWeight: 700, color: '#AAA', marginBottom: 10 }}>
                STYLE DE L'ARRIÈRE-PLAN VIDÉO :
              </div>
              <div style={{ display: 'flex', gap: 10, marginBottom: 12 }}>
                {(['blur', 'color', 'gradient'] as const).map((t) => (
                  <button
                    key={t}
                    type="button"
                    onClick={() => onChangeBackground(t, backgroundColor)}
                    style={{
                      backgroundColor: backgroundType === t ? '#E5A93C' : '#191C28',
                      color: backgroundType === t ? '#000' : '#FFF',
                      border: '1px solid #2B2F42',
                      borderRadius: 8,
                      padding: '6px 14px',
                      cursor: 'pointer',
                      fontSize: 12,
                      fontWeight: 700,
                      textTransform: 'uppercase',
                    }}
                  >
                    {t === 'blur' ? '🌫️ Flou Cinéma' : t === 'color' ? '🎨 Couleur Unie' : '🌈 Dégradé'}
                  </button>
                ))}
              </div>

              {backgroundType === 'color' && (
                <div style={{ display: 'flex', gap: 8 }}>
                  {backgroundColors.map((c) => (
                    <div
                      key={c}
                      onClick={() => onChangeBackground('color', c)}
                      style={{
                        width: 28,
                        height: 28,
                        borderRadius: '50%',
                        backgroundColor: c,
                        border: backgroundColor === c ? '2px solid #FFF' : '1px solid #444',
                        cursor: 'pointer',
                      }}
                    />
                  ))}
                </div>
              )}
            </div>
          )}

          {/* E. STICKERS & ÉLÉMENTS VIRAUX */}
          {activeTool === 'stickers' && (
            <div>
              <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
                {(['viral', 'reactions', 'africa', 'badges'] as const).map((cat) => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => setSelectedStickerCategory(cat)}
                    style={{
                      backgroundColor: selectedStickerCategory === cat ? '#E5A93C' : '#191C28',
                      color: selectedStickerCategory === cat ? '#000' : '#AAA',
                      border: 'none',
                      borderRadius: 6,
                      padding: '4px 10px',
                      fontSize: 11,
                      fontWeight: 700,
                      cursor: 'pointer',
                      textTransform: 'capitalize',
                    }}
                  >
                    {cat}
                  </button>
                ))}
              </div>
              <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', fontSize: 24 }}>
                {stickers[selectedStickerCategory].map((s, idx) => (
                  <button
                    key={idx}
                    type="button"
                    style={{
                      background: '#191C28',
                      border: '1px solid #282C3D',
                      borderRadius: 10,
                      padding: '6px 12px',
                      cursor: 'pointer',
                      fontSize: 20,
                      color: '#FFF',
                    }}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* F. SOUS-TITRES AUTOMATIQUES (SPEECH-TO-TEXT IA & STYLES TIKTOK) */}
          {activeTool === 'subtitles' && (
            <div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12, marginBottom: 12 }}>
                <div>
                  <div style={{ fontSize: 13, fontWeight: 700, color: '#FFF' }}>
                    💬 Sous-Titres Automatiques & Animation Mot-à-Mot
                  </div>
                  <div style={{ fontSize: 11, color: '#888F9E' }}>
                    Synchronisation speech-to-text IA inspirée de CapCut & TikTok pour maximiser la rétention
                  </div>
                </div>
                <button
                  type="button"
                  disabled={isGeneratingSubtitles}
                  onClick={onAutoGenerateSubtitles}
                  style={{
                    backgroundColor: '#2ED573',
                    color: '#000',
                    border: 'none',
                    borderRadius: 8,
                    padding: '10px 16px',
                    fontWeight: 800,
                    fontSize: 12,
                    cursor: isGeneratingSubtitles ? 'not-allowed' : 'pointer',
                  }}
                >
                  {isGeneratingSubtitles ? 'Transcription en cours...' : '⚡ Générer les sous-titres IA'}
                </button>
              </div>

              {/* Styles de sous-titres dynamiques */}
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <span style={{ fontSize: 11, color: '#AAA', alignSelf: 'center' }}>Style visuel :</span>
                {[
                  { name: '🔥 Jaune Néon TikTok', style: 'color: #FFD700, textShadow: 0 0 10px #000' },
                  { name: '🎤 Karaoké Mot-à-Mot', style: 'color: #FFF, highlight: #2ED573' },
                  { name: '👑 Or Impérial PANU', style: 'color: #E5A93C, border: 1px solid #E5A93C' },
                  { name: '⚡ Minimaliste Blanc', style: 'color: #FFF, background: rgba(0,0,0,0.5)' },
                ].map((st, i) => (
                  <button
                    key={i}
                    type="button"
                    style={{
                      backgroundColor: 'rgba(255, 255, 255, 0.05)',
                      border: '1px solid rgba(255, 255, 255, 0.1)',
                      borderRadius: 6,
                      padding: '4px 10px',
                      color: '#DDD',
                      fontSize: 11,
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    {st.name}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* G. DÉCOUPAGE (CUT / SPLIT) */}
          {activeTool === 'cut' && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <div style={{ fontSize: 12, color: '#AAA' }}>
                Positionnez la tête de lecture puis cliquez sur l'action souhaitée :
              </div>
              <button
                type="button"
                style={{
                  backgroundColor: '#E5A93C',
                  color: '#000',
                  border: 'none',
                  borderRadius: 8,
                  padding: '8px 14px',
                  fontWeight: 800,
                  fontSize: 12,
                  cursor: 'pointer',
                }}
              >
                ✂️ Scinder à la tête de lecture
              </button>
              <button
                type="button"
                style={{
                  backgroundColor: '#FF4757',
                  color: '#FFF',
                  border: 'none',
                  borderRadius: 8,
                  padding: '8px 14px',
                  fontWeight: 700,
                  fontSize: 12,
                  cursor: 'pointer',
                }}
              >
                🗑️ Supprimer le segment
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
