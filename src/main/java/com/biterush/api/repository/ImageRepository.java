package com.biterush.api.repository;

import com.biterush.api.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Image entity
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    /**
     * Find all images for a specific entity
     */
    List<Image> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    /**
     * Find primary image for an entity
     */
    Optional<Image> findByEntityTypeAndEntityIdAndIsPrimaryTrue(String entityType, Long entityId);
    
    /**
     * Find images ordered by creation date
     */
    List<Image> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
    
    /**
     * Check if primary image exists for an entity
     */
    boolean existsByEntityTypeAndEntityIdAndIsPrimaryTrue(String entityType, Long entityId);
    
    /**
     * Find image by ID
     */
    Optional<Image> findById(Long id);
    
    /**
     * Delete all images for an entity
     */
    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);
}
