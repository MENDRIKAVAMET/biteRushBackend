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
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    /**
     * Toujours true tant qu'aucune vraie passerelle de paiement n'est branchée.
     * Le frontend peut s'en servir pour afficher un bandeau "mode test".
     */
    public boolean mock = true;
}
