package com.biterush.api.controller;

import com.biterush.api.dto.ClientRequestDTO;
import com.biterush.api.dto.ClientResponseDTO;
import com.biterush.api.service.ClientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> getAll() {

        List<ClientResponseDTO> response = clientService.getAll();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getById(@PathVariable Long id) {

        ClientResponseDTO response = clientService.getById(id);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ClientResponseDTO> create(
            @PathVariable Long userId,
            @Valid @RequestBody ClientRequestDTO dto
    ) {

        ClientResponseDTO response = clientService.save(userId, dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequestDTO dto
    ) {

        ClientResponseDTO response = clientService.update(id, dto);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        clientService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
