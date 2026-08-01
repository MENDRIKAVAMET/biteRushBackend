package com.biterush.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.CLIENT;

    // Adresse du compte client — la colonne `address` existe déjà sur la table
    // `users` (l'entité Client est une vue @MapsId de cette même table, pas une
    // table séparée : l'adresse vit donc sur la ligne utilisateur).
    private String address;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;

    // Flow "mot de passe oublié" (ddl-auto=update, pas de migration à écrire)
    @JsonIgnore
    private String resetPasswordToken;

    @JsonIgnore
    private java.time.LocalDateTime resetPasswordTokenExpiry;
}