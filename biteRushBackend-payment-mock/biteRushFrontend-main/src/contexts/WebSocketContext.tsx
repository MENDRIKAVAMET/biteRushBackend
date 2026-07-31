import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

import { getWebSocketService, resetWebSocketService } from '../services/websocket';
import { useAuth } from '../hooks/useAuth';
import type { WebSocketService } from '../services/websocket';

interface WebSocketContextType {
  wsService: WebSocketService | null;
  isConnected: boolean;
  connectionError: string | null;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(
  undefined
);

export const WebSocketProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const { token, user } = useAuth();

  const [wsService, setWsService] = useState<WebSocketService | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);

  useEffect(() => {
    // ❌ si pas connecté => reset propre
    if (!token || !user) {
      resetWebSocketService();
      setWsService(null);
      setIsConnected(false);
      setConnectionError(null);
      return;
    }

    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';

    const service = getWebSocketService(apiUrl, token);
    setWsService(service);

    let isMounted = true;

    const connect = async () => {
      try {
        await service.connect();

        if (!isMounted) return;

        setIsConnected(true);
        setConnectionError(null);
      } catch (err) {
        if (!isMounted) return;

        const msg =
          err instanceof Error ? err.message : 'WebSocket connection failed';

        setConnectionError(msg);
        setIsConnected(false);
      }
    };

    connect();

    // 🔥 plus besoin de polling 5s (SUPPRIMÉ)
    const interval = setInterval(() => {
      if (!isMounted) return;
      setIsConnected(service.isConnected());
    }, 3000);

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [token, user]);

  return (
    <WebSocketContext.Provider
      value={{ wsService, isConnected, connectionError }}
    >
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = (): WebSocketContextType => {
  const context = useContext(WebSocketContext);

  if (!context) {
    throw new Error('useWebSocket must be used within WebSocketProvider');
  }

  return context;
};