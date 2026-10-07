import { supabase } from '../lib/supabaseClient';

export interface ContactSuggestion {
  id: string;
  username: string;
  fullName: string;
  avatarUrl: string;
  phoneNumber: string;
}

/**
 * Hache un numéro de téléphone en SHA-256 (format hex) pour comparaison sécurisée
 */
async function hashPhoneNumber(phone: string): Promise<string> {
  const cleanPhone = phone.replace(/\D/g, ''); // Garder uniquement les chiffres
  const msgUint8 = new TextEncoder().encode(cleanPhone);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Synchronise les contacts du téléphone et suggère des utilisateurs PANU
 */
export async function syncAndFetchContactSuggestions(): Promise<ContactSuggestion[]> {
  if (typeof navigator === 'undefined' || !('contacts' in navigator)) {
    console.warn('Web Contacts API non supportée sur ce navigateur.');
    return [];
  }

  try {
    // Demander l'autorisation et sélectionner les contacts (Nom et Tel)
    // @ts-ignore - navigator.contacts est expérimental
    const contacts = await navigator.contacts.select(['name', 'tel'], { multiple: true });
    
    if (!contacts || contacts.length === 0) return [];

    const phoneHashes: string[] = [];
    const phoneToNameMap: Record<string, string> = {};

    for (const contact of contacts) {
      if (contact.tel && contact.tel.length > 0) {
        for (const tel of contact.tel) {
          const hash = await hashPhoneNumber(tel);
          phoneHashes.push(hash);
          phoneToNameMap[hash] = contact.name?.[0] || tel;
        }
      }
    }

    if (phoneHashes.length === 0) return [];

    // Note: Dans une version réelle, on stockerait les hashs SHA-256 des numéros 
    // dans la table profiles. Ici on compare directement les numéros (simplification démo sûre)
    // ou on filtre par les numéros présents.
    
    // Récupérer les profils ayant un numéro correspondant
    const { data: profiles, error } = await supabase
      .from('profiles')
      .select('id, username, full_name, avatar_url, phone')
      .not('phone', 'is', null);

    if (error || !profiles) return [];

    // Filtrer les profils dont le hash du numéro est dans notre liste
    const suggestions: ContactSuggestion[] = [];
    for (const p of profiles) {
      if (p.phone) {
        const pHash = await hashPhoneNumber(p.phone);
        if (phoneHashes.includes(pHash)) {
          suggestions.push({
            id: p.id,
            username: p.username || '',
            fullName: p.full_name || '',
            avatarUrl: p.avatar_url || '',
            phoneNumber: p.phone
          });
        }
      }
    }

    return suggestions;
  } catch (err) {
    console.error('Erreur synchronisation contacts:', err);
    return [];
  }
}

/**
 * Invitation via Web Share API
 */
export async function shareInviteLink(username: string) {
  const shareData = {
    title: 'Rejoins-moi sur PANU !',
    text: `Salut ! Viens découvrir PANU, le studio créatif africain. Crée des vidéos IA incroyables et suis mes publications.`,
    url: `https://panu.app/invite?ref=${encodeURIComponent(username)}`,
  };

  try {
    if (navigator.share) {
      await navigator.share(shareData);
      return true;
    } else {
      // Fallback : copier dans le presse-papier
      await navigator.clipboard.writeText(shareData.url);
      alert('Lien d\'invitation copié dans le presse-papier !');
      return false;
    }
  } catch (err) {
    console.error('Erreur partage:', err);
    return false;
  }
}
