package com.biterush.api.controller;

import com.biterush.api.entity.Delivery;
import com.biterush.api.entity.Order;
import com.biterush.api.entity.RestaurantStaff;
import com.biterush.api.entity.User;
import com.biterush.api.repository.DeliveryRepository;
import com.biterush.api.repository.OrderRepository;
import com.biterush.api.repository.RestaurantStaffRepository;
import com.biterush.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;

    @GetMapping("/deliveries")
    @PreAuthorize("hasRole('LIVREUR')")
    public ResponseEntity<List<Delivery>> getDeliveryHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        List<Delivery> deliveries = deliveryRepository.findByLivreurEmail(userEmail);
        return ResponseEntity.ok(deliveries);
    }

    /**
     * Corrigé (session du 01/08/2026, suite 4) : renvoyait auparavant l'historique de
     * TOUTES les commandes, tous restaurants confondus, à n'importe quel
     * RESTAURANT_STAFF authentifié — aucune vérification d'appartenance. Scopé
     * maintenant à SON restaurant, même patron que
     * RestaurantService.getCurrentStaffRestaurantId() /
     * RestaurantStaffService.getCurrentStaffRestaurantId() (dupliqué localement pour
     * rester cohérent avec le style déjà en place dans le projet, pas de classe
     * utilitaire partagée existante pour ça).
     */
    @GetMapping("/restaurant/orders")
    @PreAuthorize("hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<List<Order>> getRestaurantOrderHistory() {

        Long staffRestaurantId = getCurrentStaffRestaurantId();

        List<Order> orders = orderRepository.findByRestaurant_IdOrderByCreateAtDesc(staffRestaurantId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAllOrdersHistory() {
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }

    /**
     * Résout le restaurant du staff actuellement connecté. Contrairement à
     * RestaurantService.getCurrentStaffRestaurantId(), cet endpoint est déjà
     * restreint à RESTAURANT_STAFF via @PreAuthorize (pas d'accès ADMIN ici), donc
     * pas de cas "retourne null pour ADMIN" à gérer.
     */
    private Long getCurrentStaffRestaurantId() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

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
}

