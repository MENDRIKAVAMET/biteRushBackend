package com.biterush.api.dto;

import jakarta.validation.constraints.NotBlank;

public class DeliveryPersonUpdateDTO {

    @NotBlank(message = "La zone est obligatoire")
    public String zone;

    @NotBlank(message = "Le véhicule est obligatoire")
    public String vehicule;
}
