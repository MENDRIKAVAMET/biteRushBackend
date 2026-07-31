package com.biterush.api.service;

import com.biterush.api.dto.ClientRequestDTO;
import com.biterush.api.dto.ClientResponseDTO;
import com.biterush.api.entity.Client;
import com.biterush.api.entity.User;
import com.biterush.api.repository.ClientRepository;
import com.biterush.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    /*
     * =========================================================
     * CREATE
     * =========================================================
     */
    public ClientResponseDTO save(Long userId, ClientRequestDTO dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable"
                ));

        if (clientRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un profil client existe déjà pour cet utilisateur"
            );
        }

        Client client = new Client();
        client.setUser(user);
        client.setAddress(dto.address.trim());

        Client saved = clientRepository.save(client);

        return mapToResponse(saved);
    }

    /*
     * =========================================================
     * READ
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<ClientResponseDTO> getAll() {

        return clientRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponseDTO getById(Long id) {

        Client client = findClientEntityById(id);

        return mapToResponse(client);
    }

    /*
     * =========================================================
     * UPDATE
     * =========================================================
     */
    public ClientResponseDTO update(Long id, ClientRequestDTO dto) {

        Client client = findClientEntityById(id);

        client.setAddress(dto.address.trim());

        Client updated = clientRepository.save(client);

        return mapToResponse(updated);
    }

    /*
     * =========================================================
     * DELETE
     * =========================================================
     */
    public void delete(Long id) {

        Client client = findClientEntityById(id);

        clientRepository.delete(client);
    }

    /*
     * =========================================================
     * PRIVATE HELPERS
     * =========================================================
     */

    private Client findClientEntityById(Long id) {

        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Client introuvable"
                ));
    }

    /*
     * =========================================================
     * DTO MAPPING
     * =========================================================
     */

    private ClientResponseDTO mapToResponse(Client client) {

        ClientResponseDTO dto = new ClientResponseDTO();

        dto.id = client.getId();
        dto.userId = client.getUser().getId();
        dto.userName = client.getUser().getNom();
        dto.email = client.getUser().getEmail();
        dto.address = client.getAddress();

        return dto;
    }
}
