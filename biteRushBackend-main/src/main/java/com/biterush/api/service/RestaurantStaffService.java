package com.biterush.api.service;

import com.biterush.api.dto.RestaurantStaffRequestDTO;
import com.biterush.api.dto.RestaurantStaffResponseDTO;
import com.biterush.api.entity.RestaurantStaff;
import com.biterush.api.entity.User;
import com.biterush.api.repository.RestaurantStaffRepository;
import com.biterush.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
     * ACCÈS SCOPÉ PAR RESTAURANT (RESTAURANT_STAFF en lecture seule)
     * =========================================================
     * Décision de conception (point 3 de la mission) : un RESTAURANT_STAFF
     * peut consulter (lecture seule) le staff de SON PROPRE restaurant,
     * mais pas créer/modifier/supprimer de compte staff — ces opérations
     * restent ADMIN-only (@PreAuthorize sur le contrôleur). Justification :
     * la lecture seule est utile opérationnellement (voir qui fait partie
     * de son équipe) sans risque particulier, alors que la gestion des
     * comptes (création/suppression d'accès) reste une opération
     * sensible qu'on préfère garder centralisée côté ADMIN pour l'instant.
     * Même patron de résolution que RestaurantService.getCurrentStaffRestaurantId().
     */

    private Long getCurrentStaffRestaurantId() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Non authentifié"
            );
        }

        boolean isAdmin = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return null;
        }

        boolean isStaff = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_STAFF"));

        if (!isStaff) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Accès refusé"
            );
        }

        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Utilisateur invalide"
                ));

        RestaurantStaff staff = restaurantStaffRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Aucun profil staff restaurant associé à ce compte"
                ));

        return staff.getRestaurantId();
    }

    @Transactional(readOnly = true)
    public List<RestaurantStaffResponseDTO> getAllScoped() {

        Long staffRestaurantId = getCurrentStaffRestaurantId();

        if (staffRestaurantId == null) {
            return getAll();
        }

        return getByRestaurantId(staffRestaurantId);
    }

    @Transactional(readOnly = true)
    public RestaurantStaffResponseDTO getByIdScoped(Long id) {

        Long staffRestaurantId = getCurrentStaffRestaurantId();

        RestaurantStaff staff = findRestaurantStaffEntityById(id);

        if (staffRestaurantId != null
                && !staffRestaurantId.equals(staff.getRestaurantId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Ce membre du staff n'appartient pas à votre restaurant"
            );
        }

        return mapToResponse(staff);
    }

    @Transactional(readOnly = true)
    public List<RestaurantStaffResponseDTO> getByRestaurantIdScoped(Long restaurantId) {

        Long staffRestaurantId = getCurrentStaffRestaurantId();

        if (staffRestaurantId != null && !staffRestaurantId.equals(restaurantId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous ne pouvez consulter que le staff de votre propre restaurant"
            );
        }

        return getByRestaurantId(restaurantId);
    }

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
