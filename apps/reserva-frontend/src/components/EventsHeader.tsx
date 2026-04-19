
import { Button } from '@/components/ui/button';
import { Navigate } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';

const EventsHeader = () => {

  const navigate = useNavigate();
  return (
    <header className="bg-white shadow-sm border-b-2 border-ccaa-blue-600">
      <div className="container mx-auto px-4 py-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-gradient-to-r from-ccaa-blue-600 to-ccaa-red-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-xl">C</span>
            </div>
            <div>
              <h1 className="text-3xl font-bold font-poppins bg-gradient-to-r from-ccaa-blue-700 to-ccaa-red-600 bg-clip-text text-transparent">
                CCAA
              </h1>
              <p className="text-gray-600 font-inter">Sistema de reserva de salas e agendamento de reuniões</p>
            </div>
          </div>
          

          <nav>
            <Button variant="outline" className="border-ccaa-blue-600 text-ccaa-blue-600 hover:bg-ccaa-blue-50"
            onClick={() => navigate("/")}>
              
              Voltar ao Início
            </Button>
          </nav>
        </div>
      </div>
    </header>
  );
};

export default EventsHeader;
