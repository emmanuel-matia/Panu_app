import React, { useState } from 'react';

export interface CoinPack {
  id: string;
  coins: number;
  bonusCoins: number;
  priceUsd: number;
  priceFc: number; // Franc Congolais (~2800 FC / USD)
  priceXof: number; // Franc CFA (~650 XOF / USD)
  badge?: string;
  isPopular?: boolean;
}

export const COIN_PACKS: CoinPack[] = [
  {
    id: 'pack_100',
    coins: 100,
    bonusCoins: 0,
    priceUsd: 1,
    priceFc: 2800,
    priceXof: 650,
    badge: 'Découverte',
  },
  {
    id: 'pack_500',
    coins: 500,
    bonusCoins: 50,
    priceUsd: 5,
    priceFc: 14000,
    priceXof: 3250,
    badge: '+50 Offertes',
  },
  {
    id: 'pack_1200',
    coins: 1200,
    bonusCoins: 200,
    priceUsd: 10,
    priceFc: 28000,
    priceXof: 6500,
    badge: 'Le Plus Populaire 🔥',
    isPopular: true,
  },
  {
    id: 'pack_3000',
    coins: 3000,
    bonusCoins: 600,
    priceUsd: 25,
    priceFc: 70000,
    priceXof: 16250,
    badge: '+600 Offertes',
  },
  {
    id: 'pack_7000',
    coins: 7000,
    bonusCoins: 1500,
    priceUsd: 50,
    priceFc: 140000,
    priceXof: 32500,
    badge: 'VIP Créateur 👑',
  },
];

interface CinetPayRechargeModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentBalance: number;
  onSuccess: (newCoins: number) => void;
}

type PaymentCategory = 'mobile_money' | 'card' | 'international';

export const CinetPayRechargeModal: React.FC<CinetPayRechargeModalProps> = ({
  isOpen,
  onClose,
  currentBalance,
  onSuccess,
}) => {
  const [selectedPack, setSelectedPack] = useState<CoinPack>(COIN_PACKS[2]); // 1200 par défaut
  const [paymentCategory, setPaymentCategory] = useState<PaymentCategory>('mobile_money');
  const [selectedProvider, setSelectedProvider] = useState<string>('mpesa_vodacom');
  const [phoneNumber, setPhoneNumber] = useState<string>('');
  const [cardNumber, setCardNumber] = useState<string>('');
  const [cardExp, setCardExp] = useState<string>('');
  const [cardCvv, setCardCvv] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [successInfo, setSuccessInfo] = useState<{ txId: string; totalCoins: number } | null>(null);

  if (!isOpen) return null;

  const totalCoinsToReceive = selectedPack.coins + selectedPack.bonusCoins;

  const handleProcessPayment = () => {
    setIsProcessing(true);
    setTimeout(() => {
      const txId = 'CP-' + Math.floor(10000000 + Math.random() * 90000000);
      setIsProcessing(false);
      setSuccessInfo({ txId, totalCoins: totalCoinsToReceive });
      onSuccess(totalCoinsToReceive);
    }, 1800);
  };

  const handleFinish = () => {
    setSuccessInfo(null);
    onClose();
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(5, 6, 10, 0.85)',
        backdropFilter: 'blur(8px)',
        zIndex: 9990,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 16,
      }}
      onClick={onClose}
    >
      <div
        style={{
          backgroundColor: '#12141C',
          borderRadius: 20,
          border: '1px solid rgba(229, 169, 60, 0.4)',
          maxWidth: 580,
          width: '100%',
          maxHeight: '90vh',
          overflowY: 'auto',
          padding: 24,
          boxShadow: '0 20px 60px rgba(0, 0, 0, 0.8)',
          color: '#FFF',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {successInfo ? (
          /* ÉCRAN DE CONFIRMATION SUCCÈS CINETPAY */
          <div style={{ textAlign: 'center', padding: '24px 8px' }}>
            <div
              style={{
                width: 76,
                height: 76,
                borderRadius: '50%',
                backgroundColor: 'rgba(46, 213, 115, 0.15)',
                border: '2px solid #2ED573',
                color: '#2ED573',
                fontSize: 38,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 18px',
              }}
            >
              ✓
            </div>
            <h2 style={{ fontSize: 22, fontWeight: 900, margin: '0 0 8px', color: '#FFF' }}>
              Paiement Sécurisé Réussi !
            </h2>
            <p style={{ color: '#A0A5BA', fontSize: 14, margin: '0 0 20px' }}>
              Votre solde a été immédiatement crédité via la passerelle certifiée{' '}
              <strong style={{ color: '#E5A93C' }}>CinetPay</strong>.
            </p>

            <div
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid rgba(255, 255, 255, 0.08)',
                borderRadius: 14,
                padding: 16,
                textAlign: 'left',
                marginBottom: 24,
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontSize: 13 }}>
                <span style={{ color: '#8E92A4' }}>ID Transaction :</span>
                <span style={{ fontWeight: 700, fontFamily: 'monospace', color: '#E5A93C' }}>
                  {successInfo.txId}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontSize: 13 }}>
                <span style={{ color: '#8E92A4' }}>Pièces achetées :</span>
                <span style={{ fontWeight: 800, color: '#FFD700' }}>
                  +{successInfo.totalCoins} Pièces 🪙
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontSize: 13 }}>
                <span style={{ color: '#8E92A4' }}>Montant débité :</span>
                <span style={{ fontWeight: 800, color: '#FFF' }}>
                  ${selectedPack.priceUsd}.00 USD ({selectedPack.priceFc.toLocaleString()} FC)
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                <span style={{ color: '#8E92A4' }}>Nouveau Solde :</span>
                <span style={{ fontWeight: 900, color: '#2ED573' }}>
                  {(currentBalance + successInfo.totalCoins).toLocaleString()} Pièces
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={handleFinish}
              style={{
                width: '100%',
                backgroundColor: '#E5A93C',
                color: '#000',
                border: 'none',
                borderRadius: 12,
                padding: '14px',
                fontSize: 15,
                fontWeight: 900,
                cursor: 'pointer',
              }}
            >
              Terminer & Utiliser mes Pièces
            </button>
          </div>
        ) : (
          /* FORMULAIRE DE RECHARGEMENT CINETPAY */
          <>
            {/* EN-TÊTE DU MODAL */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
                paddingBottom: 14,
                marginBottom: 18,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 10,
                    backgroundColor: '#E5A93C',
                    color: '#000',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 900,
                    fontSize: 20,
                  }}
                >
                  🪙
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900 }}>
                    Recharger des Pièces (Coins)
                  </h3>
                  <p style={{ margin: 0, fontSize: 12, color: '#8E92A4' }}>
                    Solde actuel : <strong style={{ color: '#FFD700' }}>{currentBalance.toLocaleString()} Pièces</strong>
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={onClose}
                style={{
                  backgroundColor: 'transparent',
                  border: 'none',
                  color: '#8E92A4',
                  fontSize: 22,
                  cursor: 'pointer',
                }}
              >
                ✕
              </button>
            </div>

            {/* SÉLECTION DU PACK DE PIÈCES */}
            <div style={{ marginBottom: 20 }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: '#DDD', marginBottom: 10 }}>
                1. Choisissez votre Pack de Pièces :
              </div>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
                  gap: 10,
                }}
              >
                {COIN_PACKS.map((pack) => {
                  const isSelected = selectedPack.id === pack.id;
                  return (
                    <div
                      key={pack.id}
                      onClick={() => setSelectedPack(pack)}
                      style={{
                        backgroundColor: isSelected ? 'rgba(229, 169, 60, 0.15)' : 'rgba(255, 255, 255, 0.03)',
                        border: isSelected ? '2px solid #E5A93C' : '1px solid rgba(255, 255, 255, 0.1)',
                        borderRadius: 12,
                        padding: '12px 10px',
                        cursor: 'pointer',
                        textAlign: 'center',
                        position: 'relative',
                        transition: 'all 0.2s ease',
                      }}
                    >
                      {pack.badge && (
                        <span
                          style={{
                            position: 'absolute',
                            top: -8,
                            left: '50%',
                            transform: 'translateX(-50%)',
                            backgroundColor: pack.isPopular ? '#FF2E4C' : '#E5A93C',
                            color: pack.isPopular ? '#FFF' : '#000',
                            fontSize: 9,
                            fontWeight: 900,
                            padding: '2px 6px',
                            borderRadius: 999,
                            whiteSpace: 'nowrap',
                          }}
                        >
                          {pack.badge}
                        </span>
                      )}
                      <div style={{ fontSize: 20, fontWeight: 900, color: '#FFD700', marginTop: 4 }}>
                        {pack.coins + pack.bonusCoins} 🪙
                      </div>
                      <div style={{ fontSize: 14, fontWeight: 800, color: '#FFF', marginTop: 4 }}>
                        ${pack.priceUsd}.00
                      </div>
                      <div style={{ fontSize: 10, color: '#8E92A4', marginTop: 2 }}>
                        {pack.priceFc.toLocaleString()} FC / {pack.priceXof.toLocaleString()} CFA
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* SÉLECTEUR DE CATÉGORIE DE PAIEMENT */}
            <div style={{ marginBottom: 18 }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: '#DDD', marginBottom: 10 }}>
                2. Mode de Paiement Sécurisé (CinetPay Gateway) :
              </div>
              <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
                <button
                  type="button"
                  onClick={() => {
                    setPaymentCategory('mobile_money');
                    setSelectedProvider('mpesa_vodacom');
                  }}
                  style={{
                    flex: 1,
                    padding: '8px 10px',
                    borderRadius: 8,
                    fontSize: 12,
                    fontWeight: 700,
                    cursor: 'pointer',
                    border: paymentCategory === 'mobile_money' ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.1)',
                    backgroundColor: paymentCategory === 'mobile_money' ? 'rgba(229,169,60,0.15)' : 'rgba(255,255,255,0.02)',
                    color: paymentCategory === 'mobile_money' ? '#E5A93C' : '#AAA',
                  }}
                >
                  📱 Mobile Money
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setPaymentCategory('card');
                    setSelectedProvider('visa');
                  }}
                  style={{
                    flex: 1,
                    padding: '8px 10px',
                    borderRadius: 8,
                    fontSize: 12,
                    fontWeight: 700,
                    cursor: 'pointer',
                    border: paymentCategory === 'card' ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.1)',
                    backgroundColor: paymentCategory === 'card' ? 'rgba(229,169,60,0.15)' : 'rgba(255,255,255,0.02)',
                    color: paymentCategory === 'card' ? '#E5A93C' : '#AAA',
                  }}
                >
                  💳 Cartes & M-Pesa Visa
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setPaymentCategory('international');
                    setSelectedProvider('paypal');
                  }}
                  style={{
                    flex: 1,
                    padding: '8px 10px',
                    borderRadius: 8,
                    fontSize: 12,
                    fontWeight: 700,
                    cursor: 'pointer',
                    border: paymentCategory === 'international' ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.1)',
                    backgroundColor: paymentCategory === 'international' ? 'rgba(229,169,60,0.15)' : 'rgba(255,255,255,0.02)',
                    color: paymentCategory === 'international' ? '#E5A93C' : '#AAA',
                  }}
                >
                  🌐 International
                </button>
              </div>

              {/* OPÉRATEURS SELON LA CATÉGORIE */}
              {paymentCategory === 'mobile_money' && (
                <div style={{ backgroundColor: 'rgba(255,255,255,0.03)', padding: 14, borderRadius: 12, border: '1px solid rgba(255,255,255,0.06)' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, marginBottom: 12 }}>
                    {[
                      { id: 'mpesa_vodacom', name: 'Vodacom M-Pesa', logo: '🔴' },
                      { id: 'orange_money', name: 'Orange Money', logo: '🟠' },
                      { id: 'airtel_money', name: 'Airtel Money', logo: '🔴' },
                      { id: 'mtn_momo', name: 'MTN MoMo', logo: '🟡' },
                    ].map((op) => (
                      <button
                        key={op.id}
                        type="button"
                        onClick={() => setSelectedProvider(op.id)}
                        style={{
                          padding: '8px 4px',
                          borderRadius: 8,
                          fontSize: 11,
                          fontWeight: 700,
                          cursor: 'pointer',
                          backgroundColor: selectedProvider === op.id ? '#E5A93C' : 'rgba(0,0,0,0.4)',
                          color: selectedProvider === op.id ? '#000' : '#DDD',
                          border: selectedProvider === op.id ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.1)',
                          textAlign: 'center',
                        }}
                      >
                        <div>{op.logo}</div>
                        <div style={{ marginTop: 2 }}>{op.name}</div>
                      </button>
                    ))}
                  </div>

                  <label style={{ display: 'block', fontSize: 12, color: '#AAA', marginBottom: 4 }}>
                    Numéro de téléphone Mobile Money :
                  </label>
                  <input
                    type="tel"
                    placeholder="+243 81 234 5678 ou +225 07..."
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      backgroundColor: '#090A0F',
                      border: '1px solid rgba(229, 169, 60, 0.4)',
                      borderRadius: 8,
                      padding: '10px 12px',
                      color: '#FFF',
                      fontSize: 14,
                      outline: 'none',
                    }}
                  />
                  <div style={{ fontSize: 11, color: '#7E8299', marginTop: 4 }}>
                    Un prompt USSD apparaîtra sur votre téléphone pour confirmer le code PIN secret.
                  </div>
                </div>
              )}

              {paymentCategory === 'card' && (
                <div style={{ backgroundColor: 'rgba(255,255,255,0.03)', padding: 14, borderRadius: 12, border: '1px solid rgba(255,255,255,0.06)' }}>
                  <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
                    {[
                      { id: 'visa', label: 'Visa & M-Pesa Visa' },
                      { id: 'mastercard', label: 'Mastercard' },
                    ].map((c) => (
                      <button
                        key={c.id}
                        type="button"
                        onClick={() => setSelectedProvider(c.id)}
                        style={{
                          flex: 1,
                          padding: '6px',
                          borderRadius: 6,
                          fontSize: 11,
                          fontWeight: 700,
                          cursor: 'pointer',
                          backgroundColor: selectedProvider === c.id ? '#E5A93C' : 'rgba(0,0,0,0.4)',
                          color: selectedProvider === c.id ? '#000' : '#DDD',
                          border: 'none',
                        }}
                      >
                        💳 {c.label}
                      </button>
                    ))}
                  </div>

                  <label style={{ display: 'block', fontSize: 12, color: '#AAA', marginBottom: 4 }}>
                    Numéro de Carte Bancaire ou Carte Virtuelle :
                  </label>
                  <input
                    type="text"
                    placeholder="4000 1234 5678 9010"
                    value={cardNumber}
                    onChange={(e) => setCardNumber(e.target.value)}
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      backgroundColor: '#090A0F',
                      border: '1px solid rgba(229, 169, 60, 0.4)',
                      borderRadius: 8,
                      padding: '10px 12px',
                      color: '#FFF',
                      fontSize: 14,
                      outline: 'none',
                      marginBottom: 8,
                    }}
                  />

                  <div style={{ display: 'flex', gap: 10 }}>
                    <div style={{ flex: 1 }}>
                      <label style={{ display: 'block', fontSize: 11, color: '#AAA', marginBottom: 4 }}>
                        Expiration (MM/AA)
                      </label>
                      <input
                        type="text"
                        placeholder="12/28"
                        value={cardExp}
                        onChange={(e) => setCardExp(e.target.value)}
                        style={{
                          width: '100%',
                          boxSizing: 'border-box',
                          backgroundColor: '#090A0F',
                          border: '1px solid rgba(255,255,255,0.1)',
                          borderRadius: 8,
                          padding: '8px 10px',
                          color: '#FFF',
                          fontSize: 13,
                        }}
                      />
                    </div>
                    <div style={{ flex: 1 }}>
                      <label style={{ display: 'block', fontSize: 11, color: '#AAA', marginBottom: 4 }}>
                        CVV / CVC
                      </label>
                      <input
                        type="password"
                        placeholder="•••"
                        maxLength={4}
                        value={cardCvv}
                        onChange={(e) => setCardCvv(e.target.value)}
                        style={{
                          width: '100%',
                          boxSizing: 'border-box',
                          backgroundColor: '#090A0F',
                          border: '1px solid rgba(255,255,255,0.1)',
                          borderRadius: 8,
                          padding: '8px 10px',
                          color: '#FFF',
                          fontSize: 13,
                        }}
                      />
                    </div>
                  </div>
                </div>
              )}

              {paymentCategory === 'international' && (
                <div style={{ backgroundColor: 'rgba(255,255,255,0.03)', padding: 14, borderRadius: 12, border: '1px solid rgba(255,255,255,0.06)', textAlign: 'center' }}>
                  <div style={{ display: 'flex', justifyContent: 'center', gap: 12, marginBottom: 12 }}>
                    {[
                      { id: 'paypal', label: 'PayPal', icon: '🅿️' },
                      { id: 'applepay', label: 'Apple Pay', icon: '🍎' },
                      { id: 'gpay', label: 'Google Pay', icon: '🇬' },
                    ].map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => setSelectedProvider(p.id)}
                        style={{
                          padding: '10px 16px',
                          borderRadius: 8,
                          fontSize: 12,
                          fontWeight: 700,
                          cursor: 'pointer',
                          backgroundColor: selectedProvider === p.id ? '#E5A93C' : 'rgba(0,0,0,0.4)',
                          color: selectedProvider === p.id ? '#000' : '#DDD',
                          border: selectedProvider === p.id ? '1px solid #E5A93C' : '1px solid rgba(255,255,255,0.1)',
                        }}
                      >
                        {p.icon} {p.label}
                      </button>
                    ))}
                  </div>
                  <p style={{ fontSize: 12, color: '#8E92A4', margin: 0 }}>
                    Vous serez redirigé vers l’interface d’authentification sécurisée {selectedProvider.toUpperCase()} avec protection biométrique (Face ID / Touch ID).
                  </p>
                </div>
              )}
            </div>

            {/* SÉCURITÉ & ARCHITECTURE CLOUD */}
            <div
              style={{
                backgroundColor: 'rgba(46, 213, 115, 0.08)',
                border: '1px solid rgba(46, 213, 115, 0.25)',
                borderRadius: 10,
                padding: '10px 14px',
                fontSize: 11,
                color: '#2ED573',
                marginBottom: 18,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <span>🔒</span>
              <span>
                <strong>Passerelle CinetPay certifiée PCI-DSS Niveau 1</strong> : Vos coordonnées bancaires et transactions sont chiffrées de bout en bout (AES-256).
              </span>
            </div>

            {/* BOUTON D'ACTION DE PAIEMENT */}
            <button
              type="button"
              disabled={isProcessing}
              onClick={handleProcessPayment}
              style={{
                width: '100%',
                backgroundColor: '#E5A93C',
                color: '#000',
                border: 'none',
                borderRadius: 12,
                padding: '14px',
                fontSize: 15,
                fontWeight: 900,
                cursor: isProcessing ? 'not-allowed' : 'pointer',
                opacity: isProcessing ? 0.7 : 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8,
                boxShadow: '0 4px 15px rgba(229, 169, 60, 0.4)',
              }}
            >
              {isProcessing ? (
                <>
                  <span style={{ display: 'inline-block', animation: 'spin 1s linear infinite' }}>⏳</span>
                  <span>Validation Sécurisée CinetPay...</span>
                </>
              ) : (
                <>
                  <span>Payer ${selectedPack.priceUsd}.00 ({selectedPack.priceFc.toLocaleString()} FC)</span>
                  <span>➔</span>
                  <span style={{ color: '#000', backgroundColor: '#FFF', padding: '2px 8px', borderRadius: 999, fontSize: 12 }}>
                    +{totalCoinsToReceive} Pièces
                  </span>
                </>
              )}
            </button>
          </>
        )}
      </div>
    </div>
  );
};
