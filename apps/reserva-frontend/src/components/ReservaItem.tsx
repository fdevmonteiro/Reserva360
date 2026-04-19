// src/components/ReservaItem.tsx
import { useAuth } from '@/contexts/AuthContext';
import { Reserva } from '@/types/reserva';
import { Clock, MapPin, Trash2, User, FilePenLine, PlayCircle, Video } from 'lucide-react';
import { Button } from '@/components/ui/button';
import api from '@/services/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

type StartUrlResponse = { startUrl: string };

function isStartUrlResponse(x: unknown): x is StartUrlResponse {
  return typeof x === 'object'
    && x !== null
    && typeof (x as Record<string, unknown>).startUrl === 'string';
}

async function fetchStartUrl(reservaId: number | string): Promise<string> {
  const resp = await fetch(`/api/reservas/${reservaId}/start-url`, {
    method: 'GET',
    headers: { 'Accept': 'application/json' },   // força JSON
    credentials: 'include',                      // garante cookies/sessão
  });

  const ctype = resp.headers.get('content-type') || '';

  // Se não OK, tente extrair mensagem de erro (JSON ou texto)
  if (!resp.ok) {
    let msg = '';
    try {
      if (ctype.includes('application/json')) {
        const j = await resp.json();
        msg = (j && (j.message || j.error || j.detail)) ?? JSON.stringify(j);
      } else {
        msg = await resp.text();
      }
    } catch {
      msg = `HTTP ${resp.status}`;
    }
    throw new Error(msg || `HTTP ${resp.status}`);
  }

  // OK, mas garante que veio JSON (senão provavelmente é HTML de login/erro)
  if (!ctype.includes('application/json')) {
    const html = await resp.text();
    // Dica: se quiser logar pra depurar
    console.error('Resposta não-JSON recebida do /start-url:', html.slice(0, 200));
    throw new Error('Sessão expirada ou sem permissão (servidor respondeu HTML). Faça login e tente novamente.');
  }

  const data: unknown = await resp.json();
  if (!isStartUrlResponse(data)) {
    throw new Error('Resposta inesperada do servidor (start_url ausente).');
  }
  return data.startUrl;
}

async function createZoomForReserva(reservaId: number | string): Promise<void> {
  const resp = await fetch(`/api/reservas/${reservaId}/create-zoom`, {
    method: 'PUT',
    headers: { 'Accept': 'application/json' },
    credentials: 'include',
  });

  const ctype = resp.headers.get('content-type') || '';
  if (!resp.ok) {
    const msg = ctype.includes('application/json')
      ? (() => {
          try { return JSON.stringify(resp.json()); } catch { return ''; }
        })()
      : await resp.text();
    throw new Error(msg || `HTTP ${resp.status}`);
  }
}

// ---- Props ----
interface ReservaItemProps {
  reserva: Reserva;
  onDelete: (id: number | string) => void;
  onEdit: (reserva: Reserva) => void;
  // opcional: passe um onRefresh do pai para refetch após create-zoom
  onRefresh?: () => void;
}

const ReservaItem = ({ reserva, onDelete, onEdit, onRefresh }: ReservaItemProps) => {
  const { user } = useAuth();
  const isAdmin = user?.roles?.includes('ROLE_ADMIN') || user?.roles?.includes('ROLE_G.TI');
  const isOwner = user?.username === reserva.createdByUsername;
  const canManage = isAdmin || isOwner;

  const formatDateTime = (dateTime: string) => {
    if (!dateTime) return 'Data inválida';
    const date = new Date(dateTime);
    return date.toLocaleString('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  };

  const formatDateRange = () => {
    const inicio = formatDateTime(reserva.startTime);
    const fim = new Date(reserva.endTime).toLocaleTimeString('pt-BR', {
      hour: '2-digit', minute: '2-digit'
    });
    return `${inicio} - ${fim}`;
  };

  const meetingEnded = (() => {
    const end = new Date(reserva.endTime).getTime();
    if (Number.isNaN(end)) return false;
    return end <= Date.now();
  })();

  const handleDelete = () => {
    if (reserva.id && window.confirm('Tem certeza que deseja excluir esta reserva?')) {
      onDelete(reserva.id);
    }
  };

  const handleStartMeeting = async () => {
    try {
      let url: string | undefined;
      try {
        const { data } = await api.get<StartUrlResponse>(`/api/reservas/${reserva.id}/start-url`, {
          headers: { Accept: 'application/json' },
        });
        url = data?.startUrl;
      } catch (e) {
        // fallback para o fetch antigo (pode falhar sem JWT)
        url = await fetchStartUrl(reserva.id);
      }
      if (!url) throw new Error('startUrl indisponível');
      window.open(url, '_blank', 'noopener,noreferrer');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err);
      alert(message || 'Erro ao iniciar a reunião.');
      console.error(err);
    }
  };

  const handleCreateZoom = async () => {
    try {
      await createZoomForReserva(reserva.id);
      if (onRefresh) onRefresh();
      else alert('Reunião criada no Zoom. Atualize a lista para ver o botão de iniciar.');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err);
      alert(message || 'Erro ao criar reunião no Zoom.');
      console.error(err);
    }
  };

  return (
    <Card className="flex flex-col h-full hover:shadow-lg transition-shadow duration-300">
      <CardHeader className="pb-4">
        <div className="flex justify-between items-start gap-2">
          <CardTitle className="text-lg font-bold text-gray-800">{reserva.title}</CardTitle>

          {canManage && (
            <div className="flex items-center gap-1">
              {meetingEnded ? (
                <span className="text-xs text-gray-500 px-2 py-1 border border-gray-200 rounded-md bg-gray-50">
                  Reunião encerrada
                </span>
              ) : reserva.zoomMeetingId ? (
                <Button
                  size="sm"
                  onClick={handleStartMeeting}
                  className="bg-blue-600 hover:bg-blue-700 text-white"
                  title="Iniciar reunião (host)"
                >
                  <PlayCircle className="h-4 w-4 mr-2" />
                  Iniciar
                </Button>
              ) : (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={handleCreateZoom}
                  title="Criar reunião Zoom para esta reserva"
                >
                  <Video className="h-4 w-4 mr-2" />
                  Criar Zoom
                </Button>
              )}

              <Button
                variant="ghost"
                size="icon"
                onClick={() => onEdit(reserva)}
                className="text-gray-400 hover:text-blue-600 hover:bg-blue-50"
                title="Editar"
              >
                <FilePenLine className="h-4 w-4" />
              </Button>

              <Button
                variant="ghost"
                size="icon"
                onClick={handleDelete}
                className="text-gray-400 hover:text-red-600 hover:bg-red-50"
                title="Excluir"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
          )}
        </div>

        {reserva.description && (
          <p className="text-sm text-gray-600 pt-1">{reserva.description}</p>
        )}
      </CardHeader>

      <CardContent className="flex-grow space-y-3">
        <div className="flex items-center text-sm text-gray-700">
          <Clock className="h-4 w-4 mr-3 text-gray-400" />
          <span>{formatDateRange()}</span>
        </div>

        <div className="flex items-center text-sm text-gray-700">
          <MapPin className="h-4 w-4 mr-3 text-gray-400" />
          <span>{reserva.sala.nome}</span>
        </div>

        <div className="flex items-center text-sm text-gray-700">
          <User className="h-4 w-4 mr-3 text-gray-400" />
          <span>{reserva.organizerEmail}</span>
        </div>
      </CardContent>
    </Card>
  );
};

export default ReservaItem;
