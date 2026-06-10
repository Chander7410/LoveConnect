import React, { useEffect, useState } from 'react';
import ProfileCard from '../components/ProfileCard.jsx';
import api, { apiUnavailableMessage, clearSession } from '../services/api.js';

const genderOptions = [
  { value: '', label: 'Any gender', accent: 'All' },
  { value: 'FEMALE', label: 'Female', accent: 'F' },
  { value: 'MALE', label: 'Male', accent: 'M' },
  { value: 'NON_BINARY', label: 'Non-binary', accent: 'NB' },
  { value: 'OTHER', label: 'Other', accent: 'O' }
];

const demoMatches = [
  {
    matchScore: 96,
    commonInterests: ['Marathi culture', 'Travel', 'Family values'],
    user: {
      id: 'demo-aarya',
      name: 'Aarya Patil',
      age: 27,
      gender: 'FEMALE',
      location: 'New Jersey, USA',
      verified: true,
      online: true,
      fakeProfileScore: 10,
      profilePictureUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=900&q=85'
    }
  },
  {
    matchScore: 91,
    commonInterests: ['Ganesh Utsav', 'Fitness', 'Startups'],
    user: {
      id: 'demo-rohan',
      name: 'Rohan Deshmukh',
      age: 30,
      gender: 'MALE',
      location: 'Dallas, USA',
      verified: true,
      online: false,
      fakeProfileScore: 8,
      profilePictureUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=900&q=85'
    }
  },
  {
    matchScore: 88,
    commonInterests: ['Classical music', 'Food', 'Hiking'],
    user: {
      id: 'demo-sayali',
      name: 'Sayali Jadhav',
      age: 25,
      gender: 'FEMALE',
      location: 'California, USA',
      verified: false,
      online: true,
      fakeProfileScore: 12,
      profilePictureUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=900&q=85'
    }
  }
];

export default function SearchPage() {
  const [filters, setFilters] = useState({ minAge: 21, maxAge: 45, gender: '', city: 'New Jersey', interest: 'Marathi culture' });
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
        setError(`${apiUnavailableMessage} Showing Maratha USA demo matches for preview.`);
        setMatches(demoMatches);
      } else if (err.response.status === 401 || err.response.status === 403) {
        clearSession();
        setError('Please login again before searching matches.');
        setMatches(demoMatches);
      } else {
        setError(`${err.response?.data?.message || 'Search failed. Please try again.'} Showing preview profiles.`);
        setMatches(demoMatches);
      }
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  return (
    <div className="container py-4 page-transition love-search-page">
      <div className="search-hero mb-4">
        <div>
          <p className="eyebrow">Maratha USA discovery</p>
          <h2>Search matches</h2>
          <p className="search-subtitle">Find Marathi and Maratha singles by age, gender, US city, and shared interests.</p>
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
          <div className="filter-field"><label className="form-label">City</label><input className="form-control" placeholder="New Jersey" value={filters.city} onChange={(e) => setFilters({ ...filters, city: e.target.value })} /></div>
          <div className="filter-field"><label className="form-label">Interest</label><input className="form-control" placeholder="Marathi culture, travel..." value={filters.interest} onChange={(e) => setFilters({ ...filters, interest: e.target.value })} /></div>
        </div>
      </div>
      <div className="search-results-bar mb-3">
        <span>{matches.length} profiles found</span>
        <span>Maratha USA recommendations refresh instantly</span>
      </div>
      <div className="profile-grid">
        {matches.map((match) => <ProfileCard key={match.user.id} match={match} onReact={load} />)}
        {matches.length === 0 && <div className="surface empty-state p-4">No profiles yet. Adjust filters or add more users.</div>}
      </div>
    </div>
  );
}
