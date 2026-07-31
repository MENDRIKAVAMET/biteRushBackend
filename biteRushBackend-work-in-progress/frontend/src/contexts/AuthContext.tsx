import React, { createContext, useState, useCallback, useContext, useEffect, type ReactNode } from 'react';
import type { User, AuthResponse } from '../types/api';
import { UserRole } from '../types/enums';
import { apiClient } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<User>;
  register: (data: {
    email: string;
    password: string;
    name: string;
    role?: string;
    phoneNumber?: string;
    address?: string;
    vehicule?: string;
    zone?: string;
    restaurantName?: string;
  }) => Promise<User>;
  logout: () => void;
  hasRole: (role: UserRole) => boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const safeGetUser = (): User | null => {
    try {
      const stored = localStorage.getItem('authUser');

      if (!stored || stored === 'undefined' || stored === 'null') {
        return null;
      }

      const parsed = JSON.parse(stored);

      // validation minimale pour éviter crash silencieux
      if (!parsed || typeof parsed !== 'object' || !parsed.roles) {
        localStorage.removeItem('authUser');
        return null;
      }

      return parsed as User;
    } catch {
      localStorage.removeItem('authUser');
      return null;
    }
  };
  const [user, setUser] = useState<User | null>(safeGetUser);

  const [token, setToken] = useState<string | null>(
    () => localStorage.getItem('authToken') || null
  );

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAuthResponse = useCallback((response: AuthResponse): User => {
    const userFromResponse: User = {
      id: response.id,
      email: (response as any).username ?? (response as any).email,
      name: response.name ?? (response as any).fullName ?? '',
      roles: [response.role as UserRole],
    };

    setUser(userFromResponse);
    setToken(response.token);
    apiClient.setToken(response.token);
    localStorage.setItem('authUser', JSON.stringify(userFromResponse));
    localStorage.setItem('authToken', response.token);

    return userFromResponse;
  }, []);

  const setUserFromMe = useCallback((userData: User) => {
    setUser(userData);
    localStorage.setItem('authUser', JSON.stringify(userData));
  }, []);

  const handleUnauthorized = useCallback(() => {
    setUser(null);
    setToken(null);
    apiClient.clearAuth();
    localStorage.removeItem('authUser');
    localStorage.removeItem('authToken');
    window.location.href = '/login';
  }, []);

  // Setup auto-logout on 401
  useEffect(() => {
    apiClient.onUnauthorized(handleUnauthorized);
  }, [handleUnauthorized]);

  // On mount, if a token exists try to fetch the current user from /auth/me
  useEffect(() => {
    let mounted = true;
    const init = async () => {
      if (!token) return;
      setLoading(true);
      try {
        const me = await apiClient.getCurrentUser();
        if (mounted && me) {
          setUserFromMe(me);
        }
      } catch (err) {
        // If token is invalid, trigger unauthorized flow to clean state
        handleUnauthorized();
      } finally {
        if (mounted) setLoading(false);
      }
    };

    init();

    return () => {
      mounted = false;
    };
  }, [token, handleUnauthorized, setUserFromMe]);

  const login = useCallback(
    async (email: string, password: string) => {
      setLoading(true);
      setError(null);
      try {
        const response = await apiClient.login({ email, password });
        return handleAuthResponse(response);
      } catch (err) {
        const message =
          err instanceof Error ? err.message : 'Erreur de connexion';
        setError(message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [handleAuthResponse]
  );

  const register = useCallback(
    async (data: {
      email: string;
      password: string;
      name: string;
      role?: string;
      phoneNumber?: string;
      address?: string;
      vehicule?: string;
      zone?: string;
      restaurantName?: string;
    }) => {
      setLoading(true);
      setError(null);
      try {
        const payload = {
          name: data.name,
          email: data.email,
          password: data.password,
          role: data.role,
          ...(data.phoneNumber ? { phoneNumber: data.phoneNumber } : {}),
          ...(data.address ? { address: data.address } : {}),
          ...(data.vehicule ? { vehicule: data.vehicule } : {}),
          ...(data.zone ? { zone: data.zone } : {}),
          ...(data.restaurantName ? { restaurantName: data.restaurantName } : {}),
          user: {
            name: data.name,
            email: data.email,
            password: data.password,
            role: data.role,
            ...(data.phoneNumber ? { phoneNumber: data.phoneNumber } : {}),
            ...(data.address ? { address: data.address } : {}),
            ...(data.vehicule ? { vehicule: data.vehicule } : {}),
            ...(data.zone ? { zone: data.zone } : {}),
            ...(data.restaurantName ? { restaurantName: data.restaurantName } : {}),
          },
          username: data.email,
          fullName: data.name,
        } as any;

        return handleAuthResponse(await apiClient.register(payload));
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Erreur lors de l\'inscription';
        setError(message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [handleAuthResponse]
  );

  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    apiClient.clearAuth();
    localStorage.removeItem('authUser');
    localStorage.removeItem('authToken');
  }, []);

  const hasRole = useCallback(
    (role: UserRole): boolean => {
      return user?.roles.includes(role) ?? false;
    },
    [user]
  );

  const value = React.useMemo(
    () => ({ user, token, loading, error, login, register, logout, hasRole }),
    [user, token, loading, error, login, register, logout, hasRole]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
