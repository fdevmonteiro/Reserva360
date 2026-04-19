

import { BrowserRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";


import ProtectedRoute from './components/ProtectedRoute';
import MainLayout from "./components/MainLayout";
import LoginPage from './pages/LoginPage';
import ReservasPage from './pages/ReservasPage';
import EventsPage from './pages/EventsPage';
import Index from './pages/Index';
import NotFound from './pages/NotFound';
import ConfirmarEventoPage from "@/pages/ConfirmarEventoPage";
import CancelarEventoPage from "@/pages/CancelarEventoPage";
import ConfirmarEventoRecorrentePage from "@/pages/ConfirmarEventoRecorrentePage";
import InscricaoPage from "@/pages/InscricaoPage";

import { AuthProvider } from "./contexts/AuthContext";
import { ThemeProvider } from "./components/ThemeProvider";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
 <BrowserRouter>
 <ThemeProvider>
      <AuthProvider>
       
          {/* Componentes de notificação podem ficar aqui */}
          <Toaster />
          <Sonner />

          <Routes>
            {/* Rotas Públicas */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/inscricao/evento/:eventoId" element={<InscricaoPage />} />
          
            
            {/* Rotas que usam o Layout Principal (com Header) */}
            <Route element={<MainLayout />}>
              <Route path="/" element={<Index />} />
                <Route path="/eventos/confirmar" element={<ConfirmarEventoPage />} />
            <Route path="/eventos/confirmar-recorrente" element={<ConfirmarEventoRecorrentePage />} />
            <Route path="/eventos/cancelar" element={<CancelarEventoPage />} />
              {/* Rotas Protegidas dentro do Layout Principal */}
              <Route element={<ProtectedRoute />}>
                <Route path="/reservas" element={<ReservasPage />} />
                <Route path="/eventos" element={<EventsPage />} />
              </Route>
            </Route>

            {/* Rota para páginas não encontradas */}
            <Route path="*" element={<NotFound />} />
          </Routes>
          </AuthProvider>
          </ThemeProvider>
        </BrowserRouter>
      
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
