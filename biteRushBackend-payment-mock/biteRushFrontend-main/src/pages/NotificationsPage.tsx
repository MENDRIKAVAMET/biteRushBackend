import React, { useState, useEffect, useMemo } from 'react';
import { apiClient } from '../services/api';
import { useWebSocket } from '../contexts/WebSocketContext';
import { useAuth } from '../hooks/useAuth';
import { useApp } from '../contexts/AppContext';
import { Pagination } from '../components/Pagination';
import type { NotificationDTO } from '../types/api';
import './NotificationsPage.css';

export const NotificationsPage: React.FC = () => {
  const { wsService, isConnected } = useWebSocket();
  const { user } = useAuth();
  const { addError } = useApp();
  const [notifications, setNotifications] = useState<NotificationDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterUnread, setFilterUnread] = useState(false);
  const [sortBy, setSortBy] = useState<'date' | 'type'>('date');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 15;

  useEffect(() => {
    loadNotifications();
  }, []);

  useEffect(() => {
    if (!wsService || !user || !isConnected) return;

    wsService.subscribe(
      `/topic/notifications/${user.id}`,
      (message: unknown) => {
        const newNotification = message as NotificationDTO;
        console.log('📬 New notification via WebSocket:', newNotification);
        setNotifications((prev) => [newNotification, ...prev]);
      }
    );

    return () => {
      wsService.unsubscribe(`/topic/notifications/${user.id}`);
    };
  }, [wsService, user, isConnected]);

  const loadNotifications = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getNotifications();
      setNotifications(data);
    } catch (err) {
      addError('Impossible de charger les notifications', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await apiClient.markNotificationAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
    } catch (err) {
      addError('Erreur', 'error');
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await apiClient.markAllNotificationsAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      addError('Toutes les notifications marquées comme lues', 'success');
    } catch (err) {
      addError('Erreur', 'error');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await apiClient.deleteNotification(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      addError('Notification supprimée', 'success');
    } catch (err) {
      addError('Erreur', 'error');
    }
  };

  const filteredAndSorted = useMemo(() => {
    let filtered = notifications;

    if (filterUnread) {
      filtered = filtered.filter((n) => !n.read);
    }

    let sorted = [...filtered];
    sorted.sort((a, b) => {
      let comparison = 0;
      if (sortBy === 'date') {
        comparison =
          new Date(a.createdAt).getTime() -
          new Date(b.createdAt).getTime();
      } else {
        comparison = a.type.localeCompare(b.type);
      }
      return sortOrder === 'asc' ? comparison : -comparison;
    });

    return sorted;
  }, [notifications, filterUnread, sortBy, sortOrder]);

  const totalPages = Math.ceil(filteredAndSorted.length / itemsPerPage);
  const paginatedNotifications = filteredAndSorted.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  if (loading) {
    return (
      <div className="page">
        <h1>Notifications</h1>
        <div className="loading">Chargement...</div>
      </div>
    );
  }

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <div className="page">
      <div className="notifications-header">
        <h1>Notifications ({unreadCount} non-lues)</h1>
        {isConnected && <span className="ws-badge">🟢 Temps réel</span>}
        {!isConnected && <span className="ws-badge offline">🔴 Offline</span>}
        {unreadCount > 0 && (
          <button className="btn btn-sm" onClick={handleMarkAllAsRead}>
            Tout marquer comme lu
          </button>
        )}
      </div>

      <div className="filters-container">
        <div className="filter-group">
          <label>
            <input
              type="checkbox"
              checked={filterUnread}
              onChange={(e) => {
                setFilterUnread(e.target.checked);
                setCurrentPage(1);
              }}
            />
            Non-lues uniquement
          </label>
        </div>

        <div className="filter-group">
          <label>Trier par:</label>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value as 'date' | 'type')}>
            <option value="date">Date</option>
            <option value="type">Type</option>
          </select>
        </div>

        <div className="filter-group">
          <button
            className={`sort-btn ${sortOrder === 'asc' ? 'active' : ''}`}
            onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
            title={sortOrder === 'asc' ? 'Croissant' : 'Décroissant'}
          >
            {sortOrder === 'asc' ? '▲' : '▼'}
          </button>
        </div>
      </div>

      {notifications.length === 0 ? (
        <div className="empty-state">
          <p>Aucune notification</p>
        </div>
      ) : filteredAndSorted.length === 0 ? (
        <div className="empty-state">
          <p>Aucune notification ne correspond aux filtres</p>
        </div>
      ) : (
        <>
          <div className="notifications-list">
            {paginatedNotifications.map((notification) => (
              <div
                key={notification.id}
                className={`notification-item ${notification.read ? 'read' : 'unread'}`}
              >
                <div className="notification-content">
                  <h3>{notification.message}</h3>
                  <p className="notification-type">{notification.type}</p>
                  <p className="notification-date">
                    {new Date(notification.createdAt).toLocaleString('fr-FR')}
                  </p>
                </div>

                <div className="notification-actions">
                  {!notification.read && (
                    <button
                      className="btn btn-sm"
                      onClick={() => handleMarkAsRead(notification.id)}
                    >
                      ✓ Lire
                    </button>
                  )}
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(notification.id)}
                  >
                    🗑️ Supprimer
                  </button>
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
              itemsPerPage={itemsPerPage}
              totalItems={filteredAndSorted.length}
            />
          )}
        </>
      )}
    </div>
  );
};
