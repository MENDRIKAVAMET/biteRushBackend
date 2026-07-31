import React from 'react';
import { useApp } from '../contexts/AppContext';

export const ToastContainer: React.FC = () => {
  const { errors, removeError } = useApp();

  return (
    <div className="toast-container">
      {errors.map(error => (
        <div key={error.id} className={`toast toast-${error.type}`}>
          <div className="toast-content">
            <span>{error.message}</span>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              {error.action && (
                <button
                  className="btn btn-small"
                  onClick={() => {
                    // dispatch a custom event with id to allow AppContext to invoke callback
                    window.dispatchEvent(new CustomEvent('toast-action', { detail: { id: error.id } }));
                    removeError(error.id);
                  }}
                >
                  {error.action.label}
                </button>
              )}
              <button 
                onClick={() => removeError(error.id)}
                className="toast-close"
                aria-label="Fermer"
              >
                ✕
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};
