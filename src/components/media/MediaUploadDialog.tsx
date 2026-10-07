import React, { useState } from 'react';
import {
  pickImageFromPhoneGallery,
  takePhotoFromNativeCamera,
  pickVideoFromPhoneGallery,
  SelectedMediaFile,
} from '../../services/nativeMediaService';

export interface MediaUploadDialogProps {
  isOpen: boolean;
  title?: string;
  subtitle?: string;
  targetType?: 'avatar' | 'cover' | 'logo' | 'video';
  onClose: () => void;
  onMediaReady: (media: SelectedMediaFile) => void;
}

/**
 * Modale de sélection média universelle pour React / Next.js / PWA / Capacitor
 * - Ouvre directement la galerie locale du smartphone sans forcer Google Photos
 * - Permet la capture directe avec l'appareil photo
 * - Sans attribut capture restrictif sur les sélecteurs de galerie
 */
export const MediaUploadDialog: React.FC<MediaUploadDialogProps> = ({
  isOpen,
  title = 'Sélectionner un média',
  subtitle = 'Choisissez une photo ou vidéo depuis votre téléphone',
  targetType = 'avatar',
  onClose,
  onMediaReady,
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handlePickGallery = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const media = targetType === 'video'
        ? await pickVideoFromPhoneGallery()
        : await pickImageFromPhoneGallery();
      onMediaReady(media);
      onClose();
    } catch (err: any) {
      if (err.message !== 'Sélection annulée') {
        setError(err.message || 'Impossible d’accéder à la galerie');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleTakePhoto = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const media = await takePhotoFromNativeCamera();
      onMediaReady(media);
      onClose();
    } catch (err: any) {
      if (err.message !== 'Sélection annulée') {
        setError(err.message || 'Impossible d’activer la caméra');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        zIndex: 9999,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 16,
      }}
      onClick={onClose}
    >
      <div
        style={{
          backgroundColor: '#161722',
          border: '1px solid rgba(229, 169, 60, 0.4)',
          borderRadius: 20,
          padding: 24,
          maxWidth: 440,
          width: '100%',
          color: '#FFF',
          boxShadow: '0 20px 40px rgba(0,0,0,0.6)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0, color: '#E5A93C', fontSize: 18 }}>{title}</h3>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#999',
              fontSize: 20,
              cursor: 'pointer',
            }}
          >
            ✕
          </button>
        </div>

        <p style={{ margin: '0 0 20px', color: '#AAA', fontSize: 13 }}>{subtitle}</p>

        {error && (
          <div
            style={{
              backgroundColor: 'rgba(255, 71, 87, 0.15)',
              border: '1px solid #FF4757',
              color: '#FF6B81',
              padding: 10,
              borderRadius: 8,
              fontSize: 12,
              marginBottom: 16,
            }}
          >
            {error}
          </div>
        )}

        <div style={{ display: 'grid', gap: 12 }}>
          {/* Option 1: Galerie locale du téléphone */}
          <button
            type="button"
            onClick={handlePickGallery}
            disabled={isLoading}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 14,
              backgroundColor: '#1F202E',
              border: '1px solid rgba(229, 169, 60, 0.3)',
              borderRadius: 14,
              padding: 16,
              color: '#FFF',
              cursor: 'pointer',
              textAlign: 'left',
              width: '100%',
            }}
          >
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: '50%',
                backgroundColor: 'rgba(229, 169, 60, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 22,
              }}
            >
              🖼️
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: 14, color: '#E5A93C' }}>
                {targetType === 'video' ? 'Galerie Vidéos locale du téléphone' : 'Galerie Photos locale du téléphone'}
              </div>
              <div style={{ fontSize: 12, color: '#888', marginTop: 2 }}>
                Ouvrir directement l’application Galerie sans forcer Google Photos
              </div>
            </div>
          </button>

          {/* Option 2: Appareil photo physique (Prendre une photo) */}
          {targetType !== 'video' && (
            <button
              type="button"
              onClick={handleTakePhoto}
              disabled={isLoading}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 14,
                backgroundColor: '#1F202E',
                border: '1px solid rgba(229, 169, 60, 0.3)',
                borderRadius: 14,
                padding: 16,
                color: '#FFF',
                cursor: 'pointer',
                textAlign: 'left',
                width: '100%',
              }}
            >
              <div
                style={{
                  width: 44,
                  height: 44,
                  borderRadius: '50%',
                  backgroundColor: 'rgba(229, 169, 60, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 22,
                }}
              >
                📸
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14, color: '#E5A93C' }}>
                  Appareil photo physique
                </div>
                <div style={{ fontSize: 12, color: '#888', marginTop: 2 }}>
                  Prendre une photo instantanée avec la caméra de votre téléphone
                </div>
              </div>
            </button>
          )}
        </div>

        {isLoading && (
          <div style={{ textAlign: 'center', marginTop: 16, color: '#E5A93C', fontSize: 13 }}>
            Chargement du média...
          </div>
        )}
      </div>
    </div>
  );
};
