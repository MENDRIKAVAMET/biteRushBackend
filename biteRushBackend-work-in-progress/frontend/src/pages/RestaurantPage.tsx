import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiClient } from '../services/api';
import { useCart } from '../contexts/CartContext';
import type { MenuItemDTO, RestaurantDTO } from '../types/api';
import { MenuItemCard } from '../components/MenuItemCard';
import './RestaurantPage.css';

export const RestaurantPage: React.FC = () => {
  const { id } = useParams();
  const [restaurant, setRestaurant] = useState<RestaurantDTO | null>(null);
  const [menuItems, setMenuItems] = useState<MenuItemDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const { addItem } = useCart();

  useEffect(() => {
    const load = async () => {
      if (!id) return;

      try {
        const restaurantId = Number(id);
        const [restaurantData, menuData] = await Promise.all([
          apiClient.getRestaurantById(restaurantId),
          apiClient.getMenuItems(restaurantId),
        ]);

        setRestaurant(restaurantData);
        setMenuItems(menuData.filter((item) => item.available));
      } catch (error) {
        console.error('Unable to load restaurant details', error);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [id]);

  if (loading) {
    return <div className="page">Chargement du restaurant…</div>;
  }

  if (!restaurant) {
    return <div className="page">Restaurant introuvable.</div>;
  }

  return (
    <div className="page">
      <Link to="/" className="nav-link">← Retour à l’accueil</Link>
      <h1>{restaurant.name}</h1>
      <p>{restaurant.address}</p>
      <p>Temps de livraison estimé : {restaurant.deliveryTime ?? 30} min</p>

      <div className="menu-list" style={{ marginTop: '1.5rem' }}>
        {menuItems.length === 0 ? (
          <div className="empty-state">
            <p>Aucun plat disponible pour le moment dans ce restaurant.</p>
          </div>
        ) : (
          menuItems.map((item) => (
            <MenuItemCard
              key={item.id}
              item={item}
              onAdd={(it) => addItem(it, restaurant.id, restaurant.name)}
            />
          ))
        )}
      </div>
    </div>
  );
};
