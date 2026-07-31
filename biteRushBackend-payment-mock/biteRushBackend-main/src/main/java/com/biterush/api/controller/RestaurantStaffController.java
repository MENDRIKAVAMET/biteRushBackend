package com.biterush.api.controller;

import com.biterush.api.dto.RestaurantStaffRequestDTO;
import com.biterush.api.dto.RestaurantStaffResponseDTO;
import com.biterush.api.service.RestaurantStaffService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurant-staff")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RestaurantStaffController {

    private final RestaurantStaffService restaurantStaffService;

    @GetMapping
    public ResponseEntity<List<RestaurantStaffResponseDTO>> getAll() {

        List<RestaurantStaffResponseDTO> response = restaurantStaffService.getAll();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantStaffResponseDTO> getById(@PathVariable Long id) {

        RestaurantStaffResponseDTO response = restaurantStaffService.getById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<RestaurantStaffResponseDTO>> getByRestaurantId(
            @PathVariable Long restaurantId
    ) {

        List<RestaurantStaffResponseDTO> response =
                restaurantStaffService.getByRestaurantId(restaurantId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}")
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
    public ResponseEntity<RestaurantStaffResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantStaffRequestDTO dto
    ) {

        RestaurantStaffResponseDTO response = restaurantStaffService.update(id, dto);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        restaurantStaffService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
