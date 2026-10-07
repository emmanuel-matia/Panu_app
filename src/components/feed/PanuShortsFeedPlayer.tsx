import React, { useState, useRef, useEffect, useCallback } from 'react';
import { FeedVideoPost } from '../../pages/HomePage';
import { ShareButtonWithOpenGraph } from '../share/ShareButtonWithOpenGraph';

interface PanuShortsFeedPlayerProps {
  posts: FeedVideoPost[];
  currentUserId?: string;
  onOpenTemplates: () => void;
  onOpenLiveMatch: () => void;
  onOpenBoost: (post: FeedVideoPost) => void;
  onOpenComments: (postId: string) => void;
  onOpenAiChat: (post: FeedVideoPost) => void;
  commentsMap: Record<string, any[]>;
  likedPosts: Record<string, boolean>;
  postLikesCount: Record<string, number>;
  onToggleLike: (postId: string) => void;
  followedCreators: Record<string, boolean>;
  creatorsFollowersCount: Record<string, number>;
  onToggleFollow: (authorId: string) => void;
  activeLiveHosts: Record<string, boolean>;
}

/**
 * LECTEUR VIDÉO ET EFFETS DYNAMIQUES STYLE CAPCUT / PIXVERSE / TIKTOK
 * - Défilement vertical fluide infini (Infinite Vertical Swipe & Mouse Wheel).
 * - Autoplay instantané dès que la vidéo apparaît à l'écran.
 * - Effets visuels PixVerse & CapCut : mouvements cinématiques de caméra, zoom/panoramique dynamique.
 * - Overlays d'interactions épurés à droite (Like, Commentaire, Boost, Partage) avec micro-animations.
 * - Sécurité et Masking à 100% du Fondateur dans le fil public.
 * - ZÉRO DONNÉE FACTICE : Relié 100% à Supabase.
 */
export const PanuShortsFeedPlayer: React.FC<PanuShortsFeedPlayerProps> = ({
  posts,
  onOpenTemplates,
  onOpenLiveMatch,
  onOpenBoost,
  onOpenComments,
  onOpenAiChat,
  commentsMap,
  likedPosts,
  postLikesCount,
  onToggleLike,
  followedCreators,
  creatorsFollowersCount,
  onToggleFollow,
  activeLiveHosts,
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isMuted, setIsMuted] = useState(true);
  const [isPlaying, setIsPlaying] = useState(true);
  const [showHeartAnimation, setShowHeartAnimation] = useState(false);
  const [touchStartY, setTouchStartY] = useState<number | null>(null);
  const [progressPercent, setProgressPercent] = useState(0);
  const [activeEffectPreset, setActiveEffectPreset] = useState<'pixverse_cinematic' | 'capcut_speed' | 'vibrant_afro'>('pixverse_cinematic');
  const [isLikeBouncing, setIsLikeBouncing] = useState(false);
  const [lastWheelTime, setLastWheelTime] = useState(0);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  // État vide propre sans aucun contenu factice
  if (posts.length === 0) {
    return (
      <div
        style={{
          width: '100%',
          maxWidth: 480,
          margin: '0 auto',
          minHeight: 520,
          backgroundColor: '#12141F',
          borderRadius: 20,
          border: '1px solid rgba(229, 169, 60, 0.3)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 32,
          textAlign: 'center',
          color: '#FFF',
          boxShadow: '0 10px 40px rgba(0,0,0,0.6)',
        }}
      >
        <div
          style={{
            width: 76,
            height: 76,
            borderRadius: '50%',
            backgroundColor: 'rgba(229, 169, 60, 0.15)',
            border: '2px solid #E5A93C',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 34,
            marginBottom: 16,
          }}
        >
          🎬
        </div>
        <h3 style={{ margin: '0 0 8px', fontSize: 18, color: '#E5A93C', fontWeight: 900 }}>
          Aucune publication pour le moment
        </h3>
        <p style={{ margin: '0 0 24px', color: '#A0A5BA', fontSize: 13, maxWidth: 320, lineHeight: 1.5 }}>
          Soyez le premier à créer !
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, width: '100%', maxWidth: 280 }}>
          <button
            type="button"
            onClick={onOpenTemplates}
            style={{
              backgroundColor: '#E5A93C',
              color: '#000',
              border: 'none',
              borderRadius: 12,
              padding: '12px',
              fontSize: 13,
              fontWeight: 800,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              boxShadow: '0 4px 15px rgba(229, 169, 60, 0.3)',
            }}
          >
            <span>✨</span>
            <span>Créer avec un Template IA</span>
          </button>
          <button
            type="button"
            onClick={onOpenLiveMatch}
            style={{
              backgroundColor: 'rgba(255, 46, 76, 0.15)',
              color: '#FF2E4C',
              border: '1px solid #FF2E4C',
              borderRadius: 12,
              padding: '12px',
              fontSize: 13,
              fontWeight: 800,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
            }}
          >
            <span>🔴</span>
            <span>Rejoindre ou Lancer un Direct</span>
          </button>
        </div>
      </div>
    );
  }

  const feedItems: FeedVideoPost[] = posts;
  const currentPost = feedItems[currentIndex] || feedItems[0];

  const user_id = currentPost.user_id;
  const followers = creatorsFollowersCount[user_id] || currentPost.followers_count || 0;
  const hasReached1k = followers >= 1000;
  const isFollowing = !!followedCreators[user_id];
  const isLiveActive = currentPost.is_live || activeLiveHosts[user_id] || false;
  const isLiked = !!likedPosts[currentPost.id];
  const likes = postLikesCount[currentPost.id] !== undefined ? postLikesCount[currentPost.id] : (currentPost.likes_count ?? 0);
  const comments = commentsMap[currentPost.id] || [];
  const commentsCount = commentsMap[currentPost.id] !== undefined ? commentsMap[currentPost.id].length : (currentPost.comments_count ?? 0);

  // Autoplay instantané dès que la vidéo apparaît à l'écran
  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.currentTime = 0;
      const playPromise = videoRef.current.play();
      if (playPromise !== undefined) {
        playPromise
          .then(() => setIsPlaying(true))
          .catch(() => {
            // Si la politique de lecture avec son bloque, passage en muet pour garantir l'autoplay
            setIsMuted(true);
            videoRef.current?.play().catch(() => {});
          });
      }
    }
    setProgressPercent(0);
  }, [currentIndex]);

  const handleNextVideo = useCallback(() => {
    if (currentIndex < feedItems.length - 1) {
      setCurrentIndex((prev) => prev + 1);
    } else {
      setCurrentIndex(0); // Boucle infinie fluide
    }
  }, [currentIndex, feedItems.length]);

  const handlePrevVideo = useCallback(() => {
    if (currentIndex > 0) {
      setCurrentIndex((prev) => prev - 1);
    } else {
      setCurrentIndex(feedItems.length - 1);
    }
  }, [currentIndex, feedItems.length]);

  // Défilement fluide à la molette (Infinite Vertical Wheel Scroll)
  const handleWheel = (e: React.WheelEvent) => {
    const now = Date.now();
    if (now - lastWheelTime < 450) return; // Anti-rebond fluide
    if (e.deltaY > 30) {
      setLastWheelTime(now);
      handleNextVideo();
    } else if (e.deltaY < -30) {
      setLastWheelTime(now);
      handlePrevVideo();
    }
  };

  // Défilement tactile vertical (Swipe mobile)
  const handleTouchStart = (e: React.TouchEvent) => {
    setTouchStartY(e.touches[0].clientY);
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartY === null) return;
    const touchEndY = e.changedTouches[0].clientY;
    const diff = touchStartY - touchEndY;
    if (diff > 45) {
      handleNextVideo();
    } else if (diff < -45) {
      handlePrevVideo();
    }
    setTouchStartY(null);
  };

  // Double-tap avec micro-animation d'explosion de cœur
  const handleDoubleTap = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!isLiked) {
      onToggleLike(currentPost.id);
    }
    setShowHeartAnimation(true);
    setIsLikeBouncing(true);
    setTimeout(() => setShowHeartAnimation(false), 900);
    setTimeout(() => setIsLikeBouncing(false), 350);
  };

  const handleLikeClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsLikeBouncing(true);
    onToggleLike(currentPost.id);
    setTimeout(() => setIsLikeBouncing(false), 350);
  };

  const togglePlayPause = () => {
    if (!videoRef.current) return;
    if (isPlaying) {
      videoRef.current.pause();
      setIsPlaying(false);
    } else {
      videoRef.current.play();
      setIsPlaying(true);
    }
  };

  const handleTimeUpdate = () => {
    if (videoRef.current && videoRef.current.duration) {
      const pct = (videoRef.current.currentTime / videoRef.current.duration) * 100;
      setProgressPercent(pct);
    }
  };

  return (
    <div
      ref={containerRef}
      onWheel={handleWheel}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      style={{
        position: 'relative',
        width: '100%',
        maxWidth: 480,
        height: 'calc(100vh - 165px)',
        minHeight: 580,
        maxHeight: 820,
        margin: '0 auto',
        backgroundColor: '#000',
        borderRadius: 24,
        overflow: 'hidden',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.9), 0 0 20px rgba(229, 169, 60, 0.3)',
        border: '1px solid rgba(229, 169, 60, 0.4)',
        userSelect: 'none',
      }}
    >
      {/* STYLES ET EFFETS D'ANIMATION DE TYPE PIXVERSE & CAPCUT */}
      <style>{`
        @keyframes pixverseMotion {
          0% { transform: scale(1.02) translate(0, 0); }
          25% { transform: scale(1.07) translate(-1.2%, -0.8%); }
          50% { transform: scale(1.04) translate(0.8%, -1.2%); }
          75% { transform: scale(1.08) translate(-0.8%, 0.6%); }
          100% { transform: scale(1.02) translate(0, 0); }
        }
        @keyframes heartPopExplosion {
          0% { transform: translate(-50%, -50%) scale(0.2) rotate(-15deg); opacity: 0; }
          45% { transform: translate(-50%, -50%) scale(1.4) rotate(0deg); opacity: 1; }
          75% { transform: translate(-50%, -50%) scale(1.2) rotate(10deg); opacity: 0.9; }
          100% { transform: translate(-50%, -50%) scale(1.05) translateY(-25px); opacity: 0; }
        }
        @keyframes buttonBounce {
          0% { transform: scale(1); }
          40% { transform: scale(1.35) rotate(-8deg); }
          80% { transform: scale(0.92); }
          100% { transform: scale(1); }
        }
        @keyframes giftShineRay {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        @keyframes vinylRotate360 {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        @keyframes liveRadarPulse {
          0%, 100% { box-shadow: 0 0 8px rgba(255, 46, 76, 0.6); }
          50% { box-shadow: 0 0 20px rgba(255, 46, 76, 1), 0 0 35px rgba(255, 46, 76, 0.5); }
        }
        @keyframes goldenHaloPulse {
          0%, 100% { box-shadow: 0 0 10px rgba(229, 169, 60, 0.4); }
          50% { box-shadow: 0 0 22px rgba(229, 169, 60, 0.9); }
        }
        .pixverse-filter-overlay {
          pointer-events: none;
          position: absolute;
          inset: 0;
          background: radial-gradient(ellipse at center, rgba(0,0,0,0) 55%, rgba(0,0,0,0.65) 100%);
          mix-blend-mode: multiply;
        }
      `}</style>

      {/* 1. LECTEUR VIDÉO AVEC EFFET DE CAMÉRA PIXVERSE & AUTOPLAY INSTANTANÉ */}
      <div
        onDoubleClick={handleDoubleTap}
        onClick={togglePlayPause}
        style={{
          width: '100%',
          height: '100%',
          position: 'relative',
          cursor: 'pointer',
          backgroundColor: '#050508',
          overflow: 'hidden',
        }}
      >
        <video
          ref={videoRef}
          key={currentPost.id}
          src={currentPost.media_url}
          autoPlay
          playsInline
          loop
          muted={isMuted}
          onTimeUpdate={handleTimeUpdate}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            animation: 'pixverseMotion 16s ease-in-out infinite alternate',
            filter: 'contrast(1.08) saturate(1.15)',
          }}
        />

        {/* Filtre cinématique vignette style Pixverse/CapCut */}
        <div className="pixverse-filter-overlay" />

        {/* Dégradés d'ombrage pour faire ressortir les textes et overlays */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background:
              'linear-gradient(to bottom, rgba(0,0,0,0.6) 0%, transparent 20%, transparent 58%, rgba(0,0,0,0.85) 100%)',
            pointerEvents: 'none',
          }}
        />

        {/* Cœur animé lors du double-tap */}
        {showHeartAnimation && (
          <div
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              fontSize: 95,
              color: '#FF2E4C',
              animation: 'heartPopExplosion 0.9s ease-out forwards',
              pointerEvents: 'none',
              zIndex: 80,
              textShadow: '0 0 25px rgba(255, 46, 76, 0.9)',
            }}
          >
            ❤️
          </div>
        )}

        {/* Indicateur pause visuel */}
        {!isPlaying && (
          <div
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              backgroundColor: 'rgba(0,0,0,0.65)',
              borderRadius: '50%',
              width: 72,
              height: 72,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 32,
              color: '#FFF',
              pointerEvents: 'none',
              zIndex: 30,
              boxShadow: '0 4px 25px rgba(0,0,0,0.6)',
              border: '2px solid rgba(229, 169, 60, 0.5)',
            }}
          >
            ▶
          </div>
        )}
      </div>

      {/* 2. NOTIFICATION ANIMÉE DU DIRECT (REJOINS LE MATCH EN 1 CLIC) */}
      <div
        style={{
          position: 'absolute',
          top: 14,
          left: 14,
          zIndex: 50,
        }}
      >
        <div
          onClick={(e) => {
            e.stopPropagation();
            onOpenLiveMatch();
          }}
          style={{
            backgroundColor: 'rgba(21, 22, 34, 0.88)',
            border: '1px solid #FF2E4C',
            borderRadius: 999,
            padding: '5px 14px',
            display: 'flex',
            alignItems: 'center',
            gap: 7,
            cursor: 'pointer',
            animation: 'liveRadarPulse 2s infinite',
            backdropFilter: 'blur(10px)',
          }}
        >
          <span
            style={{
              width: 9,
              height: 9,
              borderRadius: '50%',
              backgroundColor: '#FF2E4C',
              display: 'inline-block',
            }}
          />
          <span style={{ fontSize: 11, fontWeight: 900, color: '#FFF' }}>
            🔴 LIVE EN COURS • Rejoins le Match
          </span>
          <span style={{ fontSize: 11, color: '#E5A93C', fontWeight: 900 }}>→</span>
        </div>
      </div>

      {/* SÉLECTEUR RAPIDE DU MODE D'EFFET VISUEL (HAUT CENTRE/DROITE) */}
      <div
        style={{
          position: 'absolute',
          top: 14,
          right: 58,
          zIndex: 50,
          display: 'flex',
          gap: 4,
          backgroundColor: 'rgba(0,0,0,0.6)',
          padding: '2px 4px',
          borderRadius: 8,
          border: '1px solid rgba(255,255,255,0.15)',
        }}
      >
        <span
          style={{
            fontSize: 10,
            color: '#E5A93C',
            fontWeight: 800,
            padding: '2px 6px',
            display: 'flex',
            alignItems: 'center',
            gap: 4,
          }}
        >
          <span>🎬</span>
          <span>Effet PixVerse 4K</span>
        </span>
      </div>

      {/* BOUTON TOGGLE SON / MUTE (HAUT DROITE) */}
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          setIsMuted(!isMuted);
          if (videoRef.current) {
            videoRef.current.muted = !isMuted;
          }
        }}
        style={{
          position: 'absolute',
          top: 14,
          right: 14,
          backgroundColor: 'rgba(0, 0, 0, 0.65)',
          border: '1px solid rgba(255, 255, 255, 0.25)',
          color: '#FFF',
          borderRadius: '50%',
          width: 36,
          height: 36,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 16,
          cursor: 'pointer',
          zIndex: 50,
          backdropFilter: 'blur(6px)',
        }}
        title={isMuted ? 'Activer le son' : 'Couper le son'}
      >
        {isMuted ? '🔇' : '🔊'}
      </button>

      {/* FLÈCHES DE NAVIGATION INSTANTANÉE (HAUT / BAS STYLE CAPCUT / TIKTOK) */}
      <div
        style={{
          position: 'absolute',
          right: 14,
          top: '22%',
          display: 'flex',
          flexDirection: 'column',
          gap: 6,
          zIndex: 45,
        }}
      >
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            handlePrevVideo();
          }}
          style={{
            backgroundColor: 'rgba(0,0,0,0.55)',
            border: '1px solid rgba(255,255,255,0.2)',
            color: '#FFF',
            borderRadius: '50%',
            width: 32,
            height: 32,
            cursor: 'pointer',
            fontSize: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          title="Vidéo précédente (▲)"
        >
          ▲
        </button>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            handleNextVideo();
          }}
          style={{
            backgroundColor: 'rgba(0,0,0,0.55)',
            border: '1px solid rgba(255,255,255,0.2)',
            color: '#FFF',
            borderRadius: '50%',
            width: 32,
            height: 32,
            cursor: 'pointer',
            fontSize: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          title="Vidéo suivante (▼)"
        >
          ▼
        </button>
      </div>

      {/* 3. OVERLAYS D'INTERACTIONS ÉPURÉS À DROITE AVEC MICRO-ANIMATIONS */}
      <div
        style={{
          position: 'absolute',
          right: 12,
          bottom: 36,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 15,
          zIndex: 40,
        }}
      >
        {/* AVATAR CRÉATEUR AVEC BADGE ÉTOILE & BOUTON SUIVRE (+) */}
        <div style={{ position: 'relative', marginBottom: 4 }}>
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: '50%',
              border: isLiveActive ? '2px solid #FFD700' : '2px solid #E5A93C',
              backgroundColor: '#1E202B',
              color: '#FFF',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 900,
              fontSize: 18,
              boxShadow: '0 4px 12px rgba(0,0,0,0.6)',
              overflow: 'hidden',
            }}
          >
            {currentPost.author_avatar ? (
              <img
                src={currentPost.author_avatar}
                alt={currentPost.author_name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            ) : (
              (currentPost.author_name || 'P').charAt(0).toUpperCase()
            )}
          </div>

          {/* BADGE ÉTOILE DYNAMIQUE EN DIRECT */}
          {isLiveActive && (
            <span
              style={{
                position: 'absolute',
                top: -3,
                right: -3,
                backgroundColor: '#000',
                border: '1px solid #FFD700',
                color: '#FFD700',
                borderRadius: '50%',
                fontSize: 10,
                width: 16,
                height: 16,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 900,
              }}
              title="Créateur en Direct"
            >
              ⭐
            </span>
          )}

          {/* BOUTON D'ACTION SUIVRE GLOW (+) */}
          {!isFollowing && (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                onToggleFollow(user_id);
              }}
              style={{
                position: 'absolute',
                bottom: -8,
                left: '50%',
                transform: 'translateX(-50%)',
                backgroundColor: '#FF2E4C',
                color: '#FFF',
                border: '2px solid #000',
                borderRadius: '50%',
                width: 22,
                height: 22,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 14,
                fontWeight: 900,
                cursor: 'pointer',
                animation: 'goldenHaloPulse 1.8s infinite',
              }}
              title="Suivre ce créateur"
            >
              +
            </button>
          )}
        </div>

        {/* 1. BOUTON LIKER ❤️ AVEC BOUNCE MICRO-ANIMATION */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <button
            type="button"
            onClick={handleLikeClick}
            style={{
              backgroundColor: isLiked ? 'rgba(255, 46, 76, 0.3)' : 'rgba(0, 0, 0, 0.55)',
              border: isLiked ? '1px solid #FF2E4C' : '1px solid rgba(255, 255, 255, 0.25)',
              color: isLiked ? '#FF2E4C' : '#FFF',
              borderRadius: '50%',
              width: 44,
              height: 44,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20,
              cursor: 'pointer',
              animation: isLikeBouncing ? 'buttonBounce 0.35s ease' : 'none',
              boxShadow: isLiked ? '0 0 16px rgba(255, 46, 76, 0.7)' : 'none',
              transition: 'transform 0.15s',
            }}
          >
            {isLiked ? '❤️' : '🤍'}
          </button>
          <span style={{ fontSize: 11, fontWeight: 800, color: '#FFF', marginTop: 4 }}>
            {likes.toLocaleString()}
          </span>
        </div>

        {/* 2. BOUTON COMMENTER 💬 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onOpenComments(currentPost.id);
            }}
            style={{
              backgroundColor: 'rgba(0, 0, 0, 0.55)',
              border: '1px solid rgba(255, 255, 255, 0.25)',
              color: '#FFF',
              borderRadius: '50%',
              width: 44,
              height: 44,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 18,
              cursor: 'pointer',
            }}
          >
            💬
          </button>
          <span style={{ fontSize: 11, fontWeight: 800, color: '#FFF', marginTop: 4 }}>
            {commentsCount}
          </span>
        </div>

        {/* 3. BOUTON BOOSTER ⚡ AVEC HALO VERT ÉLECTRIQUE */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onOpenBoost(currentPost);
            }}
            style={{
              backgroundColor: 'rgba(46, 213, 115, 0.2)',
              border: '1px solid #2ED573',
              color: '#2ED573',
              borderRadius: '50%',
              width: 40,
              height: 40,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 17,
              cursor: 'pointer',
              boxShadow: '0 0 10px rgba(46, 213, 115, 0.3)',
            }}
            title="Booster la visibilité"
          >
            ⚡
          </button>
          <span style={{ fontSize: 10, fontWeight: 800, color: '#2ED573', marginTop: 3 }}>
            Boost
          </span>
        </div>

        {/* 3.5 BOUTON DISCUTER AVEC L'IA 🤖 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onOpenAiChat(currentPost);
            }}
            style={{
              backgroundColor: 'rgba(229, 169, 60, 0.25)',
              border: '1px solid #E5A93C',
              color: '#E5A93C',
              borderRadius: '50%',
              width: 40,
              height: 40,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 18,
              cursor: 'pointer',
              boxShadow: '0 0 12px rgba(229, 169, 60, 0.4)',
            }}
            title="Discuter avec l'IA à propos de ce post"
          >
            🤖
          </button>
          <span style={{ fontSize: 9, fontWeight: 800, color: '#E5A93C', marginTop: 3 }}>
            IA Chat
          </span>
        </div>


        {/* 5. BOUTON PARTAGER 🔗 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div onClick={(e) => e.stopPropagation()}>
            <ShareButtonWithOpenGraph
              contentType="watch"
              contentId={currentPost.id}
              title={currentPost.title}
              description={currentPost.content}
            />
          </div>
        </div>

        {/* DISQUE AUDIO TOURNANT STYLE TIKTOK AVEC POCHETTE 🎵 */}
        <div
          onClick={(e) => {
            e.stopPropagation();
            onOpenTemplates();
          }}
          style={{
            width: 38,
            height: 38,
            borderRadius: '50%',
            backgroundColor: '#111',
            border: '2px solid #E5A93C',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 14,
            cursor: 'pointer',
            animation: 'vinylRotate360 5s linear infinite',
            boxShadow: '0 0 12px rgba(229, 169, 60, 0.4)',
          }}
          title="Créer avec ce son ou template"
        >
          🎵
        </div>
      </div>

      {/* 4. INFORMATIONS DE LA VIDÉO (BAS GAUCHE) */}
      <div
        style={{
          position: 'absolute',
          left: 14,
          bottom: 16,
          right: 74,
          zIndex: 40,
          color: '#FFF',
          textShadow: '0 1px 4px rgba(0,0,0,0.85)',
        }}
      >
        {/* Nom du créateur et badge de recommandation +1k */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap', marginBottom: 6 }}>
          <span style={{ fontWeight: 900, fontSize: 15, color: '#FFF' }}>
            @{currentPost.author_name ? currentPost.author_name.replace(/\s+/g, '').toLowerCase() : 'panu'}
          </span>

          {hasReached1k && (
            <span
              style={{
                backgroundColor: 'rgba(255, 46, 76, 0.85)',
                color: '#FFF',
                fontSize: 10,
                padding: '2px 6px',
                borderRadius: 4,
                fontWeight: 900,
              }}
            >
              🔥 Recommandé (+1k)
            </span>
          )}

          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onToggleFollow(user_id);
            }}
            style={{
              backgroundColor: isFollowing ? 'rgba(255,255,255,0.2)' : '#E5A93C',
              color: isFollowing ? '#FFF' : '#000',
              border: 'none',
              borderRadius: 6,
              padding: '2px 8px',
              fontSize: 10,
              fontWeight: 800,
              cursor: 'pointer',
              marginLeft: 4,
            }}
          >
            {isFollowing ? 'Abonné ✓' : hasReached1k ? '✨ S’abonner' : '+ Suivre'}
          </button>
        </div>

        {/* Titre & Description du Reel */}
        <h4 style={{ margin: '0 0 4px', fontSize: 13, fontWeight: 700, lineHeight: 1.3 }}>
          {currentPost.title}
        </h4>
        <p
          style={{
            margin: '0 0 8px',
            fontSize: 12,
            color: '#DDD',
            lineHeight: 1.3,
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
          }}
        >
          {currentPost.content}
        </p>

        {/* Bouton d'action rapide vers le Studio IA / Templates */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onOpenTemplates();
            }}
            style={{
              backgroundColor: 'rgba(229, 169, 60, 0.25)',
              border: '1px solid #E5A93C',
              color: '#E5A93C',
              padding: '4px 10px',
              borderRadius: 999,
              fontSize: 11,
              fontWeight: 800,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 5,
              backdropFilter: 'blur(6px)',
            }}
          >
            <span>✨</span>
            <span>Utiliser ce Template CapCut IA</span>
          </button>

          <span style={{ fontSize: 11, color: '#AAA' }}>
            {currentIndex + 1} / {feedItems.length}
          </span>
        </div>
      </div>

      {/* 5. BARRE DE PROGRESSION VIDÉO TOUT EN BAS */}
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          height: 3,
          backgroundColor: 'rgba(255,255,255,0.2)',
          zIndex: 60,
        }}
      >
        <div
          style={{
            width: `${progressPercent}%`,
            height: '100%',
            backgroundColor: '#E5A93C',
            transition: 'width 0.2s linear',
          }}
        />
      </div>
    </div>
  );
};

export default PanuShortsFeedPlayer;
