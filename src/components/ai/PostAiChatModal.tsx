import React, { useState, useRef, useEffect } from 'react';
import { FeedVideoPost } from '../../pages/HomePage';
import { generateWithMultiAiHub } from '../../services/multiAiService';

interface PostAiChatModalProps {
  post: FeedVideoPost | null;
  isOpen: boolean;
  onClose: () => void;
}

interface ChatMessage {
  sender: 'user' | 'ai';
  text: string;
  imageUrl?: string;
}

export const PostAiChatModal: React.FC<PostAiChatModalProps> = ({ post, isOpen, onClose }) => {
  if (!isOpen || !post) return null;

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      sender: 'ai',
      text: `Bonjour ! Je suis l'assistant IA de PANU. J'ai analysé ce post intitulé "${post.title}". Posez-moi vos questions ou discutez du contenu !`,
    },
  ]);
  const [input, setInput] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [isMultimodalMode, setIsMultimodalMode] = useState(false); // Mode multimodal (image + texte)
  const [isListening, setIsListening] = useState(false);
  const chatEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isGenerating]);

  // Dictée vocale avec Web Speech API
  const handleVoiceDictation = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert("La dictée vocale n'est pas supportée par votre navigateur.");
      return;
    }
    try {
      const recognition = new SpeechRecognition();
      recognition.lang = 'fr-FR';
      recognition.interimResults = false;
      recognition.onstart = () => setIsListening(true);
      recognition.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        setInput((prev) => (prev ? prev + ' ' + transcript : transcript));
      };
      recognition.onerror = () => setIsListening(false);
      recognition.onend = () => setIsListening(false);
      recognition.start();
    } catch (e) {
      console.warn('Speech recognition error:', e);
      setIsListening(false);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isGenerating) return;

    const userText = input.trim();
    setInput('');
    setMessages((prev) => [...prev, { sender: 'user', text: userText }]);
    setIsGenerating(true);

    try {
      // Construction du prompt contextuel avec l'analyse du post
      const contextPrompt = `Contexte du post analysé :
Titre : "${post.title}"
Légende : "${post.content}"
Type de média : ${post.media_type}

Question / Échange utilisateur : ${userText}`;

      if (isMultimodalMode) {
        // Génération Multimodale (Image + Texte)
        const res = await generateWithMultiAiHub({
          task: 'poster_image',
          prompt: `Génère une illustration visuelle et une analyse créative basées sur ce post et cette discussion: ${userText} (Post: ${post.title})`,
          preferredProvider: 'gemini',
        });

        setMessages((prev) => [
          ...prev,
          {
            sender: 'ai',
            text: res.outputText || 'Voici la génération visuelle et textuelle basée sur votre discussion :',
            imageUrl: res.mediaUrl || post.media_url,
          },
        ]);
      } else {
        // Réponse textuelle standard via Gemini API REST / MultiAiHub
        const res = await generateWithMultiAiHub({
          task: 'viral_script',
          prompt: contextPrompt,
          preferredProvider: 'gemini',
        });

        setMessages((prev) => [
          ...prev,
          {
            sender: 'ai',
            text: res.outputText || "Je n'ai pas pu analyser correctement la réponse, mais je suis à votre écoute pour poursuivre la discussion sur ce post.",
          },
        ]);
      }
    } catch (err: any) {
      setMessages((prev) => [
        ...prev,
        {
          sender: 'ai',
          text: `Erreur lors de la communication avec l'IA : ${err?.message || 'Erreur réseau'}`,
        },
      ]);
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0,0,0,0.8)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 9999,
        padding: 16,
      }}
      onClick={onClose}
    >
      <div
        style={{
          backgroundColor: '#151722',
          border: '2px solid #E5A93C',
          borderRadius: 20,
          maxWidth: 540,
          width: '100%',
          maxHeight: '85vh',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          boxShadow: '0 20px 60px rgba(0,0,0,0.9)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* EN-TÊTE MODAL */}
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid rgba(255,255,255,0.1)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'between',
            backgroundColor: '#1C1F2E',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: 1 }}>
            <span style={{ fontSize: 24 }}>🤖</span>
            <div>
              <h3 style={{ margin: 0, color: '#E5A93C', fontSize: 16, fontWeight: 900 }}>
                Discussion IA avec le Post
              </h3>
              <p style={{ margin: '2px 0 0', fontSize: 11, color: '#A0A5BA', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 320 }}>
                {post.title}
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {/* BOUTON BASCULER VERS IA (Multimodal) */}
            <button
              type="button"
              onClick={() => setIsMultimodalMode(!isMultimodalMode)}
              style={{
                backgroundColor: isMultimodalMode ? '#FF2E4C' : 'rgba(229, 169, 60, 0.2)',
                border: isMultimodalMode ? '1px solid #FF2E4C' : '1px solid #E5A93C',
                color: isMultimodalMode ? '#FFF' : '#E5A93C',
                borderRadius: 8,
                padding: '6px 10px',
                fontSize: 11,
                fontWeight: 800,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 4,
              }}
              title="Basculer entre Texte Standard et Génération Multimodale (Image + Texte)"
            >
              <span>{isMultimodalMode ? '🎨 Mode Multimodal (Actif)' : '⚡ Basculer vers IA'}</span>
            </button>

            <button
              type="button"
              onClick={onClose}
              style={{
                backgroundColor: 'transparent',
                border: 'none',
                color: '#FFF',
                fontSize: 18,
                cursor: 'pointer',
                padding: 4,
              }}
            >
              ✕
            </button>
          </div>
        </div>

        {/* CORPS DE LA CONVERSATION */}
        <div
          style={{
            flex: 1,
            padding: 16,
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: 12,
            backgroundColor: '#0F111A',
          }}
        >
          {messages.map((msg, index) => (
            <div
              key={index}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: msg.sender === 'user' ? 'flex-end' : 'flex-start',
              }}
            >
              <div
                style={{
                  maxWidth: '85%',
                  backgroundColor: msg.sender === 'user' ? '#E5A93C' : '#1E2235',
                  color: msg.sender === 'user' ? '#000' : '#FFF',
                  padding: '10px 14px',
                  borderRadius: 14,
                  fontSize: 13,
                  lineHeight: 1.4,
                  boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
                  border: msg.sender === 'ai' ? '1px solid rgba(229, 169, 60, 0.2)' : 'none',
                }}
              >
                {msg.text}
                {msg.imageUrl && (
                  <div style={{ marginTop: 10 }}>
                    <img
                      src={msg.imageUrl}
                      alt="Génération IA Multimodale"
                      style={{ width: '100%', borderRadius: 10, maxHeight: 220, objectFit: 'cover' }}
                    />
                  </div>
                )}
              </div>
            </div>
          ))}

          {isGenerating && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#E5A93C', fontSize: 12, fontStyle: 'italic' }}>
              <span>✨ L'IA analyse et génère la réponse{isMultimodalMode ? ' (Multimodal image+texte)' : ''}...</span>
            </div>
          )}
          <div ref={chatEndRef} />
        </div>

        {/* CHAMP DE SAISIE & DICTÉE VOCALE */}
        <form
          onSubmit={handleSendMessage}
          style={{
            padding: '12px 16px',
            borderTop: '1px solid rgba(255,255,255,0.1)',
            backgroundColor: '#151722',
            display: 'flex',
            gap: 8,
            alignItems: 'center',
          }}
        >
          {/* BOUTON DICTÉE VOCALE */}
          <button
            type="button"
            onClick={handleVoiceDictation}
            style={{
              backgroundColor: isListening ? '#FF4757' : 'rgba(255,255,255,0.08)',
              border: isListening ? '1px solid #FF4757' : '1px solid rgba(255,255,255,0.15)',
              color: isListening ? '#FFF' : '#E5A93C',
              borderRadius: 10,
              width: 40,
              height: 40,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 18,
              cursor: 'pointer',
              flexShrink: 0,
            }}
            title={isListening ? 'Écoute en cours...' : 'Dictée vocale (Web Speech API)'}
          >
            {isListening ? '🔴' : '🎙️'}
          </button>

          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={isListening ? 'Parlez, j’écoute...' : 'Posez une question sur ce post...'}
            style={{
              flex: 1,
              backgroundColor: '#0D0E14',
              border: '1px solid rgba(255,255,255,0.15)',
              borderRadius: 10,
              padding: '10px 14px',
              color: '#FFF',
              fontSize: 13,
              outline: 'none',
            }}
          />

          <button
            type="submit"
            disabled={isGenerating || !input.trim()}
            style={{
              backgroundColor: '#E5A93C',
              color: '#000',
              border: 'none',
              borderRadius: 10,
              padding: '10px 16px',
              fontWeight: 900,
              cursor: isGenerating || !input.trim() ? 'not-allowed' : 'pointer',
              fontSize: 13,
            }}
          >
            Envoyer
          </button>
        </form>
      </div>
    </div>
  );
};

export default PostAiChatModal;
