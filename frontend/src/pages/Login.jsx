import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthToast from '../components/AuthToast.jsx';
import api, { apiUnavailableMessage, saveSession } from '../services/api.js';

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState({ type: location.state?.message ? 'success' : '', message: location.state?.message || '' });

  const login = async (event) => {
    event.preventDefault();
    setLoading(true);
    setToast({ type: '', message: '' });
    try {
      const { data } = await api.post('/auth/login', {
        email: form.email.trim().toLowerCase(),
        password: form.password,
        rememberMe: true
      });
      saveSession(data, true);
      navigate('/home');
    } catch (err) {
      setToast({ type: 'error', message: err.response ? (err.response.data?.message || 'Invalid email or password') : apiUnavailableMessage });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container auth-shell py-5">
      <div className="auth-card surface slide-up">
        <p className="eyebrow">Secure access</p>
        <h2>Welcome back</h2>
        <AuthToast type={toast.type} message={toast.message} />
        <form onSubmit={login}>
          <div className="form-floating mb-3">
            <input id="loginEmail" className="form-control" placeholder="Gmail / Email ID" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
            <label htmlFor="loginEmail">Gmail / Email ID</label>
          </div>
          <div className="form-floating mb-3">
            <input id="loginPassword" className="form-control" placeholder="Password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
            <label htmlFor="loginPassword">Password</label>
          </div>
          <button className="btn btn-rose w-100" type="submit" disabled={loading}>{loading ? 'Logging in...' : 'Login'}</button>
          <div className="d-flex justify-content-between mt-3 small">
            <Link to="/signup">Create account</Link>
            <Link to="/forgot-password">Forgot password?</Link>
          </div>
        </form>
      </div>
    </div>
  );
}
