import axios from 'axios';

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
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
  me: () => api.get('/auth/me'),
  profiles: () => api.get('/profiles'),
  updateProfile: (payload) => api.put('/profiles/me', payload),
  chatHistory: (uid) => api.get(`/chats/${uid}`),
  sendMessage: (payload) => api.post('/chats/messages', payload),
  startCall: (payload) => api.post('/calls/start', payload),
  endCall: (callId, status = 'ENDED') => api.post(`/calls/${callId}/end`, null, { params: { status } }),
  callHistory: () => api.get('/calls/history')
};
