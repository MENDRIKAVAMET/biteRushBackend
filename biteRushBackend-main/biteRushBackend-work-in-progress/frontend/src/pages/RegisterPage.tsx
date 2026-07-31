import React, { useState } from 'react';
import { useApp } from '../contexts/AppContext';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { UserRole } from '../types/enums';
import { useAuth } from '../hooks/useAuth';
import { getFriendlyErrorMessage } from '../utils/errorUtils';
import { getRoleHomePath } from '../utils/navigationUtils';
import './Auth.css';

export const RegisterPage: React.FC = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'ROLE_CLIENT',
    phoneNumber: '',
    address: '',
    vehicule: '',
    zone: '',
    restaurantName: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const { register, user, token } = useAuth();
  const { addError } = useApp();
  React.useEffect(() => {
    if (error) {
      addError(error, 'error', 6000);
      setError('');
    }
  }, [error, addError]);
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  if (token && user) {
    return <Navigate to={getRoleHomePath(user.roles)} replace />;
  }

  const validateForm = () => {
    const errs: Record<string, string> = {};

    if (!formData.name.trim()) errs.name = 'Le nom est requis.';
    if (!formData.email.trim()) errs.email = 'L’email est requis.';
    if (!formData.password) errs.password = 'Le mot de passe est requis.';
    if (formData.password !== formData.confirmPassword) errs.confirmPassword = 'Les mots de passe ne correspondent pas.';

    if (formData.role === UserRole.LIVREUR) {
      if (!formData.phoneNumber.trim()) errs.phoneNumber = 'Le numéro de téléphone est requis pour un livreur.';
      if (!formData.vehicule.trim()) errs.vehicule = 'Le véhicule est requis pour un livreur.';
      if (!formData.zone.trim()) errs.zone = 'La zone est requise pour un livreur.';
    }

    if (formData.role === UserRole.RESTAURANT_STAFF && !formData.restaurantName.trim()) {
      errs.restaurantName = 'Le nom du restaurant est requis pour un membre du personnel restaurant.';
    }

    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmitAsync = async () => {
    setError('');

    if (!validateForm()) {
      return;
    }

    setLoading(true);
    try {
      const authUser = await register({
        email: formData.email,
        password: formData.password,
        name: formData.name,
        role: formData.role,
        phoneNumber: formData.phoneNumber || undefined,
        address: formData.address || undefined,
        vehicule: formData.vehicule || undefined,
        zone: formData.zone || undefined,
        restaurantName: formData.restaurantName || undefined,
      });
      navigate(getRoleHomePath(authUser.roles), { replace: true });
    } catch (err) {
      const msg = getFriendlyErrorMessage(err, 'register');
      setError(msg);
      addError(msg, 'error', 6000);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h1>BiteRush</h1>
        <h2>S'inscrire</h2>

        {/* errors displayed as toasts */}

        <form onSubmit={(e) => { e.preventDefault(); void handleSubmitAsync(); }}>
          <div className="form-group">
            <label htmlFor="name">Nom</label>
            <input
              id="name"
              name="name"
              type="text"
              value={formData.name}
              onChange={handleChange}
              required
              disabled={loading}
            />
            {fieldErrors.name && <div className="field-error">{fieldErrors.name}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              value={formData.email}
              onChange={handleChange}
              required
              disabled={loading}
            />
            {fieldErrors.email && <div className="field-error">{fieldErrors.email}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="role">Vous inscrivez en tant que</label>
            <select
              id="role"
              name="role"
              value={formData.role}
              onChange={handleChange}
              disabled={loading}
            >
              <option value={UserRole.CLIENT}>Client</option>
              <option value={UserRole.RESTAURANT_STAFF}>Personnel restaurant</option>
              <option value={UserRole.LIVREUR}>Livreur</option>
              <option value={UserRole.ADMIN}>Administrateur</option>
            </select>
          </div>

          {formData.role === UserRole.LIVREUR && (
            <>
              <div className="form-group">
                <label htmlFor="phoneNumber">Numéro de téléphone</label>
                <input
                  id="phoneNumber"
                  name="phoneNumber"
                  type="tel"
                  value={formData.phoneNumber}
                  onChange={handleChange}
                  required={formData.role === UserRole.LIVREUR}
                  disabled={loading}
                />
                {fieldErrors.phoneNumber && <div className="field-error">{fieldErrors.phoneNumber}</div>}
              </div>
              <div className="form-group">
                <label htmlFor="vehicule">Véhicule</label>
                <input
                  id="vehicule"
                  name="vehicule"
                  type="text"
                  value={formData.vehicule}
                  onChange={handleChange}
                  required={formData.role === UserRole.LIVREUR}
                  disabled={loading}
                />
                {fieldErrors.vehicule && <div className="field-error">{fieldErrors.vehicule}</div>}
              </div>
              <div className="form-group">
                <label htmlFor="zone">Zone</label>
                <input
                  id="zone"
                  name="zone"
                  type="text"
                  value={formData.zone}
                  onChange={handleChange}
                  required={formData.role === UserRole.LIVREUR}
                  disabled={loading}
                />
                {fieldErrors.zone && <div className="field-error">{fieldErrors.zone}</div>}
              </div>
            </>
          )}

          {formData.role === UserRole.CLIENT && (
            <div className="form-group">
              <label htmlFor="address">Adresse</label>
              <input
                id="address"
                name="address"
                type="text"
                value={formData.address}
                onChange={handleChange}
                disabled={loading}
              />
            </div>
          )}

          {formData.role === UserRole.RESTAURANT_STAFF && (
            <div className="form-group">
              <label htmlFor="restaurantName">Nom du restaurant</label>
              <input
                id="restaurantName"
                name="restaurantName"
                type="text"
                value={formData.restaurantName}
                onChange={handleChange}
                required={formData.role === UserRole.RESTAURANT_STAFF}
                disabled={loading}
              />
            {fieldErrors.restaurantName && <div className="field-error">{fieldErrors.restaurantName}</div>}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="password">Mot de passe</label>
            <input
              id="password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              required
              disabled={loading}
            />
            {fieldErrors.password && <div className="field-error">{fieldErrors.password}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirmer le mot de passe</label>
            <input
              id="confirmPassword"
              name="confirmPassword"
              type="password"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
              disabled={loading}
            />
            {fieldErrors.confirmPassword && <div className="field-error">{fieldErrors.confirmPassword}</div>}
          </div>

          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Inscription en cours...' : 'S\'inscrire'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Vous avez déjà un compte?{' '}
            <Link to="/login">Se connecter</Link>
          </p>
        </div>
      </div>
    </div>
  );
};
