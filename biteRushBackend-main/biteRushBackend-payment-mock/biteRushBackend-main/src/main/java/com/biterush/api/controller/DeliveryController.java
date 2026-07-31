package com.biterush.api.controller;

import com.biterush.api.dto.DeliveryPersonProfileDTO;
import com.biterush.api.dto.DeliveryRequestDTO;
import com.biterush.api.dto.DeliveryResponseDTO;
import com.biterush.api.security.BusinessSecurityUtil;
import com.biterush.api.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final BusinessSecurityUtil businessSecurity;

    @PostMapping("/assign")
    public DeliveryResponseDTO assignDelivery(
            @Valid @RequestBody DeliveryRequestDTO dto
    ) {
        return deliveryService.assignDelivery(dto.orderId, dto.livreurId);
    }

    @GetMapping
    public List<DeliveryResponseDTO> getAllDeliveries() {
        return deliveryService.getAllDeliveries();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        deliveryService.delete(id);
    }

    @GetMapping("/me")
    public List<DeliveryResponseDTO> getMyDeliveries() {
        return deliveryService.getMyDeliveries();
    }

    @PatchMapping("/{id}/start")
    public DeliveryResponseDTO start(@PathVariable Long id) {
        return deliveryService.startDelivery(id);
    }

    /**
     * Alias métier de /start : le livreur accepte/prend en charge une livraison
     * qui lui a été assignée. Correspond à ce qu'appelle le frontend.
     */
    @PatchMapping("/{id}/accept")
    public DeliveryResponseDTO accept(@PathVariable Long id) {
        return deliveryService.startDelivery(id);
    }

    @GetMapping("/profile")
    public DeliveryPersonProfileDTO getMyProfile() {
        return deliveryService.getMyProfile();
    }

    @PutMapping("/profile")
    public DeliveryPersonProfileDTO updateMyProfile(@RequestBody DeliveryPersonProfileDTO dto) {
        return deliveryService.updateMyProfile(dto);
    }

    @PatchMapping("/availability")
    public DeliveryPersonProfileDTO setAvailability(@RequestBody Map<String, Boolean> body) {
        boolean available = Boolean.TRUE.equals(body.get("available"));
        return deliveryService.setAvailability(available);
    }

    @PatchMapping("/{id}/deliver")
    public DeliveryResponseDTO deliver(@PathVariable Long id) {
        return deliveryService.deliver(id);
    }

    @PatchMapping("/{id}/cancel")
    public DeliveryResponseDTO cancel(@PathVariable Long id) {
        return deliveryService.cancelDelivery(id);
    }

    @GetMapping("/{id}")
    public DeliveryResponseDTO findById(@PathVariable Long id) {
        DeliveryResponseDTO response = deliveryService.findById(id);
        businessSecurity.verifyDeliveryAccess(deliveryService.getDeliveryEntity(id));
        return response;
    }
}