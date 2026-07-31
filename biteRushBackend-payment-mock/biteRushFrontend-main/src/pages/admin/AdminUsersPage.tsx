import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/api';
import { useApp } from '../../contexts/AppContext';
import type { User } from '../../types/api';
import './AdminDashboard.css';

export const AdminUsersPage: React.FC = () => {
  const { addError } = useApp();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>('all');

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getAllUsers();
      setUsers(data);
    } catch (err) {
      addError('Impossible de charger les utilisateurs', 'error');
    } finally {
      setLoading(false);
    }
  };

  const filteredUsers = users.filter(user => {
    if (filter === 'all') return true;
    return user.roles.includes(filter as any);
  });

  const getRoleColor = (roles: any[]) => {
    if (roles.includes('ROLE_ADMIN')) return '#ff6b6b';
    if (roles.includes('ROLE_RESTAURANT_STAFF')) return '#4ecdc4';
    if (roles.includes('ROLE_LIVREUR')) return '#45b7d1';
    if (roles.includes('ROLE_CLIENT')) return '#96ceb4';
    return '#999';
  };

  const getRoleLabel = (roles: any[]) => {
    if (roles.includes('ROLE_ADMIN')) return 'Admin';
    if (roles.includes('ROLE_RESTAURANT_STAFF')) return 'Restaurant';
    if (roles.includes('ROLE_LIVREUR')) return 'Livreur';
    if (roles.includes('ROLE_CLIENT')) return 'Client';
    return 'Inconnu';
  };

  if (loading) {
    return <div className="loading">Chargement des utilisateurs...</div>;
  }

  return (
    <div className="admin-users">
      <h1>👥 Gestion des Utilisateurs</h1>

      <div className="users-filters">
        <label>Filtrer par rôle:</label>
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="all">Tous ({users.length})</option>
          <option value="ROLE_CLIENT">Clients ({users.filter(u => u.roles.includes('ROLE_CLIENT')).length})</option>
          <option value="ROLE_RESTAURANT_STAFF">Restaurants ({users.filter(u => u.roles.includes('ROLE_RESTAURANT_STAFF')).length})</option>
          <option value="ROLE_LIVREUR">Livreurs ({users.filter(u => u.roles.includes('ROLE_LIVREUR')).length})</option>
          <option value="ROLE_ADMIN">Admins ({users.filter(u => u.roles.includes('ROLE_ADMIN')).length})</option>
        </select>
      </div>

      <div className="users-table-container">
        <table className="users-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nom</th>
              <th>Email</th>
              <th>Rôle</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.map(user => (
              <tr key={user.id}>
                <td>#{user.id}</td>
                <td>{user.firstName} {user.lastName}</td>
                <td>{user.email}</td>
                <td>
                  <span 
                    className="role-badge"
                    style={{ backgroundColor: getRoleColor(user.roles) }}
                  >
                    {getRoleLabel(user.roles)}
                  </span>
                </td>
                <td>
                  <div className="user-actions">
                    <button className="btn btn-sm btn-info" title="Voir détails">
                      👁️
                    </button>
                    <button className="btn btn-sm btn-warning" title="Éditer">
                      ✏️
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {filteredUsers.length === 0 && (
        <div className="empty-state">
          <p>Aucun utilisateur trouvé</p>
        </div>
      )}

      <div className="quick-actions">
        <button onClick={loadUsers} className="btn btn-primary">
          🔄 Actualiser
        </button>
      </div>
    </div>
  );
};
