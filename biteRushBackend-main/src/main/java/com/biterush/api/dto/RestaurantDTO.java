package com.biterush.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDTO {
    public Long id;

    @NotBlank(message = "Le nom du restaurant est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    public String name;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    public String address;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères")
    public String phoneNumber;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email n'est pas valide")
    public String email;

    public String imageUrl;
    public Double rating;
    public Integer deliveryTime;
    public boolean active;

    // Optionnelles : utilisées par DeliveryFeeService pour le calcul
    // des frais de livraison par distance.
    public Double latitude;
    public Double longitude;
}
