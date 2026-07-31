package com.biterush.api.service;

import com.biterush.api.dto.RestaurantDashboardDTO;
import com.biterush.api.dto.OrderResponseDTO;
import com.biterush.api.dto.DeliveryResponseDTO;
import com.biterush.api.entity.*;
import com.biterush.api.repository.OrderRepository;
import com.biterush.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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

    /*
     * =========================================================
     * DASHBOARD
     * =========================================================
     */

    public RestaurantDashboardDTO getDashboard() {

        List<Order> pending = orderRepository.findByStatusOrderByCreateAtDesc(OrderStatus.EN_ATTENTE);
        List<Order> preparing = orderRepository.findByStatusOrderByCreateAtDesc(OrderStatus.EN_PREPARATION);
        List<Order> ready = orderRepository.findByStatusOrderByCreateAtDesc(OrderStatus.PRETE);

        RestaurantDashboardDTO dto = new RestaurantDashboardDTO();
        dto.pendingCount = pending.size();
        dto.preparingCount = preparing.size();
        dto.readyCount = ready.size();

        OrderService orderSvc = orderService;
        dto.pending = pending.stream()
                .map(orderSvc::mapToResponsePublic)
                .toList();
        dto.preparing = preparing.stream()
                .map(orderSvc::mapToResponsePublic)
                .toList();
        dto.ready = ready.stream()
                .map(orderSvc::mapToResponsePublic)
                .toList();

        return dto;
    }

    /*
     * =========================================================
     * GET ORDERS BY STATUS
     * =========================================================
     */

    public List<OrderResponseDTO> getPendingOrders() {

        return orderRepository.findByStatusOrderByCreateAtDesc(OrderStatus.EN_ATTENTE)
                .stream()
                .map(o -> orderService.mapToResponsePublic(o))
                .toList();
    }

    public List<OrderResponseDTO> getPreparingOrders() {

        return orderRepository.findByStatusOrderByCreateAtDesc(OrderStatus.EN_PREPARATION)
                .stream()
                .map(o -> orderService.mapToResponsePublic(o))
                .toList();
    }

    public List<OrderResponseDTO> getReadyOrders() {

        return orderRepository.findByStatusOrderByCreateAtDesc(OrderStatus.PRETE)
                .stream()
                .map(o -> orderService.mapToResponsePublic(o))
                .toList();
    }

    /*
     * =========================================================
     * ACTIONS - ACCEPTER COMMANDE
     * =========================================================
     */

    public void acceptOrder(Long orderId) {

        validateRestaurantStaff();

        Order order = getOrder(orderId);

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

        validateRestaurantStaff();

        Order order = getOrder(orderId);

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

        validateRestaurantStaff();

        Order order = getOrder(orderId);

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

        validateRestaurantStaff();

        Order order = getOrder(orderId);

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
        validateRestaurantStaff();

        Order order = getOrder(orderId);

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

    private void validateRestaurantStaff() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Non authentifié"
            );
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
    }
}
