/**
 * CONFIGURATION INTERNE SÉCURISÉE DES SERVICES BACKEND
 * Centralise les noms de buckets et constantes sans exposition directe dans l'UI.
 */

export const STORAGE_BUCKETS = {
  POST_MEDIA: 'post-media',
  AVATARS: 'avatars',
  COVERS: 'covers',
  LOGOS: 'logos',
};

export const SUPABASE_CONFIG = {
  TABLES: {
    POSTS: 'posts',
    PROFILES: 'profiles',
    LIVES: 'lives',
    USER_WALLETS: 'user_wallets',
  },
};
