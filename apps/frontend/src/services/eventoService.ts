// src/services/eventoService.ts

import api from './api';
import type { Evento, EventoFormData } from '@/types/evento';

function toIsoDateTime(value: string): string {
  if (!value) return value;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toISOString();
}

function normalizeEventoPayload(data: EventoFormData): EventoFormData {
  return {
    ...data,
    startTime: toIsoDateTime(data.startTime),
    endTime: toIsoDateTime(data.endTime),
  };
}
// Busca todos os eventos
export const getEventos = async (): Promise<Evento[]> => {
  const response = await api.get('/eventos');
  return response.data;
};

// Cria um novo evento
export const createEvento = async (data: EventoFormData): Promise<Evento> => {
  const response = await api.post('/eventos', normalizeEventoPayload(data));
  return response.data;
};

// Atualiza um evento existente
export const updateEvento = async (id: number, data: EventoFormData): Promise<Evento> => {
  const response = await api.put(`/eventos/${id}`, normalizeEventoPayload(data));
  return response.data;
};

export const getUpcomingEventos = async (): Promise<Evento[]> => {
  const response = await api.get('/eventos/proximos');
  return response.data;
};

// Deleta um evento
export const deleteEvento = async (id: number): Promise<void> => {
  await api.delete(`/eventos/${id}`);
};

// Deleta série recorrente inteira (pai + ocorrências)
export const deleteEventoSerie = async (id: number): Promise<void> => {
  await api.delete(`/eventos/${id}/serie`);
};

export const getPastEventos = async (): Promise<Evento[]> => {
  const response = await api.get('/eventos/passados');
  return response.data;
};

export const getMyEventos = async (): Promise<Evento[]> => {
  const { data } = await api.get("/eventos/me");
  return data;
};

export const getMyPastEventos = async (): Promise<Evento[]> => {
  const { data } = await api.get("/eventos/me/past");
  return data;
};


// src/services/eventoService.ts

type StartUrlResponse = { startUrl?: string; error?: string };
type RegistrantsErrorResponse = { error?: string };
export type MeetingRegistrant = {
  id?: string;
  name: string;
  email: string;
  status: string;
  joinUrl?: string;
  registeredAt?: string;
};

export async function getStartUrl(eventoId: number): Promise<string> {
  const { data } = await api.get<StartUrlResponse>(`/eventos/${eventoId}/start-url`);
  const { startUrl, error } = data ?? {};
  if (!startUrl) {
    throw new Error(error ?? "startUrl indisponível");
  }
  return startUrl;
}

export async function getMeetingRegistrants(eventoId: number, status = "approved"): Promise<MeetingRegistrant[]> {
  const { data } = await api.get<MeetingRegistrant[] | RegistrantsErrorResponse>(
    `/eventos/${eventoId}/registrants`,
    { params: { status } }
  );

  if (!Array.isArray(data)) {
    throw new Error(data?.error ?? "Não foi possível carregar inscritos.");
  }
  return data;
}

// ── Relatórios ──────────────────────────────────────────────────────────────

export type EventoReport = {
  id?: number;
  meetingUuid?: string;
  topic?: string;
  actualStartTime?: string;
  actualEndTime?: string;
  durationMinutes?: number;
  totalParticipants?: number;
  peakParticipants?: number;
  totalMinutes?: number;
  recordingUrl?: string;
  recordingStatus?: string;
  generatedAt?: string;
  emailed?: boolean;
  emailedAt?: string;
};

export async function getEventoReport(eventoId: number): Promise<EventoReport> {
  const { data } = await api.get<EventoReport>(`/eventos/${eventoId}/report`);
  return data;
}

export async function generateEventoReport(eventoId: number): Promise<EventoReport> {
  const { data } = await api.post<EventoReport>(`/eventos/${eventoId}/report`);
  return data;
}

export async function downloadReportPdf(eventoId: number, titulo: string): Promise<void> {
  const response = await api.get(`/eventos/${eventoId}/report/pdf`, { responseType: "blob" });
  const url = window.URL.createObjectURL(new Blob([response.data], { type: "application/pdf" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = `relatorio-${titulo.replace(/\s+/g, "-").toLowerCase()}.pdf`;
  link.click();
  window.URL.revokeObjectURL(url);
}
