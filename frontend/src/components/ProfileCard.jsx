import React, { useState } from 'react';
import { BadgeCheck, Ban, Flag, Heart, MapPin, Sparkles, Star, X } from 'lucide-react';
import api, { mediaUrl } from '../services/api.js';

export default function ProfileCard({ match, onReact }) {
  const user = match.user;
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [error, setError] = useState('');
  const fallbackPhoto = user.gender === 'MALE'
    ? 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=900&q=85'
    : 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=900&q=85';
  const like = async (liked) => {
    if (String(user.id).startsWith('demo-')) {
      setFeedback(liked ? 'Liked preview profile' : 'Passed preview profile');
      return;
    }
    setBusy(true);
    setError('');
    setFeedback('');
    try {
      await api.post('/likes', { targetUserId: user.id, liked });
      setFeedback(liked ? 'Liked' : 'Passed');
      onReact?.();
    } catch (err) {
      setError(err.response?.data?.message || 'Action failed. Please login and try again.');
    } finally {
      setBusy(false);
    }
  };
  const superLike = async () => {
    if (String(user.id).startsWith('demo-')) {
      setFeedback('Super Like sent to preview profile');
      return;
    }
    setBusy(true);
    setError('');
    setFeedback('');
    try {
      await api.post('/likes', { targetUserId: user.id, liked: true, superLike: true });
      setFeedback('Super Like sent');
      onReact?.();
    } catch (err) {
      setError(err.response?.data?.message || 'Super Like failed. Please login and try again.');
    } finally {
      setBusy(false);
    }
  };
  const report = async () => {
    if (String(user.id).startsWith('demo-')) {
      setFeedback('Preview profile report noted');
      return;
    }
    const details = window.prompt('Why are you reporting this profile?', 'Fake profile or unsafe behavior');
    if (!details) return;
    setBusy(true);
    setError('');
    setFeedback('');
    try {
      await api.post('/safety/reports', { reportedUserId: user.id, reason: 'Profile report', details });
      setFeedback('Report sent');
    } catch (err) {
      setError(err.response?.data?.message || 'Report failed.');
    } finally {
      setBusy(false);
    }
  };
  const block = async () => {
    if (String(user.id).startsWith('demo-')) {
      setFeedback('Preview profile hidden');
      return;
    }
    setBusy(true);
    setError('');
    setFeedback('');
    try {
      await api.post(`/safety/blocks/${user.id}`);
      setFeedback('User blocked');
      onReact?.();
    } catch (err) {
      setError(err.response?.data?.message || 'Block failed.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <article className="profile-card match-card">
      <div className="profile-photo match-photo" style={{ backgroundImage: `url(${mediaUrl(user.profilePictureUrl, fallbackPhoto)})` }}>
        <span className="score-badge">{match.matchScore}% match</span>
      </div>
      <div className="profile-card-body">
        <div className="d-flex justify-content-between align-items-start">
          <div>
            <h5 className="mb-1">{user.name}, {user.age} {user.verified && <BadgeCheck size={18} className="verified-badge" />}</h5>
            <p className="match-location mb-2"><MapPin size={15} /> {user.location} · {user.gender}</p>
            <p className="community-line mb-2">Maratha community · USA based</p>
            {user.fakeProfileScore >= 60 && <p className="safety-warning mb-2">Safety review suggested</p>}
          </div>
          <span className={`match-status ${user.online ? 'online' : ''}`}>{user.online ? 'Online' : 'Offline'}</span>
        </div>
        <div className="d-flex flex-wrap gap-2 mb-3">
          {match.commonInterests?.map((interest) => <span className="interest-chip" key={interest}>{interest}</span>)}
          {(!match.commonInterests || match.commonInterests.length === 0) && <span className="interest-chip neutral"><Sparkles size={13} /> Discover interests</span>}
        </div>
        <div className="match-actions">
          <button className="btn like-button flex-fill" disabled={busy} onClick={() => like(true)}><Heart size={18} fill="currentColor" /> Like</button>
          <button className="btn btn-outline-primary flex-fill" disabled={busy} onClick={superLike}><Star size={18} fill="currentColor" /> Super</button>
          <button className="btn dislike-button flex-fill" disabled={busy} onClick={() => like(false)}><X size={18} /> Pass</button>
        </div>
        <div className="match-actions mt-2">
          <button className="btn btn-sm btn-outline-danger flex-fill" disabled={busy} onClick={report}><Flag size={15} /> Report</button>
          <button className="btn btn-sm btn-outline-dark flex-fill" disabled={busy} onClick={block}><Ban size={15} /> Block</button>
        </div>
        {feedback && <div className="match-feedback success">{feedback}</div>}
        {error && <div className="match-feedback error">{error}</div>}
      </div>
    </article>
  );
}
