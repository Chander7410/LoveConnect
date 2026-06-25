import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthToast from '../components/AuthToast.jsx';
import api, { apiUnavailableMessage } from '../services/api.js';

export default function SignupOtpVerify() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = useMemo(() => location.state?.email || sessionStorage.getItem('loveconnect_signup_email') || '', [location.state]);
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendSeconds, setResendSeconds] = useState(60);
  const [toast, setToast] = useState({ type: location.state?.message ? 'success' : '', message: location.state?.message || '' });

  useEffect(() => {
    if (resendSeconds <= 0) return undefined;
    const timer = window.setTimeout(() => setResendSeconds((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [resendSeconds]);

  const verifyOtp = async (event) => {
    event.preventDefault();
    setLoading(true);
    setToast({ type: '', message: '' });
    try {
      const { data } = await api.post('/auth/verify-signup-otp', { email, otp });
      sessionStorage.removeItem('loveconnect_pending_signup');
      sessionStorage.removeItem('loveconnect_signup_email');
      navigate('/login', { state: { message: data.message || 'Signup completed successfully. Please login.' } });
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || 'Invalid or expired OTP' });
    } finally {
      setLoading(false);
    }
  };

  const resendOtp = async () => {
    const saved = sessionStorage.getItem('loveconnect_pending_signup');
    if (!saved) {
      setToast({ type: 'error', message: 'Signup details expired. Please start signup again.' });
      return;
    }
    setLoading(true);
    try {
      const pending = JSON.parse(saved);
      const { data } = await api.post('/auth/resend-signup-otp', { email: pending.email });
      setToast({ type: 'success', message: data.message || 'OTP sent again' });
      setResendSeconds(60);
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || apiUnavailableMessage });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container auth-shell py-5">
      <div className="auth-card surface slide-up">
        <p className="eyebrow">Verify signup</p>
        <h2>Email OTP</h2>
        <AuthToast type={toast.type} message={toast.message} />
        <form onSubmit={verifyOtp}>
          <div className="form-floating mb-3">
            <input id="signupOtpEmail" className="form-control" placeholder="Email" type="email" value={email} readOnly />
            <label htmlFor="signupOtpEmail">Gmail / Email ID</label>
          </div>
          <div className="form-floating mb-3">
            <input id="signupOtp" className="form-control" placeholder="OTP" inputMode="numeric" pattern="[0-9]{6}" maxLength="6" value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} required />
            <label htmlFor="signupOtp">6 digit OTP</label>
          </div>
          <button className="btn btn-rose w-100" type="submit" disabled={loading}>{loading ? 'Verifying...' : 'Verify OTP'}</button>
          <button className="btn btn-outline-dark w-100 mt-3" type="button" onClick={resendOtp} disabled={loading || resendSeconds > 0}>
            {resendSeconds > 0 ? `Resend OTP in ${resendSeconds}s` : 'Resend OTP'}
          </button>
          <div className="text-center mt-3">
            <Link to="/signup">Change signup details</Link>
          </div>
        </form>
      </div>
    </div>
  );
}
