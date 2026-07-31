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
import java.util.UUID;

/**
 * =============================================================
 * SERVICE DE PAIEMENT — MOCK
 * =============================================================
 * Il n'y a ICI AUCUNE intégration avec une vraie passerelle de
 * paiement (Stripe, PayPal, Mobile Money, etc). Tout est simulé :
 *
 * - la "référence de transaction" est générée localement
 * - le résultat (réussi/échoué) est tiré aléatoirement (ou toujours
 *   réussi selon la méthode) pour permettre de tester le flux de
 *   bout en bout côté frontend sans dépendance externe
 * - un endpoint /payments/{id}/webhook permet de simuler manuellement
 *   un callback de passerelle (utile pour les tests)
 *
 * À remplacer par un vrai PSP avant la mise en production.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    // Taux de réussite simulé pour CARTE / MOBILE_MONEY (90%)
    private static final double MOCK_SUCCESS_RATE = 0.9;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final SecureRandom random = new SecureRandom();

    /*
     * =========================================================
     * INITIER UN PAIEMENT (mock)
     * =========================================================
     */
    public PaymentResponseDTO initiatePayment(PaymentRequestDTO dto) {

        Order order = orderRepository.findById(dto.orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Commande introuvable"
                ));

        if (order.getPayment() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un paiement existe déjà pour cette commande"
            );
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        payment.setMethod(dto.method);
        payment.setTransactionRef(generateMockTransactionRef());
        payment.setStatus(PaymentStatus.EN_ATTENTE);

        /*
         * ESPECES (paiement à la livraison) : pas de "traitement" à
         * simuler, le paiement reste EN_ATTENTE jusqu'à la livraison.
         * CARTE / MOBILE_MONEY : on simule un traitement immédiat.
         */
        if (dto.method == PaymentMethod.ESPECES) {
            payment = paymentRepository.save(payment);
            return mapToResponse(payment);
        }

        payment = paymentRepository.save(payment);
        payment = processMockPayment(payment);

        return mapToResponse(payment);
    }

    /*
     * =========================================================
     * SIMULER UN CALLBACK DE PASSERELLE (webhook mock)
     * =========================================================
     * Permet de forcer manuellement le statut d'un paiement pendant
     * les tests, comme le ferait un vrai webhook Stripe/PayPal.
     */
    public PaymentResponseDTO simulateWebhook(Long paymentId, PaymentStatus forcedStatus) {

        Payment payment = getPaymentById(paymentId);

        applyStatus(payment, forcedStatus);

        payment = paymentRepository.save(payment);

        return mapToResponse(payment);
    }

    /*
     * =========================================================
     * MARQUER COMME PAYÉ (ex: espèces à la livraison)
     * =========================================================
     */
    public PaymentResponseDTO markAsPaid(Long paymentId) {

        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() == PaymentStatus.REUSSI) {
            return mapToResponse(payment);
        }

        applyStatus(payment, PaymentStatus.REUSSI);

        payment = paymentRepository.save(payment);

        return mapToResponse(payment);
    }

    /*
     * =========================================================
     * REMBOURSEMENT (mock)
     * =========================================================
     */
    public PaymentResponseDTO refund(Long paymentId) {

        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatus.REUSSI) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seul un paiement réussi peut être remboursé"
            );
        }

        payment.setStatus(PaymentStatus.REMBOURSE);

        payment = paymentRepository.save(payment);

        return mapToResponse(payment);
    }

    /*
     * =========================================================
     * LECTURE
     * =========================================================
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO findById(Long id) {
        return mapToResponse(getPaymentById(id));
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO findByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun paiement trouvé pour cette commande"
                ));
        return mapToResponse(payment);
    }

    /*
     * =========================================================
     * PRIVATE HELPERS
     * =========================================================
     */

    private Payment processMockPayment(Payment payment) {

        boolean success = random.nextDouble() < MOCK_SUCCESS_RATE;

        applyStatus(payment, success ? PaymentStatus.REUSSI : PaymentStatus.ECHOUE);

        return paymentRepository.save(payment);
    }

    private void applyStatus(Payment payment, PaymentStatus status) {

        payment.setStatus(status);

        if (status == PaymentStatus.REUSSI) {
            payment.setPaidAt(LocalDateTime.now());
            orderService.confirmOrderAfterPayment(payment.getOrder().getId());
        }
    }

    private Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Paiement introuvable"
                ));
    }

    private String generateMockTransactionRef() {
        return "MOCK-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();
    }

    private PaymentResponseDTO mapToResponse(Payment payment) {

        PaymentResponseDTO dto = new PaymentResponseDTO();

        dto.id = payment.getId();
        dto.orderId = payment.getOrder().getId();
        dto.amount = payment.getAmount();
        dto.method = payment.getMethod();
        dto.status = payment.getStatus();
        dto.transactionRef = payment.getTransactionRef();
        dto.createAt = payment.getCreateAt();
        dto.paidAt = payment.getPaidAt();

        return dto;
    }
}
