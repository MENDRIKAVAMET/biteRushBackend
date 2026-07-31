package com.biterush.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "delivery_persons")
public class DeliveryPerson {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    private User user;

    private String zone;

    private String vehicule;

    // Disponibilité déclarée par le livreur lui-même (n'affecte pas
    // l'assignation automatique, qui n'existe pas encore côté service —
    // sert uniquement à afficher/filtrer côté frontend pour l'instant).
    private boolean available = true;
}
