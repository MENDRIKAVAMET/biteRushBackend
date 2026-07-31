import React, { useState, useEffect } from 'react';

import { apiClient } from '../../services/api';
import { useApp } from '../../contexts/AppContext';
import type { MenuItemDTO, CreateOrderRequest } from '../../types/api';
import './OrderForm.css';

export const OrderFormPage: React.FC = () => {
  const { addError } = useApp();
  const [menuItems, setMenuItems] = useState<MenuItemDTO[]>([]);
  const [selectedItems, setSelectedItems] = useState<Map<number, number>>(new Map());
  const [clientName, setClientName] = useState('');
  const [deliveryAddress, setDeliveryAddress] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{ clientName?: string; deliveryAddress?: string; phoneNumber?: string; items?: string }>({});

  useEffect(() => {
    void loadMenuItems();
  }, []);

  const loadMenuItems = async () => {
    try {
      const items = await apiClient.getMenuItems(1);
      setMenuItems(items);
    } catch (err) {
      if (err instanceof Error) {
        addError(err.message, 'error');
      } else {
        addError('Impossible de charger le menu', 'error');
      }
      console.error(err);
    }
  };

  const toggleItem = (itemId: number) => {
    setSelectedItems((prev) => {
      const newMap = new Map(prev);
      if (newMap.has(itemId)) {
        newMap.delete(itemId);
      } else {
        newMap.set(itemId, 1);
      }
      return newMap;
    });
  };

  const updateQuantity = (itemId: number, quantity: number) => {
    if (quantity <= 0) {
      setSelectedItems((prev) => {
        const newMap = new Map(prev);
        newMap.delete(itemId);
        return newMap;
      });
    } else {
      setSelectedItems((prev) => new Map(prev).set(itemId, quantity));
    }
  };

  const handleSubmit = (e: any) => {
    e.preventDefault();
    void handleSubmitAsync();
  };

  const handleSubmitAsync = async () => {
    setLoading(true);

    // Validation (field-level)
    const errs: { clientName?: string; deliveryAddress?: string; phoneNumber?: string; items?: string } = {};
    if (!clientName.trim()) errs.clientName = 'Veuillez entrer votre nom';
    if (!deliveryAddress.trim()) errs.deliveryAddress = 'Veuillez entrer une adresse de livraison';
    if (!phoneNumber.trim() || phoneNumber.length < 9) errs.phoneNumber = 'Veuillez entrer un numéro de téléphone valide';
    if (selectedItems.size === 0) errs.items = 'Sélectionnez au moins un article';

    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
      setLoading(false);
      return;
    }
    setFieldErrors({});

    try {
      const items = Array.from(selectedItems.entries()).map(([productId, quantity]) => ({
        productId,
        quantity,
      }));

      const request: CreateOrderRequest = {
        restaurantId: 1,
        clientName,
        phone: phoneNumber,
        address: deliveryAddress,
        items,
      };

      await apiClient.createOrder(request);
      setSuccess(true);
      addError('Commande créée avec succès!', 'success');
      setSelectedItems(new Map());
      setClientName('');
      setDeliveryAddress('');
      setPhoneNumber('');

      setTimeout(() => {
        window.location.href = '/orders';
      }, 1500);
    } catch (err) {
      addError(err instanceof Error ? err.message : 'Erreur lors de la création de la commande', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>Nouvelle commande</h1>

      {success && <div className="success-message">Commande créée avec succès!</div>}

      <form onSubmit={handleSubmit} className="order-form">
        <div className="form-section">
          <h2>Adresse et contact</h2>

          <div className="form-group">
            <label htmlFor="clientName">Nom du client</label>
            <input
              id="clientName"
              type="text"
              value={clientName}
              onChange={(e) => setClientName(e.target.value)}
              required
              disabled={loading}
              className={fieldErrors.clientName ? 'input-error' : ''}
            />
            {fieldErrors.clientName && <div className="field-error">{fieldErrors.clientName}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="address">Adresse de livraison</label>
            <input
              id="address"
              type="text"
              value={deliveryAddress}
              onChange={(e) => setDeliveryAddress(e.target.value)}
              required
              disabled={loading}
              className={fieldErrors.deliveryAddress ? 'input-error' : ''}
            />
            {fieldErrors.deliveryAddress && <div className="field-error">{fieldErrors.deliveryAddress}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="phone">Numéro de téléphone</label>
            <input
              id="phone"
              type="tel"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              required
              disabled={loading}
              className={fieldErrors.phoneNumber ? 'input-error' : ''}
            />
            {fieldErrors.phoneNumber && <div className="field-error">{fieldErrors.phoneNumber}</div>}
          </div>
        </div>

        <div className="form-section">
          <h2>Sélectionnez vos articles</h2>

          <div className="menu-grid">
            {menuItems.map((item) => (
              <div
                key={item.id}
                className={`menu-item-card ${selectedItems.has(item.id) ? 'selected' : ''}`}
              >
                <button
                  type="button"
                  className="menu-item-header"
                  onClick={() => toggleItem(item.id)}
                  aria-pressed={selectedItems.has(item.id)}
                >
                  <h3>{item.name}</h3>
                  <p className="price">{item.price.toFixed(2)} Ar</p>
                </button>

                {selectedItems.has(item.id) && (
                  <div className="quantity-control">
                    <button
                      type="button"
                      onClick={() =>
                        updateQuantity(item.id, (selectedItems.get(item.id) || 1) - 1)
                      }
                    >
                      −
                    </button>
                    <span>{selectedItems.get(item.id)}</span>
                    <button
                      type="button"
                      onClick={() =>
                        updateQuantity(item.id, (selectedItems.get(item.id) || 1) + 1)
                      }
                    >
                      +
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        <div className="form-section">
          <div className="order-summary">
            <h3>Récapitulatif</h3>
            <p>Articles sélectionnés: {selectedItems.size}</p>
            {fieldErrors.items && <div className="field-error">{fieldErrors.items}</div>}
          </div>

          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Création en cours...' : 'Confirmer la commande'}
          </button>
        </div>
      </form>
    </div>
  );
};
