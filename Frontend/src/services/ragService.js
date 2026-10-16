import api from './api';

export const ragService = {
  async askPolicy(question) {
    const response = await api.get(`/api/ask?q=${encodeURIComponent(question)}`);
    return response.data; // { question, answer }
  },

  async searchPolicy(term) {
    const response = await api.get(`/api/search?q=${encodeURIComponent(term)}`);
    return response.data; // [{ score, text }]
  },
};

export default ragService;
