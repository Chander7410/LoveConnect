import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { apiUnavailableMessage, saveSession } from '../services/api.js';

export default function LoginPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '', rememberMe: true });
  const [forgotOpen, setForgotOpen] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [resetForm, setResetForm] = useState({ token: '', newPassword: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

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
      setError(err.response?.data?.message || 'Login failed');
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
