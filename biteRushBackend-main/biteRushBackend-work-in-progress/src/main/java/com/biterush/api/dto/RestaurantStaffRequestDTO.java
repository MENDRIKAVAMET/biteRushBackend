package com.biterush.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RestaurantStaffRequestDTO {

    @NotNull(message = "L'ID du restaurant est obligatoire")
    @Positive(message = "L'ID du restaurant doit être positif")
    public Long restaurantId;
}
