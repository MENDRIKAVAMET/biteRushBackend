package com.biterush.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemDTO {
    public Long id;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    public String name;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    public String description;

    @NotNull(message = "Le prix est obligatoire")
    @Min(value = 0, message = "Le prix ne peut pas être négatif")
    public Double price;

    @NotNull(message = "Le stock est obligatoire")
    @Min(value = 0, message = "Le stock ne peut pas être négatif")
    public Integer stock;

    /**
     * Legacy : catégorie en texte libre, conservée pour compatibilité.
     * Plus obligatoire depuis l'ajout de la vraie ressource MenuCategory
     * (voir categoryId) — le frontend envoie désormais categoryId.
     */
    public String category;

    /**
     * Référence vers une MenuCategory du même restaurant. Optionnel pour ne
     * pas casser les items existants créés avant cette fonctionnalité.
     */
    public Long categoryId;

    public String imageUrl;
    public Long restaurantId;
    // Boolean nullable : permet de distinguer "champ absent" (on conserve la
    // valeur existante en base) de available=false (désactivation explicite).
    // Évite qu'un PUT partiel sans ce champ désactive silencieusement l'article.
    public Boolean available;
}
