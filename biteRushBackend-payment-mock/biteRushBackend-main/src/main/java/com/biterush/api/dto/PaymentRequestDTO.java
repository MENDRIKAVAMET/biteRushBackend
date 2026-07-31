package com.biterush.api.dto;

import com.biterush.api.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class PaymentRequestDTO {
    @NotNull
    public Long orderId;

    @NotNull
    public PaymentMethod method;
}
