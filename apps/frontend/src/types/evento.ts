// types/evento.ts
export type RecurrenceDTO = {
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  interval: number;          // a cada X (semanas, por enquanto)
  weekDays: number[];        // 1..7 (1=Seg ... 7=Dom)
  endType: 'NEVER' | 'UNTIL_DATE' | 'AFTER_OCCURRENCES';
  untilDate?: string | null; // 'YYYY-MM-DD' quando UNTIL_DATE
  maxOccurrences?: number | null; // quando AFTER_OCCURRENCES
};

export type EventoFormData = {
  title: string;
  description?: string;
  startTime: string;        // datetime-local
  endTime: string;          // datetime-local
  participantCount: number;
  organizerEmail: string;
  coHostEmail?: string;
  registrationRequired: boolean;
  // UI-only
  isRecurring: boolean;
  recurrence?: RecurrenceDTO | null;
};

export type Evento = {
  id: number;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  parentEventId?: number | { id?: number } | null;
  status?: string;
  confirmationToken?: string;
  participantCount?: number;
  organizerEmail?: string;
  createdByUsername?: string;
  coHostEmail?: string;
  registrationRequired?: boolean;
};
