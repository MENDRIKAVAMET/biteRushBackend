import React from 'react';
import { Link } from 'react-router-dom';
import type { RestaurantDTO } from '../types/api';
import './RestaurantCard.css';

interface Props {
  restaurant: RestaurantDTO;
}

export const RestaurantCard: React.FC<Props> = ({ restaurant }) => {
  return (
    <Link to={`/restaurant/${restaurant.id}`} className="restaurant-card">
      <div className="restaurant-image" style={{ backgroundImage: `url(${restaurant.imageUrl ?? '/placeholder-restaurant.png'})` }} />
      <div className="restaurant-body">
        <div className="restaurant-title">{restaurant.name}</div>
        <div className="restaurant-meta">
          <span className="restaurant-rating">⭐ {restaurant.rating?.toFixed(1) ?? '—'}</span>
          <span className="restaurant-delivery">· {restaurant.deliveryTime ?? 30} min</span>
        </div>
      </div>
    </Link>
  );
};

export default RestaurantCard;
