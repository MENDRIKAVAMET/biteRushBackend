package com.biterush.api.service;

import com.biterush.api.dto.DeliveryPersonProfileDTO;
import com.biterush.api.dto.DeliveryResponseDTO;
import com.biterush.api.entity.*;
import com.biterush.api.repository.DeliveryPersonRepository;
import com.biterush.api.repository.DeliveryRepository;
import com.biterush.api.repository.OrderRepository;
import com.biterush.api.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;

    /*
     * =====================================================
     * ADMIN - ASSIGN DELIVERY
     * =====================================================
     */
    public DeliveryResponseDTO assignDelivery(Long orderId, Long livreurId) {

        validateAdmin();

        Order order = getOrder(orderId);

        if (order.getDelivery() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Commande déjà assignée"
            );
        }

        User livreur = getUser(livreurId);

        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setLivreur(livreur);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());

        order.setStatus(OrderStatus.EN_LIVRAISON);

        orderRepository.save(order);

        Delivery saved = deliveryRepository.save(delivery);

        return mapToResponse(saved);
    }

    /*
     * =====================================================
     * GET ALL (ADMIN)
     * =====================================================
     */
    public List<DeliveryResponseDTO> getAllDeliveries() {

        validateAdmin();

        return deliveryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /*
     * =====================================================
     * GET ONE
     * =====================================================
     */
    public DeliveryResponseDTO findById(Long id) {

        Delivery delivery = getDelivery(id);

        validateAccess(delivery);

        return mapToResponse(delivery);
    }

    /*
     * =====================================================
     * LIVREUR - MY DELIVERIES
     * =====================================================
     */
    public List<DeliveryResponseDTO> getMyDeliveries() {

        User current = getCurrentUser();

        return deliveryRepository.findByLivreurId(current.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /*
     * =====================================================
     * LIVREUR - START DELIVERY
     * =====================================================
     */
    public DeliveryResponseDTO startDelivery(Long deliveryId) {

        Delivery delivery = getDelivery(deliveryId);

        validateOwner(delivery);

        if (delivery.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Livraison doit être ASSIGNED"
            );
        }

        delivery.setStatus(DeliveryStatus.IN_PROGRESS);

        return mapToResponse(deliveryRepository.save(delivery));
    }

    /*
     * =====================================================
     * LIVREUR - COMPLETE DELIVERY
     * =====================================================
     */
    public DeliveryResponseDTO deliver(Long deliveryId) {

        Delivery delivery = getDelivery(deliveryId);

        validateOwner(delivery);

        if (delivery.getStatus() != DeliveryStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Livraison pas en cours"
            );
        }

        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());

        Order order = delivery.getOrder();
        order.setStatus(OrderStatus.LIVREE);

        orderRepository.save(order);

        return mapToResponse(deliveryRepository.save(delivery));
    }

    /*
     * =====================================================
     * LIVREUR - CANCEL
     * =====================================================
     */
    public DeliveryResponseDTO cancelDelivery(Long deliveryId) {

        Delivery delivery = getDelivery(deliveryId);

        validateOwner(delivery);

        delivery.setStatus(DeliveryStatus.CANCELLED);

        Order order = delivery.getOrder();
        order.setStatus(OrderStatus.EN_PREPARATION);

        orderRepository.save(order);

        return mapToResponse(deliveryRepository.save(delivery));
    }

    /*
     * =====================================================
     * LIVREUR - PROFILE & AVAILABILITY
     * =====================================================
     */
    public DeliveryPersonProfileDTO getMyProfile() {

        User current = getCurrentUser();
        DeliveryPerson person = getDeliveryPersonEntity(current.getId());

        return mapToProfile(person);
    }

    public DeliveryPersonProfileDTO updateMyProfile(DeliveryPersonProfileDTO dto) {

        User current = getCurrentUser();
        DeliveryPerson person = getDeliveryPersonEntity(current.getId());

        if (dto.zone != null && !dto.zone.isBlank()) {
            person.setZone(dto.zone);
        }
        if (dto.vehicule != null && !dto.vehicule.isBlank()) {
            person.setVehicule(dto.vehicule);
        }

        return mapToProfile(deliveryPersonRepository.save(person));
    }

    public DeliveryPersonProfileDTO setAvailability(boolean available) {

        User current = getCurrentUser();
        DeliveryPerson person = getDeliveryPersonEntity(current.getId());

        person.setAvailable(available);

        return mapToProfile(deliveryPersonRepository.save(person));
    }

    private DeliveryPerson getDeliveryPersonEntity(Long userId) {
        return deliveryPersonRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profil livreur introuvable"
                ));
    }

    private DeliveryPersonProfileDTO mapToProfile(DeliveryPerson person) {
        DeliveryPersonProfileDTO dto = new DeliveryPersonProfileDTO();
        dto.id = person.getId();
        dto.nom = person.getUser().getNom();
        dto.email = person.getUser().getEmail();
        dto.zone = person.getZone();
        dto.vehicule = person.getVehicule();
        dto.available = person.isAvailable();
        return dto;
    }

    /*
     * =====================================================
     * ASSIGN - RESTAURANT STAFF (PUBLIC)
     * =====================================================
     */
    public DeliveryResponseDTO assignOrderToDeliveryPublic(Long orderId, User livreur) {

        Order order = getOrder(orderId);

        if (order.getDelivery() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Commande déjà assignée"
            );
        }

        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setLivreur(livreur);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());

        order.setStatus(OrderStatus.EN_LIVRAISON);

        orderRepository.save(order);

        Delivery saved = deliveryRepository.save(delivery);

        notifyDeliveryAssigned(saved);

        return mapToResponse(saved);
    }

    private void notifyDeliveryAssigned(Delivery delivery) {
        // Will be called from RestaurantService if NotificationService available
    }

    /*
     * =====================================================
     * DELETE (ADMIN)
     * =====================================================
     */
    public void delete(Long id) {

        validateAdmin();

        Delivery delivery = getDelivery(id);

        deliveryRepository.delete(delivery);
    }

    /*
     * =====================================================
     * HELPERS
     * =====================================================
     */

    private Delivery getDelivery(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Livraison introuvable"
                ));
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Commande introuvable"
                ));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable"
                ));
    }

    /*
     * =====================================================
     * SECURITY
     * =====================================================
     */

    private void validateAdmin() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Non authentifié"
            );
        }

        boolean isAdmin = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin requis"
            );
        }
    }

    private void validateOwner(Delivery delivery) {

        User current = getCurrentUser();

        if (!delivery.getLivreur().getId().equals(current.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Accès refusé"
            );
        }
    }

    private void validateAccess(Delivery delivery) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) return;

        validateOwner(delivery);
    }

    private User getCurrentUser() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utilisateur non authentifié"
            );
        }

        // JwtFilter place l'email (String) comme principal, pas l'entité User.
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Utilisateur invalide"
                ));
    }

    /*
     * =====================================================
     * MAPPING DTO
     * =====================================================
     */

    private DeliveryResponseDTO mapToResponse(Delivery delivery) {

        DeliveryResponseDTO dto = new DeliveryResponseDTO();

        dto.id = delivery.getId();

        dto.orderId = delivery.getOrder().getId();
        dto.clientName = delivery.getOrder().getClientName();
        dto.address = delivery.getOrder().getAddress();

        dto.livreurId = delivery.getLivreur().getId();
        dto.livreurName = delivery.getLivreur().getNom();

        dto.status = delivery.getStatus();

        dto.createdAt = delivery.getAssignedAt();
        dto.updatedAt = delivery.getDeliveredAt();

        return dto;
    }

    /**
     * Get delivery entity by ID (internal use for security checks)
     */
    public Delivery getDeliveryEntity(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Livraison non trouvée"
                ));
    }
}