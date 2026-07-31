import React, { useState, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { apiClient } from '../../services/api';
import '../client/Profile.css';

export const RestaurantStaffProfilePage: React.FC = () => {
  const { user } = useAuth();
  const [profile, setProfile] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      // TODO: Implement getRestaurantStaffProfile in API
      const staffProfile = await apiClient.getRestaurantStaffProfile();
      setProfile(staffProfile);
    } catch (err) {
      // Fallback
      setProfile({
        id: user?.id,
        user: user,
        restaurantId: 1,
        restaurantName: 'Restaurant ABC',
      });
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="page">
        <h1>Mon Profil</h1>
        <div className="loading">Chargement...</div>
      </div>
    );
  }

  return (
    <div className="page profile-page">
      <h1>Mon Profil - Restaurant Staff</h1>

      <div className="profile-card">
        <div className="profile-section">
          <h2>Informations Personnelles</h2>
          <div className="info-group">
            <label>Email</label>
            <p>{user?.email}</p>
          </div>
          <div className="info-group">
            <label>Nom</label>
            <p>{user?.firstName} {user?.lastName}</p>
          </div>
        </div>

        <div className="profile-section">
          <h2>Restaurant</h2>
          <div className="info-group">
            <label>Nom du restaurant</label>
            <p>{profile?.restaurantName || 'Restaurant'}</p>
          </div>
          <div className="info-group">
            <label>ID Restaurant</label>
            <p>{profile?.restaurantId}</p>
          </div>
        </div>

        <div className="profile-info">
          <p>📋 Pour modifier les informations du restaurant, contactez l'administrateur.</p>
        </div>
      </div>
    </div>
  );
};
