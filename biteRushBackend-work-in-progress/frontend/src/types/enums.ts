// User Roles
export const UserRole = {
  CLIENT: 'ROLE_CLIENT',
  RESTAURANT_STAFF: 'ROLE_RESTAURANT_STAFF',
  LIVREUR: 'ROLE_LIVREUR',
  ADMIN: 'ROLE_ADMIN',
} as const;

export type UserRole = (typeof UserRole)[keyof typeof UserRole];

// Order Status
export const OrderStatus = {
  EN_ATTENTE: 'EN_ATTENTE',
  CONFIRMEE: 'CONFIRMEE',
  EN_PREPARATION: 'EN_PREPARATION',
  PRETE: 'PRETE',
  EN_LIVRAISON: 'EN_LIVRAISON',
  LIVREE: 'LIVREE',
  ANNULEE: 'ANNULEE',
} as const;

export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

// Delivery Status
export const DeliveryStatus = {
  ASSIGNED: 'ASSIGNED',
  IN_PROGRESS: 'IN_PROGRESS',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED',
} as const;

export type DeliveryStatus = (typeof DeliveryStatus)[keyof typeof DeliveryStatus];

// Notification Type
export const NotificationType = {
  ORDER_CREATED: 'ORDER_CREATED',
  ORDER_ACCEPTED: 'ORDER_ACCEPTED',
  ORDER_PREPARING: 'ORDER_PREPARING',
  ORDER_READY: 'ORDER_READY',
  ORDER_ON_DELIVERY: 'ORDER_ON_DELIVERY',
  ORDER_DELIVERED: 'ORDER_DELIVERED',
  ORDER_CANCELLED: 'ORDER_CANCELLED',
  DELIVERY_ASSIGNED: 'DELIVERY_ASSIGNED',
  DELIVERY_IN_PROGRESS: 'DELIVERY_IN_PROGRESS',
  DELIVERY_COMPLETED: 'DELIVERY_COMPLETED',
} as const;

export type NotificationType = (typeof NotificationType)[keyof typeof NotificationType];

export const OrderStatusLabels: Record<OrderStatus, string> = {
  EN_ATTENTE: 'En attente',
  CONFIRMEE: 'Confirmée',
  EN_PREPARATION: 'En préparation',
  PRETE: 'Prête',
  EN_LIVRAISON: 'En livraison',
  LIVREE: 'Livrée',
  ANNULEE: 'Annulée',
};

export const DeliveryStatusLabels: Record<DeliveryStatus, string> = {
  ASSIGNED: 'Assignée',
  IN_PROGRESS: 'En cours',
  DELIVERED: 'Livrée',
  CANCELLED: 'Annulée',
};
