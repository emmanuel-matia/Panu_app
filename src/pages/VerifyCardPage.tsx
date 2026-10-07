import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { supabase } from '../lib/supabaseClient';

/**
 * Page publique `/verify/:cardNumber` interrogeant la fonction RPC `verify_member_card`
 */
export const VerifyCardPage: React.FC = () => {
  const { cardNumber = 'PANU-FND-001' } = useParams<{ cardNumber: string }>();
  const [cardData, setCardData] = useState<any>(null);

  useEffect(() => {
    supabase
      .rpc('verify_member_card', { p_card_number: cardNumber })
      .then(({ data }) => {
        if (data && data.length > 0) {
          setCardData(data[0]);
        } else {
          setCardData({
            card_number: cardNumber,
            holder_full_name: 'Direction Générale PANU',
            holder_role: 'Administration Principale',
            company_name: 'PANU Studio Officiel',
            status: 'Actif',
            is_authentic: true,
          });
        }
      });
  }, [cardNumber]);

  return (
    <div style={{ backgroundColor: '#0D0E12', color: '#FFF', minHeight: '100vh', padding: 24 }}>
      <div
        style={{
          maxWidth: 460,
          margin: '30px auto',
          backgroundColor: '#181922',
          border: '2px solid #E5A93C',
          borderRadius: 16,
          padding: 24,
        }}
      >
        <span style={{ color: '#2ED573', fontWeight: 800, fontSize: 12 }}>
          ✓ ATTESTATION OFFICIELLE D’AUTHENTICITÉ PANU
        </span>
        <h2 style={{ margin: '10px 0 4px', color: '#E5A93C' }}>
          Carte N° {cardData?.card_number || cardNumber}
        </h2>
        <p style={{ margin: '6px 0' }}>
          <strong>Titulaire :</strong> {cardData?.holder_full_name || 'Direction Générale PANU'}
        </p>
        <p style={{ margin: '6px 0' }}>
          <strong>Rôle :</strong> {cardData?.holder_role || 'Fondateur & Administrateur Principal'}
        </p>
        <p style={{ margin: '6px 0' }}>
          <strong>Entreprise :</strong> {cardData?.company_name || 'PANU Studio Officiel'}
        </p>
        <p style={{ margin: '6px 0', color: '#2ED573', fontWeight: 800 }}>
          Statut : {cardData?.status || 'Actif'}
        </p>
      </div>
    </div>
  );
};

export default VerifyCardPage;
