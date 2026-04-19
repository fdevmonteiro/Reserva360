// src/services/reservaService.ts

import { Reserva } from '@/types/reserva';
import api from "./api"

export type ReservaFormData = Omit<Reserva, 'id'>;


export const updateReserva = async (id: number | string, reservaData: ReservaFormData): Promise<Reserva> => {
  const response = await api.put(`/reservas/${id}`, reservaData);
  return response.data;
};

export const getReservas = async (page = 0, size = 50): Promise<Reserva[]> => {
  const response = await api.get('/reservas', { params: { page, size } });
  return response.data.content ?? response.data;
};

// 2. Criar uma nova reserva (POST)
// A tipagem Promise<Reserva> também usa a interface importada.
export const createReserva = async (reservaData: ReservaFormData): Promise<Reserva> => {
   const response = await api.post('/reservas', reservaData);
  return response.data;
};

// 3. Deletar uma reserva (DELETE)
export const deleteReserva = async (id: string | number) => {
  await api.delete(`/reservas/${id}`);
};

export const getUpcomingReservas = async (): Promise<Reserva[]> => {
  const response = await api.get('/reservas/proximas');
  return response.data;
};

  export const getMyReservas = async (): Promise<Reserva[]> => {
  const response = await api.get('/reservas/minhas');
  return response.data;
};


export async function getReservaStartUrl(id: number) {
  const { data } = await api.get<{ startUrl: string }>(`/reservas/${id}/start-url`);
  return data.startUrl;
}


export interface Filters {
  roomName?: string;
  selectedDate?: string; 
}
