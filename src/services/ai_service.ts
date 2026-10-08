/**
 * Centralized AI Service (`ai_service.ts`)
 * PANU Creative Studio - Secure Authentication & Robust Error Handling Layer
 */

export interface AiServiceErrorDetails {
  status?: number;
  code: 'UNAUTHORIZED' | 'FORBIDDEN' | 'RATE_LIMIT' | 'CONFIG_ERROR' | 'NETWORK_ERROR' | 'UNKNOWN';
  message: string;
  actionableFeedback: string;
}

export class AiServiceException extends Error {
  public details: AiServiceErrorDetails;

  constructor(details: AiServiceErrorDetails) {
    super(details.actionableFeedback);
    this.name = 'AiServiceException';
    this.details = details;
  }
}

export class AiService {
  /**
   * Récupère la clé API de manière sécurisée depuis les variables d'environnement (Vite)
   */
  public static getApiKey(provider: 'gemini' | 'fal' | 'minimax' | 'luma' | 'bfl'): string {
    let key = '';
    switch (provider) {
      case 'gemini':
        key = (import.meta as any).env?.VITE_GEMINI_API_KEY || (import.meta as any).env?.GEMINI_API_KEY || '';
        break;
      case 'fal':
        key = (import.meta as any).env?.VITE_FAL_KEY || (import.meta as any).env?.FAL_KEY || '';
        break;
      case 'minimax':
        key = (import.meta as any).env?.VITE_MINIMAX_API_KEY || (import.meta as any).env?.MINIMAX_API_KEY || '';
        break;
      case 'luma':
        key = (import.meta as any).env?.VITE_LUMA_API_KEY || (import.meta as any).env?.LUMA_API_KEY || '';
        break;
      case 'bfl':
        key = (import.meta as any).env?.VITE_BFL_API_KEY || (import.meta as any).env?.BFL_API_KEY || '';
        break;
    }
    return typeof key === 'string' ? key.trim().replace(/^["']|["']$/g, '') : '';
  }

  /**
   * Analyse et transforme une réponse ou erreur HTTP en un message clair et actionnable pour l'UI
   */
  public static handleApiError(status: number, rawResponseText?: string): AiServiceException {
    let code: AiServiceErrorDetails['code'] = 'UNKNOWN';
    let message = rawResponseText || 'Erreur inconnue du fournisseur IA';

    try {
      if (rawResponseText) {
        const parsed = JSON.parse(rawResponseText);
        if (parsed.error?.message) {
          message = parsed.error.message;
        }
      }
    } catch (_) {
      // Garder le texte brut si ce n'est pas du JSON
    }

    let actionableFeedback = '';

    switch (status) {
      case 401:
        code = 'UNAUTHORIZED';
        actionableFeedback = '🔒 Erreur d’authentification (401) : Clé API invalide ou expirée. Veuillez vérifier votre clé dans Administration → Configuration IA.';
        break;
      case 403:
        code = 'FORBIDDEN';
        actionableFeedback = '🚫 Accès refusé (403) : Votre compte ou votre clé API ne dispose pas des autorisations nécessaires pour ce modèle IA. Vérifiez vos quotas.';
        break;
      case 429:
        code = 'RATE_LIMIT';
        actionableFeedback = '⏳ Limite de taux dépassée (429) : Trop de requêtes simultanées. Veuillez patienter quelques secondes avant de relancer la génération.';
        break;
      case 400:
      case 422:
        code = 'CONFIG_ERROR';
        actionableFeedback = `⚙️ Requête invalide (${status}) : Paramètres ou prompt incorrects. Détails : ${message}`;
        break;
      case 500:
      case 502:
      case 503:
      case 504:
        code = 'UNKNOWN';
        actionableFeedback = `⚡ Erreur du serveur fournisseur (${status}) : Le service IA est temporairement indisponible. Veuillez réessayer avec le bouton « Réessayer ».`;
        break;
      default:
        code = 'UNKNOWN';
        actionableFeedback = `❌ Erreur de génération (${status}) : ${message}`;
        break;
    }

    return new AiServiceException({
      status,
      code,
      message,
      actionableFeedback,
    });
  }

  /**
   * Vérifie si un fournisseur est configuré
   */
  public static isProviderConfigured(provider: 'gemini' | 'fal' | 'minimax' | 'luma' | 'bfl'): boolean {
    const key = this.getApiKey(provider);
    return key.length > 5 && !key.startsWith('YOUR_');
  }

  /**
   * Exécute une requête sécurisée vers l'API Gemini (Google AI)
   */
  public static async generateGeminiContent(prompt: string, model: string = 'gemini-2.5-flash'): Promise<string> {
    const apiKey = this.getApiKey('gemini');
    if (!apiKey) {
      throw new AiServiceException({
        code: 'CONFIG_ERROR',
        message: 'Clé API Gemini non configurée',
        actionableFeedback: '⚠️ Aucun moteur IA n’est configuré. Ajoutez votre clé API Gemini dans Administration → Configuration IA.'
      });
    }

    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.7, maxOutputTokens: 2048 }
      })
    });

    const responseText = await response.text();

    if (!response.ok) {
      throw this.handleApiError(response.status, responseText);
    }

    try {
      const data = JSON.parse(responseText);
      const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!text) {
        throw new Error('Réponse vide reçue du modèle Gemini.');
      }
      return text;
    } catch (parseErr: any) {
      throw new AiServiceException({
        code: 'UNKNOWN',
        message: parseErr.message || 'Erreur de décodage JSON',
        actionableFeedback: '❌ Erreur de traitement de la réponse IA. Veuillez réessayer.'
      });
    }
  }
}
