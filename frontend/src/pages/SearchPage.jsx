import React, { useEffect, useState } from 'react';
import ProfileCard from '../components/ProfileCard.jsx';
import api, { clearSession } from '../services/api.js';

const genderOptions = [
  { value: '', label: 'Any gender', accent: 'All' },
  { value: 'FEMALE', label: 'Female', accent: 'F' },
  { value: 'MALE', label: 'Male', accent: 'M' },
  { value: 'NON_BINARY', label: 'Non-binary', accent: 'NB' },
  { value: 'OTHER', label: 'Other', accent: 'O' }
];

export default function SearchPage() {
  const [filters, setFilters] = useState({ minAge: 18, maxAge: 60, gender: '', city: '', interest: '' });
  const [matches, setMatches] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [genderOpen, setGenderOpen] = useState(false);
  const selectedGender = genderOptions.find((option) => option.value === filters.gender) || genderOptions[0];
  const load = async () => {
    setError('');
    setLoading(true);
    try {
      const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ''));
      const { data } = await api.get('/search', { params });
      setMatches(data);
    } catch (err) {
      if (!err.response) {
        setError('Backend API is not reachable. Make sure Spring Boot is running on http://localhost:8080.');
      } else if (err.response.status === 401 || err.response.status === 403) {
        clearSession();
        setError('Please login again before searching matches.');
      } else {
        setError(err.response?.data?.message || 'Search failed. Please try again.');
      }
      setMatches([]);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  return (
    <div className="container py-4 page-transition love-search-page">
      <div className="search-hero mb-4">
        <div>
          <p className="eyebrow">Curated discovery</p>
          <h2>Search matches</h2>
          <p className="search-subtitle">Find compatible LoveConnect profiles by age, gender, city, and interests.</p>
        </div>
        <button className="btn btn-rose" onClick={load} disabled={loading}>{loading ? 'Searching...' : 'Search'}</button>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="surface search-filters mb-4">
        <div className="search-filter-grid">
          <div className="filter-field"><label className="form-label">Min age</label><input className="form-control" type="number" value={filters.minAge} onChange={(e) => setFilters({ ...filters, minAge: e.target.value })} /></div>
          <div className="filter-field"><label className="form-label">Max age</label><input className="form-control" type="number" value={filters.maxAge} onChange={(e) => setFilters({ ...filters, maxAge: e.target.value })} /></div>
          <div className="filter-field gender-filter-field">
            <label className="form-label">Gender</label>
            <div className={`gender-picker ${genderOpen ? 'open' : ''}`}>
              <button className="gender-picker-button" type="button" onClick={() => setGenderOpen((open) => !open)} aria-expanded={genderOpen}>
                <span className="gender-picker-badge">{selectedGender.accent}</span>
                <span>{selectedGender.label}</span>
              </button>
              {genderOpen && (
                <div className="gender-picker-menu">
                  {genderOptions.map((option) => (
                    <button
                      className={filters.gender === option.value ? 'active' : ''}
                      key={option.value || 'any'}
                      type="button"
                      onClick={() => {
                        setFilters({ ...filters, gender: option.value });
                        setGenderOpen(false);
                      }}
                    >
                      <span className="gender-picker-badge">{option.accent}</span>
                      <span>{option.label}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
          <div className="filter-field"><label className="form-label">City</label><input className="form-control" placeholder="Delhi" value={filters.city} onChange={(e) => setFilters({ ...filters, city: e.target.value })} /></div>
          <div className="filter-field"><label className="form-label">Interest</label><input className="form-control" placeholder="Music, travel..." value={filters.interest} onChange={(e) => setFilters({ ...filters, interest: e.target.value })} /></div>
        </div>
      </div>
      <div className="search-results-bar mb-3">
        <span>{matches.length} profiles found</span>
        <span>Premium recommendations refresh instantly</span>
      </div>
      <div className="profile-grid">
        {matches.map((match) => <ProfileCard key={match.user.id} match={match} onReact={load} />)}
        {matches.length === 0 && <div className="surface empty-state p-4">No profiles yet. Adjust filters or add more users.</div>}
      </div>
    </div>
  );
}
