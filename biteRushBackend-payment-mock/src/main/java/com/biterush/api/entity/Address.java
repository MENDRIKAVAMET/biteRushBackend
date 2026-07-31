package com.biterush.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String zipCode;
    private String country;
    private Double latitude;
    private Double longitude;
    private String label;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
