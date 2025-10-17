import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  timeout: 10000, // 10 second timeout
  headers: {
    'Content-Type': 'application/json',
  },
});

// Note: JWT token is handled via HTTP-only cookies by the backend
// No need to manually add Authorization header

// Auth API
export const authApi = {
  register: (email: string, password: string) =>
    apiClient.post('/auth/register', { email, password }),
  
  login: (email: string, password: string) =>
    apiClient.post('/auth/login', { email, password }),
  
  logout: () =>
    apiClient.post('/auth/logout'),
};

// Rooms API
export const roomsApi = {
  getRooms: () =>
    apiClient.get('/api/rooms'),
  
  createRoom: () =>
    apiClient.post('/api/rooms'),
  
  joinRoom: (roomId: string) =>
    apiClient.post(`/api/rooms/${roomId}/join`),
  
  leaveRoom: (roomId: string) =>
    apiClient.post(`/api/rooms/${roomId}/leave`),
};

// Game API
export const gameApi = {
  getGameState: (gameId: string) =>
    apiClient.get(`/api/games/${gameId}`),
  
  attack: (gameId: string, actionId: string, turnNumber: number, target: { r: number; c: number }) =>
    apiClient.post(`/api/games/${gameId}/action/attack`, {
      actionId,
      turnNumber,
      type: 'ATTACK',
      target,
    }),
  
  requestSuggestion: (gameId: string) =>
    apiClient.post(`/api/games/${gameId}/suggest`),
};


