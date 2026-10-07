/**
 * CONFIGURATION CENTRALISÉE DES LOGOS, ICÔNES ET ÉCRAN DE DÉMARRAGE (SPLASH SCREEN)
 * Remplacez simplement les fichiers aux emplacements ci-dessous dans `public/` ou `src/assets/`
 * pour mettre à jour l'identité visuelle de toute l'application PANU.
 */

export const PANU_BRANDING_ASSETS = {
  /**
   * 1. LOGO PRINCIPAL DE L'APPLICATION (Barre de navigation, Écran de connexion, Studio)
   * Dossier : `public/assets/` (ou `src/assets/`)
   * Nom de fichier à remplacer : `panu-icon-192.png` (ou `logo-panu.png`)
   */
  appLogoUrl: '/assets/panu-icon-192.png',

  /**
   * 2. ICÔNES DE L'APPLICATION PWA & FAVICON (Écran d'accueil mobile & navigateur)
   * Dossiers : `public/icons/` et `public/assets/`
   * Noms de fichiers à remplacer :
   * - `/public/icons/icon-192x192.png` (192x192 px)
   * - `/public/icons/icon-512x512.png` (512x512 px)
   * - `/public/assets/panu-icon-192.png` (192x192 px)
   * - `/public/assets/panu-icon-512.png` (512x512 px)
   */
  appIcon192Url: '/icons/icon-192x192.png',
  appIcon512Url: '/icons/icon-512x512.png',

  /**
   * 3. ÉCRAN DE DÉMARRAGE (SPLASH SCREEN)
   * Dossier : `public/assets/`
   * Nom de fichier à remplacer : `panu-icon-512.png` (utilisé par le manifest.json PWA au démarrage)
   * ou `/public/assets/splash-screen.png`
   */
  splashScreenLogoUrl: '/assets/panu-icon-512.png',
} as const;

export default PANU_BRANDING_ASSETS;
