import React, { useState, useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import PanuAppRouter from './router';
import { Header } from './components/Header';
import { PanuBottomNav } from './components/nav/PanuBottomNav';
import { NotificationProvider } from './context/NotificationContext';
import { supabase } from './lib/supabaseClient';

export const App: React.FC = () => {
  const [userCredits, setUserCredits] = useState<number>(0);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const fetchCredits = async (userId: string) => {
      const { data } = await supabase
        .from('ai_credits')
        .select('balance')
        .eq('user_id', userId)
        .maybeSingle();
      if (data?.balance !== undefined && data?.balance !== null) {
        setUserCredits(data.balance);
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
  );
};

export default App;
