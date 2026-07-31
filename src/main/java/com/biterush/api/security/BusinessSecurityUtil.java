package com.biterush.api.security;

import com.biterush.api.entity.Order;
import com.biterush.api.entity.User;
import com.biterush.api.entity.Delivery;
import com.biterush.api.entity.Role;
import com.biterush.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Business security utility for role-based data filtering.
 * 
 * Rules:
 * - CLIENT can only access their own orders
 * - LIVREUR can only access their own deliveries
 * - ADMIN can access everything
 */
@Component
public class BusinessSecurityUtil {

    private final UserRepository userRepository;

    public BusinessSecurityUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the current authenticated user from security context.
     *
     * IMPORTANT : le principal posé par JwtFilter est l'email (String) du
     * token JWT, PAS l'entité User — un cast direct échoue systématiquement.
     * On résout donc l'utilisateur via le repository à partir de cet email.
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utilisateur non authentifié"
            );
        }

        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Utilisateur invalide"
                ));
    }

    /**
     * Verify that the current user can access a specific order
     * 
     * Rules:
     * - CLIENT: Can access only their own order (where user.id == order.creator.id)
     * - LIVREUR: Can access via delivery assignment
     * - ADMIN: Can access all
     * 
     * @param order The order to access
     * @throws ResponseStatusException if access is denied
     */
    public void verifyOrderAccess(Order order) {
        User currentUser = getCurrentUser();
        
        // Admin can access everything
        if (isAdmin(currentUser)) {
            return;
        }
        
        // Client can access only their own order
        if (isClient(currentUser)) {
            if (order.getCreator().getId().equals(currentUser.getId())) {
                return;
            }
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous n'avez pas accès à cette commande"
            );
        }
        
        // Delivery person can access orders assigned to them via delivery
        if (isDeliveryPerson(currentUser)) {
            if (order.getDelivery() != null && 
                order.getDelivery().getLivreur().getId().equals(currentUser.getId())) {
                return;
            }
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous n'avez pas accès à cette commande"
            );
        }
        
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Accès refusé"
        );
    }

    /**
     * Verify that the current user can access a specific delivery
     * 
     * Rules:
     * - LIVREUR: Can access only their own deliveries
     * - ADMIN: Can access all
     * 
     * @param delivery The delivery to access
     * @throws ResponseStatusException if access is denied
     */
    public void verifyDeliveryAccess(Delivery delivery) {
        User currentUser = getCurrentUser();
        
        // Admin can access everything
        if (isAdmin(currentUser)) {
            return;
        }
        
        // Delivery person can access only their own deliveries
        if (isDeliveryPerson(currentUser)) {
            if (delivery.getLivreur().getId().equals(currentUser.getId())) {
                return;
            }
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous n'avez accès qu'à vos propres livraisons"
            );
        }
        
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Accès refusé"
        );
    }

    /**
     * Verify that the current user is the owner of a specific order
     * Used for operations like cancellation
     * 
     * @param order The order to verify
     * @throws ResponseStatusException if user is not the owner
     */
    public void verifyOrderOwner(Order order) {
        User currentUser = getCurrentUser();
        
        if (!order.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous ne pouvez que gérer vos propres commandes"
            );
        }
    }

    /**
     * Check if user is an admin
     */
    public boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    /**
     * Check if user is a client
     */
    public boolean isClient(User user) {
        return user.getRole() == Role.CLIENT;
    }

    /**
     * Check if user is a delivery person
     */
    public boolean isDeliveryPerson(User user) {
        return user.getRole() == Role.LIVREUR;
    }

    /**
     * Check if current user has a specific role
     */
    public boolean hasRole(Role role) {
        User currentUser = getCurrentUser();
        return currentUser.getRole() == role;
    }

    /**
     * Enforce that the current user is an admin
     * 
     * @throws ResponseStatusException if user is not an admin
     */
    public void requireAdmin() {
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Accès administrateur requis"
            );
        }
    }

    /**
     * Enforce that the current user is a delivery person
     * 
     * @throws ResponseStatusException if user is not a delivery person
     */
    public void requireDeliveryPerson() {
        User currentUser = getCurrentUser();
        if (!isDeliveryPerson(currentUser)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Accès livreur requis"
            );
        }
    }
}
