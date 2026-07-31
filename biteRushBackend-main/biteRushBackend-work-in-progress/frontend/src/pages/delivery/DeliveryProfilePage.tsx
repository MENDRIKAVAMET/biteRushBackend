import React, { useState, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useApp } from '../../contexts/AppContext';
import { apiClient } from '../../services/api';
import '../client/Profile.css';

export const DeliveryProfilePage: React.FC = () => {
  const { user } = useAuth();
  const { addError } = useApp();
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [isAvailable, setIsAvailable] = useState(false);

  // Edit form state
  const [phoneNumber, setPhoneNumber] = useState('');
  const [vehicleType, setVehicleType] = useState('');
  const [zone, setZone] = useState('');
  const [vehiclePlate, setVehiclePlate] = useState('');

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const profile = await apiClient.getDeliveryProfile();
      setPhoneNumber(profile.phoneNumber || '');
      setVehicleType(profile.vehicule || '');
      setZone(profile.zone || '');
      setVehiclePlate('');
      setIsAvailable(profile.available ?? true);
    } catch (err) {
      addError('Impossible de charger le profil', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateProfile = async () => {
    try {
      await apiClient.updateDeliveryProfile({
        phoneNumber,
        vehicule: vehicleType,
        zone,
      });
      addError('Profil mis à jour', 'success');
      setIsEditing(false);
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const handleToggleAvailability = async () => {
    try {
      await apiClient.toggleAvailability(!isAvailable);
      setIsAvailable(!isAvailable);
      addError(isAvailable ? 'Indisponible' : 'Disponible', 'success');
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
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
      <h1>Mon Profil Livreur</h1>

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
          <div className="info-group">
            <label>Disponibilité</label>
            <div className="availability-toggle">
              <span className={`status ${isAvailable ? 'available' : 'unavailable'}`}>
                {isAvailable ? '🟢 Disponible' : '🔴 Indisponible'}
              </span>
              <button
                className="btn btn-sm"
                onClick={handleToggleAvailability}
              >
                {isAvailable ? 'Se marquer indisponible' : 'Se rendre disponible'}
              </button>
            </div>
          </div>
        </div>

        {!isEditing ? (
          <>
            <div className="profile-section">
              <h2>Informations Véhicule</h2>
              <div className="info-group">
                <label>Numéro de téléphone</label>
                <p>{phoneNumber}</p>
              </div>
              <div className="info-group">
                <label>Type de véhicule</label>
                <p>{vehicleType}</p>
              </div>
              <div className="info-group">
                <label>Zone</label>
                <p>{zone || 'Non renseignée'}</p>
              </div>
              <div className="info-group">
                <label>Immatriculation</label>
                <p>{vehiclePlate || 'Non renseignée'}</p>
              </div>
            </div>

            <div className="profile-actions">
              <button className="btn btn-primary" onClick={() => setIsEditing(true)}>
                ✏️ Modifier le profil
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="profile-section">
              <h2>Modifier Informations</h2>
              <div className="form-group">
                <label htmlFor="phone">Numéro de téléphone</label>
                <input
                  id="phone"
                  type="tel"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label htmlFor="vehicle">Type de véhicule</label>
                <select
                  id="vehicle"
                  value={vehicleType}
                  onChange={(e) => setVehicleType(e.target.value)}
                >
                  <option>Moto</option>
                  <option>Vélo</option>
                  <option>Auto</option>
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="zone">Zone</label>
                <input
                  id="zone"
                  type="text"
                  value={zone}
                  onChange={(e) => setZone(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label htmlFor="plate">Immatriculation</label>
                <input
                  id="plate"
                  type="text"
                  value={vehiclePlate}
                  onChange={(e) => setVehiclePlate(e.target.value)}
                />
              </div>
            </div>

            <div className="profile-actions">
              <button className="btn btn-primary" onClick={handleUpdateProfile}>
                💾 Enregistrer
              </button>
              <button className="btn btn-secondary" onClick={() => setIsEditing(false)}>
                ✕ Annuler
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};
