import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Home, Video, Plus, Radio, User } from 'lucide-react';

export interface PanuBottomNavProps {
  onOpenCreate?: () => void;
}

/**
 * Composant `PanuBottomNav` :
 * - Barre de navigation inférieure fixe avec le bouton central jaune d'action rapide (+).
 * - Navigation fluide entre Accueil, Studio, Directs et Profil.
 */
export const PanuBottomNav: React.FC<PanuBottomNavProps> = ({ onOpenCreate }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path;

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-gray-950/95 backdrop-blur-md border-t border-gray-800/80 z-50 px-4 py-2">
      <div className="max-w-md mx-auto flex items-center justify-between">
        {/* 1. ACCUEIL */}
        <button
          onClick={() => navigate('/')}
          className={`flex flex-col items-center gap-1 transition-colors ${
            isActive('/') || isActive('/home') ? 'text-amber-400 font-bold' : 'text-gray-400 hover:text-white'
          }`}
        >
          <Home className="w-5 h-5" />
          <span className="text-[10px]">Accueil</span>
        </button>

        {/* 2. STUDIO */}
        <button
          onClick={() => navigate('/studio')}
          className={`flex flex-col items-center gap-1 transition-colors ${
            isActive('/studio') ? 'text-amber-400 font-bold' : 'text-gray-400 hover:text-white'
          }`}
        >
          <Video className="w-5 h-5" />
          <span className="text-[10px]">Studio</span>
        </button>

        {/* 3. BOUTON JAUNE + CENTRAL */}
        <button
          onClick={() => {
            if (onOpenCreate) {
              onOpenCreate();
            } else {
              navigate('/studio');
            }
          }}
          aria-label="Créer ou publier"
          className="relative -top-4 w-12 h-12 rounded-full bg-gradient-to-tr from-amber-500 to-yellow-400 text-black flex items-center justify-center font-black shadow-lg shadow-amber-500/30 hover:scale-105 active:scale-95 transition-all border-2 border-black"
        >
          <Plus className="w-7 h-7 stroke-[3]" />
        </button>

        {/* 4. DIRECTS */}
        <button
          onClick={() => navigate('/live')}
          className={`flex flex-col items-center gap-1 transition-colors ${
            isActive('/live') ? 'text-amber-400 font-bold' : 'text-gray-400 hover:text-white'
          }`}
        >
          <Radio className="w-5 h-5" />
          <span className="text-[10px]">Directs</span>
        </button>

        {/* 5. PROFIL */}
        <button
          onClick={() => navigate('/profile')}
          className={`flex flex-col items-center gap-1 transition-colors ${
            isActive('/profile') ? 'text-amber-400 font-bold' : 'text-gray-400 hover:text-white'
          }`}
        >
          <User className="w-5 h-5" />
          <span className="text-[10px]">Profil</span>
        </button>
      </div>
    </nav>
  );
};

export default PanuBottomNav;
