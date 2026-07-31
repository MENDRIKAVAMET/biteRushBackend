package com.biterush.api.dto;

import jakarta.validation.constraints.NotNull;

public class DeliveryRequestDTO {
    @NotNull(message = "OrderId obligatoire")
    public Long orderId;

    @NotNull(message = "Livreur obligatoire")
    public Long livreurId;
}