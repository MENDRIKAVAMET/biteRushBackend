package com.biterush.api.service;

import com.biterush.api.dto.RestaurantDashboardDTO;
import com.biterush.api.dto.OrderResponseDTO;
import com.biterush.api.dto.DeliveryResponseDTO;
import com.biterush.api.entity.*;
import com.biterush.api.repository.OrderRepository;
import com.biterush.api.repository.RestaurantStaffRepository;
import com.biterush.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final DeliveryService deliveryService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;

    /*
     * =========================================================
     * LISTE DES LIVREURS (pour la modale d'assignation)
     * =========================================================
     * Délègue à DeliveryService : liste des livreurs (DeliveryPersonProfileDTO),
     * triée par zone puis nom, sans scoping restaurant — un restaurant n'a pas
     * besoin de limiter les livreurs qu'il peut assigner. Le filtre optionnel
     * availableOnly (défaut false) ne garde que les livreurs disponibles.
     */
    public java.util.List<com.biterush.api.dto.DeliveryPersonProfileDTO> getDeliveryPersons(
            boolean availableOnly) {
        return deliveryService.getAllDeliveryPersons(availableOnly);
    }

    /*
     * =========================================================
     * DASHBOARD (scoped au restaurant du staff connecté ; ADMIN voit tout)
     * =========================================================
     */

    public RestaurantDashboardDTO getDashboard() {

        Long restaurantId = getCurrentStaffRestaurantId();

        List<Order> pending = findByStatus(OrderStatus.EN_ATTENTE, restaurantId);
        List<Order> preparing = findByStatus(OrderStatus.EN_PREPARATION, restaurantId);
        List<Order> ready = findByStatus(OrderStatus.PRETE, restaurantId);

        RestaurantDashboardDTO dto = new RestaurantDashboardDTO();
        dto.pendingCount = pending.size();
        dto.preparingCount = preparing.size();
        dto.readyCount = ready.size();

        dto.pending = pending.stream().map(orderService::mapToResponsePublic).toList();
        dto.preparing = preparing.stream().map(orderService::mapToResponsePublic).toList();
        dto.ready = ready.stream().map(orderService::mapToResponsePublic).toList();

        return dto;
    }

    /*
     * =========================================================
     * GET ORDERS BY STATUS (scoped au restaurant du staff connecté)
     * =========================================================
     */

    public List<OrderResponseDTO> getPendingOrders() {
        return findByStatus(OrderStatus.EN_ATTENTE, getCurrentStaffRestaurantId())
                .stream()
                .map(orderService::mapToResponsePublic)
                .toList();
    }

    public List<OrderResponseDTO> getPreparingOrders() {
        return findByStatus(OrderStatus.EN_PREPARATION, getCurrentStaffRestaurantId())
                .stream()
                .map(orderService::mapToResponsePublic)
                .toList();
    }

    public List<OrderResponseDTO> getReadyOrders() {
        return findByStatus(OrderStatus.PRETE, getCurrentStaffRestaurantId())
                .stream()
                .map(orderService::mapToResponsePublic)
                .toList();
    }

    private List<Order> findByStatus(OrderStatus status, Long restaurantId) {
        // restaurantId == null signifie ADMIN : pas de filtre, vue globale
        if (restaurantId == null) {
            return orderRepository.findByStatusOrderByCreateAtDesc(status);
        }
        return orderRepository.findByStatusAndRestaurant_IdOrderByCreateAtDesc(status, restaurantId);
    }

    /*
     * =========================================================
     * ACTIONS - ACCEPTER COMMANDE
     * =========================================================
     */

    public void acceptOrder(Long orderId) {

        Long staffRestaurantId = getCurrentStaffRestaurantId();

        Order order = getOrder(orderId);
        verifyOrderBelongsToStaffRestaurant(order, staffRestaurantId);

        if (order.getStatus() != OrderStatus.EN_ATTENTE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La commande doit être en attente"
            );
        }

        order.setStatus(OrderStatus.CONFIRMEE);
        orderRepository.save(order);

        notificationService.notifyOrderStatusChanged(order, OrderStatus.CONFIRMEE);
    }

    /*
     * =========================================================
     * ACTIONS - COMMENCER PRÉPARATION
     * =========================================================
     */

    public void startPreparing(Long orderId) {

        Long staffRestaurantId = getCurrentStaffRestaurantId();

        Order order = getOrder(orderId);
        verifyOrderBelongsToStaffRestaurant(order, staffRestaurantId);

        if (order.getStatus() != OrderStatus.CONFIRMEE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La commande doit être confirmée"
            );
        }

        order.setStatus(OrderStatus.EN_PREPARATION);
        orderRepository.save(order);

        notificationService.notifyOrderStatusChanged(order, OrderStatus.EN_PREPARATION);
    }

    /*
     * =========================================================
     * ACTIONS - MARQUER PRÊTE
     * =========================================================
     */

    public void markOrderReady(Long orderId) {

        Long staffRestaurantId = getCurrentStaffRestaurantId();

        Order order = getOrder(orderId);
        verifyOrderBelongsToStaffRestaurant(order, staffRestaurantId);

        if (order.getStatus() != OrderStatus.EN_PREPARATION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La commande doit être en préparation"
            );
        }

        order.setStatus(OrderStatus.PRETE);
        orderRepository.save(order);

        notificationService.notifyOrderStatusChanged(order, OrderStatus.PRETE);
    }

    /*
     * =========================================================
     * ACTIONS - ASSIGNER À UN LIVREUR
     * =========================================================
     */

    public DeliveryResponseDTO assignToDelivery(Long orderId, Long livreurId) {

        Long staffRestaurantId = getCurrentStaffRestaurantId();

        Order order = getOrder(orderId);
        verifyOrderBelongsToStaffRestaurant(order, staffRestaurantId);

        if (order.getStatus() != OrderStatus.PRETE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La commande doit être prête"
            );
        }

        User livreur = userRepository.findById(livreurId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Livreur introuvable"
                ));

        if (livreur.getRole() != Role.LIVREUR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'utilisateur n'est pas un livreur"
            );
        }

        return deliveryService.assignOrderToDeliveryPublic(orderId, livreur);
    }

    public void rejectOrder(Long orderId, String reason) {

        Long staffRestaurantId = getCurrentStaffRestaurantId();

        Order order = getOrder(orderId);
        verifyOrderBelongsToStaffRestaurant(order, staffRestaurantId);

        if (order.getStatus() != OrderStatus.EN_ATTENTE &&
            order.getStatus() != OrderStatus.CONFIRMEE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La commande ne peut être rejetée qu'en attente ou confirmée"
            );
        }

        order.setStatus(OrderStatus.REJETEE);
        orderRepository.save(order);

        notificationService.notifyOrderStatusChanged(order, OrderStatus.REJETEE);
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private Order getOrder(Long id) {

        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Commande introuvable"
                ));
    }

    /**
     * Résout le restaurant du staff actuellement connecté.
     *
     * @return l'ID du restaurant du staff, ou {@code null} si l'appelant est ADMIN
     *         (l'ADMIN n'est pas scoped à un restaurant précis).
     * @throws ResponseStatusException si l'appelant n'est ni ADMIN ni RESTAURANT_STAFF,
     *         ou si un RESTAURANT_STAFF authentifié n'a pas (ou plus) de profil staff.
     */
    private Long getCurrentStaffRestaurantId() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Non authentifié"
            );
        }

        boolean isAdmin = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return null;
        }

        boolean isStaff = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_STAFF"));

        if (!isStaff) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Accès refusé - Staff restaurant requis"
            );
        }

        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Utilisateur invalide"
                ));

        // RestaurantStaff partage sa clé primaire avec User (@MapsId)
        RestaurantStaff staff = restaurantStaffRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Aucun profil staff restaurant associé à ce compte"
                ));

        return staff.getRestaurantId();
    }

    /**
     * Empêche un staff du restaurant A d'agir sur les commandes du restaurant B.
     * Sans cette vérification, n'importe quel RESTAURANT_STAFF pouvait accepter,
     * préparer, rejeter ou assigner un livreur sur la commande de n'importe quel
     * autre restaurant en devinant simplement l'ID de la commande.
     */
    private void verifyOrderBelongsToStaffRestaurant(Order order, Long staffRestaurantId) {

        if (staffRestaurantId == null) {
            // ADMIN : pas de restriction
            return;
        }

        if (order.getRestaurant() == null
                || !order.getRestaurant().getId().equals(staffRestaurantId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cette commande n'appartient pas à votre restaurant"
            );
        }
    }
}
