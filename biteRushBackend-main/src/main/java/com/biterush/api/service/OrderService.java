package com.biterush.api.service;

import com.biterush.api.dto.*;
import com.biterush.api.entity.*;
import com.biterush.api.repository.DeliveryRepository;
import com.biterush.api.repository.OrderRepository;
import com.biterush.api.repository.ProductRepository;
import com.biterush.api.repository.RestaurantRepository;
import com.biterush.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.EN_ATTENTE
    );

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DeliveryRepository deliveryRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    /*
     * =========================================================
     * CREATE ORDER
     * =========================================================
     */

    public OrderResponseDTO createOrder(OrderRequestDTO dto) {

        validateOrderRequest(dto);

        User currentUser = getCurrentUser();

        Restaurant restaurant = restaurantRepository.findById(dto.restaurantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurant introuvable"
                ));

        Order order = new Order();

        order.setClientName(dto.clientName.trim());
        order.setPhone(dto.phone.trim());
        order.setAddress(dto.address.trim());

        // `user`/`creator` sont NOT NULL en base ; sans ceci, la sauvegarde
        // échouait systématiquement (violation de contrainte SQL).
        order.setUser(currentUser);
        order.setCreator(currentUser);
        order.setRestaurant(restaurant);

        order.setStatus(OrderStatus.EN_ATTENTE);
        order.setCancelToken(generateSecureToken());

        List<OrderItem> items = buildOrderItems(order, dto.items);

        order.setItems(items);
        order.calculateTotal();

        Order savedOrder = orderRepository.save(order);

        return mapToResponse(savedOrder);
    }

    /*
     * =========================================================
     * FIND
     * =========================================================
     */

    @Transactional(readOnly = true)
    public OrderResponseDTO findByIdWithToken(Long id, String token) {

        Order order = getOrderById(id);

        validateCancelToken(order, token);

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> findAll() {

        // Découvert lors de la vérification systématique SecurityConfig <-> service :
        // cette méthode (utilisée par GET /orders/admin) n'avait AUCUNE vérification
        // interne, alors que SecurityConfig autorise CLIENT/LIVREUR/ADMIN sur
        // GET /orders/** — n'importe quel client ou livreur pouvait donc lister TOUTES
        // les commandes du système. Ajout de validateAdmin() pour que ce endpoint
        // "admin" soit réellement réservé à l'ADMIN, comme son chemin le laisse penser.
        validateAdmin();

        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /*
     * =========================================================
     * UPDATE
     * =========================================================
     */

    public OrderResponseDTO updateOrder(Long id, OrderUpdateDTO dto) {

        validateAdmin();

        Order order = getOrderById(id);

        updateBasicInformation(order, dto);

        if (dto.status != null) {
            validateStatusTransition(order.getStatus(), dto.status);
            order.setStatus(dto.status);
            if(dto.status == OrderStatus.CONFIRMEE){
                handleOrderConfirmation(order);
            }
        }


        if (dto.items != null && !dto.items.isEmpty()) {
            validateOrderEditable(order);
            restoreStock(order);
            List<OrderItem> updatedItems =
                    buildOrderItems(order, dto.items);

            order.getItems().clear();
            order.getItems().addAll(updatedItems);

            order.calculateTotal();
        }

        Order updatedOrder = orderRepository.save(order);

        return mapToResponse(updatedOrder);
    }

    private void restoreStock(Order order){
        for(OrderItem item: order.getItems()){
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }

    /*
     * =========================================================
     * CANCEL
     * =========================================================
     */

    public void cancelOrder(Long id, String token) {

        Order order = getOrderById(id);

        validateCancelToken(order, token);

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cette commande ne peut plus être annulée"
            );
        }

        order.setStatus(OrderStatus.ANNULEE);
        for(OrderItem item: order.getItems()){
            Product product = item.getProduct();
            product.setStock(
                    product.getStock() + item.getQuantity()
            );
            productRepository.save(product);
        }

        orderRepository.save(order);
    }

    /*
     * =========================================================
     * DELETE
     * =========================================================
     */

    public void deleteOrder(Long id) {

        validateAdmin();

        Order order = getOrderById(id);

        restoreStock(order);

        orderRepository.delete(order);
    }

    /*
     * =========================================================
     * DELIVERY
     * =========================================================
     */

    public void markAsDelivered(Long id) {

        validateAdmin();

        Order order = getOrderById(id);

        if (order.getStatus() != OrderStatus.EN_LIVRAISON) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La commande n'est pas en livraison"
            );
        }

        order.setStatus(OrderStatus.LIVREE);

        orderRepository.save(order);
    }

    /*
    * =========================================================
    * DELIVRY
    * =========================================================
     */

    public Delivery assignOrderToDelivery(Long orderId, User livreur){
        Order order = orderRepository.findById(orderId)
                .orElseThrow();
        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setLivreur(livreur);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());

        return deliveryRepository.save(delivery);
    }

    public Delivery createFormOrder(Order order, User livreur){
        if(order.getDelivery()!=null){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une livraison existe déjà pour cette commande"
            );
        }
        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setLivreur(livreur);
        delivery.setStatus(DeliveryStatus.CREATED);
        delivery.setAssignedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.EN_LIVRAISON);
        orderRepository.save(order);
        return deliveryRepository.save(delivery);
    }


    /*
     * =========================================================
     * PRIVATE HELPERS
     * =========================================================
     */

    /*
     * =========================================================
     * PAYMENT (appelé par PaymentService lors d'un paiement réussi)
     * =========================================================
     */

    public void confirmOrderAfterPayment(Long orderId) {

        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.EN_ATTENTE) {
            order.setStatus(OrderStatus.CONFIRMEE);
            handleOrderConfirmation(order);
            orderRepository.save(order);
        }
    }

    private void handleOrderConfirmation(Order order){
        if(order.getDelivery() == null){
            createFormOrder(order, null);
        }
    }

    private void validateOrderRequest(OrderRequestDTO dto) {

        if (dto.items == null || dto.items.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La commande doit contenir au moins un produit"
            );
        }
    }

    private void validateOrderEditable(Order order){
        if(order.getStatus() != OrderStatus.EN_ATTENTE){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La commande ne peut plus être modifiée"
            );
        }
    }

    private void updateBasicInformation(Order order,
                                        OrderUpdateDTO dto) {

        if (dto.clientName != null) {
            order.setClientName(dto.clientName.trim());
        }

        if (dto.phone != null) {
            order.setPhone(dto.phone.trim());
        }

        if (dto.address != null) {
            order.setAddress(dto.address.trim());
        }
    }

    private List<OrderItem> buildOrderItems(Order order,
                                            List<OrderItemDTO> itemDTOs) {

        List<Long> productIds = itemDTOs.stream()
                .map(item -> item.productId)
                .distinct()
                .toList();

        Map<Long, Product> productMap = productRepository
                .findAllByIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        return itemDTOs.stream()
                .map(itemDto -> createOrderItem(order, itemDto, productMap))
                .toList();
    }

    private OrderItem createOrderItem(Order order,
                                      OrderItemDTO itemDto,
                                      Map<Long, Product> productMap) {

        Product product = productMap.get(itemDto.productId);

        if (product == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Produit avec ID " + itemDto.productId + " introuvable"
            );
        }

        if (itemDto.quantity <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantité invalide"
            );
        }

        /*
         * Gestion du stock
         */

        if (product.getStock() < itemDto.quantity) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Stock insuffisant pour le produit : " + product.getNom()
            );
        }

        product.setStock(product.getStock() - itemDto.quantity);
        productRepository.save(product);

        OrderItem item = new OrderItem();

        item.setOrder(order);
        item.setProduct(product);

        item.setQuantity(itemDto.quantity);
        item.setPrice(product.getPrix());

        return item;
    }

    private void validateStatusTransition(OrderStatus current,
                                          OrderStatus next) {

        boolean valid = switch (current) {

            case EN_ATTENTE ->
                    next == OrderStatus.CONFIRMEE
                            || next == OrderStatus.ANNULEE;

            case CONFIRMEE ->
                    next == OrderStatus.EN_PREPARATION;

            case EN_PREPARATION ->
                    next == OrderStatus.PRETE;

            case PRETE ->
                    next == OrderStatus.EN_LIVRAISON;

            case EN_LIVRAISON ->
                    next == OrderStatus.LIVREE;

            default -> false;
        };

        if (!valid) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transition de statut invalide"
            );
        }
    }

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utilisateur non authentifié"
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Utilisateur invalide"
                ));
    }

    /*
     * =========================================================
     * MES COMMANDES (client connecté)
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> findMyOrders() {

        User currentUser = getCurrentUser();

        return orderRepository.findByUser_Id(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateAdmin() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utilisateur non authentifié"
            );
        }

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(auth ->
                        auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Accès refusé"
            );
        }
    }

    private void validateCancelToken(Order order,
                                     String token) {

        if (token == null || token.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Token requis"
            );
        }

        if (!order.getCancelToken().equals(token)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Token invalide"
            );
        }
    }

    private Order getOrderById(Long id) {

        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Commande introuvable"
                ));
    }

    private String generateSecureToken() {

        SecureRandom random = new SecureRandom();

        byte[] bytes = new byte[32];

        random.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    /*
     * =========================================================
     * DTO MAPPING
     * =========================================================
     */

    private OrderResponseDTO mapToResponse(Order order) {

        OrderResponseDTO dto = new OrderResponseDTO();

        dto.id = order.getId();
        dto.clientName = order.getClientName();
        dto.phone = order.getPhone();
        dto.address = order.getAddress();

        dto.total = order.getTotal();
        dto.restaurantId = order.getRestaurant() != null ? order.getRestaurant().getId() : null;
        dto.status = order.getStatus();

        dto.createAt = order.getCreateAt();

        dto.items = order.getItems()
                .stream()
                .map(this::mapItemToResponse)
                .toList();

        return dto;
    }

    public OrderResponseDTO mapToResponsePublic(Order order) {
        return mapToResponse(order);
    }

    private OrderItemResponseDTO mapItemToResponse(OrderItem item) {

        OrderItemResponseDTO dto =
                new OrderItemResponseDTO();

        dto.id = item.getId();

        dto.productId = item.getProduct().getId();
        dto.productName = item.getProduct().getNom();

        dto.quantity = item.getQuantity();
        dto.price = item.getPrice();

        dto.subtotal = item.getSubtotal();

        return dto;
    }

    /**
     * Get order entity by ID (internal use for security checks)
     */
    public Order getOrderEntity(Long id) {
        return getOrderById(id);
    }
}