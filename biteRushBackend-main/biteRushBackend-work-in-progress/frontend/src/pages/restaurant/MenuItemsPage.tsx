import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/api';
import { useApp } from '../../contexts/AppContext';
import { useAuth } from '../../contexts/AuthContext';
import type { MenuItemDTO } from '../../types/api';
import './MenuManagement.css';

interface Category {
  id: number;
  name: string;
}

export const MenuItemsPage: React.FC<{ restaurantId?: number }> = ({ 
  restaurantId: propRestaurantId
}) => {
  const { addError } = useApp();
  const { user } = useAuth();
  const [restaurantId, setRestaurantId] = useState<number | null>(propRestaurantId ?? null);
  const [items, setItems] = useState<MenuItemDTO[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Form state
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [available, setAvailable] = useState(true);

  useEffect(() => {
    const fetchRestaurantId = async () => {
      if (propRestaurantId) {
        setRestaurantId(propRestaurantId);
        return;
      }
      
      try {
        const profile = await apiClient.getRestaurantStaffProfile();
        if (profile.restaurantId) {
          setRestaurantId(profile.restaurantId);
        }
      } catch (err) {
        addError('Impossible de charger le restaurant', 'error');
      }
    };

    fetchRestaurantId();
  }, [propRestaurantId, user]);

  useEffect(() => {
    if (restaurantId) {
      loadData();
    }
  }, [restaurantId]);

  const loadData = async () => {
    if (!restaurantId) return;
    try {
      setLoading(true);
      const [itemsData, catsData] = await Promise.all([
        apiClient.getMenuItems(restaurantId),
        apiClient.getMenuCategories(restaurantId),
      ]);
      setItems(itemsData);
      setCategories(catsData);
    } catch (err) {
      addError('Impossible de charger les données', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!name.trim() || !price || !restaurantId) {
      addError('Données manquantes', 'error');
      return;
    }

    try {
      if (editingId) {
        await apiClient.updateMenuItem(restaurantId, editingId, {
          name,
          price: parseFloat(price),
          description,
          categoryId: categoryId ? parseInt(categoryId) : undefined,
          available,
        });
        addError('Article mis à jour', 'success');
      } else {
        await apiClient.createMenuItem(restaurantId, {
          name,
          price: parseFloat(price),
          description,
          categoryId: categoryId ? parseInt(categoryId) : undefined,
        });
        addError('Article créé', 'success');
      }
      resetForm();
      loadData();
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const handleEdit = (item: MenuItemDTO) => {
    setEditingId(item.id);
    setName(item.name);
    setPrice(item.price.toString());
    setDescription(item.description || '');
    setCategoryId('');
    setAvailable(true);
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Supprimer cet article?')) return;
    if (!restaurantId) return;

    try {
      await apiClient.deleteMenuItem(restaurantId, id);
      addError('Article supprimé', 'success');
      loadData();
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const handleToggleAvailability = async (item: MenuItemDTO) => {
    if (!restaurantId) return;
    try {
      await apiClient.toggleMenuItemAvailability(restaurantId, item.id, !item.available);
      addError(
        !item.available ? 'Article activé' : 'Article désactivé',
        'success'
      );
      loadData();
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const resetForm = () => {
    setName('');
    setPrice('');
    setDescription('');
    setCategoryId('');
    setAvailable(true);
    setEditingId(null);
    setShowForm(false);
  };

  const filteredItems = items.filter((item) =>
    item.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="page">
        <h1>Gestion Articles</h1>
        <div className="loading">Chargement...</div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>🍽️ Articles du Menu</h1>
        <button 
          className="btn btn-primary"
          onClick={() => showForm ? resetForm() : setShowForm(true)}
        >
          {showForm ? '✕ Annuler' : '➕ Nouvel Article'}
        </button>
      </div>

      <div className="search-box">
        <input
          type="text"
          placeholder="Rechercher article..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {showForm && (
        <div className="form-card">
          <h2>{editingId ? 'Éditer' : 'Nouvel'} Article</h2>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="name">Nom *</label>
              <input
                id="name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Ex: Pizza Margherita"
              />
            </div>
            <div className="form-group">
              <label htmlFor="price">Prix *</label>
              <input
                id="price"
                type="number"
                step="0.01"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="Ex: 12.99"
              />
            </div>
            <div className="form-group">
              <label htmlFor="category">Catégorie</label>
              <select
                id="category"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
              >
                <option value="">-- Sélectionner --</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>
            {editingId && (
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    checked={available}
                    onChange={(e) => setAvailable(e.target.checked)}
                  />
                  Disponible
                </label>
              </div>
            )}
          </div>
          <div className="form-group">
            <label htmlFor="desc">Description</label>
            <textarea
              id="desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Description optionnelle..."
              rows={3}
            />
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" onClick={handleSave}>
              💾 Enregistrer
            </button>
            <button className="btn btn-secondary" onClick={resetForm}>
              Annuler
            </button>
          </div>
        </div>
      )}

      <div className="items-table">
        {filteredItems.length === 0 ? (
          <div className="empty-state">
            <p>Aucun article {searchTerm && `contenant "${searchTerm}"`}</p>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Prix</th>
                <th>Catégorie</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item) => (
                <tr key={item.id} className={!item.available ? 'inactive' : ''}>
                  <td className="name-cell">
                    <strong>{item.name}</strong>
                    {item.description && (
                      <div className="description">{item.description}</div>
                    )}
                  </td>
                  <td>{item.price.toFixed(2)} Ar</td>
                  <td>
                    {item.categoryId ? (
                      categories.find((c) => c.id === item.categoryId)?.name || '-'
                    ) : (
                      '-'
                    )}
                  </td>
                  <td>
                    <span className={`status ${item.available ? 'available' : 'unavailable'}`}>
                      {item.available ? '🟢 Disponible' : '🔴 Indisponible'}
                    </span>
                  </td>
                  <td className="actions-cell">
                    <button
                      className="btn btn-sm"
                      onClick={() => handleToggleAvailability(item)}
                    >
                      {item.available ? '👁️‍🗨️ Désactiver' : '👁️ Activer'}
                    </button>
                    <button
                      className="btn btn-sm"
                      onClick={() => handleEdit(item)}
                    >
                      ✏️ Éditer
                    </button>
                    <button
                      className="btn btn-sm btn-danger"
                      onClick={() => handleDelete(item.id)}
                    >
                      🗑️ Supprimer
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
