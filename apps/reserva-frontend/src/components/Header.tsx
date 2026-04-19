// src/components/Header.tsx
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { LogIn, LogOut, User } from 'lucide-react';
import { ThemeToggle } from './ThemeToggle';

const Header = () => {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="bg-white shadow-sm border-b-2 border-ccaa-blue-600">
        <div className="container mx-auto px-4 py-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
            
             <div className="w-13 h-13 flex items-center justify-center">
              <img 
                src="/logo-ccaa-lp.png" 
                alt="Logo CCAA" 
                className="w-13 h-13 object-contain cursor-pointer" 
                onClick={() => navigate('/')}
                
              />
            </div>

              <div>
               
                <p className="text-gray-600 font-inter"></p>
              </div>
            </div>
          
          {/* Menu Dinâmico */}
          <nav className="flex items-center gap-2">
            <Button variant="ghost" onClick={() => navigate('/')}>Voltar ao Início</Button>
            
            {isAuthenticated && user ? (
              
              <>
                <div className="flex items-center gap-2 text-sm font-medium text-gray-700 border-l pl-4">
                  <User className="h-4 w-4 text-gray-500" />
                  <span>Olá, {user.username}</span>
                </div>
                <Button onClick={logout} variant="outline" size="sm">
                  <LogOut className="mr-2 h-4 w-4" />
                  Sair
                </Button>
              </>
            ) : (
             
              <Button onClick={() => navigate('/login')}>
                <LogIn className="mr-2 h-4 w-4" />
                Fazer Login
              </Button>
            )}
            
          </nav>
        </div>
      </div>
    </header>
  );
};

export default Header;
