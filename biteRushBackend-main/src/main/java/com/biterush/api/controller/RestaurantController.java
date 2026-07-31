package com.biterush.api.controller;

import com.biterush.api.dto.RestaurantDTO;
import com.biterush.api.entity.Restaurant;
import com.biterush.api.repository.RestaurantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantRepository restaurantRepository;

    @GetMapping
    public ResponseEntity<List<Restaurant>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getRestaurant(@PathVariable Long id) {
        return restaurantRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Restaurant> createRestaurant(@Valid @RequestBody RestaurantDTO dto) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(dto.name);
        restaurant.setAddress(dto.address);
        restaurant.setPhoneNumber(dto.phoneNumber);
        restaurant.setEmail(dto.email);
        restaurant.setLatitude(dto.latitude);
        restaurant.setLongitude(dto.longitude);

        Restaurant saved = restaurantRepository.save(restaurant);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<Restaurant> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantDTO dto) {

        return restaurantRepository.findById(id)
            .map(restaurant -> {
                restaurant.setName(dto.name);
                restaurant.setAddress(dto.address);
                restaurant.setPhoneNumber(dto.phoneNumber);
                restaurant.setEmail(dto.email);
                restaurant.setLatitude(dto.latitude);
                restaurant.setLongitude(dto.longitude);
                return ResponseEntity.ok(restaurantRepository.save(restaurant));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        if (restaurantRepository.existsById(id)) {
            restaurantRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> searchRestaurants(@RequestParam String query) {
        List<Restaurant> results = restaurantRepository.findByNameContainingIgnoreCase(query);
        return ResponseEntity.ok(results);
    }
}
