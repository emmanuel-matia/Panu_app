import React, { useState } from 'react';
import { supabase } from '../../lib/supabaseClient'; // Chemin vers le client Supabase PANU

export interface LoginFormProps {
  onSuccess?: (user: any) => void;
  redirectTo?: string;
}

export function LoginForm({ onSuccess, redirectTo = '/' }: LoginFormProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // === FONCTION D'AUTHENTIFICATION SUPABASE ===
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMessage(null);

    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email: email.trim(),
        password: password,
      });

      if (error) {
        setErrorMessage(error.message);
        alert('Erreur de connexion : ' + error.message);
        return;
      }

      console.log('Connecté avec succès :', data.user);
      if (onSuccess) {
        onSuccess(data.user);
      }
      // Redirection vers la page d'accueil ou destination demandée
      window.location.href = redirectTo;
    } catch (err: any) {
      console.error('Erreur inattendue :', err);
      setErrorMessage(err?.message || 'Erreur inattendue lors de la connexion');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleLogin} style={{ display: 'grid', gap: 12 }}>
      {errorMessage && (
        <div
          style={{
            backgroundColor: 'rgba(255, 71, 87, 0.15)',
            border: '1px solid #FF4757',
            borderRadius: 8,
            padding: 10,
            color: '#FF4757',
            fontSize: 13,
          }}
        >
          {errorMessage}
        </div>
      )}
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Votre e-mail"
        required
        style={{
          padding: 12,
          borderRadius: 8,
          backgroundColor: '#0D0E12',
          color: '#FFF',
          border: '1px solid #3B3E52',
          fontSize: 14,
        }}
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Votre mot de passe"
        required
        style={{
          padding: 12,
          borderRadius: 8,
          backgroundColor: '#0D0E12',
          color: '#FFF',
          border: '1px solid #3B3E52',
          fontSize: 14,
        }}
      />
      <button
        type="submit"
        disabled={loading}
        style={{
          backgroundColor: '#E5A93C',
          color: '#000',
          fontWeight: 800,
          padding: 12,
          borderRadius: 10,
          border: 'none',
          cursor: loading ? 'not-allowed' : 'pointer',
          opacity: loading ? 0.7 : 1,
          fontSize: 14,
          transition: 'all 0.2s',
        }}
      >
        {loading ? 'Connexion en cours...' : 'Se connecter'}
      </button>
    </form>
  );
}

export default LoginForm;
