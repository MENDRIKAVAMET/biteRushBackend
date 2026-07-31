package com.biterush.api.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public class ProductRequestDTO {

    @NotBlank(message = "Le nom est obligatoire.")
    public String nom;

    @NotNull(message = "Le prix est obligatoire.")
    @Min(value = 1, message = "le prix ne peut pas être nul.")
    public Double prix;

    @NotBlank(message = "La description est obligatoire.")
    public String description;

    @NotNull(message = "Le stock est obligatoire.")
    @Min(0)
    public Integer stock;
}
