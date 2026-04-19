// src/utils/eventoTime.ts
type EventoLike = {
  startTime?: string | null;
  endTime?: string | null;
  inicio?: string | null;
  fim?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  data?: string | null;
  date?: string | null;
  end?: string | null;
};

export function parseEventoDate(raw: unknown): Date | null {
  if (raw == null) return null;

  if (raw instanceof Date) {
    return Number.isNaN(raw.getTime()) ? null : raw;
  }

  if (typeof raw === "number") {
    const d = new Date(raw);
    return Number.isNaN(d.getTime()) ? null : d;
  }

  if (typeof raw !== "string") return null;
  const text = raw.trim();
  if (!text) return null;

  // Backend pode retornar LocalDateTime sem offset (UTC "naive").
  const hasExplicitZone = /(?:Z|[+-]\d{2}:\d{2})$/i.test(text);
  const candidate = hasExplicitZone ? text : `${text}Z`;
  const d = new Date(candidate);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function getEventoStart(e: EventoLike): Date | null {
  const raw = e?.startTime || e?.inicio || e?.dataInicio || e?.data || e?.date || null;
  return parseEventoDate(raw);
}

export function getEventoEnd(e: EventoLike): Date | null {
  const raw = e?.endTime || e?.fim || e?.dataFim || e?.end || null;
  return parseEventoDate(raw);
}

export const START_WINDOW_MINUTES = 45;

export function canStartNow(e: EventoLike): boolean {
  return canStartNowAt(e, new Date());
}

export function canStartNowAt(e: EventoLike, now: Date): boolean {
  const start = getEventoStart(e);
  if (!start) return false;

  const end = getEventoEnd(e);
  if (end && now.getTime() > end.getTime()) return false;

  return now.getTime() >= start.getTime() - START_WINDOW_MINUTES * 60_000;
}

export function startCountdownText(e: EventoLike, now = new Date()): string {
  const start = getEventoStart(e);
  const end = getEventoEnd(e);
  if (!start) return "Sem data";
  if (end && now.getTime() > end.getTime()) return "Evento já terminou";
  if (canStartNowAt(e, now)) return "Disponível para iniciar";

  const startWindow = start.getTime() - START_WINDOW_MINUTES * 60_000;
  const msUntilStartWindow = startWindow - now.getTime();
  if (msUntilStartWindow <= 0) return "Disponível para iniciar";

  const totalMinutes = Math.ceil(msUntilStartWindow / 60_000);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (hours > 0) return `Disponível em ${hours}h ${minutes}min`;
  if (totalMinutes <= 1) return "Disponível em menos de 1 min";
  return `Disponível em ${totalMinutes}min`;
}
