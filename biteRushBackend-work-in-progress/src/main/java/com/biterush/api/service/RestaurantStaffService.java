package com.biterush.api.service;

import com.biterush.api.dto.RestaurantStaffRequestDTO;
import com.biterush.api.dto.RestaurantStaffResponseDTO;
import com.biterush.api.entity.RestaurantStaff;
import com.biterush.api.entity.User;
import com.biterush.api.repository.RestaurantStaffRepository;
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
public class RestaurantStaffService {

    private final RestaurantStaffRepository restaurantStaffRepository;
    private final UserRepository userRepository;

    /*
     * =========================================================
     * CREATE
     * =========================================================
     */
    public RestaurantStaffResponseDTO save(Long userId,
                                            RestaurantStaffRequestDTO dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable"
                ));

        if (restaurantStaffRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un profil staff existe déjà pour cet utilisateur"
            );
        }

        RestaurantStaff staff = new RestaurantStaff();
        staff.setUser(user);
        staff.setRestaurantId(dto.restaurantId);

        RestaurantStaff saved = restaurantStaffRepository.save(staff);

        return mapToResponse(saved);
    }

    /*
     * =========================================================
     * READ
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<RestaurantStaffResponseDTO> getAll() {

        return restaurantStaffRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantStaffResponseDTO getById(Long id) {

        RestaurantStaff staff = findRestaurantStaffEntityById(id);

        return mapToResponse(staff);
    }

    @Transactional(readOnly = true)
    public List<RestaurantStaffResponseDTO> getByRestaurantId(Long restaurantId) {

        return restaurantStaffRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /*
     * =========================================================
     * UPDATE
     * =========================================================
     */
    public RestaurantStaffResponseDTO update(Long id,
                                              RestaurantStaffRequestDTO dto) {

        RestaurantStaff staff = findRestaurantStaffEntityById(id);

        staff.setRestaurantId(dto.restaurantId);

        RestaurantStaff updated = restaurantStaffRepository.save(staff);

        return mapToResponse(updated);
    }

    /*
     * =========================================================
     * DELETE
     * =========================================================
     */
    public void delete(Long id) {

        RestaurantStaff staff = findRestaurantStaffEntityById(id);

        restaurantStaffRepository.delete(staff);
    }

    /*
     * =========================================================
     * PRIVATE HELPERS
     * =========================================================
     */

    private RestaurantStaff findRestaurantStaffEntityById(Long id) {

        return restaurantStaffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Staff restaurant introuvable"
                ));
    }

    /*
     * =========================================================
     * DTO MAPPING
     * =========================================================
     */

    private RestaurantStaffResponseDTO mapToResponse(RestaurantStaff staff) {

        RestaurantStaffResponseDTO dto = new RestaurantStaffResponseDTO();

        dto.id = staff.getId();
        dto.userId = staff.getUser().getId();
        dto.userName = staff.getUser().getNom();
        dto.email = staff.getUser().getEmail();
        dto.restaurantId = staff.getRestaurantId();

        return dto;
    }
}
