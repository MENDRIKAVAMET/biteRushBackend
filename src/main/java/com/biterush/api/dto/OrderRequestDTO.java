package com.biterush.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class OrderRequestDTO {

    @NotBlank(message = "Le nom du client est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    public String clientName;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    public String phone;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    public String address;

    @jakarta.validation.constraints.NotNull(message = "Le restaurant est obligatoire")
    public Long restaurantId;

    // Optionnelles : coordonnées GPS de l'adresse de livraison, utilisées
    // par DeliveryFeeService pour calculer les frais par distance. Si absentes
    // (frontend qui n'envoie pas encore lat/lng), un tarif forfaitaire
    // par défaut s'applique à la place.
    public Double latitude;
    public Double longitude;

    @Valid
    @NotEmpty(message = "La commande doit contenir au moins un produit")
    public List<OrderItemDTO> items;
}