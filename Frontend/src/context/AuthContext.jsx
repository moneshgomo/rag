import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import authService from '../services/authService';
import { extractErrorMessage } from '../utils/errorHandler';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem('finflow_user');
      return savedUser ? JSON.parse(savedUser) : null;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState(() => localStorage.getItem('finflow_token') || null);
  const [loading, setLoading] = useState(true);

  // Initialize and verify user on mount
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('finflow_token');
      if (!storedToken) {
        setLoading(false);
        return;
      }

      try {
        const userProfile = await authService.getCurrentUser();
        setUser(userProfile);
        localStorage.setItem('finflow_user', JSON.stringify(userProfile));
      } catch (err) {
        console.warn('Session verification failed:', err);
        localStorage.removeItem('finflow_token');
        localStorage.removeItem('finflow_user');
        setToken(null);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = async (email, password) => {
    try {
      const authData = await authService.login({ email, password });
      const authToken = authData.token;
      
      localStorage.setItem('finflow_token', authToken);
      setToken(authToken);

      // Fetch comprehensive user profile
      try {
        const userProfile = await authService.getCurrentUser();
        setUser(userProfile);
        localStorage.setItem('finflow_user', JSON.stringify(userProfile));
        return { success: true, role: userProfile.role };
      } catch {
        // Fallback to auth response user
        const basicUser = {
          id: authData.id,
          name: authData.name,
          email: authData.email || email,
          role: authData.role,
        };
        setUser(basicUser);
        localStorage.setItem('finflow_user', JSON.stringify(basicUser));
        return { success: true, role: authData.role };
      }
    } catch (err) {
      const errorObj = extractErrorMessage(err);
      throw errorObj;
    }
  };

  const register = async (registerData) => {
    try {
      const authData = await authService.register(registerData);
      const authToken = authData.token;
      
      localStorage.setItem('finflow_token', authToken);
      setToken(authToken);

      const basicUser = {
        id: authData.id,
        name: authData.name,
        email: authData.email || registerData.email,
        role: authData.role,
      };
      setUser(basicUser);
      localStorage.setItem('finflow_user', JSON.stringify(basicUser));

      return { success: true, role: authData.role };
    } catch (err) {
      const errorObj = extractErrorMessage(err);
      throw errorObj;
    }
  };

  const logout = useCallback(() => {
    localStorage.removeItem('finflow_token');
    localStorage.removeItem('finflow_user');
    setToken(null);
    setUser(null);
  }, []);

  const refreshProfile = async () => {
    if (!token) return;
    try {
      const profile = await authService.getCurrentUser();
      setUser(profile);
      localStorage.setItem('finflow_user', JSON.stringify(profile));
    } catch (err) {
      console.warn('Failed to refresh profile:', err);
    }
  };

  const value = {
    user,
    token,
    role: user?.role || null,
    isAuthenticated: !!token && !!user,
    loading,
    login,
    register,
    logout,
    refreshProfile,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
