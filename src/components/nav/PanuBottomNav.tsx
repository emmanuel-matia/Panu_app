import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Home as HomeIcon,
  Sparkles as SparklesIcon,
  Video as VideoIcon,
  User as UserIcon
} from 'lucide-react';

export interface PanuBottomNavProps {
  onOpenCreate?: () => void;
}

/**
 * Composant `PanuBottomNav` :
 * Barre de navigation inférieure corrigée avec 4 onglets :
 * - Accueil
 * - Studio IA
 * - Créations
 * - Compte
 */
export const PanuBottomNav: React.FC<PanuBottomNavProps> = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const isHomeActive = location.pathname === '/' || location.pathname === '/home';
  const isStudioActive = location.pathname === '/studio' || location.pathname === '/studio-ia';
  const isCreationsActive = location.pathname === '/creations' || location.pathname === '/vod';
  const isAccountActive = location.pathname === '/profile' || location.pathname === '/compte' || location.pathname === '/account';

  // Barre de navigation inférieure corrigée
  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-100 shadow-lg flex justify-around items-center py-2 z-50">
      <button
        onClick={() => navigate('/')}
        className={`flex flex-col items-center transition-colors ${
          isHomeActive ? 'text-amber-600 font-medium' : 'text-gray-400 hover:text-amber-600'
        }`}
      >
        <HomeIcon className="w-6 h-6" />
        <span className="text-xs mt-1">Accueil</span>
      </button>

      <button
        onClick={() => navigate('/studio')}
        className={`flex flex-col items-center transition-colors ${
          isStudioActive ? 'text-amber-600 font-medium' : 'text-gray-400 hover:text-amber-600'
        }`}
      >
        <SparklesIcon className="w-6 h-6" />
        <span className="text-xs mt-1">Studio IA</span>
      </button>

      <button
        onClick={() => navigate('/creations')}
        className={`flex flex-col items-center transition-colors ${
          isCreationsActive ? 'text-amber-600 font-medium' : 'text-gray-400 hover:text-amber-600'
        }`}
      >
        <VideoIcon className="w-6 h-6" />
        <span className="text-xs mt-1">Créations</span>
      </button>

      <button
        onClick={() => navigate('/profile')}
        className={`flex flex-col items-center transition-colors ${
          isAccountActive ? 'text-amber-600 font-medium' : 'text-gray-400 hover:text-amber-600'
        }`}
      >
        <UserIcon className="w-6 h-6" />
        <span className="text-xs mt-1">Compte</span>
      </button>
    </nav>
  );
};

export default PanuBottomNav;
