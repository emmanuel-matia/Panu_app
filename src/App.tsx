import React, { useState, useEffect, Component, ReactNode } from 'react';
import { BrowserRouter } from 'react-router-dom';
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
            L'application a rencontre un probleme. Rafraichissez la page pour reessayer.
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

const LoadingScreen = () => (
  <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#0B0C12' }}>
    <div style={{
      width: 72, height: 72, background: '#E5A93C', borderRadius: 18,
      display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 24,
      boxShadow: '0 0 40px rgba(229,169,60,0.3)',
      animation: 'pulse 1.5s ease-in-out infinite',
    }}>
      <span style={{ color: '#000', fontWeight: 900, fontSize: 36 }}>P</span>
    </div>
    <div style={{ color: '#E5A93C', fontWeight: 900, fontSize: 24, letterSpacing: 2, marginBottom: 8 }}>PANU</div>
    <div style={{ color: '#6B7280', fontSize: 13, fontWeight: 500 }}>Chargement du studio creatif...</div>
    <style>{`@keyframes pulse { 0%,100% { transform: scale(1); opacity: 1; } 50% { transform: scale(0.95); opacity: 0.8; } }`}</style>
  </div>
);

export const App: React.FC = () => {
  const [userCredits, setUserCredits] = useState<number>(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [isReady, setIsReady] = useState(false);

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

    const init = async () => {
      try {
        const { data } = await supabase.auth.getUser();
        if (data?.user?.id) await fetchCredits(data.user.id);
      } catch (e) {
        console.warn('Auth check failed:', e);
      }
      setIsReady(true);
    };

    init();

    const { data: listener } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session?.user?.id) {
        fetchCredits(session.user.id);
      } else {
        setUserCredits(0);
      }
    });

    return () => { listener.subscription.unsubscribe(); };
  }, []);

  if (!isReady) return <LoadingScreen />;

  return (
    <ErrorBoundary>
      <BrowserRouter>
        <NotificationProvider>
          <div className="flex flex-col min-h-screen bg-[#0B0C12] text-white">
            <Header
              userBalance={userCredits}
              onBalanceUpdate={setUserCredits}
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
            />

            <main className="flex-grow pb-24">
              <PanuAppRouter searchQuery={searchQuery} />
            </main>

            <PanuBottomNav />
          </div>
        </NotificationProvider>
      </BrowserRouter>
    </ErrorBoundary>
  );
};

export default App;
