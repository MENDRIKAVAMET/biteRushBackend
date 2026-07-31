import React, { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useCart } from '../contexts/CartContext';
import { apiClient } from '../services/api';
import type { RestaurantDTO } from '../types/api';
import { RestaurantCard } from '../components/RestaurantCard';
import './HomePage.css';

export const HomePage: React.FC = () => {
  const [restaurants, setRestaurants] = useState<RestaurantDTO[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const { itemCount } = useCart();

  useEffect(() => {
    const loadRestaurants = async () => {
      try {
        const data = await apiClient.getRestaurants();
        setRestaurants(data);
      } catch (error) {
        console.error('Unable to load restaurants', error);
      } finally {
        setLoading(false);
      }
    };

    loadRestaurants();
  }, []);

  // Server-side search with debounce
  const searchRef = useRef<number | null>(null);
  const handleSearch = (q: string) => {
    setSearchQuery(q);
    if (searchRef.current) window.clearTimeout(searchRef.current);
    searchRef.current = window.setTimeout(async () => {
      setLoading(true);
      try {
        if (!q || q.trim().length < 2) {
          const data = await apiClient.getRestaurants();
          setRestaurants(data);
        } else {
          const data = await apiClient.searchRestaurants(q.trim());
          setRestaurants(data);
        }
      } catch (err) {
        console.error('Search failed', err);
      } finally {
        setLoading(false);
      }
    }, 400);
  };

  return (
    <div className="page">
      <div className="home-header">
        <div>
          <h1>Restaurants à proximité</h1>
          <p>Choisissez un établissement et ajoutez vos plats au panier.</p>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <input
            value={searchQuery}
            placeholder="Rechercher un restaurant..."
            aria-label="Rechercher"
            onChange={(e) => handleSearch(e.target.value)}
            style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #e5e7eb', minWidth: 220 }}
          />
          <Link to="/cart" className="nav-link">Panier ({itemCount})</Link>
        </div>
      </div>

      {!user && (
        <div style={{ marginTop: '1rem' }}>
          <Link to="/login">Se connecter</Link> {' '} <Link to="/register">Créer un compte</Link>
        </div>
      )}

      {(() => {
        if (loading) {
          return <p>Chargement…</p>;
        }

        if (restaurants.length === 0) {
          return (
            <div className="empty-state">
              <p>Aucun restaurant trouvé{searchQuery ? ` pour « ${searchQuery} »` : ''}.</p>
              <p>Essayez un autre terme de recherche ou revenez à la page d'accueil.</p>
            </div>
          );
        }

        return (
          <div className="restaurant-grid">
            {restaurants.map((restaurant) => (
              <RestaurantCard key={restaurant.id} restaurant={restaurant} />
            ))}
          </div>
        );
      })()}
    </div>
  );
};
