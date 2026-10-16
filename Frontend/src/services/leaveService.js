
import api from './api';

export const leaveService = {
  async submitLeave(leaveData) {
    const response = await api.post('/api/leaves', leaveData);
    return response.data;
  },

  
async getMyLeaves() {
  const response = await api.get('/api/leaves/my');

  let data = response.data;

  // Unwrap nested arrays returned by the API.
  while (
    Array.isArray(data) &&
    data.length === 1 &&
    Array.isArray(data[0])
  ) {
    data = data[0];
  }

  if (!Array.isArray(data)) {
    return [];
  }

  return data.map((leave) => {
    // Support records already returned as objects.
    if (!Array.isArray(leave)) {
      return leave;
    }

    const [
      id,
      reason,
      status,
      startDate,
      endDate,
      daysCount,
      ragDecision,
      createdAt,
      employeeId,
      leaveType,
    ] = leave;

    return {
      id,
      reason,
      status,
      startDate,
      endDate,
      daysCount,
      ragDecision,
      createdAt,
      employeeId,
      leaveType,
    };
  });
}
,

  async getMyBalance() {
    const response = await api.get('/api/leaves/balance');
    return response.data;
  },

  async getPendingLeaves() {
    const response = await api.get('/api/leaves/pending');
    return response.data;
  },

  async approveLeave(id, comment = '') {
    const response = await api.post(`/api/leaves/${id}/approve`, { comment });
    return response.data;
  },

  async rejectLeave(id, comment) {
    const response = await api.post(`/api/leaves/${id}/reject`, { comment });
    return response.data;
  },

  async executeTokenAction(token) {
    const response = await api.post(
      `/api/token/${token}/action`,
      {},
      {
        headers: {
          Accept: 'text/html, application/json',
        },
      }
    );

    return response.data;
  },
};

export default leaveService;
