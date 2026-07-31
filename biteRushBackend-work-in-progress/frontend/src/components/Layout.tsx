import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useNotifications } from '../hooks/useNotifications';
import { useCart } from '../contexts/CartContext';
import { UserRole } from '../types/enums';
import { Bell, Home, Menu, X, ShoppingBag, Truck, User, BarChart } from 'lucide-react';
import './Layout.css';

export const Header: React.FC = () => {
  const { user, logout, hasRole } = useAuth();
  const { unreadCount } = useNotifications();
  const { itemCount } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = React.useState(false);
  const [badgePop, setBadgePop] = React.useState(false);

  const prevCountRef = React.useRef<number>(itemCount);

  React.useEffect(() => {
    if (itemCount > prevCountRef.current) {
      setBadgePop(true);
      window.setTimeout(() => setBadgePop(false), 650);
    }
    prevCountRef.current = itemCount;
  }, [itemCount]);

  const isActive = (path: string) => location.pathname === path;
  let profilePath = '/';

  if (hasRole(UserRole.CLIENT)) {
    profilePath = '/profile';
  } else if (hasRole(UserRole.RESTAURANT_STAFF)) {
    profilePath = '/restaurant/profile';
  } else if (hasRole(UserRole.LIVREUR)) {
    profilePath = '/profile/delivery';
  }

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const roleLinks = React.useMemo(() => {
    const links: Array<{ to: string; label: string }> = [];

    if (hasRole(UserRole.CLIENT)) {
      links.push(
        { to: '/order-form', label: 'Nouvelle commande' },
        { to: '/orders', label: 'Mes commandes' },
        { to: '/cart', label: 'Panier' },
        { to: '/profile', label: 'Profil client' }
      );
    }

    if (hasRole(UserRole.RESTAURANT_STAFF) || hasRole(UserRole.ADMIN)) {
      links.push(
        { to: '/restaurant/dashboard', label: 'Dashboard resto' },
        { to: '/restaurant/profile', label: 'Profil resto' },
        { to: '/restaurant/menu/categories', label: 'Catégories' },
        { to: '/restaurant/menu/items', label: 'Articles' }
      );
    }

    if (hasRole(UserRole.LIVREUR) || hasRole(UserRole.ADMIN)) {
      links.push({ to: '/delivery-dashboard', label: 'Tableau livreur' });
      links.push({ to: '/deliveries', label: 'Livraisons' });
      if (hasRole(UserRole.LIVREUR) || hasRole(UserRole.ADMIN)) {
        links.push({ to: '/profile/delivery', label: 'Profil livreur' });
      }
    }

    if (hasRole(UserRole.ADMIN)) {
      links.push(
        { to: '/admin/dashboard', label: 'Admin dashboard' },
        { to: '/admin/users', label: 'Utilisateurs' },
        { to: '/admin/restaurants', label: 'Restaurants' },
        { to: '/admin/charts', label: 'Graphiques' }
      );
    }

    links.push({ to: '/notifications', label: 'Notifications' });
    return links;
  }, [hasRole, profilePath]);

  return (
    <header className="header">
      <div className="header-content">
        <div className="header-left">
          <Link to="/" className="logo">
            BiteRush
          </Link>
          <nav className="header-nav">
            <Link
              to="/"
              className={isActive('/') ? 'nav-button active' : 'nav-button'}
              title="Accueil"
              aria-label="Accueil"
            >
              <Home size={18} />
            </Link>
            {hasRole(UserRole.CLIENT) && (
              <>
                <Link
                  to="/orders"
                  className={isActive('/orders') ? 'nav-button active' : 'nav-button'}
                  title="Mes commandes"
                  aria-label="Mes commandes"
                >
                  <ShoppingBag size={18} />
                </Link>
                <Link
                  to="/cart"
                  className={isActive('/cart') ? 'nav-button active' : 'nav-button'}
                  title="Panier"
                  aria-label="Panier"
                >
                  <div style={{ position: 'relative', display: 'inline-flex', alignItems: 'center' }}>
                    <ShoppingBag size={18} />
                    {itemCount > 0 && (
                      <span className={`cart-badge ${badgePop ? 'pop' : ''}`} aria-hidden>
                        {itemCount}
                      </span>
                    )}
                  </div>
                </Link>
              </>
            )}
            {hasRole(UserRole.RESTAURANT_STAFF) && (
              <Link
                to="/restaurant/dashboard"
                className={isActive('/restaurant/dashboard') ? 'nav-button active' : 'nav-button'}
                title="Tableau de bord restaurant"
                aria-label="Tableau de bord restaurant"
              >
                <BarChart size={18} />
              </Link>
            )}
            {hasRole(UserRole.LIVREUR) && (
              <Link
                to="/delivery-dashboard"
                className={isActive('/delivery-dashboard') ? 'nav-button active' : 'nav-button'}
                title="Tableau de bord livreur"
                aria-label="Tableau de bord livreur"
              >
                <Truck size={18} />
              </Link>
            )}
            {hasRole(UserRole.ADMIN) && (
              <Link
                to="/admin/dashboard"
                className={isActive('/admin/dashboard') ? 'nav-button active' : 'nav-button'}
                title="Tableau de bord admin"
                aria-label="Tableau de bord admin"
              >
                <BarChart size={18} />
              </Link>
            )}
            <Link
              to={profilePath}
              className={profilePath === location.pathname ? 'nav-button active' : 'nav-button'}
              title="Mon profil"
              aria-label="Mon profil"
            >
              <User size={18} />
            </Link>
            <div className="header-links">
              {roleLinks.map((link) => (
                <Link
                  key={link.to}
                  to={link.to}
                  className={isActive(link.to) ? 'nav-link active' : 'nav-link'}
                >
                  {link.label}
                </Link>
              ))}
            </div>
          </nav>
        </div>

        <div className="header-right">
          <Link to="/notifications" className="notification-btn">
            <Bell size={20} />
            {unreadCount > 0 && (
              <span className="notification-badge">{unreadCount}</span>
            )}
          </Link>

          <div className="user-menu">
            <button className="menu-toggle" onClick={() => setMenuOpen(!menuOpen)}>
              {menuOpen ? <X size={20} /> : <Menu size={20} />}
            </button>

            {menuOpen && (
              <div className="dropdown-menu">
                <div className="user-info">
                  <p>{user?.name}</p>
                  <small>{user?.email}</small>
                </div>
                <div className="menu-divider"></div>
                {user?.roles.includes('ROLE_CLIENT') && (
                  <Link to="/profile" className="menu-item">
                    Mon profil
                  </Link>
                )}
                {user?.roles.includes('ROLE_RESTAURANT_STAFF') && (
                  <Link to="/restaurant/profile" className="menu-item">
                    Mon profil
                  </Link>
                )}
                {user?.roles.includes('ROLE_LIVREUR') && (
                  <Link to="/profile/delivery" className="menu-item">
                    Mon profil
                  </Link>
                )}
                <button onClick={handleLogout} className="menu-item logout-btn">
                  Déconnexion
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export const Sidebar: React.FC = () => {
  const { hasRole } = useAuth();
  const location = useLocation();
  const isActive = (path: string) => location.pathname === path;

  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        {hasRole(UserRole.CLIENT) && (
          <>
            <h3>Client</h3>
            <Link to="/orders" className={isActive('/orders') ? 'active' : ''}>
              Mes commandes
            </Link>
            <Link to="/order-form" className={isActive('/order-form') ? 'active' : ''}>
              Nouvelle commande
            </Link>
          </>
        )}

        {hasRole(UserRole.RESTAURANT_STAFF) && (
          <>
            <h3>Restaurant</h3>
            <Link to="/restaurant/dashboard" className={isActive('/restaurant/dashboard') ? 'active' : ''}>
              Dashboard
            </Link>
            <Link to="/restaurant/menu/categories" className={isActive('/restaurant/menu/categories') ? 'active' : ''}>
              Catégories Menu
            </Link>
            <Link to="/restaurant/menu/items" className={isActive('/restaurant/menu/items') ? 'active' : ''}>
              Articles Menu
            </Link>
          </>
        )}

        {hasRole(UserRole.LIVREUR) && (
          <>
            <h3>Livraison</h3>
            <Link to="/deliveries" className={isActive('/deliveries') ? 'active' : ''}>
              Mes livraisons
            </Link>
          </>
        )}

        {hasRole(UserRole.ADMIN) && (
          <>
            <h3>Admin</h3>
            <Link to="/admin/dashboard" className={isActive('/admin/dashboard') ? 'active' : ''}>
              Dashboard
            </Link>
            <Link to="/admin/users" className={isActive('/admin/users') ? 'active' : ''}>
              Utilisateurs
            </Link>
            <Link to="/admin/restaurants" className={isActive('/admin/restaurants') ? 'active' : ''}>
              Restaurants
            </Link>
            <Link to="/admin/charts" className={isActive('/admin/charts') ? 'active' : ''}>
              Graphiques
            </Link>
          </>
        )}
      </nav>
    </aside>
  );
};

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <div className="app-layout">
      <Header />
      <div className="main-content">
        <main className="page-content-full">
          {children}
        </main>
      </div>
    </div>
  );
};
