// src/components/MainLayout.tsx

import { Outlet } from 'react-router-dom';
import   Header  from "@/components/Header"
const MainLayout = () => {
  return (
    <div>
      <Header />
      <main>
        {/* O <Outlet /> é onde o React Router vai renderizar a página da rota atual (ReservasPage, EventsPage, etc.) */}
        <Outlet />
      </main>
    </div>
  );
};

export default MainLayout;