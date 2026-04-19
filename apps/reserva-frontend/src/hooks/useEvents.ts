// hooks/useEvents.ts (VERSÃO NOVA - CONECTADO AO BACKEND)

import { useState, useEffect, useMemo } from 'react';
import api from '@/services/api';

// Supondo que você tenha um tipo definido para seu evento
export type Evento = {
  id: number;
  assunto: string;
  descricao?: string;
  dataHoraInicio: string;
  dataHoraFim: string;
  emailOrganizador: string;
  emailSala: string;
  idEventoGraph?: string;
};

export const useEvents = () => {
  // 1. O estado de eventos começa como um array vazio
  const [events, setEvents] = useState<Evento[]>([]);

  // 2. Novos estados para gerenciar o ciclo de vida da requisição
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Estados dos filtros (continuam os mesmos)
  const [searchTerm, setSearchTerm] = useState('');
  const [periodFilter, setPeriodFilter] = useState('todos');
  const [categoryFilter, setCategoryFilter] = useState('todas');

  // 3. useEffect para buscar os dados da API quando o componente montar
  useEffect(() => {
    const fetchEvents = async () => {
      try {
        setIsLoading(true); // Começa o carregamento
        setError(null); // Limpa erros anteriores

        // 4. A chamada à API usa a baseURL do ambiente (VITE_API_BASE_URL)
        const response = await api.get<Evento[]>('/eventos');
        
        setEvents(response.data); // Guarda os eventos recebidos no estado
      } catch (err) {
        console.error("Falha ao buscar eventos:", err);
        setError("Não foi possível carregar os eventos. Tente novamente mais tarde.");
      } finally {
        setIsLoading(false); // Termina o carregamento (com sucesso ou erro)
      }
    };

    fetchEvents();
  }, []); // O array vazio [] garante que isso só rode uma vez

  // 5. Lógica de filtro (agora envolvida em um useMemo para performance)
  const filteredEvents = useMemo(() => {
    return events.filter(event => {
      
      const matchesSearch = event.assunto.toLowerCase().includes(searchTerm.toLowerCase());
      
      // Adicione aqui a lógica para periodFilter e categoryFilter se necessário
      // const matchesPeriod = ...
      // const matchesCategory = ...

      return matchesSearch; // && matchesPeriod && matchesCategory;
    });
  }, [events, searchTerm]); // Recalcula apenas se estas dependências mudarem

  const clearFilters = () => {
    setSearchTerm('');
    setPeriodFilter('todos');
    setCategoryFilter('todas');
  };

  // Retornamos os novos estados para que a UI possa usá-los
  return {
    isLoading,
    error,
    searchTerm,
    setSearchTerm,
    periodFilter,
    setPeriodFilter,
    categoryFilter,
    setCategoryFilter,
    filteredEvents,
    clearFilters
  };
};
