import axios, { AxiosError } from 'axios';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  OrderDTO,
  CreateOrderRequest,
  DeliveryDTO,
  NotificationDTO,
  RestaurantDashboardDTO,
  ClientResponseDTO,
  RestaurantStaffResponseDTO,
  MenuItemDTO,
  UnreadCountResponse,
  AssignDeliveryRequest,
  User,
  RestaurantDTO,
  AddressDTO,
  AddressCreateRequest,
  AddressUpdateRequest,
  ReviewRequest,
  ReviewDTO,
} from '../types/api';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

class ApiClient {
  private readonly client: ReturnType<typeof axios.create>;
  private token: string | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: API_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Load token from localStorage
    const storedToken = localStorage.getItem('authToken');
    if (storedToken) {
      this.token = storedToken;
      this.setAuthHeader(storedToken);
    }

    // Request interceptor
    this.client.interceptors.request.use((config) => {
      if (this.token) {
        config.headers.Authorization = `Bearer ${this.token}`;
      }
      return config;
    });

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.response?.status === 401) {
          // Token expired or invalid
          this.clearAuth();
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  private setAuthHeader(token: string) {
    this.client.defaults.headers.common.Authorization = `Bearer ${token}`;
  }

  setToken(token: string) {
    this.token = token;
    this.setAuthHeader(token);
    localStorage.setItem('authToken', token);
  }

  clearAuth() {
    this.token = null;
    delete this.client.defaults.headers.common.Authorization;
    localStorage.removeItem('authToken');
    localStorage.removeItem('authUser');
  }

  // Setup auto-logout on 401
  onUnauthorized(callback: () => void) {
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.response?.status === 401) {
          this.clearAuth();
          callback();
        }
        return Promise.reject(error);
      }
    );
  }

  // ============ Auth Endpoints ============
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await this.client.post('/auth/login', credentials);
    return response.data;
  }

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await this.client.post('/auth/register', data, {
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
    });
    return response.data;
  }

  async refreshToken(): Promise<AuthResponse | null> {
    try {
      const response = await this.client.post('/auth/refresh');
      return response.data;
    } catch (err) {
      console.error('Token refresh failed:', err);
      return null;
    }
  }

  async logout(): Promise<void> {
    await this.client.post('/auth/logout');
    this.clearAuth();
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.client.get('/auth/me');
    return response.data;
  }

  // ============ User Endpoints ============
  async getUsers(): Promise<User[]> {
    const response = await this.client.get('/users');
    return response.data;
  }

  async createUser(data: { name: string; email: string; password: string }): Promise<User> {
    const response = await this.client.post('/users', data);
    return response.data;
  }

  async getUserById(id: number): Promise<User> {
    const response = await this.client.get(`/users/${id}`);
    return response.data;
  }

  async updateUser(id: number, data: { name?: string; email?: string; password?: string }): Promise<User> {
    const response = await this.client.put(`/users/${id}`, data);
    return response.data;
  }

  async deleteUser(id: number): Promise<void> {
    await this.client.delete(`/users/${id}`);
  }

  // ============ Restaurant Endpoints ============
  async getRestaurants(): Promise<RestaurantDTO[]> {
    const response = await this.client.get('/restaurants');
    return response.data;
  }

  async getRestaurantById(id: number): Promise<RestaurantDTO> {
    const response = await this.client.get(`/restaurants/${id}`);
    return response.data;
  }

  async createRestaurant(data: RestaurantDTO): Promise<RestaurantDTO> {
    const response = await this.client.post('/restaurants', data);
    return response.data;
  }

  async updateRestaurant(id: number, data: Partial<RestaurantDTO>): Promise<RestaurantDTO> {
    const response = await this.client.put(`/restaurants/${id}`, data);
    return response.data;
  }

  async deleteRestaurant(id: number): Promise<void> {
    await this.client.delete(`/restaurants/${id}`);
  }

  async searchRestaurants(query: string): Promise<RestaurantDTO[]> {
    const response = await this.client.get(`/restaurants/search?query=${encodeURIComponent(query)}`);
    return response.data;
  }

  // ============ Notification Endpoints ============
  async getNotifications(): Promise<NotificationDTO[]> {
    const response = await this.client.get('/notifications');
    return response.data;
  }

  async getUnreadNotifications(): Promise<NotificationDTO[]> {
    const response = await this.client.get('/notifications/unread');
    return response.data;
  }

  async getUnreadCount(): Promise<number> {
    const response = await this.client.get<UnreadCountResponse>(
      '/notifications/unread/count'
    );
    return response.data.count;
  }

  async markNotificationAsRead(id: number): Promise<NotificationDTO> {
    const response = await this.client.patch(`/notifications/${id}/read`);
    return response.data;
  }

  async markAllNotificationsAsRead(): Promise<number> {
    const response = await this.client.patch('/notifications/read-all');
    return response.data;
  }

  async deleteNotification(id: number): Promise<void> {
    await this.client.delete(`/notifications/${id}`);
  }

  // ============ Order Endpoints ============
  async createOrder(data: CreateOrderRequest): Promise<OrderDTO> {
    const response = await this.client.post('/orders', data);
    return response.data;
  }

  async getMyOrders(): Promise<OrderDTO[]> {
    const response = await this.client.get('/orders/my-orders');
    return response.data;
  }

  async getAllOrders(): Promise<OrderDTO[]> {
    const response = await this.client.get('/admin/orders');
    return response.data;
  }

  async getOrderById(id: number): Promise<OrderDTO> {
    const response = await this.client.get(`/orders/${id}`);
    return response.data;
  }

  async cancelOrder(id: number): Promise<OrderDTO> {
    const response = await this.client.patch(`/orders/${id}/cancel`);
    return response.data;
  }

  // ============ Restaurant Order Endpoints ============
  async getRestaurantDashboard(): Promise<RestaurantDashboardDTO> {
    const response = await this.client.get('/restaurant/orders/dashboard');
    return response.data;
  }

  async getPendingOrders(): Promise<OrderDTO[]> {
    const response = await this.client.get('/restaurant/orders/pending');
    return response.data;
  }

  async getPreparingOrders(): Promise<OrderDTO[]> {
    const response = await this.client.get('/restaurant/orders/preparing');
    return response.data;
  }

  async getReadyOrders(): Promise<OrderDTO[]> {
    const response = await this.client.get('/restaurant/orders/ready');
    return response.data;
  }

  async acceptOrder(id: number): Promise<OrderDTO> {
    const response = await this.client.patch(`/restaurant/orders/${id}/accept`);
    return response.data;
  }

  async startOrderPreparation(id: number): Promise<OrderDTO> {
    const response = await this.client.patch(
      `/restaurant/orders/${id}/start-preparing`
    );
    return response.data;
  }

  async markOrderReady(id: number): Promise<OrderDTO> {
    const response = await this.client.patch(`/restaurant/orders/${id}/ready`);
    return response.data;
  }

  async assignDelivery(
    orderId: number,
    data: AssignDeliveryRequest
  ): Promise<OrderDTO> {
    const response = await this.client.post(
      `/restaurant/orders/${orderId}/assign-delivery`,
      data
    );
    return response.data;
  }

  // ============ Address Endpoints ============
  async getAddresses(): Promise<AddressDTO[]> {
    const response = await this.client.get('/addresses');
    return response.data;
  }

  async getAddressById(id: number): Promise<AddressDTO> {
    const response = await this.client.get(`/addresses/${id}`);
    return response.data;
  }

  async createAddress(data: AddressCreateRequest): Promise<AddressDTO> {
    const response = await this.client.post('/addresses', data);
    return response.data;
  }

  async updateAddress(id: number, data: AddressUpdateRequest): Promise<AddressDTO> {
    const response = await this.client.put(`/addresses/${id}`, data);
    return response.data;
  }

  async deleteAddress(id: number): Promise<void> {
    await this.client.delete(`/addresses/${id}`);
  }

  // ============ Delivery Endpoints ============
  // Préfixe unifié /api/deliveries (aligné sur DeliveryController côté backend)
  async getMyDeliveries(): Promise<DeliveryDTO[]> {
    const response = await this.client.get('/api/deliveries/me');
    return response.data;
  }

  async getDeliveryById(id: number): Promise<DeliveryDTO> {
    const response = await this.client.get(`/api/deliveries/${id}`);
    return response.data;
  }

  async acceptDelivery(id: number): Promise<DeliveryDTO> {
    const response = await this.client.patch(`/api/deliveries/${id}/accept`);
    return response.data;
  }

  async completeDelivery(id: number): Promise<DeliveryDTO> {
    const response = await this.client.patch(`/api/deliveries/${id}/deliver`);
    return response.data;
  }

  async cancelDelivery(id: number): Promise<DeliveryDTO> {
    const response = await this.client.patch(`/api/deliveries/${id}/cancel`);
    return response.data;
  }

  async assignDeliveryPerson(orderId: number, livreurId: number): Promise<DeliveryDTO> {
    const response = await this.client.post(`/api/deliveries/assign`, { orderId, livreurId });
    return response.data;
  }

  async startDelivery(id: number): Promise<DeliveryDTO> {
    const response = await this.client.patch(`/api/deliveries/${id}/start`);
    return response.data;
  }

  async deliverDelivery(id: number): Promise<DeliveryDTO> {
    const response = await this.client.patch(`/api/deliveries/${id}/deliver`);
    return response.data;
  }

  async getDeliveryProfile(): Promise<DeliveryPersonProfile> {
    const response = await this.client.get('/api/deliveries/profile');
    return response.data;
  }

  async updateDeliveryProfile(data: Partial<DeliveryPersonProfile>): Promise<DeliveryPersonProfile> {
    const response = await this.client.put('/api/deliveries/profile', data);
    return response.data;
  }

  async setDeliveryAvailability(available: boolean): Promise<DeliveryPersonProfile> {
    const response = await this.client.patch('/api/deliveries/availability', { available });
    return response.data;
  }

  // ============ Payment Endpoints ============
  async createPayment(orderId: number, method: 'CARTE' | 'MOBILE_MONEY' | 'ESPECES'): Promise<PaymentDTO> {
    const response = await this.client.post('/payments', { orderId, method });
    return response.data;
  }

  async getPaymentByOrder(orderId: number): Promise<PaymentDTO> {
    const response = await this.client.get(`/payments/order/${orderId}`);
    return response.data;
  }

  // getMyOrders() et changePassword() sont déjà définis plus bas dans ce fichier

  // ============ Review Endpoints ============
  async createReview(orderId: number, data: ReviewRequest): Promise<ReviewDTO> {
    const response = await this.client.post(`/reviews/orders/${orderId}`, data);
    return response.data;
  }

  async getReviewsByRestaurant(restaurantId: number): Promise<ReviewDTO[]> {
    const response = await this.client.get(`/reviews/restaurants/${restaurantId}`);
    return response.data;
  }

  async getAverageReview(restaurantId: number): Promise<{ average: number }> {
    const response = await this.client.get(`/reviews/restaurants/${restaurantId}/average`);
    return response.data;
  }

  // ============ Client Profile Endpoints ============
  async getClientProfile(): Promise<ClientResponseDTO> {
    const response = await this.client.get('/clients/profile');
    return response.data;
  }

  async updateClientProfile(data: {
    name?: string;
    phoneNumber?: string;
    address?: string;
    deliveryAddresses?: string[];
  }): Promise<ClientResponseDTO> {
    const response = await this.client.put('/clients/profile', data);
    return response.data;
  }

  // ============ Restaurant Staff Endpoints ============
  async getRestaurantStaffProfile(): Promise<RestaurantStaffResponseDTO> {
    const response = await this.client.get('/restaurant-staff/profile');
    return response.data;
  }

  async changePassword(data: {
    currentPassword: string;
    newPassword: string;
  }): Promise<{ message: string }> {
    const response = await this.client.post('/auth/change-password', data);
    return response.data;
  }

  async getDeliveryProfile(): Promise<{
    phoneNumber?: string;
    vehicule?: string;
    zone?: string;
    available?: boolean;
  }> {
    const response = await this.client.get('/deliveries/profile');
    return response.data;
  }

  async updateDeliveryProfile(data: {
    phoneNumber?: string;
    vehicule?: string;
    zone?: string;
    available?: boolean;
  }): Promise<{ message: string }> {
    const response = await this.client.put('/deliveries/profile', data);
    return response.data;
  }

  async toggleAvailability(available: boolean): Promise<{ message: string }> {
    const response = await this.client.patch('/deliveries/availability', { available });
    return response.data;
  }

  // ============ Menu Endpoints ============
  async getMenuItems(restaurantId: number): Promise<MenuItemDTO[]> {
    const response = await this.client.get(
      `/restaurants/${restaurantId}/menu-items`
    );
    return response.data;
  }

  async getMenuCategories(restaurantId: number): Promise<any[]> {
    const response = await this.client.get(
      `/restaurants/${restaurantId}/menu-categories`
    );
    return response.data;
  }

  async createMenuCategory(restaurantId: number, data: {
    name: string;
    description?: string;
  }): Promise<any> {
    const response = await this.client.post(
      `/restaurants/${restaurantId}/menu-categories`,
      data
    );
    return response.data;
  }

  async updateMenuCategory(restaurantId: number, categoryId: number, data: {
    name: string;
    description?: string;
  }): Promise<any> {
    const response = await this.client.put(
      `/restaurants/${restaurantId}/menu-categories/${categoryId}`,
      data
    );
    return response.data;
  }

  async deleteMenuCategory(restaurantId: number, categoryId: number): Promise<void> {
    await this.client.delete(
      `/restaurants/${restaurantId}/menu-categories/${categoryId}`
    );
  }

  async createMenuItem(restaurantId: number, data: {
    name: string;
    price: number;
    description?: string;
    categoryId?: number;
    category?: string;
    stock?: number;
    imageUrl?: string;
  }): Promise<MenuItemDTO> {
    const response = await this.client.post(
      `/restaurants/${restaurantId}/menu-items`,
      data
    );
    return response.data;
  }

  async updateMenuItem(restaurantId: number, itemId: number, data: {
    name: string;
    price: number;
    description?: string;
    categoryId?: number;
    category?: string;
    stock?: number;
    imageUrl?: string;
    available?: boolean;
  }): Promise<MenuItemDTO> {
    const response = await this.client.put(
      `/restaurants/${restaurantId}/menu-items/${itemId}`,
      data
    );
    return response.data;
  }

  async deleteMenuItem(restaurantId: number, itemId: number): Promise<void> {
    await this.client.delete(
      `/restaurants/${restaurantId}/menu-items/${itemId}`
    );
  }

  async toggleMenuItemAvailability(restaurantId: number, itemId: number, available: boolean): Promise<MenuItemDTO> {
    const response = await this.client.patch(
      `/restaurants/${restaurantId}/menu-items/${itemId}/availability`,
      { available }
    );
    return response.data;
  }

  // Admin endpoints
  async getAdminStatistics(): Promise<{
    totalClients: number;
    totalRestaurants: number;
    totalDeliveryPersons: number;
    totalOrders: number;
    totalRevenue: number;
  }> {
    const response = await this.client.get('/admin/statistics');
    return response.data;
  }

  async getAdminOrdersChart(): Promise<{
    date: string;
    count: number;
    revenue: number;
  }[]> {
    const response = await this.client.get('/admin/orders/chart');
    return response.data;
  }

  async getAllUsers(): Promise<User[]> {
    const response = await this.client.get('/admin/users');
    return response.data;
  }

  async getAllRestaurants(): Promise<RestaurantDTO[]> {
    const response = await this.client.get('/admin/restaurants');
    return response.data;
  }

  async getRestaurantDetails(restaurantId: number): Promise<{
    restaurant: RestaurantDTO;
    orderCount: number;
    totalRevenue: number;
    avgOrderValue: number;
  }> {
    const response = await this.client.get(`/admin/restaurants/${restaurantId}`);
    return response.data;
  }
}

export const apiClient = new ApiClient();
