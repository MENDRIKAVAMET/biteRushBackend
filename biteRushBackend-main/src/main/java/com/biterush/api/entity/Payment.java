package com.biterush.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité de paiement — MOCK.
 * Aucune passerelle réelle (Stripe/PayPal/Mobile Money) n'est appelée ici :
 * le "traitement" est simulé en mémoire dans PaymentService.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.EN_ATTENTE;

    /**
     * Référence de transaction simulée (aucun lien avec une vraie passerelle).
     */
    @Column(unique = true, nullable = false)
    private String transactionRef;

    private LocalDateTime createAt = LocalDateTime.now();

    private LocalDateTime paidAt;
}
