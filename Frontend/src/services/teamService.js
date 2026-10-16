import api from './api';

export const teamService = {
  async listTeams() {
    const response = await api.get('/api/teams');
    return response.data;
  },

  async getTeam(id) {
    const response = await api.get(`/api/teams/${id}`);
    return response.data;
  },

  async createTeam(data) {
    const response = await api.post('/api/teams', data);
    return response.data;
  },

  async addMemberToTeam(teamId, memberData) {
    const response = await api.post(`/api/teams/${teamId}/members`, memberData);
    return response.data;
  },

  async listTeamMembers(teamId) {
    const response = await api.get(`/api/teams/${teamId}/members`);
    return response.data;
  },
};

export default teamService;
