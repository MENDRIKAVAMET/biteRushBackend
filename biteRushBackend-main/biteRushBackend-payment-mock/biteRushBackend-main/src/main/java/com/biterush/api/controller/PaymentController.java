package com.biterush.api.controller;

import com.biterush.api.dto.PaymentRequestDTO;
import com.biterush.api.dto.PaymentResponseDTO;
import com.biterush.api.entity.PaymentStatus;
import com.biterush.api.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> create(@Valid @RequestBody PaymentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDTO> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.findByOrderId(orderId));
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<PaymentResponseDTO> markAsPaid(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.markAsPaid(id));
    }

    @PatchMapping("/{id}/refund")
    public ResponseEntity<PaymentResponseDTO> refund(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.refund(id));
    }

    /**
     * Simule le callback qu'enverrait une vraie passerelle (Stripe/PayPal/...).
     * Réservé à l'admin en attendant l'intégration d'un vrai PSP.
     */
    @PostMapping("/{id}/webhook")
    public ResponseEntity<PaymentResponseDTO> webhook(
            @PathVariable Long id,
            @RequestParam PaymentStatus status
    ) {
        return ResponseEntity.ok(paymentService.simulateWebhook(id, status));
    }
}
