package com.biterush.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Image entity for storing Base64 encoded images
 * Used for products, restaurants, user profiles, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "images")
public class Image {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "entity_type", nullable = false)
    private String entityType; // "PRODUCT", "RESTAURANT", "USER_PROFILE", etc.
    
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    
    @Column(name = "image_data", columnDefinition = "LONGTEXT", nullable = false)
    private String imageData; // Base64 encoded image
    
    @Column(name = "mime_type", nullable = false)
    private String mimeType; // "image/png", "image/jpeg", etc.
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_primary")
    private boolean isPrimary = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
