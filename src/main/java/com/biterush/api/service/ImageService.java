package com.biterush.api.service;

import com.biterush.api.dto.ImageUploadDTO;
import com.biterush.api.entity.Image;
import com.biterush.api.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing images
 * Handles Base64 encoded image uploads for products, restaurants, user profiles
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ImageService {
    
    private final ImageRepository imageRepository;
    
    /**
     * Allowed MIME types for images
     */
    private static final String[] ALLOWED_MIME_TYPES = {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    };
    
    /**
     * Maximum Base64 image size (5 MB)
     */
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    
    /**
     * Upload an image for an entity
     */
    public Image uploadImage(String entityType, Long entityId, ImageUploadDTO dto) {
        validateImageUpload(dto);
        
        // Set primary if it's the first image for this entity
        List<Image> existingImages = imageRepository.findByEntityTypeAndEntityId(entityType, entityId);
        boolean shouldBePrimary = dto.base64Data != null && existingImages.isEmpty();
        
        Image image = new Image();
        image.setEntityType(entityType);
        image.setEntityId(entityId);
        image.setImageData(dto.base64Data);
        image.setMimeType(dto.mimeType);
        image.setFileName(generateFileName(dto.mimeType));
        image.setDescription(dto.description);
        image.setPrimary(shouldBePrimary);
        image.setCreatedAt(LocalDateTime.now());
        image.setUpdatedAt(LocalDateTime.now());
        
        return imageRepository.save(image);
    }
    
    /**
     * Get all images for an entity
     */
    @Transactional(readOnly = true)
    public List<Image> getImages(String entityType, Long entityId) {
        return imageRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
    
    /**
     * Get primary image for an entity
     */
    @Transactional(readOnly = true)
    public Optional<Image> getPrimaryImage(String entityType, Long entityId) {
        return imageRepository.findByEntityTypeAndEntityIdAndIsPrimaryTrue(entityType, entityId);
    }
    
    /**
     * Get image by ID
     */
    @Transactional(readOnly = true)
    public Image getImageById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Image non trouvée"
                ));
    }
    
    /**
     * Set image as primary for an entity
     */
    public Image setPrimaryImage(String entityType, Long entityId, Long imageId) {
        Image image = getImageById(imageId);
        
        // Verify the image belongs to the entity
        if (!image.getEntityType().equals(entityType) || !image.getEntityId().equals(entityId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cette image n'appartient pas à cette entité"
            );
        }
        
        // Remove primary status from other images of this entity
        List<Image> images = getImages(entityType, entityId);
        for (Image img : images) {
            if (img.isPrimary()) {
                img.setPrimary(false);
                imageRepository.save(img);
            }
        }
        
        // Set this image as primary
        image.setPrimary(true);
        image.setUpdatedAt(LocalDateTime.now());
        return imageRepository.save(image);
    }
    
    /**
     * Delete an image
     */
    public void deleteImage(Long id) {
        Image image = getImageById(id);
        imageRepository.delete(image);
    }
    
    /**
     * Delete all images for an entity
     */
    public void deleteAllImages(String entityType, Long entityId) {
        imageRepository.deleteByEntityTypeAndEntityId(entityType, entityId);
    }
    
    /**
     * Validate image upload
     */
    private void validateImageUpload(ImageUploadDTO dto) {
        if (dto.base64Data == null || dto.base64Data.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les données Base64 de l'image sont requises"
            );
        }
        
        if (dto.mimeType == null || dto.mimeType.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le type MIME est requis"
            );
        }
        
        // Validate MIME type
        boolean isValidMimeType = false;
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (dto.mimeType.equals(allowed)) {
                isValidMimeType = true;
                break;
            }
        }
        
        if (!isValidMimeType) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Type MIME non autorisé: " + dto.mimeType
            );
        }
        
        // Validate size (Base64 is ~33% larger than binary)
        long estimatedSize = (long) (dto.base64Data.length() * 0.75);
        if (estimatedSize > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'image est trop grande (max 5MB)"
            );
        }
    }
    
    /**
     * Generate filename based on MIME type
     */
    private String generateFileName(String mimeType) {
        String extension = switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".img";
        };
        return "image_" + System.currentTimeMillis() + extension;
    }
}
