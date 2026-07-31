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
@Table(name = "restaurant_staff")
public class RestaurantStaff {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    private User user;

    private Long restaurantId;
}
