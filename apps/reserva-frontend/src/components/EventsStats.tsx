
import { Event } from '@/types/event';

interface EventsStatsProps {
  events: Event[];
}

const EventsStats = ({ events }: EventsStatsProps) => {
  const totalEvents = events.length;
  const availableSpots = events.reduce((acc, event) => acc + (event.capacity - event.enrolled), 0);
  const categories = new Set(events.map(event => event.category)).size;

  return (
    <div className="mt-12 bg-gradient-to-r from-ccaa-blue-600 to-ccaa-red-600 rounded-xl p-8 text-white">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-center">
        <div>
          <h4 className="text-3xl font-bold font-poppins">{totalEvents}</h4>
          <p className="text-blue-100 font-inter">Eventos Disponíveis</p>
        </div>
        <div>
          <h4 className="text-3xl font-bold font-poppins">{availableSpots}</h4>
          <p className="text-blue-100 font-inter">Vagas Disponíveis</p>
        </div>
        <div>
          <h4 className="text-3xl font-bold font-poppins">{categories}</h4>
          <p className="text-blue-100 font-inter">Categorias</p>
        </div>
      </div>
    </div>
  );
};

export default EventsStats;
