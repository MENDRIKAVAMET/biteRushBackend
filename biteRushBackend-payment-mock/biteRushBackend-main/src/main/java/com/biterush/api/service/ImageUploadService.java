package com.biterush.api.service;

import com.biterush.api.dto.ImageUploadDTO;
import com.biterush.api.entity.Image;
import com.biterush.api.repository.ImageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing image uploads
 * Supports Base64 encoded images for products, restaurants, and user profiles
 */
@Service
@RequiredArgsConstructor
public class ImageUploadService {
    
    private final ImageRepository imageRepository;
    
    // Supported entity types
    private static final List<String> SUPPORTED_ENTITIES = List.of("product", "restaurant", "user");
    
    // Supported MIME types
    private static final List<String> SUPPORTED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml"
    );
    
    // Max file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    /**
     * Upload a new image
     */
    public Image uploadImage(String entityType, Long entityId, ImageUploadDTO dto) {
        validateInput(entityType, entityId, dto);
        
        Image image = new Image();
        image.setEntityType(entityType.toLowerCase());
        image.setEntityId(entityId);
        image.setImageData(dto.base64Data);
        image.setMimeType(dto.mimeType);
        image.setFileName(generateFileName(dto.mimeType));
        image.setDescription(dto.description);
        
        // If no primary image exists for this entity, set this as primary
        boolean hasPrimary = imageRepository.existsByEntityTypeAndEntityIdAndIsPrimaryTrue(
                entityType.toLowerCase(), entityId
        );
        image.setPrimary(!hasPrimary);
        
        image.setCreatedAt(LocalDateTime.now());
        image.setUpdatedAt(LocalDateTime.now());
        
        return imageRepository.save(image);
    }
    
    /**
     * Get all images for an entity
     */
    public List<Image> getImages(String entityType, Long entityId) {
        return imageRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                entityType.toLowerCase(), entityId
        );
    }
    
    /**
     * Get primary image for an entity
     */
    public Optional<Image> getPrimaryImage(String entityType, Long entityId) {
        return imageRepository.findByEntityTypeAndEntityIdAndIsPrimaryTrue(
                entityType.toLowerCase(), entityId
        );
    }
    
    /**
     * Get image by ID
     */
    public Image getImageById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with ID: " + id));
    }
    
    /**
     * Set image as primary for an entity
     */
    public Image setPrimaryImage(String entityType, Long entityId, Long imageId) {
        validateEntityType(entityType);
        
        Image image = getImageById(imageId);
        
        // Verify image belongs to the entity
        if (!image.getEntityType().equals(entityType.toLowerCase()) || 
            !image.getEntityId().equals(entityId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image does not belong to the specified entity"
            );
        }
        
        // Remove primary flag from other images of this entity
        List<Image> otherImages = imageRepository.findByEntityTypeAndEntityIdAndIsPrimaryTrue(
                entityType.toLowerCase(), entityId
        ).stream().toList();
        
        for (Image other : otherImages) {
            if (!other.getId().equals(imageId)) {
                other.setPrimary(false);
                imageRepository.save(other);
            }
        }
        
        // Set this image as primary
        image.setPrimary(true);
        image.setUpdatedAt(LocalDateTime.now());
        
        return imageRepository.save(image);
    }
    
    /**
     * Delete image by ID
     */
    public void deleteImage(Long id) {
        Image image = getImageById(id);
        
        // If this was the primary image, set another as primary if exists
        if (image.isPrimary()) {
            List<Image> otherImages = imageRepository.findByEntityTypeAndEntityId(
                    image.getEntityType(), image.getEntityId()
            );
            
            for (Image other : otherImages) {
                if (!other.getId().equals(id)) {
                    other.setPrimary(true);
                    imageRepository.save(other);
                    break;
                }
            }
        }
        
        imageRepository.deleteById(id);
    }
    
    /**
     * Delete all images for an entity
     */
    public void deleteAllImages(String entityType, Long entityId) {
        validateEntityType(entityType);
        imageRepository.deleteByEntityTypeAndEntityId(entityType.toLowerCase(), entityId);
    }
    
    /**
     * Validate input
     */
    private void validateInput(String entityType, Long entityId, ImageUploadDTO dto) {
        // Validate entity type
        validateEntityType(entityType);
        
        // Validate entity ID
        if (entityId == null || entityId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid entity ID"
            );
        }
        
        // Validate MIME type
        if (dto.mimeType == null || !SUPPORTED_MIME_TYPES.contains(dto.mimeType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported MIME type. Supported: " + SUPPORTED_MIME_TYPES
            );
        }
        
        // Validate Base64 data
        if (dto.base64Data == null || dto.base64Data.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image data is required"
            );
        }
        
        // Validate file size (Base64 size = 4/3 of actual file size)
        long estimatedSize = (long) (dto.base64Data.length() / 1.33);
        if (estimatedSize > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File size exceeds maximum limit of 5MB"
            );
        }
    }
    
    /**
     * Generate file name based on MIME type
     */
    private String generateFileName(String mimeType) {
        String extension = switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/svg+xml" -> "svg";
            default -> "img";
        };
        return "image_" + System.currentTimeMillis() + "." + extension;
    }
    
    /**
     * Validate entity type
     */
    private void validateEntityType(String entityType) {
        if (entityType == null || !SUPPORTED_ENTITIES.contains(entityType.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported entity type. Supported: " + SUPPORTED_ENTITIES
            );
        }
    }
}
