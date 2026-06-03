import { useState } from 'react';
import { Heart, KeyRound, Phone } from 'lucide-react';
import { firebaseReady, requestOtp, setupRecaptcha } from '../services/firebase.js';

export default function LoginPage({ onLogin, initialError }) {
  const [phoneNumber, setPhoneNumber] = useState('+91');
  const [otp, setOtp] = useState('');
  const [confirmation, setConfirmation] = useState(null);
  const [devToken, setDevToken] = useState('dev:+919999999999');
  const [message, setMessage] = useState(initialError || '');
  const enableDev = import.meta.env.VITE_ENABLE_DEV_TOKEN === 'true';

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

  return (
    <main className="login">
      <section className="loginCard">
        <div className="brand large">
          <Heart fill="currentColor" />
          <span>LoveConnect</span>
        </div>
        <h1>Phone OTP login for secure dating profiles</h1>
        <p className="muted">Use Firebase Authentication for mobile verification, then access secure Spring Boot APIs backed by MongoDB Atlas.</p>

        {!firebaseReady() && <p className="error">Firebase frontend config is missing. Add VITE_FIREBASE_* values in frontend .env.</p>}
        {message && <p className="notice">{message}</p>}

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
