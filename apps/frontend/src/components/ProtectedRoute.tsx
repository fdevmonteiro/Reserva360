// src/components/ProtectedRoute.tsx

import { Navigate, Outlet } from 'react-router-dom';

const ProtectedRoute = () => {
  // 1. Verifica se o token existe no armazenamento local do navegador
  const token = sessionStorage.getItem('authToken');

  // 2. Se o token existir, ele renderiza a página filha que está sendo protegida.
  // O <Outlet /> é um placeholder para a rota aninhada (ex: a ReservasPage).
  if (token) {
    return <Outlet />;
  }

  // 3. Se o token NÃO existir, ele redireciona o usuário para a página de login.
  // O 'replace' evita que o usuário possa usar o botão "voltar" do navegador para acessar a página protegida.
  return <Navigate to="/login" replace />;
};

export default ProtectedRoute;