import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Home,
  Film,
  Plus,
  Sparkles,
  User,
} from 'lucide-react';

export interface PanuBottomNavProps {
  onOpenCreate?: () => void;
  onOpenTemplates?: () => void;
}

/**
 * Barre de navigation inférieure sombre fixe (Style TikTok / Shorts)
 * 5 onglets :
 * 🏠 Accueil (Onglet actif, redirige vers /)
 * 🎬 Templates (Mis en avant en couleur dorée/orange #f59e0b)
 * [ + ] Créer (Grand bouton central coloré style TikTok 3D)
 * ✨ Studio IA (Redirige vers /studio-ia)
 * 👤 Profil (Redirige vers /profile)
 */
export const PanuBottomNav: React.FC<PanuBottomNavProps> = ({ onOpenCreate, onOpenTemplates }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const isHomeActive = location.pathname === '/' || location.pathname === '/home';
  const isTemplatesActive = location.pathname === '/vod' || location.pathname === '/creations';
  const isStudioActive = location.pathname === '/studio' || location.pathname === '/studio-ia';
  const isAccountActive = location.pathname === '/profile' || location.pathname === '/compte' || location.pathname === '/account';

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-black w-full border-t border-slate-800 z-50 px-3 py-2 flex items-center justify-around pointer-events-auto shadow-2xl">
      {/* 1. 🏠 Accueil */}
      <button
        onClick={() => navigate('/')}
        className={`flex flex-col items-center transition-transform active:scale-95 ${
          isHomeActive ? 'text-white font-bold' : 'text-gray-400 hover:text-white'
        }`}
      >
        <Home className={`w-6 h-6 ${isHomeActive ? 'text-white' : 'text-gray-400'}`} />
        <span className="text-[11px] mt-0.5 tracking-tight">Accueil</span>
      </button>

      {/* 2. 🎬 Templates (Mis en avant en couleur dorée/orange #f59e0b) */}
      <button
        onClick={() => {
          if (onOpenTemplates) {
            onOpenTemplates();
          } else {
            navigate('/vod');
          }
        }}
        className={`flex flex-col items-center transition-transform active:scale-95 text-amber-500 font-bold`}
      >
        <Film className="w-6 h-6 text-amber-500" />
        <span className="text-[11px] mt-0.5 tracking-tight text-amber-500 font-extrabold">Templates</span>
      </button>

      {/* 3. [ + ] Créer (Grand bouton central coloré style TikTok 3D) */}
      <button
        onClick={() => {
          if (onOpenCreate) {
            onOpenCreate();
          } else {
            navigate('/studio-ia');
          }
        }}
        className="flex items-center justify-center transition-transform active:scale-90 px-1"
        title="Créer"
      >
        <div className="relative flex items-center justify-center">
          <div className="absolute -left-1 w-11 h-8 bg-cyan-400 rounded-lg" />
          <div className="absolute -right-1 w-11 h-8 bg-red-500 rounded-lg" />
          <div className="relative w-11 h-8 bg-white rounded-lg flex items-center justify-center z-10 shadow-md">
            <Plus className="w-5 h-5 text-black stroke-[3.5]" />
          </div>
        </div>
      </button>

      {/* 4. ✨ Studio IA */}
      <button
        onClick={() => navigate('/studio-ia')}
        className={`flex flex-col items-center transition-transform active:scale-95 ${
          isStudioActive ? 'text-amber-400 font-bold' : 'text-gray-400 hover:text-white'
        }`}
      >
        <Sparkles className="w-6 h-6 text-amber-400" />
        <span className="text-[11px] mt-0.5 tracking-tight">Studio IA</span>
      </button>

      {/* 5. 👤 Profil */}
      <button
        onClick={() => navigate('/profile')}
        className={`flex flex-col items-center transition-transform active:scale-95 ${
          isAccountActive ? 'text-white font-bold' : 'text-gray-400 hover:text-white'
        }`}
      >
        <User className={`w-6 h-6 ${isAccountActive ? 'text-white' : 'text-gray-400'}`} />
        <span className="text-[11px] mt-0.5 tracking-tight">Profil</span>
      </button>
    </nav>
  );
};

export default PanuBottomNav;
