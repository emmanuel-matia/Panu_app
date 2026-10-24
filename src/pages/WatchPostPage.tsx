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
  const [isOwner, setIsOwner] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [imgError, setImgError] = useState(false);

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

      const { data: { session } } = await supabase.auth.getSession();
      const currentUid = session?.user?.id || currentUserId;
      setIsOwner(data.user_id === currentUid || data.author_id === currentUid);

      const { data: profile } = await supabase
        .from('profiles')
        .select('full_name, username, avatar_url')
        .eq('id', data.user_id || data.author_id)
        .maybeSingle();

      const enriched: WatchPost = {
        ...data,
        user_id: data.user_id || data.author_id,
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
  }, [id, currentUserId]);

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

  const handleSaveEdit = async () => {
    if (!post) return;
    const { error } = await supabase
      .from('posts')
      .update({ title: editTitle.trim(), content: editContent.trim() })
      .eq('id', post.id);
    if (error) {
      alert(`Erreur: ${error.message}`);
      return;
    }
    setPost({ ...post, title: editTitle.trim(), content: editContent.trim() });
    setEditing(false);
  };

  const handleDelete = async () => {
    if (!post) return;
    if (!confirm('Voulez-vous vraiment supprimer cette publication ? Cette action est irreversible.')) return;
    const { error } = await supabase.from('posts').delete().eq('id', post.id);
    if (error) {
      alert(`Erreur: ${error.message}`);
      return;
    }
    navigate('/');
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
        <button onClick={() => navigate(-1)} style={{ background: 'none', border: 'none', color: '#E5A93C', fontSize: 14, fontWeight: 700, cursor: 'pointer', marginBottom: 16 }}>
          ← Retour
        </button>

        {/* Auteur */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
          <div style={{ width: 44, height: 44, borderRadius: '50%', border: '2px solid #E5A93C', overflow: 'hidden', backgroundColor: '#1E202B', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 900, flexShrink: 0 }}>
            {post.author_avatar ? <img src={post.author_avatar} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : (post.author_name || 'P').charAt(0).toUpperCase()}
          </div>
          <span style={{ fontWeight: 800, fontSize: 15 }}>{post.author_name}</span>
          {isOwner && (
            <div style={{ marginLeft: 'auto', display: 'flex', gap: 6 }}>
              <button onClick={() => { setEditTitle(post.title || ''); setEditContent(post.content || ''); setEditing(!editing); }}
                style={{ background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.2)', color: '#FFF', padding: '5px 10px', borderRadius: 6, fontSize: 11, fontWeight: 700, cursor: 'pointer' }}>
                {editing ? 'Annuler' : '✏️ Modifier'}
              </button>
              <button onClick={handleDelete}
                style={{ background: 'rgba(255,71,87,0.15)', border: '1px solid #FF4757', color: '#FF4757', padding: '5px 10px', borderRadius: 6, fontSize: 11, fontWeight: 700, cursor: 'pointer' }}>
                🗑️ Supprimer
              </button>
            </div>
          )}
        </div>

        {/* Edition mode */}
        {editing ? (
          <div style={{ backgroundColor: '#15161E', borderRadius: 14, padding: 16, marginBottom: 16, border: '1px solid rgba(229,169,60,0.3)' }}>
            <input type="text" value={editTitle} onChange={e => setEditTitle(e.target.value)} placeholder="Titre"
              style={{ width: '100%', background: '#0D0E12', border: '1px solid rgba(255,255,255,0.2)', borderRadius: 8, padding: '10px 12px', color: '#FFF', fontSize: 14, marginBottom: 10 }} />
            <textarea value={editContent} onChange={e => setEditContent(e.target.value)} placeholder="Contenu" rows={4}
              style={{ width: '100%', background: '#0D0E12', border: '1px solid rgba(255,255,255,0.2)', borderRadius: 8, padding: '10px 12px', color: '#FFF', fontSize: 14, marginBottom: 10, resize: 'vertical' }} />
            <button onClick={handleSaveEdit}
              style={{ background: '#E5A93C', color: '#000', border: 'none', borderRadius: 8, padding: '10px 20px', fontWeight: 800, fontSize: 13, cursor: 'pointer' }}>
              Enregistrer
            </button>
          </div>
        ) : (
          <>
            {/* Média */}
            {post.media_url && post.media_type === 'video' && (
              <video src={post.media_url} controls autoPlay loop playsInline
                style={{ width: '100%', borderRadius: 16, maxHeight: '70vh', objectFit: 'cover', backgroundColor: '#000' }} />
            )}
            {post.media_url && post.media_type === 'image' && !imgError && (
              <img src={post.media_url} alt={post.title || 'Publication'}
                style={{ width: '100%', borderRadius: 16, maxHeight: '70vh', objectFit: 'contain', backgroundColor: '#000' }}
                onError={() => setImgError(true)} />
            )}
            {post.media_url && post.media_type === 'image' && imgError && (
              <div style={{ width: '100%', borderRadius: 16, padding: '40px 20px', textAlign: 'center', backgroundColor: '#15161E', color: '#888', border: '1px solid rgba(255,255,255,0.1)' }}>
                <div style={{ fontSize: 32, marginBottom: 8 }}>📷</div>
                <p style={{ fontSize: 13 }}>Cette image n'a pas pu etre chargee. Le lien est peut-etre expire ou invalide.</p>
              </div>
            )}
            {!post.media_url && (
              <div style={{ width: '100%', borderRadius: 16, padding: '40px 20px', textAlign: 'center', backgroundColor: 'rgba(229,169,60,0.05)', border: '1px solid rgba(229,169,60,0.2)', color: '#DDD', fontSize: 18, fontStyle: 'italic', marginBottom: 16 }}>
                📝 Publication texte
              </div>
            )}

            {/* Titre & description */}
            {post.title && <h2 style={{ fontSize: 18, fontWeight: 800, marginTop: 16, marginBottom: 4 }}>{post.title}</h2>}
            {post.content && <p style={{ fontSize: 14, color: '#CCC', marginBottom: 16, lineHeight: 1.5 }}>{post.content}</p>}
          </>
        )}

        {/* Actions */}
        {!editing && (
          <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 20, flexWrap: 'wrap' }}>
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
        )}

        {/* Commentaires */}
        {!editing && (
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
        )}
      </div>
    </div>
  );
};

export default WatchPostPage;
