import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { NotificationProvider } from './contexts/NotificationContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import { AppProvider } from './contexts/AppContext';
import { CartProvider } from './contexts/CartContext';
import { ErrorBoundary } from './components/ErrorBoundary';
import { ToastContainer } from './components/ToastContainer';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { UserRole } from './types/enums';

// Pages - Client
import { OrderFormPage } from './pages/client/OrderFormPage';
import { MyOrdersPage } from './pages/client/MyOrdersPage';
import { OrderDetailPage } from './pages/client/OrderDetailPage';
import { ClientProfilePage } from './pages/client/ClientProfilePage';

// Pages - Restaurant
import { RestaurantDashboardPage } from './pages/restaurant/RestaurantDashboardPage';
import { RestaurantStaffProfilePage } from './pages/restaurant/RestaurantStaffProfilePage';
import { MenuCategoriesPage } from './pages/restaurant/MenuCategoriesPage';
import { MenuItemsPage } from './pages/restaurant/MenuItemsPage';

// Pages - Deliveries
import { MyDeliveriesPage } from './pages/delivery/MyDeliveriesPage';
import { DeliveryProfilePage } from './pages/delivery/DeliveryProfilePage';

// Pages - Notifications
import { NotificationsPage } from './pages/NotificationsPage';

// Pages - Admin
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminCharts } from './pages/admin/AdminCharts';
import { AdminUsersPage } from './pages/admin/AdminUsersPage';
import { AdminRestaurantsPage } from './pages/admin/AdminRestaurantsPage';

// Pages - Other
import { HomePage } from './pages/HomePage';
import { RestaurantPage } from './pages/RestaurantPage';
import { CartPage } from './pages/CartPage';
import { DeliveryDashboardPage } from './pages/DeliveryDashboardPage';
import { UnauthorizedPage } from './pages/UnauthorizedPage';

const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <Router>
        <AuthProvider>
          <WebSocketProvider>
            <NotificationProvider>
              <AppProvider>
                <CartProvider>
                <Routes>
                  {/* Public Routes */}
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register" element={<RegisterPage />} />
                  <Route path="/unauthorized" element={<UnauthorizedPage />} />

                  <Route
                    path="/"
                    element={
                      <Layout>
                        <HomePage />
                      </Layout>
                    }
                  />
                  <Route
                    path="/restaurant/:id"
                    element={
                      <Layout>
                        <RestaurantPage />
                      </Layout>
                    }
                  />
                  <Route
                    path="/cart"
                    element={
                      <Layout>
                        <CartPage />
                      </Layout>
                    }
                  />
                  <Route
                    path="/delivery-dashboard"
                    element={
                      <ProtectedRoute
                        element={
                          <Layout>
                            <DeliveryDashboardPage />
                          </Layout>
                        }
                        requiredRoles={[UserRole.LIVREUR, UserRole.ADMIN]}
                      />
                    }
                  />

            {/* Protected Routes - with Layout */}

            {/* Client Routes */}
            <Route
              path="/order-form"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <OrderFormPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.CLIENT]}
                />
              }
            />
            <Route
              path="/orders"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <MyOrdersPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.CLIENT]}
                />
              }
            />
            <Route
              path="/orders/:id"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <OrderDetailPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.CLIENT]}
                />
              }
            />

            <Route
              path="/profile"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <ClientProfilePage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.CLIENT]}
                />
              }
            />

            {/* Restaurant Routes */}
            <Route
              path="/restaurant/dashboard"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <RestaurantDashboardPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.RESTAURANT_STAFF, UserRole.ADMIN]}
                />
              }
            />
            <Route
              path="/restaurant/profile"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <RestaurantStaffProfilePage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.RESTAURANT_STAFF, UserRole.ADMIN]}
                />
              }
            />
            <Route
              path="/restaurant/menu/categories"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <MenuCategoriesPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.RESTAURANT_STAFF, UserRole.ADMIN]}
                />
              }
            />
            <Route
              path="/restaurant/menu/items"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <MenuItemsPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.RESTAURANT_STAFF, UserRole.ADMIN]}
                />
              }
            />

            {/* Delivery Routes */}
            <Route
              path="/deliveries"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <MyDeliveriesPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.LIVREUR, UserRole.ADMIN]}
                />
              }
            />
            <Route
              path="/profile/delivery"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <DeliveryProfilePage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.LIVREUR, UserRole.ADMIN]}
                />
              }
            />

            {/* Notifications */}
            <Route
              path="/notifications"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <NotificationsPage />
                    </Layout>
                  }
                />
              }
            />

            {/* Admin Routes */}
            <Route
              path="/admin/dashboard"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <AdminDashboardPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.ADMIN]}
                />
              }
            />
            <Route
              path="/admin/charts"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <AdminCharts />
                    </Layout>
                  }
                  requiredRoles={[UserRole.ADMIN]}
                />
              }
            />
            <Route
              path="/admin/users"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <AdminUsersPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.ADMIN]}
                />
              }
            />
            <Route
              path="/admin/restaurants"
              element={
                <ProtectedRoute
                  element={
                    <Layout>
                      <AdminRestaurantsPage />
                    </Layout>
                  }
                  requiredRoles={[UserRole.ADMIN]}
                />
              }
            />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
              <ToastContainer />
                </CartProvider>
            </AppProvider>
          </NotificationProvider>
        </WebSocketProvider>
      </AuthProvider>
    </Router>
  </ErrorBoundary>
  );
};

export default App;
