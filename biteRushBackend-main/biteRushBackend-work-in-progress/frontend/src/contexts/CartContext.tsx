import React, { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useApp } from './AppContext';
import type { MenuItemDTO } from '../types/api';

export interface CartItem extends MenuItemDTO {
  quantity: number;
  restaurantId?: number;
  restaurantName?: string;
}

interface CartContextType {
  items: CartItem[];
  addItem: (item: MenuItemDTO, restaurantId?: number, restaurantName?: string) => void;
  removeItem: (id: number) => void;
  updateQuantity: (id: number, quantity: number) => void;
  clearCart: () => void;
  itemCount: number;
  subtotal: number;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

interface CartProviderProps {
  children: ReactNode;
}

export const CartProvider: React.FC<CartProviderProps> = ({ children }) => {
  const [items, setItems] = useState<CartItem[]>(() => {
    if (typeof window === 'undefined') {
      return [];
    }

    try {
      const saved = localStorage.getItem('biteRushCart');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    localStorage.setItem('biteRushCart', JSON.stringify(items));
  }, [items]);

  const app = useApp();

  const addItem = (item: MenuItemDTO, restaurantId?: number, restaurantName?: string) => {
    setItems((current) => {
      const exists = current.some((entry) => entry.id === item.id);

      if (exists) {
        return current.map((entry) =>
          entry.id === item.id ? { ...entry, quantity: entry.quantity + 1 } : entry
        );
      }

      return [
        ...current,
        {
          ...item,
          quantity: 1,
          restaurantId,
          restaurantName,
        },
      ];
    });
    // optimistic feedback toast
    app?.addError(`Ajouté au panier : ${item.name}`, 'success', 3000);
  };

  const removeItem = (id: number) => {
    const found = items.find((entry) => entry.id === id) ?? null;
    if (!found) {
      app?.addError('Article retiré du panier', 'info', 2500);
      setItems((current) => current.filter((entry) => entry.id !== id));
      return;
    }

    setItems((current) => current.filter((entry) => entry.id !== id));

    app?.addError(
      `Article retiré : ${found.name}`,
      'info',
      6000,
      {
        label: 'Annuler',
        onClick: () => {
          setItems((cur) => [found, ...cur]);
        },
      }
    );
  };

  const updateQuantity = (id: number, quantity: number) => {
    setItems((current) => {
      if (quantity <= 0) {
        app?.addError('Article retiré du panier', 'info', 2500);
        return current.filter((entry) => entry.id !== id);
      }

      return current.map((entry) => (entry.id === id ? { ...entry, quantity } : entry));
    });
  };

  const clearCart = () => {
    const snapshot = items;
    if (snapshot.length === 0) return;

    setItems([]);
    app?.addError('Panier vidé', 'info', 8000, {
      label: 'Annuler',
      onClick: () => setItems(snapshot),
    });
  };

  const itemCount = useMemo(() => items.reduce((sum, entry) => sum + entry.quantity, 0), [items]);
  const subtotal = useMemo(
    () => items.reduce((sum, entry) => sum + entry.price * entry.quantity, 0),
    [items]
  );

  const value = useMemo(
    () => ({
      items,
      addItem,
      removeItem,
      updateQuantity,
      clearCart,
      itemCount,
      subtotal,
    }),
    [items, itemCount, subtotal]
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
};

export const useCart = () => {
  const context = useContext(CartContext);

  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }

  return context;
};
