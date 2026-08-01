package com.biterush.api.controller;

import com.biterush.api.dto.ImageUploadDTO;
import com.biterush.api.entity.Image;
import com.biterush.api.entity.MenuItem;
import com.biterush.api.entity.RestaurantStaff;
import com.biterush.api.entity.User;
import com.biterush.api.repository.MenuItemRepository;
import com.biterush.api.repository.RestaurantStaffRepository;
import com.biterush.api.repository.UserRepository;
import com.biterush.api.service.ImageUploadService;
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
 * Controller for image upload and management
 * Supports Base64 encoded image uploads for:
 * - Products
 * - Restaurants
 * - User profiles
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ImageController {
    
    private final ImageUploadService imageService;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;
    
    /**
     * Upload image for an entity
     * POST /api/images/products/{id}
     * POST /api/images/restaurants/{id}
     * POST /api/images/users/{id}
     */
    @PostMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<ImageResponseDTO> uploadImage(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @Valid @RequestBody ImageUploadDTO dto
    ) {
        verifyMenuItemOwnershipIfApplicable(entityType, entityId);
        Image image = imageService.uploadImage(entityType, entityId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(image));
    }
    
    /**
     * Get all images for an entity
     * GET /api/images/products/{id}
     * GET /api/images/restaurants/{id}
     * GET /api/images/users/{id}
     */
    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<ImageResponseDTO>> getImages(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        List<Image> images = imageService.getImages(entityType, entityId);
        List<ImageResponseDTO> response = images.stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get primary image for an entity
     * GET /api/images/products/{id}/primary
     * GET /api/images/restaurants/{id}/primary
     * GET /api/images/users/{id}/primary
     */
    @GetMapping("/{entityType}/{entityId}/primary")
    public ResponseEntity<ImageResponseDTO> getPrimaryImage(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        return imageService.getPrimaryImage(entityType, entityId)
                .map(image -> ResponseEntity.ok(mapToResponse(image)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Get image by ID
     * GET /api/images/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ImageResponseDTO> getImageById(@PathVariable Long id) {
        Image image = imageService.getImageById(id);
        return ResponseEntity.ok(mapToResponse(image));
    }
    
    /**
     * Set image as primary
     * PUT /api/images/{entityType}/{entityId}/images/{imageId}/primary
     */
    @PutMapping("/{entityType}/{entityId}/{imageId}/primary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<ImageResponseDTO> setPrimaryImage(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PathVariable Long imageId
    ) {
        verifyMenuItemOwnershipIfApplicable(entityType, entityId);
        Image image = imageService.setPrimaryImage(entityType, entityId, imageId);
        return ResponseEntity.ok(mapToResponse(image));
    }
    
    /**
     * Delete image by ID
     * DELETE /api/images/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        Image existing = imageService.getImageById(id);
        verifyMenuItemOwnershipIfApplicable(existing.getEntityType(), existing.getEntityId());
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Delete all images for an entity
     * DELETE /api/images/{entityType}/{entityId}
     */
    @DeleteMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESTAURANT_STAFF')")
    public ResponseEntity<Void> deleteAllImages(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        verifyMenuItemOwnershipIfApplicable(entityType, entityId);
        imageService.deleteAllImages(entityType, entityId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Un RESTAURANT_STAFF ne peut uploader/supprimer une image que pour un
     * menu item de SON restaurant (même patron d'appartenance que
     * MenuCategoryController/RestaurantService, dupliqué localement pour
     * rester cohérent avec le style déjà en place). ADMIN passe sans
     * restriction. Sans cette vérification, un staff pourrait manipuler les
     * images des articles de n'importe quel autre restaurant en devinant
     * l'ID du menu item.
     */
    private void verifyMenuItemOwnershipIfApplicable(String entityType, Long entityId) {

        if (!"menuitem".equalsIgnoreCase(entityType)) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return;
        }

        MenuItem item = menuItemRepository.findById(entityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item de menu introuvable"
                ));

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

        if (!item.getRestaurant().getId().equals(staff.getRestaurantId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cet item de menu n'appartient pas à votre restaurant"
            );
        }
    }
    
    /**
     * DTO for image response
     */
    public static class ImageResponseDTO {
        public Long id;
        public String entityType;
        public Long entityId;
        public String base64Data;
        public String mimeType;
        public String fileName;
        public String description;
        public boolean isPrimary;
        public String createdAt;
        public String updatedAt;
    }
    
    /**
     * Map Image entity to response DTO
     */
    private ImageResponseDTO mapToResponse(Image image) {
        ImageResponseDTO dto = new ImageResponseDTO();
        dto.id = image.getId();
        dto.entityType = image.getEntityType();
        dto.entityId = image.getEntityId();
        dto.base64Data = image.getImageData();
        dto.mimeType = image.getMimeType();
        dto.fileName = image.getFileName();
        dto.description = image.getDescription();
        dto.isPrimary = image.isPrimary();
        dto.createdAt = image.getCreatedAt().toString();
        dto.updatedAt = image.getUpdatedAt().toString();
        return dto;
    }
}
