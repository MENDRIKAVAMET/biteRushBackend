package com.biterush.api.controller;

import com.biterush.api.dto.ClientRequestDTO;
import com.biterush.api.dto.ClientResponseDTO;
import com.biterush.api.entity.User;
import com.biterush.api.security.BusinessSecurityUtil;
import com.biterush.api.service.ClientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ClientController {

    private final ClientService clientService;
    private final BusinessSecurityUtil businessSecurity;

    /**
     * Reserve ADMIN : liste tous les profils clients.
     * (Avant : ouvert a tout utilisateur authentifie, aucune restriction.)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClientResponseDTO>> getAll() {

        List<ClientResponseDTO> response = clientService.getAll();

        return ResponseEntity.ok(response);
    }

    /**
     * Un client ne peut consulter que son propre profil (id de Client ==
     * id de User, relation @MapsId). ADMIN peut tout consulter.
     * (Avant : aucun filtre, un client pouvait lire le profil de n'importe
     * quel autre client en devinant son id.)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientResponseDTO> getById(@PathVariable Long id) {

        verifyOwner(id);

        ClientResponseDTO response = clientService.getById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Un utilisateur ne peut creer un profil client que pour lui-meme.
     * ADMIN peut creer pour n'importe quel userId.
     */
    @PostMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientResponseDTO> create(
            @PathVariable Long userId,
            @Valid @RequestBody ClientRequestDTO dto
    ) {

        verifyOwner(userId);

        ClientResponseDTO response = clientService.save(userId, dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequestDTO dto
    ) {

        verifyOwner(id);

        ClientResponseDTO response = clientService.update(id, dto);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        verifyOwner(id);

        clientService.delete(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Un client ne peut agir que sur son propre profil (id de Client ==
     * id de User, cf. relation @MapsId sur l'entite Client). ADMIN n'est
     * pas restreint.
     * Replique le patron utilise dans AddressController.verifyOwner().
     */
    private void verifyOwner(Long clientId) {
        User currentUser = businessSecurity.getCurrentUser();

        if (businessSecurity.isAdmin(currentUser)) {
            return;
        }

        if (!currentUser.getId().equals(clientId)) {
            throw new AccessDeniedException("Vous n'avez pas acces a ce profil client");
        }
    }
}
