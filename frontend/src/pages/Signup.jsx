import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthToast from '../components/AuthToast.jsx';
import api, { apiUnavailableMessage } from '../services/api.js';

const initialForm = {
  name: '',
  email: '',
  password: '',
  confirmPassword: ''
};

const retryDelays = [5000, 10000, 15000];
const retryableStatuses = new Set([408, 425, 429, 500, 502, 503, 504]);

const wait = (delay) => new Promise((resolve) => window.setTimeout(resolve, delay));

const shouldRetry = (error) => !error.response || retryableStatuses.has(error.response.status);

export default function Signup() {
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState({ type: '', message: '' });

  const change = (key, value) => setForm((current) => ({ ...current, [key]: value }));

  const validate = () => {
    if (!form.name.trim()) return 'Full Name is required';
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) return 'Enter a valid Gmail / Email ID';
    if (form.password.length < 8 || !/[A-Za-z]/.test(form.password) || !/[0-9]/.test(form.password)) {
      return 'Password must be at least 8 characters and include letters and numbers';
    }
    if (form.password !== form.confirmPassword) return 'Password and Confirm Password must match';
    return '';
  };

  const sendOtp = async (event) => {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setToast({ type: 'error', message: validationError });
      return;
    }
    setLoading(true);
    setToast({ type: '', message: '' });
    try {
      const payload = { ...form, email: form.email.trim().toLowerCase() };
      let response;
      for (let attempt = 0; attempt <= retryDelays.length; attempt += 1) {
        try {
          response = await api.post('/auth/register', payload, { timeout: 45000 });
          break;
        } catch (error) {
          if (!shouldRetry(error) || attempt === retryDelays.length) throw error;
          setToast({
            type: 'info',
            message: 'The signup server is starting. Your OTP request will retry automatically.'
          });
          await wait(retryDelays[attempt]);
        }
      }
      const { data } = response;
      sessionStorage.setItem('loveconnect_pending_signup', JSON.stringify(payload));
      sessionStorage.setItem('loveconnect_signup_email', payload.email);
      navigate('/signup/verify-otp', {
        state: {
          email: payload.email,
          message: data.message
        }
      });
    } catch (err) {
      setToast({
        type: 'error',
        message: err.response?.data?.message
          || `${apiUnavailableMessage} The free backend may still be starting; please try again in one minute.`
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container auth-shell py-5">
      <div className="auth-card surface slide-up">
        <p className="eyebrow">Create account</p>
        <h2>Join LoveConnect</h2>
        <AuthToast type={toast.type} message={toast.message} />
        <form onSubmit={sendOtp}>
          <div className="form-floating mb-3">
            <input id="signupName" className="form-control" placeholder="Full Name" value={form.name} onChange={(e) => change('name', e.target.value)} required />
            <label htmlFor="signupName">Full Name</label>
          </div>
          <div className="form-floating mb-3">
            <input id="signupEmail" className="form-control" placeholder="Gmail / Email ID" type="email" value={form.email} onChange={(e) => change('email', e.target.value)} required />
            <label htmlFor="signupEmail">Gmail / Email ID</label>
          </div>
          <div className="form-floating mb-3">
            <input id="signupPassword" className="form-control" placeholder="Password" type="password" minLength="8" value={form.password} onChange={(e) => change('password', e.target.value)} required />
            <label htmlFor="signupPassword">Password</label>
          </div>
          <div className="form-floating mb-3">
            <input id="signupConfirmPassword" className="form-control" placeholder="Confirm Password" type="password" minLength="8" value={form.confirmPassword} onChange={(e) => change('confirmPassword', e.target.value)} required />
            <label htmlFor="signupConfirmPassword">Confirm Password</label>
          </div>
          <button className="btn btn-rose w-100" type="submit" disabled={loading}>{loading ? 'Sending OTP...' : 'Send OTP'}</button>
          <div className="text-center mt-3">
            <Link to="/login">Already have an account? Login</Link>
          </div>
        </form>
      </div>
    </div>
  );
}
