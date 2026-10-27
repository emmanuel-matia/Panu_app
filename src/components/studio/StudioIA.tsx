import React, { useState, useEffect } from 'react';
import { supabase } from '../../lib/supabaseClient';
import { useNotifications } from '../../context/NotificationContext';

export interface StudioIAProps {
  currentUser?: { id: string; email?: string } | null;
}

export default function StudioIA({ currentUser }: StudioIAProps) {
  // Auto-load from localStorage on initial mount
  const [prompt, setPrompt] = useState(() => {
    try {
      const saved = localStorage.getItem('panu_studio_draft');
      if (saved) {
        const parsed = JSON.parse(saved);
        return parsed.prompt || '';
      }
    } catch (_) {}
    return '';
  });

  const [mediaType, setMediaType] = useState<'video' | 'image'>(() => {
    try {
      const saved = localStorage.getItem('panu_studio_draft');
      if (saved) {
        const parsed = JSON.parse(saved);
        return parsed.mediaType || 'video';
      }
    } catch (_) {}
    return 'video';
  });

  const [stylePreset, setStylePreset] = useState(() => {
    try {
      const saved = localStorage.getItem('panu_studio_draft');
      if (saved) {
        const parsed = JSON.parse(saved);
        return parsed.stylePreset || 'Cinématique 8K';
      }
    } catch (_) {}
    return 'Cinématique 8K';
  });

  const [loading, setLoading] = useState(false);
  const [generatedMedia, setGeneratedMedia] = useState<string | null>(null);
  const [published, setPublished] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [resolvedUser, setResolvedUser] = useState<{ id: string; email?: string } | null>(currentUser || null);
  const { showToast } = useNotifications();

  // Auto-save to localStorage whenever prompt, mediaType, or stylePreset changes
  useEffect(() => {
    try {
      const draft = JSON.stringify({ prompt, mediaType, stylePreset });
      localStorage.setItem('panu_studio_draft', draft);
    } catch (e) {
      console.error('Failed to auto-save studio draft', e);
    }
  }, [prompt, mediaType, stylePreset]);

  useEffect(() => {
    if (currentUser) {
      setResolvedUser(currentUser);
    } else {
      supabase.auth.getSession().then(({ data: { session } }) => {
        if (session?.user) {
          setResolvedUser({ id: session.user.id, email: session.user.email });
        }
      });
    }
  }, [currentUser]);

  const handleGenerate = async () => {
    if (!prompt.trim()) return alert('Veuillez saisir un prompt.');
    const activeUser = resolvedUser || currentUser;
    if (!activeUser?.id) {
      showToast('Veuillez vous connecter pour utiliser le Studio IA', 'error');
      return;
    }

    setLoading(true);
    setGeneratedMedia(null);
    setPublished(false);

    try {
      const CREDIT_COST = mediaType === 'video' ? 25 : 5;

      const { data: creditData, error: creditError } = await supabase.rpc('consume_ai_credits', {
        p_user_id: activeUser.id,
        p_amount: CREDIT_COST
      });

      if (creditError || !creditData?.[0]?.success) {
        showToast(`Solde insuffisant ! Nécessite ${CREDIT_COST} crédits.`, 'error');
        setLoading(false);
        return;
      }

      const falKey =
        (typeof process !== 'undefined' && process?.env?.REACT_APP_FAL_KEY) ||
        (typeof process !== 'undefined' && process?.env?.FAL_KEY) ||
        (typeof import.meta !== 'undefined' && (import.meta as any)?.env?.VITE_FAL_KEY);

      let mediaUrl: string | null = null;

      if (falKey) {
        const endpoint = mediaType === 'video'
          ? 'https://fal.run/fal-ai/fast-svd'
          : 'https://fal.run/fal-ai/flux/schnell';

        const payload = mediaType === 'video'
          ? { prompt: `${stylePreset}: ${prompt}`, motion_bucket_id: 127, fps: 24 }
          : { prompt: `${stylePreset}: ${prompt}`, image_size: 'portrait_16_9', num_inference_steps: 4 };

        const response = await fetch(endpoint, {
          method: 'POST',
          headers: {
            Authorization: `Key ${falKey}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(payload),
        });

        if (response.ok) {
          const data = await response.json();
          if (mediaType === 'video' && data?.video?.url) {
            mediaUrl = data.video.url;
          } else if (mediaType === 'image' && data?.images?.[0]?.url) {
            mediaUrl = data.images[0].url;
          }
        } else {
          throw new Error('Erreur de génération via FAL.ai');
        }
      } else {
        showToast('Clé API FAL non configurée. Génération IA indisponible pour le moment.', 'error');
        setLoading(false);
        return;
      }

      if (!mediaUrl) {
        throw new Error('La génération a échoué (URL vide)');
      }

      setGeneratedMedia(mediaUrl);
      showToast(`Génération réussie (-${CREDIT_COST} crédits). Cliquez sur "Publier sur PANU" pour partager.`, 'success');
    } catch (err: any) {
      console.error(err);
      showToast(err?.message || 'Erreur lors de la génération IA.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handlePublish = async () => {
    const activeUser = resolvedUser || currentUser;
    if (!activeUser?.id || !generatedMedia) return;

    setPublishing(true);
    try {
      const { error } = await supabase.from('posts').insert([
        {
          user_id: activeUser.id,
          title: prompt.substring(0, 80),
          content: prompt,
          media_url: generatedMedia,
          media_type: mediaType,
          prompt_used: prompt,
          is_public: true,
          status: 'published',
        },
      ]);

      if (error) {
        showToast(`Erreur de publication : ${error.message}`, 'error');
      } else {
        setPublished(true);
        showToast('Publication mise en ligne sur PANU avec succès !', 'success');
        // Clear draft after successful publish
        localStorage.removeItem('panu_studio_draft');
      }
    } catch (err: any) {
      showToast(err?.message || 'Erreur lors de la publication.', 'error');
    } finally {
      setPublishing(false);
    }
  };

  const handleClearDraft = () => {
    setPrompt('');
    setMediaType('video');
    setStylePreset('Cinématique 8K');
    localStorage.removeItem('panu_studio_draft');
    showToast('Brouillon effacé.', 'success');
  };

  return (
    <div
      style={{
        maxWidth: '480px',
        margin: '0 auto',
        padding: '24px',
        background: '#151622',
        color: '#FFF',
        borderRadius: '16px',
        border: '1px solid rgba(229, 169, 60, 0.3)',
        boxShadow: '0 10px 40px rgba(0,0,0,0.6)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 900, color: '#E5A93C' }}>
          Studio IA - Génération Vidéo & Image
        </h2>
        <button
          type="button"
          onClick={handleClearDraft}
          style={{
            background: 'transparent',
            border: '1px solid #2E3248',
            color: '#AAA',
            fontSize: '11px',
            padding: '4px 8px',
            borderRadius: '6px',
            cursor: 'pointer',
          }}
          title="Effacer le brouillon auto-sauvegardé"
        >
          Effacer brouillon
        </button>
      </div>

      <label style={{ fontSize: 13, fontWeight: 700, color: '#DDD', display: 'block', marginBottom: 6 }}>
        1. Type de contenu :
      </label>
      <div style={{ display: 'flex', gap: 10, marginBottom: 16 }}>
        <button
          type="button"
          onClick={() => setMediaType('video')}
          style={{
            flex: 1, padding: '10px', borderRadius: 10, fontWeight: 800, fontSize: 13,
            border: mediaType === 'video' ? '1px solid #E5A93C' : '1px solid #2E3248',
            backgroundColor: mediaType === 'video' ? 'rgba(229, 169, 60, 0.2)' : '#0D0E14',
            color: mediaType === 'video' ? '#E5A93C' : '#AAA', cursor: 'pointer',
          }}
        >
          🎬 Vidéo IA
        </button>
        <button
          type="button"
          onClick={() => setMediaType('image')}
          style={{
            flex: 1, padding: '10px', borderRadius: 10, fontWeight: 800, fontSize: 13,
            border: mediaType === 'image' ? '1px solid #E5A93C' : '1px solid #2E3248',
            backgroundColor: mediaType === 'image' ? 'rgba(229, 169, 60, 0.2)' : '#0D0E14',
            color: mediaType === 'image' ? '#E5A93C' : '#AAA', cursor: 'pointer',
          }}
        >
          🖼️ Image IA
        </button>
      </div>

      <label style={{ fontSize: 13, fontWeight: 700, color: '#DDD' }}>2. Style artistique :</label>
      <select
        value={stylePreset}
        onChange={(e) => setStylePreset(e.target.value)}
        style={{ ...inputStyle, backgroundColor: '#0D0E14', color: '#FFF', borderColor: '#2E3248' }}
      >
        <option>Cinématique 8K</option>
        <option>Décor 3D Animation</option>
        <option>Anime Studio</option>
        <option>Photo Réaliste Ultra HD</option>
        <option>Cyberpunk Néon</option>
      </select>

      <label style={{ fontSize: 13, fontWeight: 700, color: '#DDD' }}>3. Descriptif (Prompt) :</label>
      <textarea
        value={prompt}
        onChange={(e) => setPrompt(e.target.value)}
        placeholder={
          mediaType === 'video'
            ? 'Ex: Métropole Africaine 8K au Coucher du Sol, vue par drone...'
            : "Ex: Portrait futuriste d'un guerrier masaï en armure néon 8K..."
        }
        style={{
          ...inputStyle, height: '90px', backgroundColor: '#0D0E14',
          color: '#FFF', borderColor: '#2E3248', resize: 'vertical',
        }}
      />

      <button
        type="button"
        onClick={handleGenerate}
        disabled={loading}
        style={{
          width: '100%', padding: '14px',
          backgroundColor: loading ? '#555' : '#E5A93C',
          color: '#000', border: 'none', borderRadius: '10px',
          fontWeight: 900, fontSize: 14,
          cursor: loading ? 'not-allowed' : 'pointer',
          boxShadow: '0 4px 15px rgba(229, 169, 60, 0.3)',
          transition: 'all 0.2s ease',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        }}
      >
        {loading && (
          <span style={{ width: 16, height: 16, border: '2px solid #000', borderTop: '2px solid transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
        )}
        {loading ? 'Génération en cours...' : mediaType === 'video' ? '✨ Générer la Vidéo IA' : '✨ Générer l\'Image IA'}
      </button>

      {/* Inline Error Display */}
      {/* Note: I'll maintain errors in local state or show as toast, keeping it simple as requested */}
      <style>{`
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
      `}</style>


      {generatedMedia && (
        <div style={{ marginTop: '20px' }}>
          <h4 style={{ margin: '0 0 8px', color: '#E5A93C' }}>Résultat Généré :</h4>
          {mediaType === 'video' ? (
            <video
              src={generatedMedia}
              controls
              autoPlay
              loop
              style={{ width: '100%', borderRadius: '10px', border: '1px solid #2E3248' }}
            />
          ) : (
            <img
              src={generatedMedia}
              alt="Génération AI"
              style={{ width: '100%', borderRadius: '10px', border: '1px solid #2E3248', objectFit: 'cover' }}
            />
          )}

          {!published ? (
            <button
              type="button"
              onClick={handlePublish}
              disabled={publishing}
              style={{
                width: '100%', marginTop: 14, padding: '14px',
                backgroundColor: publishing ? '#555' : '#2ED573',
                color: '#000', border: 'none', borderRadius: '10px',
                fontWeight: 900, fontSize: 14,
                cursor: publishing ? 'not-allowed' : 'pointer',
                boxShadow: '0 4px 15px rgba(46, 213, 115, 0.3)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
              }}
            >
              {publishing ? 'Publication en cours...' : '🚀 Publier sur PANU'}
            </button>
          ) : (
            <div style={{ marginTop: 14, backgroundColor: 'rgba(46,213,115,0.15)', border: '1px solid #2ED573', borderRadius: 10, padding: '12px', textAlign: 'center', color: '#2ED573', fontWeight: 800, fontSize: 13 }}>
              ✅ Publié sur PANU ! Visible dans le fil d'accueil.
            </div>
          )}
        </div>
      )}
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '10px 12px',
  margin: '8px 0 16px 0',
  borderRadius: '8px',
  border: '1px solid #ccc',
  fontSize: '14px',
  boxSizing: 'border-box',
};

export { StudioIA };
