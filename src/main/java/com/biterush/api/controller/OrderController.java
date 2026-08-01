package com.biterush.api.controller;

import com.biterush.api.dto.OrderRequestDTO;
import com.biterush.api.dto.OrderResponseDTO;
import com.biterush.api.dto.OrderUpdateDTO;
import com.biterush.api.security.BusinessSecurityUtil;
import com.biterush.api.service.OrderService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
// Les ports 4200 (Angular)/3000 (CRA) ne correspondent à aucun frontend de ce
// projet (Vite, port 5173) et une annotation @CrossOrigin de contrôleur
// remplace la config CORS globale (CorsOrigin, qui autorise bien 5173) pour
// ce contrôleur précis — /orders/** était donc bloqué par CORS depuis le
// vrai frontend. Retrait de l'annotation pour retomber sur la config globale,
// comme le reste des contrôleurs.
public class OrderController {

    private final OrderService orderService;
    private final BusinessSecurityUtil businessSecurity;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody OrderRequestDTO dto
    ) {

        OrderResponseDTO response =
                orderService.createOrder(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @PathVariable Long id,
            @RequestParam(required = false) String token
    ) {

        OrderResponseDTO response =
                orderService.findByIdWithToken(id, token);
        
        businessSecurity.verifyOrderAccess(orderService.getOrderEntity(id));

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String token
    ) {

        businessSecurity.verifyOrderOwner(orderService.getOrderEntity(id));
        orderService.cancelOrder(id, token);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders() {

        List<OrderResponseDTO> response = orderService.findMyOrders();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {

        List<OrderResponseDTO> response =
                orderService.findAll();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/chart")
    public ResponseEntity<List<com.biterush.api.dto.OrderChartPointDTO>> getAdminOrdersChart() {

        List<com.biterush.api.dto.OrderChartPointDTO> response =
                orderService.getAdminOrdersChart();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<OrderResponseDTO> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateDTO dto
    ) {

        OrderResponseDTO response =
                orderService.updateOrder(id, dto);

        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id
    ) {

        orderService.deleteOrder(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/{id}/deliver")
    public ResponseEntity<Void> markAsDelivered(
            @PathVariable Long id
    ) {

        orderService.markAsDelivered(id);

        return ResponseEntity.noContent().build();
    }
}