import React, { useState } from 'react';
import { supabase } from '../../lib/supabaseClient';
import { useNotifications } from '../../context/NotificationContext';
import { X, Wallet, CheckCircle2, Loader2, CreditCard, Smartphone } from 'lucide-react';

interface CreditRechargeModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentBalance: number;
}

const CREDIT_PACKS = [
  { id: 'pack-starter', credits: 250, price: '2€', icon: '⚡', description: 'Idéal pour débuter' },
  { id: 'pack-popular', credits: 1000, price: '7€', icon: '🔥', popular: true, description: 'Le choix des créateurs' },
  { id: 'pack-pro', credits: 5000, price: '30€', icon: '💎', description: 'Usage intensif Pro' },
];

export const CreditRechargeModal: React.FC<CreditRechargeModalProps> = ({
  isOpen,
  onClose,
  currentBalance,
}) => {
  const [selectedPack, setSelectedPack] = useState(CREDIT_PACKS[1]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const { showToast, currentUserId } = useNotifications();

  if (!isOpen) return null;

  const handleRecharge = async () => {
    if (!currentUserId) {
      showToast('Veuillez vous connecter pour recharger vos crédits', 'error');
      return;
    }

    setIsProcessing(true);

    try {
      // Simulation du délai de paiement
      await new Promise(resolve => setTimeout(resolve, 2000));

      // Mise à jour réelle dans Supabase
      // On récupère le solde actuel d'abord pour être sûr (même si on l'a en prop)
      const { data: currentData, error: fetchError } = await supabase
        .from('ai_credits')
        .select('balance')
        .eq('user_id', currentUserId)
        .single();

      if (fetchError) throw fetchError;

      const newBalance = (currentData?.balance || 0) + selectedPack.credits;

      const { error: updateError } = await supabase
        .from('ai_credits')
        .update({ balance: newBalance, updated_at: new Date().toISOString() })
        .eq('user_id', currentUserId);

      if (updateError) throw updateError;

      // Log de la transaction
      await supabase.from('ai_credit_transactions').insert({
        user_id: currentUserId,
        amount: selectedPack.credits,
        transaction_type: 'purchase',
        description: `Achat Pack ${selectedPack.credits} Crédits`
      });

      setIsSuccess(true);
      showToast(`${selectedPack.credits} crédits ajoutés avec succès !`, 'success');
      
      // Auto-fermeture après succès
      setTimeout(() => {
        setIsSuccess(false);
        onClose();
      }, 3000);

    } catch (err: any) {
      console.error('Erreur recharge:', err);
      showToast('Échec de la recharge : ' + (err.message || 'Erreur inconnue'), 'error');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[999] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-300">
      <div 
        className="w-full max-w-md bg-[#12141F] border border-white/10 rounded-3xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-300"
        onClick={(e) => e.stopPropagation()}
      >
        {/* HEADER */}
        <div className="flex items-center justify-between p-6 border-b border-white/5">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-amber-500/20 rounded-xl flex items-center justify-center text-amber-500">
              <Wallet className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-xl font-black text-white tracking-tight">Recharger</h2>
              <p className="text-xs text-gray-400 font-bold uppercase tracking-wider">
                Solde actuel : <span className="text-amber-500">{currentBalance} 🪙</span>
              </p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="w-10 h-10 flex items-center justify-center text-gray-400 hover:text-white bg-white/5 rounded-full transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* CONTENT */}
        <div className="p-6">
          {!isSuccess ? (
            <div className="space-y-6">
              <div className="grid gap-3">
                {CREDIT_PACKS.map((pack) => (
                  <button
                    key={pack.id}
                    onClick={() => setSelectedPack(pack)}
                    className={`relative flex items-center justify-between p-4 rounded-2xl border transition-all ${
                      selectedPack.id === pack.id 
                        ? 'bg-amber-500/10 border-amber-500 shadow-lg shadow-amber-500/5' 
                        : 'bg-white/[0.02] border-white/5 hover:bg-white/[0.05]'
                    }`}
                  >
                    <div className="flex items-center gap-4">
                      <span className="text-2xl">{pack.icon}</span>
                      <div className="text-left">
                        <div className="text-lg font-black text-white">{pack.credits} Crédits</div>
                        <div className="text-[10px] text-gray-500 font-bold uppercase">{pack.description}</div>
                      </div>
                    </div>
                    <div className="flex flex-col items-end">
                      <div className="text-xl font-black text-amber-500">{pack.price}</div>
                      {pack.popular && (
                        <span className="text-[9px] font-black bg-amber-500 text-black px-2 py-0.5 rounded-full mt-1 uppercase tracking-tighter">
                          Populaire
                        </span>
                      )}
                    </div>
                  </button>
                ))}
              </div>

              <div className="space-y-4">
                <div className="flex items-center gap-4 text-xs font-bold text-gray-500 uppercase tracking-widest px-1">
                  <div className="flex-grow h-px bg-white/5"></div>
                  <span>Mode de paiement</span>
                  <div className="flex-grow h-px bg-white/5"></div>
                </div>
                
                <div className="grid grid-cols-2 gap-3">
                  <div className="flex items-center gap-3 p-3 bg-white/[0.02] border border-white/5 rounded-xl text-gray-300">
                    <Smartphone className="w-4 h-4 text-amber-500" />
                    <span className="text-xs font-bold">Mobile Money</span>
                  </div>
                  <div className="flex items-center gap-3 p-3 bg-white/[0.02] border border-white/5 rounded-xl text-gray-300">
                    <CreditCard className="w-4 h-4 text-amber-500" />
                    <span className="text-xs font-bold">Carte Bancaire</span>
                  </div>
                </div>
              </div>

              <button
                disabled={isProcessing}
                onClick={handleRecharge}
                className="w-full bg-amber-500 hover:bg-amber-400 disabled:bg-gray-700 text-black h-14 rounded-2xl font-black text-lg shadow-xl shadow-amber-500/20 transition-all flex items-center justify-center gap-3"
              >
                {isProcessing ? (
                  <>
                    <Loader2 className="w-6 h-6 animate-spin" />
                    Traitement...
                  </>
                ) : (
                  <>Recharger {selectedPack.credits} Crédits</>
                )}
              </button>
              
              <p className="text-[10px] text-center text-gray-500 leading-relaxed px-4">
                Paiement sécurisé via PANU Pay Gateway. <br />
                En cliquant sur recharger, vous acceptez nos conditions de vente.
              </p>
            </div>
          ) : (
            <div className="py-12 flex flex-col items-center text-center space-y-6 animate-in zoom-in-95 duration-500">
              <div className="w-24 h-24 bg-emerald-500/20 rounded-full flex items-center justify-center text-emerald-500">
                <CheckCircle2 className="w-16 h-16" />
              </div>
              <div>
                <h3 className="text-2xl font-black text-white">Recharge Réussie !</h3>
                <p className="text-gray-400 mt-2">
                  Votre compte a été crédité de <span className="text-amber-500 font-bold">{selectedPack.credits} 🪙</span>.
                </p>
              </div>
              <div className="p-4 bg-white/[0.02] border border-white/5 rounded-2xl w-full">
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-gray-400 font-bold uppercase text-[10px]">ID Transaction</span>
                  <span className="text-white font-mono text-xs tracking-tighter">TRX-{Math.random().toString(36).substring(2, 10).toUpperCase()}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-400 font-bold uppercase text-[10px]">Nouveau Solde</span>
                  <span className="text-emerald-500 font-black">{currentBalance + selectedPack.credits} 🪙</span>
                </div>
              </div>
              <button
                onClick={onClose}
                className="w-full bg-white/5 hover:bg-white/10 text-white h-12 rounded-xl font-bold transition-all"
              >
                Fermer
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
