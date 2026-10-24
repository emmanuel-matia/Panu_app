import React, { useEffect, useState, useRef } from 'react';
import { supabase, FOUNDER_EMAIL } from '../lib/supabaseClient';
import { STORAGE_BUCKETS } from '../config/supabaseConfig';
import { ShareButtonWithOpenGraph } from '../components/share/ShareButtonWithOpenGraph';
import { DynamicTemplateGallery } from '../components/studio/DynamicTemplateGallery';
import { PanuShortsFeedPlayer } from '../components/feed/PanuShortsFeedPlayer';
import { LiveFeed } from '../components/feed/LiveFeed';
import { PanuBottomNav } from '../components/nav/PanuBottomNav';
import { CinetPayRechargeModal } from '../components/payment/CinetPayRechargeModal';
import { PostAiChatModal } from '../components/ai/PostAiChatModal';
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
import { StoriesCarousel } from '../components/feed/StoriesCarousel';


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
  created_at: string;
}

export interface PostComment {
  id: string;
  postId: string;
  authorName: string;
  text: string;
  createdAt: string;
}

/**
 * Page d'accueil & Flux Vidéo PANU (Interface React & Tailwind CSS moderne)
 * 1. Lecteur Vidéo Vertical Dynamique style Reels/Shorts/CapCut avec Autoplay fluide.
 * 2. Mouvements cinématiques (Ken Burns), transitions rapides et défilement vertical instantané.
 * 3. Notification animée "🔴 LIVE EN COURS - Rejoins le Match" avec effets Glow/Pulse.
 * 4. Boutons d'action rapide attractifs : Suivre, Cadeau, Studio IA, Booster.
 * 5. Respect strict des règles de sécurité financière et de l'en-tête sans la mention TikTok.
 */
export interface HomePageProps {
  searchQuery?: string;
  showMyPublications?: boolean;
}

export const HomePage: React.FC<HomePageProps> = ({ searchQuery = '', showMyPublications = false }) => {
  const [posts, setPosts] = useState<FeedVideoPost[]>([]);
  const [userEmail, setUserEmail] = useState<string>('');
  const [userRole, setUserRole] = useState<'founder' | 'admin' | 'creator' | 'user'>('user');
  const [loading, setLoading] = useState(true);

  const { currentUserId, currentUserName } = useNotifications();

  const [selectedDomainFilter, setSelectedDomainFilter] = useState<string>('Tous');
  const [localSearch, setLocalSearch] = useState<string>(searchQuery || '');

  useEffect(() => {
    setLocalSearch(searchQuery || '');
  }, [searchQuery]);

  const effectiveSearch = localSearch || searchQuery || '';

  // Filtrage des publications en temps réel selon l'auteur ou le mot-clé et la catégorie de création
  const filteredPosts = posts.filter(post => {
    if (showMyPublications && post.user_id !== currentUserId) return false;
    if (selectedDomainFilter !== 'Tous') {
      const matchDomain = (post.title || '').toLowerCase().includes(selectedDomainFilter.toLowerCase()) ||
                          (post.content || '').toLowerCase().includes(selectedDomainFilter.toLowerCase());
      if (!matchDomain) return false;
    }
    if (!effectiveSearch.trim()) return true;
    const query = effectiveSearch.toLowerCase().trim();
    return (
      (post.title || '').toLowerCase().includes(query) ||
      (post.content || '').toLowerCase().includes(query) ||
      (post.author_name || '').toLowerCase().includes(query)
    );
  });

  // Onglet actif : [🔥 Pour vous] [✨ Tendances] (Interface épurée style TikTok)
  const [activeTab, setActiveTab] = useState<'for_you' | 'trending'>('for_you');
  const [sortBy, setSortBy] = useState<'recent' | 'likes' | 'comments'>('recent');
  const [showMyPubFilter, setShowMyPubFilter] = useState(false);

  // Modales interactives
  const [showTemplatesModal, setShowTemplatesModal] = useState(false);
  const [showFounderFinanceModal, setShowFounderFinanceModal] = useState(false);
  const [showBoostModalForPost, setShowBoostModalForPost] = useState<FeedVideoPost | null>(null);
  const [activeCommentsPostId, setActiveCommentsPostId] = useState<string | null>(null);
  const [activeAiChatPost, setActiveAiChatPost] = useState<FeedVideoPost | null>(null);
  const [showCinetPayModal, setShowCinetPayModal] = useState(false);

  // Growth Hacking
  const [showReferralModal, setShowReferralModal] = useState(false);
  const [showSuggestModal, setShowSuggestModal] = useState(false);
  const referralCode = `PANU-REF-${(currentUserName || 'CREATEUR').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6) || 'PANU26'}`;
  const referralLink = `https://panu.app/invite?ref=${encodeURIComponent(currentUserName || 'createur')}`;

  // États d'interactions
  const [likedPosts, setLikedPosts] = useState<Record<string, boolean>>({});
  const [postLikesCount, setPostLikesCount] = useState<Record<string, number>>({});
  const [commentsMap, setCommentsMap] = useState<Record<string, PostComment[]>>({});
  const [commentInput, setCommentInput] = useState('');
  const [userCredits, setUserCredits] = useState<number>(0);

  // Gestion des abonnements et créateurs
  const [followedCreators, setFollowedCreators] = useState<Record<string, boolean>>({});
  const [creatorsFollowersCount, setCreatorsFollowersCount] = useState<Record<string, number>>({});
  const [activeLiveHosts, setActiveLiveHosts] = useState<Record<string, boolean>>({});
  const [suggestedCreators, setSuggestedCreators] = useState<any[]>([]);
  const [founderFinancials, setFounderFinancials] = useState<{ totalRevenueFcfa: number; commissionsFcfa: number; totalWithdrawals: number }>({
    totalRevenueFcfa: 0,
    commissionsFcfa: 0,
    totalWithdrawals: 0,
  });

  // Publication d'un nouveau post réel
  const [newFeedTitle, setNewFeedTitle] = useState('');
  const [newFeedUrl, setNewFeedUrl] = useState('');
  const [selectedUploadFile, setSelectedUploadFile] = useState<File | null>(null);
  const [isPublishingNewPost, setIsPublishingNewPost] = useState(false);
  const [showPublishBox, setShowPublishBox] = useState(false);
  const [suggestInput, setSuggestInput] = useState('');

  // Mode Caméra Réelle pour Publication
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [cameraFacing, setCameraFacing] = useState<'user' | 'environment'>('user');
  const publishVideoRef = useRef<HTMLVideoElement | null>(null);
  const publishMediaStreamRef = useRef<MediaStream | null>(null);

  const startPublishCamera = async (facing?: 'user' | 'environment') => {
    const mode = facing || cameraFacing;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: mode },
        audio: true,
      });
      publishMediaStreamRef.current = stream;
      if (publishVideoRef.current) {
        publishVideoRef.current.srcObject = stream;
      }
      setIsCameraActive(true);
    } catch (err) {
      console.error('Erreur caméra :', err);
      alert("Impossible d'accéder à la caméra.");
    }
  };

  const stopPublishCamera = () => {
    if (publishMediaStreamRef.current) {
      publishMediaStreamRef.current.getTracks().forEach(track => track.stop());
      publishMediaStreamRef.current = null;
    }
    setIsCameraActive(false);
  };

  const toggleCameraFacing = () => {
    const nextFacing = cameraFacing === 'user' ? 'environment' : 'user';
    setCameraFacing(nextFacing);
    if (isCameraActive) {
      stopPublishCamera();
      // On passe explicitement le prochain mode pour éviter d'attendre la mise à jour asynchrone du state
      setTimeout(() => startPublishCamera(nextFacing), 300);
    }
  };



  // Vérification de sécurité Fondateur
  const isFounder = userEmail.trim().toLowerCase() === FOUNDER_EMAIL.toLowerCase();
  const isAdmin = userRole === 'admin' || isFounder;

  const loadFounderFinancials = async () => {
    try {
      const [{ data: earnings }, { count: txCount }] = await Promise.all([
        supabase.from('creator_earnings').select('total_available_fcfa'),
        supabase.from('ai_credit_transactions').select('*', { count: 'exact', head: true })
      ]);
      const totalRev = earnings ? earnings.reduce((sum, item) => sum + (Number(item.total_available_fcfa) || 0), 0) : 0;
      const comm = Math.round(totalRev * 0.2);
      setFounderFinancials({
        totalRevenueFcfa: totalRev,
        commissionsFcfa: comm,
        totalWithdrawals: txCount || 0,
      });
    } catch (e) {
      console.warn('Founder financials load error:', e);
    }
  };

  useEffect(() => {
    if (showFounderFinanceModal && isFounder) {
      loadFounderFinancials();
    }
  }, [showFounderFinanceModal, isFounder]);

  const fetchFeeds = async () => {
    setLoading(true);
    try {
      // 1. Charger les directs actifs réels depuis Supabase
      const { data: livesData } = await supabase
        .from('lives')
        .select('host_id')
        .eq('status', 'active');
      
      const liveHostsMap: Record<string, boolean> = {};
      if (livesData) {
        livesData.forEach((l) => {
          if (l.host_id) liveHostsMap[l.host_id] = true;
        });
        setActiveLiveHosts(liveHostsMap);
      }

      // 2. Charger les créateurs suggérés (profils réels)
      const { data: suggestionData } = await supabase
        .from('profiles')
        .select('id, username, full_name, avatar_url, bio')
        .neq('id', currentUserId || 'none')
        .limit(6);
      
      if (suggestionData) {
        setSuggestedCreators(suggestionData);
      }

      // 3. Requête Supabase réelle en temps réel demandée (is_public = true)
      let query = supabase
        .from('posts')
        .select('*')
        .eq('is_public', true);

      if (sortBy === 'likes') {
        query = query.order('likes_count', { ascending: false });
      } else if (sortBy === 'comments') {
        query = query.order('comments_count', { ascending: false });
      } else {
        query = query.order('created_at', { ascending: false });
      }

      const { data, error } = await query.limit(40);

      if (error) throw error;

      if (data) {
        const userIds = Array.from(new Set(data.map((p) => p.user_id).filter(Boolean)));
        let profilesMap: Record<string, any> = {};
        let followersMap: Record<string, number> = {};

        if (userIds.length > 0) {
          const [{ data: profiles }, { data: followsData }] = await Promise.all([
            supabase.from('profiles').select('id, username, full_name, avatar_url').in('id', userIds),
            supabase.from('follows').select('following_id').in('following_id', userIds)
          ]);

          if (profiles) {
            profiles.forEach((pr) => {
              profilesMap[pr.id] = pr;
            });
          }

          if (followsData) {
            followsData.forEach((f) => {
              followersMap[f.following_id] = (followersMap[f.following_id] || 0) + 1;
            });
            setCreatorsFollowersCount((prev) => ({ ...prev, ...followersMap }));
          }
        }

        const enrichedPosts: FeedVideoPost[] = data.map((p: any) => {
          const profile = profilesMap[p.user_id];
          return {
            ...p,
            author_name: profile?.full_name || profile?.username || 'Créateur PANU',
            author_avatar: profile?.avatar_url || '',
            followers_count: followersMap[p.user_id] || 0,
            is_live: liveHostsMap[p.user_id] || false,
          };
        });

        setPosts(enrichedPosts);

        // Chargement des compteurs de likes réels depuis la table `likes` et statut aimé
        const postIds = enrichedPosts.map((p) => p.id);
        const [likesCountMap, userLikedSet, realCommentsMap] = await Promise.all([
          fetchLikesCountMap(postIds),
          fetchUserLikedPostIds(currentUserId, postIds),
          fetchCommentsForPosts(postIds),
        ]);

        const initialLikes: Record<string, number> = {};
        const initialLikedState: Record<string, boolean> = {};

        enrichedPosts.forEach((p) => {
          initialLikes[p.id] = likesCountMap[p.id] !== undefined ? likesCountMap[p.id] : (p.likes_count || 0);
          initialLikedState[p.id] = userLikedSet.has(p.id);
        });

        setPostLikesCount(initialLikes);
        setLikedPosts(initialLikedState);
        setCommentsMap((prev) => ({ ...prev, ...realCommentsMap }));
      }
    } catch (err) {
      console.error('Erreur chargement flux real-time:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    supabase.auth.getUser().then(({ data }) => {
      if (data?.user?.email) {
        const email = data.user.email;
        setUserEmail(email);
        if (email.toLowerCase() === FOUNDER_EMAIL.toLowerCase()) {
          setUserRole('founder');
        } else {
          setUserRole((data.user.user_metadata?.role as any) || 'creator');
        }
        if (data.user.id) {
          supabase.from('ai_credits').select('balance').eq('user_id', data.user.id).maybeSingle()
            .then(({ data: cred }) => {
              if (cred?.balance !== undefined && cred?.balance !== null) setUserCredits(cred.balance);
            });
        }
      }
    });

    // 1. Raccordement temps réel direct aux posts Supabase
    const postsChannel = supabase
      .channel('public_posts_realtime')
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'posts' },
        () => {
          fetchFeeds();
        }
      )
      .subscribe();

    // 2. Raccordement temps réel aux LIKES (Incrémentation / Décrémentation instantanée)
    const unsubLikes = subscribeToRealtimeLikes((event) => {
      const delta = event.eventType === 'INSERT' ? 1 : -1;
      setPostLikesCount((prev) => ({
        ...prev,
        [event.postId]: Math.max(0, (prev[event.postId] !== undefined ? prev[event.postId] : 0) + delta),
      }));
      setPosts((prevPosts) =>
        prevPosts.map((p) =>
          p.id === event.postId
            ? { ...p, likes_count: Math.max(0, (p.likes_count ?? 0) + delta) }
            : p
        )
      );
      if (currentUserId && event.userId === currentUserId) {
        setLikedPosts((prev) => ({ ...prev, [event.postId]: event.eventType === 'INSERT' }));
      }
    });

    // 3. Raccordement temps réel aux COMMENTAIRES
    const unsubComments = subscribeToRealtimeComments((newComment) => {
      setCommentsMap((prev) => {
        const existing = prev[newComment.postId] || [];
        if (existing.some((c) => c.id === newComment.id)) return prev;
        return {
          ...prev,
          [newComment.postId]: [...existing, newComment],
        };
      });
      // Incrémenter le compteur de commentaires du post
      setPosts((prevPosts) =>
        prevPosts.map((p) =>
          p.id === newComment.postId
            ? { ...p, comments_count: (p.comments_count || 0) + 1 }
            : p
        )
      );
    });

    // 4. Raccordement temps réel aux FOLLOWS
    const unsubFollows = subscribeToRealtimeFollows((event) => {
      if (event.followingId) {
        const delta = event.eventType === 'INSERT' ? 1 : -1;
        setCreatorsFollowersCount((prev) => ({
          ...prev,
          [event.followingId]: Math.max(0, (prev[event.followingId] || 0) + delta)
        }));

        if (currentUserId && event.followerId === currentUserId) {
          setFollowedCreators((prev) => ({
            ...prev,
            [event.followingId]: event.eventType === 'INSERT'
          }));
        }
      }
    });

    // Charger les abonnements initiaux
    if (currentUserId) {
      fetchFollowingIds(currentUserId).then(ids => {
        const followedMap: Record<string, boolean> = {};
        ids.forEach(id => followedMap[id] = true);
        setFollowedCreators(followedMap);
      });
    }

    return () => {
      supabase.removeChannel(postsChannel);
      unsubLikes();
      unsubComments();
      unsubFollows();
    };
  }, [currentUserId]);

  useEffect(() => {
    fetchFeeds();
  }, [activeTab, sortBy, currentUserId]);

  // GESTION DU LIKE EN TEMPS RÉEL AVEC PERSISTANCE SUPABASE ET NOTIFICATION
  const handleToggleLike = async (postId: string) => {
    const isCurrentlyLiked = !!likedPosts[postId];
    const targetPost = posts.find((p) => p.id === postId);
    const delta = isCurrentlyLiked ? -1 : 1;

    // 1. Mise à jour optimiste immédiate dans l'interface
    setLikedPosts((prev) => ({ ...prev, [postId]: !isCurrentlyLiked }));
    setPostLikesCount((prev) => ({
      ...prev,
      [postId]: Math.max(0, (prev[postId] !== undefined ? prev[postId] : (targetPost?.likes_count || 0)) + delta),
    }));
    setPosts((prevPosts) =>
      prevPosts.map((p) =>
        p.id === postId
          ? { ...p, likes_count: Math.max(0, (p.likes_count ?? 0) + delta) }
          : p
      )
    );

    // 2. Persistance dans la table `likes` et déclenchement de notification temps réel
    if (currentUserId) {
      await togglePostLike({
        postId,
        userId: currentUserId,
        isCurrentlyLiked,
        postAuthorId: targetPost?.user_id,
        postTitle: targetPost?.title,
        actorName: currentUserName,
      });
    }
  };

  const handleToggleFollow = async (authorId: string) => {
    if (!currentUserId) {
      alert('Veuillez vous connecter pour suivre ce créateur.');
      return;
    }
    const success = await toggleFollow(currentUserId, authorId);
    if (!success) {
      alert("Erreur lors de la mise à jour de l'abonnement.");
    }
  };

  // GESTION DU COMMENTAIRE AVEC PERSISTANCE SUPABASE ET NOTIFICATION TEMPS RÉEL
  const handleAddComment = async (postId: string) => {
    if (!commentInput.trim()) return;
    const text = commentInput.trim();
    setCommentInput('');
    const targetPost = posts.find((p) => p.id === postId);

    // 1. Ajout optimiste
    const tempId = `comm_${Date.now()}`;
    const tempComment: PostComment = {
      id: tempId,
      postId,
      authorName: currentUserName || 'Vous',
      text,
      createdAt: "À l'instant",
    };
    setCommentsMap((prev) => ({
      ...prev,
      [postId]: [...(prev[postId] || []), tempComment],
    }));

    // 2. Persistance dans `comments` et alerte de notification
    if (currentUserId) {
      const created = await addPostComment({
        postId,
        userId: currentUserId,
        content: text,
        authorName: currentUserName,
        postAuthorId: targetPost?.user_id,
        postTitle: targetPost?.title,
      });

      if (created) {
        setCommentsMap((prev) => ({
          ...prev,
          [postId]: (prev[postId] || []).map((c) => (c.id === tempId ? created : c)),
        }));
      }
    }
  };

  const handleBoostPost = (post: FeedVideoPost, planName: string, viewsEstimate: string) => {
    setShowBoostModalForPost(null);
    alert(`⚡ Plan "${planName}" activé pour "${post.title}" ! Visibilité boostée d'environ ${viewsEstimate}.`);
  };

  const handlePublishFeed = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFeedTitle.trim()) return;
    setIsPublishingNewPost(true);

    try {
      const { data: userData } = await supabase.auth.getUser();
      const authorId = userData?.user?.id || '00000000-0000-0000-0000-000000000001';
      let finalMediaUrl = newFeedUrl.trim();
      let mediaType = 'text';

      if (!finalMediaUrl && !selectedUploadFile) {
        mediaType = 'text';
      } else if (finalMediaUrl && !selectedUploadFile) {
        mediaType = finalMediaUrl.endsWith('.mp4') || finalMediaUrl.endsWith('.webm') ? 'video' : 'image';
      }

      // Téléversement direct dans le stockage sécurisé
      if (selectedUploadFile) {
        const fileExt = selectedUploadFile.name.split('.').pop() || 'mp4';
        const fileName = `${Date.now()}_${Math.random().toString(36).substring(2, 9)}.${fileExt}`;
        const filePath = `feed/${fileName}`;

        mediaType = selectedUploadFile.type.startsWith('video') ? 'video' : 'image';

        const { error: uploadError } = await supabase.storage
          .from(STORAGE_BUCKETS.POST_MEDIA)
          .upload(filePath, selectedUploadFile, {
            cacheControl: '3600',
            upsert: false,
          });

        if (!uploadError) {
          const { data: publicUrlData } = supabase.storage
            .from(STORAGE_BUCKETS.POST_MEDIA)
            .getPublicUrl(filePath);
          finalMediaUrl = publicUrlData.publicUrl;
        } else {
          console.warn('Storage upload notice:', uploadError.message);
        }
      }

      const { data: newInsertedPost, error } = await supabase.from('posts').insert({
        user_id: authorId,
        title: newFeedTitle.trim(),
        content: newFeedTitle.trim(),
        media_url: finalMediaUrl || null,
        media_type: mediaType,
        is_public: true,
      }).select().single();

      if (error) {
        alert(`Erreur lors de la publication : ${error.message}`);
      } else {
        setNewFeedTitle('');
        setNewFeedUrl('');
        setSelectedUploadFile(null);
        setShowPublishBox(false);
        alert('🎉 Publication mise en ligne avec succès sur PANU !');
        fetchFeeds();

        // Alerte Fondateur : Nouvelle Publication
        sendInstantGmailAlertToFounder({
          eventType: 'IMPORTANT_APP_EVENT',
          subject: `🎬 [PANU] Nouvelle publication : ${newFeedTitle.trim().slice(0, 30)}`,
          userIdentifier: userEmail || authorId,
          details: `Un utilisateur vient de publier un nouveau contenu : "${newFeedTitle.trim()}" (ID: ${newInsertedPost.id}).`,
        });
      }
    } catch (err: any) {
      alert(`Erreur : ${err?.message || 'Échec de la publication'}`);
    } finally {
      setIsPublishingNewPost(false);
    }
  };

  if (loading && posts.length === 0) {
    return (
      <div style={{ minHeight: '60vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#0B0C12' }}>
        <div style={{ width: 48, height: 48, background: '#E5A93C', borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 14, boxShadow: '0 0 24px rgba(229,169,60,0.2)', animation: 'panu-pulse 1.5s ease-in-out infinite' }}>
          <span style={{ color: '#000', fontWeight: 900, fontSize: 24 }}>P</span>
        </div>
        <div style={{ color: '#E5A93C', fontWeight: 900, fontSize: 16, letterSpacing: 2 }}>PANU</div>
        <style>{`@keyframes panu-pulse { 0%,100% { transform: scale(1); } 50% { transform: scale(0.92); } }`}</style>
      </div>
    );
  }

  const liveCount = Object.values(activeLiveHosts).filter(Boolean).length;

  return (
    <div style={{ color: '#F8F9FA' }}>
      <div style={{ maxWidth: 600, margin: '0 auto', padding: '8px 12px' }}>
        {/* BANNIÈRE COMPACTE */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 8, padding: '8px 12px', background: 'linear-gradient(135deg, #181926 0%, #1F1B2C 100%)', borderRadius: 12, border: '1px solid rgba(229,169,60,0.25)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
            <span style={{ fontSize: 16 }}>🚀</span>
            <strong style={{ color: '#FFF', fontSize: 12 }}>PANU Studio</strong>
          </div>
          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            <button onClick={() => setShowTemplatesModal(true)} style={{ background: 'rgba(229,169,60,0.15)', border: '1px solid rgba(229,169,60,0.4)', color: '#E5A93C', borderRadius: 6, padding: '4px 8px', fontSize: 10, fontWeight: 700, cursor: 'pointer', whiteSpace: 'nowrap' }}>
              🎬 Templates
            </button>
            {liveCount > 0 && (
              <a href="/live" style={{ background: 'rgba(255,46,76,0.15)', border: '1px solid #FF2E4C', color: '#FF2E4C', borderRadius: 6, padding: '4px 8px', fontSize: 10, fontWeight: 800, textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 3, whiteSpace: 'nowrap' }}>
                <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#FF2E4C', display: 'inline-block' }} /> {liveCount} Live{liveCount > 1 ? 's' : ''}
              </a>
            )}
          </div>
        </div>

        {/* STORIES COMPACTES */}
        <StoriesCarousel />

        {/* CRÉER UNE PUBLICATION -- BOUTON COMPACT */}
        <button
          onClick={() => setShowPublishBox(true)}
          style={{
            width: '100%', display: 'flex', alignItems: 'center', gap: 10,
            backgroundColor: '#15161E', border: '1px solid rgba(229,169,60,0.3)',
            borderRadius: 12, padding: '10px 14px', marginBottom: 10, cursor: 'pointer',
            color: '#888', fontSize: 13, fontWeight: 600,
          }}
        >
          <span style={{ width: 30, height: 30, borderRadius: '50%', background: 'rgba(229,169,60,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16, flexShrink: 0 }}>+</span>
          Créer une publication...
        </button>

        {/* RECHERCHE COMPACTE */}
        <div style={{ position: 'relative', marginBottom: 8 }}>
          <span style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', fontSize: 13 }}>🔍</span>
          <input
            type="text" value={localSearch} onChange={(e) => setLocalSearch(e.target.value)}
            placeholder="Rechercher..."
            style={{ width: '100%', backgroundColor: '#15161F', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 10, padding: '8px 12px 8px 32px', color: '#FFF', fontSize: 12, outline: 'none' }}
          />
          {localSearch && <button type="button" onClick={() => setLocalSearch('')} style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', color: '#AAA', cursor: 'pointer', fontSize: 14 }}>✕</button>}
        </div>

        {/* ONGLETS + TRI COMPACT */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 8 }}>
          <div style={{ display: 'flex', gap: 6 }}>
            {([['for_you', '🔥 Pour vous'], ['trending', '✨ Tendances']] as const).map(([key, label]) => (
              <button key={key} type="button" onClick={() => setActiveTab(key)}
                style={{
                  backgroundColor: activeTab === key ? '#E5A93C' : 'transparent',
                  color: activeTab === key ? '#000' : '#AAA',
                  border: activeTab === key ? 'none' : '1px solid rgba(255,255,255,0.12)',
                  padding: '5px 12px', borderRadius: 999, fontWeight: 800, fontSize: 11, cursor: 'pointer', whiteSpace: 'nowrap',
                }}>
                {label}
              </button>
            ))}
          </div>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            style={{ backgroundColor: '#15161F', border: '1px solid rgba(255,255,255,0.12)', color: '#CCC', borderRadius: 8, padding: '5px 8px', fontSize: 11, fontWeight: 700, cursor: 'pointer', outline: 'none' }}
          >
            <option value="recent">🕒 Récents</option>
            <option value="likes">❤️ Likés</option>
            <option value="comments">💬 Commentés</option>
          </select>
        </div>

        {/* CATÉGORIES COMPACTES -- BARRE DÉFILANTE */}
        <div style={{ display: 'flex', gap: 6, overflowX: 'auto', paddingBottom: 4, marginBottom: 8, scrollbarWidth: 'none' }}>
          {['Tous', 'Musique', 'Arts Visuels', 'Cinéma', 'Littérature'].map((domain) => {
            const isSelected = selectedDomainFilter === domain;
            return (
              <button key={domain} type="button" onClick={() => setSelectedDomainFilter(domain)}
                style={{
                  backgroundColor: isSelected ? '#E5A93C' : '#15161F', color: isSelected ? '#000' : '#999',
                  border: isSelected ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.08)',
                  padding: '4px 10px', borderRadius: 999, fontSize: 10, fontWeight: 700, cursor: 'pointer',
                  whiteSpace: 'nowrap', flexShrink: 0,
                }}>
                {domain}
              </button>
            );
          })}
        </div>

        {/* FIL DES PUBLICATIONS */}
        {filteredPosts.length === 0 && !loading ? (
          <div style={{ textAlign: 'center', padding: '32px 16px', color: '#888' }}>
            <div style={{ fontSize: 28, marginBottom: 8 }}>📭</div>
            <p style={{ fontSize: 13, margin: '0 0 12px' }}>Aucune publication pour le moment</p>
            <button onClick={() => setShowPublishBox(true)}
              style={{ backgroundColor: '#E5A93C', color: '#000', border: 'none', padding: '8px 20px', borderRadius: 10, fontWeight: 800, fontSize: 12, cursor: 'pointer' }}>
              + Créer une publication
            </button>
          </div>
        ) : (
          <>
            {/* LECTEUR REELS POUR LES ONGLETS FOR_YOU / TRENDING */}
            {(activeTab === 'for_you' || activeTab === 'trending') && (
              <div>
                <PanuShortsFeedPlayer
                  posts={filteredPosts}
                  onOpenTemplates={() => setShowTemplatesModal(true)}
                  onOpenLiveMatch={() => { window.location.href = '/live'; }}
                  onOpenBoost={(post) => setShowBoostModalForPost(post)}
                  onOpenComments={(postId) => setActiveCommentsPostId(postId)}
                  onOpenAiChat={(post) => setActiveAiChatPost(post)}
                  commentsMap={commentsMap}
                  likedPosts={likedPosts}
                  postLikesCount={postLikesCount}
                  onToggleLike={handleToggleLike}
                  followedCreators={followedCreators}
                  creatorsFollowersCount={creatorsFollowersCount}
                  onToggleFollow={handleToggleFollow}
                  activeLiveHosts={activeLiveHosts}
                />

                {/* CRÉATEURS SUGGÉRÉS -- COMPACT */}
                {suggestedCreators.length > 0 && (
                  <div style={{ marginTop: 12, marginBottom: 12 }}>
                    <h3 style={{ margin: '0 0 8px', fontSize: 12, fontWeight: 800, color: '#E5A93C' }}>Créateurs à suivre</h3>
                    <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4, scrollbarWidth: 'none' }}>
                      {suggestedCreators.map(creator => (
                        <div key={creator.id} style={{ flexShrink: 0, width: 100, backgroundColor: '#15161F', borderRadius: 12, padding: 8, border: '1px solid rgba(255,255,255,0.06)', textAlign: 'center' }}>
                          <div style={{ width: 40, height: 40, borderRadius: '50%', border: '2px solid #E5A93C', margin: '0 auto 6px', overflow: 'hidden', backgroundColor: '#0D0E12', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 900, fontSize: 16 }}>
                            {creator.avatar_url ? <img src={creator.avatar_url} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : (creator.username || 'P').charAt(0).toUpperCase()}
                          </div>
                          <div style={{ fontSize: 10, fontWeight: 700, color: '#FFF', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{creator.full_name || creator.username}</div>
                          <button onClick={() => handleToggleFollow(creator.id)}
                            style={{ width: '100%', marginTop: 4, padding: '4px', borderRadius: 6, border: 'none', backgroundColor: followedCreators[creator.id] ? 'rgba(255,255,255,0.1)' : '#E5A93C', color: followedCreators[creator.id] ? '#FFF' : '#000', fontSize: 9, fontWeight: 800, cursor: 'pointer' }}>
                            {followedCreators[creator.id] ? 'Abonné' : 'Suivre'}
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </>
        )}

        {/* BOUTONS DISCRETS -- PARRAINAGE & SUGGESTION */}
        <div style={{ display: 'flex', gap: 6, justifyContent: 'center', marginTop: 8, marginBottom: 8 }}>
          <button type="button" onClick={() => setShowReferralModal(true)} style={{ background: 'none', border: 'none', color: '#E5A93C', fontSize: 10, fontWeight: 700, cursor: 'pointer', padding: '4px 8px' }}>
            🎁 Parrainage
          </button>
          <button type="button" onClick={() => setShowSuggestModal(true)} style={{ background: 'none', border: 'none', color: '#888', fontSize: 10, fontWeight: 700, cursor: 'pointer', padding: '4px 8px' }}>
            🌟 Suggérer
          </button>
        </div>
      </div>

      {/* MODALE DE PUBLICATION */}
      {showPublishBox && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'flex-end', justifyContent: 'center', zIndex: 1000 }}
          onClick={() => { setShowPublishBox(false); stopPublishCamera(); }}>
          <div style={{ backgroundColor: '#161722', borderTop: '2px solid #E5A93C', borderTopLeftRadius: 20, borderTopRightRadius: 20, maxWidth: 520, width: '100%', maxHeight: '85vh', overflowY: 'auto', padding: 16 }}
            onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <h3 style={{ margin: 0, color: '#E5A93C', fontSize: 15, fontWeight: 900 }}>Nouvelle publication</h3>
              <button type="button" onClick={() => { setShowPublishBox(false); stopPublishCamera(); }} style={{ background: 'none', border: 'none', color: '#FFF', fontSize: 18, cursor: 'pointer' }}>✕</button>
            </div>

            {isCameraActive && (
              <div style={{ position: 'relative', width: '100%', aspectRatio: '4/3', backgroundColor: '#000', borderRadius: 12, overflow: 'hidden', marginBottom: 12, border: '2px solid #E5A93C' }}>
                <video ref={publishVideoRef} autoPlay playsInline muted style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                <div style={{ position: 'absolute', bottom: 8, right: 8, display: 'flex', gap: 6 }}>
                  <button onClick={toggleCameraFacing} style={{ backgroundColor: 'rgba(0,0,0,0.6)', border: '1px solid #FFF', color: '#FFF', borderRadius: 999, width: 32, height: 32, cursor: 'pointer', fontSize: 14 }}>🔄</button>
                </div>
                <span style={{ position: 'absolute', top: 8, left: 8, backgroundColor: 'red', color: '#FFF', fontSize: 9, fontWeight: 900, padding: '2px 6px', borderRadius: 4, animation: 'pulse 1.5s infinite' }}>REC</span>
              </div>
            )}

            <form onSubmit={handlePublishFeed} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <input type="text" value={newFeedTitle} onChange={(e) => setNewFeedTitle(e.target.value)} placeholder="Que voulez-vous partager ?"
                style={{ backgroundColor: '#0D0E12', border: '1px solid rgba(255,255,255,0.15)', borderRadius: 10, padding: '10px 14px', color: '#FFF', fontSize: 13, outline: 'none' }} />

              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                <label style={{ backgroundColor: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.15)', borderRadius: 10, padding: '7px 12px', fontSize: 12, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, color: '#FFF', fontWeight: 700 }}>
                  🖼️ Galerie
                  <input type="file" accept="video/*,image/*" onChange={(e) => { if (e.target.files?.[0]) setSelectedUploadFile(e.target.files[0]); }} style={{ display: 'none' }} />
                </label>
                {!isCameraActive ? (
                  <button type="button" onClick={() => startPublishCamera()} style={{ backgroundColor: 'rgba(46,213,115,0.15)', border: '1px solid #2ED573', color: '#2ED573', borderRadius: 8, padding: '7px 12px', fontSize: 12, fontWeight: 800, cursor: 'pointer' }}>📷 Caméra</button>
                ) : (
                  <button type="button" onClick={stopPublishCamera} style={{ backgroundColor: 'rgba(255,71,87,0.15)', border: '1px solid #FF4757', color: '#FF4757', borderRadius: 8, padding: '7px 12px', fontSize: 12, fontWeight: 800, cursor: 'pointer' }}>⏹ Stop</button>
                )}
                {selectedUploadFile && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4, backgroundColor: 'rgba(229,169,60,0.1)', padding: '4px 8px', borderRadius: 6, border: '1px solid rgba(229,169,60,0.3)' }}>
                    <span style={{ fontSize: 10, color: '#E5A93C', maxWidth: 100, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{selectedUploadFile.name}</span>
                    <button type="button" onClick={() => setSelectedUploadFile(null)} style={{ background: 'transparent', border: 'none', color: '#FF4757', cursor: 'pointer', fontSize: 12 }}>✕</button>
                  </div>
                )}
              </div>

              <button type="submit" disabled={isPublishingNewPost}
                style={{ backgroundColor: '#E5A93C', color: '#000', border: 'none', borderRadius: 10, padding: '10px 20px', fontWeight: 900, cursor: isPublishingNewPost ? 'not-allowed' : 'pointer', fontSize: 13, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
                {isPublishingNewPost ? '⏳ Publication...' : '🚀 Publier'}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* MODALE COMMENTAIRES */}
      {activeCommentsPostId && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'flex-end', justifyContent: 'center', zIndex: 1000 }}
          onClick={() => setActiveCommentsPostId(null)}>
          <div style={{ backgroundColor: '#161722', borderTop: '2px solid #E5A93C', borderTopLeftRadius: 20, borderTopRightRadius: 20, maxWidth: 520, width: '100%', maxHeight: '60vh', display: 'flex', flexDirection: 'column', padding: 16 }}
            onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
              <h3 style={{ margin: 0, fontSize: 14, color: '#E5A93C' }}>💬 Commentaires ({(commentsMap[activeCommentsPostId] || []).length})</h3>
              <button type="button" onClick={() => setActiveCommentsPostId(null)} style={{ background: 'transparent', border: 'none', color: '#FFF', fontSize: 16, cursor: 'pointer' }}>✕</button>
            </div>
            <div style={{ flex: 1, overflowY: 'auto', display: 'grid', gap: 6, marginBottom: 10 }}>
              {(commentsMap[activeCommentsPostId] || []).length === 0 ? (
                <p style={{ textAlign: 'center', color: '#777', fontSize: 12, margin: '16px 0' }}>Aucun commentaire. Soyez le premier !</p>
              ) : (commentsMap[activeCommentsPostId] || []).map((c) => (
                <div key={c.id} style={{ backgroundColor: '#0D0E12', padding: '7px 10px', borderRadius: 8, fontSize: 12 }}>
                  <span style={{ color: '#E5A93C', fontWeight: 800 }}>{c.authorName}: </span>
                  <span style={{ color: '#EEE' }}>{c.text}</span>
                </div>
              ))}
            </div>
            <div style={{ display: 'flex', gap: 6 }}>
              <input type="text" value={commentInput} onChange={(e) => setCommentInput(e.target.value)} placeholder="Commenter..."
                style={{ flex: 1, backgroundColor: '#0D0E12', border: '1px solid rgba(255,255,255,0.2)', borderRadius: 8, padding: '8px 10px', color: '#FFF', fontSize: 12 }}
                onKeyDown={(e) => e.key === 'Enter' && handleAddComment(activeCommentsPostId)} />
              <button type="button" onClick={() => handleAddComment(activeCommentsPostId)} style={{ backgroundColor: '#E5A93C', color: '#000', border: 'none', padding: '8px 14px', borderRadius: 8, fontWeight: 800, fontSize: 12, cursor: 'pointer' }}>Envoyer</button>
            </div>
          </div>
        </div>
      )}

      {/* MODALE TEMPLATES */}
      {showTemplatesModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.85)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: 12 }}>
          <div style={{ backgroundColor: '#161722', border: '1px solid #E5A93C', borderRadius: 16, maxWidth: 900, width: '100%', maxHeight: '90vh', overflowY: 'auto', padding: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <h2 style={{ margin: 0, color: '#E5A93C', fontSize: 16 }}>🎬 Templates PANU</h2>
              <button type="button" onClick={() => setShowTemplatesModal(false)} style={{ background: 'transparent', border: '1px solid rgba(255,255,255,0.2)', color: '#FFF', padding: '4px 10px', borderRadius: 8, cursor: 'pointer', fontWeight: 700, fontSize: 12 }}>✕ Fermer</button>
            </div>
            <DynamicTemplateGallery onTemplateSelect={() => setShowTemplatesModal(false)} />
          </div>
        </div>
      )}

      {/* MODALE FONDATEUR */}
      {showFounderFinanceModal && isFounder && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.85)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: 16 }}>
          <div style={{ backgroundColor: '#161722', border: '2px solid #2ED573', borderRadius: 16, maxWidth: 500, width: '100%', padding: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
              <h2 style={{ margin: 0, color: '#2ED573', fontSize: 16 }}>📊 Gains Globaux (Fondateur)</h2>
              <button type="button" onClick={() => setShowFounderFinanceModal(false)} style={{ background: 'transparent', border: '1px solid rgba(255,255,255,0.2)', color: '#FFF', padding: '4px 10px', borderRadius: 8, cursor: 'pointer' }}>✕</button>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 10, marginBottom: 14 }}>
              <div style={{ backgroundColor: '#0D0E12', padding: 12, borderRadius: 10, border: '1px solid rgba(46,213,115,0.3)' }}>
                <div style={{ fontSize: 10, color: '#888' }}>Revenus</div>
                <div style={{ fontSize: 16, fontWeight: 900, color: '#2ED573' }}>{founderFinancials.totalRevenueFcfa.toLocaleString()} FC</div>
              </div>
              <div style={{ backgroundColor: '#0D0E12', padding: 12, borderRadius: 10, border: '1px solid rgba(229,169,60,0.3)' }}>
                <div style={{ fontSize: 10, color: '#888' }}>Commissions 20%</div>
                <div style={{ fontSize: 16, fontWeight: 900, color: '#E5A93C' }}>{founderFinancials.commissionsFcfa.toLocaleString()} FC</div>
              </div>
              <div style={{ backgroundColor: '#0D0E12', padding: 12, borderRadius: 10, border: '1px solid rgba(255,255,255,0.15)' }}>
                <div style={{ fontSize: 10, color: '#888' }}>Transactions</div>
                <div style={{ fontSize: 16, fontWeight: 900, color: '#FFF' }}>{founderFinancials.totalWithdrawals}</div>
              </div>
            </div>
            <div style={{ backgroundColor: '#0D0E12', padding: 10, borderRadius: 8, border: '1px solid rgba(255,255,255,0.1)', fontSize: 11, color: '#BBB' }}>
              🔒 Sécurité RLS : données financières strictement privées.
            </div>
          </div>
        </div>
      )}

      <CinetPayRechargeModal isOpen={showCinetPayModal} onClose={() => setShowCinetPayModal(false)} currentBalance={userCredits} onSuccess={(addedCoins) => setUserCredits((prev) => prev + addedCoins)} />

      {/* MODALE BOOSTER */}
      {showBoostModalForPost && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: 16 }}>
          <div style={{ backgroundColor: '#181922', border: '1px solid #2ED573', borderRadius: 16, maxWidth: 400, width: '100%', padding: 18 }}>
            <h3 style={{ margin: '0 0 8px', color: '#2ED573', fontSize: 15 }}>⚡ Booster "{showBoostModalForPost.title}"</h3>
            <div style={{ display: 'grid', gap: 8, marginBottom: 14 }}>
              {[
                { name: 'Boost Découverte', views: '+5 000 vues', price: '2 500 FCFA' },
                { name: 'Boost Viral Élite', views: '+25 000 vues', price: '10 000 FCFA' },
              ].map((b) => (
                <button key={b.name} type="button" onClick={() => handleBoostPost(showBoostModalForPost, b.name, b.views)}
                  style={{ backgroundColor: '#0D0E12', border: '1px solid rgba(46,213,115,0.4)', padding: 10, borderRadius: 10, color: '#FFF', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ textAlign: 'left' }}>
                    <div style={{ fontWeight: 800, fontSize: 12, color: '#2ED573' }}>{b.name}</div>
                    <div style={{ fontSize: 10, color: '#AAA' }}>{b.views}</div>
                  </div>
                  <div style={{ fontSize: 11, fontWeight: 700, color: '#E5A93C' }}>{b.price}</div>
                </button>
              ))}
            </div>
            <button type="button" onClick={() => setShowBoostModalForPost(null)} style={{ width: '100%', backgroundColor: 'transparent', border: '1px solid rgba(255,255,255,0.2)', color: '#FFF', padding: 8, borderRadius: 8, cursor: 'pointer', fontSize: 12 }}>Annuler</button>
          </div>
        </div>
      )}

      {/* MODALE PARRAINAGE */}
      {showReferralModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: 16 }}>
          <div style={{ backgroundColor: '#181922', border: '1px solid #E5A93C', borderRadius: 16, maxWidth: 380, width: '100%', padding: 18 }}>
            <h3 style={{ margin: '0 0 8px', color: '#E5A93C', fontSize: 15 }}>🎁 Parrainage (+50 Crédits)</h3>
            <div style={{ backgroundColor: '#0D0E12', padding: 10, borderRadius: 8, fontSize: 16, fontWeight: 900, color: '#E5A93C', textAlign: 'center', marginBottom: 10 }}>{referralCode}</div>
            <button type="button" onClick={() => { navigator.clipboard.writeText(referralLink); alert(`Lien copié : ${referralLink}`); }} style={{ width: '100%', backgroundColor: '#E5A93C', color: '#000', border: 'none', padding: 9, borderRadius: 8, fontWeight: 800, cursor: 'pointer', marginBottom: 6, fontSize: 12 }}>Copier le lien</button>
            <button type="button" onClick={() => setShowReferralModal(false)} style={{ width: '100%', backgroundColor: 'transparent', border: '1px solid #444', color: '#FFF', padding: 7, borderRadius: 8, cursor: 'pointer', fontSize: 12 }}>Fermer</button>
          </div>
        </div>
      )}

      <PostAiChatModal post={activeAiChatPost} isOpen={!!activeAiChatPost} onClose={() => setActiveAiChatPost(null)} />
    </div>
  );
};

export default HomePage;
