import React, { useState, useEffect } from 'react';
import { Wallet, CreditCard, ShieldCheck, ChevronRight, Sparkles } from 'lucide-react';
import { supabase } from '../lib/supabaseClient';

export default function PaymentPage() {
  const [balance, setBalance] = useState<number>(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchBalance = async () => {
      const { data: { user } } = await supabase.auth.getUser();
      if (user) {
        const { data } = await supabase
          .from('ai_credits')
          .select('balance')
          .eq('user_id', user.id)
          .single();
        if (data) setBalance(data.balance);
      }
      setLoading(false);
    };
    fetchBalance();
  }, []);

  const rechargePacks = [
    { id: 1, name: 'Pack Découverte', credits: 50, price: '1 000 FCFA', popular: false },
    { id: 2, name: 'Pack Créateur', credits: 250, price: '4 500 FCFA', popular: true },
    { id: 3, name: 'Pack Studio Pro', credits: 1000, price: '15 000 FCFA', popular: false },
  ];

  return (
    <div className="min-h-screen bg-[#0B0C12] text-white p-6 pb-24">
      <div className="max-w-xl mx-auto">
        <h1 className="text-3xl font-black text-amber-500 mb-2 uppercase tracking-tighter">Recharger mes crédits</h1>
        <p className="text-gray-400 text-sm mb-8">Boostez vos créations IA et envoyez des cadeaux pendant les lives.</p>

        {/* SOLDE ACTUEL */}
        <div className="bg-amber-500/10 border border-amber-500/20 rounded-3xl p-6 mb-10 flex items-center justify-between">
          <div>
            <div className="text-[10px] font-bold text-amber-500 uppercase tracking-widest mb-1">Solde actuel</div>
            <div className="text-4xl font-black text-white flex items-center gap-2">
              {loading ? '...' : balance} <span className="text-xl">🪙</span>
            </div>
          </div>
          <div className="w-14 h-14 bg-amber-500 rounded-2xl flex items-center justify-center shadow-lg shadow-amber-500/20">
            <Wallet className="w-8 h-8 text-black" />
          </div>
        </div>

        {/* LISTE DES PACKS */}
        <h2 className="text-lg font-black mb-4 uppercase tracking-tight flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-amber-500" />
          Choisissez un pack
        </h2>
        <div className="space-y-4 mb-10">
          {rechargePacks.map((pack) => (
            <button
              key={pack.id}
              className={`w-full p-5 rounded-2xl border transition-all flex items-center justify-between group ${
                pack.popular 
                ? 'bg-amber-500 border-amber-400 text-black scale-[1.02] shadow-xl shadow-amber-500/10' 
                : 'bg-white/5 border-white/10 text-white hover:bg-white/10'
              }`}
            >
              <div className="flex flex-col items-start">
                {pack.popular && (
                  <span className="bg-black text-white text-[8px] font-black px-2 py-0.5 rounded-full mb-2 uppercase tracking-widest">
                    Plus Populaire
                  </span>
                )}
                <div className="text-lg font-black">{pack.name}</div>
                <div className={`text-xs font-bold ${pack.popular ? 'text-black/60' : 'text-gray-400'}`}>
                  {pack.credits} Crédits PANU
                </div>
              </div>
              <div className="flex items-center gap-3">
                <div className="text-right">
                  <div className="font-black text-xl">{pack.price}</div>
                </div>
                <ChevronRight className={`w-5 h-5 ${pack.popular ? 'text-black' : 'text-amber-500'}`} />
              </div>
            </button>
          ))}
        </div>

        {/* MOYENS DE PAIEMENT */}
        <div className="bg-white/5 border border-white/10 rounded-3xl p-6">
          <h3 className="text-sm font-black mb-4 uppercase text-gray-300 flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-500" />
            Paiement sécurisé
          </h3>
          <div className="grid grid-cols-2 gap-3 mb-6">
            <div className="bg-black/40 rounded-xl p-3 flex flex-col items-center justify-center border border-white/5 opacity-60">
              <CreditCard className="w-6 h-6 mb-2" />
              <span className="text-[10px] font-bold">CARTE BANCAIRE</span>
            </div>
            <div className="bg-black/40 rounded-xl p-3 flex flex-col items-center justify-center border border-white/5 opacity-60 text-center">
              <div className="text-amber-500 font-black text-lg leading-none">CinetPay</div>
              <span className="text-[8px] font-bold">MOBILE MONEY</span>
            </div>
          </div>
          <p className="text-[10px] text-gray-500 leading-relaxed text-center italic">
            Les crédits sont ajoutés instantanément à votre compte après confirmation du paiement par notre partenaire sécurisé.
          </p>
        </div>
      </div>
    </div>
  );
}
