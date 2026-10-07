import React, { useState, useEffect } from 'react';
import { supabase } from '../../supabaseClient';
import { useNotifications } from '../../context/NotificationContext';

export interface StudioIAProps {
  currentUser?: { id: string; email?: string } | null;
}

export default function StudioIA({ currentUser }: StudioIAProps) {
  const [prompt, setPrompt] = useState('');
  const [mediaType, setMediaType] = useState<'video' | 'image'>('video');
  const [stylePreset, setStylePreset] = useState('Cinématique 8K');
  const [loading, setLoading] = useState(false);
  const [generatedMedia, setGeneratedMedia] = useState<string | null>(null);
  const [resolvedUser, setResolvedUser] = useState<{ id: string; email?: string } | null>(currentUser || null);
  const { showToast } = useNotifications();

  useEffect(() => {
    if (currentUser) {
      setResolvedUser(currentUser);
    } else {
      supabase.auth.getSession().then(({ data: { session } }) => {
        if (session?.user) {
          setResolvedUser({ id: session.user.id, email: session.user.email });
        } else {
          const guestId = localStorage.getItem('panu_guest_uuid') || '11111111-1111-4111-8111-000000000001';
          setResolvedUser({ id: guestId });
        }
      });
    }
  }, [currentUser]);

  const handleGenerateAndPublish = async () => {
    if (!prompt.trim()) return alert('Veuillez saisir un prompt.');
    const activeUser = resolvedUser || currentUser;
    if (!activeUser?.id) return alert('Veuillez vous connecter.');

    setLoading(true);

    try {
      const activeUser = resolvedUser || currentUser;
      if (!activeUser?.id) {
        showToast('Veuillez vous connecter pour utiliser le Studio IA', 'error');
        return;
      }

      // 0. Vérification et déduction des crédits (5 crédits pour Image, 25 pour Vidéo)
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

      const endpoint = mediaType === 'video'
        ? 'https://fal.run/fal-ai/fast-svd'
        : 'https://fal.run/fal-ai/flux/schnell';

      const payload = mediaType === 'video'
        ? { prompt: `${stylePreset}: ${prompt}`, motion_bucket_id: 127, fps: 24 }
        : { prompt: `${stylePreset}: ${prompt}`, image_size: 'portrait_16_9', num_inference_steps: 4 };

      if (falKey) {
        try {
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
            const errorText = await response.text();
            console.error('FAL API Error:', errorText);
            throw new Error('Erreur de génération via FAL.ai');
          }
        } catch (apiErr) {
          console.error('Fal.ai API Error:', apiErr);
          throw apiErr;
        }
      } else {
        throw new Error('Clé API FAL non configurée');
      }

      if (!mediaUrl) {
        throw new Error('La génération a échoué (URL vide)');
      }

      setGeneratedMedia(mediaUrl);

      // 2. Publication automatique dans Supabase `posts`
      const { error } = await supabase.from('posts').insert([
        {
          user_id: activeUser.id,
          title: prompt.substring(0, 50),
          media_url: mediaUrl,
          media_type: mediaType,
          prompt_used: prompt,
          is_public: true,
          status: 'published',
          author_name: 'Créateur PANU', // Valeur par défaut si non résolu
        },
      ]);

      alert(
        mediaType === 'video'
          ? '🎬 Vidéo générée et publiée !'
          : '🖼️ Image générée et publiée !'
      );
      showToast(`Génération réussie (-${CREDIT_COST} 🪙)`, 'success');
    } catch (err) {
      console.error(err);
      showToast('Erreur lors de la génération IA.', 'error');
    } finally {
      setLoading(false);
    }
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
      <h2 style={{ margin: '0 0 16px', fontSize: 20, fontWeight: 900, color: '#E5A93C' }}>
        Studio IA - Génération Vidéo & Image
      </h2>

      {/* SÉLECTEUR DE TYPE DE MÉDIA */}
      <label style={{ fontSize: 13, fontWeight: 700, color: '#DDD', display: 'block', marginBottom: 6 }}>
        1. Type de contenu :
      </label>
      <div style={{ display: 'flex', gap: 10, marginBottom: 16 }}>
        <button
          type="button"
          onClick={() => setMediaType('video')}
          style={{
            flex: 1,
            padding: '10px',
            borderRadius: 10,
            fontWeight: 800,
            fontSize: 13,
            border: mediaType === 'video' ? '1px solid #E5A93C' : '1px solid #2E3248',
            backgroundColor: mediaType === 'video' ? 'rgba(229, 169, 60, 0.2)' : '#0D0E14',
            color: mediaType === 'video' ? '#E5A93C' : '#AAA',
            cursor: 'pointer',
          }}
        >
          🎬 Vidéo IA
        </button>
        <button
          type="button"
          onClick={() => setMediaType('image')}
          style={{
            flex: 1,
            padding: '10px',
            borderRadius: 10,
            fontWeight: 800,
            fontSize: 13,
            border: mediaType === 'image' ? '1px solid #E5A93C' : '1px solid #2E3248',
            backgroundColor: mediaType === 'image' ? 'rgba(229, 169, 60, 0.2)' : '#0D0E14',
            color: mediaType === 'image' ? '#E5A93C' : '#AAA',
            cursor: 'pointer',
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
            : 'Ex: Portrait futuriste d’un guerrier masaï en armure néon 8K...'
        }
        style={{
          ...inputStyle,
          height: '90px',
          backgroundColor: '#0D0E14',
          color: '#FFF',
          borderColor: '#2E3248',
          resize: 'vertical',
        }}
      />

      <button
        type="button"
        onClick={handleGenerateAndPublish}
        disabled={loading}
        style={{
          width: '100%',
          padding: '14px',
          backgroundColor: loading ? '#555' : '#E5A93C',
          color: '#000',
          border: 'none',
          borderRadius: '10px',
          fontWeight: 900,
          fontSize: 14,
          cursor: loading ? 'not-allowed' : 'pointer',
          boxShadow: '0 4px 15px rgba(229, 169, 60, 0.3)',
          transition: 'all 0.2s ease',
        }}
      >
        {loading
          ? 'Génération & Publication en cours...'
          : mediaType === 'video'
          ? '✨ Générer Vidéo & Publier sur Accueil'
          : '✨ Générer Image & Publier sur Accueil'}
      </button>

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
