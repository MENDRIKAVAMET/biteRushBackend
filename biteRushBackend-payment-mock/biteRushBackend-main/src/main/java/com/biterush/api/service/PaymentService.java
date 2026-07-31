package com.biterush.api.service;

import com.biterush.api.dto.PaymentRequestDTO;
import com.biterush.api.dto.PaymentResponseDTO;
import com.biterush.api.entity.Order;
import com.biterush.api.entity.Payment;
import com.biterush.api.entity.PaymentMethod;
import com.biterush.api.entity.PaymentStatus;
import com.biterush.api.repository.OrderRepository;
import com.biterush.api.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service de paiement 100% MOCK.
 * Aucune passerelle réelle (Stripe/PayPal/Mobile Money) n'est appelée ici.
 * Pour brancher un vrai PSP plus tard : remplacer uniquement processMockPayment()
 * par un vrai appel API, tout le reste (entité, DTO, sécurité, liaison à la commande)
 * reste valable tel quel.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private static final double SUCCESS_RATE = 0.9;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public PaymentResponseDTO createPayment(PaymentRequestDTO dto) {

        Order order = orderRepository.findById(dto.orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Commande introuvable"));

        if (order.getPayment() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Un paiement existe déjà pour cette commande");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        payment.setMethod(dto.method);
        payment.setTransactionRef(generateMockTransactionRef());

        if (dto.method == PaymentMethod.ESPECES) {
            // Paiement à la livraison : reste en attente jusqu'à /mark-paid
            payment.setStatus(PaymentStatus.EN_ATTENTE);
        } else {
            processMockPayment(payment);
        }

        Payment saved = paymentRepository.save(payment);
        order.setPayment(saved);
        orderRepository.save(order);

        if (saved.getStatus() == PaymentStatus.REUSSI) {
            orderService.confirmOrderAfterPayment(order.getId());
        }

        return mapToResponse(saved);
    }

    /**
     * Simule l'appel à une passerelle de paiement (carte / mobile money).
     * 90% de réussite par défaut, comme un vrai PSP en environnement de test.
     */
    private void processMockPayment(Payment payment) {
        boolean success = new SecureRandom().nextDouble() < SUCCESS_RATE;
        payment.setStatus(success ? PaymentStatus.REUSSI : PaymentStatus.ECHOUE);
        payment.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO findById(Long id) {
        return mapToResponse(getPayment(id));
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO findByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aucun paiement pour cette commande"));
        return mapToResponse(payment);
    }

    /**
     * Marque un paiement ESPECES comme payé (encaissé par le livreur/restaurant).
     */
    public PaymentResponseDTO markAsPaid(Long id) {

        Payment payment = getPayment(id);

        if (payment.getMethod() != PaymentMethod.ESPECES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seuls les paiements en espèces peuvent être marqués manuellement");
        }

        payment.setStatus(PaymentStatus.REUSSI);
        payment.setUpdatedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        orderService.confirmOrderAfterPayment(payment.getOrder().getId());

        return mapToResponse(saved);
    }

    /**
     * Endpoint admin simulant un callback webhook envoyé par un vrai PSP.
     */
    public PaymentResponseDTO simulateWebhook(Long id, PaymentStatus newStatus) {

        Payment payment = getPayment(id);
        payment.setStatus(newStatus);
        payment.setUpdatedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        if (newStatus == PaymentStatus.REUSSI) {
            orderService.confirmOrderAfterPayment(payment.getOrder().getId());
        }

        return mapToResponse(saved);
    }

    public PaymentResponseDTO refund(Long id) {

        Payment payment = getPayment(id);

        if (payment.getStatus() != PaymentStatus.REUSSI) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seul un paiement réussi peut être remboursé");
        }

        payment.setStatus(PaymentStatus.REMBOURSE);
        payment.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(paymentRepository.save(payment));
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Paiement introuvable"));
    }

    private String generateMockTransactionRef() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return "MOCK-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private PaymentResponseDTO mapToResponse(Payment payment) {
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.id = payment.getId();
        dto.orderId = payment.getOrder().getId();
        dto.amount = payment.getAmount();
        dto.method = payment.getMethod();
        dto.status = payment.getStatus();
        dto.transactionRef = payment.getTransactionRef();
        dto.createdAt = payment.getCreatedAt();
        dto.updatedAt = payment.getUpdatedAt();
        dto.mock = true;
        return dto;
    }
}
