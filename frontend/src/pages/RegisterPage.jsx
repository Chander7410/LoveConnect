import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { API_BASE_URL, apiUnavailableMessage, saveSession } from '../services/api.js';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', mobileNumber: '', gender: 'FEMALE', age: 18, location: '', password: '' });
  const [error, setError] = useState('');

  const change = (key, value) => setForm({ ...form, [key]: value });
  const submit = async (event) => {
    event.preventDefault();
    setError('');
    if (!API_BASE_URL) {
      setError(apiUnavailableMessage);
      return;
    }
    try {
      const { data } = await api.post('/auth/register', { ...form, age: Number(form.age) });
      saveSession(data, true);
      navigate('/profile');
    } catch (err) {
      if (!err.response) {
        setError(apiUnavailableMessage);
        return;
      }
      if (err.response.status === 404 || err.response.status === 405) {
        setError(`Backend API is not configured correctly. Current API URL is ${API_BASE_URL || 'empty'}, but registration is reaching a static frontend route. Set VITE_API_URL to your live Spring Boot backend /api URL.`);
        return;
      }
      setError(err.response?.data?.message || `Registration failed with status ${err.response.status}. Please check the backend API URL and entered details.`);
    }
  };

  return (
    <div className="container py-5 page-transition">
      <div className="row justify-content-center">
        <div className="col-lg-8">
          <p className="eyebrow">Premium onboarding</p>
          <h2>Create your profile</h2>
          <form className="surface p-4" onSubmit={submit}>
            {error && <div className="alert alert-danger">{error}</div>}
            <div className="row g-3">
              <div className="col-md-6 form-floating"><input id="regName" className="form-control" placeholder="Name" required value={form.name} onChange={(e) => change('name', e.target.value)} /><label htmlFor="regName">Name</label></div>
              <div className="col-md-6 form-floating"><input id="regEmail" className="form-control" placeholder="Email" type="email" required value={form.email} onChange={(e) => change('email', e.target.value)} /><label htmlFor="regEmail">Email</label></div>
              <div className="col-md-6 form-floating"><input id="regMobile" className="form-control" placeholder="Mobile" required value={form.mobileNumber} onChange={(e) => change('mobileNumber', e.target.value)} /><label htmlFor="regMobile">Mobile</label></div>
              <div className="col-md-3"><label className="form-label">Gender</label><select className="form-select" value={form.gender} onChange={(e) => change('gender', e.target.value)}><option>FEMALE</option><option>MALE</option><option>NON_BINARY</option><option>OTHER</option></select></div>
              <div className="col-md-3 form-floating"><input id="regAge" className="form-control" placeholder="Age" type="number" min="18" max="100" value={form.age} onChange={(e) => change('age', e.target.value)} /><label htmlFor="regAge">Age</label></div>
              <div className="col-md-6 form-floating"><input id="regLocation" className="form-control" placeholder="Location" required value={form.location} onChange={(e) => change('location', e.target.value)} /><label htmlFor="regLocation">Location</label></div>
              <div className="col-md-6 form-floating"><input id="regPassword" className="form-control" placeholder="Password" type="password" required minLength="8" value={form.password} onChange={(e) => change('password', e.target.value)} /><label htmlFor="regPassword">Password</label></div>
            </div>
            <button className="btn btn-rose mt-4">Register</button>
          </form>
        </div>
      </div>
    </div>
  );
}
