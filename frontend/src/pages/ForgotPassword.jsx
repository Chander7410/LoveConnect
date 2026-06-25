import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthToast from '../components/AuthToast.jsx';
import api, { apiUnavailableMessage } from '../services/api.js';

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState({ type: '', message: '' });

  const sendOtp = async (event) => {
    event.preventDefault();
    setLoading(true);
    setToast({ type: '', message: '' });
    try {
      const normalizedEmail = email.trim().toLowerCase();
      const { data } = await api.post('/auth/forgot-password/send-otp', { email: normalizedEmail });
      sessionStorage.setItem('loveconnect_forgot_email', normalizedEmail);
      navigate('/forgot-password/verify-otp', {
        state: {
          email: normalizedEmail,
          message: data.message
        }
      });
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || apiUnavailableMessage });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container auth-shell py-5">
      <div className="auth-card surface slide-up">
        <p className="eyebrow">Account recovery</p>
        <h2>Forgot password</h2>
        <AuthToast type={toast.type} message={toast.message} />
        <form onSubmit={sendOtp}>
          <div className="form-floating mb-3">
            <input id="forgotEmail" className="form-control" placeholder="Gmail / Email ID" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            <label htmlFor="forgotEmail">Gmail / Email ID</label>
          </div>
          <button className="btn btn-rose w-100" type="submit" disabled={loading}>{loading ? 'Sending OTP...' : 'Send OTP'}</button>
          <div className="text-center mt-3">
            <Link to="/login">Back to login</Link>
          </div>
        </form>
      </div>
    </div>
  );
}
