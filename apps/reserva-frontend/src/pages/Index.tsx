// src/pages/Index.tsx

import { useState, useEffect } from 'react';
import { Calendar, ArrowRight, BookOpen, Users, Globe } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useNavigate } from 'react-router-dom';
import { Reserva } from '@/types/reserva';
import { Evento } from '@/types/evento';
import { getUpcomingReservas } from '@/services/reservaService';
import { getUpcomingEventos } from '@/services/eventoService';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

const Index = () => {
  const navigate = useNavigate();
  const [upcomingReservas, setUpcomingReservas] = useState<Reserva[]>([]);
  const [upcomingEventos, setUpcomingEventos] = useState<Evento[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true);
        const [reservasData, eventosData] = await Promise.all([
          getUpcomingReservas(),
          getUpcomingEventos()
        ]);
        setUpcomingReservas(reservasData);
        setUpcomingEventos(eventosData);
      } catch (error) {
        console.error("Erro ao buscar dados para o dashboard:", error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchData();
  }, []);

  return (
    <div className="min-h-screen bg-gray-50">
      
      {/* Hero Section (Mantida) */}
      <section className="container mx-auto px-4 pt-16 pb-12 text-center">
        <h2 className="text-5xl font-bold font-poppins text-gray-900 mb-4">
          Bem-vindo ao Sistema de Reservas
        </h2>
        <p className="text-xl text-gray-600 font-inter max-w-3xl mx-auto mb-8">
          Sua plataforma central para agendamento de salas e eventos online.
        </p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Button size="lg"  className="bg-ccaa-red-600 hover:bg-ccaa-red-800"  onClick={() => navigate("/reservas")}>
            <Calendar className="mr-2 h-5 w-5" />
            Ver Reservas de Sala
            <ArrowRight className="ml-2 h-5 w-5" />
          </Button>
          <Button size="lg" className="bg-ccaa-blue-600 hover:bg-ccaa-blue-800" onClick={() => navigate("/eventos")}>
            Reservar Evento Online (Zoom)
          </Button>
        </div>
      </section>

      {/* Seção de Dashboard (Substituindo as Features) */}
      <section className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          
          {/* Card de Próximas Reservas */}
          <Card>
            <CardHeader>
              <CardTitle>Próximas Reservas de Sala</CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? <p>Carregando...</p> : upcomingReservas.length > 0 ? (
                <ul className="space-y-3">
                  {upcomingReservas.map(reserva => (
                    <li key={reserva.id} className="text-sm border-b pb-2">
                      <p className="font-semibold">{reserva.title} - <span className="font-normal text-gray-600">{reserva.sala.nome}</span></p>
                      <p className="text-gray-500">{format(new Date(reserva.startTime), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })}</p>
                    </li>
                  ))}
                </ul>
              ) : <p className="text-gray-500">Nenhuma reserva futura encontrada.</p>}
            </CardContent>
          </Card>

          {/* Card de Próximos Eventos */}
          <Card>
            <CardHeader>
              <CardTitle>Próximos Eventos Online</CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? <p>Carregando...</p> : upcomingEventos.length > 0 ? (
                <ul className="space-y-3">
                  {upcomingEventos.map(evento => (
                     <li key={evento.id} className="text-sm border-b pb-2">
                      <p className="font-semibold">{evento.title}</p>
                      <p className="text-gray-500">{format(new Date(evento.startTime), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })}</p>
                    </li>
                  ))}
                </ul>
              ) : <p className="text-gray-500">Nenhum evento futuro encontrado.</p>}
            </CardContent>
          </Card>

        </div>
      </section>
      
      {/* Rodapé Simplificado */}
      <footer className="bg-white py-6 mt-12 border-t">
        <div className="container mx-auto px-4 text-center text-gray-500">
            <p>&copy; {new Date().getFullYear()} CCAA. Sistema de Reservas Interno.</p>
        </div>
      </footer>
    </div>
  );
};

export default Index;