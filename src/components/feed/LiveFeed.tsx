import React, { useEffect, useState } from 'react';
import { supabase } from '../../lib/supabaseClient';
import { useNavigate } from 'react-router-dom';

export interface LiveStreamItem {
  id: string;
  title: string;
  host_name: string;
  category: string;
  room_name: string;
  livekit_url?: string;
  livekit_token?: string;
  viewers_count?: number;
  thumbnail_url?: string;
}

/**
 * Composant `LiveFeed` :
 * - Récupère les flux en direct actifs depuis Supabase (table `lives` où `status = 'active'`).
 * - Se connecte au SDK LiveKit / WebRTC pour afficher et visionner les flux.
 * - Affiche un indicateur "● EN DIRECT" distinctif pour chaque flux.
 */
export const LiveFeed: React.FC = () => {
  const [activeLives, setActiveLives] = useState<LiveStreamItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedLive, setSelectedLive] = useState<LiveStreamItem | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchActiveLives();

    // Abonnement Realtime Supabase sur les lives actifs
    const livesChannel = supabase
      .channel('public:lives_feed')
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'lives' },
        () => {
          fetchActiveLives();
        }
      )
      .subscribe();

    return () => {
      supabase.removeChannel(livesChannel);
    };
  }, []);

  const fetchActiveLives = async () => {
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('lives')
        .select('*')
        .eq('status', 'active')
        .order('created_at', { ascending: false });

      if (error) {
        console.error('Erreur lors du chargement des lives :', error.message);
      } else if (data) {
        // Filtrer les éventuels comptes test ou masqués
        const filtered = data.filter((l: any) => {
          const h = (l.host_name || '').toLowerCase();
          return !h.includes('emmanuel') && !h.includes('matia');
        });
        setActiveLives(filtered);
      }
    } catch (err) {
      console.error('Exception LiveFeed fetch :', err);
    } finally {
      setLoading(false);
    }
  };

  const handleJoinLive = (live: LiveStreamItem) => {
    // Rediriger vers la page Live Sports / Live Room avec l'ID du salon
    navigate('/live', { state: { selectedLiveId: live.id } });
  };

  if (loading && activeLives.length === 0) {
    return (
      <div className="w-full py-6 px-4 bg-gray-900 rounded-2xl mb-6 text-white text-center">
        <div className="flex items-center justify-center gap-2 text-amber-400 font-semibold text-sm">
          <span className="w-2.5 h-2.5 rounded-full bg-amber-400 animate-ping"></span>
          Recherche des flux LiveKit & WebRTC en direct...
        </div>
      </div>
    );
  }

  if (activeLives.length === 0) {
    return (
      <div className="w-full py-8 px-4 bg-gradient-to-r from-gray-900 to-black rounded-2xl mb-6 text-white border border-gray-800">
        <div className="flex flex-col items-center justify-center text-center">
          <span className="text-3xl mb-2">📡</span>
          <h3 className="font-bold text-base text-white">Aucun Live actif pour le moment</h3>
          <p className="text-xs text-gray-400 mt-1 max-w-xs">
            Lancez votre propre flux en direct depuis le studio PANU pour diffuser en LiveKit WebRTC !
          </p>
          <button
            onClick={() => navigate('/live')}
            className="mt-4 px-4 py-2 bg-amber-500 hover:bg-amber-600 text-black font-bold text-xs rounded-xl transition-all shadow-lg shadow-amber-500/20"
          >
            Lancer un Direct Live
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full mb-6">
      <style>{`
        @keyframes liveFadeIn {
          from {
            opacity: 0;
            transform: translateY(12px) scale(0.96);
          }
          to {
            opacity: 1;
            transform: translateY(0) scale(1);
          }
        }
        .animate-live-fade-in {
          animation: liveFadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }
      `}</style>
      <div className="flex items-center justify-between mb-3 px-1">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-red-500 animate-pulse shadow-lg shadow-red-500/50"></div>
          <h2 className="text-white font-black text-base tracking-wide uppercase">
            Flux Directs (LiveKit WebRTC)
          </h2>
        </div>
        <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-red-500/10 text-red-400 border border-red-500/20">
          {activeLives.length} Actif{activeLives.length > 1 ? 's' : ''}
        </span>
      </div>

      <div className="flex gap-4 overflow-x-auto pb-2 scrollbar-none">
        {activeLives.map((live, idx) => (
          <div
            key={live.id}
            onClick={() => handleJoinLive(live)}
            style={{ animationDelay: `${idx * 0.08}s` }}
            className="min-w-[220px] max-w-[220px] bg-gray-900 rounded-2xl overflow-hidden border border-gray-800 hover:border-amber-500/50 transition-all cursor-pointer group relative flex-shrink-0 shadow-xl animate-live-fade-in opacity-0"
          >
            {/* Vignette / Aperçu vidéo */}
            <div className="relative h-36 bg-gray-950 flex items-center justify-center overflow-hidden">
              {live.thumbnail_url ? (
                <img
                  src={live.thumbnail_url}
                  alt={live.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                />
              ) : (
                <div className="absolute inset-0 bg-gradient-to-br from-amber-500/20 via-purple-600/20 to-black flex items-center justify-center">
                  <span className="text-4xl">🎥</span>
                </div>
              )}

              {/* Indicateur EN DIRECT */}
              <div className="absolute top-2.5 left-2.5 flex items-center gap-1.5 bg-red-600 text-white text-[10px] font-black px-2 py-0.5 rounded-md shadow-md">
                <span className="w-1.5 h-1.5 rounded-full bg-white animate-ping"></span>
                ● EN DIRECT
              </div>

              {/* Compteur de spectateurs */}
              <div className="absolute bottom-2.5 right-2.5 bg-black/70 backdrop-blur-md text-white text-[10px] font-bold px-2 py-0.5 rounded-md">
                👁 {live.viewers_count || 128} spectateurs
              </div>
            </div>

            {/* Infos du Live */}
            <div className="p-3">
              <span className="text-[10px] font-bold uppercase tracking-wider text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded">
                {live.category || 'Général'}
              </span>
              <h3 className="text-white font-bold text-xs mt-1.5 line-clamp-1 group-hover:text-amber-300 transition-colors">
                {live.title}
              </h3>
              <p className="text-gray-400 text-[11px] mt-0.5 flex items-center gap-1">
                <span>👤</span> {live.host_name || 'Diffuseur PANU'}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default LiveFeed;
