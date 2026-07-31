package com.biterush.api.dto;

import com.biterush.api.entity.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public class DeliveryUpdateDTO {

    @NotNull(message = "Action obligatoire")
    public String action;
}