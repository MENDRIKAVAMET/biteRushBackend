package com.biterush.api.controller;

import com.biterush.api.dto.MenuItemDTO;
import com.biterush.api.entity.MenuItem;
import com.biterush.api.entity.Restaurant;
import com.biterush.api.repository.MenuItemRepository;
import com.biterush.api.repository.RestaurantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/menu-items")
@RequiredArgsConstructor
public class MenuController {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    @GetMapping
    public ResponseEntity<List<MenuItem>> getAllMenuItems() {
        return ResponseEntity.ok(menuItemRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItem> getMenuItem(@PathVariable Long id) {
        return menuItemRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<MenuItem> createMenuItem(@Valid @RequestBody MenuItemDTO dto) {
        MenuItem item = new MenuItem();
        item.setName(dto.name);
        item.setDescription(dto.description);
        item.setPrice(dto.price);
        item.setStock(dto.stock);
        item.setCategory(dto.category);

        if (dto.restaurantId != null) {
            Restaurant restaurant = restaurantRepository.findById(dto.restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
            item.setRestaurant(restaurant);
        }

        MenuItem saved = menuItemRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<MenuItem> updateMenuItem(
            @PathVariable Long id,
            @Valid @RequestBody MenuItemDTO dto) {

        return menuItemRepository.findById(id)
            .map(item -> {
                item.setName(dto.name);
                item.setDescription(dto.description);
                item.setPrice(dto.price);
                item.setStock(dto.stock);
                item.setCategory(dto.category);
                return ResponseEntity.ok(menuItemRepository.save(item));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        if (menuItemRepository.existsById(id)) {
            menuItemRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/restaurant/{restaurantId}/menu")
    public ResponseEntity<List<MenuItem>> getRestaurantMenu(@PathVariable Long restaurantId) {
        List<MenuItem> items = menuItemRepository.findByRestaurantId(restaurantId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MenuItem>> searchMenuItems(@RequestParam String query) {
        List<MenuItem> results = menuItemRepository.findByNameContainingIgnoreCase(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<MenuItem>> getByCategory(@PathVariable String category) {
        List<MenuItem> items = menuItemRepository.findByCategory(category);
        return ResponseEntity.ok(items);
    }
}
