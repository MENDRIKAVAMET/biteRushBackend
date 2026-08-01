package com.biterush.api.service;

import com.biterush.api.dto.DeliveryResponseDTO;
import com.biterush.api.entity.*;
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
    private final NotificationService notificationService;
    private final com.biterush.api.repository.DeliveryPersonRepository deliveryPersonRepository;

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

        // Avant : aucune notification envoyée sur ce chemin d'assignation
        // (seul assignOrderToDeliveryPublic() appelait notifyDeliveryAssigned()).
        // Même événement métier (assignation d'une livraison à un livreur),
        // donc même notification, quel que soit le point d'entrée.
        notifyDeliveryAssigned(saved);

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
     * LIVREUR - PROFIL (GET/PUT /api/deliveries/profile)
     * =====================================================
     */
    public com.biterush.api.dto.DeliveryPersonProfileDTO getProfile() {

        User current = getCurrentUser();

        com.biterush.api.entity.DeliveryPerson person = getDeliveryPersonForCurrentUser(current);

        return mapToProfileResponse(person);
    }

    public com.biterush.api.dto.DeliveryPersonProfileDTO updateProfile(
            com.biterush.api.dto.DeliveryPersonUpdateDTO dto) {

        User current = getCurrentUser();

        com.biterush.api.entity.DeliveryPerson person = getDeliveryPersonForCurrentUser(current);

        person.setZone(dto.zone.trim());
        person.setVehicule(dto.vehicule.trim());

        return mapToProfileResponse(deliveryPersonRepository.save(person));
    }

    /*
     * =====================================================
     * LIVREUR - DISPONIBILITÉ (PATCH /api/deliveries/availability)
     * =====================================================
     */
    public com.biterush.api.dto.DeliveryPersonProfileDTO setAvailability(
            com.biterush.api.dto.AvailabilityUpdateDTO dto) {

        User current = getCurrentUser();

        com.biterush.api.entity.DeliveryPerson person = getDeliveryPersonForCurrentUser(current);

        person.setAvailable(dto.available);

        return mapToProfileResponse(deliveryPersonRepository.save(person));
    }

    private com.biterush.api.entity.DeliveryPerson getDeliveryPersonForCurrentUser(User current) {
        return deliveryPersonRepository.findByUser_Id(current.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profil livreur introuvable pour cet utilisateur"
                ));
    }

    /*
     * =====================================================
     * LISTE DES LIVREURS (RESTAURANT_STAFF + ADMIN)
     * =====================================================
     * Liste des livreurs (id, nom, zone, vehicule, available), triée par zone
     * puis par nom — exposée au staff restaurant via
     * /restaurant/orders/delivery-persons pour alimenter la modale d'assignation
     * (auparavant réservée ADMIN via /admin/deliveries/persons, d'où la saisie
     * manuelle de l'ID côté frontend).
     *
     * @param availableOnly si true, ne renvoie que les livreurs ayant déclaré
     *                      available=true (filtre optionnel — par défaut on
     *                      renvoie tout le monde, le frontend marque visuellement
     *                      les indisponibles).
     */
    public java.util.List<com.biterush.api.dto.DeliveryPersonProfileDTO> getAllDeliveryPersons(
            boolean availableOnly) {
        return deliveryPersonRepository.findAll().stream()
                .map(this::mapToProfileResponse)
                .filter(dto -> !availableOnly || dto.available)
                .sorted(
                        java.util.Comparator
                                .comparing(
                                        (com.biterush.api.dto.DeliveryPersonProfileDTO d) ->
                                                d.zone == null ? "" : d.zone,
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                        d -> d.nom == null ? "" : d.nom,
                                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private com.biterush.api.dto.DeliveryPersonProfileDTO mapToProfileResponse(
            com.biterush.api.entity.DeliveryPerson person) {

        com.biterush.api.dto.DeliveryPersonProfileDTO dto =
                new com.biterush.api.dto.DeliveryPersonProfileDTO();

        dto.id = person.getId();
        dto.userId = person.getUser().getId();
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

    /**
     * Stub vide auparavant. Branché sur NotificationService.notifyDeliveryAssigned(),
     * qui existait déjà (crée une Notification pour le livreur + broadcast WebSocket)
     * mais n'était jamais appelé depuis ce flux. Réutilise le patron existant plutôt
     * que d'en inventer un nouveau.
     */
    private void notifyDeliveryAssigned(Delivery delivery) {
        notificationService.notifyDeliveryAssigned(delivery);
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

        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utilisateur non authentifié"
            );
        }

        // Le principal posé par JwtFilter est l'email (String), pas l'entité User.
        String email = auth.getName();

        return userRepository.findByEmail(email)
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
        dto.orderPhone = delivery.getOrder().getPhone();
        dto.orderTotal = delivery.getOrder().getTotal();

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