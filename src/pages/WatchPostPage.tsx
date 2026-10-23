import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { supabase } from '../lib/supabaseClient';
import { ShareButtonWithOpenGraph } from '../components/share/ShareButtonWithOpenGraph';
import { togglePostLike } from '../services/likeService';
import { fetchCommentsForPosts, addPostComment } from '../services/commentService';
import { useNotifications } from '../context/NotificationContext';

interface WatchPost {
  id: string;
  user_id: string;
  title: string;
  content: string;
  media_url: string;
  media_type: string;
  author_name?: string;
  author_avatar?: string;
  likes_count?: number;
  comments_count?: number;
}

interface CommentItem {
  id: string;
  postId: string;
  authorName: string;
  text: string;
  createdAt: string;
}

export const WatchPostPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { currentUserId, currentUserName } = useNotifications();
  const [post, setPost] = useState<WatchPost | null>(null);
  const [loading, setLoading] = useState(true);
  const [liked, setLiked] = useState(false);
  const [likesCount, setLikesCount] = useState(0);
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [commentInput, setCommentInput] = useState('');

  useEffect(() => {
    const loadPost = async () => {
      if (!id) return;
      setLoading(true);
      const { data, error } = await supabase
        .from('posts')
        .select('*')
        .eq('id', id)
        .maybeSingle();

      if (error || !data) {
        setLoading(false);
        return;
      }

      const { data: profile } = await supabase
        .from('profiles')
        .select('full_name, username, avatar_url')
        .eq('id', data.user_id)
        .maybeSingle();

      const enriched: WatchPost = {
        ...data,
        author_name: profile?.full_name || profile?.username || 'Créateur PANU',
        author_avatar: profile?.avatar_url || '',
      };
      setPost(enriched);
      setLikesCount(data.likes_count || 0);

      const [commentsMap] = await Promise.all([
        fetchCommentsForPosts([id]),
      ]);
      setComments(commentsMap[id] || []);

      setLoading(false);
    };
    loadPost();
  }, [id]);

  const handleLike = async () => {
    if (!post || !currentUserId) return;
    const isCurrentlyLiked = liked;
    setLiked(!isCurrentlyLiked);
    setLikesCount(prev => Math.max(0, prev + (isCurrentlyLiked ? -1 : 1)));
    await togglePostLike({
      postId: post.id,
      userId: currentUserId,
      isCurrentlyLiked,
      postAuthorId: post.user_id,
      postTitle: post.title,
      actorName: currentUserName,
    });
  };

  const handleComment = async () => {
    if (!post || !commentInput.trim() || !currentUserId) return;
    const text = commentInput.trim();
    setCommentInput('');
    const tempId = `comm_${Date.now()}`;
    setComments(prev => [...prev, {
      id: tempId, postId: post.id, authorName: currentUserName || 'Vous', text, createdAt: 'À l\'instant',
    }]);
    const created = await addPostComment({
      postId: post.id,
      userId: currentUserId,
      content: text,
      authorName: currentUserName,
      postAuthorId: post.user_id,
      postTitle: post.title,
    });
    if (created) {
      setComments(prev => prev.map(c => c.id === tempId ? created : c));
    }
  };

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: '#0B0C12' }}>
        <span style={{ color: '#E5A93C', fontSize: 14 }}>Chargement...</span>
      </div>
    );
  }

  if (!post) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', backgroundColor: '#0B0C12', color: '#FFF' }}>
        <div style={{ fontSize: 40, marginBottom: 12 }}>🎬</div>
        <p>Cette publication n'existe plus ou n'est pas disponible.</p>
        <button onClick={() => navigate('/')} style={{ marginTop: 16, backgroundColor: '#E5A93C', color: '#000', border: 'none', padding: '10px 20px', borderRadius: 10, fontWeight: 800, cursor: 'pointer' }}>
          Retour à l'accueil
        </button>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#0B0C12', color: '#F8F9FA' }}>
      <div style={{ maxWidth: 600, margin: '0 auto', padding: '16px' }}>
        {/* Retour */}
        <button onClick={() => navigate(-1)} style={{ background: 'none', border: 'none', color: '#E5A93C', fontSize: 14, fontWeight: 700, cursor: 'pointer', marginBottom: 16 }}>
          ← Retour
        </button>

        {/* Auteur */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
          <div style={{ width: 44, height: 44, borderRadius: '50%', border: '2px solid #E5A93C', overflow: 'hidden', backgroundColor: '#1E202B', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 900 }}>
            {post.author_avatar ? <img src={post.author_avatar} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : (post.author_name || 'P').charAt(0).toUpperCase()}
          </div>
          <span style={{ fontWeight: 800, fontSize: 15 }}>{post.author_name}</span>
        </div>

        {/* Média */}
        {post.media_url && (
          post.media_type === 'video' ? (
            <video src={post.media_url} controls autoPlay loop playsInline style={{ width: '100%', borderRadius: 16, maxHeight: '70vh', objectFit: 'cover', backgroundColor: '#000' }} />
          ) : (
            <img src={post.media_url} alt={post.title} style={{ width: '100%', borderRadius: 16, maxHeight: '70vh', objectFit: 'cover' }} />
          )
        )}

        {/* Titre & description */}
        <h2 style={{ fontSize: 18, fontWeight: 800, marginTop: 16, marginBottom: 4 }}>{post.title}</h2>
        {post.content && <p style={{ fontSize: 14, color: '#CCC', marginBottom: 16 }}>{post.content}</p>}

        {/* Actions */}
        <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 20 }}>
          <button onClick={handleLike} style={{
            backgroundColor: liked ? 'rgba(255,46,76,0.2)' : 'rgba(255,255,255,0.06)',
            border: liked ? '1px solid #FF2E4C' : '1px solid rgba(255,255,255,0.15)',
            color: liked ? '#FF2E4C' : '#FFF', padding: '8px 14px', borderRadius: 999, fontSize: 13, fontWeight: 700, cursor: 'pointer',
            display: 'flex', alignItems: 'center', gap: 6,
          }}>
            {liked ? '❤️' : '🤍'} {likesCount}
          </button>
          <ShareButtonWithOpenGraph
            contentType="watch"
            contentId={post.id}
            title={post.title}
            description={post.content}
          />
        </div>

        {/* Commentaires */}
        <div style={{ backgroundColor: '#15161E', borderRadius: 14, padding: 16, border: '1px solid rgba(255,255,255,0.08)' }}>
          <h3 style={{ margin: '0 0 12px', fontSize: 15, color: '#E5A93C' }}>💬 Commentaires ({comments.length})</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 14, maxHeight: 300, overflowY: 'auto' }}>
            {comments.length === 0 ? (
              <p style={{ textAlign: 'center', color: '#777', fontSize: 13 }}>Aucun commentaire. Soyez le premier !</p>
            ) : comments.map(c => (
              <div key={c.id} style={{ backgroundColor: '#0D0E12', padding: '8px 12px', borderRadius: 8, fontSize: 12 }}>
                <span style={{ color: '#E5A93C', fontWeight: 800 }}>{c.authorName} : </span>
                <span style={{ color: '#EEE' }}>{c.text}</span>
              </div>
            ))}
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              type="text"
              value={commentInput}
              onChange={e => setCommentInput(e.target.value)}
              placeholder="Ajouter un commentaire..."
              style={{ flex: 1, backgroundColor: '#0D0E12', border: '1px solid rgba(255,255,255,0.2)', borderRadius: 8, padding: '9px 12px', color: '#FFF', fontSize: 13 }}
              onKeyDown={e => e.key === 'Enter' && handleComment()}
            />
            <button onClick={handleComment} style={{ backgroundColor: '#E5A93C', color: '#000', border: 'none', padding: '9px 16px', borderRadius: 8, fontWeight: 800, fontSize: 13, cursor: 'pointer' }}>
              Envoyer
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WatchPostPage;
