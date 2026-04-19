
import { Filter, Search } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { PeriodFilter } from '@/types/event';

interface EventsFiltersProps {
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  periodFilter: PeriodFilter;
  setPeriodFilter: (period: PeriodFilter) => void;
  categoryFilter: string;
  setCategoryFilter: (category: string) => void;
}

const EventsFilters = ({
  searchTerm,
  setSearchTerm,
  periodFilter,
  setPeriodFilter,
  categoryFilter,
  setCategoryFilter
}: EventsFiltersProps) => {
  return (
    <div className="bg-white rounded-xl shadow-lg p-6 mb-8 border border-gray-100">
      <div className="flex items-center mb-4">
        <Filter className="h-5 w-5 text-ccaa-blue-600 mr-2" />
        <h3 className="text-lg font-semibold font-poppins text-gray-900">Filtros</h3>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
          <Input
            placeholder="Buscar eventos..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10 border-gray-200 focus:border-ccaa-blue-500 focus:ring-ccaa-blue-500"
          />
        </div>
        
        <Select value={periodFilter} onValueChange={(value: PeriodFilter) => setPeriodFilter(value)}>
          <SelectTrigger className="border-gray-200 focus:border-ccaa-blue-500 focus:ring-ccaa-blue-500">
            <SelectValue placeholder="Período" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="proximos_7_dias">Próximos 7 dias</SelectItem>
            <SelectItem value="proximo_mes">Próximo mês</SelectItem>
            <SelectItem value="proximos_3_meses">Próximos 3 meses</SelectItem>
            <SelectItem value="todos">Todos os eventos</SelectItem>
          </SelectContent>
        </Select>
        
        <Select value={categoryFilter} onValueChange={setCategoryFilter}>
          <SelectTrigger className="border-gray-200 focus:border-ccaa-blue-500 focus:ring-ccaa-blue-500">
            <SelectValue placeholder="Categoria" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="todos">Todas as categorias</SelectItem>
            <SelectItem value="workshop">Workshops</SelectItem>
            <SelectItem value="palestra">Palestras</SelectItem>
            <SelectItem value="curso">Cursos</SelectItem>
            <SelectItem value="evento_cultural">Eventos Culturais</SelectItem>
            <SelectItem value="prova">Provas</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>
  );
};

export default EventsFilters;
