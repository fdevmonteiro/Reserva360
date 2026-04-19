
import { Calendar } from 'lucide-react';
import { Button } from '@/components/ui/button';
import EventCard from '@/components/EventCard';
import { Event } from '@/types/event';

interface EventsGridProps {
  events: Event[];
  onClearFilters: () => void;
}

const EventsGrid = ({ events, onClearFilters }: EventsGridProps) => {
  if (events.length === 0) {
    return (
      <div className="text-center py-16">
        <Calendar className="h-16 w-16 text-gray-300 mx-auto mb-4" />
        <h3 className="text-xl font-semibold text-gray-900 mb-2">Nenhum evento encontrado</h3>
        <p className="text-gray-600 mb-4">Tente ajustar os filtros para encontrar eventos que interessam você.</p>
        <Button 
          onClick={onClearFilters}
          className="bg-ccaa-blue-600 hover:bg-ccaa-blue-700"
        >
          Limpar Filtros
        </Button>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {events.map((event, index) => (
        <div key={event.id} className="animate-fade-in" style={{ animationDelay: `${index * 0.1}s` }}>
          <EventCard event={event} />
        </div>
      ))}
    </div>
  );
};

export default EventsGrid;
