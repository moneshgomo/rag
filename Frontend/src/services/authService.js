import api from './api';

export const authService = {
  async login(credentials) {
    const response = await api.post('/api/auth/login', credentials);
    return response.data;
  },

  async register(data) {
    const response = await api.post('/api/auth/register', data);
    return response.data;
  },

  async getCurrentUser() {
    const response = await api.get('/api/auth/me');
    return response.data;
  },

  async validateSetupToken(token) {
    const response = await api.get(`/api/auth/setup-password/validate?token=${encodeURIComponent(token)}`);
    return response.data;
  },

  async setupPassword(data) {
    const response = await api.post('/api/auth/setup-password', data);
    return response.data;
  },

  async regenerateSetupLink(data) {
    const response = await api.post('/api/auth/regenerate-setup-link', data);
    return response.data;
  },
};

export default authService;
