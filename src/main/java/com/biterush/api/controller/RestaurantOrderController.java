package com.biterush.api.controller;

import com.biterush.api.dto.RestaurantDashboardDTO;
import com.biterush.api.dto.OrderResponseDTO;
import com.biterush.api.dto.DeliveryResponseDTO;
import com.biterush.api.dto.DeliveryPersonProfileDTO;
import com.biterush.api.service.RestaurantService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurant/orders")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RestaurantOrderController {

    private final RestaurantService restaurantService;

    @GetMapping("/dashboard")
    public ResponseEntity<RestaurantDashboardDTO> getDashboard() {

        RestaurantDashboardDTO response = restaurantService.getDashboard();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<OrderResponseDTO>> getPendingOrders() {

        List<OrderResponseDTO> response = restaurantService.getPendingOrders();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/preparing")
    public ResponseEntity<List<OrderResponseDTO>> getPreparingOrders() {

        List<OrderResponseDTO> response = restaurantService.getPreparingOrders();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ready")
    public ResponseEntity<List<OrderResponseDTO>> getReadyOrders() {

        List<OrderResponseDTO> response = restaurantService.getReadyOrders();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<Void> acceptOrder(@PathVariable Long id) {

        restaurantService.acceptOrder(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/start-preparing")
    public ResponseEntity<Void> startPreparing(@PathVariable Long id) {

        restaurantService.startPreparing(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ready")
    public ResponseEntity<Void> markOrderReady(@PathVariable Long id) {

        restaurantService.markOrderReady(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/delivery-persons")
    public ResponseEntity<List<DeliveryPersonProfileDTO>> getDeliveryPersons(
            @RequestParam(defaultValue = "false") boolean availableOnly) {

        List<DeliveryPersonProfileDTO> response =
                restaurantService.getDeliveryPersons(availableOnly);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign-delivery")
    public ResponseEntity<DeliveryResponseDTO> assignDelivery(
            @PathVariable Long id,
            @RequestParam Long livreurId
    ) {

        DeliveryResponseDTO response = restaurantService.assignToDelivery(id, livreurId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        restaurantService.rejectOrder(id, reason);

        return ResponseEntity.noContent().build();
    }
}
