import api from './api';

export const adminService = {
  async listManagers() {
    const response = await api.get('/api/admin/managers');
    return response.data;
  },

  async listEmployees() {
    const response = await api.get('/api/admin/employees');
    return response.data;
  },

  async createManager(data) {
    const response = await api.post('/api/admin/managers', data);
    return response.data;
  },

  async listUsers(role = '') {
    const url = role ? `/api/admin/users?role=${encodeURIComponent(role)}` : '/api/admin/users';
    const response = await api.get(url);
    return response.data;
  },

  async createUser(data) {
    const response = await api.post('/api/admin/users', data);
    return response.data;
  },

  async toggleUserStatus(id) {
    const response = await api.patch(`/api/admin/users/${id}/toggle-status`);
    return response.data;
  },
};

export default adminService;
