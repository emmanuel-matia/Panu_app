import React, { useState, useEffect } from 'react';
import { supabase, FOUNDER_EMAIL } from '../lib/supabaseClient';
import { MediaUploadDialog } from '../components/media/MediaUploadDialog';
import { SelectedMediaFile } from '../services/nativeMediaService';
import { PanuTopNavbar } from '../components/nav/PanuTopNavbar';
import { fetchLikesCountMap, subscribeToRealtimeLikes } from '../services/likeService';
import { syncAndFetchContactSuggestions, ContactSuggestion, shareInviteLink } from '../services/contactSyncService';
import { toggleFollow, fetchFollowingIds, subscribeToRealtimeFollows } from '../services/followService';
import { useNotifications } from '../context/NotificationContext';

// Les 30 Catégories officielles PANU
export const ALL_PANU_CATEGORIES = [
  'Blog personnel',
  'Créateur de contenu',
  'Entrepreneur & Business',
  'Comédie & Humour',
  'Musique & Artiste',
  'Peinture & Arts visuels',
  'Motivation & Développement personnel',
  'Séries & Films',
  'Danse & Chorégraphie',
  'Créateur de Reels & Shorts',
  'Mode & Style de vie',
  'Beauté & Soins personnels',
  'Cuisine & Gastronomie africaine',
  'Technologie & Innovation',
  'Éducation & Tutoriels',
  'Sport & Fitness',
  'Santé & Bien-être',
  'Voyages & Découvertes',
  'Automobile & Engins',
  'Jeux vidéo & Gaming',
  'Actualités & Média',
  'Politique & Débats',
  'Foi, Religion & Spiritualité',
  'Immobilier & Architecture',
  'Finances personnelles & Crypto',
  'Coiffure & Esthétique',
  'Photographie & Vidéaste',
  'Culture & Traditions',
  'Événementiel & Organisation',
  'Agro-business & Agriculture',
];

/**
 * Page de Profil & Compte PANU
 * 1. Sélecteur de Catégories : choix garanti de 1 à 4 catégories sur les 30 officielles.
 * 2. Solde privé et conversion en monnaie locale (CDF / USD).
 * 3. Sécurité RLS et RBAC :
 *    - Utilisateurs : paramètres de compte personnels uniquement, paramètres globaux masqués.
 *    - Administrateurs : modération des contenus uniquement, SANS accès financier.
 *    - Fondateur (emmanuelmatia150@gmail.com) : vision globale des finances et base de données.
 */
export interface ProfilePageProps {
  searchQuery?: string;
}

export const ProfilePage: React.FC<ProfilePageProps> = ({ searchQuery = '' }) => {
  const [userId, setUserId] = useState<string>('user_master');
  const [email, setEmail] = useState<string>(FOUNDER_EMAIL);
  const [fullName, setFullName] = useState<string>('Créateur PANU');
  const [username, setUsername] = useState<string>('createur_panu');
  const [bio, setBio] = useState<string>('Créateur officiel sur la Plateforme Numérique Africaine (PANU).');
  const [accountType, setAccountType] = useState<'creator' | 'business'>('creator');
  const [userRole, setUserRole] = useState<'founder' | 'admin' | 'creator' | 'user'>('user');

  // Liens sociaux externes
  const [tiktokUrl, setTiktokUrl] = useState('');
  const [youtubeUrl, setYoutubeUrl] = useState('');
  const [facebookUrl, setFacebookUrl] = useState('');

  // Sélecteur des 4 catégories maximum
  const [selectedCategories, setSelectedCategories] = useState<string[]>([
    'Créateur de contenu',
    'Créateur de Reels & Shorts',
  ]);
  const [categoryWarning, setCategoryWarning] = useState<string | null>(null);

  // Portefeuille privé de l'utilisateur (Gains et jetons cadeaux)
  const [privateTokensBalance, setPrivateTokensBalance] = useState<number>(0);
  const [privateCurrency, setPrivateCurrency] = useState<'CDF' | 'USD'>('CDF');

  // Stats d'abonnés/abonnements réels
  const [followersCount, setFollowersCount] = useState<number>(0);
  const [followingCount, setFollowingCount] = useState<number>(0);

  // Parrainage
  const referralCode = `PANU-REF-${username.toUpperCase().slice(0, 6)}`;
  const referralLink = `https://panu.app/invite?ref=${encodeURIComponent(username)}`;

  // Avatars et couverture (Zéro mock data - valeurs réelles de l'utilisateur)
  const [avatarUrl, setAvatarUrl] = useState<string>('');
  const [coverUrl, setCoverUrl] = useState<string>('');

  const [dialogState, setDialogState] = useState<{
    isOpen: boolean;
    targetType: 'avatar' | 'cover' | 'logo' | 'video';
    title: string;
    subtitle: string;
  }>({
    isOpen: false,
    targetType: 'avatar',
    title: 'Changer la photo',
    subtitle: '',
  });

  const [statusMsg, setStatusMsg] = useState<string | null>(null);
  const [userPosts, setUserPosts] = useState<any[]>([]);
  const [loadingPosts, setLoadingPosts] = useState(false);
  const [contactSuggestions, setContactSuggestions] = useState<ContactSuggestion[]>([]);
  const [isSyncingContacts, setIsSyncingContacts] = useState(false);
  const [followedCreators, setFollowedCreators] = useState<Record<string, boolean>>({});
  const [pushEnabled, setPushEnabled] = useState(false);

  useEffect(() => {
    if ('Notification' in window) {
      if (Notification.permission === 'granted') {
        setPushEnabled(true);
      }
    }
  }, []);

  const handleEnablePushAlerts = async () => {
    if (!('Notification' in window)) {
      showToast("Les notifications Push ne sont pas supportées par votre navigateur.", "info");
      return;
    }
    try {
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        setPushEnabled(true);
        showToast("✅ Notifications Push activées avec succès !", "success");
        if ('serviceWorker' in navigator) {
          const registration = await navigator.serviceWorker.ready;
          if (registration && registration.showNotification) {
            registration.showNotification("PANU - Alertes activées", {
              body: "Vous recevrez désormais les notifications en direct de vos créateurs et matchs favoris.",
              icon: avatarUrl || "/icons/icon-192x192.png",
              badge: "/icons/icon-192x192.png"
            });
          }
        }
      } else {
        showToast("⚠️ Permission de notification refusée.", "info");
      }
    } catch (e) {
      console.error("Push permission error:", e);
      showToast("Erreur lors de l'activation des alertes push.", "error");
    }
  };

  const { showToast } = useNotifications();

  // Filtrage des publications du profil en temps réel
  const filteredUserPosts = userPosts.filter(post => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return (
      post.title.toLowerCase().includes(query) ||
      (post.content || '').toLowerCase().includes(query)
    );
  });

  const isFounder = email.trim().toLowerCase() === FOUNDER_EMAIL.toLowerCase();
  const isAdmin = userRole === 'admin' || isFounder;

  useEffect(() => {
    const loadProfile = async () => {
      const { data: { session } } = await supabase.auth.getSession();
      if (session?.user) {
        setUserId(session.user.id);
        const userEmail = session.user.email || FOUNDER_EMAIL;
        setEmail(userEmail);

        if (userEmail.toLowerCase() === FOUNDER_EMAIL.toLowerCase()) {
          setUserRole('founder');
        } else {
          setUserRole((session.user.user_metadata?.role as any) || 'creator');
        }

        const { data: profile } = await supabase
          .from('profiles')
          .select('*')
          .eq('id', session.user.id)
          .single();

        if (profile) {
          if (profile.full_name) setFullName(profile.full_name);
          if (profile.username) setUsername(profile.username);
          if (profile.bio) setBio(profile.bio);
          if (profile.avatar_url) setAvatarUrl(profile.avatar_url);
          if (profile.cover_url) setCoverUrl(profile.cover_url);
          if (profile.account_type) setAccountType(profile.account_type);
          if (profile.currency) setPrivateCurrency(profile.currency);
        }

        // Chargement du solde du portefeuille réel
        const { data: wallet } = await supabase
          .from('user_wallets')
          .select('*')
          .eq('user_id', session.user.id)
          .single();

        if (wallet && wallet.tokens_balance !== undefined) {
          setPrivateTokensBalance(wallet.tokens_balance);
          if (wallet.currency) setPrivateCurrency(wallet.currency);
        } else {
          // Secours : consultation des crédits réels de l'utilisateur
          const { data: credits } = await supabase
            .from('ai_credits')
            .select('balance')
            .eq('user_id', session.user.id)
            .single();
          if (credits && credits.balance !== undefined) {
            setPrivateTokensBalance(credits.balance);
          }
        }

        // Chargement des compteurs d'abonnés/abonnements
        fetchStats(session.user.id);

        // Charger les abonnements pour savoir qui on suit déjà parmi les suggestions
        fetchFollowingIds(session.user.id).then(ids => {
          const map: Record<string, boolean> = {};
          ids.forEach(id => map[id] = true);
          setFollowedCreators(map);
        });
      }
    };
    loadProfile();

    const fetchStats = async (uid: string) => {
      // Nombre d'abonnés
      const { count: followers } = await supabase
        .from('follows')
        .select('*', { count: 'exact', head: true })
        .eq('following_id', uid);
      
      setFollowersCount(followers || 0);

      // Nombre d'abonnements
      const { count: following } = await supabase
        .from('follows')
        .select('*', { count: 'exact', head: true })
        .eq('follower_id', uid);
      
      setFollowingCount(following || 0);
    };

    // Raccordement temps réel direct à Supabase pour le profil
    let postsChannel: any;
    let followsChannel: any;
    let unsubLikes: (() => void) | undefined;

    if (userId) {
      postsChannel = supabase
        .channel(`profile_posts_${userId}`)
        .on(
          'postgres_changes',
          { event: '*', schema: 'public', table: 'posts', filter: `user_id=eq.${userId}` },
          () => {
            fetchUserPosts(userId);
          }
        )
        .subscribe();

      // Écoute Realtime sur les abonnements
      followsChannel = supabase
        .channel(`profile_follows_${userId}`)
        .on(
          'postgres_changes',
          { event: '*', schema: 'public', table: 'follows' },
          (payload) => {
            if (payload.new && (payload.new.following_id === userId || payload.new.follower_id === userId)) {
              fetchStats(userId);
            }
            if (payload.old && (payload.old.following_id === userId || payload.old.follower_id === userId)) {
              fetchStats(userId);
            }

            // Mettre à jour l'état local si c'est moi qui suit
            if (payload.new?.follower_id === userId) {
               setFollowedCreators(prev => ({ ...prev, [payload.new.following_id]: true }));
            }
            if (payload.old?.follower_id === userId) {
               setFollowedCreators(prev => ({ ...prev, [payload.old.following_id]: false }));
            }
          }
        )
        .subscribe();

      // Écoute Realtime sur les likes pour mettre à jour les compteurs du profil
      unsubLikes = subscribeToRealtimeLikes((event) => {
        setUserPosts((prev) =>
          prev.map((post) => {
            if (post.id === event.postId) {
              const delta = event.eventType === 'INSERT' ? 1 : -1;
              return {
                ...post,
                likes_count: Math.max(0, (post.likes_count || 0) + delta),
              };
            }
            return post;
          })
        );
      });
    }

    return () => {
      if (postsChannel) supabase.removeChannel(postsChannel);
      if (followsChannel) supabase.removeChannel(followsChannel);
      if (unsubLikes) unsubLikes();
    };
  }, [userId]);

  const fetchUserPosts = async (uid: string) => {
    setLoadingPosts(true);
    try {
      const { data, error } = await supabase
        .from('posts')
        .select('*')
        .eq('user_id', uid)
        .order('created_at', { ascending: false });

      if (!error && data) {
        // Récupération des compteurs réels de likes depuis la table `likes`
        const postIds = data.map((p) => p.id);
        const likesMap = await fetchLikesCountMap(postIds);

        const postsWithRealLikes = data.map((post: any) => ({
          ...post,
          likes_count: likesMap[post.id] !== undefined ? likesMap[post.id] : (post.likes_count || 0),
        }));

        setUserPosts(postsWithRealLikes);
      }
    } catch (err) {
      console.error('Erreur chargement posts profil:', err);
    } finally {
      setLoadingPosts(false);
    }
  };

  useEffect(() => {
    if (userId && userId !== 'user_master') {
      fetchUserPosts(userId);
    }
  }, [userId]);

  // Gestion du sélecteur de catégories (Maximum 4)
  const toggleCategory = (cat: string) => {
    setCategoryWarning(null);
    if (selectedCategories.includes(cat)) {
      if (selectedCategories.length <= 1) {
        setCategoryWarning('Vous devez conserver au moins 1 catégorie pour identifier votre contenu.');
        return;
      }
      setSelectedCategories(selectedCategories.filter((c) => c !== cat));
    } else {
      if (selectedCategories.length >= 4) {
        setCategoryWarning('⚠️ Limite atteinte : Vous ne pouvez sélectionner que 4 catégories au maximum.');
        return;
      }
      setSelectedCategories([...selectedCategories, cat]);
    }
  };

  const handleSaveProfile = async () => {
    setStatusMsg('Sauvegarde du profil en cours...');
    const { error } = await supabase.from('profiles').upsert({
      id: userId,
      full_name: fullName,
      username: username,
      bio: bio,
      account_type: accountType,
      category: selectedCategories.join(', '),
      currency: privateCurrency,
      tiktok_url: tiktokUrl,
      youtube_url: youtubeUrl,
      facebook_url: facebookUrl
    });

    if (error) {
      setStatusMsg(`Erreur : ${error.message}`);
    } else {
      setStatusMsg('✅ Profil mis à jour avec succès !');
      setTimeout(() => setStatusMsg(null), 4000);
    }
  };

  const handleSyncContacts = async () => {
    setIsSyncingContacts(true);
    const suggestions = await syncAndFetchContactSuggestions();
    setContactSuggestions(suggestions);
    setIsSyncingContacts(false);
    if (suggestions.length === 0) {
      showToast('Aucun de vos contacts n’est encore sur PANU. Invitez-les !', 'info');
    } else {
      showToast(`${suggestions.length} ami(s) trouvé(s) !`, 'success');
    }
  };

  const handleInviteFriends = async () => {
    await shareInviteLink(username);
  };

  const handleFollowSuggestion = async (sid: string) => {
    const success = await toggleFollow(userId, sid);
    if (success) {
      // Le state sera mis à jour par le channel realtime
    }
  };

  const openPicker = (target: 'avatar' | 'cover') => {
    setDialogState({
      isOpen: true,
      targetType: target,
      title: target === 'avatar' ? 'Changer la photo de profil' : 'Changer la bannière',
      subtitle: 'Sélectionnez une image depuis votre galerie',
    });
  };

  const handleMediaReady = (media: SelectedMediaFile) => {
    if (dialogState.targetType === 'avatar') {
      setAvatarUrl(media.previewUrl);
    } else if (dialogState.targetType === 'cover') {
      setCoverUrl(media.previewUrl);
    }
    setDialogState((prev) => ({ ...prev, isOpen: false }));
    setStatusMsg('Image sélectionnée. N’oubliez pas de sauvegarder votre profil.');
  };

  // Calcul du solde converti en monnaie locale (CDF / USD)
  // 1 token = 28.50 CDF (ou 0.0100 USD)
  const cdfRate = 28.5;
  const usdRate = 0.01;
  const grossCdf = privateTokensBalance * cdfRate;
  const netCdf = Math.round(grossCdf * 0.8); // après 20% de commission
  const netUsd = (privateTokensBalance * usdRate * 0.8).toFixed(2);

  return (
    <div style={{ color: '#FFF', minHeight: '100vh' }}>
      <div style={{ padding: '16px 20px', maxWidth: 720, margin: '0 auto' }}>
      {/* 1. COUVERTURE & AVATAR */}
      <div style={{ position: 'relative', marginBottom: 60 }}>
        <div
          onClick={() => openPicker('cover')}
          style={{
            height: 180,
            borderRadius: 16,
            backgroundImage: `url(${coverUrl})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            cursor: 'pointer',
            border: '1px solid rgba(229, 169, 60, 0.3)',
            position: 'relative',
          }}
        >
          <span style={{ position: 'absolute', bottom: 8, right: 12, backgroundColor: 'rgba(0,0,0,0.6)', padding: '4px 8px', borderRadius: 6, fontSize: 11, color: '#E5A93C' }}>
            📷 Modifier la couverture
          </span>
        </div>

        {/* Avatar */}
        <div
          onClick={() => openPicker('avatar')}
          style={{
            position: 'absolute',
            bottom: -45,
            left: 20,
            width: 90,
            height: 90,
            borderRadius: '50%',
            backgroundImage: `url(${avatarUrl})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            border: '4px solid #0D0E12',
            boxShadow: '0 4px 12px rgba(0,0,0,0.6)',
            cursor: 'pointer',
          }}
        >
          <span style={{ position: 'absolute', bottom: 0, right: 0, backgroundColor: '#E5A93C', color: '#000', borderRadius: '50%', width: 24, height: 24, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 900 }}>
            ✎
          </span>
        </div>
      </div>

      {statusMsg && (
        <div style={{ backgroundColor: '#181922', border: '1px solid #E5A93C', color: '#E5A93C', padding: 12, borderRadius: 10, marginBottom: 16, fontSize: 13, fontWeight: 600 }}>
          {statusMsg}
        </div>
      )}

      {/* 2. IDENTITÉ DU COMPTE */}
      <div style={{ display: 'grid', gap: 14, marginBottom: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <h1 style={{ margin: 0, fontSize: 22, color: '#FFF' }}>{fullName}</h1>
              {isFounder && (
                <span style={{ backgroundColor: 'rgba(46, 213, 115, 0.2)', border: '1px solid #2ED573', color: '#2ED573', fontSize: 10, padding: '2px 8px', borderRadius: 999, fontWeight: 800 }}>
                  FONDATEUR
                </span>
              )}
            </div>
            <div style={{ color: '#E5A93C', fontSize: 13, fontWeight: 700 }}>@{username}</div>
            <div style={{ color: '#777', fontSize: 12 }}>{email}</div>
          </div>

          <button 
            onClick={handleInviteFriends}
            style={{ backgroundColor: 'rgba(229, 169, 60, 0.1)', border: '1px solid #E5A93C', color: '#E5A93C', padding: '6px 12px', borderRadius: 8, fontSize: 11, fontWeight: 800, cursor: 'pointer' }}
          >
            🔗 Inviter
          </button>
        </div>

        {/* STATS : ABONNÉS & ABONNEMENTS (RÉEL & TEMPS RÉEL) */}
        <div style={{ display: 'flex', gap: 24, padding: '4px 0' }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 20, fontWeight: 900, color: '#FFF' }}>{followersCount}</div>
            <div style={{ fontSize: 11, color: '#888', textTransform: 'uppercase', fontWeight: 700, tracking: 1 }}>Abonnés</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 20, fontWeight: 900, color: '#FFF' }}>{followingCount}</div>
            <div style={{ fontSize: 11, color: '#888', textTransform: 'uppercase', fontWeight: 700, tracking: 1 }}>Abonnements</div>
          </div>
        </div>

        {/* Type de compte : Créateur ou Business */}
        <div style={{ display: 'flex', gap: 10 }}>
          <button
            type="button"
            onClick={() => setAccountType('creator')}
            style={{
              flex: 1,
              backgroundColor: accountType === 'creator' ? '#E5A93C' : '#181922',
              color: accountType === 'creator' ? '#000' : '#AAA',
              border: '1px solid rgba(229, 169, 60, 0.3)',
              padding: '10px 14px',
              borderRadius: 10,
              fontWeight: 800,
              fontSize: 13,
              cursor: 'pointer',
            }}
          >
            🎨 Profil Créateur
          </button>
          <button
            type="button"
            onClick={() => setAccountType('business')}
            style={{
              flex: 1,
              backgroundColor: accountType === 'business' ? '#E5A93C' : '#181922',
              color: accountType === 'business' ? '#000' : '#AAA',
              border: '1px solid rgba(229, 169, 60, 0.3)',
              padding: '10px 14px',
              borderRadius: 10,
              fontWeight: 800,
              fontSize: 13,
              cursor: 'pointer',
            }}
          >
            🏢 Profil Business
          </button>
        </div>
      </div>

      {/* ============================================================================== */}
      {/* 3. PORTEFEUILLE PRIVÉ DE L'UTILISATEUR & CONVERSION MONNAIE LOCALE (CDF / USD)  */}
      {/* ============================================================================== */}
      <div
        style={{
          backgroundColor: '#15161F',
          borderRadius: 14,
          border: '1px solid rgba(229, 169, 60, 0.35)',
          padding: 16,
          marginBottom: 20,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <div>
            <h3 style={{ margin: 0, color: '#E5A93C', fontSize: 16 }}>💰 Mon Portefeuille Privé</h3>
            <span style={{ fontSize: 11, color: '#888' }}>Vos gains de cadeaux et récompenses directes</span>
          </div>
          <div style={{ display: 'flex', gap: 6 }}>
            <button
              type="button"
              onClick={() => setPrivateCurrency('CDF')}
              style={{
                backgroundColor: privateCurrency === 'CDF' ? '#E5A93C' : '#222',
                color: privateCurrency === 'CDF' ? '#000' : '#FFF',
                border: 'none',
                padding: '4px 8px',
                borderRadius: 6,
                fontSize: 11,
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              CDF (FC)
            </button>
            <button
              type="button"
              onClick={() => setPrivateCurrency('USD')}
              style={{
                backgroundColor: privateCurrency === 'USD' ? '#E5A93C' : '#222',
                color: privateCurrency === 'USD' ? '#000' : '#FFF',
                border: 'none',
                padding: '4px 8px',
                borderRadius: 6,
                fontSize: 11,
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              USD ($)
            </button>
          </div>
        </div>

        {/* Détail du solde privé */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12, marginTop: 10 }}>
          <div style={{ backgroundColor: '#0D0E12', padding: 12, borderRadius: 10, border: '1px solid rgba(255,255,255,0.1)' }}>
            <div style={{ fontSize: 11, color: '#888' }}>Solde de Jetons Cadeaux</div>
            <div style={{ fontSize: 22, fontWeight: 900, color: '#E5A93C', marginTop: 4 }}>
              {privateTokensBalance.toLocaleString()} 🪙
            </div>
            <div style={{ fontSize: 10, color: '#AAA', marginTop: 2 }}>Gagnés via lives et flux</div>
          </div>

          <div style={{ backgroundColor: '#0D0E12', padding: 12, borderRadius: 10, border: '1px solid rgba(46, 213, 115, 0.3)' }}>
            <div style={{ fontSize: 11, color: '#888' }}>Valeur Retirable Nette</div>
            <div style={{ fontSize: 22, fontWeight: 900, color: '#2ED573', marginTop: 4 }}>
              {privateCurrency === 'CDF' ? `${netCdf.toLocaleString()} FC` : `$${netUsd} USD`}
            </div>
            <div style={{ fontSize: 10, color: '#2ED573', marginTop: 2 }}>Mobile Money & Virement</div>
          </div>
        </div>

        <p style={{ margin: '12px 0 0', fontSize: 11, color: '#777' }}>
          🔒 <strong>Sécurité RLS :</strong> Ce solde est strictement privé. Les administrateurs et autres utilisateurs n'y ont aucun accès.
        </p>
      </div>

      {/* 4. NOTIFICATIONS PUSH & SERVICE WORKER */}
      <div style={{ backgroundColor: '#15161F', borderRadius: 14, border: '1px solid rgba(229, 169, 60, 0.3)', padding: 16, marginBottom: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
          <div>
            <h3 style={{ margin: '0 0 4px', color: '#E5A93C', fontSize: 15 }}>🔔 Notifications Push & Service Worker</h3>
            <p style={{ margin: 0, fontSize: 11, color: '#AAA' }}>
              {pushEnabled ? '✓ Les alertes push sont actives via le Service Worker.' : 'Recevez des alertes en temps réel pour vos lives et abonnements.'}
            </p>
          </div>
          <button
            type="button"
            onClick={handleEnablePushAlerts}
            style={{
              backgroundColor: pushEnabled ? 'rgba(46, 213, 115, 0.2)' : '#E5A93C',
              border: pushEnabled ? '1px solid #2ED573' : 'none',
              color: pushEnabled ? '#2ED573' : '#000',
              padding: '8px 14px',
              borderRadius: 8,
              fontSize: 12,
              fontWeight: 800,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            {pushEnabled ? '✓ Alertes Actives' : 'Activer les alertes'}
          </button>
        </div>
      </div>

      {/* ============================================================================== */}
      {/* 4. SÉLECTEUR DE CATÉGORIES (CHOIX DE 1 À 4 CATÉGORIES SUR LES 30)              */}
      {/* ============================================================================== */}
      <div
        style={{
          backgroundColor: '#15161F',
          borderRadius: 14,
          border: '1px solid rgba(255, 255, 255, 0.12)',
          padding: 16,
          marginBottom: 20,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <h3 style={{ margin: 0, fontSize: 15, color: '#FFF' }}>
            🏷️ Vos Catégories de Création
          </h3>
          <span
            style={{
              fontSize: 12,
              fontWeight: 800,
              color: selectedCategories.length === 4 ? '#E5A93C' : '#2ED573',
              backgroundColor: 'rgba(255,255,255,0.06)',
              padding: '2px 10px',
              borderRadius: 999,
            }}
          >
            {selectedCategories.length} / 4 sélectionnées
          </span>
        </div>
        <p style={{ margin: '0 0 14px', fontSize: 12, color: '#AAA' }}>
          Sélectionnez jusqu'à <strong>4 catégories</strong> sur les 30 pour orienter l'algorithme des flux Pour vous et Tendances :
        </p>

        {categoryWarning && (
          <div style={{ backgroundColor: 'rgba(255, 71, 87, 0.15)', border: '1px solid #FF4757', color: '#FF6B81', padding: '8px 12px', borderRadius: 8, fontSize: 12, marginBottom: 12 }}>
            {categoryWarning}
          </div>
        )}

        {/* Grille des 30 catégories */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, maxHeight: 260, overflowY: 'auto', padding: '4px 0' }}>
          {ALL_PANU_CATEGORIES.map((cat) => {
            const isSelected = selectedCategories.includes(cat);
            return (
              <button
                key={cat}
                type="button"
                onClick={() => toggleCategory(cat)}
                style={{
                  backgroundColor: isSelected ? 'rgba(229, 169, 60, 0.25)' : '#0D0E12',
                  border: isSelected ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.15)',
                  color: isSelected ? '#E5A93C' : '#DDD',
                  padding: '6px 12px',
                  borderRadius: 999,
                  fontSize: 12,
                  fontWeight: isSelected ? 800 : 500,
                  cursor: 'pointer',
                  transition: 'all 0.15s',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                }}
              >
                <span>{isSelected ? '✓' : '+'}</span>
                <span>{cat}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 5. FORMULAIRE DES PARAMÈTRES DU COMPTE */}
      <div
        style={{
          backgroundColor: '#15161F',
          borderRadius: 14,
          border: '1px solid rgba(255, 255, 255, 0.12)',
          padding: 16,
          display: 'grid',
          gap: 12,
          marginBottom: 20,
        }}
      >
        <h3 style={{ margin: '0 0 4px', fontSize: 15, color: '#FFF' }}>⚙️ Paramètres du Compte</h3>

        <div>
          <label style={{ fontSize: 11, color: '#888' }}>Nom d'affichage :</label>
          <input
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            style={{ width: '100%', backgroundColor: '#0D0E12', border: '1px solid #333', padding: 10, borderRadius: 8, color: '#FFF', marginTop: 4 }}
          />
        </div>

        <div>
          <label style={{ fontSize: 11, color: '#888' }}>Nom d'utilisateur (@) :</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            style={{ width: '100%', backgroundColor: '#0D0E12', border: '1px solid #333', padding: 10, borderRadius: 8, color: '#FFF', marginTop: 4 }}
          />
        </div>

        <div>
          <label style={{ fontSize: 11, color: '#888' }}>Biographie créateur :</label>
          <textarea
            rows={3}
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            style={{ width: '100%', backgroundColor: '#0D0E12', border: '1px solid #333', padding: 10, borderRadius: 8, color: '#FFF', marginTop: 4 }}
          />
        </div>

        {/* Liens Réseaux Sociaux */}
        <div>
          <label style={{ fontSize: 11, color: '#888' }}>Lien TikTok officiel :</label>
          <input
            type="text"
            placeholder="https://tiktok.com/@votre_compte"
            value={tiktokUrl}
            onChange={(e) => setTiktokUrl(e.target.value)}
            style={{ width: '100%', backgroundColor: '#0D0E12', border: '1px solid #333', padding: 8, borderRadius: 8, color: '#FFF', marginTop: 4 }}
          />
        </div>

        <div>
          <label style={{ fontSize: 11, color: '#888' }}>Chaîne YouTube :</label>
          <input
            type="text"
            placeholder="https://youtube.com/@votre_chaine"
            value={youtubeUrl}
            onChange={(e) => setYoutubeUrl(e.target.value)}
            style={{ width: '100%', backgroundColor: '#0D0E12', border: '1px solid #333', padding: 8, borderRadius: 8, color: '#FFF', marginTop: 4 }}
          />
        </div>

        <div>
          <label style={{ fontSize: 11, color: '#888' }}>Page Facebook :</label>
          <input
            type="text"
            placeholder="https://facebook.com/votre_page"
            value={facebookUrl}
            onChange={(e) => setFacebookUrl(e.target.value)}
            style={{ width: '100%', backgroundColor: '#0D0E12', border: '1px solid #333', padding: 8, borderRadius: 8, color: '#FFF', marginTop: 4 }}
          />
        </div>

        <button
          type="button"
          onClick={handleSaveProfile}
          style={{
            backgroundColor: '#E5A93C',
            color: '#000',
            fontWeight: 800,
            padding: 12,
            borderRadius: 10,
            border: 'none',
            cursor: 'pointer',
            marginTop: 6,
            fontSize: 14,
          }}
        >
          Enregistrer les modifications
        </button>
      </div>

      {/* SYNCHRONISATION DES CONTACTS */}
      <div style={{ backgroundColor: '#15161F', borderRadius: 14, border: '1px solid rgba(229, 169, 60, 0.3)', padding: 16, marginBottom: 20 }}>
        <h3 style={{ margin: '0 0 6px', color: '#E5A93C', fontSize: 15 }}>👥 Retrouver vos amis</h3>
        <p style={{ margin: '0 0 12px', fontSize: 11, color: '#AAA', lineHeight: 1.4 }}>
          Synchronisez vos contacts pour découvrir instantanément vos amis déjà inscrits sur PANU. Un hash sécurisé SHA-256 est généré pour chaque numéro afin de préserver votre vie privée.
        </p>
        
        {isSyncingContacts ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, backgroundColor: '#0D0E12', padding: 14, borderRadius: 10, border: '1px solid rgba(229, 169, 60, 0.2)' }}>
            <div style={{ width: 18, height: 18, border: '2px solid #E5A93C', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
            <span style={{ fontSize: 12, fontWeight: 700, color: '#E5A93C' }}>Génération des hashs SHA-256 & analyse des contacts...</span>
          </div>
        ) : contactSuggestions === null ? (
          <button 
            onClick={handleSyncContacts}
            style={{ width: '100%', backgroundColor: '#E5A93C', color: '#000', padding: 12, borderRadius: 10, fontWeight: 900, cursor: 'pointer', border: 'none', fontSize: 13 }}
          >
            🔐 Synchroniser mes contacts en toute sécurité
          </button>
        ) : contactSuggestions.length === 0 ? (
          <div style={{ backgroundColor: '#0D0E12', padding: 16, borderRadius: 10, textAlign: 'center', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ fontSize: 24, marginBottom: 6 }}>🔍</div>
            <div style={{ fontSize: 13, fontWeight: 800, color: '#FFF', marginBottom: 4 }}>Aucun ami trouvé</div>
            <div style={{ fontSize: 11, color: '#888', marginBottom: 12 }}>Aucun de vos contacts ne correspond aux profils inscrits sur PANU.</div>
            <button
              onClick={handleSyncContacts}
              style={{ background: 'none', border: '1px solid #E5A93C', color: '#E5A93C', padding: '6px 12px', borderRadius: 6, fontSize: 11, fontWeight: 700, cursor: 'pointer' }}
            >
              Réessayer
            </button>
          </div>
        ) : (
          <div style={{ display: 'grid', gap: 10 }}>
            <div style={{ fontSize: 11, color: '#2ED573', fontWeight: 700, marginBottom: 2 }}>
              ✨ {contactSuggestions.length} ami(s) trouvé(s) dans vos contacts !
            </div>
            {contactSuggestions.map(s => (
              <div key={s.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#0D0E12', padding: 10, borderRadius: 10, border: '1px solid rgba(255,255,255,0.05)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ width: 34, height: 34, borderRadius: '50%', backgroundColor: '#E5A93C', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 900, fontSize: 12, overflow: 'hidden' }}>
                    {s.avatarUrl ? <img src={s.avatarUrl} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : s.username.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <div style={{ fontSize: 12, fontWeight: 800 }}>{s.fullName || s.username}</div>
                    <div style={{ fontSize: 10, color: '#666' }}>@{s.username}</div>
                  </div>
                </div>
                <button 
                  onClick={() => handleFollowSuggestion(s.id)}
                  style={{ backgroundColor: followedCreators[s.id] ? 'transparent' : '#E5A93C', border: followedCreators[s.id] ? '1px solid #444' : 'none', color: followedCreators[s.id] ? '#888' : '#000', padding: '5px 12px', borderRadius: 6, fontSize: 10, fontWeight: 800, cursor: 'pointer' }}
                >
                  {followedCreators[s.id] ? 'Abonné' : 'Suivre'}
                </button>
              </div>
            ))}
            <button 
              onClick={() => setContactSuggestions(null as any)}
              style={{ background: 'none', border: 'none', color: '#666', fontSize: 10, cursor: 'pointer', marginTop: 4 }}
            >
              Fermer les suggestions
            </button>
          </div>
        )}
      </div>

      {/* 6. PARRAINAGE (+50 CRÉDITS) */}
      <div style={{ backgroundColor: '#15161F', borderRadius: 14, border: '1px solid rgba(229, 169, 60, 0.3)', padding: 16, marginBottom: 20 }}>
        <h3 style={{ margin: '0 0 6px', color: '#E5A93C', fontSize: 15 }}>🎁 Parrainage & Partage de lien</h3>
        <p style={{ margin: '0 0 10px', fontSize: 12, color: '#AAA' }}>Partagez votre code pour obtenir +50 crédits lors de chaque nouvelle inscription :</p>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#0D0E12', padding: 10, borderRadius: 8 }}>
          <span style={{ fontWeight: 800, color: '#E5A93C' }}>{referralCode}</span>
          <button
            type="button"
            onClick={() => {
              navigator.clipboard.writeText(referralLink);
              alert(`Lien copié : ${referralLink}`);
            }}
            style={{ backgroundColor: '#E5A93C', color: '#000', border: 'none', padding: '6px 12px', borderRadius: 6, fontWeight: 800, cursor: 'pointer', fontSize: 12 }}
          >
            Copier
          </button>
        </div>
      </div>

      {/* 7. MES PUBLICATIONS (FLUX RÉEL) */}
      <div
        style={{
          backgroundColor: '#15161F',
          borderRadius: 14,
          border: '1px solid rgba(255, 255, 255, 0.12)',
          padding: 16,
          marginBottom: 100, // Espace pour la bottom nav
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <h3 style={{ margin: 0, fontSize: 16, color: '#E5A93C' }}>🎥 Mes Publications</h3>
          {userPosts.length > 0 && (
            <button
              type="button"
              onClick={() => {
                const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(userPosts, null, 2));
                const downloadAnchor = document.createElement('a');
                downloadAnchor.setAttribute("href", dataStr);
                downloadAnchor.setAttribute("download", `panu_creator_posts_${username || 'export'}.json`);
                document.body.appendChild(downloadAnchor);
                downloadAnchor.click();
                downloadAnchor.remove();
                showToast("Export JSON de vos publications réussi !", "success");
              }}
              style={{
                backgroundColor: 'rgba(229, 169, 60, 0.15)',
                border: '1px solid #E5A93C',
                color: '#E5A93C',
                padding: '6px 12px',
                borderRadius: 8,
                fontSize: 11,
                fontWeight: 800,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
              title="Exporter toutes vos publications personnelles dans un fichier JSON local (Portabilité des données)"
            >
              <span>📥</span>
              <span>Exporter JSON</span>
            </button>
          )}
        </div>

        {loadingPosts ? (
          <div style={{ textAlign: 'center', color: '#888', padding: '20px 0' }}>Chargement de vos créations...</div>
        ) : filteredUserPosts.length === 0 ? (
          <div style={{ textAlign: 'center', color: '#666', padding: '40px 0', border: '1px dashed #333', borderRadius: 10 }}>
            <div style={{ fontSize: 30, marginBottom: 10 }}>🎬</div>
            <p style={{ margin: 0 }}>Vous n'avez pas encore publié de contenu.</p>
            <p style={{ fontSize: 11, marginTop: 4 }}>Vos vidéos et photos apparaîtront ici.</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 4 }}>
            {filteredUserPosts.map((post) => (
              <div
                key={post.id}
                style={{
                  aspectRatio: '9/16',
                  backgroundColor: '#000',
                  borderRadius: 6,
                  overflow: 'hidden',
                  position: 'relative',
                  border: '1px solid rgba(255,255,255,0.05)',
                }}
              >
                {post.media_url ? (
                  post.media_type === 'video' ? (
                    <video
                      src={post.media_url}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  ) : (
                    <img
                      src={post.media_url}
                      alt={post.title}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  )
                ) : (
                  <div style={{ padding: 10, fontSize: 10, color: '#AAA' }}>{post.title}</div>
                )}
                <div style={{ position: 'absolute', bottom: 4, left: 4, display: 'flex', alignItems: 'center', gap: 2, fontSize: 9, color: '#FFF', textShadow: '0 1px 2px rgba(0,0,0,0.8)' }}>
                  <span>❤️</span> {post.likes_count || 0}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modale de sélection d'image */}
      <MediaUploadDialog
        isOpen={dialogState.isOpen}
        title={dialogState.title}
        subtitle={dialogState.subtitle}
        targetType={dialogState.targetType}
        onClose={() => setDialogState((prev) => ({ ...prev, isOpen: false }))}
        onMediaReady={handleMediaReady}
      />
      </div>
    </div>
  );
};

export default ProfilePage;
