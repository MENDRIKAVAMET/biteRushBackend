package com.biterush.api.controller;

import com.biterush.api.dto.MenuCategoryDTO;
import com.biterush.api.entity.MenuCategory;
import com.biterush.api.entity.Restaurant;
import com.biterush.api.entity.RestaurantStaff;
import com.biterush.api.entity.User;
import com.biterush.api.repository.MenuCategoryRepository;
import com.biterush.api.repository.MenuItemRepository;
import com.biterush.api.repository.RestaurantRepository;
import com.biterush.api.repository.RestaurantStaffRepository;
import com.biterush.api.repository.UserRepository;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Ressource "catégorie de menu" — jusqu'ici le frontend appelait
 * /restaurants/{restaurantId}/menu-categories sans qu'aucun contrôleur
 * n'existe côté backend (404 systématique). `MenuItem.category` restait un
 * champ texte libre, non structuré, non réutilisable pour un vrai filtrage.
 *
 * Scoping identique au patron déjà en place dans RestaurantService /
 * RestaurantStaffService / HistoryController : un RESTAURANT_STAFF ne peut
 * gérer que les catégories de SON restaurant, un ADMIN peut agir sur
 * n'importe quel restaurant.
 */
@RestController
@RequestMapping("/restaurants/{restaurantId}/menu-categories")
@RequiredArgsConstructor
public class MenuCategoryController {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;

    @GetMapping
    @PreAuthorize("hasRole('RESTAURANT_STAFF') or hasRole('ADMIN')")
    public ResponseEntity<List<MenuCategory>> getCategories(
            @PathVariable Long restaurantId
    ) {
        verifyRestaurantAccess(restaurantId);

        return ResponseEntity.ok(menuCategoryRepository.findByRestaurant_Id(restaurantId));
    }

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_STAFF') or hasRole('ADMIN')")
    public ResponseEntity<MenuCategory> createCategory(
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuCategoryDTO dto
    ) {
        Restaurant restaurant = verifyRestaurantAccess(restaurantId);

        MenuCategory category = new MenuCategory();
        category.setName(dto.name);
        category.setDescription(dto.description);
        category.setRestaurant(restaurant);

        MenuCategory saved = menuCategoryRepository.save(category);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('RESTAURANT_STAFF') or hasRole('ADMIN')")
    public ResponseEntity<MenuCategory> updateCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId,
            @Valid @RequestBody MenuCategoryDTO dto
    ) {
        verifyRestaurantAccess(restaurantId);

        MenuCategory category = getCategoryInRestaurant(restaurantId, categoryId);

        category.setName(dto.name);
        category.setDescription(dto.description);

        MenuCategory updated = menuCategoryRepository.save(category);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('RESTAURANT_STAFF') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId
    ) {
        verifyRestaurantAccess(restaurantId);

        MenuCategory category = getCategoryInRestaurant(restaurantId, categoryId);

        // Les items existants ne sont pas supprimés en cascade : ils perdent
        // simplement leur catégorie (repassent en "non catégorisé") plutôt que
        // d'échouer avec une violation de contrainte FK ou de disparaître.
        List<com.biterush.api.entity.MenuItem> affectedItems =
                menuItemRepository.findByMenuCategory_Id(categoryId);

        affectedItems.forEach(item -> item.setMenuCategory(null));
        menuItemRepository.saveAll(affectedItems);

        menuCategoryRepository.delete(category);

        return ResponseEntity.noContent().build();
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private MenuCategory getCategoryInRestaurant(Long restaurantId, Long categoryId) {

        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Catégorie introuvable"
                ));

        if (!category.getRestaurant().getId().equals(restaurantId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Catégorie introuvable pour ce restaurant"
            );
        }

        return category;
    }

    /**
     * Vérifie que le restaurant existe ET que l'appelant y a droit :
     * - ADMIN : toujours autorisé
     * - RESTAURANT_STAFF : uniquement si {@code restaurantId} correspond à
     *   SON restaurant (même patron que RestaurantService.getCurrentStaffRestaurantId(),
     *   dupliqué localement pour rester cohérent avec le style déjà en place
     *   dans le projet — pas de classe utilitaire partagée existante pour ça).
     */
    private Restaurant verifyRestaurantAccess(Long restaurantId) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurant introuvable"
                ));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return restaurant;
        }

        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Utilisateur invalide"
                ));

        RestaurantStaff staff = restaurantStaffRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Aucun profil staff restaurant associé à ce compte"
                ));

        if (!staff.getRestaurantId().equals(restaurantId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Ce restaurant n'est pas le vôtre"
            );
        }

        return restaurant;
    }
}
