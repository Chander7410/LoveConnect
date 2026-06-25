import React, { useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthToast from '../components/AuthToast.jsx';
import api, { apiUnavailableMessage } from '../services/api.js';

export default function ResetPassword() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = useMemo(() => location.state?.email || sessionStorage.getItem('loveconnect_forgot_email') || '', [location.state]);
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState({ type: location.state?.message ? 'success' : '', message: location.state?.message || '' });

  const resetPassword = async (event) => {
    event.preventDefault();
    if (form.newPassword !== form.confirmPassword) {
      setToast({ type: 'error', message: 'New Password and Confirm Password must match' });
      return;
    }
    setLoading(true);
    try {
      const { data } = await api.post('/auth/forgot-password/reset', { email, ...form });
      sessionStorage.removeItem('loveconnect_forgot_email');
      navigate('/login', { state: { message: data.message || 'Password updated. Please login.' } });
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || apiUnavailableMessage });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container auth-shell py-5">
      <div className="auth-card surface slide-up">
        <p className="eyebrow">New password</p>
        <h2>Reset password</h2>
        <AuthToast type={toast.type} message={toast.message} />
        <form onSubmit={resetPassword}>
          <div className="form-floating mb-3">
            <input id="resetEmail" className="form-control" placeholder="Email" type="email" value={email} readOnly />
            <label htmlFor="resetEmail">Gmail / Email ID</label>
          </div>
          <div className="form-floating mb-3">
            <input id="newPassword" className="form-control" placeholder="New Password" type="password" minLength="8" value={form.newPassword} onChange={(e) => setForm({ ...form, newPassword: e.target.value })} required />
            <label htmlFor="newPassword">New Password</label>
          </div>
          <div className="form-floating mb-3">
            <input id="confirmPassword" className="form-control" placeholder="Confirm Password" type="password" minLength="8" value={form.confirmPassword} onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })} required />
            <label htmlFor="confirmPassword">Confirm Password</label>
          </div>
          <button className="btn btn-rose w-100" type="submit" disabled={loading}>{loading ? 'Updating...' : 'Update password'}</button>
          <div className="text-center mt-3">
            <Link to="/login">Back to login</Link>
          </div>
        </form>
      </div>
    </div>
  );
}
