import React, { useState } from 'react';
import { GiftAnimationData } from './FullScreenGiftAnimation';

export interface VirtualGiftItem {
  id: string;
  name: string;
  icon: string;
  coins: number;
  category: 'small' | 'super' | 'special';
  categoryLabel: string;
  animationType: 'rose_shower' | 'rocket_launch' | 'crown_descent' | 'lion_roar' | 'fireworks_burst' | 'heart_explosion';
  description: string;
}

export const VIRTUAL_GIFTS_CATALOG: VirtualGiftItem[] = [
  // 1. PETITES ATTENTIONS (10 À 50 PIÈCES)
  {
    id: 'gift_rose',
    name: 'Rose d’Or',
    icon: '🌹',
    coins: 10,
    category: 'small',
    categoryLabel: 'Petites Attentions',
    animationType: 'rose_shower',
    description: 'Une pluie délicate de roses dorées sur l’écran.',
  },
  {
    id: 'gift_heart',
    name: 'Cœur Vibrant',
    icon: '❤️',
    coins: 25,
    category: 'small',
    categoryLabel: 'Petites Attentions',
    animationType: 'heart_explosion',
    description: 'Vague d’amour et pulsation rouge lumineuse.',
  },
  {
    id: 'gift_coffee',
    name: 'Café du Matin',
    icon: '☕',
    coins: 30,
    category: 'small',
    categoryLabel: 'Petites Attentions',
    animationType: 'rose_shower',
    description: 'Pour booster l’énergie du créateur pendant son direct.',
  },
  {
    id: 'gift_star',
    name: 'Étoile Africaine',
    icon: '⭐',
    coins: 50,
    category: 'small',
    categoryLabel: 'Petites Attentions',
    animationType: 'rose_shower',
    description: 'Éclat doré scintillant d’encouragement.',
  },

  // 2. SUPER CADEAUX (500 À 5 000 PIÈCES)
  {
    id: 'gift_crown',
    name: 'Couronne Impériale',
    icon: '👑',
    coins: 500,
    category: 'super',
    categoryLabel: 'Super Cadeaux',
    animationType: 'crown_descent',
    description: 'Descente royale de la couronne PANU en plein écran.',
  },
  {
    id: 'gift_lion',
    name: 'Lion Majestueux',
    icon: '🦁',
    coins: 1000,
    category: 'super',
    categoryLabel: 'Super Cadeaux',
    animationType: 'lion_roar',
    description: 'Le rugissement puissant du roi des animaux d’Afrique !',
  },
  {
    id: 'gift_supercar',
    name: 'Voiture de Sport',
    icon: '🏎️',
    coins: 2500,
    category: 'super',
    categoryLabel: 'Super Cadeaux',
    animationType: 'fireworks_burst',
    description: 'Dérapage néon et gerbes d’étincelles spectaculaires.',
  },
  {
    id: 'gift_rocket',
    name: 'Fusée Interstellaire',
    icon: '🚀',
    coins: 5000,
    category: 'super',
    categoryLabel: 'Super Cadeaux',
    animationType: 'rocket_launch',
    description: 'Décollage cosmique avec traînée de feu et vibration !',
  },
];

interface VirtualGiftDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  userBalance: number;
  recipientName: string;
  onSendGift: (gift: VirtualGiftItem, animData: GiftAnimationData) => void;
  onOpenRecharge: () => void;
}

export const VirtualGiftDrawer: React.FC<VirtualGiftDrawerProps> = ({
  isOpen,
  onClose,
  userBalance,
  recipientName,
  onSendGift,
  onOpenRecharge,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<'all' | 'small' | 'super'>('all');
  const [selectedGiftId, setSelectedGiftId] = useState<string>(VIRTUAL_GIFTS_CATALOG[0].id);
  const [sendCount, setSendCount] = useState<number>(1);
  const [isSending, setIsSending] = useState(false);

  if (!isOpen) return null;

  const filteredGifts = VIRTUAL_GIFTS_CATALOG.filter((g) => {
    if (selectedCategory === 'all') return true;
    return g.category === selectedCategory;
  });

  const activeGift = VIRTUAL_GIFTS_CATALOG.find((g) => g.id === selectedGiftId) || VIRTUAL_GIFTS_CATALOG[0];
  const totalCost = activeGift.coins * sendCount;
  const hasEnoughBalance = userBalance >= totalCost;

  // Calcul transparent prix en Dollars ($) et Franc Congolais (FC, ~2800 FC / $)
  const usdPrice = (totalCost * 0.01).toFixed(2);
  const fcPrice = Math.round(totalCost * 28).toLocaleString();

  const handleSend = () => {
    if (!hasEnoughBalance) {
      onOpenRecharge();
      return;
    }
    setIsSending(true);
    setTimeout(() => {
      onSendGift(activeGift, {
        giftType: activeGift.name,
        icon: activeGift.icon,
        animationType: activeGift.animationType,
        senderName: 'Vous',
      });
      setIsSending(false);
      onClose();
    }, 400);
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(5, 6, 12, 0.75)',
        backdropFilter: 'blur(5px)',
        zIndex: 9980,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'flex-end',
      }}
      onClick={onClose}
    >
      <div
        style={{
          backgroundColor: '#12141F',
          borderTop: '2px solid rgba(229, 169, 60, 0.45)',
          borderTopLeftRadius: 24,
          borderTopRightRadius: 24,
          padding: '16px 20px 24px',
          maxHeight: '80vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 -10px 40px rgba(0, 0, 0, 0.8)',
          color: '#FFF',
          animation: 'slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <style>{`
          @keyframes slideUp {
            from { transform: translateY(100%); }
            to { transform: translateY(0); }
          }
        `}</style>

        {/* POIGNÉE DE GLISSEMENT & EN-TÊTE */}
        <div style={{ width: 44, height: 4, borderRadius: 2, backgroundColor: 'rgba(255,255,255,0.2)', margin: '0 auto 12px' }} />

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ fontSize: 20 }}>🎁</span>
              <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900 }}>Boutique de Cadeaux Virtuels</h3>
            </div>
            <p style={{ margin: 0, fontSize: 12, color: '#8E92A4' }}>
              Envoyer à : <strong style={{ color: '#E5A93C' }}>{recipientName}</strong>
            </p>
          </div>

          {/* SOLDE ET BOUTON RECHARGER RAPIDE */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div
              style={{
                backgroundColor: 'rgba(229, 169, 60, 0.15)',
                border: '1px solid rgba(229, 169, 60, 0.35)',
                borderRadius: 999,
                padding: '4px 10px',
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              <span style={{ fontSize: 14 }}>🪙</span>
              <span style={{ fontWeight: 900, color: '#FFD700', fontSize: 13 }}>
                {userBalance.toLocaleString()}
              </span>
            </div>
            <button
              type="button"
              onClick={onOpenRecharge}
              style={{
                backgroundColor: '#E5A93C',
                color: '#000',
                border: 'none',
                borderRadius: 999,
                padding: '5px 12px',
                fontSize: 12,
                fontWeight: 900,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 4,
              }}
            >
              <span>+</span>
              <span>Recharger</span>
            </button>
          </div>
        </div>

        {/* ONGLETS DE FILTRAGE PAR CATÉGORIE */}
        <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
          {[
            { key: 'all', label: '✨ Tous les Cadeaux' },
            { key: 'small', label: '🌸 Petites Attentions (10-50 🪙)' },
            { key: 'super', label: '👑 Super Cadeaux (500-5000 🪙)' },
          ].map((cat) => (
            <button
              key={cat.key}
              type="button"
              onClick={() => setSelectedCategory(cat.key as any)}
              style={{
                padding: '6px 12px',
                borderRadius: 8,
                fontSize: 12,
                fontWeight: 700,
                cursor: 'pointer',
                backgroundColor: selectedCategory === cat.key ? 'rgba(229, 169, 60, 0.2)' : 'rgba(255, 255, 255, 0.05)',
                color: selectedCategory === cat.key ? '#E5A93C' : '#AAA',
                border: selectedCategory === cat.key ? '1px solid #E5A93C' : '1px solid transparent',
              }}
            >
              {cat.label}
            </button>
          ))}
        </div>

        {/* GRILLE DES CADEAUX TIKTOK STYLE */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(4, 1fr)',
            gap: 10,
            overflowY: 'auto',
            maxHeight: 260,
            padding: '4px 2px',
            marginBottom: 16,
          }}
        >
          {filteredGifts.map((gift) => {
            const isSelected = selectedGiftId === gift.id;
            return (
              <div
                key={gift.id}
                onClick={() => setSelectedGiftId(gift.id)}
                style={{
                  backgroundColor: isSelected ? 'rgba(229, 169, 60, 0.2)' : 'rgba(255, 255, 255, 0.03)',
                  border: isSelected ? '2px solid #E5A93C' : '1px solid rgba(255, 255, 255, 0.08)',
                  borderRadius: 14,
                  padding: '12px 6px',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  textAlign: 'center',
                  cursor: 'pointer',
                  position: 'relative',
                  transition: 'all 0.15s ease',
                  transform: isSelected ? 'scale(1.03)' : 'scale(1)',
                }}
              >
                <div style={{ fontSize: 36, marginBottom: 4, filter: isSelected ? 'drop-shadow(0 0 10px #FFD700)' : 'none' }}>
                  {gift.icon}
                </div>
                <div style={{ fontSize: 12, fontWeight: 800, color: '#FFF', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100%' }}>
                  {gift.name}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 3, marginTop: 4, fontSize: 11, fontWeight: 900, color: '#FFD700' }}>
                  <span>🪙</span>
                  <span>{gift.coins}</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* DÉTAILS DU CADEAU SÉLECTIONNÉ & TRANSPARENCE DES PRIX */}
        <div
          style={{
            backgroundColor: 'rgba(255, 255, 255, 0.03)',
            borderRadius: 12,
            padding: '10px 14px',
            marginBottom: 14,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            border: '1px solid rgba(255, 255, 255, 0.06)',
          }}
        >
          <div>
            <div style={{ fontSize: 12, color: '#8E92A4' }}>
              Effet visuel spécial : <strong style={{ color: '#2ED573' }}>Plein Écran Animé</strong>
            </div>
            <div style={{ fontSize: 11, color: '#DDD', marginTop: 2 }}>
              {activeGift.description}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 13, fontWeight: 900, color: '#FFD700' }}>
              {totalCost} Pièces
            </div>
            <div style={{ fontSize: 11, color: '#8E92A4' }}>
              ~${usdPrice} USD ({fcPrice} FC)
            </div>
          </div>
        </div>

        {/* MULTIPLICATEUR DE CADEAUX & BOUTON D'ENVOI */}
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          {/* SÉLECTEUR DE QUANTITÉ (x1, x5, x10) */}
          <div style={{ display: 'flex', backgroundColor: 'rgba(255,255,255,0.06)', borderRadius: 10, padding: 3 }}>
            {[1, 5, 10].map((qty) => (
              <button
                key={qty}
                type="button"
                onClick={() => setSendCount(qty)}
                style={{
                  border: 'none',
                  backgroundColor: sendCount === qty ? '#E5A93C' : 'transparent',
                  color: sendCount === qty ? '#000' : '#AAA',
                  borderRadius: 8,
                  padding: '6px 10px',
                  fontWeight: 800,
                  fontSize: 12,
                  cursor: 'pointer',
                }}
              >
                x{qty}
              </button>
            ))}
          </div>

          {/* BOUTON D'ACTION D'ENVOI */}
          <button
            type="button"
            disabled={isSending}
            onClick={handleSend}
            style={{
              flex: 1,
              backgroundColor: hasEnoughBalance ? '#E5A93C' : '#3A3F50',
              color: hasEnoughBalance ? '#000' : '#FFF',
              border: 'none',
              borderRadius: 12,
              padding: '12px',
              fontSize: 14,
              fontWeight: 900,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              boxShadow: hasEnoughBalance ? '0 4px 15px rgba(229, 169, 60, 0.4)' : 'none',
            }}
          >
            {hasEnoughBalance ? (
              <>
                <span>Envoyer {activeGift.icon}</span>
                <span>•</span>
                <span>{totalCost} Pièces</span>
              </>
            ) : (
              <>
                <span>🪙 Solde insuffisant (Recharger)</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
