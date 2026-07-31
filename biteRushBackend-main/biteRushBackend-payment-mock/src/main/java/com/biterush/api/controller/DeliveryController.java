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

    @PatchMapping("/{id}/start")
    public DeliveryResponseDTO start(@PathVariable Long id) {
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