import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { apiClient } from '../../services/api';
import type { OrderDTO } from '../../types/api';
import { OrderStatusLabels } from '../../types/enums';

export const OrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<OrderDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadOrder();
    const interval = setInterval(loadOrder, 5000);
    return () => clearInterval(interval);
  }, [id]);

  const loadOrder = async () => {
    try {
      if (!id) return;
      const data = await apiClient.getOrderById(Number(id));
      setOrder(data);
    } catch (err) {
      console.error('Failed to load order:', err);
      setError('Impossible de charger la commande');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="page">Chargement...</div>;
  }

  if (error || !order) {
    return <div className="page"><div className="error-message">{error || 'Commande non trouvée'}</div></div>;
  }

  return (
    <div className="page">
      <h1>Commande #{order.id}</h1>

      <div className="order-detail-card">
        <div className="detail-section">
          <h2>Statut</h2>
          <p className={`status-badge status-${order.status.toLowerCase()}`}>
            {OrderStatusLabels[order.status]}
          </p>
        </div>

        <div className="detail-section">
          <h2>Informations</h2>
          <p><strong>Montant:</strong> {order.totalAmount.toFixed(2)} Ar</p>
          <p><strong>Adresse:</strong> {order.deliveryAddress}</p>
          <p><strong>Téléphone:</strong> {order.phoneNumber}</p>
          <p><strong>Date:</strong> {new Date(order.createdAt).toLocaleString()}</p>
        </div>

        <div className="detail-section">
          <h2>Articles ({order.items.length})</h2>
          <table className="items-table">
            <thead>
              <tr>
                <th>Article</th>
                <th>Quantité</th>
                <th>Prix unitaire</th>
              </tr>
            </thead>
            <tbody>
              {order.items.map((item) => (
                <tr key={item.id}>
                  <td>{item.menuItem.name}</td>
                  <td>{item.quantity}</td>
                  <td>{item.menuItem.price.toFixed(2)} Ar</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {order.delivery && (
          <div className="detail-section">
            <h2>Livraison</h2>
            <p><strong>Livreur:</strong> {order.delivery.deliveryPerson?.firstName} {order.delivery.deliveryPerson?.lastName}</p>
          </div>
        )}
      </div>
    </div>
  );
};
