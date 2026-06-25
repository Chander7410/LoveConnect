import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthToast from '../components/AuthToast.jsx';
import api, { apiUnavailableMessage } from '../services/api.js';

export default function ForgotOtpVerify() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = useMemo(() => location.state?.email || sessionStorage.getItem('loveconnect_forgot_email') || '', [location.state]);
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
      const { data } = await api.post('/auth/forgot-password/verify-otp', { email, otp });
      navigate('/forgot-password/reset', { state: { email, message: data.message || 'OTP verified successfully' } });
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || 'Invalid or expired OTP' });
    } finally {
      setLoading(false);
    }
  };

  const resendOtp = async () => {
    setLoading(true);
    try {
      const { data } = await api.post('/auth/forgot-password/send-otp', { email });
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
        <p className="eyebrow">Verify recovery</p>
        <h2>Forgot password OTP</h2>
        <AuthToast type={toast.type} message={toast.message} />
        <form onSubmit={verifyOtp}>
          <div className="form-floating mb-3">
            <input id="forgotOtpEmail" className="form-control" placeholder="Email" type="email" value={email} readOnly />
            <label htmlFor="forgotOtpEmail">Gmail / Email ID</label>
          </div>
          <div className="form-floating mb-3">
            <input id="forgotOtp" className="form-control" placeholder="OTP" inputMode="numeric" pattern="[0-9]{6}" maxLength="6" value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} required />
            <label htmlFor="forgotOtp">6 digit OTP</label>
          </div>
          <button className="btn btn-rose w-100" type="submit" disabled={loading}>{loading ? 'Verifying...' : 'Verify OTP'}</button>
          <button className="btn btn-outline-dark w-100 mt-3" type="button" onClick={resendOtp} disabled={loading || resendSeconds > 0}>
            {resendSeconds > 0 ? `Resend OTP in ${resendSeconds}s` : 'Resend OTP'}
          </button>
          <div className="text-center mt-3">
            <Link to="/forgot-password">Change email</Link>
          </div>
        </form>
      </div>
    </div>
  );
}
