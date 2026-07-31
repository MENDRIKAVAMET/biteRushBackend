import React, { useState, useRef } from 'react';
import type { MenuItemDTO } from '../types/api';
import './MenuItemCard.css';

interface Props {
  item: MenuItemDTO;
  onAdd: (item: MenuItemDTO) => void;
}

export const MenuItemCard: React.FC<Props> = ({ item, onAdd }) => {
  const [added, setAdded] = useState(false);

  const btnRef = useRef<HTMLButtonElement | null>(null);

  const handleAdd = () => {
    try {
      onAdd(item);
      setAdded(true);
      window.setTimeout(() => setAdded(false), 600);

      // Fly-to-cart animation (best-effort in DOM)
      if (typeof document !== 'undefined' && btnRef.current) {
        const imgUrl = item.imageUrl ?? '/placeholder-food.png';
        const fly = document.createElement('div');
        fly.className = 'fly-image';
        fly.style.backgroundImage = `url(${imgUrl})`;

        const start = btnRef.current.getBoundingClientRect();
        const targetEl = document.querySelector('.cart-badge') as HTMLElement | null;
        const end = targetEl ? targetEl.getBoundingClientRect() : (document.querySelector('.logo') as HTMLElement)?.getBoundingClientRect();

        fly.style.left = `${start.left + start.width / 2 - 18}px`;
        fly.style.top = `${start.top + start.height / 2 - 18}px`;

        document.body.appendChild(fly);

        // Force styles to apply then animate
        requestAnimationFrame(() => {
          if (end) {
            const dx = end.left + end.width / 2 - (start.left + start.width / 2);
            const dy = end.top + end.height / 2 - (start.top + start.height / 2);
            fly.style.transform = `translate(${dx}px, ${dy}px) scale(0.2)`;
            fly.style.opacity = '0.0';
          } else {
            fly.style.transform = `translateY(-40px) scale(0.6)`;
            fly.style.opacity = '0.0';
          }
        });

        fly.addEventListener('transitionend', () => fly.remove());
      }
    } catch {
      // ignore — optimistic UI only
    }
  };

  return (
    <div className="menu-item-card">
      <div className="menu-item-left">
        <div className="menu-item-image" style={{ backgroundImage: `url(${item.imageUrl ?? '/placeholder-food.png'})` }} />
      </div>

      <div className="menu-item-right">
        <div className="menu-item-title">{item.name}</div>
        <div className="menu-item-desc">{item.description}</div>
        <div className="menu-item-foot">
          <div className="menu-item-price">{item.price.toFixed(2)} €</div>
          <button ref={btnRef} className={`btn btn-primary ${added ? 'added' : ''}`} onClick={handleAdd}>Ajouter</button>
        </div>
      </div>
    </div>
  );
};

export default MenuItemCard;
