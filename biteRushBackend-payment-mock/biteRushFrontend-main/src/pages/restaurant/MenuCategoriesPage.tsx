import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/api';
import { useApp } from '../../contexts/AppContext';
import { useAuth } from '../../contexts/AuthContext';
import './MenuManagement.css';

interface Category {
  id: number;
  name: string;
  description?: string;
}

export const MenuCategoriesPage: React.FC<{ restaurantId?: number }> = ({ 
  restaurantId: propRestaurantId
}) => {
  const { addError } = useApp();
  const { user } = useAuth();
  const [restaurantId, setRestaurantId] = useState<number | null>(propRestaurantId ?? null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  // Form state
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

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
      loadCategories();
    }
  }, [restaurantId]);

  const loadCategories = async () => {
    if (!restaurantId) return;
    try {
      setLoading(true);
      const data = await apiClient.getMenuCategories(restaurantId);
      setCategories(data);
    } catch (err) {
      addError('Impossible de charger les catégories', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!name.trim() || !restaurantId) {
      addError('Données manquantes', 'error');
      return;
    }

    try {
      if (editingId) {
        await apiClient.updateMenuCategory(restaurantId, editingId, {
          name,
          description,
        });
        addError('Catégorie mise à jour', 'success');
      } else {
        await apiClient.createMenuCategory(restaurantId, {
          name,
          description,
        });
        addError('Catégorie créée', 'success');
      }
      resetForm();
      loadCategories();
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const handleEdit = (category: Category) => {
    setEditingId(category.id);
    setName(category.name);
    setDescription(category.description || '');
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Supprimer cette catégorie?')) return;
    if (!restaurantId) return;

    try {
      await apiClient.deleteMenuCategory(restaurantId, id);
      addError('Catégorie supprimée', 'success');
      loadCategories();
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const resetForm = () => {
    setName('');
    setDescription('');
    setEditingId(null);
    setShowForm(false);
  };

  if (loading) {
    return (
      <div className="page">
        <h1>Gestion Catégories</h1>
        <div className="loading">Chargement...</div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>📂 Catégories du Menu</h1>
        <button 
          className="btn btn-primary"
          onClick={() => showForm ? resetForm() : setShowForm(true)}
        >
          {showForm ? '✕ Annuler' : '➕ Nouvelle Catégorie'}
        </button>
      </div>

      {showForm && (
        <div className="form-card">
          <h2>{editingId ? 'Éditer' : 'Nouvelle'} Catégorie</h2>
          <div className="form-group">
            <label htmlFor="name">Nom *</label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ex: Pizzas, Burgers, Desserts"
            />
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

      <div className="categories-grid">
        {categories.length === 0 ? (
          <div className="empty-state">
            <p>Aucune catégorie. Créez-en une pour commencer!</p>
          </div>
        ) : (
          categories.map((cat) => (
            <div key={cat.id} className="category-card">
              <h3>{cat.name}</h3>
              {cat.description && <p>{cat.description}</p>}
              <div className="card-actions">
                <button
                  className="btn btn-sm"
                  onClick={() => handleEdit(cat)}
                >
                  ✏️ Éditer
                </button>
                <button
                  className="btn btn-sm btn-danger"
                  onClick={() => handleDelete(cat.id)}
                >
                  🗑️ Supprimer
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
