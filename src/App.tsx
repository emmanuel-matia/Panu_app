import React, { useState, useEffect, Component, ReactNode } from 'react';
import { BrowserRouter, useLocation } from 'react-router-dom';
import PanuAppRouter from './router';
import { Header } from './components/Header';
import { PanuBottomNav } from './components/nav/PanuBottomNav';
import { NotificationProvider } from './context/NotificationContext';
import { supabase } from './lib/supabaseClient';

class ErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean; message: string }> {
  constructor(props: { children: ReactNode }) {
    super(props);
    this.state = { hasError: false, message: '' };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, message: error.message };
  }

  componentDidCatch(error: Error) {
    console.error('[PANU ErrorBoundary]', error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#0B0C12', color: '#F8F9FA', fontFamily: 'sans-serif', padding: 24, textAlign: 'center' }}>
          <div style={{ width: 64, height: 64, background: '#E5A93C', borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20 }}>
            <span style={{ color: '#000', fontWeight: 900, fontSize: 32 }}>P</span>
          </div>
          <h2 style={{ fontSize: 20, fontWeight: 800, marginBottom: 8 }}>Une erreur est survenue</h2>
          <p style={{ color: '#9CA3AF', fontSize: 14, maxWidth: 400, marginBottom: 20 }}>
            L'application a rencontré un problème. Cliquez ci-dessous pour recharger.
          </p>
          <button
            onClick={() => { this.setState({ hasError: false, message: '' }); window.location.reload(); }}
            style={{ background: '#E5A93C', color: '#000', border: 'none', borderRadius: 10, padding: '12px 28px', fontWeight: 800, fontSize: 14, cursor: 'pointer' }}
          >
            Recharger
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

interface AppContentProps {
  userCredits: number;
  setUserCredits: React.Dispatch<React.SetStateAction<number>>;
  searchQuery: string;
  setSearchQuery: React.Dispatch<React.SetStateAction<string>>;
  showMyPublications: boolean;
  setShowMyPublications: React.Dispatch<React.SetStateAction<boolean>>;
}

const AppContent: React.FC<AppContentProps> = ({
  userCredits,
  setUserCredits,
  searchQuery,
  setSearchQuery,
  showMyPublications,
  setShowMyPublications,
}) => {
  const location = useLocation();
  const isImmersiveFeed =
    location.pathname === '/' ||
    location.pathname === '/home' ||
    location.pathname === '/index.html' ||
    location.pathname === '/feed' ||
    location.pathname === '';

  return (
    <div className="flex flex-col min-h-screen bg-[#0B0C12] text-white">
      {/* N'afficher le Header classique QUE sur les pages secondaires (ex: profil, login) */}
      {!isImmersiveFeed && (
        <Header
          userBalance={userCredits}
          onBalanceUpdate={setUserCredits}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          showMyPublications={showMyPublications}
          onToggleMyPublications={() => setShowMyPublications(!showMyPublications)}
        />
      )}

      {/* Sur la page d'accueil, le lecteur immersif prend 100% de la hauteur sans padding */}
      <main className={isImmersiveFeed ? 'w-full h-screen overflow-hidden' : 'flex-grow pb-20'}>
        <PanuAppRouter searchQuery={searchQuery} showMyPublications={showMyPublications} />
      </main>

      {/* Sur les pages secondaires, afficher la barre de navigation inférieure commune */}
      {!isImmersiveFeed && <PanuBottomNav />}
    </div>
  );
};

export const App: React.FC = () => {
  const [userCredits, setUserCredits] = useState<number>(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [showMyPublications, setShowMyPublications] = useState(false);

  useEffect(() => {
    const fetchCredits = async (userId: string) => {
      try {
        const { data } = await supabase
          .from('ai_credits')
          .select('balance')
          .eq('user_id', userId)
          .maybeSingle();
        if (data?.balance !== undefined && data?.balance !== null) {
          setUserCredits(data.balance);
        }
      } catch (e) {
        console.warn('Could not fetch credits:', e);
      }
    };

    supabase.auth.getUser().then(({ data }) => {
      if (data?.user?.id) fetchCredits(data.user.id);
    });

    const { data: listener } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session?.user?.id) {
        fetchCredits(session.user.id);
      } else {
        setUserCredits(0);
      }
    });

    return () => { listener.subscription.unsubscribe(); };
  }, []);

  return (
    <ErrorBoundary>
      <BrowserRouter>
        <NotificationProvider>
          <AppContent
            userCredits={userCredits}
            setUserCredits={setUserCredits}
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
            showMyPublications={showMyPublications}
            setShowMyPublications={setShowMyPublications}
          />
        </NotificationProvider>
      </BrowserRouter>
    </ErrorBoundary>
  );
};

export default App;
