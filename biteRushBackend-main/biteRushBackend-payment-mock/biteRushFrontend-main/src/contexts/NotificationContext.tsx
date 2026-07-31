import React, {
  createContext,
  useState,
  useCallback,
  useEffect,
  type ReactNode,
  useRef,
} from 'react';

import type { NotificationDTO } from '../types/api';
import { apiClient } from '../services/api';
import { getWebSocketService } from '../services/websocket';
import { useAuth } from '../hooks/useAuth';

interface NotificationContextType {
  notifications: NotificationDTO[];
  unreadCount: number;
  loading: boolean;

  addNotification: (notification: NotificationDTO) => void;
  markAsRead: (id: number) => Promise<void>;
  markAllAsRead: () => Promise<void>;
  deleteNotification: (id: number) => Promise<void>;
  refreshNotifications: () => Promise<void>;
}

export const NotificationContext =
  createContext<NotificationContextType | undefined>(undefined);

interface Props {
  children: ReactNode;
}

export const NotificationProvider: React.FC<Props> = ({ children }) => {
  const [notifications, setNotifications] = useState<NotificationDTO[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const wsRef = useRef<any>(null);

  // =========================
  // LOAD INIT NOTIFICATIONS
  // =========================
  const refreshNotifications = useCallback(async () => {
    try {
      setLoading(true);

      const [list, count] = await Promise.all([
        apiClient.getNotifications(),
        apiClient.getUnreadCount(),
      ]);

      setNotifications(list);
      setUnreadCount(count);
    } catch (err: any) {
      // Ignore auth/permission errors when not logged in
      if (err?.response?.status === 401 || err?.response?.status === 403) {
        console.debug('Skipping notifications load due to auth:', err?.response?.status);
        return;
      }
      console.error('Failed to load notifications:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  // =========================
  // ADD NOTIFICATION (REALTIME)
  // =========================
  const addNotification = useCallback((notification: NotificationDTO) => {
    setNotifications((prev) => [notification, ...prev]);

    if (!notification.read) {
      setUnreadCount((prev) => prev + 1);
    }
  }, []);

  // =========================
  // MARK AS READ
  // =========================
  const markAsRead = useCallback(async (id: number) => {
    try {
      await apiClient.markNotificationAsRead(id);

      setNotifications((prev) =>
        prev.map((n) =>
          n.id === id ? { ...n, read: true } : n
        )
      );

      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      console.error('markAsRead failed:', err);
      throw err;
    }
  }, []);

  // =========================
  // MARK ALL AS READ
  // =========================
  const markAllAsRead = useCallback(async () => {
    try {
      await apiClient.markAllNotificationsAsRead();

      setNotifications((prev) =>
        prev.map((n) => ({ ...n, read: true }))
      );

      setUnreadCount(0);
    } catch (err) {
      console.error('markAllAsRead failed:', err);
      throw err;
    }
  }, []);

  // =========================
  // DELETE NOTIFICATION
  // =========================
  const deleteNotification = useCallback(async (id: number) => {
    try {
      await apiClient.deleteNotification(id);

      setNotifications((prev) => {
        const target = prev.find((n) => n.id === id);

        if (target && !target.read) {
          setUnreadCount((c) => Math.max(0, c - 1));
        }

        return prev.filter((n) => n.id !== id);
      });
    } catch (err) {
      console.error('deleteNotification failed:', err);
      throw err;
    }
  }, []);

  // =========================
  // INIT + WEBSOCKET
  // =========================
  const { user, token } = useAuth();

  useEffect(() => {
    if (!token || !user) return;

    const run = async () => {
      try {
        await refreshNotifications();
      } catch (err) {
        // suppress auth errors (e.g., 403) when token is invalid
        console.debug('Notifications refresh skipped:', err);
      }
    };

    run();

    const ws = getWebSocketService(
      import.meta.env.VITE_API_URL,
      token
    );

    wsRef.current = ws;

    ws.connect()
      .then(() => {
        ws.subscribe('/topic/notifications', (msg: any) => {
          addNotification(msg);
        });

        ws.subscribe('/user/queue/notifications', (msg: any) => {
          addNotification(msg);
        });
      })
      .catch((err: any) => {
        console.debug('WebSocket init failed:', err);
      });

    return () => {
      try { ws.disconnect(); } catch (_) {}
    };
  }, [refreshNotifications, addNotification, token, user]);

  // =========================
  // CONTEXT VALUE
  // =========================
  const value: NotificationContextType = {
    notifications,
    unreadCount,
    loading,

    addNotification,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    refreshNotifications,
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
};