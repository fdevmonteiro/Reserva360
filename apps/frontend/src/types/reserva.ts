// src/types/reserva.ts

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELED';

export interface SalaBasic {
  id: number;
  nome: string;
}

export interface ZoomAccountBasic {
  id?: number;
  email: string;
}

export interface Reserva {
  id: number;

  title: string;
  description?: string | null;

  // ISO strings vindas do back (LocalDateTime serializado)
  startTime: string;
  endTime: string;

  organizerEmail: string;
  createdByUsername: string;

  // Sala vinculada
  sala: SalaBasic;

  // Status & token de confirmação
  status: ReservationStatus;
  confirmationToken?: string | null;

  // Flags e contagem
  needsSlideClicker?: boolean;
  createZoomMeeting?: boolean;
  participantCount?: number;

  // Zoom
  zoomMeetingId?: number | null; // campo no JSON vira camelCase via getter getZoomMeetingId()
  link?: string | null;          // join_url
  startUrl?: string | null;      // start_url (host)
  passcode?: string | null;

  // Conta Zoom host (se atribuída)
  hostZoomAccount?: ZoomAccountBasic | null;
}
