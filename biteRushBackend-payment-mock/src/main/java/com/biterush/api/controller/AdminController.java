package com.biterush.api.controller;

import com.biterush.api.dto.UserRequestDTO;
import com.biterush.api.entity.Role;
import com.biterush.api.entity.User;
import com.biterush.api.entity.Restaurant;
import com.biterush.api.entity.Order;
import com.biterush.api.entity.OrderStatus;
import com.biterush.api.security.BusinessSecurityUtil;
import com.biterush.api.service.UserService;
import com.biterush.api.service.RestaurantService;
import com.biterush.api.service.OrderService;
import com.biterush.api.repository.UserRepository;
import com.biterush.api.repository.RestaurantRepository;
import com.biterush.api.repository.OrderRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Admin endpoints for managing users, restaurants, deliveries, and orders
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserService userService;
    private final RestaurantService restaurantService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private final BusinessSecurityUtil businessSecurity;

    // =====================================================
    // USER MANAGEMENT
    // =====================================================

    /**
     * GET all users (paginated)
     * Only accessible by ADMIN
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(Pageable pageable) {
        businessSecurity.requireAdmin();
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * GET user by ID
     * Only accessible by ADMIN
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        businessSecurity.requireAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilisateur non trouvé"
                ));
        return ResponseEntity.ok(user);
    }

    /**
     * POST create new user (admin can create any role)
     * Only accessible by ADMIN
     */
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequestDTO dto) {
        businessSecurity.requireAdmin();
        User user = userService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * PUT update user
     * Only accessible by ADMIN
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO dto
    ) {
        businessSecurity.requireAdmin();
        User user = userService.update(id, dto);
        return ResponseEntity.ok(user);
    }

    /**
     * DELETE user
     * Only accessible by ADMIN
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        businessSecurity.requireAdmin();
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET users by role
     * Only accessible by ADMIN
     */
    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role) {
        businessSecurity.requireAdmin();
        try {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            List<User> users = userRepository.findAll()
                    .stream()
                    .filter(u -> u.getRole() == roleEnum)
                    .toList();
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rôle invalide: " + role
            );
        }
    }

    // =====================================================
    // RESTAURANT MANAGEMENT
    // =====================================================

    /**
     * GET all restaurants
     * Only accessible by ADMIN
     */
    @GetMapping("/restaurants")
    public ResponseEntity<List<Restaurant>> getAllRestaurants() {
        businessSecurity.requireAdmin();
        List<Restaurant> restaurants = restaurantRepository.findAll();
        return ResponseEntity.ok(restaurants);
    }

    /**
     * GET restaurant by ID
     * Only accessible by ADMIN
     */
    @GetMapping("/restaurants/{id}")
    public ResponseEntity<Restaurant> getRestaurantById(@PathVariable Long id) {
        businessSecurity.requireAdmin();
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurant non trouvé"
                ));
        return ResponseEntity.ok(restaurant);
    }

    /**
     * PUT update restaurant
     * Only accessible by ADMIN
     */
    @PutMapping("/restaurants/{id}")
    public ResponseEntity<Restaurant> updateRestaurant(
            @PathVariable Long id,
            @RequestBody Restaurant dto
    ) {
        businessSecurity.requireAdmin();
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurant non trouvé"
                ));
        
        if (dto.getName() != null) {
            restaurant.setName(dto.getName());
        }
        if (dto.getAddress() != null) {
            restaurant.setAddress(dto.getAddress());
        }
        if (dto.getPhoneNumber() != null) {
            restaurant.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getEmail() != null) {
            restaurant.setEmail(dto.getEmail());
        }
        if (dto.getImageUrl() != null) {
            restaurant.setImageUrl(dto.getImageUrl());
        }
        
        Restaurant updated = restaurantRepository.save(restaurant);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE restaurant
     * Only accessible by ADMIN
     */
    @DeleteMapping("/restaurants/{id}")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        businessSecurity.requireAdmin();
        restaurantRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // =====================================================
    // DELIVERY PERSON MANAGEMENT
    // =====================================================

    /**
     * GET all delivery persons
     * Only accessible by ADMIN
     */
    @GetMapping("/deliveries/persons")
    public ResponseEntity<List<User>> getAllDeliveryPersons() {
        businessSecurity.requireAdmin();
        List<User> livreurs = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == Role.LIVREUR)
                .toList();
        return ResponseEntity.ok(livreurs);
    }

    /**
     * GET delivery person by ID
     * Only accessible by ADMIN
     */
    @GetMapping("/deliveries/persons/{id}")
    public ResponseEntity<User> getDeliveryPersonById(@PathVariable Long id) {
        businessSecurity.requireAdmin();
        User livreur = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Livreur non trouvé"
                ));
        
        if (livreur.getRole() != Role.LIVREUR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cet utilisateur n'est pas un livreur"
            );
        }
        
        return ResponseEntity.ok(livreur);
    }

    /**
     * PUT update delivery person
     * Only accessible by ADMIN
     */
    @PutMapping("/deliveries/persons/{id}")
    public ResponseEntity<User> updateDeliveryPerson(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO dto
    ) {
        businessSecurity.requireAdmin();
        User livreur = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Livreur non trouvé"
                ));
        
        if (livreur.getRole() != Role.LIVREUR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cet utilisateur n'est pas un livreur"
            );
        }
        
        User updated = userService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE delivery person
     * Only accessible by ADMIN
     */
    @DeleteMapping("/deliveries/persons/{id}")
    public ResponseEntity<Void> deleteDeliveryPerson(@PathVariable Long id) {
        businessSecurity.requireAdmin();
        User livreur = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Livreur non trouvé"
                ));
        
        if (livreur.getRole() != Role.LIVREUR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cet utilisateur n'est pas un livreur"
            );
        }
        
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // =====================================================
    // ORDER MANAGEMENT
    // =====================================================

    /**
     * GET all orders (paginated)
     * Only accessible by ADMIN
     */
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        businessSecurity.requireAdmin();
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }

    /**
     * GET order by ID
     * Only accessible by ADMIN
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        businessSecurity.requireAdmin();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Commande non trouvée"
                ));
        return ResponseEntity.ok(order);
    }

    /**
     * PUT update order status (admin override)
     * Only accessible by ADMIN
     */
    @PutMapping("/orders/{id}/force-status")
    public ResponseEntity<Void> forceOrderStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        businessSecurity.requireAdmin();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Commande non trouvée"
                ));
        
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(orderStatus);
            orderRepository.save(order);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Statut invalide: " + status
            );
        }
    }

    /**
     * DELETE order (hard delete)
     * Only accessible by ADMIN
     */
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        businessSecurity.requireAdmin();
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET orders by status
     * Only accessible by ADMIN
     */
    @GetMapping("/orders/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
        businessSecurity.requireAdmin();
        try {
            List<Order> orders = orderRepository.findAll()
                    .stream()
                    .filter(o -> o.getStatus().toString().equals(status.toUpperCase()))
                    .toList();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Statut invalide: " + status
            );
        }
    }

    /**
     * GET statistics dashboard
     * Only accessible by ADMIN
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getStats() {
        businessSecurity.requireAdmin();
        
        AdminStatsDTO stats = new AdminStatsDTO();
        stats.totalUsers = userRepository.count();
        stats.totalRestaurants = restaurantRepository.count();
        stats.totalOrders = orderRepository.count();
        stats.totalDeliveryPersons = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == Role.LIVREUR)
                .count();
        
        return ResponseEntity.ok(stats);
    }

    /**
     * DTO for admin statistics
     */
    public static class AdminStatsDTO {
        public long totalUsers;
        public long totalRestaurants;
        public long totalOrders;
        public long totalDeliveryPersons;
    }
}
