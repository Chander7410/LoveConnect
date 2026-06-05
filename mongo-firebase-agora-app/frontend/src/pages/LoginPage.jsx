import { useState } from 'react';
import { Heart, KeyRound, Mail, Phone, UserRound } from 'lucide-react';
import { endpoints } from '../services/api.js';
import { firebaseReady, requestOtp, setupRecaptcha } from '../services/firebase.js';

export default function LoginPage({ onLogin, initialError }) {
  const [phoneNumber, setPhoneNumber] = useState('+91');
  const [otp, setOtp] = useState('');
  const [confirmation, setConfirmation] = useState(null);
  const [authMode, setAuthMode] = useState(firebaseReady() ? 'phone' : 'email');
  const [isRegistering, setIsRegistering] = useState(false);
  const [emailForm, setEmailForm] = useState({
    name: '',
    email: '',
    mobileNumber: '+91',
    gender: 'MALE',
    age: '25',
    location: 'Pune',
    password: ''
  });
  const [devToken, setDevToken] = useState('dev:+919999999999');
  const [message, setMessage] = useState(initialError || '');
  const enableDev = import.meta.env.VITE_ENABLE_DEV_TOKEN === 'true';

  function updateEmailField(field, value) {
    setEmailForm((current) => ({ ...current, [field]: value }));
  }

  async function sendOtp(event) {
    event.preventDefault();
    setMessage('');
    try {
      const verifier = setupRecaptcha('recaptcha-container');
      const result = await requestOtp(phoneNumber, verifier);
      setConfirmation(result);
      setMessage('OTP sent. Check the phone number in Firebase Authentication.');
    } catch (err) {
      setMessage(err.message);
    }
  }

  async function verifyOtp(event) {
    event.preventDefault();
    setMessage('');
    try {
      const credential = await confirmation.confirm(otp);
      const token = await credential.user.getIdToken();
      await onLogin(token);
    } catch (err) {
      setMessage(err.message);
    }
  }

  async function submitEmailAuth(event) {
    event.preventDefault();
    setMessage('');
    try {
      const payload = isRegistering
        ? {
            ...emailForm,
            age: Number(emailForm.age)
          }
        : {
            email: emailForm.email,
            password: emailForm.password,
            rememberMe: true
          };
      const response = isRegistering ? await endpoints.register(payload) : await endpoints.login(payload);
      await onLogin(response.data.token);
    } catch (err) {
      setMessage(err.response?.data?.message || err.message);
    }
  }

  return (
    <main className="login">
      <section className="loginCard">
        <div className="brand large">
          <Heart fill="currentColor" />
          <span>LoveConnect</span>
        </div>
        <h1>{authMode === 'phone' ? 'Phone OTP login for secure dating profiles' : 'MongoDB login for live dating profiles'}</h1>
        <p className="muted">Access secure Spring Boot APIs backed by MongoDB Atlas.</p>

        <div className="segmented">
          <button className={authMode === 'email' ? 'active' : ''} type="button" onClick={() => setAuthMode('email')}>
            <Mail size={17} /> Email
          </button>
          <button className={authMode === 'phone' ? 'active' : ''} type="button" onClick={() => setAuthMode('phone')} disabled={!firebaseReady()}>
            <Phone size={17} /> Phone
          </button>
        </div>

        {!firebaseReady() && authMode === 'phone' && <p className="error">Firebase frontend config is missing. Use email login for MongoDB live testing.</p>}
        {message && <p className="notice">{message}</p>}

        {authMode === 'email' ? (
          <form className="form" onSubmit={submitEmailAuth}>
            {isRegistering && (
              <>
                <label>
                  Name
                  <div className="inputIcon">
                    <UserRound size={18} />
                    <input value={emailForm.name} onChange={(event) => updateEmailField('name', event.target.value)} placeholder="Your name" />
                  </div>
                </label>
                <div className="formRow">
                  <label>
                    Age
                    <input type="number" min="18" value={emailForm.age} onChange={(event) => updateEmailField('age', event.target.value)} />
                  </label>
                  <label>
                    Gender
                    <select value={emailForm.gender} onChange={(event) => updateEmailField('gender', event.target.value)}>
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </label>
                </div>
                <div className="formRow">
                  <label>
                    Mobile
                    <input value={emailForm.mobileNumber} onChange={(event) => updateEmailField('mobileNumber', event.target.value)} placeholder="+919876543210" />
                  </label>
                  <label>
                    Location
                    <input value={emailForm.location} onChange={(event) => updateEmailField('location', event.target.value)} placeholder="Pune" />
                  </label>
                </div>
              </>
            )}
            <label>
              Email
              <div className="inputIcon">
                <Mail size={18} />
                <input type="email" value={emailForm.email} onChange={(event) => updateEmailField('email', event.target.value)} placeholder="you@example.com" />
              </div>
            </label>
            <label>
              Password
              <div className="inputIcon">
                <KeyRound size={18} />
                <input type="password" value={emailForm.password} onChange={(event) => updateEmailField('password', event.target.value)} placeholder="Minimum 8 characters" />
              </div>
            </label>
            <button className="primary" type="submit">{isRegistering ? 'Create account' : 'Login'}</button>
            <button className="secondary" type="button" onClick={() => setIsRegistering((value) => !value)}>
              {isRegistering ? 'Use existing account' : 'Create a new account'}
            </button>
          </form>
        ) : (
          <form className="form" onSubmit={confirmation ? verifyOtp : sendOtp}>
            <label>
              Mobile number
              <div className="inputIcon">
                <Phone size={18} />
                <input value={phoneNumber} onChange={(event) => setPhoneNumber(event.target.value)} placeholder="+919876543210" />
              </div>
            </label>
            {confirmation && (
              <label>
                OTP
                <div className="inputIcon">
                  <KeyRound size={18} />
                  <input value={otp} onChange={(event) => setOtp(event.target.value)} placeholder="6 digit OTP" />
                </div>
              </label>
            )}
            <button className="primary" type="submit" disabled={!firebaseReady()}>
              {confirmation ? 'Verify OTP' : 'Send OTP'}
            </button>
            <div id="recaptcha-container" />
          </form>
        )}

        {enableDev && (
          <div className="devBox">
            <p className="muted">Development token login</p>
            <input value={devToken} onChange={(event) => setDevToken(event.target.value)} />
            <button className="secondary" onClick={() => onLogin(devToken)}>Use dev token</button>
          </div>
        )}
      </section>
      <section className="loginArt">
        <div className="metric"><strong>Realtime</strong><span>Chat and call signaling</span></div>
        <div className="metric"><strong>Secure</strong><span>Firebase ID token APIs</span></div>
        <div className="metric"><strong>Atlas</strong><span>Cloud MongoDB storage</span></div>
      </section>
    </main>
  );
}
