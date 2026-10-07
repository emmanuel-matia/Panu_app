import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { supabase } from '../lib/supabaseClient';
import {
  AppNotification,
  fetchUserNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  subscribeToUserNotifications,
} from '../services/notificationService';

interface NotificationContextType {
  notifications: AppNotification[];
  unreadCount: number;
  activeToast: AppNotification | null;
  genericToast: { message: string; type: 'success' | 'error' | 'info' } | null;
  currentUserId: string;
  currentUserName: string;
  dismissToast: () => void;
  showToast: (message: string, type?: 'success' | 'error' | 'info') => void;
  markAsRead: (id: string) => Promise<void>;
  markAllAsRead: () => Promise<void>;
  refreshNotifications: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [activeToast, setActiveToast] = useState<AppNotification | null>(null);
  const [genericToast, setGenericToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);
  const [currentUserId, setCurrentUserId] = useState<string>('');
  const [currentUserName, setCurrentUserName] = useState<string>('Créateur PANU');

  // Récupérer l'identifiant de l'utilisateur (authentifié ou invité persistant)
  useEffect(() => {
    const resolveUser = async () => {
      const { data: { session } } = await supabase.auth.getSession();
      if (session?.user?.id) {
        setCurrentUserId(session.user.id);
        const name = session.user.user_metadata?.full_name || session.user.email?.split('@')[0] || 'Créateur';
        setCurrentUserName(name);
      } else {
        let guestId = localStorage.getItem('panu_guest_uuid');
        if (!guestId) {
          guestId = '11111111-1111-4111-8111-' + Math.random().toString(16).substring(2, 14).padEnd(12, '0');
          localStorage.setItem('panu_guest_uuid', guestId);
        }
        setCurrentUserId(guestId);
      }
    };

    resolveUser();

    const { data: listener } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session?.user?.id) {
        setCurrentUserId(session.user.id);
        const name = session.user.user_metadata?.full_name || session.user.email?.split('@')[0] || 'Créateur';
        setCurrentUserName(name);
      }
    });

    return () => {
      listener.subscription.unsubscribe();
    };
  }, []);

  // Charger les notifications existantes
  const refreshNotifications = useCallback(async () => {
    if (!currentUserId) return;
    const list = await fetchUserNotifications(currentUserId);
    setNotifications(list);
  }, [currentUserId]);

  useEffect(() => {
    if (currentUserId) {
      refreshNotifications();
    }
  }, [currentUserId, refreshNotifications]);

  // Souscription Realtime aux alertes instantanées
  useEffect(() => {
    if (!currentUserId) return;

    const unsubscribe = subscribeToUserNotifications(currentUserId, (newNotif) => {
      setNotifications((prev) => {
        const isDuplicate = prev.some(
          (n) =>
            n.id === newNotif.id ||
            (n.postId &&
              n.postId === newNotif.postId &&
              n.actorId === newNotif.actorId &&
              n.notificationType === newNotif.notificationType &&
              Math.abs(new Date(n.createdAt).getTime() - new Date(newNotif.createdAt).getTime()) < 5000)
        );
        if (isDuplicate) return prev;

        // 2. Déclencher le toast visuel en temps réel pour nouvelle alerte
        setActiveToast(newNotif);

        // Vibration haptique sur mobile si disponible
        if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
          try {
            navigator.vibrate([100, 50, 100]);
          } catch {}
        }

        return [newNotif, ...prev];
      });
    });

    return () => {
      unsubscribe();
    };
  }, [currentUserId]);

  // Auto-fermeture des toasts après 4 secondes
  useEffect(() => {
    if (activeToast || genericToast) {
      const timer = setTimeout(() => {
        dismissToast();
      }, 4000);
      return () => clearTimeout(timer);
    }
  }, [activeToast, genericToast, dismissToast]);

  const showToast = useCallback((message: string, type: 'success' | 'error' | 'info' = 'success') => {
    setGenericToast({ message, type });
  }, []);

  const dismissToast = useCallback(() => {
    setActiveToast(null);
    setGenericToast(null);
  }, []);

  const markAsRead = useCallback(async (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
    );
    await markNotificationAsRead(id);
  }, []);

  const markAllAsRead = useCallback(async () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    if (currentUserId) {
      await markAllNotificationsAsRead(currentUserId);
    }
  }, [currentUserId]);

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        activeToast,
        genericToast,
        currentUserId,
        currentUserName,
        dismissToast,
        showToast,
        markAsRead,
        markAllAsRead,
        refreshNotifications,
      }}
    >
      {children}
      {/* Toast Flottant Realtime au sommet de l'application */}
      {(activeToast || genericToast) && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[9999] w-[92%] max-w-md animate-in slide-in-from-top-4 duration-300">
          <div
            onClick={dismissToast}
            className={`flex items-center gap-3 p-4 rounded-2xl bg-[#151722]/95 backdrop-blur-xl border ${genericToast ? (genericToast.type === 'success' ? 'border-emerald-500/40' : genericToast.type === 'error' ? 'border-red-500/40' : genericToast.type === 'info' ? 'border-blue-500/40' : 'border-amber-500/40') : 'border-amber-500/40'} text-white shadow-2xl cursor-pointer hover:border-white/20 transition-all`}
          >
            <div className={`w-10 h-10 rounded-xl ${genericToast ? (genericToast.type === 'success' ? 'bg-emerald-500' : genericToast.type === 'error' ? 'bg-red-500' : genericToast.type === 'info' ? 'bg-blue-500' : 'bg-gray-500') : 'bg-gradient-to-tr from-amber-500 to-yellow-400'} text-black flex items-center justify-center text-xl font-black flex-shrink-0 shadow-lg`}>
              {genericToast ? (genericToast.type === 'success' ? '✅' : '❌') : (activeToast?.notificationType === 'like' ? '❤️' : activeToast?.notificationType === 'comment' ? '💬' : '🔔')}
            </div>
            <div className="flex-grow min-w-0">
              {activeToast && (
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-black text-amber-400 uppercase tracking-wider truncate">
                    {activeToast.title}
                  </span>
                  <span className="text-[10px] text-gray-400 flex-shrink-0">À l’instant</span>
                </div>
              )}
              <p className="text-xs text-gray-200 font-medium truncate mt-0.5">
                {genericToast ? genericToast.message : activeToast?.message}
              </p>
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation();
                dismissToast();
              }}
              className="text-gray-400 hover:text-white p-1 text-sm font-bold flex-shrink-0"
            >
              ✕
            </button>
          </div>
        </div>
      )}
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
};
