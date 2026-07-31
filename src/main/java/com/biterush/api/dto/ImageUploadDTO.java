package com.biterush.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for image upload
 */
public class ImageUploadDTO {
    
    @NotBlank(message = "Le contenu Base64 est requis")
    public String base64Data;
    
    @NotBlank(message = "Le type MIME est requis")
    public String mimeType;
    
    public String description;
    
    public ImageUploadDTO() {}
    
    public ImageUploadDTO(String base64Data, String mimeType) {
        this.base64Data = base64Data;
        this.mimeType = mimeType;
    }
}
