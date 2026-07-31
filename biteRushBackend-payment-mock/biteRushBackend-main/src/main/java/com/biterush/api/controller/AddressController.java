package com.biterush.api.controller;

import com.biterush.api.dto.AddressDTO;
import com.biterush.api.entity.Address;
import com.biterush.api.entity.User;
import com.biterush.api.exception.ResourceNotFoundException;
import com.biterush.api.repository.AddressRepository;
import com.biterush.api.repository.UserRepository;
import com.biterush.api.util.PageResponse;
import com.biterush.api.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Address> createAddress(@Valid @RequestBody AddressDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.isDefault) {
            addressRepository.findByUserIdAndIsDefault(user.getId(), true)
                .ifPresent(addr -> {
                    addr.setDefault(false);
                    addressRepository.save(addr);
                });
        }

        Address address = new Address();
        address.setUser(user);
        address.setStreet(dto.street);
        address.setCity(dto.city);
        address.setZipCode(dto.zipCode);
        address.setCountry(dto.country);
        address.setLatitude(dto.latitude);
        address.setLongitude(dto.longitude);
        address.setLabel(dto.label);
        address.setDefault(dto.isDefault);

        Address saved = addressRepository.save(address);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PageResponse<Address>> getAddresses(
            Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Address> addresses = addressRepository.findByUserId(user.getId());
        Page<Address> page = new PageImpl<>(addresses, pageable, addresses.size());
        return ResponseEntity.ok(PaginationUtil.mapPageToResponse(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Address> getAddress(@PathVariable Long id) {
        return addressRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Address> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressDTO dto) {

        Address address = addressRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setStreet(dto.street);
        address.setCity(dto.city);
        address.setZipCode(dto.zipCode);
        address.setCountry(dto.country);
        address.setLatitude(dto.latitude);
        address.setLongitude(dto.longitude);
        address.setLabel(dto.label);

        Address updated = addressRepository.save(address);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        if (addressRepository.existsById(id)) {
            addressRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("Address not found");
    }
}
