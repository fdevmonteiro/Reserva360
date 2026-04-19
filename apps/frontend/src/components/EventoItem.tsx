// src/components/EventoItem.tsx
import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import type { Evento } from '@/types/evento';
import { Calendar, Video, Trash2, FilePenLine, User, Loader2, Users, ClipboardList, FileText } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { useToast } from '@/hooks/use-toast';
import { getStartUrl } from '@/services/eventoService';
import { canStartNow, getEventoStart, startCountdownText } from '@/utils/eventoTime';

interface EventoItemProps {
  evento: Evento;
  onDelete: (id: number) => void;
  onEdit: (evento: Evento) => void;
  onViewRegistrants?: (evento: Evento) => void;
  onDownloadReport?: (evento: Evento) => Promise<void>;
}

function getErrorMessage(e: unknown): string {
  if (e instanceof Error) return e.message;
  if (typeof e === 'string') return e;
  return 'Erro desconhecido';
}

// --- helpers de tempo (frontend only) ---
function getStartDate(e: Evento): Date {
  return getEventoStart(e) ?? new Date(e.startTime);
}

const EventoItem = ({ evento, onDelete, onEdit, onViewRegistrants, onDownloadReport }: EventoItemProps) => {
  const { user, hasRole } = useAuth();
  const { toast } = useToast();
  const [starting, setStarting] = useState(false);
  const [downloadingReport, setDownloadingReport] = useState(false);

  // força re-render a cada 60s para atualizar o tooltip/estado do botão
  const [, setTick] = useState(0);
  useEffect(() => {
    const id = setInterval(() => setTick(t => t + 1), 60_000);
    return () => clearInterval(id);
  }, []);

  // Permissões
  const isAdmin = hasRole('ROLE_ADMIN') || hasRole('ADMIN'); // cobre os dois formatos
  const isOrganizer =
    !!user?.username &&
    !!evento.createdByUsername &&
    user.username.toLowerCase() === evento.createdByUsername.toLowerCase();

  const canManage = isAdmin || isOrganizer;

  const startDate = getStartDate(evento);
  const formattedDate = format(startDate, "dd 'de' MMMM 'de' yyyy", { locale: ptBR });
  const formattedTime = format(startDate, "HH:mm'h'", { locale: ptBR });

  const allowedByTime = canStartNow(evento);
  const startTooltip = startCountdownText(evento);

  const handleDelete = () => {
    if (window.confirm(`Tem certeza que deseja excluir o evento "${evento.title}"?`)) {
      onDelete(evento.id);
    }
  };

  const handleDownloadReport = async () => {
    if (!onDownloadReport) return;
    setDownloadingReport(true);
    try {
      await onDownloadReport(evento);
    } catch (e: unknown) {
      toast({
        title: 'Relatório não disponível',
        description: getErrorMessage(e),
        variant: 'destructive',
      });
    } finally {
      setDownloadingReport(false);
    }
  };

  const handleStart = async () => {
    if (!allowedByTime) return; // guarda de segurança no front
    setStarting(true);
    const win = window.open('', '_blank', 'noopener'); // abre a aba antes do await

    try {
      const url = await getStartUrl(evento.id);
      if (win) {
        win.location.href = url;
      } else {
        window.open(url, '_blank', 'noopener');
      }
    } catch (e: unknown) {
      if (win) win.close();
      toast({
        title: 'Erro ao iniciar',
        description: getErrorMessage(e),
        variant: 'destructive',
      });
    } finally {
      setStarting(false);
    }
  };

  return (
    <Card className="flex flex-col h-full transition-all duration-300 hover:shadow-xl hover:-translate-y-1 bg-white dark:bg-gray-900">
      <CardHeader className="pb-4">
        <div className="flex items-start gap-4">
          <div className="flex flex-col items-center justify-center bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-300 p-2 rounded-md w-16 text-center">
            <span className="font-bold text-xs uppercase">
              {format(startDate, 'MMM', { locale: ptBR })}
            </span>
            <span className="font-bold text-2xl tracking-tighter">
              {format(startDate, 'dd')}
            </span>
          </div>
          <div className="flex-1">
            <CardTitle className="text-lg font-bold text-gray-900 dark:text-gray-100">{evento.title}</CardTitle>
            <CardDescription className="text-sm text-gray-500 dark:text-gray-400 flex items-center gap-2">
              <Calendar className="h-4 w-4" /> {formattedDate} às {formattedTime}
            </CardDescription>
          </div>
        </div>
      </CardHeader>

      <CardContent className="pt-4 flex-grow space-y-3">
        <p className="text-sm text-gray-700 dark:text-gray-300">{evento.description}</p>

        {evento.registrationRequired && (
          <div className="flex items-center gap-1.5">
            <Badge variant="secondary" className="text-xs gap-1">
              <ClipboardList className="h-3 w-3" />
              Inscrição obrigatória
            </Badge>
          </div>
        )}

        <div className="flex items-center text-sm text-gray-600 dark:text-gray-400 pt-3 border-t dark:border-gray-700">
          <User className="h-4 w-4 mr-3" />
          <span>
            Organizado por: <span className="font-medium">{evento.organizerEmail}</span>
          </span>
        </div>
      </CardContent>

      <CardFooter className="flex justify-between items-center bg-gray-50 dark:bg-gray-800/50 p-4">
        {canManage ? (
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <span>
                  <div className="flex flex-col items-start gap-1">
                    <Button
                      onClick={handleStart}
                      className="bg-green-600 hover:bg-green-700 text-white"
                      disabled={!allowedByTime || starting}
                    >
                      {starting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Video className="mr-2 h-4 w-4" />}
                      Iniciar Reunião
                    </Button>
                    {!allowedByTime && (
                      <span className="text-xs text-amber-700 dark:text-amber-400">{startTooltip}</span>
                    )}
                  </div>
                </span>
              </TooltipTrigger>
              <TooltipContent>{allowedByTime ? 'Iniciar reunião' : startTooltip}</TooltipContent>
            </Tooltip>
          </TooltipProvider>
        ) : (
          <div />
        )}

        {canManage && (
          <div className="flex items-center">
            {onDownloadReport && (
              <Button
                variant="ghost"
                size="icon"
                onClick={handleDownloadReport}
                disabled={downloadingReport}
                className="text-gray-500 hover:text-ccaa-blue-600"
                title="Baixar relatório PDF"
              >
                {downloadingReport
                  ? <Loader2 className="h-4 w-4 animate-spin" />
                  : <FileText className="h-4 w-4" />
                }
              </Button>
            )}
            {evento.registrationRequired && !evento.parentEventId && onViewRegistrants && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => onViewRegistrants(evento)}
                className="text-gray-500 hover:text-emerald-600"
                title="Ver inscritos do sistema"
              >
                <Users className="h-4 w-4" />
              </Button>
            )}

            <Button variant="ghost" size="icon" onClick={handleDelete} className="text-gray-500 hover:text-red-600">
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        )}
      </CardFooter>
    </Card>
  );
};

export default EventoItem;
