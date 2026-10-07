import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { supabase, FOUNDER_EMAIL } from '../../lib/supabaseClient';
import { CinetPayRechargeModal } from '../payment/CinetPayRechargeModal';
import { Search, Menu, Bell } from 'lucide-react';

interface PanuTopNavbarProps {
  onOpenTemplates?: () => void;
  onOpenFounderSettings?: () => void;
  userBalance?: number;
  onBalanceUpdate?: (newBalance: number) => void;
}

interface NotificationItem {
  id: string;
  type: 'live' | 'gift' | 'template' | 'follower';
  title: string;
  desc: string;
  time: string;
  read: boolean;
  link?: string;
}

/**
 * RESTRUCTURATION DE LA BARRE D'EN-TÊTE PANU (HEADER LAYOUT)
 * 1. Alignement à gauche : Logo PANU lisible, clair et aligné sans chevauchement.
 * 2. Alignement à droite (maximum 3 icônes) :
 *    - Recherche (🔍)
 *    - Notifications (🔔 avec badge d'alertes)
 *    - Menu Hamburger (☰)
 * 3. Déplacement des icônes secondaires :
 *    - Paramètres (⚙️) et QR Code (🔲) placés à l'intérieur du Menu Hamburger.
 *    - Panneau de Cadeaux (🎁) accessible directement dans le lecteur vidéo / direct.
 */
export const PanuTopNavbar: React.FC<PanuTopNavbarProps> = ({
  onOpenTemplates,
  onOpenFounderSettings,
  userBalance = 250,
  onBalanceUpdate,
}) => {
  const navigate = useNavigate();
  const location = useLocation();

  const [userEmail, setUserEmail] = useState<string>('');
  const [userName, setUserName] = useState<string>('Créateur PANU');
  const [balance, setBalance] = useState<number>(userBalance);

  // Modales déclenchées par les 3 icônes principales
  const [showSearchModal, setShowSearchModal] = useState<boolean>(false);
  const [showNotificationsDrawer, setShowNotificationsDrawer] = useState<boolean>(false);
  const [showHamburgerDrawer, setShowHamburgerDrawer] = useState<boolean>(false);

  // Modale CinetPay
  const [showCinetPayModal, setShowCinetPayModal] = useState<boolean>(false);

  // Modale QR Code
  const [showQrModal, setShowQrModal] = useState<boolean>(false);

  // État de Recherche
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [searchCategory, setSearchCategory] = useState<'all' | 'creators' | 'lives' | 'templates'>('all');

  // Notifications
  const [notifications, setNotifications] = useState<NotificationItem[]>([
    {
      id: 'n1',
      type: 'live',
      title: '⚽ Grand Match d’Afrique en Direct !',
      desc: 'Le coup d’envoi est lancé en streaming haute vitesse WebRTC.',
      time: 'Il y a 3 min',
      read: false,
      link: '/live',
    },
    {
      id: 'n2',
      type: 'template',
      title: '🎬 Nouveau Template CapCut / PixVerse 8K',
      desc: 'Créez votre clip viral avec effets cinématiques en 1 clic.',
      time: 'Il y a 25 min',
      read: false,
      link: '/studio',
    },
    {
      id: 'n3',
      type: 'gift',
      title: '🎁 Bonus de Bienvenue CinetPay',
      desc: 'Rechargez vos pièces et profitez de jusqu’à +1500 pièces bonus.',
      time: 'Il y a 2h',
      read: false,
    },
  ]);

  const [activeLivesSearch, setActiveLivesSearch] = useState<any[]>([]);

  useEffect(() => {
    supabase.auth.getUser().then(({ data }) => {
      if (data?.user?.email) {
        setUserEmail(data.user.email);
        setUserName(data.user.email.split('@')[0]);
      }
    });

    // Chargement dynamique des directs réels depuis Supabase (Zéro mock data)
    supabase
      .from('lives')
      .select('id, title, host_name, category')
      .eq('status', 'active')
      .then(({ data }) => {
        if (data) {
          const cleanLives = data.filter((l) => {
            const h = (l.host_name || '').toLowerCase();
            return !h.includes('emmanuel') && !h.includes('matia');
          });
          setActiveLivesSearch(cleanLives);
        }
      });
  }, []);

  useEffect(() => {
    setBalance(userBalance);
  }, [userBalance]);

  const isFounder = userEmail.trim().toLowerCase() === FOUNDER_EMAIL.toLowerCase();
  const unreadCount = notifications.filter((n) => !n.read).length;

  const handleNav = (path: string) => {
    setShowHamburgerDrawer(false);
    if (location.pathname !== path) {
      navigate(path);
    }
  };

  const handleMarkAllNotificationsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const handleCoinRechargeSuccess = (addedCoins: number) => {
    const updated = balance + addedCoins;
    setBalance(updated);
    onBalanceUpdate?.(updated);
  };

  // Liste de suggestions de recherche reliée aux données réelles (avec MASKING STRICT du fondateur)
  const allSearchItems = [
    { type: 'creators', name: 'Studio Créatif PANU', desc: 'Studio Officiel • Production IA', link: '/studio' },
    ...activeLivesSearch.map((l) => ({
      type: 'lives',
      name: `🔴 ${l.title}`,
      desc: `Direct par ${l.host_name} • Catégorie : ${l.category}`,
      link: '/live',
    })),
    { type: 'templates', name: 'Clip CapCut & PixVerse : Speed-Ramp 4K', desc: 'Template Vidéo Virale 9:16', link: '/studio' },
    { type: 'templates', name: 'Survol Drone 8K Métropole Moderne', desc: 'PixVerse FPV & Veo 3.1', link: '/studio' },
    { type: 'templates', name: 'Affiche de Concert & Festival Afro-Urbain', desc: 'Poster Canva Pro', link: '/studio' },
    { type: 'templates', name: 'Carte Cadeau & Voucher VIP Doré', desc: 'Pass Digital & Réductions', link: '/studio' },
  ];

  // FILTRE DE RECHERCHE AVEC MASKING DU FONDATEUR
  const filteredSearchResults = allSearchItems.filter((item) => {
    const text = (item.name + ' ' + item.desc).toLowerCase();
    // Masking du fondateur : 100% invisible
    if (text.includes('emmanuel') || text.includes('matia') || text.includes(FOUNDER_EMAIL.toLowerCase())) {
      return false;
    }
    const matchesQuery = searchQuery.trim() === '' || text.includes(searchQuery.toLowerCase().trim());
    const matchesCat = searchCategory === 'all' || item.type === searchCategory;
    return matchesQuery && matchesCat;
  });

  return (
    <>
      <header className="w-full bg-white border-b border-gray-100 px-4 py-3 flex items-center justify-between sticky top-0 z-50">
        {/* CÔTÉ GAUCHE : LOGO PANU SEUL ET NET */}
        <div className="flex items-center cursor-pointer" onClick={() => handleNav('/')}>
          <span className="text-2xl font-black tracking-wider text-black font-sans uppercase">
            PANU
          </span>
        </div>

        {/* CÔTÉ DROIT : RECHERCHE, NOTIFICATIONS ET MENU */}
        <div className="flex items-center gap-3 text-gray-700">
          <button 
            onClick={() => setShowSearchModal(true)}
            className="p-1.5 hover:bg-gray-100 rounded-full transition-colors" 
            aria-label="Recherche"
          >
            <Search className="w-6 h-6"/>
          </button>
          <button 
            onClick={() => setShowNotificationsDrawer(true)}
            className="relative p-1.5 hover:bg-gray-100 rounded-full transition-colors" 
            aria-label="Notifications"
          >
            <Bell className="w-6 h-6"/>
            {unreadCount > 0 && (
              <span className="absolute top-0 right-0 w-4 h-4 bg-red-500 text-white text-[10px] font-black rounded-full flex items-center justify-center">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>
          <button 
            onClick={() => setShowHamburgerDrawer(true)}
            className="p-1.5 hover:bg-gray-100 rounded-full transition-colors" 
            aria-label="Menu"
          >
            <Menu className="w-6 h-6"/>
          </button>
        </div>
      </header>

      {/* ========================================================================= */}
      {/* 3. MENU HAMBURGER (SLIDE-OVER DRAWER LATÉRAL)                             */}
      {/* ========================================================================= */}
      {showHamburgerDrawer && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(5, 6, 12, 0.8)',
            backdropFilter: 'blur(8px)',
            zIndex: 9995,
            display: 'flex',
            justifyContent: 'flex-end',
          }}
          onClick={() => setShowHamburgerDrawer(false)}
        >
          <div
            style={{
              backgroundColor: '#12141F',
              width: '100%',
              maxWidth: 340,
              height: '100%',
              boxSizing: 'border-box',
              padding: '24px 20px',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              boxShadow: '-10px 0 40px rgba(0, 0, 0, 0.8)',
              borderLeft: '1px solid rgba(229, 169, 60, 0.3)',
              overflowY: 'auto',
              color: '#FFF',
              animation: 'drawerSlide 0.25s ease-out forwards',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <style>{`
              @keyframes drawerSlide {
                from { transform: translateX(100%); }
                to { transform: translateX(0); }
              }
            `}</style>

            {/* HAUT DU MENU HAMBURGER */}
            <div>
              {/* Entête avec profil et fermeture */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
                  paddingBottom: 16,
                  marginBottom: 16,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: '50%',
                      backgroundColor: '#E5A93C',
                      color: '#000',
                      fontWeight: 900,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 18,
                    }}
                  >
                    {userName.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <div style={{ fontWeight: 800, fontSize: 15, color: '#FFF' }}>
                      {userName}
                    </div>
                    <div style={{ fontSize: 11, color: '#8E92A4' }}>
                      {userEmail || 'Utilisateur PANU'}
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setShowHamburgerDrawer(false)}
                  style={{
                    backgroundColor: 'transparent',
                    border: 'none',
                    color: '#8E92A4',
                    fontSize: 22,
                    cursor: 'pointer',
                  }}
                >
                  ✕
                </button>
              </div>

              {/* Solde de Pièces CinetPay */}
              <div
                style={{
                  backgroundColor: 'rgba(229, 169, 60, 0.12)',
                  border: '1px solid rgba(229, 169, 60, 0.3)',
                  borderRadius: 14,
                  padding: '12px 14px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  marginBottom: 20,
                }}
              >
                <div>
                  <div style={{ fontSize: 11, color: '#DDD' }}>Solde de Pièces</div>
                  <div style={{ fontSize: 18, fontWeight: 900, color: '#FFD700', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <span>🪙</span>
                    <span>{balance.toLocaleString()} Pièces</span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setShowHamburgerDrawer(false);
                    setShowCinetPayModal(true);
                  }}
                  style={{
                    backgroundColor: '#E5A93C',
                    color: '#000',
                    border: 'none',
                    borderRadius: 8,
                    padding: '8px 12px',
                    fontSize: 12,
                    fontWeight: 900,
                    cursor: 'pointer',
                    boxShadow: '0 2px 8px rgba(229, 169, 60, 0.4)',
                  }}
                >
                  + Recharger
                </button>
              </div>

              {/* LIENS DE NAVIGATION PRINCIPAUX */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {[
                  { label: 'Accueil & Reels', icon: '🏠', path: '/' },
                  { label: 'Directs & Matchs de Foot', icon: '🔴', path: '/live', badge: 'LIVE' },
                  { label: 'Templates IA & Montage', icon: '🎬', action: () => { setShowHamburgerDrawer(false); if (onOpenTemplates) onOpenTemplates(); else handleNav('/studio'); } },
                  { label: 'Studio Créatif IA', icon: '✨', path: '/studio' },
                  { label: 'Mon Profil', icon: '👤', path: '/profile' },
                  { label: 'Séries & Documentaires', icon: '📺', path: '/vod' },
                ].map((item, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      if (item.action) {
                        item.action();
                      } else if (item.path) {
                        handleNav(item.path);
                      }
                    }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 14px',
                      borderRadius: 10,
                      backgroundColor: location.pathname === item.path ? 'rgba(229, 169, 60, 0.18)' : 'rgba(255, 255, 255, 0.03)',
                      border: location.pathname === item.path ? '1px solid #E5A93C' : '1px solid transparent',
                      color: location.pathname === item.path ? '#E5A93C' : '#DDD',
                      fontSize: 14,
                      fontWeight: 700,
                      cursor: 'pointer',
                      textAlign: 'left',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <span style={{ fontSize: 18 }}>{item.icon}</span>
                      <span>{item.label}</span>
                    </div>
                    {item.badge && (
                      <span
                        style={{
                          backgroundColor: '#FF2E4C',
                          color: '#FFF',
                          fontSize: 9,
                          fontWeight: 900,
                          padding: '2px 6px',
                          borderRadius: 999,
                        }}
                      >
                        {item.badge}
                      </span>
                    )}
                  </button>
                ))}
              </div>

              {/* SECTION SECONDAIRE DÉPLACÉE DU HEADER (QR CODE, INVITATION, RECHARGE) */}
              <div
                style={{
                  borderTop: '1px solid rgba(255, 255, 255, 0.08)',
                  marginTop: 18,
                  paddingTop: 16,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                <div style={{ fontSize: 11, fontWeight: 700, color: '#8E92A4', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 4 }}>
                  Options Rapides
                </div>

                {/* BOUTON RECHARGER PIÈCES CINETPAY */}
                <button
                  type="button"
                  onClick={() => {
                    setShowHamburgerDrawer(false);
                    setShowCinetPayModal(true);
                  }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    padding: '10px 12px',
                    borderRadius: 10,
                    backgroundColor: 'rgba(255, 255, 255, 0.03)',
                    border: '1px solid rgba(255, 255, 255, 0.06)',
                    color: '#FFD700',
                    fontSize: 13,
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  <span style={{ fontSize: 16 }}>🪙</span>
                  <span>Acheter des Pièces (CinetPay)</span>
                </button>

                {/* BOUTON MON QR CODE */}
                <button
                  type="button"
                  onClick={() => {
                    setShowHamburgerDrawer(false);
                    setShowQrModal(true);
                  }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    padding: '10px 12px',
                    borderRadius: 10,
                    backgroundColor: 'rgba(255, 255, 255, 0.03)',
                    border: '1px solid rgba(255, 255, 255, 0.06)',
                    color: '#DDD',
                    fontSize: 13,
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  <span style={{ fontSize: 16 }}>🔲</span>
                  <span>Mon QR Code Partageable</span>
                </button>

                {/* BOUTON INVITER DES AMIS */}
                <button
                  type="button"
                  onClick={() => {
                    setShowHamburgerDrawer(false);
                    if (navigator.share) {
                      navigator.share({
                        title: 'Rejoins PANU',
                        text: 'Regarde les matchs en direct et crée des vidéos virales avec l’IA sur PANU !',
                        url: window.location.origin,
                      }).catch(() => {});
                    } else {
                      navigator.clipboard.writeText(window.location.origin);
                      alert('Lien PANU copié dans le presse-papier !');
                    }
                  }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    padding: '10px 12px',
                    borderRadius: 10,
                    backgroundColor: 'rgba(255, 255, 255, 0.03)',
                    border: '1px solid rgba(255, 255, 255, 0.06)',
                    color: '#DDD',
                    fontSize: 13,
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  <span style={{ fontSize: 16 }}>👤+</span>
                  <span>Inviter des Amis (+50 Pièces)</span>
                </button>
              </div>
            </div>

            {/* BAS DU MENU : PARAMÈTRES & SÉCURITÉ */}
            <div style={{ borderTop: '1px solid rgba(255, 255, 255, 0.08)', paddingTop: 14 }}>
              {isFounder ? (
                <button
                  type="button"
                  onClick={() => {
                    setShowHamburgerDrawer(false);
                    onOpenFounderSettings?.();
                  }}
                  style={{
                    width: '100%',
                    backgroundColor: 'rgba(46, 213, 115, 0.2)',
                    border: '1px solid #2ED573',
                    color: '#2ED573',
                    borderRadius: 10,
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
                  <span>⚙️</span>
                  <span>Paramètres & Gains Globaux</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => handleNav('/profile')}
                  style={{
                    width: '100%',
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    color: '#AAA',
                    borderRadius: 10,
                    padding: '10px',
                    fontSize: 12,
                    fontWeight: 700,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 6,
                  }}
                >
                  <span>⚙️</span>
                  <span>Paramètres du Compte</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 4. MODALE DE RECHERCHE (🔍) AVEC MASKING DU FONDATEUR                      */}
      {/* ========================================================================= */}
      {showSearchModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(5, 6, 12, 0.85)',
            backdropFilter: 'blur(8px)',
            zIndex: 9996,
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'center',
            padding: '40px 16px 20px',
          }}
          onClick={() => setShowSearchModal(false)}
        >
          <div
            style={{
              backgroundColor: '#12141F',
              border: '1px solid rgba(229, 169, 60, 0.4)',
              borderRadius: 20,
              width: '100%',
              maxWidth: 560,
              padding: 20,
              color: '#FFF',
              boxShadow: '0 20px 50px rgba(0, 0, 0, 0.8)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Champ de recherche */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
              <span style={{ fontSize: 20 }}>🔍</span>
              <input
                type="text"
                autoFocus
                placeholder="Rechercher des matchs, créateurs, templates..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{
                  flex: 1,
                  backgroundColor: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(255, 255, 255, 0.12)',
                  borderRadius: 10,
                  padding: '10px 14px',
                  color: '#FFF',
                  fontSize: 14,
                  outline: 'none',
                }}
              />
              <button
                type="button"
                onClick={() => setShowSearchModal(false)}
                style={{
                  backgroundColor: 'transparent',
                  border: 'none',
                  color: '#8E92A4',
                  fontSize: 20,
                  cursor: 'pointer',
                }}
              >
                ✕
              </button>
            </div>

            {/* Filtres de recherche */}
            <div style={{ display: 'flex', gap: 8, marginBottom: 16, overflowX: 'auto', paddingBottom: 4 }}>
              {[
                { key: 'all', label: 'Tout' },
                { key: 'lives', label: '🔴 Matchs en direct' },
                { key: 'templates', label: '🎬 Templates' },
                { key: 'creators', label: '👤 Créateurs' },
              ].map((cat) => (
                <button
                  key={cat.key}
                  type="button"
                  onClick={() => setSearchCategory(cat.key as any)}
                  style={{
                    padding: '6px 12px',
                    borderRadius: 999,
                    fontSize: 12,
                    fontWeight: 700,
                    cursor: 'pointer',
                    border: searchCategory === cat.key ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.08)',
                    backgroundColor: searchCategory === cat.key ? 'rgba(229,169,60,0.2)' : 'rgba(255,255,255,0.03)',
                    color: searchCategory === cat.key ? '#E5A93C' : '#AAA',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {cat.label}
                </button>
              ))}
            </div>

            {/* Mots-clés tendances */}
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 16 }}>
              {['#Football', '#CapCut', '#Afrobeat', '#Pixverse', '#Kinshasa'].map((tag) => (
                <span
                  key={tag}
                  onClick={() => setSearchQuery(tag.replace('#', ''))}
                  style={{
                    fontSize: 11,
                    color: '#E5A93C',
                    backgroundColor: 'rgba(229, 169, 60, 0.1)',
                    padding: '3px 8px',
                    borderRadius: 6,
                    cursor: 'pointer',
                  }}
                >
                  {tag}
                </span>
              ))}
            </div>

            {/* Résultats de recherche */}
            <div style={{ maxHeight: 280, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 8 }}>
              {filteredSearchResults.length > 0 ? (
                filteredSearchResults.map((res, i) => (
                  <div
                    key={i}
                    onClick={() => {
                      setShowSearchModal(false);
                      handleNav(res.link);
                    }}
                    style={{
                      padding: '10px 12px',
                      borderRadius: 10,
                      backgroundColor: 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid rgba(255, 255, 255, 0.06)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 800, fontSize: 13, color: '#FFF' }}>{res.name}</div>
                      <div style={{ fontSize: 11, color: '#8E92A4' }}>{res.desc}</div>
                    </div>
                    <span style={{ color: '#E5A93C', fontSize: 12, fontWeight: 700 }}>Ouvrir ➔</span>
                  </div>
                ))
              ) : (
                <div style={{ textAlign: 'center', color: '#8E92A4', padding: '20px 0', fontSize: 13 }}>
                  Aucun résultat pour cette recherche.
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 5. CENTRE DE NOTIFICATIONS (🔔)                                           */}
      {/* ========================================================================= */}
      {showNotificationsDrawer && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(5, 6, 12, 0.8)',
            backdropFilter: 'blur(8px)',
            zIndex: 9996,
            display: 'flex',
            justifyContent: 'flex-end',
          }}
          onClick={() => setShowNotificationsDrawer(false)}
        >
          <div
            style={{
              backgroundColor: '#12141F',
              width: '100%',
              maxWidth: 360,
              height: '100%',
              boxSizing: 'border-box',
              padding: '24px 20px',
              display: 'flex',
              flexDirection: 'column',
              boxShadow: '-10px 0 40px rgba(0, 0, 0, 0.8)',
              borderLeft: '1px solid rgba(229, 169, 60, 0.3)',
              color: '#FFF',
              animation: 'drawerSlide 0.25s ease-out forwards',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
                paddingBottom: 14,
                marginBottom: 16,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 18 }}>🔔</span>
                <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900 }}>Notifications</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowNotificationsDrawer(false)}
                style={{
                  backgroundColor: 'transparent',
                  border: 'none',
                  color: '#8E92A4',
                  fontSize: 22,
                  cursor: 'pointer',
                }}
              >
                ✕
              </button>
            </div>

            {/* Action marquer tout comme lu */}
            {unreadCount > 0 && (
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
                <button
                  type="button"
                  onClick={handleMarkAllNotificationsRead}
                  style={{
                    backgroundColor: 'transparent',
                    border: 'none',
                    color: '#E5A93C',
                    fontSize: 12,
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  ✓ Tout marquer comme lu
                </button>
              </div>
            )}

            {/* Liste des alertes */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, overflowY: 'auto' }}>
              {notifications.map((item) => (
                <div
                  key={item.id}
                  onClick={() => {
                    if (item.link) {
                      setShowNotificationsDrawer(false);
                      handleNav(item.link);
                    }
                  }}
                  style={{
                    backgroundColor: item.read ? 'rgba(255, 255, 255, 0.02)' : 'rgba(229, 169, 60, 0.1)',
                    border: item.read ? '1px solid rgba(255, 255, 255, 0.06)' : '1px solid rgba(229, 169, 60, 0.35)',
                    borderRadius: 12,
                    padding: 12,
                    cursor: item.link ? 'pointer' : 'default',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <div style={{ fontWeight: 800, fontSize: 13, color: '#FFF' }}>
                      {item.title}
                    </div>
                    <span style={{ fontSize: 10, color: '#8E92A4' }}>{item.time}</span>
                  </div>
                  <div style={{ fontSize: 12, color: '#DDD', lineHeight: 1.4 }}>
                    {item.desc}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 6. MODALE QR CODE PARTAGEABLE                                             */}
      {/* ========================================================================= */}
      {showQrModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(5, 6, 12, 0.85)',
            backdropFilter: 'blur(8px)',
            zIndex: 9998,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 16,
          }}
          onClick={() => setShowQrModal(false)}
        >
          <div
            style={{
              backgroundColor: '#12141F',
              borderRadius: 20,
              border: '1px solid rgba(229, 169, 60, 0.4)',
              padding: 24,
              maxWidth: 360,
              width: '100%',
              textAlign: 'center',
              color: '#FFF',
              boxShadow: '0 20px 60px rgba(0,0,0,0.8)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ margin: '0 0 6px', fontSize: 18, fontWeight: 900 }}>
              Mon QR Code PANU
            </h3>
            <p style={{ margin: '0 0 18px', fontSize: 12, color: '#8E92A4' }}>
              Faites scanner ce code pour partager instantanément votre profil et vos créations.
            </p>

            <div
              style={{
                backgroundColor: '#FFF',
                borderRadius: 16,
                padding: 18,
                display: 'inline-block',
                marginBottom: 16,
                boxShadow: '0 4px 20px rgba(229, 169, 60, 0.3)',
              }}
            >
              {/* QR Code vectoriel stylisé */}
              <svg width="180" height="180" viewBox="0 0 100 100" fill="#000">
                <rect x="5" y="5" width="25" height="25" fill="#000" rx="4" />
                <rect x="9" y="9" width="17" height="17" fill="#FFF" rx="2" />
                <rect x="13" y="13" width="9" height="9" fill="#000" />

                <rect x="70" y="5" width="25" height="25" fill="#000" rx="4" />
                <rect x="74" y="9" width="17" height="17" fill="#FFF" rx="2" />
                <rect x="78" y="13" width="9" height="9" fill="#000" />

                <rect x="5" y="70" width="25" height="25" fill="#000" rx="4" />
                <rect x="9" y="74" width="17" height="17" fill="#FFF" rx="2" />
                <rect x="13" y="78" width="9" height="9" fill="#000" />

                {/* Motif matriciel */}
                <rect x="36" y="8" width="8" height="8" fill="#E5A93C" />
                <rect x="50" y="12" width="10" height="6" fill="#000" />
                <rect x="42" y="24" width="6" height="12" fill="#000" />
                <rect x="12" y="42" width="12" height="6" fill="#000" />
                <rect x="28" y="44" width="8" height="8" fill="#E5A93C" />
                <rect x="44" y="44" width="12" height="12" fill="#000" />
                <rect x="62" y="40" width="8" height="8" fill="#000" />
                <rect x="76" y="46" width="14" height="6" fill="#000" />
                <rect x="38" y="66" width="10" height="10" fill="#000" />
                <rect x="56" y="64" width="8" height="14" fill="#E5A93C" />
                <rect x="72" y="72" width="16" height="8" fill="#000" />
                <rect x="42" y="82" width="8" height="8" fill="#000" />
              </svg>
            </div>

            <div style={{ fontSize: 13, fontWeight: 800, color: '#E5A93C', marginBottom: 18 }}>
              panu.app/@{userName}
            </div>

            <button
              type="button"
              onClick={() => setShowQrModal(false)}
              style={{
                width: '100%',
                backgroundColor: '#E5A93C',
                color: '#000',
                border: 'none',
                borderRadius: 12,
                padding: '12px',
                fontSize: 14,
                fontWeight: 900,
                cursor: 'pointer',
              }}
            >
              Fermer
            </button>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 7. MODALE RECHARGEMENT CINETPAY                                           */}
      {/* ========================================================================= */}
      <CinetPayRechargeModal
        isOpen={showCinetPayModal}
        onClose={() => setShowCinetPayModal(false)}
        currentBalance={balance}
        onSuccess={handleCoinRechargeSuccess}
      />
    </>
  );
};

export const Header = PanuTopNavbar;
export default PanuTopNavbar;
