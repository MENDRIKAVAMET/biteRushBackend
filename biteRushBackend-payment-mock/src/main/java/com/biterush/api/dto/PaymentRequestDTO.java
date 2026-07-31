package com.biterush.api.dto;

import com.biterush.api.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public class PaymentRequestDTO {

    @NotNull(message = "L'identifiant de la commande est requis")
    public Long orderId;

    @NotNull(message = "La méthode de paiement est requise")
    public PaymentMethod method;
}
