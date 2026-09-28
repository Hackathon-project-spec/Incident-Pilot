import axios from 'axios';

const api = axios.create({
  baseURL: '/api', // Vite proxy forwards to backend
  headers: {
    'Content-Type': 'application/json',
  },
});

export const analyzeIncident = (payload: any) =>
  api.post('/incidents/analyze', payload).then(res => res.data);

export const compareIncident = (payload: any) =>
  api.post('/incidents/compare', payload).then(res => res.data);

export const diagnoseLegacy = (payload: any) =>
  api.post('/incidents/diagnose', payload).then(res => res.data);

export const submitPostmortem = (payload: any) =>
  api.post('/postmortems', payload).then(res => res.data);

export const demoReset = () =>
  api.post('/demo/reset').then(res => res.data);

export const demoSeedMemory = () =>
  api.post('/demo/seed-memory').then(res => res.data);

export const demoSeedAll = () =>
  api.post('/demo/seed-all').then(res => res.data);

export const demoSimilarIncident = (payload: any) =>
  api.post('/demo/similar-incident', payload).then(res => res.data);

// Demo comparison – GET version used for a quick one‑click demo
export const demoGetComparison = () =>
  api.get('/demo/compare').then(res => res.data);
