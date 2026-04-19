// src/services/salaService.ts

import api from './api'; 
import { Sala } from '@/types/sala'; 


export const getAvailableSalas = async (startTime: string, endTime: string): Promise<Sala[]> => {
  // Envia as datas como parâmetros de query para o endpoint
  const response = await api.get('/api/salas/disponiveis', {
    params: {
      startTime,
      endTime,
    },
  });
  return response.data;
};