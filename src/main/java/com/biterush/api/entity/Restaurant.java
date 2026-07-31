package com.biterush.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "restaurants")
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    private String phoneNumber;

    private String email;

    private String imageUrl;

    private Double rating;

    private Integer deliveryTime;

    // Coordonnées GPS optionnelles : servent au calcul des frais de
    // livraison par distance (DeliveryFeeService). Absentes = fallback
    // sur un tarif forfaitaire par défaut, pas d'échec de commande.
    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private List<MenuItem> menuItems;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    private List<Order> orders;
}
