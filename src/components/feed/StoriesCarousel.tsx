import React, { useEffect, useState } from 'react';
import { supabase } from '../../lib/supabaseClient';
import { motion, AnimatePresence } from 'framer-motion';

export interface Story {
  id: string;
  user_id: string;
  author_name: string;
  author_avatar: string;
  media_url: string;
  created_at: string;
}

export const StoriesCarousel: React.FC = () => {
  const [stories, setStories] = useState<Story[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStories = async () => {
      setLoading(true);
      try {
        // Fetch posts marked as 'story'
        const { data, error } = await supabase
          .from('posts')
          .select('id, user_id, media_url, created_at, profiles(username, avatar_url)')
          .eq('media_type', 'story')
          .order('created_at', { ascending: false });

        if (error) throw error;

        if (data) {
          const enrichedStories = data.map((s: any) => ({
            id: s.id,
            user_id: s.user_id,
            author_name: s.profiles?.username || 'Utilisateur',
            author_avatar: s.profiles?.avatar_url || '',
            media_url: s.media_url,
            created_at: s.created_at,
          }));
          setStories(enrichedStories);
        }
      } catch (err) {
        console.error('Erreur chargement stories:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchStories();
  }, []);

  if (loading) return null;

  return (
    <div className="flex gap-4 overflow-x-auto p-4 scrollbar-hide">
      {stories.map((story) => (
        <div key={story.id} className="flex flex-col items-center gap-1">
          <div className="relative w-16 h-16 rounded-full p-[2px] bg-gradient-to-tr from-yellow-400 to-purple-600">
            <motion.div
              className="absolute inset-0 rounded-full border-2 border-transparent"
              animate={{ rotate: 360 }}
              transition={{ duration: 5, repeat: Infinity, ease: "linear" }}
            />
            <img
              src={story.author_avatar}
              alt={story.author_name}
              className="w-full h-full rounded-full object-cover border-2 border-black"
            />
          </div>
          <span className="text-xs text-white truncate w-16 text-center">
            {story.author_name}
          </span>
        </div>
      ))}
    </div>
  );
};
