package com.biterush.api.dto;

import com.biterush.api.entity.PaymentMethod;
import com.biterush.api.entity.PaymentStatus;

import java.time.LocalDateTime;

public class PaymentResponseDTO {
    public Long id;
    public Long orderId;
    public double amount;
    public PaymentMethod method;
    public PaymentStatus status;
    public String transactionRef;
    public LocalDateTime createAt;
    public LocalDateTime paidAt;

    /**
     * Indicateur explicite pour le frontend : ce paiement n'est pas réel.
     */
    public boolean mock = true;
}
