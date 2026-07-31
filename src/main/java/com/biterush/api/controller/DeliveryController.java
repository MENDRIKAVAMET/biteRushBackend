package com.biterush.api.controller;

import com.biterush.api.dto.DeliveryRequestDTO;
import com.biterush.api.dto.DeliveryResponseDTO;
import com.biterush.api.security.BusinessSecurityUtil;
import com.biterush.api.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/profile")
    public com.biterush.api.dto.DeliveryPersonProfileDTO getProfile() {
        return deliveryService.getProfile();
    }

    @PutMapping("/profile")
    public com.biterush.api.dto.DeliveryPersonProfileDTO updateProfile(
            @Valid @RequestBody com.biterush.api.dto.DeliveryPersonUpdateDTO dto) {
        return deliveryService.updateProfile(dto);
    }

    @PatchMapping("/availability")
    public com.biterush.api.dto.DeliveryPersonProfileDTO setAvailability(
            @Valid @RequestBody com.biterush.api.dto.AvailabilityUpdateDTO dto) {
        return deliveryService.setAvailability(dto);
    }

    @PatchMapping("/{id}/start")
    public DeliveryResponseDTO start(@PathVariable Long id) {
        return deliveryService.startDelivery(id);
    }

    // Alias métier de /start : côté frontend, "accepter" une livraison
    // assignée est la même action que "démarrer" (transition ASSIGNED -> IN_PROGRESS).
    @PatchMapping("/{id}/accept")
    public DeliveryResponseDTO accept(@PathVariable Long id) {
        return deliveryService.startDelivery(id);
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