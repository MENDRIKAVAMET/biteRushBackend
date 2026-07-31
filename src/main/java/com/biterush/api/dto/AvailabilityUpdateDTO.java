package com.biterush.api.dto;

import jakarta.validation.constraints.NotNull;

public class AvailabilityUpdateDTO {

    @NotNull(message = "available est obligatoire")
    public Boolean available;
}
