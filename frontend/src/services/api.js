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
    ? 'https://loveconnect-mddv.onrender.com/api'
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
  if (config.url?.startsWith('/auth/')) return config;
  const token = localStorage.getItem('loveconnect_token') || sessionStorage.getItem('loveconnect_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const saveSession = (auth, remember) => {
  const storage = remember ? localStorage : sessionStorage;
  storage.setItem('loveconnect_token', auth.token);
  storage.setItem('loveconnect_user', JSON.stringify(auth.user));
};

export const clearSession = () => {
  localStorage.removeItem('loveconnect_token');
  localStorage.removeItem('loveconnect_user');
  sessionStorage.removeItem('loveconnect_token');
  sessionStorage.removeItem('loveconnect_user');
};

export const currentUser = () => {
  const value = localStorage.getItem('loveconnect_user') || sessionStorage.getItem('loveconnect_user');
  return value ? JSON.parse(value) : null;
};

export const mediaUrl = (url, fallback = '') => {
  if (!url) return fallback;
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url;
  return `${API_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`;
};

export default api;
