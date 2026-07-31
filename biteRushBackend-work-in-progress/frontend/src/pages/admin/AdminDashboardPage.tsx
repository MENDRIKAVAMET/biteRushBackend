import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/api';
import { useApp } from '../../contexts/AppContext';
import { Link } from 'react-router-dom';
import './AdminDashboard.css';

interface Statistics {
  totalClients: number;
  totalRestaurants: number;
  totalDeliveryPersons: number;
  totalOrders: number;
  totalRevenue: number;
}

export const AdminDashboardPage: React.FC = () => {
  const { addError } = useApp();
  const [stats, setStats] = useState<Statistics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStatistics();
  }, []);

  const loadStatistics = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getAdminStatistics();
      setStats(data);
    } catch (err) {
      addError('Impossible de charger les statistiques', 'error');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="admin-dashboard">
        <div className="loading">Chargement des statistiques...</div>
      </div>
    );
  }

  if (!stats) {
    return (
      <div className="admin-dashboard">
        <div className="error">Impossible de charger les données</div>
      </div>
    );
  }

  return (
    <div className="admin-dashboard">
      <h1>📊 Dashboard Admin</h1>
      
      <div className="stats-grid">
        <div className="stat-card clients">
          <div className="stat-icon">👥</div>
          <div className="stat-content">
            <div className="stat-value">{stats.totalClients}</div>
            <div className="stat-label">Clients</div>
          </div>
        </div>

        <div className="stat-card restaurants">
          <div className="stat-icon">🍽️</div>
          <div className="stat-content">
            <div className="stat-value">{stats.totalRestaurants}</div>
            <div className="stat-label">Restaurants</div>
          </div>
        </div>

        <div className="stat-card delivery">
          <div className="stat-icon">🚗</div>
          <div className="stat-content">
            <div className="stat-value">{stats.totalDeliveryPersons}</div>
            <div className="stat-label">Livreurs</div>
          </div>
        </div>

        <div className="stat-card orders">
          <div className="stat-icon">📦</div>
          <div className="stat-content">
            <div className="stat-value">{stats.totalOrders}</div>
            <div className="stat-label">Commandes</div>
          </div>
        </div>

        <div className="stat-card revenue">
          <div className="stat-icon">💰</div>
          <div className="stat-content">
            <div className="stat-value">{stats.totalRevenue.toFixed(0)} Ar</div>
            <div className="stat-label">Revenu Total</div>
          </div>
        </div>
      </div>

      <div className="management-section">
        <h2>🔧 Gestion</h2>
        <div className="management-grid">
          <Link to="/admin/users" className="management-card">
            <div className="card-icon">👤</div>
            <div className="card-title">Utilisateurs</div>
            <div className="card-desc">Gérer clients, restaurants, livreurs</div>
          </Link>

          <Link to="/admin/restaurants" className="management-card">
            <div className="card-icon">🏪</div>
            <div className="card-title">Restaurants</div>
            <div className="card-desc">Gérer restaurants et leurs stats</div>
          </Link>

          <Link to="/admin/charts" className="management-card">
            <div className="card-icon">📈</div>
            <div className="card-title">Graphiques</div>
            <div className="card-desc">Voir tendances et performances</div>
          </Link>
        </div>
      </div>

      <div className="quick-actions">
        <button onClick={loadStatistics} className="btn btn-primary">
          🔄 Actualiser
        </button>
      </div>
    </div>
  );
};
