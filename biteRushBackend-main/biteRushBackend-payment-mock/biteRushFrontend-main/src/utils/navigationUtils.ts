import { UserRole } from '../types/enums';

export const getRoleHomePath = (roles?: string[]): string => {
  if (!roles || roles.length === 0) {
    return '/';
  }

  if (roles.includes(UserRole.ADMIN)) {
    return '/admin/dashboard';
  }

  if (roles.includes(UserRole.RESTAURANT_STAFF)) {
    return '/restaurant/dashboard';
  }

  if (roles.includes(UserRole.LIVREUR)) {
    return '/delivery-dashboard';
  }

  if (roles.includes(UserRole.CLIENT)) {
    return '/order-form';
  }

  return '/';
};
