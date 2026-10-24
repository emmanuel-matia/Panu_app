import React, { useEffect, useState, useRef, useCallback } from 'react';
import { supabase } from '../../lib/supabaseClient';
import { STORAGE_BUCKETS } from '../../config/supabaseConfig';

export interface Story {
  id: string;
  user_id: string;
  author_name: string;
  author_avatar: string;
  media_url: string;
  media_type: string;
  caption: string;
  created_at: string;
}

interface StoriesGroup {
  user_id: string;
  author_name: string;
  author_avatar: string;
  stories: Story[];
}

export const StoriesCarousel: React.FC = () => {
  const [storyGroups, setStoryGroups] = useState<StoriesGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeGroupIndex, setActiveGroupIndex] = useState<number | null>(null);
  const [activeStoryIndex, setActiveStoryIndex] = useState(0);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const progressTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const [progress, setProgress] = useState(0);

  const fetchStories = useCallback(async () => {
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('stories')
        .select('id, user_id, media_url, media_type, caption, created_at, expires_at')
        .order('created_at', { ascending: false });

      if (error) throw error;

      if (data && data.length > 0) {
        const userIds = Array.from(new Set(data.map((s: any) => s.user_id)));
        const { data: profiles } = await supabase
          .from('profiles')
          .select('id, username, full_name, avatar_url')
          .in('id', userIds);

        const profileMap: Record<string, any> = {};
        (profiles || []).forEach((p: any) => { profileMap[p.id] = p; });

        const groupsMap: Record<string, StoriesGroup> = {};
        data.forEach((s: any) => {
          const prof = profileMap[s.user_id];
          if (!groupsMap[s.user_id]) {
            groupsMap[s.user_id] = {
              user_id: s.user_id,
              author_name: prof?.full_name || prof?.username || 'Créateur',
              author_avatar: prof?.avatar_url || '',
              stories: [],
            };
          }
          groupsMap[s.user_id].stories.push({
            id: s.id,
            user_id: s.user_id,
            author_name: groupsMap[s.user_id].author_name,
            author_avatar: groupsMap[s.user_id].author_avatar,
            media_url: s.media_url,
            media_type: s.media_type || 'image',
            caption: s.caption || '',
            created_at: s.created_at,
          });
        });
        setStoryGroups(Object.values(groupsMap));
      } else {
        setStoryGroups([]);
      }
    } catch (err) {
      console.error('Erreur chargement stories:', err);
      setStoryGroups([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStories();
  }, [fetchStories]);

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';

    setUploading(true);
    try {
      const { data: { session } } = await supabase.auth.getSession();
      if (!session?.user?.id) {
        alert('Connectez-vous pour publier une story.');
        setUploading(false);
        return;
      }

      const isVideo = file.type.startsWith('video');
      const ext = file.name.split('.').pop() || (isVideo ? 'mp4' : 'jpg');
      const fileName = `${Date.now()}_${Math.random().toString(36).substring(2, 9)}.${ext}`;
      const filePath = `stories/${fileName}`;

      const { error: uploadError } = await supabase.storage
        .from(STORAGE_BUCKETS.POST_MEDIA)
        .upload(filePath, file, { cacheControl: '3600', upsert: false });

      if (uploadError) throw uploadError;

      const { data: publicUrlData } = supabase.storage
        .from(STORAGE_BUCKETS.POST_MEDIA)
        .getPublicUrl(filePath);

      const { error: insertError } = await supabase.from('stories').insert({
        user_id: session.user.id,
        media_url: publicUrlData.publicUrl,
        media_type: isVideo ? 'video' : 'image',
      });

      if (insertError) throw insertError;

      fetchStories();
    } catch (err: any) {
      alert(`Erreur story: ${err?.message || 'Echec de l upload'}`);
    } finally {
      setUploading(false);
    }
  };

  const openStoryViewer = (groupIdx: number) => {
    setActiveGroupIndex(groupIdx);
    setActiveStoryIndex(0);
    setProgress(0);
  };

  const closeStoryViewer = () => {
    setActiveGroupIndex(null);
    setActiveStoryIndex(0);
    setProgress(0);
    if (progressTimerRef.current) {
      clearInterval(progressTimerRef.current);
      progressTimerRef.current = null;
    }
  };

  const goToNextStory = () => {
    if (activeGroupIndex === null) return;
    const group = storyGroups[activeGroupIndex];
    if (activeStoryIndex < group.stories.length - 1) {
      setActiveStoryIndex(activeStoryIndex + 1);
      setProgress(0);
    } else if (activeGroupIndex < storyGroups.length - 1) {
      setActiveGroupIndex(activeGroupIndex + 1);
      setActiveStoryIndex(0);
      setProgress(0);
    } else {
      closeStoryViewer();
    }
  };

  const goToPrevStory = () => {
    if (activeGroupIndex === null) return;
    if (activeStoryIndex > 0) {
      setActiveStoryIndex(activeStoryIndex - 1);
      setProgress(0);
    } else if (activeGroupIndex > 0) {
      const prevGroup = storyGroups[activeGroupIndex - 1];
      setActiveGroupIndex(activeGroupIndex - 1);
      setActiveStoryIndex(prevGroup.stories.length - 1);
      setProgress(0);
    }
  };

  useEffect(() => {
    if (activeGroupIndex !== null) {
      setProgress(0);
      if (progressTimerRef.current) clearInterval(progressTimerRef.current);
      progressTimerRef.current = setInterval(() => {
        setProgress((prev) => {
          if (prev >= 100) {
            goToNextStory();
            return 0;
          }
          return prev + 2;
        });
      }, 80);
      return () => {
        if (progressTimerRef.current) {
          clearInterval(progressTimerRef.current);
          progressTimerRef.current = null;
        }
      };
    }
  }, [activeGroupIndex, activeStoryIndex]);

  const currentStory: Story | null =
    activeGroupIndex !== null && storyGroups[activeGroupIndex]
      ? storyGroups[activeGroupIndex].stories[activeStoryIndex]
      : null;

  if (loading) return null;

  return (
    <>
      <div style={{ display: 'flex', gap: 14, overflowX: 'auto', padding: '12px 16px', scrollbarWidth: 'none' }}>
        {/* Add story button */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, flexShrink: 0 }}>
          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
            style={{
              width: 60, height: 60, borderRadius: '50%', border: '2px dashed #E5A93C',
              backgroundColor: 'rgba(229,169,60,0.1)', display: 'flex', alignItems: 'center',
              justifyContent: 'center', cursor: uploading ? 'not-allowed' : 'pointer', color: '#E5A93C',
              fontSize: 22, fontWeight: 900,
            }}
          >
            {uploading ? '...' : '+'}
          </button>
          <span style={{ fontSize: 10, color: '#AAA', maxWidth: 60, textAlign: 'center', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Ma story</span>
        </div>
        <input ref={fileInputRef} type="file" accept="image/*,video/*" onChange={handleFileSelect} style={{ display: 'none' }} />

        {/* Story circles */}
        {storyGroups.map((group, idx) => (
          <div key={group.user_id} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, flexShrink: 0 }}>
            <button
              onClick={() => openStoryViewer(idx)}
              style={{
                width: 60, height: 60, borderRadius: '50%', padding: 3,
                background: 'linear-gradient(135deg, #E5A93C, #FF6B6B)', border: 'none', cursor: 'pointer',
              }}
            >
              {group.author_avatar ? (
                <img src={group.author_avatar} alt={group.author_name}
                  style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover', border: '2px solid #0B0C12' }} />
              ) : (
                <div style={{ width: '100%', height: '100%', borderRadius: '50%', background: '#1E202B', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#E5A93C', fontWeight: 900, fontSize: 20, border: '2px solid #0B0C12' }}>
                  {group.author_name.charAt(0).toUpperCase()}
                </div>
              )}
            </button>
            <span style={{ fontSize: 10, color: '#CCC', maxWidth: 60, textAlign: 'center', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {group.author_name}
            </span>
          </div>
        ))}
      </div>

      {/* Full-screen story viewer */}
      {currentStory && (
        <div
          style={{ position: 'fixed', inset: 0, backgroundColor: '#000', zIndex: 9999, display: 'flex', flexDirection: 'column' }}
          onClick={closeStoryViewer}
        >
          {/* Progress bars */}
          <div style={{ display: 'flex', gap: 3, padding: '8px 12px 0' }}>
            {storyGroups[activeGroupIndex!].stories.map((_, i) => (
              <div key={i} style={{ flex: 1, height: 3, borderRadius: 2, backgroundColor: 'rgba(255,255,255,0.3)' }}>
                <div style={{ width: i < activeStoryIndex ? '100%' : i === activeStoryIndex ? `${progress}%` : '0%', height: '100%', borderRadius: 2, backgroundColor: '#E5A93C', transition: 'width 0.08s linear' }} />
              </div>
            ))}
          </div>

          {/* Header */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 16px' }} onClick={(e) => e.stopPropagation()}>
            <div style={{ width: 34, height: 34, borderRadius: '50%', overflow: 'hidden', border: '2px solid #E5A93C', backgroundColor: '#1E202B', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              {currentStory.author_avatar ? <img src={currentStory.author_avatar} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <span style={{ color: '#E5A93C', fontWeight: 900 }}>{currentStory.author_name.charAt(0)}</span>}
            </div>
            <span style={{ color: '#FFF', fontWeight: 700, fontSize: 14 }}>{currentStory.author_name}</span>
            <button onClick={closeStoryViewer} style={{ marginLeft: 'auto', background: 'none', border: 'none', color: '#FFF', fontSize: 22, cursor: 'pointer' }}>✕</button>
          </div>

          {/* Media */}
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative' }} onClick={(e) => e.stopPropagation()}>
            {currentStory.media_type === 'video' ? (
              <video src={currentStory.media_url} autoPlay playsInline style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }}
                onEnded={goToNextStory} />
            ) : (
              <img src={currentStory.media_url} alt={currentStory.caption || 'Story'} style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }}
                onError={(e) => { (e.target as HTMLImageElement).style.opacity = '0.3'; }} />
            )}

            {/* Navigation arrows */}
            <button onClick={goToPrevStory} style={{ position: 'absolute', left: 8, top: '50%', transform: 'translateY(-50%)', background: 'rgba(0,0,0,0.4)', border: 'none', color: '#FFF', fontSize: 24, borderRadius: '50%', width: 40, height: 40, cursor: 'pointer' }}>‹</button>
            <button onClick={goToNextStory} style={{ position: 'absolute', right: 8, top: '50%', transform: 'translateY(-50%)', background: 'rgba(0,0,0,0.4)', border: 'none', color: '#FFF', fontSize: 24, borderRadius: '50%', width: 40, height: 40, cursor: 'pointer' }}>›</button>

            {currentStory.caption && (
              <div style={{ position: 'absolute', bottom: 20, left: 20, right: 20, textAlign: 'center', color: '#FFF', fontSize: 14, fontWeight: 600, textShadow: '0 1px 4px rgba(0,0,0,0.8)' }}>
                {currentStory.caption}
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
};

export default StoriesCarousel;
