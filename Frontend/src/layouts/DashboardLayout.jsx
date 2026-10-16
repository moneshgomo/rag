import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../components/layout/Sidebar';
import Header from '../components/layout/Header';

export const DashboardLayout = ({ breadcrumbs }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: 'var(--color-bg-page)' }}>
      {/* Sidebar */}
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main Content Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <Header
          onMenuClick={() => setSidebarOpen((prev) => !prev)}
          breadcrumbs={breadcrumbs}
        />

        <main style={{ flex: 1, padding: '32px 32px 48px', maxWidth: '1280px', width: '100%', margin: '0 auto' }}>
          <Outlet />
        </main>
      </div>

      <style>{`
        @media (max-width: 768px) {
          main {
            padding: 20px 16px 36px !important;
          }
          aside {
            position: fixed !important;
            transform: translateX(-100%);
            transition: transform 0.2s ease-in-out;
          }
          aside.open {
            transform: translateX(0);
          }
        }
      `}</style>
    </div>
  );
};

export default DashboardLayout;
