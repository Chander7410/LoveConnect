import axios from 'axios';

const normalizeApiUrl = (url) => {
  if (!url) return '';
  const trimmed = url.trim().replace(/\/+$/, '');
  return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`;
};

const isLocalFrontend = typeof window !== 'undefined' && ['localhost', '127.0.0.1'].includes(window.location.hostname);
const fallbackApiUrl = isLocalFrontend ? 'http://localhost:8080/api' : 'https://loveconnect-mddv.onrender.com/api';

export const API_URL = normalizeApiUrl(import.meta.env.VITE_API_URL || fallbackApiUrl);
export const API_ORIGIN = API_URL.replace(/\/api\/?$/, '');

let tokenProvider = async () => localStorage.getItem('loveconnect_token');

export function setTokenProvider(provider) {
  tokenProvider = provider;
}

export function saveToken(token) {
  localStorage.setItem('loveconnect_token', token);
}

export function clearToken() {
  localStorage.removeItem('loveconnect_token');
}

export const api = axios.create({
  baseURL: API_URL
});

api.interceptors.request.use(async (config) => {
  const token = await tokenProvider();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const endpoints = {
  health: () => api.get('/health'),
  register: (payload) => api.post('/auth/register', payload),
  login: (payload) => api.post('/auth/login', payload),
  me: () => api.get('/auth/me'),
  profiles: () => api.get('/profiles'),
  updateProfile: (payload) => api.put('/profiles/me', payload),
  chatHistory: (uid) => api.get(`/chats/${uid}`),
  sendMessage: (payload) => api.post('/chats/messages', payload),
  startCall: (payload) => api.post('/calls/start', payload),
  endCall: (callId, status = 'ENDED') => api.post(`/calls/${callId}/end`, null, { params: { status } }),
  callHistory: () => api.get('/calls/history')
};
