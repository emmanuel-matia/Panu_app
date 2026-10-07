import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Search, Menu, Wallet, LogOut, Home, Sparkles, PlaySquare, User, Bell, CheckCheck, Coins } from 'lucide-react';
import { supabase } from '../lib/supabaseClient';
import { useNotifications } from '../context/NotificationContext';
import { CreditRechargeModal } from './payment/CreditRechargeModal';

/**
 * COMPOSANT HEADER PANU (NETTOYÉ ET ÉPURÉ)
 * - Logo PANU à gauche (sans coupure)
 * - Recherche interactive, Cloche de Notifications temps réel (🔔), et Menu Hamburger (☰)
 */
export interface HeaderProps {
  userBalance?: number;
  onBalanceUpdate?: (newBalance: number) => void;
  searchQuery?: string;
  onSearchChange?: (query: string) => void;
}

export const Header: React.FC<HeaderProps> = ({ 
  userBalance = 250, 
  onBalanceUpdate,
  searchQuery = '',
  onSearchChange
}) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [showMenu, setShowMenu] = useState(false);
  const [showNotificationDrawer, setShowNotificationDrawer] = useState(false);
  const [userEmail, setUserEmail] = useState<string | null>(null);
  const [balance, setBalance] = useState<number>(userBalance);
  const [isSearchExpanded, setIsSearchExpanded] = useState(false);
  const [showRechargeModal, setShowRechargeModal] = useState(false);

  const { notifications, unreadCount, markAsRead, markAllAsRead } = useNotifications();

  useEffect(() => {
    supabase.auth.getUser().then(({ data }) => {
      const user = data?.user;
      setUserEmail(user?.email || null);
      if (user) {
        // Charger le solde initial
        supabase
          .from('ai_credits')
          .select('balance')
          .eq('user_id', user.id)
          .single()
          .then(({ data: creditData }) => {
            if (creditData) {
              setBalance(creditData.balance);
              onBalanceUpdate?.(creditData.balance);
            }
          });

        // Souscription Realtime aux changements de solde
        const channel = supabase
          .channel(`credits_${user.id}`)
          .on(
            'postgres_changes',
            { event: 'UPDATE', schema: 'public', table: 'ai_credits', filter: `user_id=eq.${user.id}` },
            (payload) => {
              const newBalance = payload.new.balance;
              setBalance(newBalance);
              onBalanceUpdate?.(newBalance);
            }
          )
          .subscribe();

        return () => {
          supabase.removeChannel(channel);
        };
      }
    });
  }, [onBalanceUpdate]);

  const handleNav = (path: string) => {
    setShowMenu(false);
    setShowNotificationDrawer(false);
    navigate(path);
  };

  const formatNotifTime = (dateStr: string) => {
    try {
      const diffSec = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
      if (diffSec < 60) return 'À l’instant';
      const diffMin = Math.floor(diffSec / 60);
      if (diffMin < 60) return `Il y a ${diffMin} min`;
      const diffHours = Math.floor(diffMin / 60);
      if (diffHours < 24) return `Il y a ${diffHours} h`;
      return new Date(dateStr).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
    } catch {
      return 'Récemment';
    }
  };

  return (
    <>
      <header className="w-full bg-[#10121A] border-b border-white/5 px-4 py-3 flex items-center justify-between sticky top-0 z-[100] backdrop-blur-md">
        {/* CÔTÉ GAUCHE : LOGO PANU */}
        {!isSearchExpanded && (
          <div 
            className="flex items-center gap-3 cursor-pointer select-none animate-in fade-in duration-300" 
            onClick={() => navigate('/')}
          >
            <div className="w-9 h-9 bg-amber-500 rounded-xl flex items-center justify-center shadow-lg shadow-amber-500/20 flex-shrink-0">
              <span className="text-black font-black text-xl">P</span>
            </div>
            <span className="text-2xl font-black tracking-tighter text-white uppercase whitespace-nowrap hidden sm:block">
              PANU
            </span>
          </div>
        )}

        {/* BARRE DE RECHERCHE INTERACTIVE */}
        <div className={`flex-grow mx-4 transition-all duration-300 flex items-center ${isSearchExpanded ? 'max-w-full' : 'max-w-[200px]'}`}>
          <div className="relative w-full group">
            <Search className={`absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 transition-colors ${searchQuery ? 'text-amber-500' : 'text-gray-500'}`} />
            <input
              type="text"
              placeholder="Rechercher sur PANU..."
              value={searchQuery}
              onChange={(e) => onSearchChange?.(e.target.value)}
              onFocus={() => setIsSearchExpanded(true)}
              onBlur={() => {
                if (!searchQuery) setIsSearchExpanded(false);
              }}
              className="w-full bg-white/5 border border-white/10 rounded-full py-2 pl-10 pr-4 text-sm text-white placeholder:text-gray-500 focus:outline-none focus:border-amber-500/50 focus:bg-white/10 transition-all"
            />
            {searchQuery && (
              <button 
                onClick={() => onSearchChange?.('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-white"
              >
                ✕
              </button>
            )}
          </div>
        </div>

        {/* CÔTÉ DROIT : CRÉDITS, NOTIFICATIONS & MENU */}
        <div className="flex items-center gap-2">
          {!isSearchExpanded && (
            <>
              {/* AFFICHAGE DES CRÉDITS IA */}
              <div 
                className={`hidden md:flex items-center gap-1.5 px-3 py-1.5 rounded-full cursor-pointer transition-all ${
                  balance < 50 
                    ? 'bg-red-500/20 border border-red-500/40 hover:bg-red-500/30' 
                    : 'bg-amber-500/10 border border-amber-500/20 hover:bg-amber-500/20'
                }`}
                onClick={() => setShowRechargeModal(true)}
                title={balance < 50 ? "Solde faible ! Recharger" : "Recharger mes crédits IA"}
              >
                <Coins className={`w-3.5 h-3.5 ${balance < 50 ? 'text-red-400' : 'text-amber-500'}`} />
                <div className="flex flex-col -space-y-0.5">
                  <span className="text-xs font-black text-white">{balance}</span>
                  {balance < 50 && (
                    <span className="text-[8px] font-black text-red-400 animate-pulse uppercase leading-none">Faible</span>
                  )}
                </div>
                {balance < 50 && (
                  <span className="ml-1 text-[10px] font-bold text-white bg-red-500 px-1.5 py-0.5 rounded-md">
                    ACHETER
                  </span>
                )}
              </div>

              {/* CLOCHE DE NOTIFICATIONS AVEC BADGE EN TEMPS RÉEL */}
              <button 
                onClick={() => setShowNotificationDrawer(true)}
                className="relative p-2.5 text-gray-400 hover:text-white hover:bg-white/5 rounded-full transition-all"
                aria-label="Notifications"
                title="Notifications"
              >
                <Bell className="w-5 h-5" />
                {unreadCount > 0 && (
                  <span className="absolute top-1.5 right-1.5 min-w-[18px] h-[18px] px-1 bg-red-500 text-white text-[10px] font-black rounded-full flex items-center justify-center border-2 border-[#10121A] animate-pulse shadow-lg shadow-red-500/50">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>

              {/* MENU HAMBURGER */}
              <button 
                onClick={() => setShowMenu(true)}
                className="p-2.5 text-gray-400 hover:text-white hover:bg-white/5 rounded-full transition-all"
                aria-label="Menu"
              >
                <Menu className="w-6 h-6" />
              </button>
            </>
          )}
          {isSearchExpanded && (
             <button 
              onClick={() => setIsSearchExpanded(false)}
              className="p-2 text-gray-400 hover:text-white text-xs font-bold uppercase tracking-wider"
            >
              Fermer
            </button>
          )}
        </div>
      </header>

      {/* TIROIR DES NOTIFICATIONS EN TEMPS RÉEL */}
      {showNotificationDrawer && (
        <div className="fixed inset-0 z-[200] flex justify-end">
          <div 
            className="absolute inset-0 bg-black/80 backdrop-blur-sm transition-opacity" 
            onClick={() => setShowNotificationDrawer(false)} 
          />
          <div className="relative w-full max-w-[360px] bg-[#12141F] h-full shadow-2xl border-l border-white/10 p-5 flex flex-col animate-in slide-in-from-right duration-300">
            {/* EN-TÊTE TIROIR NOTIFICATIONS */}
            <div className="flex items-center justify-between pb-4 border-b border-white/10 mb-4">
              <div className="flex items-center gap-2">
                <span className="text-xl">🔔</span>
                <h2 className="text-lg font-black text-white tracking-tight">Notifications</h2>
                {unreadCount > 0 && (
                  <span className="bg-amber-500/20 text-amber-400 text-xs font-black px-2 py-0.5 rounded-full border border-amber-500/30">
                    {unreadCount}
                  </span>
                )}
              </div>
              <button 
                onClick={() => setShowNotificationDrawer(false)}
                className="w-9 h-9 flex items-center justify-center text-gray-400 hover:text-white bg-white/5 rounded-full"
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
            <div className="flex-grow overflow-y-auto space-y-2 pr-1">
              {notifications.length === 0 ? (
                <div className="text-center py-16 px-4">
                  <div className="text-4xl mb-3">📭</div>
                  <h3 className="text-sm font-bold text-white mb-1">Aucune notification pour l'instant</h3>
                  <p className="text-xs text-gray-400">
                    Vous recevrez une alerte en temps réel dès qu'un utilisateur aime ou commente vos publications !
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
                        if (notif.postId) {
                          setShowNotificationDrawer(false);
                          navigate('/');
                        }
                      }}
                      className={`p-3.5 rounded-xl border transition-all cursor-pointer flex items-start gap-3 ${
                        notif.isRead
                          ? 'bg-white/[0.02] border-white/5 text-gray-400 hover:bg-white/[0.05]'
                          : 'bg-amber-500/10 border-amber-500/30 text-white shadow-lg shadow-amber-500/5 hover:bg-amber-500/15'
                      }`}
                    >
                      <div className={`w-9 h-9 rounded-xl flex items-center justify-center text-base flex-shrink-0 font-bold ${
                        isLike ? 'bg-red-500/20 text-red-400 border border-red-500/30' : isComment ? 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/30' : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                      }`}>
                        {isLike ? '❤️' : isComment ? '💬' : '🔔'}
                      </div>
                      <div className="flex-grow min-w-0">
                        <div className="flex items-center justify-between gap-1">
                          <span className={`text-xs font-bold truncate ${notif.isRead ? 'text-gray-300' : 'text-amber-400'}`}>
                            {notif.title}
                          </span>
                          <span className="text-[10px] text-gray-500 flex-shrink-0">
                            {formatNotifTime(notif.createdAt)}
                          </span>
                        </div>
                        <p className="text-xs mt-0.5 line-clamp-2 leading-relaxed text-gray-300">
                          {notif.message}
                        </p>
                      </div>
                      {!notif.isRead && (
                        <div className="w-2 h-2 rounded-full bg-amber-400 flex-shrink-0 mt-1.5 animate-pulse" />
                      )}
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}

      {/* MENU HAMBURGER (DRAWER) */}
      {showMenu && (
        <div className="fixed inset-0 z-[200] flex justify-end">
          <div 
            className="absolute inset-0 bg-black/80 backdrop-blur-sm transition-opacity" 
            onClick={() => setShowMenu(false)} 
          />
          <div className="relative w-[300px] bg-[#12141F] h-full shadow-2xl border-l border-white/10 p-6 flex flex-col animate-in slide-in-from-right duration-300">
            <div className="flex items-center justify-between mb-8">
              <h2 className="text-xl font-black text-white tracking-tight">MENU</h2>
              <button 
                onClick={() => setShowMenu(false)}
                className="w-10 h-10 flex items-center justify-center text-gray-400 hover:text-white bg-white/5 rounded-full"
              >
                ✕
              </button>
            </div>

            {/* INFOS UTILISATEUR & SOLDE */}
            <div className="bg-amber-500/10 border border-amber-500/20 rounded-2xl p-4 mb-6">
              <div className="text-[10px] font-bold text-amber-500 uppercase tracking-widest mb-1">Mon Compte</div>
              <div className="text-white font-bold text-sm truncate mb-3">{userEmail || 'Utilisateur PANU'}</div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-white font-black text-lg">
                  <Wallet className="w-5 h-5 text-amber-500" />
                  {balance} 🪙
                </div>
                <button 
                  onClick={() => {
                    setShowMenu(false);
                    setShowRechargeModal(true);
                  }}
                  className="bg-amber-500 hover:bg-amber-400 text-black text-[10px] font-black px-3 py-1.5 rounded-lg transition-colors"
                >
                  RECHARGER
                </button>
              </div>
            </div>

            {/* NAVIGATION RAPIDE */}
            <nav className="flex flex-col gap-2">
              {[
                { label: 'Accueil & Reels', path: '/', icon: <Home className="w-5 h-5" /> },
                { label: 'Studio Créatif IA', path: '/studio', icon: <Sparkles className="w-5 h-5" /> },
                { label: 'Directs & Matchs', path: '/live', icon: <PlaySquare className="w-5 h-5" /> },
                { label: 'Mon Profil', path: '/profile', icon: <User className="w-5 h-5" /> },
              ].map((item) => (
                <button
                  key={item.path}
                  onClick={() => handleNav(item.path)}
                  className={`flex items-center gap-4 p-4 rounded-xl font-bold text-sm transition-all ${
                    location.pathname === item.path 
                    ? 'bg-amber-500 text-black shadow-lg shadow-amber-500/20' 
                    : 'text-gray-300 hover:bg-white/5 hover:text-white'
                  }`}
                >
                  {item.icon}
                  {item.label}
                </button>
              ))}
            </nav>

            {/* DÉCONNEXION */}
            <div className="mt-auto pt-6 border-t border-white/5">
              <button 
                className="w-full flex items-center gap-4 p-4 text-red-400 hover:bg-red-400/10 rounded-xl transition-all font-bold text-sm"
                onClick={() => {
                  supabase.auth.signOut();
                  handleNav('/login');
                }}
              >
                <LogOut className="w-5 h-5" />
                Déconnexion
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODALE DE RECHARGE DE CRÉDITS */}
      <CreditRechargeModal 
        isOpen={showRechargeModal}
        onClose={() => setShowRechargeModal(false)}
        currentBalance={balance}
      />
    </>
  );
};

export default Header;
