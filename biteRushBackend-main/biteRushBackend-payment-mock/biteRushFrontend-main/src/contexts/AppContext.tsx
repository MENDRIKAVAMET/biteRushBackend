import React from 'react';
import type { ReactNode } from 'react';
import { createContext, useContext, useState, useCallback } from 'react';

export interface AppError {
  id: string;
  message: string;
  type: 'error' | 'warning' | 'success' | 'info';
  duration?: number;
  action?: {
    label: string;
    // callback id to invoke (stored in ref)
    callbackId: string;
  } | null;
}

interface AppContextType {
  errors: AppError[];
  loading: boolean;
  addError: (
    message: string,
    type?: 'error' | 'warning' | 'success' | 'info',
    duration?: number,
    action?: { label: string; onClick: () => void } | null
  ) => void;
  removeError: (id: string) => void;
  clearErrors: () => void;
  setLoading: (loading: boolean) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [errors, setErrors] = useState<AppError[]>([]);
  const [loading, setLoading] = useState(false);
  // hold callbacks in a ref to avoid serializing functions into state
  const actionCallbacks = React.useRef<Record<string, () => void>>({});

  // Listen for toast action events dispatched from ToastContainer
  React.useEffect(() => {
    const handler = (e: Event) => {
      try {
        // @ts-ignore
        const detail = (e as CustomEvent).detail;
        const id = detail?.id as string | undefined;
        if (!id) return;
        const cb = actionCallbacks.current[id];
        if (cb) cb();
      } catch (err) {
        // ignore
      }
    };

    window.addEventListener('toast-action', handler as EventListener);
    return () => window.removeEventListener('toast-action', handler as EventListener);
  }, []);

  const addError = useCallback((
    message: string,
    type: 'error' | 'warning' | 'success' | 'info' = 'error',
    duration = 5000,
    action: { label: string; onClick: () => void } | null = null
  ) => {
    const id = Date.now().toString();
    const error: AppError = { id, message, type, duration, action: action ? { label: action.label, callbackId: id } : null };

    if (action) {
      actionCallbacks.current[id] = action.onClick;
    }

    setErrors(prev => [...prev, error]);

    if (duration > 0) {
      setTimeout(() => removeError(id), duration);
    }
  }, []);

  const removeError = useCallback((id: string) => {
    setErrors(prev => prev.filter(e => e.id !== id));
    // clear stored callback if present
    if (actionCallbacks.current[id]) {
      delete actionCallbacks.current[id];
    }
  }, []);

  const clearErrors = useCallback(() => {
    setErrors([]);
  }, []);

  return (
    <AppContext.Provider value={{ errors, loading, addError, removeError, clearErrors, setLoading }}>
      {children}
    </AppContext.Provider>
  );
};

export const useApp = (): AppContextType => {
  const context = useContext(AppContext);
  if (!context) throw new Error('useApp must be used within AppProvider');
  return context;
};
