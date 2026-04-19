// src/services/dashboardService.ts
import api from './api';

export interface EventDashboardStats {
  totalEventosFuturos: number;
  totalOrganizadores: number;
   totalEventosPassados: number;
}


export const getEventDashboardStats = async (): Promise<EventDashboardStats> => {
  const response = await api.get('/dashboard/event-stats');
  return response.data;
};