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
};

export default authService;
