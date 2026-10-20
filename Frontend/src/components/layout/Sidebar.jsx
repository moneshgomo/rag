import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import finflowLogo from '../../assets/image.png';
import {
  LayoutDashboard,
  Users,
  CalendarDays,
  FilePlus,
  Coins,
  ShieldCheck,
  UserCheck,
  LogOut,
  Building2,
} from 'lucide-react';
import { formatRole } from '../../utils/formatters';

export const Sidebar = ({ isOpen, onClose }) => {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getDashboardLink = () => {
    switch (role) {
      case 'SUPER_ADMIN':
        return '/admin/dashboard';
      case 'MANAGER':
        return '/manager/dashboard';
      case 'EMPLOYEE':
      default:
        return '/employee/dashboard';
    }
  };

  return (
    <>
      {isOpen && (
        <div
          className="sidebar-backdrop"
          onClick={onClose}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.3)',
            zIndex: 39,
          }}
        />
      )}

      <aside
        style={{
          width: 'var(--sidebar-width)',
          backgroundColor: 'var(--color-bg-sidebar)',
          borderRight: '1px solid var(--color-border)',
          height: '100vh',
          position: 'sticky',
          top: 0,
          display: 'flex',
          flexDirection: 'column',
          zIndex: 40,
          userSelect: 'none',
        }}
      >
        {/* Workspace Brand Header */}
        <div
          style={{
            padding: '14px 18px',
            borderBottom: '1px solid var(--color-border-subtle)',
            display: 'flex',
            alignItems: 'center',
            backgroundColor: '#FFFFFF',
          }}
        >
          <img
            src={finflowLogo}
            alt="FinFlow - Finance & HR Policy Assistant"
            style={{
              height: '36px',
              maxWidth: '100%',
              objectFit: 'contain',
              display: 'block',
            }}
          />
        </div>

        {/* Navigation Links */}
        <nav style={{ padding: '16px 12px', flex: 1, overflowY: 'auto' }}>
          <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', padding: '0 8px 8px', letterSpacing: '0.05em' }}>
            Main
          </div>

          <NavLink
            to={getDashboardLink()}
            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            <LayoutDashboard size={16} />
            <span>Dashboard</span>
          </NavLink>

          {/* Super Admin specific nav */}
          {role === 'SUPER_ADMIN' && (
            <>
              <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', padding: '16px 8px 8px', letterSpacing: '0.05em' }}>
                Administration
              </div>
              <NavLink
                to="/admin/dashboard"
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                onClick={onClose}
              >
                <ShieldCheck size={16} />
                <span>Control Console</span>
              </NavLink>
            </>
          )}

          {/* Manager specific nav */}
          {role === 'MANAGER' && (
            <>
              <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', padding: '16px 8px 8px', letterSpacing: '0.05em' }}>
                Management
              </div>
              <NavLink
                to="/manager/dashboard"
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                onClick={onClose}
              >
                <UserCheck size={16} />
                <span>Manager Hub</span>
              </NavLink>
            </>
          )}

          {/* Employee & general leave nav */}
          {(role === 'EMPLOYEE' || role === 'MANAGER') && (
            <>
              <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', padding: '16px 8px 8px', letterSpacing: '0.05em' }}>
                Leaves & Policy
              </div>
              <NavLink
                to="/leave/request"
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                onClick={onClose}
              >
                <FilePlus size={16} />
                <span>Request Leave</span>
              </NavLink>
              <NavLink
                to="/employee/dashboard"
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                onClick={onClose}
              >
                <CalendarDays size={16} />
                <span>My Leaves</span>
              </NavLink>
            </>
          )}

          {/* Finance module placeholder */}
          <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', padding: '16px 8px 8px', letterSpacing: '0.05em' }}>
            Modules
          </div>
          <NavLink
            to="/finance"
            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            <Coins size={16} />
            <span>Finance</span>
            <span
              style={{
                marginLeft: 'auto',
                fontSize: '10px',
                padding: '2px 5px',
                borderRadius: '4px',
                background: '#E5E7EB',
                color: '#4B5563',
              }}
            >
              Soon
            </span>
          </NavLink>
        </nav>

        {/* Footer Profile & Logout */}
        <div
          style={{
            padding: '12px 14px',
            borderTop: '1px solid var(--color-border-subtle)',
            backgroundColor: '#FAFAFA',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '8px',
            }}
          >
            <div style={{ overflow: 'hidden' }}>
              <div
                style={{
                  fontWeight: 600,
                  fontSize: '13px',
                  color: 'var(--color-text-primary)',
                  whiteSpace: 'nowrap',
                  textOverflow: 'ellipsis',
                  overflow: 'hidden',
                }}
              >
                {user?.name || 'User'}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                {formatRole(role)}
              </div>
            </div>

            <button
              type="button"
              onClick={handleLogout}
              className="btn-ghost"
              style={{
                padding: '6px',
                borderRadius: '4px',
                color: 'var(--color-text-muted)',
              }}
              title="Sign Out"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>

        <style>{`
          .nav-item {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 8px 12px;
            border-radius: var(--radius-md);
            color: var(--color-text-secondary);
            font-size: var(--font-size-base);
            font-weight: 500;
            text-decoration: none;
            margin-bottom: 2px;
            transition: all var(--transition-fast);
          }
          .nav-item:hover {
            background-color: var(--color-bg-hover);
            color: var(--color-text-primary);
          }
          .nav-item.active {
            background-color: var(--color-bg-active);
            color: var(--color-primary);
            font-weight: 600;
          }
        `}</style>
      </aside>
    </>
  );
};

export default Sidebar;
