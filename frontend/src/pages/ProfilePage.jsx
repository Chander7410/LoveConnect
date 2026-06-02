import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { apiUnavailableMessage, clearSession, mediaUrl } from '../services/api.js';

export default function ProfilePage() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ bio: '', education: '', profession: '', city: '', interests: '' });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const isAuthError = (err) => {
    const status = err.response?.status;
    return status === 401 || status === 403 || err.response?.data?.message === 'Current user not found';
  };

  const load = async () => {
    try {
      const { data } = await api.get('/profile/me');
      setProfile(data);
      setForm({ bio: data.bio || '', education: data.education || '', profession: data.profession || '', city: data.city || '', interests: [...(data.interests || [])].join(', ') });
    } catch (err) {
      const serverMessage = err.response?.data?.message;
      if (isAuthError(err)) {
        clearSession();
        setError('Your login session expired because the dev database was restarted. Please register or login again, then save your profile.');
      } else {
        setError(serverMessage || 'Unable to load profile. Please login/register again.');
      }
    }
  };
  useEffect(() => { load(); }, []);

  const save = async (event) => {
    event.preventDefault();
    setMessage('');
    setError('');
    setSaving(true);
    try {
      await api.put('/profile/me', { ...form, interests: form.interests.split(',').map((x) => x.trim()).filter(Boolean) });
      await load();
      setMessage('Profile saved successfully.');
      setTimeout(() => {
        clearSession();
        navigate('/login', { replace: true });
      }, 800);
    } catch (err) {
      if (!err.response) {
        setError(apiUnavailableMessage);
      } else if (isAuthError(err)) {
        clearSession();
        setError('Your login session expired. Please register or login again before saving your profile.');
      } else {
        setError(err.response?.data?.message || 'Profile save failed.');
      }
    } finally {
      setSaving(false);
    }
  };
  const upload = async (event, path) => {
    const file = event.target.files[0];
    if (!file) return;
    const data = new FormData();
    data.append('file', file);
    setMessage('');
    setError('');
    try {
      await api.post(path, data);
      await load();
      setMessage('Photo uploaded successfully.');
    } catch (err) {
      setError(err.response?.data?.message || 'Photo upload failed.');
    }
  };
  const verify = async () => {
    setMessage('');
    setError('');
    try {
      const { data } = await api.post('/profile/verify');
      setProfile(data);
      setMessage(data.user?.verified ? 'Verification badge activated.' : 'Verification needs more profile details.');
    } catch (err) {
      setError(err.response?.data?.message || 'Verification request failed.');
    }
  };

  return (
    <div className="container py-4 page-transition">
      <p className="eyebrow">Identity studio</p>
      <h2>Profile</h2>
      <div className="row g-4">
        <div className="col-lg-4">
          <div className="surface p-3 profile-studio-card">
            <div className="avatar-preview" style={{ backgroundImage: `url(${mediaUrl(profile?.user?.profilePictureUrl, '/placeholder.svg')})` }}></div>
            <label className="form-label mt-3">Profile picture</label>
            <input className="form-control" type="file" accept="image/*" onChange={(e) => upload(e, '/profile/picture')} />
            <button className="btn btn-outline-primary w-100 mt-3" type="button" onClick={verify}>
              {profile?.user?.verified ? 'Verified profile' : 'Request verification badge'}
            </button>
            <div className="small text-muted mt-2">Fake profile score: {profile?.user?.fakeProfileScore ?? 0}/100</div>
            <label className="form-label mt-3">More photos</label>
            <input className="form-control" type="file" accept="image/*" onChange={(e) => upload(e, '/profile/photos')} />
            <div className="profile-gallery mt-3">
              {(profile?.photoUrls || []).slice(0, 4).map((url) => (
                <div className="gallery-item" key={url} style={{ backgroundImage: `url(${mediaUrl(url)})` }}></div>
              ))}
            </div>
          </div>
        </div>
        <div className="col-lg-8">
          <form className="surface p-4" onSubmit={save}>
            {message && <div className="alert alert-success">{message}</div>}
            {error && <div className="alert alert-danger">{error}</div>}
            <label className="form-label">Bio</label><textarea className="form-control mb-3" rows="4" value={form.bio} onChange={(e) => setForm({ ...form, bio: e.target.value })}></textarea>
            <div className="row g-3">
              <div className="col-md-6"><label className="form-label">Education</label><input className="form-control" value={form.education} onChange={(e) => setForm({ ...form, education: e.target.value })} /></div>
              <div className="col-md-6"><label className="form-label">Profession</label><input className="form-control" value={form.profession} onChange={(e) => setForm({ ...form, profession: e.target.value })} /></div>
              <div className="col-md-6"><label className="form-label">City</label><input className="form-control" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} /></div>
              <div className="col-md-6"><label className="form-label">Interests</label><input className="form-control" value={form.interests} onChange={(e) => setForm({ ...form, interests: e.target.value })} /></div>
            </div>
            <button className="btn btn-rose mt-4" disabled={saving}>{saving ? 'Saving...' : 'Save profile'}</button>
          </form>
        </div>
      </div>
    </div>
  );
}
