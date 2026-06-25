import axios from 'axios';

const normalizeApiUrl = (url) => {
  if (!url) return '';
  const trimmed = url.trim().replace(/\/+$/, '');
  return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`;
};

const isLocalFrontend = typeof window !== 'undefined' && ['localhost', '127.0.0.1'].includes(window.location.hostname);
const isVercelFrontend = typeof window !== 'undefined' && window.location.hostname === 'love-connect-beta.vercel.app';
const runtimeApiUrl = typeof window !== 'undefined' ? localStorage.getItem('loveconnect_api_url') : '';
const fallbackApiUrl = isLocalFrontend
  ? 'http://localhost:8080/api'
  : isVercelFrontend
    ? ''
    : '';

export const API_BASE_URL = normalizeApiUrl(runtimeApiUrl || import.meta.env.VITE_API_URL || fallbackApiUrl);
export const API_ORIGIN = API_BASE_URL.replace(/\/api\/?$/, '');
export const apiUnavailableMessage = API_BASE_URL
  ? `Backend API is not reachable at ${API_BASE_URL}. Make sure the Spring Boot API is running and CORS allows this frontend domain.`
  : 'Backend API URL is not configured. Set VITE_API_URL in deployment settings.';

export const saveRuntimeApiUrl = (url) => {
  const normalized = normalizeApiUrl(url);
  if (normalized) {
    localStorage.setItem('loveconnect_api_url', normalized);
  } else {
    localStorage.removeItem('loveconnect_api_url');
  }
  window.location.reload();
};

const api = axios.create({
  baseURL: API_BASE_URL
});

api.interceptors.request.use((config) => {
  if (!API_BASE_URL) {
    throw new Error(apiUnavailableMessage);
  }
  const publicAuthPaths = [
    '/auth/register',
    '/auth/login',
    '/auth/google',
    '/auth/send-otp',
    '/auth/verify-otp',
    '/auth/forgot-password',
    '/auth/reset-password',
    '/auth/send-signup-otp',
    '/auth/verify-signup-otp',
    '/auth/resend-signup-otp',
    '/auth/complete-signup',
    '/auth/signup/send-otp',
    '/auth/signup/verify-otp',
    '/auth/verify-login-otp',
    '/auth/forgot-password/send-otp',
    '/auth/forgot-password/verify-otp',
    '/auth/forgot-password/reset',
    '/auth/resend-otp'
  ];
  if (publicAuthPaths.includes(config.url)) return config;
  const token = localStorage.getItem('loveconnect_token') || sessionStorage.getItem('loveconnect_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const saveSession = (auth, remember) => {
  const storage = remember ? localStorage : sessionStorage;
  storage.setItem('loveconnect_token', auth.token);
  storage.setItem('loveconnect_user', JSON.stringify(auth.user));
  if (auth.user?.provider === 'GOOGLE' && auth.user?.email) {
    storage.setItem('loveconnect_google_otp_email', auth.user.email);
  }
  window.dispatchEvent(new Event('loveconnect:sessionChanged'));
};

export const clearSession = () => {
  localStorage.removeItem('loveconnect_token');
  localStorage.removeItem('loveconnect_user');
  localStorage.removeItem('loveconnect_google_otp_email');
  sessionStorage.removeItem('loveconnect_token');
  sessionStorage.removeItem('loveconnect_user');
  sessionStorage.removeItem('loveconnect_google_otp_email');
  window.dispatchEvent(new Event('loveconnect:sessionChanged'));
};

export const currentUser = () => {
  const value = localStorage.getItem('loveconnect_user') || sessionStorage.getItem('loveconnect_user');
  if (!value) return null;
  const user = JSON.parse(value);
  const googleOtpEmail = localStorage.getItem('loveconnect_google_otp_email') || sessionStorage.getItem('loveconnect_google_otp_email');
  if (user.provider === 'GOOGLE' && googleOtpEmail !== user.email) {
    clearSession();
    return null;
  }
  return user;
};

export const mediaUrl = (url, fallback = '') => {
  if (!url) return fallback;
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url;
  return `${API_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`;
};

export default api;
