import React, { useState } from 'react';
import { ShareButtonWithOpenGraph } from '../components/share/ShareButtonWithOpenGraph';
import { PanuTopNavbar } from '../components/nav/PanuTopNavbar';
import { DynamicTemplateGallery } from '../components/studio/DynamicTemplateGallery';

export const VodPage: React.FC = () => {
  const [showTemplatesModal, setShowTemplatesModal] = useState(false);

  return (
    <div style={{ color: '#FFF', minHeight: '100vh' }}>
      <div style={{ maxWidth: 1000, margin: '0 auto', padding: '24px 20px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 20 }}>
          <div>
            <h1 style={{ color: '#E5A93C', margin: '0 0 6px', fontSize: 22 }}>
              🎬 Catalogue VOD & Séries Africaines 8K
            </h1>
            <p style={{ margin: 0, color: '#AAA', fontSize: 13 }}>
              Plateforme officielle de documentaires, films et séries originales créées avec le studio IA PANU.
            </p>
          </div>

          <ShareButtonWithOpenGraph
            contentType="watch"
            contentId="vod_afro_metropolis_8k"
            title="Séries & Cinéma PANU"
            description="Catalogue VOD et séries originales sur PANU Studio"
          />
        </div>

        <div style={{ backgroundColor: '#151622', border: '1px solid rgba(229,169,60,0.3)', borderRadius: 16, padding: 32, textAlign: 'center' }}>
          <div style={{ fontSize: 44, marginBottom: 12 }}>🎥</div>
          <h3 style={{ color: '#E5A93C', margin: '0 0 8px', fontSize: 18 }}>
            Prêt pour les Nouvelles Sorties VOD Réelles
          </h3>
          <p style={{ color: '#BBB', fontSize: 13, maxWidth: 500, margin: '0 auto 20px', lineHeight: 1.5 }}>
            Zéro contenu factice. Les créateurs et studios partenaires publient directement leurs séries et documentaires ici via le Studio IA PANU.
          </p>
          <button
            type="button"
            onClick={() => setShowTemplatesModal(true)}
            style={{
              backgroundColor: '#E5A93C',
              color: '#000',
              border: 'none',
              padding: '10px 20px',
              borderRadius: 8,
              fontWeight: 800,
              fontSize: 13,
              cursor: 'pointer',
            }}
          >
            🎬 Créer une Bande-Annonce avec les Templates IA
          </button>
        </div>
      </div>

      {showTemplatesModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.85)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: 16,
          }}
        >
          <div
            style={{
              backgroundColor: '#161722',
              border: '1px solid #E5A93C',
              borderRadius: 16,
              maxWidth: 900,
              width: '100%',
              maxHeight: '90vh',
              overflowY: 'auto',
              padding: 24,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <h2 style={{ margin: 0, color: '#E5A93C', fontSize: 20 }}>🎬 Galerie des Templates PANU</h2>
              <button
                type="button"
                onClick={() => setShowTemplatesModal(false)}
                style={{ backgroundColor: 'transparent', border: '1px solid #444', color: '#FFF', padding: '6px 12px', borderRadius: 8, cursor: 'pointer' }}
              >
                ✕ Fermer
              </button>
            </div>
            <DynamicTemplateGallery onTemplateSelect={() => setShowTemplatesModal(false)} />
          </div>
        </div>
      )}
    </div>
  );
};

export default VodPage;
