import axios from 'axios';

export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
export const API_ORIGIN = API_BASE_URL.replace(/\/api\/?$/, '');
export const apiUnavailableMessage = `Backend API is not reachable at ${API_BASE_URL}. Set VITE_API_URL to your live backend URL and make sure the Spring Boot API is running.`;

const api = axios.create({
  baseURL: API_BASE_URL
});

api.interceptors.request.use((config) => {
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
