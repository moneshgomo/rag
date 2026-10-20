import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 20000,
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('finflow_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle session expiration cleanly
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Do not redirect if we are already on an auth page or making an auth request
      const isAuthPath = window.location.pathname === '/login' || 
                         window.location.pathname === '/signup' || 
                         window.location.pathname === '/setup-password';
      const isAuthRequest = error.config && error.config.url && error.config.url.includes('/api/auth/');
      
      if (!isAuthPath && !isAuthRequest) {
        localStorage.removeItem('finflow_token');
        localStorage.removeItem('finflow_user');
        window.location.href = '/login?expired=true';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
