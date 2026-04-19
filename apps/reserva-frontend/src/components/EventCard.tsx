// components/EventCard.tsx

import { Calendar, Clock, MapPin, User, Trash2 } from 'lucide-react';
import { Card, CardContent, CardFooter, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Evento } from '@/hooks/useEvents';

interface EventCardProps {
  event: Evento;
  onDelete: (id: number) => void; // Adicionamos a função de deletar
}

const EventCard = ({ event, onDelete }: EventCardProps) => {

  const dataInicio = event.dataHoraInicio ? new Date(event.dataHoraInicio) : null;
  const dataFim = event.dataHoraFim ? new Date(event.dataHoraFim) : null;

  return (
    <Card className="flex flex-col h-full hover:shadow-lg transition-shadow duration-300">
      <CardHeader>
        <CardTitle>{event.assunto}</CardTitle>
        <CardDescription className="line-clamp-2">{event.descricao}</CardDescription>
      </CardHeader>

      <CardContent className="flex-grow space-y-3">
        {dataInicio && (
          <div className="flex items-center text-sm text-muted-foreground">
            <Calendar className="h-4 w-4 mr-2" />
            <span>{dataInicio.toLocaleDateString('pt-BR')}</span>
          </div>
        )}

        {dataInicio && dataFim && (
          <div className="flex items-center text-sm text-muted-foreground">
            <Clock className="h-4 w-4 mr-2" />
            <span>
              {dataInicio.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })} - {dataFim.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
            </span>
          </div>
        )}

        <div className="flex items-center text-sm text-muted-foreground">
          <MapPin className="h-4 w-4 mr-2" />
          <span>{event.emailSala}</span>
        </div>

        <div className="flex items-center text-sm text-muted-foreground">
          <User className="h-4 w-4 mr-2" />
          <span>Organizador: {event.emailOrganizador}</span>
        </div>
      </CardContent>

      <CardFooter className="mt-auto flex flex-col space-y-2">
        <Button 
          variant="destructive" 
          className="w-full"
          onClick={() => onDelete(event.id)}
        >
          <Trash2 className="h-4 w-4 mr-2" />
          Excluir evento
        </Button>

    

        <Button 
          variant="default" 
          className="w-full"
          onClick={() => onDelete(event.id)}
        >
          
         
        </Button>
        
      </CardFooter>
    </Card>
    
  );
};

export default EventCard;