import { supabase } from '../lib/supabaseClient';
import { STORAGE_BUCKETS } from '../config/supabaseConfig';

export interface SelectedMediaFile {
  file?: File;
  blob?: Blob;
  previewUrl: string;
  name: string;
  type: 'image' | 'video';
  source: 'phone_gallery' | 'native_camera' | 'device_storage';
}

/**
 * Helper sécurisé pour détecter l'environnement natif Capacitor / Cordova
 */
function isCapacitorAvailable(): boolean {
  if (typeof window === 'undefined') return false;
  return !!(window as any).Capacitor?.isNativePlatform?.() || !!(window as any).Capacitor?.Plugins?.Camera;
}

/**
 * 1. SÉLECTION DEPUIS LA GALERIE LOCALE DU TÉLÉPHONE
 * RÈGLE FONDAMENTALE : Ne JAMAIS inclure l'attribut `capture` sur cet <input type="file">.
 * L'absence de l'attribut `capture` permet à Android et iOS d'ouvrir directement
 * la galerie photo locale du téléphone plutôt que de forcer Google Photos ou la caméra.
 */
export async function pickImageFromPhoneGallery(): Promise<SelectedMediaFile> {
  // Option A : Environnement natif Capacitor / Cordova avec plugin Camera
  if (isCapacitorAvailable()) {
    try {
      const capacitorPlugins = (window as any).Capacitor?.Plugins;
      if (capacitorPlugins?.Camera) {
        // CameraSource.Photos = 1 (Ouvre la galerie locale de l'appareil)
        const photo = await capacitorPlugins.Camera.getPhoto({
          quality: 90,
          allowEditing: false,
          resultType: 'uri', // ou 'dataUrl'
          source: 'PHOTOS', // CameraSource.Photos
        });

        const webPath = photo.webPath || photo.path;
        const response = await fetch(webPath);
        const blob = await response.blob();
        return {
          blob,
          previewUrl: webPath,
          name: `gallery_photo_${Date.now()}.jpg`,
          type: 'image',
          source: 'phone_gallery',
        };
      }
    } catch (err: any) {
      console.warn('Capacitor Camera.Photos non disponible ou annulé, passage au sélecteur web :', err);
    }
  }

  // Option B : Sélecteur Web / PWA optimisé pour la galerie locale
  return new Promise((resolve, reject) => {
    const input = document.createElement('input');
    input.type = 'file';
    // STRICTEMENT image/* SANS attribut capture pour forcer la galerie locale du téléphone
    input.accept = 'image/*';
    input.style.display = 'none';

    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) {
        reject(new Error('Aucun fichier sélectionné'));
        return;
      }
      const previewUrl = URL.createObjectURL(file);
      resolve({
        file,
        blob: file,
        previewUrl,
        name: file.name,
        type: 'image',
        source: 'phone_gallery',
      });
      document.body.removeChild(input);
    };

    input.oncancel = () => {
      reject(new Error('Sélection annulée'));
      document.body.removeChild(input);
    };

    document.body.appendChild(input);
    input.click();
  });
}

/**
 * 2. CAPTURE EN DIRECT DEPUIS L'APPAREIL PHOTO PHYSIQUE (CAMÉRA)
 */
export async function takePhotoFromNativeCamera(): Promise<SelectedMediaFile> {
  // Option A : Plugin natif Capacitor avec source = CAMERA
  if (isCapacitorAvailable()) {
    try {
      const capacitorPlugins = (window as any).Capacitor?.Plugins;
      if (capacitorPlugins?.Camera) {
        const photo = await capacitorPlugins.Camera.getPhoto({
          quality: 90,
          allowEditing: false,
          resultType: 'uri',
          source: 'CAMERA', // CameraSource.Camera
        });

        const webPath = photo.webPath || photo.path;
        const response = await fetch(webPath);
        const blob = await response.blob();
        return {
          blob,
          previewUrl: webPath,
          name: `camera_photo_${Date.now()}.jpg`,
          type: 'image',
          source: 'native_camera',
        };
      }
    } catch (err) {
      console.warn('Capacitor Camera non disponible, fallback Web :', err);
    }
  }

  // Option B : Web avec attribut capture activé UNIQUEMENT pour la caméra
  return new Promise((resolve, reject) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.setAttribute('capture', 'environment'); // Déclenche spécifiquement l'appareil photo
    input.style.display = 'none';

    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) {
        reject(new Error('Aucune capture effectuée'));
        return;
      }
      const previewUrl = URL.createObjectURL(file);
      resolve({
        file,
        blob: file,
        previewUrl,
        name: file.name,
        type: 'image',
        source: 'native_camera',
      });
      document.body.removeChild(input);
    };

    document.body.appendChild(input);
    input.click();
  });
}

/**
 * 3. SÉLECTION D'UNE VIDÉO DEPUIS LA GALERIE LOCALE
 * Sans attribut capture pour garantir l'ouverture de la galerie vidéo du téléphone
 */
export async function pickVideoFromPhoneGallery(): Promise<SelectedMediaFile> {
  return new Promise((resolve, reject) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'video/*';
    input.style.display = 'none';

    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) {
        reject(new Error('Aucune vidéo sélectionnée'));
        return;
      }
      const previewUrl = URL.createObjectURL(file);
      resolve({
        file,
        blob: file,
        previewUrl,
        name: file.name,
        type: 'video',
        source: 'phone_gallery',
      });
      document.body.removeChild(input);
    };

    document.body.appendChild(input);
    input.click();
  });
}

/**
 * 4. TÉLÉVERSEMENT AUTOMATIQUE VERS SUPABASE STORAGE
 * Buckets supportés configurés dans STORAGE_BUCKETS
 */
export async function uploadMediaToSupabase(
  media: SelectedMediaFile,
  bucket: string,
  userId: string,
  category: string
): Promise<string> {
  const content = media.file || media.blob;
  if (!content) {
    throw new Error('Fichier introuvable pour le téléversement');
  }

  const ext = media.type === 'video' ? 'mp4' : 'jpg';
  const fileName = `${userId}/${category}_${Date.now()}.${ext}`;

  // Logs de stockage sécurisés (sans URL de base de données)
  console.log(`[PANU Storage] Upload en cours -> Bucket: '${bucket}' | Fichier: '${fileName}'`, {
    bucket,
    fileName,
    sizeBytes: content.size,
    mimeType: media.type === 'video' ? 'video/mp4' : 'image/jpeg'
  });

  const { data, error } = await supabase.storage.from(bucket).upload(fileName, content, {
    upsert: true,
    contentType: media.type === 'video' ? 'video/mp4' : 'image/jpeg',
  });

  if (error) {
    console.error(`[PANU Storage] Échec upload bucket '${bucket}':`, error.message);

    // Si le bucket spécifique n'existe pas, fallback sur le stockage principal
    if (bucket !== STORAGE_BUCKETS.POST_MEDIA) {
      console.warn(`[PANU Storage] Tentative de repli (fallback) sur le stockage principal...`);
      const fallback = await supabase.storage.from(STORAGE_BUCKETS.POST_MEDIA).upload(fileName, content, {
        upsert: true,
      });
      if (fallback.error) {
        console.error(`[PANU Storage] Échec fallback:`, fallback.error.message);
        throw new Error(
          `Erreur de stockage. Veuillez vérifier votre connexion ou réessayer plus tard.`
        );
      }
      const { data: publicData } = supabase.storage.from(STORAGE_BUCKETS.POST_MEDIA).getPublicUrl(fileName);
      return publicData.publicUrl;
    }

    const detailedErr = error.message?.includes('Bucket not found') || (error as any).statusCode === '404'
      ? `Configuration de stockage introuvable. Veuillez contacter le support.`
      : `Échec upload stockage : ${error.message}`;

    throw new Error(detailedErr);
  }

  const { data: publicData } = supabase.storage.from(bucket).getPublicUrl(data.path);
  return publicData.publicUrl;
}
