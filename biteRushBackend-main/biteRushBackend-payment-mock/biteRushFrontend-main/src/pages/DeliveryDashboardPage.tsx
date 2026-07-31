import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useWebSocket } from '../contexts/WebSocketContext';
import { useApp } from '../contexts/AppContext';
import { UserRole, DeliveryStatus } from '../types/enums';
import { apiClient } from '../services/api';
import type { DeliveryDTO } from '../types/api';
import './DeliveryDashboardPage.css';

export const DeliveryDashboardPage: React.FC = () => {
  const { hasRole, user } = useAuth();
  const { wsService, isConnected } = useWebSocket();
  const { addError } = useApp();
  const [deliveries, setDeliveries] = useState<DeliveryDTO[]>([]);
  const [restaurantNames, setRestaurantNames] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (error) {
      addError(error, 'error', 5000);
      setError(null);
    }
  }, [error, addError]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const data = await apiClient.getDeliveriesForMe();
        setDeliveries(data || []);
      } catch (err) {
        console.error('Failed to load deliveries', err);
        setError('Impossible de charger vos livraisons.');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  // WebSocket subscription for delivery updates
  useEffect(() => {
    if (!wsService || !user || !isConnected) return;

    const dest = `/topic/deliveries/${user.id}`;

    wsService.subscribe(dest, (msg: unknown) => {
      try {
        const payload = msg as DeliveryDTO;
        setDeliveries((cur) => {
          const exists = cur.some((d) => d.id === payload.id);
          if (exists) {
            return cur.map((d) => (d.id === payload.id ? payload : d));
          }
          return [payload, ...cur];
        });
        addError('Mise à jour de livraison reçue', 'info', 2500);
      } catch (err) {
        console.error('Invalid delivery update', err);
      }
    });

    return () => {
      if (wsService && user) wsService.unsubscribe(dest);
    };
  }, [wsService, user, isConnected, addError]);

  useEffect(() => {
    const loadRestaurants = async () => {
      const ids = Array.from(
        new Set(deliveries.map((d) => d.order?.restaurantId).filter(Boolean))
      ) as number[];

      if (ids.length === 0) return;

      try {
        const results = await Promise.all(ids.map((id) => apiClient.getRestaurantById(id)));
        const map: Record<number, string> = {};

        results.forEach((restaurant) => {
          if (restaurant?.id) {
            map[restaurant.id] = restaurant.name;
          }
        });

        setRestaurantNames(map);
      } catch {
        // ignore restaurant name errors
      }
    };

    void loadRestaurants();
  }, [deliveries]);

  if (!hasRole(UserRole.LIVREUR)) {
    return <Navigate to="/unauthorized" replace />;
  }

  const updateLocal = (id: number, patch: Partial<DeliveryDTO>) => {
    setDeliveries((cur) => cur.map((d) => (d.id === id ? { ...d, ...patch } : d)));
  };

  const handleStart = async (id: number) => {
    try {
      updateLocal(id, { status: DeliveryStatus.IN_PROGRESS });
      await apiClient.startDelivery(id);
      updateLocal(id, { status: DeliveryStatus.IN_PROGRESS });
    } catch (err) {
      console.error(err);
      setError('Impossible de démarrer la livraison.');
      // reload
      const refreshed = await apiClient.getDeliveriesForMe();
      setDeliveries(refreshed || []);
    }
  };

  const handleDeliver = async (id: number) => {
    try {
      updateLocal(id, { status: DeliveryStatus.DELIVERED });
      await apiClient.deliverDelivery(id);
      updateLocal(id, { status: DeliveryStatus.DELIVERED });
    } catch (err) {
      console.error(err);
      setError('Impossible de marquer la commande comme livrée.');
      const refreshed = await apiClient.getDeliveriesForMe();
      setDeliveries(refreshed || []);
    }
  };

  const statusClass = (status?: string) => {
    switch (status) {
      case DeliveryStatus.ASSIGNED:
        return 'badge badge-pending';
      case DeliveryStatus.IN_PROGRESS:
        return 'badge badge-inprogress';
      case DeliveryStatus.DELIVERED:
        return 'badge badge-done';
      case DeliveryStatus.CANCELLED:
        return 'badge badge-cancelled';
      default:
        return 'badge badge-unknown';
    }
  };

  return (
    <div className="page delivery-board-page">
      <h1>Tableau de bord livreur</h1>
      {/* errors converted to toasts */}

      {loading ? (
        <p>Chargement des livraisons…</p>
      ) : (
        <div className="task-board">
          {deliveries.length === 0 && <div>Aucune livraison assignée.</div>}
          {deliveries.map((d) => (
            <div key={d.id} className="task-card">
              <div className="task-card-header">
                <div className={statusClass(d.status)}>{d.status}</div>
                <div className="task-id">#{d.id}</div>
              </div>

              <div className="task-body">
                <div className="task-row"><strong>Adresse client:</strong> {d.order?.deliveryAddress ?? '—'}</div>
                <div className="task-row"><strong>Restaurant:</strong> {d.order?.restaurantId ? (restaurantNames[d.order.restaurantId] ?? `#${d.order.restaurantId}`) : '—'}</div>
                <div className="task-row"><strong>Statut commande:</strong> {d.order?.status ?? '—'}</div>
              </div>

              <div className="task-actions">
                {(d.status !== DeliveryStatus.IN_PROGRESS && d.status !== DeliveryStatus.DELIVERED && d.status !== DeliveryStatus.CANCELLED) && (
                  <button className="btn btn-primary" onClick={() => void handleStart(d.id)}>Démarrer la livraison</button>
                )}

                {d.status !== DeliveryStatus.DELIVERED && (
                  <button className="btn" onClick={() => void handleDeliver(d.id)}>Commande livrée</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
