package com.biterush.api.controller;

import com.biterush.api.entity.Delivery;
import com.biterush.api.entity.Order;
import com.biterush.api.repository.DeliveryRepository;
import com.biterush.api.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    @GetMapping("/deliveries")
    @PreAuthorize("hasRole('LIVREUR')")
    public ResponseEntity<List<Delivery>> getDeliveryHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        List<Delivery> deliveries = deliveryRepository.findByLivreurEmail(userEmail);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/restaurant/orders")
    @PreAuthorize("hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<List<Order>> getRestaurantOrderHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAllOrdersHistory() {
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }
}
