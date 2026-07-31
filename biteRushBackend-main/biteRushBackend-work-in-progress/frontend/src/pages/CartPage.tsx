import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useCart } from '../contexts/CartContext';
import { useAuth } from '../hooks/useAuth';
import { apiClient } from '../services/api';
import { getFriendlyErrorMessage } from '../utils/errorUtils';
import { useApp } from '../contexts/AppContext';
import type { AddressDTO, CreateOrderRequest, OrderDTO } from '../types/api';
import './CartPage.css';

export const CartPage: React.FC = () => {
  const { items, removeItem, updateQuantity, clearCart, subtotal } = useCart();
  const { user } = useAuth();

  const [addresses, setAddresses] = useState<AddressDTO[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  const [loadingAddresses, setLoadingAddresses] = useState(false);
  const [creatingAddress, setCreatingAddress] = useState(false);

  const [newAddress, setNewAddress] = useState({ street: '', city: '', zipCode: '', country: 'France' });
  const { addError } = useApp();
  const [phoneOverride, setPhoneOverride] = useState<string | undefined>(user?.phoneNumber ?? '');
  const deliveryFee = Math.max(2.5, Math.round((subtotal * 0.05) * 100) / 100);
  const total = subtotal + (items.length ? deliveryFee : 0);
  const [placingOrder, setPlacingOrder] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [phoneError, setPhoneError] = useState<string | null>(null);
  const [addressError, setAddressError] = useState<string | null>(null);
  const [successOrder, setSuccessOrder] = useState<OrderDTO | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoadingAddresses(true);
      try {
        const data = await apiClient.getAddresses();
        setAddresses(data || []);
        if (data && data.length > 0) setSelectedAddressId(data[0].id);
      } catch (err) {
        console.error('Failed to load addresses', err);
      } finally {
        setLoadingAddresses(false);
      }
    };

    load();
  }, []);

  useEffect(() => {
    if (error) {
      addError(error, 'error', 5000);
      setError(null);
    }
  }, [error, addError]);

  // total item count (unused variable removed)

  const handleCreateAddress = async () => {
    setError(null);
    if (!newAddress.street.trim() || !newAddress.city.trim() || !newAddress.zipCode.trim()) {
      setError('Veuillez remplir rue, ville et code postal.');
      return;
    }

    setCreatingAddress(true);
    try {
      const created = await apiClient.createAddress({
        street: newAddress.street,
        city: newAddress.city,
        zipCode: newAddress.zipCode,
        country: newAddress.country,
      });
      setAddresses((cur) => [created, ...cur]);
      setSelectedAddressId(created.id);
      setNewAddress({ street: '', city: '', zipCode: '', country: 'France' });
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'generic'));
    } finally {
      setCreatingAddress(false);
    }
  };

  const handlePlaceOrder = async () => {
    setError(null);

    if (items.length === 0) {
      setError('Votre panier est vide.');
      return;
    }

    const phoneValue = phoneOverride?.trim() ?? '';
    setPhoneError(null);
    if (!phoneValue) {
      setPhoneError('Veuillez indiquer un numéro de téléphone pour la livraison.');
      return;
    }

    const validPhone = /^[0-9+()\s-]{7,25}$/.test(phoneValue);
    if (!validPhone) {
      setPhoneError('Veuillez entrer un numéro de téléphone valide.');
      return;
    }

    // Ensure single restaurant in cart
    const restaurantIds = Array.from(new Set(items.map((i) => i.restaurantId).filter(Boolean)));
    if (restaurantIds.length > 1) {
      setError('Le panier contient des articles de plusieurs restaurants. Séparez vos commandes par établissement.');
      return;
    }

    const addr = addresses.find((a) => a.id === selectedAddressId) ?? null;
    setAddressError(null);
    if (!addr) {
      setAddressError('Veuillez sélectionner ou créer une adresse de livraison.');
      return;
    }

    setPlacingOrder(true);
    try {
      const payload: CreateOrderRequest = {
        restaurantId: restaurantIds[0] ?? items[0].restaurantId,
        clientName: user?.name ?? '',
        phoneNumber: phoneOverride ?? user?.phoneNumber ?? '',
        deliveryAddress: `${addr.street}, ${addr.city} ${addr.zipCode}`,
        items: items.map((it) => ({ productId: it.id, quantity: it.quantity })),
        deliveryFee: Math.round(deliveryFee * 100) / 100,
      };

      const order = await apiClient.createOrder(payload);
      setSuccessOrder(order);
      clearCart();
      addError('Commande passée avec succès', 'success', 4000);
    } catch (err) {
      const msg = getFriendlyErrorMessage(err, 'generic');
      setError(msg);
      addError(msg, 'error', 5000);
    } finally {
      setPlacingOrder(false);
    }
  };

  if (successOrder) {
    return (
      <div className="page">
        <div className="order-success">
          <h1>Commande confirmée 🎉</h1>
          <p>Merci — votre commande <strong>#{successOrder.id}</strong> a bien été reçue.</p>
          <p>Montant: <strong>{successOrder.totalAmount?.toFixed(2) ?? total.toFixed(2)} €</strong></p>
          <p>Adresse de livraison: <strong>{successOrder.deliveryAddress}</strong></p>
          <p>Téléphone: <strong>{successOrder.phoneNumber}</strong></p>
          <div className="order-items-summary">
            <h2>Détails de la commande</h2>
            <ul>
              {successOrder.items?.map((item) => (
                <li key={item.id}>
                  {item.quantity} × {item.menuItem?.name ?? item.menuItem?.nom ?? 'Article'} – {item.menuItem?.price?.toFixed(2) ?? '0.00'} €
                </li>
              ))}
            </ul>
          </div>
          <Link to="/">Retour à l'accueil</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="page cart-page">
      <div className="cart-header">
        <h1>Panier</h1>
        <span className="cart-total-badge">Total estimé : {total.toFixed(2)} €</span>
      </div>

      {/* errors are shown via toasts */}

      {items.length === 0 ? (
        <div>
          <p>Votre panier est vide.</p>
          <Link to="/">Voir les restaurants</Link>
        </div>
      ) : (
        <div className="cart-grid">
          <section className="cart-items">
            {items.map((item) => (
              <div key={item.id} className="cart-item">
                <div className="cart-item-main">
                  <div className="cart-item-title">{item.name}</div>
                  <div className="cart-item-restaurant">{item.restaurantName}</div>
                </div>
                <div className="cart-item-actions">
                  <button onClick={() => updateQuantity(item.id, item.quantity - 1)}>-</button>
                  <span>{item.quantity}</span>
                  <button onClick={() => updateQuantity(item.id, item.quantity + 1)}>+</button>
                  <div className="cart-item-price">{(item.price * item.quantity).toFixed(2)} €</div>
                  <button className="btn btn-link" onClick={() => removeItem(item.id)}>Retirer</button>
                </div>
              </div>
            ))}

            <div className="cart-totals">
              <div>Sous-total</div>
              <div className="bold">{subtotal.toFixed(2)} €</div>
            </div>
          </section>

          <aside className="cart-checkout">
            <h2>Adresse de livraison</h2>
            {loadingAddresses ? (
              <p>Chargement des adresses…</p>
            ) : (
              <>
              <div className="addresses">
                {addresses.length === 0 && <p>Aucune adresse trouvée. Ajoutez-en une ci-dessous.</p>}
                {addresses.map((a) => (
                  <label key={a.id} className={`address-item ${selectedAddressId === a.id ? 'selected' : ''}`}>
                    <input
                      type="radio"
                      name="address"
                      aria-label={`Adresse ${a.street}, ${a.city} ${a.zipCode}`}
                      checked={selectedAddressId === a.id}
                      onChange={() => setSelectedAddressId(a.id)}
                    />
                    <div>
                      <div className="address-street">{a.street}</div>
                      <div className="address-meta">{a.city} · {a.zipCode}</div>
                    </div>
                  </label>
                ))}
              </div>
                {addressError && <div className="field-error" style={{ marginTop: 8 }}>{addressError}</div>}
              </>
            )}

            <div className="new-address">
              <h3>Nouvelle adresse</h3>
              <input placeholder="Rue" value={newAddress.street} onChange={(e) => setNewAddress(s => ({ ...s, street: e.target.value }))} />
              <input placeholder="Ville" value={newAddress.city} onChange={(e) => setNewAddress(s => ({ ...s, city: e.target.value }))} />
              <input placeholder="Code postal" value={newAddress.zipCode} onChange={(e) => setNewAddress(s => ({ ...s, zipCode: e.target.value }))} />
              <button className="btn btn-primary full-width" disabled={creatingAddress} onClick={handleCreateAddress}>
                {creatingAddress ? 'Ajout...' : 'Ajouter l\'adresse'}
              </button>
            </div>

            <div className="checkout-section">
              <h3>Contact</h3>
              <div className="checkout-field">
                <span className="checkout-label">Nom</span>
                <div>{user?.name ?? '—'}</div>
              </div>
              <div className="checkout-field">
                <label className="checkout-label" htmlFor="phoneOverride">Téléphone</label>
                <input
                  id="phoneOverride"
                  className="checkout-input"
                  value={phoneOverride ?? ''}
                  onChange={(e) => setPhoneOverride(e.target.value)}
                  placeholder="Téléphone"
                />
                {phoneError && <div className="field-error">{phoneError}</div>}
              </div>
            </div>

            <div className="checkout-summary">
              <div>
                <div className="summary-row"><span>Sous-total</span><strong>{subtotal.toFixed(2)} €</strong></div>
                <div className="summary-row"><span>Frais de livraison</span><strong>{deliveryFee.toFixed(2)} €</strong></div>
                <div className="summary-row total-row"><span>Total estimé</span><strong>{total.toFixed(2)} €</strong></div>
              </div>
              <button className="btn btn-primary full-width" disabled={placingOrder} onClick={handlePlaceOrder}>
                {placingOrder ? 'En cours…' : `Valider la commande (${total.toFixed(2)} €)`}
              </button>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
};
