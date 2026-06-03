import { initializeApp } from 'firebase/app';
import { getAuth, RecaptchaVerifier, signInWithPhoneNumber } from 'firebase/auth';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
};

const configured = Object.values(firebaseConfig).every(Boolean);
const app = configured ? initializeApp(firebaseConfig) : null;
export const auth = app ? getAuth(app) : null;

export function firebaseReady() {
  return Boolean(auth);
}

export function setupRecaptcha(containerId) {
  if (!auth) {
    throw new Error('Firebase config is missing. Add VITE_FIREBASE_* values.');
  }
  if (window.recaptchaVerifier) {
    return window.recaptchaVerifier;
  }
  window.recaptchaVerifier = new RecaptchaVerifier(auth, containerId, {
    size: 'invisible'
  });
  return window.recaptchaVerifier;
}

export async function requestOtp(phoneNumber, recaptchaVerifier) {
  return signInWithPhoneNumber(auth, phoneNumber, recaptchaVerifier);
}
