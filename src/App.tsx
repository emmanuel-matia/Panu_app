import React, { useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import PanuAppRouter from './router';
import { Header } from './components/Header';
import { PanuBottomNav } from './components/nav/PanuBottomNav';
import { NotificationProvider } from './context/NotificationContext';

/**
 * POINT D'ENTRÉE PRINCIPAL PANU
 * Gère le layout global avec l'entête, les notifications temps réel et la navigation inférieure.
 */
export const App: React.FC = () => {
  const [userCredits, setUserCredits] = useState<number>(250);
  const [searchQuery, setSearchQuery] = useState('');

  return (
    <BrowserRouter>
      <NotificationProvider>
        <div className="flex flex-col min-h-screen bg-[#0B0C12] text-white">
          {/* Entête Globale avec Notifications Realtime & Recherche */}
          <Header 
            userBalance={userCredits} 
            onBalanceUpdate={setUserCredits} 
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
          />
          
          {/* Contenu de la Page */}
          <main className="flex-grow pb-24">
            <PanuAppRouter searchQuery={searchQuery} />
          </main>

          {/* Barre de Navigation Inférieure avec bouton jaune + central */}
          <PanuBottomNav />
        </div>
      </NotificationProvider>
    </BrowserRouter>
  );
};

export default App;
