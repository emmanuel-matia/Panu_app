import React, { useState } from 'react';

export interface ShareButtonProps {
  /** Type de ressource à partager ('watch' pour une vidéo, 'live' pour un direct) */
  contentType: 'watch' | 'live';
  contentId: string;
  title: string;
  description?: string;
  className?: string;
}

/**
 * Bouton de partage multiplateforme raccordé à `navigator.share()` (Web Share API)
 * avec fallback modal automatique (WhatsApp, Facebook, Telegram + Copie du lien Open Graph).
 */
export const ShareButtonWithOpenGraph: React.FC<ShareButtonProps> = ({
  contentType,
  contentId,
  title,
  description = 'Regardez ce contenu exclusif sur PANU Studio !',
  className,
}) => {
  const [showFallbackModal, setShowFallbackModal] = useState(false);
  const [copied, setCopied] = useState(false);

  // URL canonique servie avec les balises Open Graph dynamiques (/watch/[id] ou /live/[id])
  const origin = typeof window !== 'undefined' ? window.location.origin : 'https://panu.app';
  const shareUrl = `${origin}/${contentType}/${encodeURIComponent(contentId)}`;

  const handleShare = async () => {
    const shareData: ShareData = {
      title: `${title} | PANU`,
      text: description,
      url: shareUrl,
    };

    // 1. Utilisation native de la Web Share API si supportée par le navigateur mobile/PWA
    if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
      try {
        if (!navigator.canShare || navigator.canShare(shareData)) {
          await navigator.share(shareData);
          return;
        }
      } catch (err: any) {
        // Si l'utilisateur annule volontairement le tiroir natif, ne pas ouvrir le fallback
        if (err?.name === 'AbortError') return;
      }
    }

    // 2. Fallback : Ouverture de la modal multiplateforme (WhatsApp, Facebook, Telegram, Copier)
    setShowFallbackModal(true);
  };

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      // Fallback ancien navigateur
      const input = document.createElement('input');
      input.value = shareUrl;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      setCopied(true);
    }
  };

  const encodedUrl = encodeURIComponent(shareUrl);
  const encodedText = encodeURIComponent(`${title} — ${description}`);

  return (
    <>
      <button
        type="button"
        onClick={handleShare}
        className={className}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 8,
          backgroundColor: 'rgba(229, 169, 60, 0.16)',
          border: '1px solid #E5A93C',
          color: '#E5A93C',
          padding: '8px 14px',
          borderRadius: 999,
          fontWeight: 700,
          fontSize: 13,
          cursor: 'pointer',
        }}
      >
        <span>🔗 Partager</span>
      </button>

      {showFallbackModal && (
        <div
          onClick={() => setShowFallbackModal(false)}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            padding: 16,
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              backgroundColor: '#181920',
              border: '1px solid rgba(229, 169, 60, 0.45)',
              borderRadius: 16,
              padding: 20,
              width: '100%',
              maxWidth: 380,
              color: '#FFF',
              display: 'flex',
              flexDirection: 'column',
              gap: 14,
            }}
          >
            <h3 style={{ margin: 0, fontSize: 16, color: '#E5A93C' }}>
              Partager « {title} »
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
              <a
                href={`https://wa.me/?text=${encodedText}%20${encodedUrl}`}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  backgroundColor: '#25D366',
                  color: '#000',
                  textAlign: 'center',
                  padding: '10px 8px',
                  borderRadius: 10,
                  textDecoration: 'none',
                  fontWeight: 800,
                  fontSize: 12,
                }}
              >
                WhatsApp
              </a>
              <a
                href={`https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}`}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  backgroundColor: '#1877F2',
                  color: '#FFF',
                  textAlign: 'center',
                  padding: '10px 8px',
                  borderRadius: 10,
                  textDecoration: 'none',
                  fontWeight: 800,
                  fontSize: 12,
                }}
              >
                Facebook
              </a>
              <a
                href={`https://t.me/share/url?url=${encodedUrl}&text=${encodedText}`}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  backgroundColor: '#229ED9',
                  color: '#FFF',
                  textAlign: 'center',
                  padding: '10px 8px',
                  borderRadius: 10,
                  textDecoration: 'none',
                  fontWeight: 800,
                  fontSize: 12,
                }}
              >
                Telegram
              </a>
            </div>

            <div style={{ display: 'flex', gap: 8 }}>
              <input
                type="text"
                readOnly
                value={shareUrl}
                style={{
                  flex: 1,
                  backgroundColor: '#0D0E12',
                  border: '1px solid rgba(255,255,255,0.2)',
                  borderRadius: 8,
                  padding: '8px 10px',
                  color: '#DDD',
                  fontSize: 12,
                }}
              />
              <button
                type="button"
                onClick={handleCopyLink}
                style={{
                  backgroundColor: '#E5A93C',
                  color: '#000',
                  border: 'none',
                  borderRadius: 8,
                  padding: '8px 12px',
                  fontWeight: 800,
                  fontSize: 12,
                  cursor: 'pointer',
                }}
              >
                {copied ? 'Copié ✓' : 'Copier'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
