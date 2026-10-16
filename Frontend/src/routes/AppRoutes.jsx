import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ProtectedRoute from './ProtectedRoute';

// Layouts
import DashboardLayout from '../layouts/DashboardLayout';

// Pages
import Login from '../pages/auth/Login';
import Signup from '../pages/auth/Signup';
import AdminDashboard from '../pages/admin/AdminDashboard';
import ManagerDashboard from '../pages/manager/ManagerDashboard';
import EmployeeDashboard from '../pages/employee/EmployeeDashboard';
import LeaveSubmission from '../pages/employee/LeaveSubmission';
import EmailLeaveAction from '../pages/email/EmailLeaveAction';
import FinancePlaceholder from '../pages/finance/FinancePlaceholder';
import NotFound from '../pages/NotFound';

export const AppRoutes = () => {
  const { user, role, isAuthenticated, loading } = useAuth();

  const getHomeRedirect = () => {
    if (!isAuthenticated) return <Navigate to="/login" replace />;
    if (role === 'SUPER_ADMIN') return <Navigate to="/admin/dashboard" replace />;
    if (role === 'MANAGER') return <Navigate to="/manager/dashboard" replace />;
    return <Navigate to="/employee/dashboard" replace />;
  };

  return (
    <Routes>
      {/* Public Authentication Routes */}
      <Route
        path="/login"
        element={
          isAuthenticated && !loading ? (
            getHomeRedirect()
          ) : (
            <Login />
          )
        }
      />
      <Route
        path="/signup"
        element={
          isAuthenticated && !loading ? (
            getHomeRedirect()
          ) : (
            <Signup />
          )
        }
      />

      {/* Public Email Action Token Route */}
      <Route path="/leave/action/:token" element={<EmailLeaveAction />} />

      {/* Root Path */}
      <Route path="/" element={getHomeRedirect()} />

      {/* Protected Super Admin Console */}
      <Route
        path="/admin/dashboard"
        element={
          <ProtectedRoute allowedRoles={['SUPER_ADMIN']}>
            <DashboardLayout
              breadcrumbs={[
                { label: 'Workspace', to: '/admin/dashboard' },
                { label: 'Super Admin Console' },
              ]}
            />
          </ProtectedRoute>
        }
      >
        <Route index element={<AdminDashboard />} />
      </Route>

      {/* Protected Manager Hub */}
      <Route
        path="/manager/dashboard"
        element={
          <ProtectedRoute allowedRoles={['MANAGER', 'SUPER_ADMIN']}>
            <DashboardLayout
              breadcrumbs={[
                { label: 'Workspace', to: '/manager/dashboard' },
                { label: 'Manager Hub' },
              ]}
            />
          </ProtectedRoute>
        }
      >
        <Route index element={<ManagerDashboard />} />
      </Route>

      {/* Protected Employee Workspace */}
      <Route
        path="/employee/dashboard"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYEE', 'MANAGER', 'SUPER_ADMIN']}>
            <DashboardLayout
              breadcrumbs={[
                { label: 'Workspace', to: '/employee/dashboard' },
                { label: 'Employee Dashboard' },
              ]}
            />
          </ProtectedRoute>
        }
      >
        <Route index element={<EmployeeDashboard />} />
      </Route>

      {/* Protected Leave Submission */}
      <Route
        path="/leave/request"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYEE', 'MANAGER', 'SUPER_ADMIN']}>
            <DashboardLayout
              breadcrumbs={[
                { label: 'Workspace', to: '/employee/dashboard' },
                { label: 'My Leaves', to: '/employee/dashboard' },
                { label: 'Request Leave' },
              ]}
            />
          </ProtectedRoute>
        }
      >
        <Route index element={<LeaveSubmission />} />
      </Route>

      {/* Protected Finance Placeholder */}
      <Route
        path="/finance"
        element={
          <ProtectedRoute>
            <DashboardLayout
              breadcrumbs={[
                { label: 'Workspace', to: '/' },
                { label: 'Finance Module' },
              ]}
            />
          </ProtectedRoute>
        }
      >
        <Route index element={<FinancePlaceholder />} />
      </Route>

      {/* Catch-all 404 */}
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};

export default AppRoutes;
