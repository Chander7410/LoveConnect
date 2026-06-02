import React, { useEffect, useState } from 'react';
import ProfileCard from '../components/ProfileCard.jsx';
import api from '../services/api.js';

export default function MatchesPage() {
  const [mutualMatches, setMutualMatches] = useState([]);
  const [recommended, setRecommended] = useState([]);
  const [likes, setLikes] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const load = async () => {
    setError('');
    setLoading(true);
    try {
      const [matches, rec, received] = await Promise.all([
        api.get('/likes/matches'),
        api.get('/search/recommendations'),
        api.get('/likes/received')
      ]);
      setMutualMatches(matches.data);
      setRecommended(rec.data);
      setLikes(received.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load matches. Please login again.');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  return (
      <div className="container py-4 page-transition">
        <p className="eyebrow">Recommendation engine</p>
      <h2>Matches</h2>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="search-results-bar mb-3">
        <span>{mutualMatches.length} matched people</span>
        <span>{loading ? 'Loading matches...' : 'Mutual likes become real matches'}</span>
      </div>
      <h5 className="mt-4">Matched people</h5>
      <div className="profile-grid mb-5">{mutualMatches.map((match) => <ProfileCard key={match.user.id} match={match} onReact={load} />)}{mutualMatches.length === 0 && <div className="surface empty-state p-4">When both people like each other, they will show here.</div>}</div>
      <h5 className="mt-4">Recommended</h5>
      <div className="profile-grid mb-5">{recommended.map((match) => <ProfileCard key={match.user.id} match={match} onReact={load} />)}{recommended.length === 0 && <div className="surface empty-state p-4">Recommendations will appear after profiles share interests.</div>}</div>
      <h5>Received likes</h5>
      <div className="profile-grid">{likes.map((match) => <ProfileCard key={match.user.id} match={match} onReact={load} />)}{likes.length === 0 && <div className="surface empty-state p-4">Received likes will show here.</div>}</div>
    </div>
  );
}
