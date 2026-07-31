import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { UserRole } from '../types/enums';

export const UnauthorizedPage: React.FC = () => {
  const { hasRole } = useAuth();

  let homePath = '/';
  if (hasRole(UserRole.ADMIN)) homePath = '/admin/dashboard';
  else if (hasRole(UserRole.RESTAURANT_STAFF)) homePath = '/restaurant/dashboard';
  else if (hasRole(UserRole.LIVREUR)) homePath = '/deliveries';
  else if (hasRole(UserRole.CLIENT)) homePath = '/order-form';

  return (
    <div className="page">
      <h1>Accès refusé 🚫</h1>
      <p>Vous n’avez pas les permissions nécessaires pour ouvrir cette page.</p>
      <p>Retournez à votre espace pour continuer.</p>
      <Link to={homePath} className="btn btn-primary">Retour à l’espace</Link>
    </div>
  );
};
