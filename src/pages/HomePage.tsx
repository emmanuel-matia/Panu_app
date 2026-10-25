import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Heart,
  MessageCircle,
  Bookmark,
  Share2,
  Shuffle,
  Rocket,
  Search,
  Plus,
  Tv,
  Music,
  Home,
  Film,
  Sparkles,
  User,
  X,
  Volume2,
  VolumeX,
  Play,
  Pause,
  Check,
  Send,
  Camera,
  Upload,
  RefreshCw,
  ChevronUp,
  ChevronDown,
  MessageSquare,
  CheckCheck,
} from 'lucide-react';
import { supabase } from '../lib/supabaseClient';
import { STORAGE_BUCKETS } from '../config/supabaseConfig';
import { DynamicTemplateGallery } from '../components/studio/DynamicTemplateGallery';
import { VideoRoom } from '../components/live/VideoRoom';
import { sendInstantGmailAlertToFounder } from '../services/securityModerationService';
import {
  fetchUserLikedPostIds,
  fetchLikesCountMap,
  togglePostLike,
  subscribeToRealtimeLikes,
} from '../services/likeService';
import {
  fetchCommentsForPosts,
  addPostComment,
  subscribeToRealtimeComments,
} from '../services/commentService';
import { toggleFollow, fetchFollowingIds, subscribeToRealtimeFollows } from '../services/followService';
import { useNotifications } from '../context/NotificationContext';

export interface FeedVideoPost {
  id: string;
  user_id: string;
  author_name?: string;
  author_avatar?: string;
  author_email?: string;
  followers_count?: number;
  is_live?: boolean;
  title: string;
  content: string;
  media_url: string;
  media_type: string;
  is_public: boolean;
  likes_count?: number;
  comments_count?: number;
  bookmarks_count?: number;
  shares_count?: number;
  music_title?: string;
  created_at: string;
}

export interface PostComment {
  id: string;
  postId: string;
  authorName: string;
  text: string;
  createdAt: string;
}

export interface HomePageProps {
  searchQuery?: string;
  showMyPublications?: boolean;
}

// Vidéos d'introduction et démonstration ultra fluides (9:16)
const INITIAL_FALLBACK_POSTS: FeedVideoPost[] = [
  {
    id: 'panu-spotlight-1',
    user_id: '00000000-0000-0000-0000-000000000001',
    author_name: 'PANU_Studio',
    author_avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
    title: 'Afrobeat Session & Montage Vidéo IA',
    content: 'Remixe cette vidéo ou booste ton contenu ! 🚀',
    media_url: 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    media_type: 'video',
    is_public: true,
    likes_count: 990500,
    comments_count: 25300,
    bookmarks_count: 139500,
    shares_count: 16000,
    music_title: 'Morceau original - PANU Studio',
    created_at: new Date().toISOString(),
  },
  {
    id: 'panu-spotlight-2',
    user_id: '00000000-0000-0000-0000-000000000002',
    author_name: 'AfroDance_Kinshasa',
    author_avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
    title: 'Chorégraphie Freestyle & Énergie Urbaine',
    content: 'Nouveau son, nouvelle danse ! Remixe cette vidéo ou booste ton contenu ! 🚀',
    media_url: 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    media_type: 'video',
    is_public: true,
    likes_count: 642800,
    comments_count: 18200,
    bookmarks_count: 92400,
    shares_count: 11200,
    music_title: 'Afrobeat Energy #2 - PANU Studio',
    created_at: new Date().toISOString(),
  },
  {
    id: 'panu-spotlight-3',
    user_id: '00000000-0000-0000-0000-000000000003',
    author_name: 'PixVerse_Creative',
    author_avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
    title: 'Cinématique Nuit Urbaine & Lumières Néon',
    content: 'Créé à 100% avec le Studio IA. Remixe cette vidéo ou booste ton contenu ! 🚀',
    media_url: 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
    media_type: 'video',
    is_public: true,
    likes_count: 421000,
    comments_count: 14700,
    bookmarks_count: 53100,
    shares_count: 8900,
    music_title: 'Cyber Afro Vibe - PANU Studio',
    created_at: new Date().toISOString(),
  },
];

// Formatage compact des compteurs (990,5K, 25,3K, 16K, etc.)
const formatCount = (count?: number): string => {
  if (!count || count <= 0) return '0';
  if (count >= 1_000_000) {
    const val = (count / 1_000_000).toFixed(1).replace('.', ',');
    return val.endsWith(',0') ? `${val.slice(0, -2)}M` : `${val}M`;
  }
  if (count >= 10_000) {
    const val = (count / 1_000).toFixed(1).replace('.', ',');
    return val.endsWith(',0') ? `${val.slice(0, -2)}K` : `${val}K`;
  }
  if (count >= 1_000) {
    const val = (count / 1_000).toFixed(1).replace('.', ',');
    return val.endsWith(',0') ? `${val.slice(0, -2)}K` : `${val}K`;
  }
  return count.toString();
};

export const HomePage: React.FC<HomePageProps> = ({ searchQuery = '', showMyPublications = false }) => {
  const navigate = useNavigate();
  const { currentUserId, currentUserName, notifications, unreadCount, markAsRead, markAllAsRead } = useNotifications();
  const [showNotificationDrawer, setShowNotificationDrawer] = useState(false);
  const [isLiveActive, setIsLiveActive] = useState(false);

  // Navigation supérieure : Suivis | Découvrir | Amis | Pour toi
  const [activeHeaderTab, setActiveHeaderTab] = useState<'following' | 'discover' | 'friends' | 'for_you'>('for_you');
  const [isSearchActive, setIsSearchActive] = useState(false);
  const [localSearch, setLocalSearch] = useState(searchQuery || '');

  // Données de publications & index actif
  const [posts, setPosts] = useState<FeedVideoPost[]>(INITIAL_FALLBACK_POSTS);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [userEmail, setUserEmail] = useState('');

  // États d'interactions
  const [likedPosts, setLikedPosts] = useState<Record<string, boolean>>({
    'panu-spotlight-1': true,
  });
  const [postLikesCount, setPostLikesCount] = useState<Record<string, number>>({
    'panu-spotlight-1': 990500,
    'panu-spotlight-2': 642800,
    'panu-spotlight-3': 421000,
  });
  const [bookmarkedPosts, setBookmarkedPosts] = useState<Record<string, boolean>>({});
  const [bookmarksCount, setBookmarksCount] = useState<Record<string, number>>({
    'panu-spotlight-1': 139500,
    'panu-spotlight-2': 92400,
    'panu-spotlight-3': 53100,
  });
  const [sharesCount, setSharesCount] = useState<Record<string, number>>({
    'panu-spotlight-1': 16000,
    'panu-spotlight-2': 11200,
    'panu-spotlight-3': 8900,
  });
  const [commentsMap, setCommentsMap] = useState<Record<string, PostComment[]>>({
    'panu-spotlight-1': [
      { id: 'c1', postId: 'panu-spotlight-1', authorName: 'Fatou', text: 'Incroyable créativité ! 🔥🔥', createdAt: 'Il y a 10 min' },
      { id: 'c2', postId: 'panu-spotlight-1', authorName: 'Malik_Art', text: 'Le remix est disponible ? J’adore le beat !', createdAt: 'Il y a 5 min' },
    ],
  });
  const [followedCreators, setFollowedCreators] = useState<Record<string, boolean>>({});

  // Modales
  const [activeCommentsPostId, setActiveCommentsPostId] = useState<string | null>(null);
  const [commentInput, setCommentInput] = useState('');
  const [showBoostModalForPost, setShowBoostModalForPost] = useState<FeedVideoPost | null>(null);
  const [showRemixModalForPost, setShowRemixModalForPost] = useState<FeedVideoPost | null>(null);
  const [showTemplatesModal, setShowTemplatesModal] = useState(false);
  const [showPublishBox, setShowPublishBox] = useState(false);

  // Lecteur vidéo et contrôles
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [, setIsPlaying] = useState(true);
  const [isMuted, setIsMuted] = useState(false);
  const [showPlayPauseFeedback, setShowPlayPauseFeedback] = useState<'play' | 'pause' | null>(null);
  const [flyingHearts, setFlyingHearts] = useState<{ id: number; x: number; y: number }[]>([]);

  // Studio de Création Complet (Caméra, Durée, Import, Retouches, Miniatures)
  const [selectedDuration, setSelectedDuration] = useState<'15s' | '60s' | '3 min'>('15s');
  const [captureMode, setCaptureMode] = useState<'video' | 'photo'>('video');
  const [isRecording, setIsRecording] = useState(false);
  const [recordedSeconds, setRecordedSeconds] = useState(0);
  const [selectedFilter, setSelectedFilter] = useState('Normal');
  const [selectedRatio, setSelectedRatio] = useState<'9:16' | '1:1' | '16:9'>('9:16');
  const [selectedCoverIndex, setSelectedCoverIndex] = useState(0);
  const [selectedMusicTitle, setSelectedMusicTitle] = useState('Morceau original - PANU Studio');
  const [newTitle, setNewTitle] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [cameraFacing, setCameraFacing] = useState<'user' | 'environment'>('user');
  const [isPublishing, setIsPublishing] = useState(false);
  const publishVideoRef = useRef<HTMLVideoElement | null>(null);
  const publishStreamRef = useRef<MediaStream | null>(null);

  // Gestion du Swipe & Gestes
  const touchStartY = useRef<number | null>(null);
  const lastWheelTime = useRef<number>(0);

  // Minuteur d'enregistrement direct
  useEffect(() => {
    let interval: any;
    if (isRecording) {
      const maxSec = selectedDuration === '15s' ? 15 : selectedDuration === '60s' ? 60 : 180;
      interval = setInterval(() => {
        setRecordedSeconds((prev) => {
          if (prev + 1 >= maxSec) {
            setIsRecording(false);
            return maxSec;
          }
          return prev + 1;
        });
      }, 1000);
    } else {
      setRecordedSeconds(0);
    }
    return () => clearInterval(interval);
  }, [isRecording, selectedDuration]);

  // Synchronisation du paramètre de recherche externe
  useEffect(() => {
    if (searchQuery) {
      setLocalSearch(searchQuery);
      setIsSearchActive(true);
    }
  }, [searchQuery]);

  // Récupération des publications Supabase
  const fetchFeeds = useCallback(async () => {
    try {
      const { data: dbPosts, error } = await supabase
        .from('posts')
        .select('*')
        .eq('is_public', true)
        .order('created_at', { ascending: false })
        .limit(30);

      if (error) {
        console.warn('Erreur Supabase posts:', error.message);
        return;
      }

      if (dbPosts && dbPosts.length > 0) {
        const userIds = Array.from(new Set(dbPosts.map((p) => p.user_id).filter(Boolean)));
        let profilesMap: Record<string, any> = {};

        if (userIds.length > 0) {
          const { data: profiles } = await supabase
            .from('profiles')
            .select('id, username, full_name, avatar_url')
            .in('id', userIds);
          if (profiles) {
            profiles.forEach((pr) => {
              profilesMap[pr.id] = pr;
            });
          }
        }

        const enriched: FeedVideoPost[] = dbPosts.map((p: any) => {
          const profile = profilesMap[p.user_id];
          return {
            ...p,
            author_name: profile?.username || profile?.full_name || 'PANU_Studio',
            author_avatar: profile?.avatar_url || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
            media_url: p.media_url || 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
            likes_count: p.likes_count ?? 990500,
            comments_count: p.comments_count ?? 25300,
            bookmarks_count: 139500,
            shares_count: 16000,
            music_title: `Morceau original - ${profile?.username || 'PANU Studio'}`,
          };
        });

        // Combiner posts réels en premier + nos fallbacks stylisés
        const combined = [...enriched, ...INITIAL_FALLBACK_POSTS.filter(fb => !enriched.some(e => e.id === fb.id))];
        setPosts(combined);

        // Likes & Commentaires
        const postIds = combined.map((p) => p.id);
        const [likesCountMap, userLikedSet, realCommentsMap] = await Promise.all([
          fetchLikesCountMap(postIds),
          fetchUserLikedPostIds(currentUserId, postIds),
          fetchCommentsForPosts(postIds),
        ]);

        setPostLikesCount((prev) => ({ ...prev, ...likesCountMap }));
        const likedMap: Record<string, boolean> = { 'panu-spotlight-1': true };
        postIds.forEach((id) => {
          if (userLikedSet.has(id)) likedMap[id] = true;
        });
        setLikedPosts((prev) => ({ ...prev, ...likedMap }));
        setCommentsMap((prev) => ({ ...prev, ...realCommentsMap }));
      }
    } catch (e) {
      console.warn('Fallback au flux de démonstration', e);
    }
  }, [currentUserId]);

  useEffect(() => {
    supabase.auth.getUser().then(({ data }) => {
      if (data?.user?.email) {
        setUserEmail(data.user.email);
      }
    });

    fetchFeeds();

    // Souscription temps réel aux LIKES
    const unsubLikes = subscribeToRealtimeLikes((event) => {
      const delta = event.eventType === 'INSERT' ? 1 : -1;
      setPostLikesCount((prev) => ({
        ...prev,
        [event.postId]: Math.max(0, (prev[event.postId] || 0) + delta),
      }));
      if (currentUserId && event.userId === currentUserId) {
        setLikedPosts((prev) => ({ ...prev, [event.postId]: event.eventType === 'INSERT' }));
      }
    });

    // Souscription temps réel aux COMMENTAIRES
    const unsubComments = subscribeToRealtimeComments((newComment) => {
      setCommentsMap((prev) => {
        const existing = prev[newComment.postId] || [];
        if (existing.some((c) => c.id === newComment.id)) return prev;
        return {
          ...prev,
          [newComment.postId]: [newComment, ...existing],
        };
      });
    });

    // Abonnements de l'utilisateur
    if (currentUserId) {
      fetchFollowingIds(currentUserId).then((ids) => {
        const fMap: Record<string, boolean> = {};
        ids.forEach((id) => {
          fMap[id] = true;
        });
        setFollowedCreators(fMap);
      });
    }

    const unsubFollows = subscribeToRealtimeFollows((event) => {
      if (event.followingId && currentUserId && event.followerId === currentUserId) {
        setFollowedCreators((prev) => ({
          ...prev,
          [event.followingId]: event.eventType === 'INSERT',
        }));
      }
    });

    return () => {
      unsubLikes();
      unsubComments();
      unsubFollows();
    };
  }, [fetchFeeds, currentUserId]);

  // Filtrage selon onglets supérieurs & recherche
  const displayPosts = posts.filter((post) => {
    if (showMyPublications && post.user_id !== currentUserId) return false;
    if (activeHeaderTab === 'following' && !followedCreators[post.user_id]) return false;
    if (activeHeaderTab === 'discover') {
      // Contenus et modèles à la une / Templates
      const isFeatured =
        (post.title || '').toLowerCase().includes('ia') ||
        (post.title || '').toLowerCase().includes('montage') ||
        (post.content || '').toLowerCase().includes('remixe') ||
        (post.likes_count || 0) > 500000;
      if (!isFeatured && posts.some((p) => (p.title || '').toLowerCase().includes('ia'))) return false;
    }
    if (localSearch.trim()) {
      const q = localSearch.toLowerCase().trim();
      const match =
        (post.title || '').toLowerCase().includes(q) ||
        (post.content || '').toLowerCase().includes(q) ||
        (post.author_name || '').toLowerCase().includes(q);
      if (!match) return false;
    }
    return true;
  });

  const effectivePosts = displayPosts.length > 0 ? displayPosts : posts;
  const currentPost = effectivePosts[currentIndex] || posts[0] || INITIAL_FALLBACK_POSTS[0];

  // Transition vers vidéo suivante / précédente
  const goToNextVideo = useCallback(() => {
    setCurrentIndex((prev) => (prev + 1 < effectivePosts.length ? prev + 1 : 0));
  }, [effectivePosts.length]);

  const goToPrevVideo = useCallback(() => {
    setCurrentIndex((prev) => (prev - 1 >= 0 ? prev - 1 : effectivePosts.length - 1));
  }, [effectivePosts.length]);

  // Gestion du swipe tactile
  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartY.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartY.current === null) return;
    const touchEndY = e.changedTouches[0].clientY;
    const diff = touchStartY.current - touchEndY;
    if (Math.abs(diff) > 50) {
      if (diff > 0) {
        goToNextVideo();
      } else {
        goToPrevVideo();
      }
    }
    touchStartY.current = null;
  };

  // Gestion de la molette souris
  const handleWheel = (e: React.WheelEvent) => {
    const now = Date.now();
    if (now - lastWheelTime.current < 450) return;
    lastWheelTime.current = now;
    if (e.deltaY > 20) {
      goToNextVideo();
    } else if (e.deltaY < -20) {
      goToPrevVideo();
    }
  };

  // Lecture / Pause
  const togglePlayPause = () => {
    if (!videoRef.current) return;
    if (videoRef.current.paused) {
      videoRef.current.play();
      setIsPlaying(true);
      setShowPlayPauseFeedback('play');
    } else {
      videoRef.current.pause();
      setIsPlaying(false);
      setShowPlayPauseFeedback('pause');
    }
    setTimeout(() => setShowPlayPauseFeedback(null), 600);
  };

  // Double tap pour Liker
  const lastTapRef = useRef<number>(0);
  const handleVideoTap = (e: React.MouseEvent<HTMLDivElement>) => {
    const now = Date.now();
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    if (now - lastTapRef.current < 300) {
      // Double tap détecté
      if (!likedPosts[currentPost.id]) {
        handleToggleLike(currentPost.id);
      }
      const newHeart = { id: Date.now(), x, y };
      setFlyingHearts((prev) => [...prev, newHeart]);
      setTimeout(() => {
        setFlyingHearts((prev) => prev.filter((h) => h.id !== newHeart.id));
      }, 1000);
    } else {
      // Simple tap
      togglePlayPause();
    }
    lastTapRef.current = now;
  };

  // Action LIKE
  const handleToggleLike = async (postId: string) => {
    const isCurrentlyLiked = !!likedPosts[postId];
    const delta = isCurrentlyLiked ? -1 : 1;

    setLikedPosts((prev) => ({ ...prev, [postId]: !isCurrentlyLiked }));
    setPostLikesCount((prev) => ({
      ...prev,
      [postId]: Math.max(0, (prev[postId] ?? (currentPost.likes_count || 0)) + delta),
    }));

    if (currentUserId) {
      await togglePostLike({
        postId,
        userId: currentUserId,
        isCurrentlyLiked,
        postAuthorId: currentPost?.user_id,
        postTitle: currentPost?.title,
        actorName: currentUserName,
      });
    }
  };

  // Action FAVORIS / BOOKMARK
  const handleToggleBookmark = (postId: string) => {
    const isBookmarked = !!bookmarkedPosts[postId];
    setBookmarkedPosts((prev) => ({ ...prev, [postId]: !isBookmarked }));
    setBookmarksCount((prev) => ({
      ...prev,
      [postId]: (prev[postId] ?? 139500) + (isBookmarked ? -1 : 1),
    }));
  };

  // Action PARTAGER
  const handleShare = async (post: FeedVideoPost) => {
    const shareUrl = `${window.location.origin}/watch/${post.id}`;
    if (navigator.share) {
      try {
        await navigator.share({
          title: post.title,
          text: `Découvre "${post.title}" sur PANU Studio !`,
          url: shareUrl,
        });
      } catch {
        // Annulé par l'utilisateur
      }
    } else {
      navigator.clipboard.writeText(shareUrl);
      alert('🔗 Lien de la vidéo copié dans le presse-papiers !');
    }
    setSharesCount((prev) => ({
      ...prev,
      [post.id]: (prev[post.id] ?? 16000) + 1,
    }));
  };

  // Action SUIVRE / S'ABONNER
  const handleToggleFollow = async (authorId: string) => {
    if (!currentUserId) {
      alert('Connectez-vous pour suivre ce créateur.');
      return;
    }
    setFollowedCreators((prev) => ({ ...prev, [authorId]: !prev[authorId] }));
    await toggleFollow(currentUserId, authorId);
  };

  // Action AJOUTER COMMENTAIRE
  const handleAddComment = async (postId: string) => {
    if (!commentInput.trim()) return;
    const text = commentInput.trim();
    setCommentInput('');

    const tempComment: PostComment = {
      id: `comm_${Date.now()}`,
      postId,
      authorName: currentUserName || 'Vous',
      text,
      createdAt: 'À l’instant',
    };

    setCommentsMap((prev) => ({
      ...prev,
      [postId]: [tempComment, ...(prev[postId] || [])],
    }));

    if (currentUserId) {
      await addPostComment({
        postId,
        userId: currentUserId,
        content: text,
        authorName: currentUserName,
        postAuthorId: currentPost?.user_id,
        postTitle: currentPost?.title,
      });
    }
  };

  // Gestion Caméra Réelle pour Créer
  const startCamera = async (facing: 'user' | 'environment' = cameraFacing) => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: facing },
        audio: true,
      });
      publishStreamRef.current = stream;
      if (publishVideoRef.current) {
        publishVideoRef.current.srcObject = stream;
      }
      setIsCameraActive(true);
    } catch {
      alert('Impossible d’accéder à la caméra.');
    }
  };

  const stopCamera = () => {
    if (publishStreamRef.current) {
      publishStreamRef.current.getTracks().forEach((track) => track.stop());
      publishStreamRef.current = null;
    }
    setIsCameraActive(false);
  };

  const handlePublish = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    setIsPublishing(true);

    try {
      const { data: userData } = await supabase.auth.getUser();
      const authorId = userData?.user?.id || currentUserId || '00000000-0000-0000-0000-000000000001';
      let finalMediaUrl = 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4';
      let mediaType = 'video';

      if (selectedFile) {
        const fileExt = selectedFile.name.split('.').pop() || 'mp4';
        const fileName = `${Date.now()}_${Math.random().toString(36).substring(2, 8)}.${fileExt}`;
        const filePath = `feed/${fileName}`;
        mediaType = selectedFile.type.startsWith('video') ? 'video' : 'image';

        const { error: uploadError } = await supabase.storage
          .from(STORAGE_BUCKETS.POST_MEDIA)
          .upload(filePath, selectedFile, { cacheControl: '3600', upsert: false });

        if (!uploadError) {
          const { data: publicUrlData } = supabase.storage
            .from(STORAGE_BUCKETS.POST_MEDIA)
            .getPublicUrl(filePath);
          finalMediaUrl = publicUrlData.publicUrl;
        }
      }

      const { data: inserted, error } = await supabase.from('posts').insert({
        user_id: authorId,
        title: newTitle.trim(),
        content: newTitle.trim(),
        media_url: finalMediaUrl,
        media_type: mediaType,
        is_public: true,
      }).select().single();

      if (!error && inserted) {
        setNewTitle('');
        setSelectedFile(null);
        stopCamera();
        setShowPublishBox(false);
        alert('🎉 Publication publiée en plein écran sur PANU !');
        fetchFeeds();

        sendInstantGmailAlertToFounder({
          eventType: 'IMPORTANT_APP_EVENT',
          subject: `🎬 [PANU] Nouvelle publication : ${newTitle.trim().slice(0, 30)}`,
          userIdentifier: userEmail || authorId,
          details: `Post publié par ${currentUserName || 'Créateur'} : "${newTitle.trim()}".`,
        });
      } else {
        alert(error ? error.message : 'Erreur publication');
      }
    } catch (err: any) {
      alert(`Erreur : ${err?.message || 'Échec publication'}`);
    } finally {
      setIsPublishing(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[110] bg-black text-white overflow-hidden select-none flex flex-col justify-between"
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      onWheel={handleWheel}
    >
      {/* ============================================================================== */}
      {/* 1. DISPOSITION IMMERSIVE : LECTEUR VIDÉO PLEIN ÉCRAN (h-screen w-full)        */}
      {/* ============================================================================== */}
      <div className="absolute inset-0 w-full h-full bg-black cursor-pointer" onClick={handleVideoTap}>
        {currentPost.media_type === 'image' ? (
          <img
            src={currentPost.media_url}
            alt={currentPost.title}
            className="w-full h-full object-cover select-none"
          />
        ) : (
          <video
            ref={videoRef}
            src={currentPost.media_url}
            autoPlay
            loop
            playsInline
            muted={isMuted}
            className="w-full h-full object-cover select-none"
            onPlay={() => setIsPlaying(true)}
            onPause={() => setIsPlaying(false)}
          />
        )}

        {/* Dégradés cinématiques supérieur et inférieur pour sublimer les textes et contrôles */}
        <div className="absolute top-0 left-0 right-0 h-36 bg-gradient-to-b from-black/85 via-black/40 to-transparent pointer-events-none" />
        <div className="absolute bottom-0 left-0 right-0 h-64 bg-gradient-to-t from-black/90 via-black/50 to-transparent pointer-events-none" />

        {/* Cœur animé volant lors du double-tap */}
        {flyingHearts.map((heart) => (
          <div
            key={heart.id}
            style={{ left: heart.x - 40, top: heart.y - 40 }}
            className="absolute pointer-events-none animate-ping text-red-500 z-40 transition-all duration-700"
          >
            <Heart className="w-20 h-20 fill-red-500 drop-shadow-[0_0_20px_rgba(239,68,68,0.9)]" />
          </div>
        ))}

        {/* Feedback visuel lors du tap simple Play / Pause */}
        {showPlayPauseFeedback && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-30">
            <div className="w-20 h-20 rounded-full bg-black/60 backdrop-blur-md flex items-center justify-center text-white/90 animate-scaleIn">
              {showPlayPauseFeedback === 'play' ? (
                <Play className="w-10 h-10 fill-white translate-x-0.5" />
              ) : (
                <Pause className="w-10 h-10 fill-white" />
              )}
            </div>
          </div>
        )}

        {/* Indicateurs de navigation rapide haut/bas */}
        <div className="absolute left-3 top-1/2 -translate-y-1/2 flex flex-col gap-2 z-20 pointer-events-auto opacity-0 hover:opacity-100 transition-opacity">
          <button
            onClick={(e) => {
              e.stopPropagation();
              goToPrevVideo();
            }}
            className="w-8 h-8 rounded-full bg-black/40 backdrop-blur-md border border-white/20 flex items-center justify-center hover:bg-white/20 active:scale-95"
            title="Vidéo précédente"
          >
            <ChevronUp className="w-5 h-5 text-white" />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              goToNextVideo();
            }}
            className="w-8 h-8 rounded-full bg-black/40 backdrop-blur-md border border-white/20 flex items-center justify-center hover:bg-white/20 active:scale-95"
            title="Vidéo suivante"
          >
            <ChevronDown className="w-5 h-5 text-white" />
          </button>
        </div>
      </div>

      {/* ============================================================================== */}
      {/* 2. EN-TÊTE SUPÉRIEUR TRANSPARENT (HEADER)                                      */}
      {/* ============================================================================== */}
      <header className="relative z-30 pt-3 px-4 flex items-center justify-between pointer-events-auto">
        {/* À gauche : Badge / Bouton 📺 LIVE rouge */}
        <button
          onClick={() => setIsLiveActive(true)}
          className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-black/40 backdrop-blur-md border border-red-500/50 text-red-500 font-extrabold text-xs tracking-wider shadow-md hover:bg-red-500/20 active:scale-95 transition-all"
        >
          <Tv className="w-3.5 h-3.5 text-red-500 animate-pulse" />
          <span>LIVE</span>
        </button>

        {/* Au centre : Onglets Suivis | Découvrir | Amis | Pour toi (actif avec barre indicatrice) */}
        <div className="flex items-center gap-4 sm:gap-6 text-sm md:text-base">
          <button
            onClick={() => setActiveHeaderTab('following')}
            className={`transition-colors relative pb-1 font-semibold ${
              activeHeaderTab === 'following'
                ? 'text-white font-bold'
                : 'text-white/60 hover:text-white'
            }`}
          >
            Suivis
            {activeHeaderTab === 'following' && (
              <span className="absolute bottom-0 left-1/2 -translate-x-1/2 w-6 h-[3px] bg-white rounded-full shadow-sm" />
            )}
          </button>

          <button
            onClick={() => setActiveHeaderTab('discover')}
            className={`transition-colors relative pb-1 font-semibold ${
              activeHeaderTab === 'discover'
                ? 'text-amber-400 font-bold'
                : 'text-white/60 hover:text-white'
            }`}
          >
            Découvrir
            {activeHeaderTab === 'discover' && (
              <span className="absolute bottom-0 left-1/2 -translate-x-1/2 w-8 h-[3px] bg-amber-400 rounded-full shadow-md" />
            )}
          </button>

          <button
            onClick={() => setActiveHeaderTab('friends')}
            className={`transition-colors relative pb-1 font-semibold ${
              activeHeaderTab === 'friends'
                ? 'text-white font-bold'
                : 'text-white/60 hover:text-white'
            }`}
          >
            Amis
            {activeHeaderTab === 'friends' && (
              <span className="absolute bottom-0 left-1/2 -translate-x-1/2 w-6 h-[3px] bg-white rounded-full shadow-sm" />
            )}
          </button>

          <button
            onClick={() => setActiveHeaderTab('for_you')}
            className={`transition-colors relative pb-1 font-bold ${
              activeHeaderTab === 'for_you'
                ? 'text-white font-black'
                : 'text-white/60 hover:text-white'
            }`}
          >
            Pour toi
            {activeHeaderTab === 'for_you' && (
              <span className="absolute bottom-0 left-1/2 -translate-x-1/2 w-7 h-[3px] bg-white rounded-full shadow-md" />
            )}
          </button>
        </div>

        {/* À droite : Icône Messagerie + Recherche + Mute */}
        <div className="flex items-center gap-2">
          {/* BOUTON MESSAGERIE & NOTIFICATIONS (Remplace le compteur "60" et le petit bouton "+") */}
          <button
            onClick={() => setShowNotificationDrawer(true)}
            className="relative p-2 rounded-full bg-black/30 backdrop-blur-md text-amber-500 hover:text-white hover:bg-white/10 transition-colors"
            title="Messages et notifications"
          >
            <MessageSquare className="w-4 h-4 text-amber-400" />
            {unreadCount > 0 && (
              <span className="absolute -top-1 -right-1 min-w-[16px] h-[16px] px-1 bg-red-500 text-white text-[9px] font-black rounded-full flex items-center justify-center border border-black shadow">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>

          <button
            onClick={() => setIsMuted(!isMuted)}
            className="p-2 rounded-full bg-black/30 backdrop-blur-md text-white/80 hover:text-white hover:bg-white/10 transition-colors"
            title={isMuted ? 'Activer le son' : 'Couper le son'}
          >
            {isMuted ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
          </button>
          <button
            onClick={() => setIsSearchActive(!isSearchActive)}
            className="p-2 rounded-full bg-black/30 backdrop-blur-md text-white/90 hover:text-white hover:bg-white/10 transition-colors"
            title="Rechercher"
          >
            <Search className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Barre de recherche déroulante si activée */}
      {isSearchActive && (
        <div className="relative z-30 px-4 pt-2 pb-1 animate-in slide-in-from-top-2 duration-200">
          <div className="relative flex items-center">
            <Search className="w-4 h-4 text-white/60 absolute left-3" />
            <input
              type="text"
              value={localSearch}
              onChange={(e) => setLocalSearch(e.target.value)}
              placeholder="Rechercher un créateur, une vidéo ou un mot-clé..."
              className="w-full bg-black/75 backdrop-blur-md border border-white/20 rounded-full pl-9 pr-9 py-2 text-xs md:text-sm text-white placeholder-white/50 outline-none focus:border-amber-500 shadow-xl"
              autoFocus
            />
            {localSearch && (
              <button
                onClick={() => setLocalSearch('')}
                className="absolute right-3 text-white/60 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      )}

      {/* ============================================================================== */}
      {/* 3. BARRE D'ACTIONS LATÉRALE (COLONNE DROITE)                                  */}
      {/* ============================================================================== */}
      <div className="absolute right-3 bottom-24 z-30 flex flex-col items-center gap-3.5 text-white pointer-events-auto">
        {/* 1. Avatar du Créateur avec petit bouton rouge + pour s'abonner */}
        <div className="relative mb-1 flex flex-col items-center">
          <button
            onClick={() => navigate('/profile')}
            className="w-12 h-12 rounded-full overflow-hidden border-2 border-white bg-slate-800 shadow-xl flex items-center justify-center font-bold text-base hover:scale-105 transition-transform"
          >
            {currentPost.author_avatar ? (
              <img
                src={currentPost.author_avatar}
                alt={currentPost.author_name}
                className="w-full h-full object-cover"
              />
            ) : (
              <span>{(currentPost.author_name || 'P').charAt(0).toUpperCase()}</span>
            )}
          </button>
          {!followedCreators[currentPost.user_id] ? (
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleToggleFollow(currentPost.user_id);
              }}
              className="absolute -bottom-1.5 left-1/2 -translate-x-1/2 w-5 h-5 rounded-full bg-red-500 text-white flex items-center justify-center border-2 border-black shadow-md hover:scale-110 active:scale-95 transition-transform"
              title="S'abonner"
            >
              <Plus className="w-3.5 h-3.5 stroke-[3]" />
            </button>
          ) : (
            <div className="absolute -bottom-1.5 left-1/2 -translate-x-1/2 w-4 h-4 rounded-full bg-white text-black flex items-center justify-center border border-black shadow-sm">
              <Check className="w-2.5 h-2.5 stroke-[3]" />
            </div>
          )}
        </div>

        {/* 2. ❤️ Like : Icône Cœur + compteur formaté (ex: 990,5 K) */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            handleToggleLike(currentPost.id);
          }}
          className="flex flex-col items-center group active:scale-90 transition-transform"
        >
          <div className="p-1 rounded-full group-hover:scale-110 transition-transform">
            <Heart
              className={`w-8 h-8 transition-colors drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)] ${
                likedPosts[currentPost.id]
                  ? 'text-red-500 fill-red-500'
                  : 'text-white hover:text-red-400'
              }`}
            />
          </div>
          <span className="text-[11px] font-bold text-white drop-shadow-[0_1px_4px_rgba(0,0,0,0.9)] -mt-0.5">
            {formatCount(postLikesCount[currentPost.id] ?? currentPost.likes_count)}
          </span>
        </button>

        {/* 3. 💬 Commentaires : Icône Bulle + compteur (ouvre le panneau onOpenComments) */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            setActiveCommentsPostId(currentPost.id);
          }}
          className="flex flex-col items-center group active:scale-90 transition-transform"
        >
          <div className="p-1 rounded-full group-hover:scale-110 transition-transform">
            <MessageCircle className="w-8 h-8 text-white drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]" />
          </div>
          <span className="text-[11px] font-bold text-white drop-shadow-[0_1px_4px_rgba(0,0,0,0.9)] -mt-0.5">
            {formatCount((commentsMap[currentPost.id] || []).length || currentPost.comments_count || 25300)}
          </span>
        </button>

        {/* 4. 🔖 Favoris : Icône Signet + compteur */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            handleToggleBookmark(currentPost.id);
          }}
          className="flex flex-col items-center group active:scale-90 transition-transform"
        >
          <div className="p-1 rounded-full group-hover:scale-110 transition-transform">
            <Bookmark
              className={`w-7 h-7 drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)] ${
                bookmarkedPosts[currentPost.id]
                  ? 'text-amber-400 fill-amber-400'
                  : 'text-white hover:text-amber-300'
              }`}
            />
          </div>
          <span className="text-[11px] font-bold text-white drop-shadow-[0_1px_4px_rgba(0,0,0,0.9)] -mt-0.5">
            {formatCount(bookmarksCount[currentPost.id] ?? currentPost.bookmarks_count ?? 139500)}
          </span>
        </button>

        {/* 5. ↗️ Partager : Icône Flèche + compteur */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            handleShare(currentPost);
          }}
          className="flex flex-col items-center group active:scale-90 transition-transform"
        >
          <div className="p-1 rounded-full group-hover:scale-110 transition-transform">
            <Share2 className="w-7 h-7 text-white drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]" />
          </div>
          <span className="text-[11px] font-bold text-white drop-shadow-[0_1px_4px_rgba(0,0,0,0.9)] -mt-0.5">
            {formatCount(sharesCount[currentPost.id] ?? currentPost.shares_count ?? 16000)}
          </span>
        </button>

        {/* 6. 🔀 Remix : Icône Remix/Shuffle (ouvre la création dérivée onOpenRemix) */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            setShowRemixModalForPost(currentPost);
          }}
          className="flex flex-col items-center group active:scale-90 transition-transform"
          title="Remixer ce contenu"
        >
          <div className="w-9 h-9 rounded-xl bg-amber-500 hover:bg-amber-400 flex items-center justify-center shadow-lg shadow-amber-500/30 group-hover:scale-105 transition-transform">
            <Shuffle className="w-5 h-5 text-white stroke-[2.5]" />
          </div>
          <span className="text-[10px] font-bold text-white drop-shadow-[0_1px_4px_rgba(0,0,0,0.9)] mt-0.5">
            Remix
          </span>
        </button>

        {/* 7. 🚀 Booster : Icône Fusée (couleur d'accent dorée #f59e0b / text-amber-500) */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            setShowBoostModalForPost(currentPost);
          }}
          className="flex flex-col items-center group active:scale-90 transition-transform"
          title="Booster la visibilité"
        >
          <div className="w-9 h-9 rounded-xl bg-black/50 backdrop-blur-md border border-amber-500/60 flex items-center justify-center shadow-lg shadow-amber-500/20 group-hover:scale-105 transition-transform">
            <Rocket className="w-5 h-5 text-amber-500 fill-amber-500/30 animate-pulse" />
          </div>
          <span className="text-[10px] font-extrabold text-amber-400 drop-shadow-[0_1px_4px_rgba(0,0,0,0.9)] mt-0.5">
            Booster
          </span>
        </button>

        {/* 8. Disque de musique : Vinyle animé qui tourne en bas à droite */}
        <div className="relative mt-1 group cursor-pointer" onClick={() => setShowTemplatesModal(true)}>
          <div className="w-10 h-10 rounded-full bg-slate-900 border-2 border-slate-700/80 p-1 flex items-center justify-center shadow-2xl animate-spin [animation-duration:4s]">
            <div className="w-4 h-4 rounded-full bg-amber-500 border border-black flex items-center justify-center">
              <div className="w-1.5 h-1.5 rounded-full bg-black" />
            </div>
          </div>
          <Music className="w-3.5 h-3.5 text-white/80 absolute -top-1 -left-1 animate-bounce" />
        </div>
      </div>

      {/* ============================================================================== */}
      {/* 4. INFORMATIONS DU CONTENU (BAS-GAUCHE)                                        */}
      {/* ============================================================================== */}
      <div className="absolute left-4 right-20 bottom-24 z-30 pointer-events-auto text-left">
        {/* Nom du créateur (@Panu) */}
        <div className="flex items-center gap-2 mb-1.5">
          <span className="font-extrabold text-white text-base md:text-lg tracking-wide drop-shadow-[0_2px_4px_rgba(0,0,0,0.9)]">
            @{currentPost.author_name ? currentPost.author_name.replace(/\s+/g, '_') : 'Panu'}
          </span>
          <span className="bg-amber-500 text-black font-black text-[9px] px-1.5 py-0.5 rounded-full shadow-sm uppercase">
            Créateur
          </span>
        </div>

        {/* Description de la vidéo + mention */}
        <p className="text-xs md:text-sm text-white/95 leading-snug drop-shadow-[0_1px_4px_rgba(0,0,0,0.9)] mb-2.5 max-w-md line-clamp-2">
          {currentPost.content || currentPost.title || 'Vidéo originale PANU Studio'} Remixe cette vidéo ou booste ton contenu ! 🚀
        </p>

        {/* Morceau de musique défilant avec l'icône 🎵 */}
        <div className="flex items-center gap-2 text-xs text-white/90 drop-shadow-[0_1px_3px_rgba(0,0,0,0.9)] overflow-hidden max-w-xs">
          <Music className="w-3.5 h-3.5 text-amber-400 flex-shrink-0 animate-pulse" />
          <div className="overflow-hidden whitespace-nowrap">
            <span className="inline-block animate-marquee font-medium">
              {currentPost.music_title || 'Morceau original - PANU Studio'}
            </span>
          </div>
        </div>
      </div>

      {/* ============================================================================== */}
      {/* 5. NAVIGATION INFÉRIEURE (BOTTOM NAVIGATION BAR)                              */}
      {/* Barre sombre fixe en bas (fixed bottom-0 bg-black w-full border-t)           */}
      {/* ============================================================================== */}
      <nav className="fixed bottom-0 left-0 right-0 bg-black w-full border-t border-slate-800 z-40 px-3 py-2 flex items-center justify-around pointer-events-auto shadow-2xl">
        {/* 1. 🏠 Accueil (Onglet actif, redirige vers /) */}
        <button
          onClick={() => {
            setCurrentIndex(0);
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }}
          className="flex flex-col items-center text-white font-bold transition-transform active:scale-95"
        >
          <Home className="w-6 h-6 text-white" />
          <span className="text-[11px] mt-0.5 tracking-tight">Accueil</span>
        </button>

        {/* 2. 🎬 Templates (Mis en avant en couleur dorée/orange #f59e0b) */}
        <button
          onClick={() => setShowTemplatesModal(true)}
          className="flex flex-col items-center text-amber-500 font-bold transition-transform active:scale-95"
        >
          <Film className="w-6 h-6 text-amber-500" />
          <span className="text-[11px] mt-0.5 tracking-tight text-amber-500 font-extrabold">
            Templates
          </span>
        </button>

        {/* 3. [ + ] Créer (Grand bouton central coloré style TikTok 3D) */}
        <button
          onClick={() => setShowPublishBox(true)}
          className="flex items-center justify-center transition-transform active:scale-90 px-1"
          title="Créer un contenu"
        >
          <div className="relative flex items-center justify-center">
            <div className="absolute -left-1 w-11 h-8 bg-cyan-400 rounded-lg" />
            <div className="absolute -right-1 w-11 h-8 bg-red-500 rounded-lg" />
            <div className="relative w-11 h-8 bg-white rounded-lg flex items-center justify-center z-10 shadow-md">
              <Plus className="w-5 h-5 text-black stroke-[3.5]" />
            </div>
          </div>
        </button>

        {/* 4. ✨ Studio IA (Redirige vers /studio-ia) */}
        <button
          onClick={() => navigate('/studio-ia')}
          className="flex flex-col items-center text-gray-300 hover:text-white transition-transform active:scale-95"
        >
          <Sparkles className="w-6 h-6 text-amber-400" />
          <span className="text-[11px] mt-0.5 tracking-tight">Studio IA</span>
        </button>

        {/* 5. 👤 Profil (Redirige vers /profile) */}
        <button
          onClick={() => navigate('/profile')}
          className="flex flex-col items-center text-gray-300 hover:text-white transition-transform active:scale-95"
        >
          <User className="w-6 h-6 text-gray-300" />
          <span className="text-[11px] mt-0.5 tracking-tight">Profil</span>
        </button>
      </nav>

      {/* ============================================================================== */}
      {/* 6. MODALE / TIROIR DES COMMENTAIRES (onOpenComments)                           */}
      {/* ============================================================================== */}
      {activeCommentsPostId && (
        <div
          className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-end justify-center"
          onClick={() => setActiveCommentsPostId(null)}
        >
          <div
            className="w-full max-w-lg bg-[#12141F] border-t-2 border-amber-500 rounded-t-3xl max-h-[70vh] flex flex-col p-4 shadow-2xl animate-in slide-in-from-bottom duration-300"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between pb-3 border-b border-white/10">
              <div className="flex items-center gap-2">
                <MessageCircle className="w-5 h-5 text-amber-500" />
                <h3 className="font-extrabold text-base text-white">
                  Commentaires ({(commentsMap[activeCommentsPostId] || []).length})
                </h3>
              </div>
              <button
                onClick={() => setActiveCommentsPostId(null)}
                className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto py-3 space-y-2.5">
              {(commentsMap[activeCommentsPostId] || []).length === 0 ? (
                <div className="text-center py-8 text-white/50 text-sm">
                  Aucun commentaire pour le moment. Soyez le premier à réagir !
                </div>
              ) : (
                (commentsMap[activeCommentsPostId] || []).map((c) => (
                  <div key={c.id} className="bg-white/5 rounded-xl p-3 border border-white/5">
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-extrabold text-xs text-amber-400">
                        {c.authorName}
                      </span>
                      <span className="text-[10px] text-white/40">{c.createdAt}</span>
                    </div>
                    <p className="text-xs text-white/90 leading-relaxed">{c.text}</p>
                  </div>
                ))
              )}
            </div>

            <div className="pt-2 border-t border-white/10 flex items-center gap-2">
              <input
                type="text"
                value={commentInput}
                onChange={(e) => setCommentInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleAddComment(activeCommentsPostId)}
                placeholder="Ajouter un commentaire..."
                className="flex-1 bg-white/10 border border-white/15 rounded-full px-4 py-2.5 text-xs text-white placeholder-white/40 outline-none focus:border-amber-500"
              />
              <button
                onClick={() => handleAddComment(activeCommentsPostId)}
                className="w-10 h-10 rounded-full bg-amber-500 hover:bg-amber-400 text-black flex items-center justify-center font-bold active:scale-95 transition-transform"
              >
                <Send className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================================== */}
      {/* 7. MODALE BOOSTER DE VISIBILITÉ (onOpenBoost)                                  */}
      {/* ============================================================================== */}
      {showBoostModalForPost && (
        <div
          className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          onClick={() => setShowBoostModalForPost(null)}
        >
          <div
            className="w-full max-w-md bg-[#131622] border border-amber-500/50 rounded-2xl p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Rocket className="w-6 h-6 text-amber-500" />
                <h3 className="font-black text-lg text-amber-500">
                  Booster la Vidéo
                </h3>
              </div>
              <button
                onClick={() => setShowBoostModalForPost(null)}
                className="text-white/60 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-white/70 mb-4">
              Propulsez <strong>"{showBoostModalForPost.title}"</strong> en tête du flux « Pour toi » et atteignez des milliers d'utilisateurs.
            </p>

            <div className="space-y-3 mb-4">
              {[
                { name: 'Boost Découverte', views: '+5 000 vues', price: '2 500 FCFA' },
                { name: 'Boost Viral Élite', views: '+25 000 vues', price: '10 000 FCFA' },
              ].map((plan) => (
                <button
                  key={plan.name}
                  onClick={() => {
                    alert(`🚀 Plan "${plan.name}" activé avec succès ! Votre vidéo bénéficie de ${plan.views}.`);
                    setShowBoostModalForPost(null);
                  }}
                  className="w-full bg-black/50 hover:bg-white/5 border border-amber-500/30 hover:border-amber-500 rounded-xl p-3 flex items-center justify-between text-left transition-colors"
                >
                  <div>
                    <div className="font-extrabold text-sm text-white">{plan.name}</div>
                    <div className="text-xs text-amber-400 font-semibold">{plan.views} garanties</div>
                  </div>
                  <div className="font-black text-sm text-amber-500">{plan.price}</div>
                </button>
              ))}
            </div>

            <button
              onClick={() => setShowBoostModalForPost(null)}
              className="w-full py-2.5 rounded-xl border border-white/20 text-white/80 hover:text-white text-xs font-bold"
            >
              Fermer
            </button>
          </div>
        </div>
      )}

      {/* ============================================================================== */}
      {/* 8. MODALE REMIX / CRÉATION DÉRIVÉE (onOpenRemix)                              */}
      {/* ============================================================================== */}
      {showRemixModalForPost && (
        <div
          className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          onClick={() => setShowRemixModalForPost(null)}
        >
          <div
            className="w-full max-w-md bg-[#131622] border border-amber-500/50 rounded-2xl p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Shuffle className="w-6 h-6 text-amber-500" />
                <h3 className="font-black text-lg text-white">
                  Remixer ce Contenu
                </h3>
              </div>
              <button
                onClick={() => setShowRemixModalForPost(null)}
                className="text-white/60 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-white/70 mb-4">
              Réutilisez le style, l'audio et les prompts de <strong>"{showRemixModalForPost.title}"</strong> dans le Studio IA pour créer une version dérivée unique.
            </p>

            <div className="space-y-2.5 mb-4">
              <button
                onClick={() => {
                  setShowRemixModalForPost(null);
                  navigate('/studio-ia');
                }}
                className="w-full py-3 bg-amber-500 hover:bg-amber-400 text-black font-extrabold text-xs rounded-xl shadow-lg shadow-amber-500/30 flex items-center justify-center gap-2"
              >
                <Sparkles className="w-4 h-4" />
                <span>Ouvrir dans le Studio IA</span>
              </button>

              <button
                onClick={() => {
                  navigator.clipboard.writeText(`Remix audio : ${showRemixModalForPost.music_title}`);
                  alert('🎵 Référence musicale copiée !');
                  setShowRemixModalForPost(null);
                }}
                className="w-full py-2.5 bg-white/10 hover:bg-white/20 text-white font-bold text-xs rounded-xl flex items-center justify-center gap-2"
              >
                <Music className="w-4 h-4 text-amber-400" />
                <span>Utiliser ce Son</span>
              </button>
            </div>

            <button
              onClick={() => setShowRemixModalForPost(null)}
              className="w-full py-2 rounded-xl border border-white/20 text-white/70 hover:text-white text-xs font-semibold"
            >
              Annuler
            </button>
          </div>
        </div>
      )}

      {/* ============================================================================== */}
      {/* 9. MODALE TEMPLATES IA (DynamicTemplateGallery)                                 */}
      {/* ============================================================================== */}
      {showTemplatesModal && (
        <div
          className="fixed inset-0 bg-black/85 backdrop-blur-md z-50 flex items-center justify-center p-4"
          onClick={() => setShowTemplatesModal(false)}
        >
          <div
            className="w-full max-w-4xl bg-[#12141F] border border-amber-500/50 rounded-2xl max-h-[85vh] overflow-y-auto p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-4 border-b border-white/10 pb-3">
              <div className="flex items-center gap-2">
                <Film className="w-6 h-6 text-amber-500" />
                <h3 className="font-black text-lg text-amber-500">
                  Galerie des Templates IA PANU
                </h3>
              </div>
              <button
                onClick={() => setShowTemplatesModal(false)}
                className="text-white/60 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <DynamicTemplateGallery
              onTemplateSelect={() => {
                setShowTemplatesModal(false);
                navigate('/studio-ia');
              }}
            />
          </div>
        </div>
      )}

      {/* ============================================================================== */}
      {/* 10. STUDIO DE CRÉATION COMPLET (Caméra Directe, Importer, Durée, Retouches)   */}
      {/* ============================================================================== */}
      {showPublishBox && (
        <div
          className="fixed inset-0 bg-black/90 backdrop-blur-md z-50 flex items-center justify-center p-3 sm:p-4 overflow-y-auto"
          onClick={() => {
            stopCamera();
            setShowPublishBox(false);
          }}
        >
          <div
            className="w-full max-w-lg bg-[#121420] border border-amber-500/40 rounded-3xl p-5 shadow-2xl my-auto text-white max-h-[95vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            {/* EN-TÊTE DU STUDIO DE CRÉATION */}
            <div className="flex items-center justify-between pb-3 border-b border-white/10 mb-3">
              <button
                onClick={() => {
                  stopCamera();
                  setShowPublishBox(false);
                }}
                className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white"
              >
                <X className="w-4 h-4" />
              </button>

              {/* Sélection Sonore en haut */}
              <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/5 border border-amber-500/30 text-amber-400 text-xs font-bold max-w-[220px] truncate">
                <Music className="w-3.5 h-3.5 flex-shrink-0 animate-pulse" />
                <span className="truncate">{selectedMusicTitle}</span>
              </div>

              <button
                type="button"
                onClick={() => {
                  const next = cameraFacing === 'user' ? 'environment' : 'user';
                  setCameraFacing(next);
                  stopCamera();
                  setTimeout(() => startCamera(next), 200);
                }}
                className="p-1.5 rounded-full bg-white/10 hover:bg-white/20 text-white"
                title="Basculer caméra"
              >
                <RefreshCw className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handlePublish} className="space-y-3.5">
              {/* APERÇU CAMÉRA DIRECT OU MÉDIA CHOISI */}
              <div
                className={`relative rounded-2xl overflow-hidden bg-black mx-auto border border-amber-500/40 flex items-center justify-center transition-all ${
                  selectedRatio === '1:1'
                    ? 'aspect-square max-h-56'
                    : selectedRatio === '16:9'
                    ? 'aspect-video max-h-48'
                    : 'aspect-[9/14] max-h-64'
                }`}
              >
                {isCameraActive ? (
                  <video
                    ref={publishVideoRef}
                    autoPlay
                    playsInline
                    muted
                    style={{
                      filter:
                        selectedFilter === 'Vibrant'
                          ? 'contrast(1.2) saturate(1.4)'
                          : selectedFilter === 'Cinéma'
                          ? 'contrast(1.1) brightness(0.95) sepia(0.25)'
                          : selectedFilter === 'Rétro'
                          ? 'sepia(0.6) hue-rotate(-15deg)'
                          : selectedFilter === 'Noir & Blanc'
                          ? 'grayscale(1)'
                          : 'none',
                    }}
                    className="w-full h-full object-cover"
                  />
                ) : selectedFile ? (
                  <div className="w-full h-full flex flex-col items-center justify-center p-4 bg-white/5 text-center">
                    <Upload className="w-10 h-10 text-amber-400 mb-2" />
                    <span className="text-xs font-bold text-white truncate max-w-[240px]">
                      {selectedFile.name}
                    </span>
                    <span className="text-[10px] text-emerald-400 mt-1">
                      ✓ Média prêt à être publié ({selectedFilter})
                    </span>
                  </div>
                ) : (
                  <div className="w-full h-full flex flex-col items-center justify-center p-4 bg-gradient-to-b from-slate-900 to-black text-center">
                    <Camera className="w-10 h-10 text-amber-500/80 mb-2" />
                    <span className="text-xs font-extrabold text-white">Viseur Caméra Prêt</span>
                    <span className="text-[11px] text-white/50 mt-0.5">
                      Activez la caméra ou importez un média
                    </span>
                  </div>
                )}

                {/* Indicateur d'enregistrement en direct */}
                {isRecording && (
                  <div className="absolute top-2 left-2 flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-red-600/90 text-white text-[10px] font-black animate-pulse">
                    <span className="w-2 h-2 rounded-full bg-white" />
                    <span>REC {recordedSeconds}s / {selectedDuration}</span>
                  </div>
                )}
              </div>

              {/* SÉLECTION DE LA DURÉE (15s, 60s, 3 min) */}
              <div className="flex items-center justify-center gap-2 pt-1">
                <span className="text-xs font-bold text-white/60 mr-1">Durée :</span>
                {(['15s', '60s', '3 min'] as const).map((dur) => (
                  <button
                    key={dur}
                    type="button"
                    onClick={() => setSelectedDuration(dur)}
                    className={`px-3 py-1 rounded-full text-xs font-black transition-all ${
                      selectedDuration === dur
                        ? 'bg-amber-500 text-black shadow-lg shadow-amber-500/30 scale-105'
                        : 'bg-white/10 text-white/70 hover:bg-white/20'
                    }`}
                  >
                    {dur}
                  </button>
                ))}
              </div>

              {/* CONTRÔLES : CAMÉRA DIRECTE, DÉCLENCHEUR & BOUTON IMPORTER */}
              <div className="flex items-center justify-around py-1">
                {/* Mode Caméra : Active / Coupe */}
                <button
                  type="button"
                  onClick={() => {
                    if (isCameraActive) {
                      stopCamera();
                    } else {
                      startCamera();
                    }
                  }}
                  className={`w-12 h-12 rounded-2xl flex flex-col items-center justify-center border transition-all ${
                    isCameraActive
                      ? 'bg-red-500/20 border-red-500 text-red-400'
                      : 'bg-white/10 border-white/15 text-white hover:bg-white/20'
                  }`}
                  title={isCameraActive ? 'Couper caméra' : 'Activer caméra'}
                >
                  <Camera className="w-5 h-5" />
                  <span className="text-[8px] font-bold mt-0.5">{isCameraActive ? 'Couper' : 'Caméra'}</span>
                </button>

                {/* Grand Déclencheur / Enregistreur */}
                <button
                  type="button"
                  onClick={() => {
                    if (!isCameraActive) startCamera();
                    setIsRecording(!isRecording);
                  }}
                  className="relative p-1 rounded-full group active:scale-95 transition-transform"
                >
                  <div
                    className={`w-16 h-16 rounded-full flex items-center justify-center transition-all shadow-xl ${
                      isRecording
                        ? 'bg-red-500 ring-4 ring-red-500/40 animate-pulse'
                        : 'bg-amber-500 ring-4 ring-amber-500/30'
                    }`}
                  >
                    {isRecording ? (
                      <div className="w-6 h-6 rounded-md bg-white" />
                    ) : (
                      <div className="w-14 h-14 rounded-full border-2 border-black/40 flex items-center justify-center">
                        <Play className="w-6 h-6 text-black fill-black translate-x-0.5" />
                      </div>
                    )}
                  </div>
                </button>

                {/* BOUTON "IMPORTER" (Galerie téléphone : Vidéo, Photo, Musique) */}
                <label className="w-12 h-12 rounded-2xl flex flex-col items-center justify-center bg-white/10 hover:bg-white/20 border border-white/15 text-white cursor-pointer active:scale-95 transition-all">
                  <Upload className="w-5 h-5 text-amber-400" />
                  <span className="text-[8px] font-bold mt-0.5">Importer</span>
                  <input
                    type="file"
                    accept="video/*,image/*,audio/*"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) {
                        setSelectedFile(file);
                        if (file.type.startsWith('audio')) {
                          setSelectedMusicTitle(file.name);
                        }
                      }
                    }}
                    className="hidden"
                  />
                </label>
              </div>

              {/* OUTILS DE RETOUCHE & MINIATURES */}
              <div className="space-y-2 pt-1 border-t border-white/10">
                {/* Ligne des filtres d'effet */}
                <div className="flex items-center gap-1.5 overflow-x-auto py-1">
                  <span className="text-[11px] font-bold text-white/50 mr-1">Filtre :</span>
                  {['Normal', 'Vibrant', 'Cinéma', 'Rétro', 'Noir & Blanc'].map((f) => (
                    <button
                      key={f}
                      type="button"
                      onClick={() => setSelectedFilter(f)}
                      className={`px-2.5 py-1 rounded-lg text-[10px] font-bold whitespace-nowrap transition-colors ${
                        selectedFilter === f
                          ? 'bg-amber-500 text-black font-black'
                          : 'bg-white/5 text-white/70 hover:bg-white/15'
                      }`}
                    >
                      {f}
                    </button>
                  ))}
                </div>

                {/* Ratios & Couverture */}
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1">
                    <span className="text-[11px] font-bold text-white/50 mr-1">Format :</span>
                    {(['9:16', '1:1', '16:9'] as const).map((r) => (
                      <button
                        key={r}
                        type="button"
                        onClick={() => setSelectedRatio(r)}
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          selectedRatio === r
                            ? 'bg-amber-400/20 text-amber-400 border border-amber-400/50'
                            : 'bg-white/5 text-white/50'
                        }`}
                      >
                        {r}
                      </button>
                    ))}
                  </div>

                  {/* Choix de miniature */}
                  <div className="flex items-center gap-1">
                    <span className="text-[11px] font-bold text-white/50 mr-1">Miniature :</span>
                    {[0, 1, 2].map((idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => setSelectedCoverIndex(idx)}
                        className={`w-6 h-8 rounded border overflow-hidden ${
                          selectedCoverIndex === idx
                            ? 'border-amber-400 ring-2 ring-amber-400/40'
                            : 'border-white/20 opacity-60'
                        }`}
                      >
                        <div className="w-full h-full bg-slate-800 text-[8px] flex items-center justify-center font-bold">
                          {idx + 1}
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Champ Titre & Description */}
              <div>
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="Titre de la vidéo, hashtags (#Afrobeat, #IA)..."
                  className="w-full bg-black/60 border border-white/20 rounded-xl px-3.5 py-2 text-xs text-white placeholder-white/40 outline-none focus:border-amber-500"
                  required
                />
              </div>

              {/* Bouton Publier */}
              <div className="flex gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => {
                    stopCamera();
                    setShowPublishBox(false);
                  }}
                  className="flex-1 py-2.5 rounded-xl border border-white/20 text-white/70 text-xs font-bold hover:bg-white/5"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={isPublishing || !newTitle.trim()}
                  className="flex-1 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-400 disabled:opacity-50 text-black text-xs font-black shadow-lg shadow-amber-500/30 flex items-center justify-center gap-1.5 transition-all"
                >
                  <Rocket className="w-4 h-4" />
                  <span>{isPublishing ? 'Publication...' : 'Publier la création'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ============================================================================== */}
      {/* 11. TIROIR DES NOTIFICATIONS & MESSAGES EN TEMPS RÉEL (Header Message Button)  */}
      {/* ============================================================================== */}
      {showNotificationDrawer && (
        <div className="fixed inset-0 z-[200] flex justify-end">
          <div
            className="absolute inset-0 bg-black/80 backdrop-blur-sm transition-opacity"
            onClick={() => setShowNotificationDrawer(false)}
          />
          <div className="relative w-full max-w-[360px] bg-[#12141F] h-full shadow-2xl border-l border-white/10 p-5 flex flex-col animate-in slide-in-from-right duration-300">
            {/* EN-TÊTE TIROIR NOTIFICATIONS & MESSAGES */}
            <div className="flex items-center justify-between pb-4 border-b border-white/10 mb-4">
              <div className="flex items-center gap-2">
                <MessageSquare className="w-5 h-5 text-amber-500" />
                <h2 className="text-lg font-black text-white tracking-tight">Messages & Alertes</h2>
                {unreadCount > 0 && (
                  <span className="bg-amber-500/20 text-amber-400 text-xs font-black px-2 py-0.5 rounded-full border border-amber-500/30">
                    {unreadCount}
                  </span>
                )}
              </div>
              <button
                onClick={() => setShowNotificationDrawer(false)}
                className="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-white bg-white/5 rounded-full"
              >
                ✕
              </button>
            </div>

            {/* ACTION TOUT MARQUER COMME LU */}
            {unreadCount > 0 && (
              <div className="flex justify-end mb-3">
                <button
                  onClick={() => markAllAsRead()}
                  className="flex items-center gap-1.5 text-xs text-amber-400 hover:text-amber-300 font-bold px-2 py-1 rounded-lg hover:bg-amber-500/10 transition-colors"
                >
                  <CheckCheck className="w-4 h-4" />
                  Tout marquer comme lu
                </button>
              </div>
            )}

            {/* LISTE DES NOTIFICATIONS TEMPS RÉEL */}
            <div className="flex-grow overflow-y-auto space-y-2.5 pr-1">
              {notifications.length === 0 ? (
                <div className="text-center py-16 px-4">
                  <div className="text-4xl mb-3">💬</div>
                  <h3 className="text-sm font-bold text-white mb-1">Aucun message pour l'instant</h3>
                  <p className="text-xs text-gray-400">
                    Vos échanges avec les créateurs et vos notifications en temps réel s'afficheront ici !
                  </p>
                </div>
              ) : (
                notifications.map((notif) => {
                  const isLike = notif.notificationType === 'like';
                  const isComment = notif.notificationType === 'comment';

                  return (
                    <div
                      key={notif.id}
                      onClick={() => {
                        if (!notif.isRead) markAsRead(notif.id);
                        setShowNotificationDrawer(false);
                      }}
                      className={`p-3.5 rounded-xl border transition-all cursor-pointer flex items-start gap-3 ${
                        notif.isRead
                          ? 'bg-white/[0.02] border-white/5 text-gray-400 hover:bg-white/[0.05]'
                          : 'bg-amber-500/10 border-amber-500/30 text-white shadow-lg shadow-amber-500/5 hover:bg-amber-500/15'
                      }`}
                    >
                      <div
                        className={`w-9 h-9 rounded-xl flex items-center justify-center text-base flex-shrink-0 font-bold ${
                          isLike
                            ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                            : isComment
                            ? 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/30'
                            : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                        }`}
                      >
                        {isLike ? '❤️' : isComment ? '💬' : '🔔'}
                      </div>
                      <div className="flex-grow min-w-0">
                        <p className="text-xs font-bold leading-snug">{notif.title}</p>
                        <p className="text-[11px] text-gray-300 mt-0.5 line-clamp-2">{notif.message}</p>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}

      {/* INTÉGRATION LIVEKIT - VIDÉO EN TEMPS RÉEL */}
      {isLiveActive && (
        <VideoRoom
          roomName="PANU Live Room"
          userName={currentUserName || 'Invité'}
          onLeave={() => setIsLiveActive(false)}
        />
      )}
    </div>
  );
};

export default HomePage;
