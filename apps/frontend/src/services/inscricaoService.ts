import axios from 'axios';
import api from './api';

export type InscricaoRequest = {
  nome: string;
  email: string;
  telefone?: string;
  nomeUnidade: string;
  cargo?: string;
};

export type InscricaoResponse = {
  mensagem: string;
  eventoTitulo: string;
  joinUrl: string | null;
};

export type Inscricao = {
  id: number;
  nome: string;
  email: string;
  telefone?: string;
  nomeUnidade?: string;
  cargo?: string;
  createdAt: string;
};

export type EventoInfoPublico = {
  id: number;
  titulo: string;
  descricao?: string;
  startTime: string;
  endTime: string;
  organizerEmail: string;
  status: string;
};

const publicApi = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL });

/** Busca informações públicas do evento para a página de inscrição (sem autenticação). */
export async function getEventoInfoPublico(eventoId: number): Promise<EventoInfoPublico> {
  const { data } = await publicApi.get(`/inscricoes/evento/${eventoId}/info`);
  return data;
}

/** Realiza inscrição no evento (sem autenticação). */
export async function inscreverNoEvento(
  eventoId: number,
  payload: InscricaoRequest
): Promise<InscricaoResponse> {
  const { data } = await publicApi.post(`/inscricoes/evento/${eventoId}`, payload);
  return data;
}

/** Lista inscritos de um evento (autenticado). */
export async function listarInscritos(eventoId: number): Promise<Inscricao[]> {
  const { data } = await api.get(`/inscricoes/evento/${eventoId}`);
  return data;
}

/** Conta inscritos de um evento (autenticado). */
export async function contarInscritos(eventoId: number): Promise<number> {
  const { data } = await api.get(`/inscricoes/evento/${eventoId}/count`);
  return data?.total ?? 0;
}

/** Baixa a planilha Excel com os inscritos (autenticado). */
export async function downloadInscritosExcel(eventoId: number, eventoTitulo: string): Promise<void> {
  const response = await api.get(`/inscricoes/evento/${eventoId}/export/excel`, {
    responseType: 'blob',
  });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `inscritos-${eventoTitulo.replace(/\s+/g, '-')}.xlsx`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
