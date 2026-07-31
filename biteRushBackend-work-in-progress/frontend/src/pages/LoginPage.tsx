import React, { useState } from 'react';
import { useApp } from '../contexts/AppContext';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { getFriendlyErrorMessage } from '../utils/errorUtils';
import { getRoleHomePath } from '../utils/navigationUtils';
import './Auth.css';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});
  const [loading, setLoading] = useState(false);
  const { login, user, token } = useAuth();
  const { addError } = useApp();
  React.useEffect(() => {
    if (error) {
      addError(error, 'error', 5000);
      setError('');
    }
  }, [error, addError]);
  const navigate = useNavigate();

  if (token && user) {
    return <Navigate to={getRoleHomePath(user.roles)} replace />;
  }

  const handleSubmitAsync = async () => {
    setError('');
    setFieldErrors({});
    // Client-side validation
    const errs: { email?: string; password?: string } = {};
    if (!email.trim()) errs.email = 'L’email est requis.';
    if (!password) errs.password = 'Le mot de passe est requis.';
    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
      return;
    }
    setLoading(true);

    try {
      const authUser = await login(email, password);
      navigate(getRoleHomePath(authUser.roles), { replace: true });
    } catch (err) {
      const msg = getFriendlyErrorMessage(err, 'login');
      setError(msg);
      addError(msg, 'error', 5000);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h1>BiteRush</h1>
        <h2>Connexion</h2>

        {/* errors displayed as toasts */}

        <form onSubmit={(e) => { e.preventDefault(); void handleSubmitAsync(); }}>
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={loading}
            />
            {fieldErrors.email && <div className="field-error">{fieldErrors.email}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="password">Mot de passe</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
            />
            {fieldErrors.password && <div className="field-error">{fieldErrors.password}</div>}
          </div>

          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Connexion en cours...' : 'Se connecter'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Pas encore de compte?{' '}
            <Link to="/register">S'inscrire</Link>
          </p>
        </div>
      </div>
    </div>
  );
};
