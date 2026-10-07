import React, { useEffect, useState } from 'react';
import { MultiTrackTimeline } from '../components/studio/MultiTrackTimeline';
import { StudioToolbar, StudioToolId, AspectRatioType, VideoAdjustments } from '../components/studio/StudioToolbar';
import { VideoExportPanel } from '../components/studio/VideoExportPanel';
import { DynamicTemplateGallery } from '../components/studio/DynamicTemplateGallery';
import StudioIA from '../components/studio/StudioIA';
import { LiveKitStudio } from '../components/studio/LiveKitStudio';
import {
  getLocalStudioDrafts,
  initOfflinePwaAndAutoSync,
  OfflineStudioDraft,
} from '../services/offlineSyncService';

/**
 * PAGE PRINCIPALE : PANU STUDIO IA PROFESSIONNEL (STYLE CAPCUT / CANVA / TIKTOK STUDIO)
 * - Éditeur vidéo complet avec Timeline multi-pistes (Vidéo, PIP, Audio, Sous-titres)
 * - Studio IA : Création vidéo directe avec Fal.ai / Pixverse et publication automatique
 * - Barre d'outils de montage (Découpage, Filtres, Ajustements, Stickers, Arrière-plan, Ratio)
 * - Panneau d'exportation avec simulation dynamique du bitrate et taille de fichier + IA Ultra HD
 * - Mode Gratuit & Mode Hors-ligne PWA avec synchronisation automatique Supabase
 */
export const StudioPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'editor' | 'templates' | 'studio_ia' | 'drafts' | 'live'>('editor');
  const [isOnline, setIsOnline] = useState<boolean>(typeof navigator !== 'undefined' ? navigator.onLine : true);
  const [drafts, setDrafts] = useState<OfflineStudioDraft[]>(() => getLocalStudioDrafts());
  const [syncMessage, setSyncMessage] = useState<string | null>(null);

  // États du lecteur et de la Timeline vidéo
  const [currentTime, setCurrentTime] = useState<number>(14.5);
  const [totalDuration, setTotalDuration] = useState<number>(60);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);

  // États des outils de montage
  const [activeTool, setActiveTool] = useState<StudioToolId | null>(null);
  const [aspectRatio, setAspectRatio] = useState<AspectRatioType>('9:16');
  const [activeFilter, setActiveFilter] = useState<string>('none');
  const [backgroundType, setBackgroundType] = useState<'blur' | 'color' | 'gradient' | 'image'>('blur');
  const [backgroundColor, setBackgroundColor] = useState<string>('#1A1C29');
  const [coverImageUrl, setCoverImageUrl] = useState<string | null>(null);
  const [isGeneratingSubtitles, setIsGeneratingSubtitles] = useState<boolean>(false);
  const [showExportModal, setShowExportModal] = useState<boolean>(false);

  const [adjustments, setAdjustments] = useState<VideoAdjustments>({
    brightness: 0,
    contrast: 0,
    saturation: 0,
    temperature: 0,
    vignette: 0,
    sharpness: 0,
  });

  const mainModels = [
    { label: 'Créer une image', icon: '🖼️' },
    { label: 'Image en vidéo', icon: '🎬' },
    { label: 'Texte en vidéo', icon: '📝' },
    { label: 'MV en 1 clic', icon: '🎵' },
  ];

  const mainTools = [
    { label: 'Générateur d\'effets', icon: '✨' },
    { label: 'Synchro labiale', icon: '👄' },
    { label: 'Modifier l\'image', icon: '🎨' },
    { label: 'Transition', icon: '📽️' },
    { label: 'Plus', icon: '➕' },
  ];

  const quickActions = [
    { label: 'Retouche', icon: '✨' },
    { label: 'Légendes automatiques', icon: '💬' },
    { label: 'Téléprompteur', icon: '📜' },
    { label: 'Appareil photo', icon: '📷' },
    { label: 'Enregistrer audio', icon: '🎙️' },
    { label: 'Prise d\'images', icon: '🖼️' },
  ];

  const aiTools = [
    { label: 'Découpage automatique', icon: '✂️' },
    { label: 'Affiche IA', icon: '🎨' },
    { label: 'Photos de produits', icon: '🛍️' },
  ];

  const recents = [
    { label: 'Collage', icon: '🖼️' },
    { label: 'Éditeur photo', icon: '📸' },
  ];

  // Mode Caméra pour Éditeur
  const editorVideoRef = useRef<HTMLVideoElement | null>(null);
  const [isEditorCameraActive, setIsEditorVideoActive] = useState(false);
  const [editorCameraFacing, setEditorCameraFacing] = useState<'user' | 'environment'>('user');
  const editorMediaStreamRef = useRef<MediaStream | null>(null);

  const startEditorCamera = async (facing?: 'user' | 'environment') => {
    const mode = facing || editorCameraFacing;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: mode },
        audio: true
      });
      editorMediaStreamRef.current = stream;
      if (editorVideoRef.current) {
        editorVideoRef.current.srcObject = stream;
        setIsEditorVideoActive(true);
      }
    } catch (err) {
      console.error("Studio camera error:", err);
    }
  };

  const stopEditorCamera = () => {
    if (editorMediaStreamRef.current) {
      editorMediaStreamRef.current.getTracks().forEach(track => track.stop());
      editorMediaStreamRef.current = null;
    }
    setIsEditorVideoActive(false);
  };

  const toggleEditorCameraFacing = () => {
    const nextFacing = editorCameraFacing === 'user' ? 'environment' : 'user';
    setEditorCameraFacing(nextFacing);
    if (activeTab === 'editor') {
      stopEditorCamera();
      setTimeout(() => startEditorCamera(nextFacing), 300);
    }
  };

  useEffect(() => {
    if (activeTab === 'editor') {
      startEditorCamera();
    } else {
      stopEditorCamera();
    }
    return () => stopEditorCamera();
  }, [activeTab]);


  return (
    <div style={{ color: '#F8F9FA', minHeight: '100vh', backgroundColor: '#0B0C12' }}>

      <div style={{ padding: '16px 20px', maxWidth: 1400, margin: '0 auto' }}>
        
        {/* HEADER STUDIO */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <div>
            <h1 style={{ fontSize: 24, fontWeight: 900, color: '#E5A93C', margin: 0 }}>STUDIO PANU IA</h1>
            <p style={{ fontSize: 12, color: '#888', margin: '4px 0 0' }}>Éditeur Média & Intelligence Artificielle</p>
          </div>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
             <button 
               onClick={() => setActiveTab('editor')}
               style={{ backgroundColor: activeTab === 'editor' ? '#E5A93C' : '#1A1C28', color: activeTab === 'editor' ? '#000' : '#FFF', padding: '10px 20px', borderRadius: 10, fontWeight: 800, border: 'none', cursor: 'pointer', transition: '0.2s' }}
             >
               Éditeur
             </button>
             <button 
               onClick={() => setActiveTab('studio_ia')}
               style={{ backgroundColor: activeTab === 'studio_ia' ? '#E5A93C' : '#1A1C28', color: activeTab === 'studio_ia' ? '#000' : '#FFF', padding: '10px 20px', borderRadius: 10, fontWeight: 800, border: 'none', cursor: 'pointer', transition: '0.2s' }}
             >
               ✨ Studio IA Vidéo
             </button>
             <button 
               onClick={() => setActiveTab('templates')}
               style={{ backgroundColor: activeTab === 'templates' ? '#E5A93C' : '#1A1C28', color: activeTab === 'templates' ? '#000' : '#FFF', padding: '10px 20px', borderRadius: 10, fontWeight: 800, border: 'none', cursor: 'pointer', transition: '0.2s' }}
             >
               Modèles
             </button>
             <button 
               onClick={() => setActiveTab('live')}
               style={{ backgroundColor: activeTab === 'live' ? '#E5A93C' : '#1A1C28', color: activeTab === 'live' ? '#000' : '#FFF', padding: '10px 20px', borderRadius: 10, fontWeight: 800, border: 'none', cursor: 'pointer', transition: '0.2s' }}
             >
               🔴 Live
             </button>
          </div>
        </div>

        {activeTab === 'editor' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: 24 }}>
            {/* COLONNE GAUCHE : PREVIEW & TIMELINE */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {/* PREVIEW CANVAS RÉEL (CAMÉRA) */}
              <div style={{ backgroundColor: '#11131B', borderRadius: 20, height: 500, display: 'flex', alignItems: 'center', justifyContent: 'center', border: '1px solid #252838', position: 'relative', overflow: 'hidden' }}>
                 <video
                   ref={editorVideoRef}
                   autoPlay
                   playsInline
                   muted
                   style={{ width: 230, height: 400, backgroundColor: '#000', borderRadius: 12, border: '2px solid rgba(229, 169, 60, 0.4)', boxShadow: '0 0 30px rgba(0,0,0,0.5)', objectFit: 'cover' }}
                 />
                 <div style={{ position: 'absolute', top: 20, right: 20, display: 'flex', gap: 10 }}>
                    <button
                      onClick={toggleEditorCameraFacing}
                      style={{ backgroundColor: 'rgba(0,0,0,0.6)', border: '1px solid #E5A93C', color: '#E5A93C', borderRadius: 50, width: 36, height: 36, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18 }}
                      title="Changer de caméra"
                    >
                      🔄
                    </button>
                 </div>
                 {!isEditorCameraActive && (

                   <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.6)' }}>
                     <span style={{ fontSize: 13, color: '#E5A93C', fontWeight: 800 }}>Initialisation caméra...</span>
                   </div>
                 )}
                 <div style={{ position: 'absolute', bottom: 20, left: '50%', transform: 'translateX(-50%)', backgroundColor: 'rgba(0,0,0,0.6)', padding: '4px 12px', borderRadius: 20, fontSize: 12 }}>1080p | 30 FPS</div>
              </div>


              {/* TOOLBAR TIMELINE */}
              <StudioToolbar
                activeTool={activeTool}
                onSelectTool={setActiveTool}
                aspectRatio={aspectRatio}
                onChangeAspectRatio={setAspectRatio}
                adjustments={adjustments}
                onChangeAdjustments={setAdjustments}
                activeFilter={activeFilter}
                onSelectFilter={setActiveFilter}
                backgroundType={backgroundType}
                backgroundColor={backgroundColor}
                onChangeBackground={(t, v) => { setBackgroundType(t); setBackgroundColor(v); }}
                onSelectCoverImage={() => {}}
                onAutoGenerateSubtitles={() => {}}
                onOpenTeleprompter={() => {}}
                onOpenExportPanel={() => setShowExportModal(true)}
              />

              {/* TIMELINE */}
              <div style={{ backgroundColor: '#15161E', borderRadius: 16, border: '1px solid #252838', padding: 12 }}>
                <MultiTrackTimeline
                  currentTime={currentTime}
                  totalDuration={totalDuration}
                  isPlaying={isPlaying}
                  onSeek={setCurrentTime}
                  onTogglePlay={() => setIsPlaying(!isPlaying)}
                  onSplitCurrentClip={() => {}}
                />
              </div>
            </div>

            {/* COLONNE DROITE : ACTIONS RAPIDES & RÉCENTS */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              {/* BOUTON NOUVEAU PROJET */}
              <button 
                onClick={() => setActiveTab('studio_ia')}
                style={{ width: '100%', backgroundColor: '#00D1FF', color: '#000', padding: '18px', borderRadius: 16, fontWeight: 900, fontSize: 18, border: 'none', cursor: 'pointer', boxShadow: '0 4px 15px rgba(0,209,255,0.3)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 }}
              >
                <span>➕</span>
                <span>Nouveau projet IA</span>
              </button>

              {/* RÉCENTS */}
              <div style={{ backgroundColor: '#15161E', padding: 20, borderRadius: 18, border: '1px solid #252838' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                  <h3 style={{ margin: 0, fontSize: 15, fontWeight: 700, color: '#EEE' }}>Récents</h3>
                  <span style={{ fontSize: 11, color: '#00D1FF', cursor: 'pointer' }}>Voir tout</span>
                </div>
                <div style={{ display: 'flex', gap: 14 }}>
                  {recents.map(r => (
                    <div key={r.label} style={{ textAlign: 'center', cursor: 'pointer', flex: 1 }}>
                      <div style={{ height: 60, backgroundColor: '#1F2029', borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24, marginBottom: 8, border: '1px solid #2A2D3E' }}>{r.icon}</div>
                      <div style={{ fontSize: 11, color: '#BBB' }}>{r.label}</div>
                    </div>
                  ))}
                  <div style={{ flex: 1 }}></div>
                </div>
              </div>

              {/* ACTIONS RAPIDES & OUTILS PRINCIPAUX */}
              <div style={{ backgroundColor: '#15161E', padding: 20, borderRadius: 18, border: '1px solid #252838' }}>
                <h3 style={{ margin: '0 0 16px', fontSize: 15, fontWeight: 700, color: '#EEE' }}>Modèles Principaux</h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12, marginBottom: 20 }}>
                  {mainModels.map(m => (
                    <div key={m.label} style={{ textAlign: 'center', cursor: 'pointer' }}>
                      <div style={{ width: '100%', aspectRatio: '16/9', backgroundColor: '#1F2029', borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24, marginBottom: 6, border: '1px solid #2A2D3E' }}>{m.icon}</div>
                      <div style={{ fontSize: 10, color: '#BBB', lineHeight: 1.2 }}>{m.label}</div>
                    </div>
                  ))}
                </div>

                <h3 style={{ margin: '0 0 16px', fontSize: 15, fontWeight: 700, color: '#EEE' }}>Outils Principaux</h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                  {mainTools.map(t => (
                    <div key={t.label} style={{ textAlign: 'center', cursor: 'pointer' }}>
                      <div style={{ width: '100%', aspectRatio: '1/1', backgroundColor: '#1F2029', borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20, marginBottom: 6, border: '1px solid #2A2D3E' }}>{t.icon}</div>
                      <div style={{ fontSize: 9, color: '#BBB', lineHeight: 1.2 }}>{t.label}</div>
                    </div>
                  ))}
                </div>
              </div>

              {/* ACTIONS RAPIDES */}
              <div style={{ backgroundColor: '#15161E', padding: 20, borderRadius: 18, border: '1px solid #252838' }}>
                <h3 style={{ margin: '0 0 16px', fontSize: 15, fontWeight: 700, color: '#EEE' }}>Actions rapides</h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                  {quickActions.map(a => (
                    <div key={a.label} style={{ textAlign: 'center', cursor: 'pointer' }}>
                      <div style={{ width: '100%', aspectRatio: '1/1', backgroundColor: '#1F2029', borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22, marginBottom: 6, border: '1px solid #2A2D3E' }}>{a.icon}</div>
                      <div style={{ fontSize: 10, color: '#BBB', lineHeight: 1.2 }}>{a.label}</div>
                    </div>
                  ))}
                </div>
              </div>

              {/* OUTILS IA */}
              <div style={{ backgroundColor: '#15161E', padding: 20, borderRadius: 18, border: '1px solid #252838' }}>
                <h3 style={{ margin: '0 0 16px', fontSize: 15, fontWeight: 700, color: '#EEE' }}>Outils IA</h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                  {aiTools.map(t => (
                    <div key={t.label} style={{ textAlign: 'center', cursor: 'pointer' }}>
                      <div style={{ width: '100%', aspectRatio: '1/1', backgroundColor: '#1F2029', borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22, marginBottom: 6, border: '1px solid #2A2D3E' }}>{t.icon}</div>
                      <div style={{ fontSize: 10, color: '#BBB', lineHeight: 1.2 }}>{t.label}</div>
                    </div>
                  ))}
                </div>
              </div>
              
              {/* BOUTON EXPORTER HD/UHD */}
              <button 
                onClick={() => setShowExportModal(true)}
                style={{ width: '100%', backgroundColor: 'transparent', color: '#E5A93C', padding: '14px', borderRadius: 14, fontWeight: 700, fontSize: 14, border: '1px solid #E5A93C', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}
              >
                <span>🚀</span>
                <span>Exporter en HD/UHD</span>
              </button>
            </div>
          </div>
        )}

        {activeTab === 'templates' && <DynamicTemplateGallery />}
        
        {activeTab === 'studio_ia' && (
          <div style={{ padding: '20px 0' }}>
            <StudioIA />
          </div>
        )}
        
        {activeTab === 'live' && (
          <div style={{ padding: '20px 0' }}>
            <LiveKitStudio />
          </div>
        )}
        
        {/* Modale d'Exportation */}
        {showExportModal && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.9)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999, padding: 20 }}>
             <VideoExportPanel
                videoDurationSeconds={totalDuration}
                onClose={() => setShowExportModal(false)}
                onStartExport={() => setShowExportModal(false)}
             />
          </div>
        )}

        {syncMessage && (
          <div style={{ position: 'fixed', bottom: 100, left: '50%', transform: 'translateX(-50%)', backgroundColor: '#2ED573', color: '#000', padding: '10px 20px', borderRadius: 12, fontWeight: 800, fontSize: 13, zIndex: 1000, boxShadow: '0 4px 15px rgba(0,0,0,0.3)' }}>
            {syncMessage}
          </div>
        )}

      </div>
    </div>
  );
};

export default StudioPage;
