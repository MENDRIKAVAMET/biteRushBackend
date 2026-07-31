import React, { useState, useEffect, useMemo } from 'react';
import { apiClient } from '../../services/api';
import { useApp } from '../../contexts/AppContext';
import { ConfirmModal } from '../../components/ConfirmModal';
import { Pagination } from '../../components/Pagination';
import type { DeliveryDTO } from '../../types/api';
import { DeliveryStatus, DeliveryStatusLabels } from '../../types/enums';
import './MyDeliveries.css';

export const MyDeliveriesPage: React.FC = () => {
  const { addError } = useApp();
  const [deliveries, setDeliveries] = useState<DeliveryDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [sortBy, setSortBy] = useState<'date' | 'status'>('date');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    deliveryId: number | null;
    action: 'complete' | 'cancel' | null;
  }>({ isOpen: false, deliveryId: null, action: null });

  useEffect(() => {
    loadDeliveries();
  }, []);

  const loadDeliveries = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getMyDeliveries();
      setDeliveries(data);
    } catch (err) {
      addError('Impossible de charger les livraisons', 'error');
    } finally {
      setLoading(false);
    }
  };

  const filteredAndSorted = useMemo(() => {
    let filtered = deliveries;

    if (filterStatus !== 'all') {
      filtered = filtered.filter((d) => d.status === filterStatus);
    }

    let sorted = [...filtered];
    sorted.sort((a, b) => {
      let comparison = 0;
      if (sortBy === 'date') {
        comparison =
          new Date(a.createdAt).getTime() -
          new Date(b.createdAt).getTime();
      } else {
        comparison = a.status.localeCompare(b.status);
      }
      return sortOrder === 'asc' ? comparison : -comparison;
    });

    return sorted;
  }, [deliveries, filterStatus, sortBy, sortOrder]);

  const totalPages = Math.ceil(filteredAndSorted.length / itemsPerPage);
  const paginatedDeliveries = filteredAndSorted.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const handleAccept = async (deliveryId: number) => {
    try {
      await apiClient.acceptDelivery(deliveryId);
      loadDeliveries();
      addError('Livraison acceptée', 'success');
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const handleComplete = async (deliveryId: number) => {
    try {
      await apiClient.completeDelivery(deliveryId);
      loadDeliveries();
      addError('Livraison complétée', 'success');
      setConfirmModal({ isOpen: false, deliveryId: null, action: null });
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const handleCancel = async (deliveryId: number) => {
    try {
      await apiClient.cancelDelivery(deliveryId);
      loadDeliveries();
      addError('Livraison annulée', 'success');
      setConfirmModal({ isOpen: false, deliveryId: null, action: null });
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  if (loading) {
    return <div className="page">Chargement...</div>;
  }

  return (
    <div className="page">
      <h1>Mes livraisons</h1>

      <div className="filters-container">
        <div className="filter-group">
          <label>Statut:</label>
          <select value={filterStatus} onChange={(e) => {
            setFilterStatus(e.target.value);
            setCurrentPage(1);
          }}>
            <option value="all">Tous ({deliveries.length})</option>
            {Object.entries(DeliveryStatusLabels).map(([status, label]) => (
              <option key={status} value={status}>
                {label} ({deliveries.filter(d => d.status === status).length})
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label>Trier par:</label>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value as 'date' | 'status')}>
            <option value="date">Date</option>
            <option value="status">Statut</option>
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
          {filterStatus !== 'all' ? 'Aucune livraison ne correspond aux filtres' : 'Aucune livraison assignée'}
        </p>
      ) : (
        <>
          <div className="deliveries-list">
            {paginatedDeliveries.map((delivery) => (
              <div key={delivery.id} className="delivery-card">
                <div className="delivery-header">
                  <h3>Livraison #{delivery.id}</h3>
                  <span className={`status-badge status-${delivery.status.toLowerCase()}`}>
                    {DeliveryStatusLabels[delivery.status as DeliveryStatus]}
                  </span>
                </div>
                {delivery.order && (
                  <div className="delivery-details">
                    <p><strong>📦 Commande:</strong> #{delivery.order.id}</p>
                    <p><strong>📍 À:</strong> {delivery.order.deliveryAddress}</p>
                    <p><strong>📞 Téléphone:</strong> {delivery.order.phoneNumber}</p>
                    <p><strong>💰 Montant:</strong> {delivery.order.totalAmount.toFixed(2)} Ar</p>
                    <p><strong>🕐 Date:</strong> {new Date(delivery.createdAt).toLocaleString('fr-FR')}</p>
                  </div>
                )}
                <div className="delivery-actions">
                  {delivery.status === DeliveryStatus.ASSIGNED && (
                    <button
                      onClick={() => handleAccept(delivery.id)}
                      className="btn btn-primary"
                    >
                      ✓ Accepter
                    </button>
                  )}
                  {delivery.status === DeliveryStatus.IN_PROGRESS && (
                    <button
                      onClick={() => setConfirmModal({
                        isOpen: true,
                        deliveryId: delivery.id,
                        action: 'complete'
                      })}
                      className="btn btn-success"
                    >
                      ✓ Complétée
                    </button>
                  )}
                  {(delivery.status === DeliveryStatus.ASSIGNED ||
                    delivery.status === DeliveryStatus.IN_PROGRESS) && (
                    <button
                      onClick={() => setConfirmModal({
                        isOpen: true,
                        deliveryId: delivery.id,
                        action: 'cancel'
                      })}
                      className="btn btn-danger"
                    >
                      ✕ Annuler
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
        isOpen={confirmModal.isOpen && confirmModal.action === 'complete'}
        title="Confirmer la livraison"
        message="Êtes-vous sûr d'avoir livré cette commande?"
        confirmLabel="Oui, complétée"
        cancelLabel="Non, retour"
        onConfirm={() => confirmModal.deliveryId && handleComplete(confirmModal.deliveryId)}
        onCancel={() => setConfirmModal({ isOpen: false, deliveryId: null, action: null })}
      />

      <ConfirmModal
        isOpen={confirmModal.isOpen && confirmModal.action === 'cancel'}
        title="Annuler la livraison"
        message="Êtes-vous sûr de vouloir annuler cette livraison?"
        confirmLabel="Oui, annuler"
        cancelLabel="Non, retour"
        isDangerous={true}
        onConfirm={() => confirmModal.deliveryId && handleCancel(confirmModal.deliveryId)}
        onCancel={() => setConfirmModal({ isOpen: false, deliveryId: null, action: null })}
      />
    </div>
  );
};
