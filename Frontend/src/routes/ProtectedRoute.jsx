import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user, role, isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexDirection: 'column',
          gap: '12px',
        }}
      >
        <span className="spinner" style={{ borderColor: 'var(--color-primary)', borderTopColor: 'transparent', width: '24px', height: '24px' }} />
        <span style={{ color: 'var(--color-text-secondary)', fontSize: '13px' }}>
          Verifying session...
        </span>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // If specific roles are required, verify access
  if (allowedRoles && !allowedRoles.includes(role)) {
    // Redirect to their respective authorized dashboard
    if (role === 'SUPER_ADMIN') {
      return <Navigate to="/admin/dashboard" replace />;
    } else if (role === 'MANAGER') {
      return <Navigate to="/manager/dashboard" replace />;
    } else {
      return <Navigate to="/employee/dashboard" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;
