import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/api';
import { useWebSocket } from '../../contexts/WebSocketContext';
import { useAuth } from '../../hooks/useAuth';
import { useApp } from '../../contexts/AppContext';
import type { RestaurantDashboardDTO } from '../../types/api';
import './RestaurantDashboard.css';

export const RestaurantDashboardPage: React.FC = () => {
  const { wsService, isConnected } = useWebSocket();
  const { user } = useAuth();
  const { addError } = useApp();
  const [dashboard, setDashboard] = useState<RestaurantDashboardDTO | null>(null);
  const [loading, setLoading] = useState(true);

  // Initial load
  useEffect(() => {
    loadDashboard();
  }, []);

  // WebSocket subscription for restaurant dashboard updates
  useEffect(() => {
    if (!wsService || !user || !isConnected) return;

    const restaurantId = 1; // TODO: Get from user.restaurantId
    wsService.subscribe(
      `/topic/orders/dashboard/${restaurantId}`,
      (updatedDashboard: unknown) => {
        console.log('📊 Dashboard updated via WebSocket:', updatedDashboard);
        setDashboard(updatedDashboard as RestaurantDashboardDTO);
      }
    );

    return () => {
      wsService.unsubscribe(`/topic/orders/dashboard/${restaurantId}`);
    };
  }, [wsService, user, isConnected]);

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getRestaurantDashboard();
      setDashboard(data);
    } catch (err) {
      addError('Impossible de charger le dashboard', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleAcceptOrder = async (orderId: number) => {
    try {
      await apiClient.acceptOrder(orderId);
      addError('Commande acceptée', 'success');
      loadDashboard();
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const handleMarkReady = async (orderId: number) => {
    try {
      await apiClient.markOrderReady(orderId);
      addError('Commande marquée prête', 'success');
      loadDashboard();
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const handleAssignDelivery = async (orderId: number) => {
    try {
      const deliveryPersonId = 1; // TODO: Modal to select delivery person
      await apiClient.assignDelivery(orderId, { deliveryPersonId });
      addError('Livreur assigné', 'success');
      loadDashboard();
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  if (loading) {
    return (
      <div className="page">
        <h1>Dashboard Restaurant</h1>
        <div className="loading">Chargement...</div>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="page">
        <h1>Dashboard Restaurant</h1>
        <div className="error-message">Impossible de charger le dashboard</div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="dashboard-header">
        <h1>Dashboard Restaurant</h1>
        {isConnected && <span className="ws-badge">🟢 Temps réel</span>}
        {!isConnected && <span className="ws-badge offline">🔴 Offline</span>}
      </div>

      <div className="dashboard-stats">
        <div className="stat-box">
          <h3>En attente</h3>
          <p className="stat-number">{dashboard.pendingCount}</p>
        </div>
        <div className="stat-box">
          <h3>En préparation</h3>
          <p className="stat-number">{dashboard.preparingCount}</p>
        </div>
        <div className="stat-box">
          <h3>Prête</h3>
          <p className="stat-number">{dashboard.readyCount}</p>
        </div>
      </div>

      <div className="dashboard-grid">
        {/* Pending Orders */}
        <div className="dashboard-column">
          <h2>📋 En attente ({dashboard.pendingOrders?.length || 0})</h2>
          <div className="orders-list">
            {dashboard.pendingOrders?.map((order) => (
              <div key={order.id} className="order-card">
                <h3>Commande #{order.id}</h3>
                <p className="order-total">{order.totalAmount?.toFixed(2)} Ar</p>
                <div className="order-items">
                  {order.items?.map((item, idx) => (
                    <p key={idx} className="item">
                      {item.quantity}x {item.menuItem?.name}
                      {item.comment && ` - ${item.comment}`}
                    </p>
                  ))}
                </div>
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => handleAcceptOrder(order.id)}
                >
                  ✓ Accepter
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Preparing Orders */}
        <div className="dashboard-column">
          <h2>👨‍🍳 En préparation ({dashboard.preparingOrders?.length || 0})</h2>
          <div className="orders-list">
            {dashboard.preparingOrders?.map((order) => (
              <div key={order.id} className="order-card">
                <h3>Commande #{order.id}</h3>
                <p className="order-total">{order.totalAmount?.toFixed(2)} Ar</p>
                <div className="order-items">
                  {order.items?.map((item, idx) => (
                    <p key={idx} className="item">
                      {item.quantity}x {item.menuItem?.name}
                    </p>
                  ))}
                </div>
                <button
                  className="btn btn-success btn-sm"
                  onClick={() => handleMarkReady(order.id)}
                >
                  ✓ Prête
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Ready Orders */}
        <div className="dashboard-column">
          <h2>✅ Prête ({dashboard.readyOrders?.length || 0})</h2>
          <div className="orders-list">
            {dashboard.readyOrders?.map((order) => (
              <div key={order.id} className="order-card">
                <h3>Commande #{order.id}</h3>
                <p className="order-total">{order.totalAmount?.toFixed(2)} Ar</p>
                <div className="order-items">
                  {order.items?.map((item, idx) => (
                    <p key={idx} className="item">
                      {item.quantity}x {item.menuItem?.name}
                    </p>
                  ))}
                </div>
                <button
                  className="btn btn-info btn-sm"
                  onClick={() => handleAssignDelivery(order.id)}
                >
                  🚗 Assigner Livreur
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
