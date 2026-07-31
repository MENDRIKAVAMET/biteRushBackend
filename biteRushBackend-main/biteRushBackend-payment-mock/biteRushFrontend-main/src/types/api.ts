import { UserRole, OrderStatus, DeliveryStatus, NotificationType } from './enums';

// Auth & User
export interface User {
  id: number;
  email: string;
  name: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  address?: string;
  vehicule?: string;
  zone?: string;
  roles: UserRole[];
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  id:number;
  username: string;
  name:string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  role?: string;
  phoneNumber?: string;
  address?: string;
  vehicule?: string;
  zone?: string;
  restaurantName?: string;
  user?: {
    name: string;
    email: string;
    password: string;
    role?: string;
    phoneNumber?: string;
    address?: string;
    vehicule?: string;
    zone?: string;
    restaurantName?: string;
  };
  username?: string;
  fullName?: string;
}

// Client
export interface ClientResponseDTO {
  id: number;
  user: User;
  phoneNumber?: string;
  address?: string;
  deliveryAddresses?: string[];
  createdAt: string;
}

// Restaurant Staff
export interface RestaurantStaffResponseDTO {
  id: number;
  user: User;
  restaurantId: number;
  restaurant?: RestaurantDTO;
}

// Restaurant
export interface RestaurantDTO {
  id: number;
  name: string;
  address: string;
  phoneNumber: string;
  email?: string;
  imageUrl?: string;
  rating?: number;
  deliveryTime?: number;
  active?: boolean;
  cuisine?: string;
}

// Menu Item
export interface MenuItemDTO {
  id: number;
  name: string;
  nom?: string;
  description?: string;
  price: number;
  prix?: number;
  stock?: number;
  available: boolean;
  restaurantId?: number;
  restaurant?: string;
  categoryId?: number;
  category?: string;
  imageUrl?: string;
}

// Menu Category
export interface MenuCategoryDTO {
  id: number;
  name: string;
  description?: string;
  restaurantId: number;
  createdAt?: string;
}

// Order Item
export interface OrderItemDTO {
  id: number;
  quantity: number;
  menuItem: MenuItemDTO;
  comment?: string;
}

// Order
export interface OrderDTO {
  id: number;
  clientId: number;
  restaurantId: number;
  status: OrderStatus;
  totalAmount: number;
  deliveryAddress: string;
  phoneNumber: string;
  items: OrderItemDTO[];
  delivery?: DeliveryDTO;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrderRequest {
  restaurantId?: number;
  clientName: string;
  phone?: string;
  phoneNumber?: string;
  address?: string;
  deliveryAddress?: string;
  deliveryFee?: number;
  items: {
    productId: number;
    quantity: number;
    comment?: string;
  }[];
}

// Delivery
export interface DeliveryDTO {
  id: number;
  orderId: number;
  deliveryPersonId: number;
  status: DeliveryStatus;
  deliveryPerson?: User;
  order?: OrderDTO;
  createdAt: string;
  updatedAt: string;
}

export interface AssignDeliveryRequest {
  deliveryPersonId: number;
  livreurId?: number;
}

// Addresses
export interface AddressDTO {
  id: number;
  street: string;
  city: string;
  zipCode: string;
  country: string;
  latitude?: number;
  longitude?: number;
  label?: string;
  default?: boolean;
  isDefault?: boolean;
}

export interface AddressCreateRequest {
  street: string;
  city: string;
  zipCode: string;
  country: string;
  latitude?: number;
  longitude?: number;
  label?: string;
  isDefault?: boolean;
  default?: boolean;
}

export interface AddressUpdateRequest extends AddressCreateRequest {}

// Reviews
export interface ReviewRequest {
  rating: number;
  comment: string;
}

export interface ReviewDTO {
  id: number;
  rating: number;
  comment: string;
  createdAt?: string;
}

// Notification
export interface NotificationDTO {
  id: number;
  type: NotificationType;
  message: string;
  targetEntityId: number;
  read: boolean;
  createdAt: string;
}

// Restaurant Dashboard
export interface RestaurantDashboardDTO {
  pendingCount: number;
  preparingCount: number;
  readyCount: number;
  pendingOrders: OrderDTO[];
  preparingOrders: OrderDTO[];
  readyOrders: OrderDTO[];
}

// API Response Wrapper
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  total: number;
}

// Unread Count Response
export interface UnreadCountResponse {
  count: number;
}

// Delivery person profile (GET/PUT /api/deliveries/profile)
export interface DeliveryPersonProfile {
  id: number;
  nom: string;
  email: string;
  zone: string;
  vehicule: string;
  available: boolean;
}

// Payment (mock) — module /payments
export type PaymentMethod = 'CARTE' | 'MOBILE_MONEY' | 'ESPECES';
export type PaymentStatus = 'EN_ATTENTE' | 'REUSSI' | 'ECHOUE' | 'REMBOURSE';

export interface PaymentDTO {
  id: number;
  orderId: number;
  amount: number;
  method: PaymentMethod;
  status: PaymentStatus;
  transactionRef: string;
  createdAt: string;
  updatedAt: string | null;
  mock: boolean;
}
