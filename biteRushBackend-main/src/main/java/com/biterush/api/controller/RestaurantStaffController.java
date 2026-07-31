package com.biterush.api.controller;

import com.biterush.api.dto.RestaurantStaffRequestDTO;
import com.biterush.api.dto.RestaurantStaffResponseDTO;
import com.biterush.api.service.RestaurantStaffService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Politique d'accès (point 3, session du 01/08/2026) :
 * - Lecture (GET) : ADMIN (vue globale) ou RESTAURANT_STAFF, mais un
 *   RESTAURANT_STAFF ne voit que le staff de SON PROPRE restaurant
 *   (filtrage fait dans RestaurantStaffService.*Scoped()).
 * - Écriture (POST/PUT/DELETE) : ADMIN uniquement. Gestion des comptes
 *   staff volontairement centralisée ADMIN pour l'instant (cf. CHANGELOG).
 */
@RestController
@RequestMapping("/restaurant-staff")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RestaurantStaffController {

    private final RestaurantStaffService restaurantStaffService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_STAFF','ADMIN')")
    public ResponseEntity<List<RestaurantStaffResponseDTO>> getAll() {

        List<RestaurantStaffResponseDTO> response = restaurantStaffService.getAllScoped();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_STAFF','ADMIN')")
    public ResponseEntity<RestaurantStaffResponseDTO> getById(@PathVariable Long id) {

        RestaurantStaffResponseDTO response = restaurantStaffService.getByIdScoped(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_STAFF','ADMIN')")
    public ResponseEntity<List<RestaurantStaffResponseDTO>> getByRestaurantId(
            @PathVariable Long restaurantId
    ) {

        List<RestaurantStaffResponseDTO> response =
                restaurantStaffService.getByRestaurantIdScoped(restaurantId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantStaffResponseDTO> create(
            @PathVariable Long userId,
            @Valid @RequestBody RestaurantStaffRequestDTO dto
    ) {

        RestaurantStaffResponseDTO response = restaurantStaffService.save(userId, dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantStaffResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantStaffRequestDTO dto
    ) {

        RestaurantStaffResponseDTO response = restaurantStaffService.update(id, dto);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        restaurantStaffService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
