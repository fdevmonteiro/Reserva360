import { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Calendar as CalendarIcon, Loader2, List } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import ReservaItem from '@/components/ReservaItem';
import NovaReservaForm from '@/components/NovaReservaForm';
import { Reserva } from '@/types/reserva';
import { getReservas, getMyReservas, createReserva, deleteReserva, updateReserva } from '@/services/reservaService';
import type { ReservaFormData } from '@/services/reservaService';
import { useAuth } from '@/contexts/AuthContext';

// Dependências do Calendário
import { Calendar, dateFnsLocalizer } from 'react-big-calendar';
import format from 'date-fns/format';
import parse from 'date-fns/parse';
import startOfWeek from 'date-fns/startOfWeek';
import getDay from 'date-fns/getDay';
import ptBR from 'date-fns/locale/pt-BR';
import 'react-big-calendar/lib/css/react-big-calendar.css';

// Configuração do Localizer para Português-Brasil
const locales = { 'pt-BR': ptBR };
const localizer = dateFnsLocalizer({ format, parse, startOfWeek, getDay, locales });

// Mensagens do calendário em português
const calendarMessages = {
  allDay: 'Dia todo', previous: 'Anterior', next: 'Próximo', today: 'Hoje',
  month: 'Mês', week: 'Semana', day: 'Dia', agenda: 'Agenda',
  date: 'Data', time: 'Hora', event: 'Reserva',
  noEventsInRange: 'Não há reservas neste período.',
  showMore: (total: number) => `+ Ver mais (${total})`
};

// --- AQUI ESTÁ A MUDANÇA ---
// Definimos os horários mínimo e máximo para a visualização do calendário
const minTime = new Date();
minTime.setHours(7, 0, 0); // 07:00 da manhã

const maxTime = new Date();
maxTime.setHours(20, 0, 0); // 20:00 (8 da noite)
// -----------------------------

const ReservasPage = () => {
  const [allReservas, setAllReservas] = useState<Reserva[]>([]); // Para o calendário
  const [myReservas, setMyReservas] = useState<Reserva[]>([]);   // Para a aba "Minhas Reservas"
  const [isLoading, setIsLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [editingReserva, setEditingReserva] = useState<Reserva | null>(null);
  const { toast } = useToast();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  // Função que carrega TODOS os dados da página
  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      // Buscamos os dois conjuntos de dados em paralelo
      const [allData, myData] = await Promise.all([
        getReservas(),
        getMyReservas()
      ]);
      setAllReservas(allData);
      setMyReservas(myData);
    } catch (error) {
      console.error('Erro ao carregar reservas:', error);
      toast({ title: "Erro", description: "Não foi possível carregar as reservas.", variant: "destructive" });
    } finally {
      setIsLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleAddNewClick = () => {
    setEditingReserva(null);
    setIsFormOpen(true);
  };
  
  const handleEditClick = (reserva: Reserva) => {
    setEditingReserva(reserva);
    setIsFormOpen(true);
  };

  const handleFormClose = () => {
    setIsFormOpen(false);
    setEditingReserva(null);
  };

  const handleSaveReserva = async (formData: ReservaFormData, id?: number) => {
    setIsSaving(true);
    try {
      const isEditMode = typeof id === 'number';
      if (isEditMode) {
        await updateReserva(id, formData);
        toast({ title: "Sucesso", description: "Reserva atualizada!" });
      } else {
        await createReserva(formData);
        toast({ title: "Sucesso", description: "Pré-reserva enviada para confirmação!" });
      }
      handleFormClose();
      await loadData(); // Recarrega todos os dados
    } catch (error) {
      toast({ title: "Erro", description: "Não foi possível salvar a reserva.", variant: "destructive" });
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteReserva = async (id: number | string) => {
    try {
      await deleteReserva(String(id));
      toast({ title: "Sucesso", description: "Reserva excluída com sucesso!" });
      await loadData(); // Recarrega todos os dados
    } catch (error) {
      toast({ title: "Erro", description: "Não foi possível excluir a reserva.", variant: "destructive" });
    }
  };

  const eventsForCalendar = useMemo(() => 
    allReservas.map(reserva => ({
      title: `${reserva.title} (${reserva.sala.nome})`,
      start: new Date(reserva.startTime),
      end: new Date(reserva.endTime),
      resource: reserva,
    })), [allReservas]);

  return (
    <main className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h2 className="text-4xl font-bold font-poppins text-gray-900 mb-2">Reservas de Sala</h2>
          <p className="text-xl text-gray-600 font-inter">Gerencie e visualize os agendamentos da instituição</p>
        </div>
        
        {isAuthenticated && (
          <Button onClick={handleAddNewClick} className="bg-ccaa-blue-600 hover:bg-ccaa-blue-700 text-white">
            <Plus className="h-4 w-4 mr-2" />
            Nova Reserva
          </Button>
        )}
      </div>

      <Tabs defaultValue="calendario" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="calendario"><CalendarIcon className="mr-2 h-4 w-4"/>Calendário</TabsTrigger>
          <TabsTrigger value="minhas-reservas"><List className="mr-2 h-4 w-4"/> Minhas Reservas</TabsTrigger>
        </TabsList>
        
        {/* Conteúdo da Aba "Calendário" */}
        <TabsContent value="calendario" className="mt-6">
          {isLoading ? (
            <div className="flex justify-center py-16"><Loader2 className="h-8 w-8 animate-spin" /></div>
          ) : (
            <div style={{ height: '75vh' }} className="p-4 bg-white rounded-lg shadow-sm border">
              <Calendar
                localizer={localizer}
                events={eventsForCalendar}
                startAccessor="start"
                endAccessor="end"
                messages={calendarMessages}
                culture='pt-BR'
                defaultView='week'
                min={minTime} // Adicionamos o horário mínimo
                max={maxTime} // Adicionamos o horário máximo
              />
            </div>
          )}
        </TabsContent>

        {/* Conteúdo da Aba "Minhas Reservas" */}
        <TabsContent value="minhas-reservas" className="mt-6">
          {isLoading ? (
            <div className="flex justify-center py-16"><Loader2 className="h-8 w-8 animate-spin" /></div>
          ) : myReservas.length === 0 ? (
            <div className="text-center py-16 bg-white rounded-lg border">
              <h3 className="text-xl font-semibold">Você ainda não tem reservas confirmadas.</h3>
              <p className="text-gray-600 mt-2">Crie uma nova reserva para começar.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {myReservas.map(reserva => (
                <ReservaItem 
                  key={reserva.id} 
                  reserva={reserva} 
                  onDelete={() => handleDeleteReserva(reserva.id)} 
                  onEdit={() => handleEditClick(reserva)}
                />
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>

      <NovaReservaForm
        isOpen={isFormOpen}
        onClose={handleFormClose}
        onSubmit={handleSaveReserva}
        isLoading={isSaving}
        reservaToEdit={editingReserva}
      />
    </main>
  );
};

export default ReservasPage;
