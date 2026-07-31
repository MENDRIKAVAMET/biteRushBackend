import React, { useState, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useApp } from '../../contexts/AppContext';
import { apiClient } from '../../services/api';
import type { ClientResponseDTO } from '../../types/api';
import './Profile.css';

export const ClientProfilePage: React.FC = () => {
  const { user } = useAuth();
  const { addError } = useApp();
  const [profile, setProfile] = useState<ClientResponseDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [showPasswordForm, setShowPasswordForm] = useState(false);

  // Edit form state
  const [name, setName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [address, setAddress] = useState('');
  const [addresses, setAddresses] = useState<string[]>([]);
  const [newAddress, setNewAddress] = useState('');

  // Password form state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordLoading, setPasswordLoading] = useState(false);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getClientProfile();
      setProfile(data);
      setName(data.user.name || '');
      setPhoneNumber(data.phoneNumber || '');
      setAddress(data.address || '');
      setAddresses(data.deliveryAddresses || []);
    } catch (err) {
      addError('Impossible de charger le profil', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateProfile = async () => {
    if (!name.trim()) {
      addError('Le nom est requis', 'error');
      return;
    }

    if (!phoneNumber.trim() || phoneNumber.length < 9) {
      addError('Numéro téléphone invalide', 'error');
      return;
    }

    try {
      await apiClient.updateClientProfile({
        name,
        phoneNumber,
        address,
        deliveryAddresses: addresses,
      });
      addError('Profil mis à jour', 'success');
      setIsEditing(false);
      loadProfile();
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    }
  };

  const handleAddAddress = () => {
    if (newAddress.trim()) {
      setAddresses([...addresses, newAddress]);
      setNewAddress('');
    }
  };

  const handleRemoveAddress = (index: number) => {
    setAddresses(addresses.filter((_, i) => i !== index));
  };

  const handleChangePassword = async () => {
    if (!currentPassword || !newPassword || !confirmPassword) {
      addError('Tous les champs sont requis', 'error');
      return;
    }

    if (newPassword !== confirmPassword) {
      addError('Les mots de passe ne correspondent pas', 'error');
      return;
    }

    if (newPassword.length < 6) {
      addError('Le mot de passe doit contenir au moins 6 caractères', 'error');
      return;
    }

    try {
      setPasswordLoading(true);
      // API endpoint for changing password (to be implemented in backend)
      await apiClient.changePassword({
        currentPassword,
        newPassword,
      });
      addError('Mot de passe changé avec succès', 'success');
      setShowPasswordForm(false);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur', 'error');
    } finally {
      setPasswordLoading(false);
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

  if (!profile || !user) {
    return (
      <div className="page">
        <h1>Mon Profil</h1>
        <div className="error-message">Impossible de charger le profil</div>
      </div>
    );
  }

  return (
    <div className="page profile-page">
      <h1>Mon Profil Client</h1>

      <div className="profile-card">
        <div className="profile-section">
          <h2>Informations Personnelles</h2>
          <div className="info-group">
            <label>Email</label>
            <p>{user.email}</p>
          </div>
          <div className="info-group">
            <label>Nom</label>
            <p>{user.name || `${user.firstName || ''} ${user.lastName || ''}`.trim()}</p>
          </div>
        </div>

        {!isEditing ? (
          <>
            <div className="profile-section">
              <h2>Contact & Adresses</h2>
              <div className="info-group">
                <label>Numéro de téléphone</label>
                <p>{phoneNumber || 'Non renseigné'}</p>
              </div>
              <div className="info-group">
                <label>Adresses de livraison</label>
                {addresses.length > 0 ? (
                  <ul className="addresses-list">
                    {addresses.map((addr, idx) => (
                      <li key={idx}>{addr}</li>
                    ))}
                  </ul>
                ) : (
                  <p className="empty">Aucune adresse</p>
                )}
              </div>
            </div>

            <div className="profile-actions">
              <button className="btn btn-primary" onClick={() => setIsEditing(true)}>
                ✏️ Modifier le profil
              </button>
              <button className="btn btn-secondary" onClick={() => setShowPasswordForm(!showPasswordForm)}>
                🔐 Changer le mot de passe
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="profile-section">
              <h2>Modifier Contact & Adresses</h2>
              <div className="form-group">
                <label htmlFor="name">Nom</label>
                <input
                  id="name"
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>

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
                <label htmlFor="address">Adresse</label>
                <input
                  id="address"
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label>Adresses de livraison</label>
                <div className="addresses-edit">
                  {addresses.map((addr, idx) => (
                    <div key={idx} className="address-item">
                      <span>{addr}</span>
                      <button
                        type="button"
                        className="btn btn-sm btn-danger"
                        onClick={() => handleRemoveAddress(idx)}
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                  <div className="add-address">
                    <input
                      type="text"
                      placeholder="Nouvelle adresse..."
                      value={newAddress}
                      onChange={(e) => setNewAddress(e.target.value)}
                    />
                    <button
                      type="button"
                      className="btn btn-sm"
                      onClick={handleAddAddress}
                    >
                      +
                    </button>
                  </div>
                </div>
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

        {showPasswordForm && (
          <div className="profile-section password-section">
            <h2>Changer le Mot de Passe</h2>
            <div className="form-group">
              <label htmlFor="current-pwd">Mot de passe actuel</label>
              <input
                id="current-pwd"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label htmlFor="new-pwd">Nouveau mot de passe</label>
              <input
                id="new-pwd"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label htmlFor="confirm-pwd">Confirmer mot de passe</label>
              <input
                id="confirm-pwd"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>
            <div className="password-actions">
              <button
                className="btn btn-primary"
                onClick={handleChangePassword}
                disabled={passwordLoading}
              >
                {passwordLoading ? 'Changement...' : '🔐 Changer'}
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setShowPasswordForm(false);
                  setCurrentPassword('');
                  setNewPassword('');
                  setConfirmPassword('');
                }}
              >
                Annuler
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
