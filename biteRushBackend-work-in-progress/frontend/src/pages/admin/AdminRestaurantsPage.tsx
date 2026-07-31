import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/api';
import { useApp } from '../../contexts/AppContext';
import type { RestaurantDTO } from '../../types/api';
import './AdminDashboard.css';

interface RestaurantStats {
  restaurant: RestaurantDTO;
  orderCount: number;
  totalRevenue: number;
  avgOrderValue: number;
}

export const AdminRestaurantsPage: React.FC = () => {
  const { addError } = useApp();
  const [restaurants, setRestaurants] = useState<RestaurantDTO[]>([]);
  const [restaurantStats, setRestaurantStats] = useState<Map<number, RestaurantStats>>(new Map());
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    loadRestaurants();
  }, []);

  const loadRestaurants = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getAllRestaurants();
      setRestaurants(data);
      
      // Load stats for each restaurant
      const statsMap = new Map<number, RestaurantStats>();
      for (const restaurant of data) {
        try {
          const stats = await apiClient.getRestaurantDetails(restaurant.id);
          statsMap.set(restaurant.id, stats);
        } catch (err) {
          console.error(`Failed to load stats for restaurant ${restaurant.id}`, err);
        }
      }
      setRestaurantStats(statsMap);
    } catch (err) {
      addError('Impossible de charger les restaurants', 'error');
    } finally {
      setLoading(false);
    }
  };

  const toggleExpand = (id: number) => {
    setExpandedId(expandedId === id ? null : id);
  };

  if (loading) {
    return <div className="loading">Chargement des restaurants...</div>;
  }

  return (
    <div className="admin-restaurants">
      <h1>🏪 Gestion des Restaurants</h1>

      <div className="restaurants-info">
        <p>Total: <strong>{restaurants.length}</strong> restaurants</p>
      </div>

      <div className="restaurants-grid">
        {restaurants.map(restaurant => {
          const stats = restaurantStats.get(restaurant.id);
          return (
            <div key={restaurant.id} className="restaurant-card">
              <div className="restaurant-header" onClick={() => toggleExpand(restaurant.id)}>
                <div className="restaurant-info">
                  <h3>{restaurant.name}</h3>
                  <p className="restaurant-cuisine">{restaurant.cuisine || 'Cuisine variée'}</p>
                </div>
                <div className="expand-icon">
                  {expandedId === restaurant.id ? '▼' : '▶'}
                </div>
              </div>

              {expandedId === restaurant.id && (
                <div className="restaurant-details">
                  <div className="detail-row">
                    <span className="label">📍 Adresse:</span>
                    <span className="value">{restaurant.address}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">📞 Téléphone:</span>
                    <span className="value">{restaurant.phoneNumber}</span>
                  </div>

                  {stats && (
                    <>
                      <div className="stats-divider"></div>
                      <div className="detail-row">
                        <span className="label">📦 Commandes:</span>
                        <span className="value">{stats.orderCount}</span>
                      </div>
                      <div className="detail-row">
                        <span className="label">💰 Revenu total:</span>
                        <span className="value">{stats.totalRevenue.toFixed(2)} Ar</span>
                      </div>
                      <div className="detail-row">
                        <span className="label">📊 Revenu moyen/cmd:</span>
                        <span className="value">{stats.avgOrderValue.toFixed(2)} Ar</span>
                      </div>
                    </>
                  )}

                  <div className="restaurant-actions">
                    <button className="btn btn-sm btn-info">👁️ Détails</button>
                    <button className="btn btn-sm btn-warning">✏️ Éditer</button>
                    <button className="btn btn-sm btn-danger">🗑️ Supprimer</button>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {restaurants.length === 0 && (
        <div className="empty-state">
          <p>Aucun restaurant trouvé</p>
        </div>
      )}

      <div className="quick-actions">
        <button onClick={loadRestaurants} className="btn btn-primary">
          🔄 Actualiser
        </button>
      </div>
    </div>
  );
};
