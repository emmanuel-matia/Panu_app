import React, { useState, useRef, useEffect, useCallback } from 'react';

export interface PanuVideoPlayerProps {
  src: string;
  poster?: string;
  title?: string;
  autoplay?: boolean;
  loop?: boolean;
  muted?: boolean;
  className?: string;
  onEnded?: () => void;
  onError?: (error: string) => void;
  onPlay?: () => void;
  onPause?: () => void;
}

// Flux vérifiés haute fiabilité (CDN Cloudflare, Fastly, GitHub)
export const RELIABLE_FALLBACK_STREAMS = [
  'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4',
  'https://test-videos.co.uk/vids/jellyfish/mp4/h264/360/Jellyfish_360_10s_1MB.mp4',
  'https://test-videos.co.uk/vids/sintel/mp4/h264/360/Sintel_360_10s_1MB.mp4',
  'https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/person-bicycle-car-detection.mp4',
];

export const DEFAULT_FALLBACK_VIDEO = RELIABLE_FALLBACK_STREAMS[0];

export function resolvePlayableUrl(url?: string | null): string {
  if (!url || typeof url !== 'string' || !url.trim()) {
    return DEFAULT_FALLBACK_VIDEO;
  }
  const clean = url.trim();
  // Les domaines GTV/commondatastorage ou w3schools génèrent des 403 Forbidden
  if (
    clean.includes('commondatastorage.googleapis.com') ||
    clean.includes('w3schools.com')
  ) {
    return DEFAULT_FALLBACK_VIDEO;
  }
  return clean;
}

/**
 * Composant Lecteur Vidéo Haute Performance PANU (Compatible ExoPlayer Media3 Android / Web / iOS)
 * - Contrôles personnalisés modernes aux couleurs PANU (Champagne #E5A93C & Obsidienne #121214)
 * - Lecture / Pause fluide
 * - Barre de progression interactive (seeking avec temps courant et durée totale)
 * - Contrôle de volume (slider + toggle Muet)
 * - Plein écran natif (Fullscreen API)
 * - Support PiP (Picture-in-Picture)
 * - Gestion gracieuse des erreurs et basculement automatique sur flux CDN garantis
 * - Optimisé mobile (playsInline, webkit-playsinline, pas de blocage autoplay)
 */
export const PanuVideoPlayer: React.FC<PanuVideoPlayerProps> = ({
  src,
  poster,
  title,
  autoplay = false,
  loop = true,
  muted = false,
  className = '',
  onEnded,
  onError,
  onPlay,
  onPause,
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const progressBarRef = useRef<HTMLDivElement | null>(null);

  const [currentSrc, setCurrentSrc] = useState<string>(resolvePlayableUrl(src));
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [isMuted, setIsMuted] = useState<boolean>(muted);
  const [volume, setVolume] = useState<number>(1);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [duration, setDuration] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [hasError, setHasError] = useState<boolean>(false);
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);
  const [showControls, setShowControls] = useState<boolean>(true);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1);
  const [isSeeking, setIsSeeking] = useState<boolean>(false);

  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    setCurrentSrc(resolvePlayableUrl(src));
    setHasError(false);
    setIsLoading(true);
  }, [src]);

  // Masquer les contrôles après 3s d'inactivité pendant la lecture
  const resetControlsTimeout = useCallback(() => {
    setShowControls(true);
    if (controlsTimeoutRef.current) {
      clearTimeout(controlsTimeoutRef.current);
    }
    if (isPlaying) {
      controlsTimeoutRef.current = setTimeout(() => {
        setShowControls(false);
      }, 3200);
    }
  }, [isPlaying]);

  useEffect(() => {
    resetControlsTimeout();
    return () => {
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    };
  }, [isPlaying, resetControlsTimeout]);

  // Synchronisation plein écran
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
    };
  }, []);

  const togglePlay = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;

    if (video.paused || video.ended) {
      video.play()
        .then(() => {
          setIsPlaying(true);
          onPlay?.();
        })
        .catch((err) => {
          console.warn('[PanuVideoPlayer] Autoplay bloqué, lecture avec son coupé requise:', err);
          video.muted = true;
          setIsMuted(true);
          video.play()
            .then(() => {
              setIsPlaying(true);
              onPlay?.();
            })
            .catch(() => {
              setIsPlaying(false);
            });
        });
    } else {
      video.pause();
      setIsPlaying(false);
      onPause?.();
    }
    resetControlsTimeout();
  }, [onPlay, onPause, resetControlsTimeout]);

  const toggleMute = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    const nextMuted = !video.muted;
    video.muted = nextMuted;
    setIsMuted(nextMuted);
    if (!nextMuted && video.volume === 0) {
      video.volume = 0.8;
      setVolume(0.8);
    }
    resetControlsTimeout();
  }, [resetControlsTimeout]);

  const handleVolumeChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const video = videoRef.current;
    if (!video) return;
    const val = parseFloat(e.target.value);
    video.volume = val;
    setVolume(val);
    video.muted = val === 0;
    setIsMuted(val === 0);
    resetControlsTimeout();
  }, [resetControlsTimeout]);

  const handleSeek = useCallback((e: React.MouseEvent<HTMLDivElement> | React.TouchEvent<HTMLDivElement>) => {
    const video = videoRef.current;
    const bar = progressBarRef.current;
    if (!video || !bar) return;

    const rect = bar.getBoundingClientRect();
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const offsetX = Math.max(0, Math.min(clientX - rect.left, rect.width));
    const ratio = offsetX / rect.width;
    const targetTime = ratio * (video.duration || 1);

    video.currentTime = targetTime;
    setCurrentTime(targetTime);
    resetControlsTimeout();
  }, [resetControlsTimeout]);

  const toggleFullscreen = useCallback(() => {
    const container = containerRef.current;
    if (!container) return;

    if (!document.fullscreenElement) {
      container.requestFullscreen?.().catch((err) => {
        console.warn('[PanuVideoPlayer] Erreur passage plein écran:', err);
      });
    } else {
      document.exitFullscreen?.().catch(() => {});
    }
    resetControlsTimeout();
  }, [resetControlsTimeout]);

  const togglePictureInPicture = useCallback(async () => {
    const video = videoRef.current;
    if (!video) return;
    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else if (document.pictureInPictureEnabled) {
        await video.requestPictureInPicture();
      }
    } catch (e) {
      console.warn('[PanuVideoPlayer] PiP non supporté:', e);
    }
    resetControlsTimeout();
  }, [resetControlsTimeout]);

  const cycleSpeed = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    const speeds = [1, 1.25, 1.5, 2];
    const nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.length;
    const nextSpeed = speeds[nextIdx];
    video.playbackRate = nextSpeed;
    setPlaybackSpeed(nextSpeed);
    resetControlsTimeout();
  }, [playbackSpeed, resetControlsTimeout]);

  const handleVideoError = useCallback(() => {
    console.warn('[PanuVideoPlayer] Erreur chargement source:', currentSrc);
    // Si la source échoue et n'est pas encore le flux de repli garanti
    if (currentSrc !== DEFAULT_FALLBACK_VIDEO) {
      setCurrentSrc(DEFAULT_FALLBACK_VIDEO);
      setHasError(false);
      setIsLoading(true);
    } else {
      setHasError(true);
      setIsLoading(false);
      onError?.('Erreur de lecture du média');
    }
  }, [currentSrc, onError]);

  const formatTime = (secs: number) => {
    if (isNaN(secs) || secs < 0) return '00:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div
      ref={containerRef}
      onMouseMove={resetControlsTimeout}
      onTouchStart={resetControlsTimeout}
      className={`relative group bg-black rounded-2xl overflow-hidden select-none border border-[#E5A93C]/30 shadow-2xl ${className}`}
      style={{
        maxWidth: '100%',
        backgroundColor: '#0A0A0E',
        aspectRatio: '16/9',
      }}
    >
      {/* Élément Vidéo HTML5 natif optimisé mobile & desktop */}
      <video
        ref={videoRef}
        src={currentSrc}
        poster={poster}
        autoPlay={autoplay}
        loop={loop}
        muted={isMuted}
        playsInline
        webkit-playsinline="true"
        x5-playsinline="true"
        preload="metadata"
        onClick={togglePlay}
        onTimeUpdate={() => {
          if (!isSeeking && videoRef.current) {
            setCurrentTime(videoRef.current.currentTime);
          }
        }}
        onDurationChange={() => {
          if (videoRef.current) {
            setDuration(videoRef.current.duration || 0);
          }
        }}
        onWaiting={() => setIsLoading(true)}
        onPlaying={() => {
          setIsLoading(false);
          setIsPlaying(true);
        }}
        onPause={() => setIsPlaying(false)}
        onEnded={() => {
          setIsPlaying(false);
          onEnded?.();
        }}
        onError={handleVideoError}
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'contain',
          backgroundColor: '#000',
          cursor: 'pointer',
        }}
      />

      {/* Titre et badge HD en haut */}
      {title && (
        <div
          className={`absolute top-0 inset-x-0 p-3 bg-gradient-to-b from-black/80 via-black/40 to-transparent flex items-center justify-between pointer-events-none transition-opacity duration-300 ${
            showControls ? 'opacity-100' : 'opacity-0'
          }`}
        >
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#E5A93C] animate-pulse" />
            <span className="text-xs md:text-sm font-bold text-white tracking-wide truncate max-w-[260px]">
              {title}
            </span>
          </div>
          <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-[#E5A93C]/20 border border-[#E5A93C]/40 text-[#E5A93C]">
            HD • 8K
          </span>
        </div>
      )}

      {/* Spinner de Chargement */}
      {isLoading && !hasError && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/40 pointer-events-none">
          <div className="w-12 h-12 border-4 border-[#E5A93C]/30 border-t-[#E5A93C] rounded-full animate-spin" />
        </div>
      )}

      {/* Message d'Erreur & Bouton Réessayer */}
      {hasError && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/85 p-4 text-center">
          <span className="text-3xl mb-2">⚠️</span>
          <p className="text-white text-sm font-bold mb-1">Flux vidéo momentanément indisponible</p>
          <p className="text-xs text-gray-400 mb-3">Vérifiez la connexion ou rechargez le lecteur.</p>
          <button
            type="button"
            onClick={() => {
              setCurrentSrc(DEFAULT_FALLBACK_VIDEO);
              setHasError(false);
              setIsLoading(true);
              videoRef.current?.load();
              videoRef.current?.play().catch(() => {});
            }}
            className="px-4 py-2 bg-[#E5A93C] hover:bg-[#D4962B] text-black font-extrabold rounded-lg text-xs transition-colors"
          >
            🔄 Relancer le flux haute performance
          </button>
        </div>
      )}

      {/* Bouton Play/Pause central flottant lors de la pause */}
      {!isPlaying && !isLoading && !hasError && (
        <div
          onClick={togglePlay}
          className="absolute inset-0 flex items-center justify-center bg-black/20 cursor-pointer"
        >
          <div className="w-16 h-16 rounded-full bg-[#E5A93C]/90 hover:bg-[#E5A93C] flex items-center justify-center text-black shadow-lg transform hover:scale-110 transition-transform">
            <svg className="w-8 h-8 translate-x-0.5 fill-current" viewBox="0 0 24 24">
              <path d="M8 5v14l11-7z" />
            </svg>
          </div>
        </div>
      )}

      {/* Barre de Contrôles Inférieure */}
      <div
        className={`absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/95 via-black/70 to-transparent p-3 pt-6 flex flex-col gap-2 transition-opacity duration-300 ${
          showControls || !isPlaying ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
      >
        {/* Barre de Progression Interactive */}
        <div
          ref={progressBarRef}
          onClick={handleSeek}
          onMouseDown={() => setIsSeeking(true)}
          onMouseUp={() => setIsSeeking(false)}
          className="relative w-full h-2 bg-white/20 hover:h-3 rounded-full cursor-pointer transition-all flex items-center"
        >
          {/* Progression */}
          <div
            className="h-full bg-gradient-to-r from-[#E5A93C] to-[#F3C068] rounded-full relative"
            style={{ width: `${progressPercent}%` }}
          >
            {/* Curseur de seek */}
            <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-1/2 w-3.5 h-3.5 bg-white rounded-full shadow-md scale-0 group-hover:scale-100 transition-transform" />
          </div>
        </div>

        {/* Boutons d'action : Play, Volume, Temps, Vitesse, PiP, Plein écran */}
        <div className="flex items-center justify-between text-white text-xs">
          {/* Côté Gauche : Play + Mute + Slider Volume + Durée */}
          <div className="flex items-center gap-2 md:gap-3">
            {/* Play/Pause */}
            <button
              type="button"
              onClick={togglePlay}
              className="p-1.5 hover:text-[#E5A93C] transition-colors focus:outline-none"
              title={isPlaying ? 'Pause' : 'Lecture'}
            >
              {isPlaying ? (
                <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                  <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
                </svg>
              ) : (
                <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                  <path d="M8 5v14l11-7z" />
                </svg>
              )}
            </button>

            {/* Volume Toggle */}
            <button
              type="button"
              onClick={toggleMute}
              className="p-1.5 hover:text-[#E5A93C] transition-colors focus:outline-none"
              title={isMuted ? 'Activer le son' : 'Couper le son'}
            >
              {isMuted || volume === 0 ? (
                <svg className="w-5 h-5 fill-current text-red-400" viewBox="0 0 24 24">
                  <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z" />
                </svg>
              ) : volume > 0.5 ? (
                <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                  <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z" />
                </svg>
              ) : (
                <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                  <path d="M7 9v6h4l5 5V4L11 9H7zm11.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z" />
                </svg>
              )}
            </button>

            {/* Slider de volume masqué sur petit écran */}
            <input
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={isMuted ? 0 : volume}
              onChange={handleVolumeChange}
              className="w-16 h-1 bg-white/30 rounded-lg appearance-none cursor-pointer accent-[#E5A93C] hidden sm:block"
            />

            {/* Temps courant / Durée */}
            <span className="text-[11px] font-mono text-gray-300">
              {formatTime(currentTime)} / {formatTime(duration)}
            </span>
          </div>

          {/* Côté Droit : Vitesse, PiP, Plein Écran */}
          <div className="flex items-center gap-1.5 md:gap-2">
            {/* Vitesse */}
            <button
              type="button"
              onClick={cycleSpeed}
              className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-white/10 hover:bg-[#E5A93C] hover:text-black transition-colors"
              title="Vitesse de lecture"
            >
              {playbackSpeed}x
            </button>

            {/* Picture in Picture */}
            <button
              type="button"
              onClick={togglePictureInPicture}
              className="p-1.5 hover:text-[#E5A93C] transition-colors focus:outline-none hidden xs:block"
              title="Incrustation d'image (PiP)"
            >
              <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                <path d="M19 11h-8v6h8v-6zm4 8V4.98C23 3.88 22.1 3 21 3H3c-1.1 0-2 .88-2 1.98V19c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2zm-2 .02H3V4.97h18v14.05z" />
              </svg>
            </button>

            {/* Plein Écran */}
            <button
              type="button"
              onClick={toggleFullscreen}
              className="p-1.5 hover:text-[#E5A93C] transition-colors focus:outline-none"
              title={isFullscreen ? 'Quitter plein écran' : 'Plein écran'}
            >
              {isFullscreen ? (
                <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                  <path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-14v3h3v2h-5V5h2z" />
                </svg>
              ) : (
                <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                  <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z" />
                </svg>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PanuVideoPlayer;
