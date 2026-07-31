package com.biterush.api.controller;

import com.biterush.api.dto.PaymentRequestDTO;
import com.biterush.api.dto.PaymentResponseDTO;
import com.biterush.api.entity.PaymentStatus;
import com.biterush.api.security.BusinessSecurityUtil;
import com.biterush.api.service.OrderService;
import com.biterush.api.service.PaymentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de paiement — MOCK, aucune vraie passerelle n'est appelée.
 * Voir PaymentService pour le détail de la simulation.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://localhost:3000"
        }
)
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final BusinessSecurityUtil businessSecurity;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> initiatePayment(
            @Valid @RequestBody PaymentRequestDTO dto
    ) {

        businessSecurity.verifyOrderAccess(orderService.getOrderEntity(dto.orderId));

        PaymentResponseDTO response = paymentService.initiatePayment(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentByOrder(
            @PathVariable Long orderId
    ) {
        businessSecurity.verifyOrderAccess(orderService.getOrderEntity(orderId));

        return ResponseEntity.ok(paymentService.findByOrderId(orderId));
    }

    /**
     * Marque un paiement en espèces comme encaissé (ex: à la livraison).
     */
    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<PaymentResponseDTO> markAsPaid(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(paymentService.markAsPaid(id));
    }

    @PatchMapping("/{id}/refund")
    public ResponseEntity<PaymentResponseDTO> refund(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(paymentService.refund(id));
    }

    /**
     * Simule un callback de passerelle de paiement (webhook).
     * Réservé à l'admin — utile uniquement pour les tests, en
     * attendant une vraie intégration PSP.
     */
    @PostMapping("/{id}/webhook")
    public ResponseEntity<PaymentResponseDTO> simulateWebhook(
            @PathVariable Long id,
            @RequestParam PaymentStatus status
    ) {
        return ResponseEntity.ok(paymentService.simulateWebhook(id, status));
    }
}
