import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../../services/api';
import { useWebSocket } from '../../contexts/WebSocketContext';
import { useApp } from '../../contexts/AppContext';
import { ConfirmModal } from '../../components/ConfirmModal';
import { Pagination } from '../../components/Pagination';
import type { OrderDTO } from '../../types/api';
import { OrderStatus, OrderStatusLabels } from '../../types/enums';
import './MyOrders.css';

export const MyOrdersPage: React.FC = () => {
  const { addError } = useApp();
  const [orders, setOrders] = useState<OrderDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'date' | 'amount'>('date');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    orderId: number | null;
  }>({ isOpen: false, orderId: null });

  useEffect(() => {
    loadOrders();
  }, []);

  const { wsService, isConnected } = useWebSocket();

  // subscribe to order updates for the current user
  useEffect(() => {
    if (!wsService || !isConnected) return;

    const dest = `/topic/orders/user`; // backend may target by authenticated user token

    wsService.subscribe(dest, (message: unknown) => {
      try {
        const updated = message as OrderDTO;
        setOrders((prev) => {
          const exists = prev.some((o) => o.id === updated.id);
          if (exists) return prev.map((o) => (o.id === updated.id ? updated : o));
          return [updated, ...prev];
        });
        addError('Mise à jour de commande reçue', 'info', 3000);
      } catch (err) {
        console.error('Invalid order update', err);
      }
    });

    return () => {
      try {
        wsService.unsubscribe(dest);
      } catch {
        // ignore
      }
    };
  }, [wsService, isConnected, addError]);

  const loadOrders = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getMyOrders();
      setOrders(data);
    } catch (err) {
      addError('Impossible de charger vos commandes', 'error');
    } finally {
      setLoading(false);
    }
  };

  const filteredAndSorted = useMemo(() => {
    let filtered = orders;

    if (filterStatus !== 'all') {
      filtered = filtered.filter(
        (order) => order.status === filterStatus
      );
    }

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (order) =>
          order.id.toString().includes(term) ||
          order.deliveryAddress.toLowerCase().includes(term)
      );
    }

    let sorted = [...filtered];
    sorted.sort((a, b) => {
      let comparison = 0;
      if (sortBy === 'date') {
        comparison =
          new Date(a.createdAt).getTime() -
          new Date(b.createdAt).getTime();
      } else {
        comparison = a.totalAmount - b.totalAmount;
      }
      return sortOrder === 'asc' ? comparison : -comparison;
    });

    return sorted;
  }, [orders, filterStatus, searchTerm, sortBy, sortOrder]);

  const totalPages = Math.ceil(filteredAndSorted.length / itemsPerPage);
  const paginatedOrders = filteredAndSorted.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const handleCancelOrder = async (orderId: number) => {
    try {
      await apiClient.cancelOrder(orderId);
      setOrders((prev) =>
        prev.map((order) =>
          order.id === orderId ? { ...order, status: 'ANNULEE' as any } : order
        )
      );
      addError('Commande annulée', 'success');
      setConfirmModal({ isOpen: false, orderId: null });
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  if (loading) {
    return <div className="page">Chargement...</div>;
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Mes commandes</h1>
        <Link to="/order-form" className="btn btn-primary">
          + Nouvelle commande
        </Link>
      </div>

      <div className="filters-container">
        <div className="filter-group">
          <label>Statut:</label>
          <select value={filterStatus} onChange={(e) => {
            setFilterStatus(e.target.value);
            setCurrentPage(1);
          }}>
            <option value="all">Tous ({orders.length})</option>
            {Object.entries(OrderStatusLabels).map(([status, label]) => (
              <option key={status} value={status}>
                {label} ({orders.filter(o => o.status === status).length})
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label>Rechercher:</label>
          <input
            type="text"
            placeholder="N° commande ou adresse..."
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setCurrentPage(1);
            }}
          />
        </div>

        <div className="filter-group">
          <label>Trier par:</label>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value as 'date' | 'amount')}>
            <option value="date">Date</option>
            <option value="amount">Montant</option>
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

      {filteredAndSorted.length === 0 ? (
        <p className="empty-state">
          {searchTerm || filterStatus !== 'all'
            ? 'Aucune commande ne correspond aux filtres'
            : 'Vous n\'avez encore aucune commande'}
        </p>
      ) : (
        <>
          <div className="orders-list">
            {paginatedOrders.map((order) => (
              <div key={order.id} className="order-card">
                <div className="order-header">
                  <h3>Commande #{order.id}</h3>
                  <span className={`status-badge status-${order.status.toLowerCase()}`}>
                    {OrderStatusLabels[order.status as OrderStatus]}
                  </span>
                </div>

                <div className="order-details">
                  <p>
                    <strong>Montant:</strong> {order.totalAmount.toFixed(2)} Ar
                  </p>
                  <p>
                    <strong>Adresse:</strong> {order.deliveryAddress}
                  </p>
                  <p>
                    <strong>Articles:</strong> {order.items.length}
                  </p>
                  <p>
                    <strong>Date:</strong>{' '}
                    {new Date(order.createdAt).toLocaleString()}
                  </p>
                </div>

                <div className="order-actions">
                  <Link to={`/orders/${order.id}`} className="btn btn-secondary">
                    Détails
                  </Link>
                  {order.status === OrderStatus.EN_ATTENTE && (
                    <button
                      onClick={() => setConfirmModal({ isOpen: true, orderId: order.id })}
                      className="btn btn-danger"
                    >
                      Annuler
                    </button>
                  )}
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

      <ConfirmModal
        isOpen={confirmModal.isOpen}
        title="Annuler la commande"
        message="Êtes-vous sûr de vouloir annuler cette commande? Cette action est irréversible."
        confirmLabel="Annuler la commande"
        cancelLabel="Non, retour"
        isDangerous={true}
        onConfirm={() => confirmModal.orderId && handleCancelOrder(confirmModal.orderId)}
        onCancel={() => setConfirmModal({ isOpen: false, orderId: null })}
      />
    </div>
  );
};
