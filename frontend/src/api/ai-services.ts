import api from './axios';

// ─── AI Service calls ─────────────────────────────────────────
export const getAIDashboard = () =>
  api.get('/ai/dashboard');

export const getAIAnomalies = () =>
  api.get('/ai/anomalies');

export const getAIInsights = () =>
  api.get('/ai/insights');

export const getAIDelays = () =>
  api.get('/ai/delays');

export const getAIRouting = () =>
  api.get('/ai/routing');

export const getAIRoutingForDa = (daId: number) =>
  api.get(`/ai/routing/${daId}`);

export const getAIDecision = (daId: number) =>
  api.get(`/ai/decision/${daId}`);
