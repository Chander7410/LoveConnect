import React, { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { apiUnavailableMessage, saveSession } from '../services/api.js';

export default function LoginPage() {
  const navigate = useNavigate();
  const googleButtonRef = useRef(null);
  const [form, setForm] = useState({ email: '', password: '', rememberMe: true });
  const [forgotOpen, setForgotOpen] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [resetForm, setResetForm] = useState({ token: '', newPassword: '' });
  const [otpForm, setOtpForm] = useState({ email: '', otp: '' });
  const [otpOpen, setOtpOpen] = useState(false);
  const [otpSending, setOtpSending] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
    || (window.location.hostname === 'love-connect-beta.vercel.app'
      ? '302420987503-59d0ip0l91asnb5crajh1tojos1j8pid.apps.googleusercontent.com'
      : '');

  useEffect(() => {
    if (!googleClientId || !googleButtonRef.current) return undefined;

    const renderGoogleButton = () => {
      if (!window.google?.accounts?.id || !googleButtonRef.current) return;
      googleButtonRef.current.innerHTML = '';
      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: handleGoogleCredential
      });
      window.google.accounts.id.renderButton(googleButtonRef.current, {
        theme: 'outline',
        size: 'large',
        width: googleButtonRef.current.offsetWidth || 320,
        text: 'continue_with'
      });
    };

    if (window.google?.accounts?.id) {
      renderGoogleButton();
      return undefined;
    }

    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = renderGoogleButton;
    document.head.appendChild(script);
    return () => {
      script.onload = null;
    };
  }, [googleClientId]);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    try {
      const { data } = await api.post('/auth/login', form);
      saveSession(data, form.rememberMe);
      navigate('/search');
    } catch (err) {
      if (!err.response) {
        setError(apiUnavailableMessage);
        return;
      }
      const message = err.response?.data?.message || 'Login failed';
      if (message === 'EMAIL_NOT_VERIFIED') {
        setOtpOpen(true);
        setOtpForm({ email: form.email, otp: '' });
        setError('Please verify your email with OTP before login.');
        return;
      }
      setError(message);
    }
  };

  const handleGoogleCredential = async (response) => {
    setError('');
    setSuccess('');
    try {
      const { data } = await api.post('/auth/google', {
        idToken: response.credential,
        rememberMe: form.rememberMe
      });
      saveSession(data, form.rememberMe);
      navigate('/search');
    } catch (err) {
      const message = err.response?.data?.message || 'Google login failed';
      if (message === 'EMAIL_NOT_VERIFIED') {
        const googleEmail = emailFromGoogleCredential(response.credential);
        setOtpOpen(true);
        setOtpForm({ email: googleEmail || form.email, otp: '' });
        setForm((current) => ({ ...current, email: googleEmail || current.email }));
        setError('Please verify your email with OTP before login.');
        return;
      }
      setError(message);
    }
  };

  const sendOtp = async (event) => {
    event?.preventDefault();
    setError('');
    setSuccess('');
    setOtpSending(true);
    try {
      const email = otpForm.email || form.email;
      const { data } = await api.post('/auth/send-otp', { email });
      setOtpOpen(true);
      setOtpForm((current) => ({ ...current, email }));
      setSuccess(data.message || 'OTP sent to your Gmail');
    } catch (err) {
      setError(err.response?.data?.message || 'Could not send OTP');
    } finally {
      setOtpSending(false);
    }
  };

  const verifyOtp = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    try {
      const { data } = await api.post('/auth/verify-otp', otpForm);
      if (data.token) {
        saveSession(data, form.rememberMe);
        navigate('/search');
        return;
      }
      setSuccess(data.message || 'OTP verified successfully');
      setOtpOpen(false);
      setForm((current) => ({ ...current, email: otpForm.email }));
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid or expired OTP');
    }
  };

  const emailFromGoogleCredential = (credential) => {
    try {
      const payload = JSON.parse(atob(credential.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return payload.email || '';
    } catch {
      return '';
    }
  };

  const requestReset = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    try {
      const { data } = await api.post('/auth/forgot-password', { email: forgotEmail });
      if (data.resetToken) {
        setResetForm((current) => ({ ...current, token: data.resetToken }));
      }
      setSuccess(data.message || 'Reset token created. Enter a new password below.');
    } catch (err) {
      setError(err.response?.data?.message || 'Could not start password reset.');
    }
  };

  const resetPassword = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    try {
      const { data } = await api.post('/auth/reset-password', resetForm);
      setSuccess(data.message || 'Password updated. You can login now.');
      setForm((current) => ({ ...current, email: forgotEmail, password: '' }));
      setForgotOpen(false);
      setResetForm({ token: '', newPassword: '' });
    } catch (err) {
      setError(err.response?.data?.message || 'Password reset failed.');
    }
  };

  return (
    <div className="container auth-shell py-5">
      <div className="auth-card surface slide-up">
      <p className="eyebrow">Secure access</p>
      <h2>Welcome back</h2>
      <form onSubmit={submit}>
        {error && <div className="alert alert-danger">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}
        <div className="form-floating mb-3">
          <input className="form-control" id="loginEmail" placeholder="Email" type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <label htmlFor="loginEmail">Email</label>
        </div>
        <div className="form-floating mb-3">
          <input className="form-control" id="loginPassword" placeholder="Password" type="password" required minLength="8" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <label htmlFor="loginPassword">Password</label>
        </div>
        <div className="form-check mb-3">
          <input id="remember" className="form-check-input" type="checkbox" checked={form.rememberMe} onChange={(e) => setForm({ ...form, rememberMe: e.target.checked })} />
          <label className="form-check-label" htmlFor="remember">Remember me</label>
        </div>
        <button className="btn btn-rose w-100">Login</button>
        <div className="auth-divider"><span>or</span></div>
        {googleClientId ? (
          <div className="google-login-button" ref={googleButtonRef}></div>
        ) : (
          <button className="btn btn-outline-dark w-100" type="button" disabled>Continue with Google</button>
        )}
        <button className="btn btn-outline-secondary w-100 mt-3" type="button" onClick={sendOtp} disabled={otpSending || !form.email}>
          {otpSending ? 'Sending OTP...' : 'Send email OTP'}
        </button>
        <div className="d-flex justify-content-between mt-3 small">
          <Link to="/register">Create account</Link>
          <button className="link-button" type="button" onClick={() => {
            setForgotOpen((open) => !open);
            setForgotEmail(form.email);
            setError('');
            setSuccess('');
          }}>Forgot password?</button>
        </div>
      </form>
      {otpOpen && (
        <div className="forgot-password-panel">
          <form onSubmit={verifyOtp}>
            <strong>Email OTP verification</strong>
            <p className="text-muted mb-3">Enter the 6 digit OTP sent to your Gmail.</p>
            <div className="form-floating mb-3">
              <input className="form-control" id="otpEmail" placeholder="Email" type="email" required value={otpForm.email} onChange={(e) => setOtpForm({ ...otpForm, email: e.target.value })} />
              <label htmlFor="otpEmail">Email</label>
            </div>
            <div className="form-floating mb-3">
              <input className="form-control" id="otpCode" placeholder="OTP" inputMode="numeric" pattern="[0-9]{6}" maxLength="6" required value={otpForm.otp} onChange={(e) => setOtpForm({ ...otpForm, otp: e.target.value.replace(/\D/g, '').slice(0, 6) })} />
              <label htmlFor="otpCode">6 digit OTP</label>
            </div>
            <div className="d-grid gap-2">
              <button className="btn btn-rose w-100" type="submit">Verify OTP</button>
              <button className="btn btn-outline-dark w-100" type="button" onClick={sendOtp} disabled={otpSending}>
                {otpSending ? 'Sending OTP...' : 'Resend OTP'}
              </button>
            </div>
          </form>
        </div>
      )}
      {forgotOpen && (
        <div className="forgot-password-panel">
          <form onSubmit={requestReset}>
            <strong>Reset your password</strong>
            <p className="text-muted mb-3">Enter your email to create a secure reset token.</p>
            <div className="form-floating mb-3">
              <input className="form-control" id="forgotEmail" placeholder="Email" type="email" required value={forgotEmail} onChange={(e) => setForgotEmail(e.target.value)} />
              <label htmlFor="forgotEmail">Account email</label>
            </div>
            <button className="btn btn-outline-dark w-100" type="submit">Get reset token</button>
          </form>
          <form className="mt-3" onSubmit={resetPassword}>
            <div className="form-floating mb-3">
              <input className="form-control" id="resetToken" placeholder="Reset token" required value={resetForm.token} onChange={(e) => setResetForm({ ...resetForm, token: e.target.value })} />
              <label htmlFor="resetToken">Reset token</label>
            </div>
            <div className="form-floating mb-3">
              <input className="form-control" id="newPassword" placeholder="New password" type="password" required minLength="8" value={resetForm.newPassword} onChange={(e) => setResetForm({ ...resetForm, newPassword: e.target.value })} />
              <label htmlFor="newPassword">New password</label>
            </div>
            <button className="btn btn-rose w-100" type="submit">Update password</button>
          </form>
        </div>
      )}
      </div>
    </div>
  );
}
