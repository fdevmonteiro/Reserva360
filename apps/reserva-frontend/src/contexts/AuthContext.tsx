import { createContext, useState, useContext, useEffect, ReactNode, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';

// Interface para os dados do usuário que guardaremos no estado
interface User {
  username: string;
  roles: string[];
}

// Interface para o conteúdo decodificado do nosso token JWT
interface DecodedToken {
  sub: string;       // Subject (o username)
  roles: string;     // As permissões (ex: "ROLE_USER,ROLE_G.TI")
  exp: number;       // Timestamp de expiração
}

// Interface para o nosso contexto
interface AuthContextType {
  isAuthenticated: boolean;
  user: User | null;
  hasRole: (role: string) => boolean; // Função para verificar permissões
  login: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const navigate = useNavigate();

  // Função centralizada para configurar a autenticação
  const setupAuth = useCallback((token: string | null) => {
    if (token) {
      try {
        const decodedToken: DecodedToken = jwtDecode(token);
        // Verifica se o token não expirou
        if (decodedToken.exp * 1000 > Date.now()) {
          // CORRIGIDO: Extrai as roles do token, divide a string em um array
          const userRoles = decodedToken.roles ? decodedToken.roles.split(',') : [];
          // CORRIGIDO: Salva o usuário com username E roles
          setUser({ username: decodedToken.sub, roles: userRoles });
          setIsAuthenticated(true);
        } else {
          // Se o token estiver expirado, limpa tudo
          sessionStorage.removeItem('authToken');
          setUser(null);
          setIsAuthenticated(false);
        }
      } catch (error) {
        console.error("Token inválido ou corrompido:", error);
        sessionStorage.removeItem('authToken');
        setUser(null);
        setIsAuthenticated(false);
      }
    } else {
      // Se não há token, garante que o estado esteja como deslogado
      setUser(null);
      setIsAuthenticated(false);
    }
  }, []);

  // Roda uma vez quando a aplicação carrega para verificar se já existe um token salvo
  useEffect(() => {
    const token = sessionStorage.getItem('authToken');
    setupAuth(token);
  }, [setupAuth]);

  // Função de login agora usa a função setupAuth
  const login = (token: string) => {
    sessionStorage.setItem('authToken', token);
    setupAuth(token);
    navigate('/');
  };

  // Função de logout agora usa a função setupAuth
  const logout = () => {
    sessionStorage.removeItem('authToken');
    setupAuth(null);
    navigate('/login');
  };

  // Função helper para verificar se o usuário tem uma determinada permissão
  const hasRole = (role: string): boolean => {
    return user?.roles.includes(role) ?? false;
  };

  
  const contextValue = { isAuthenticated, user, hasRole, login, logout };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

// Hook customizado para facilitar o uso do contexto
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider');
  }
  return context;
};