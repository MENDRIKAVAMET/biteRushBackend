package com.biterush.api.controller;

import com.biterush.api.dto.NotificationDTO;
import com.biterush.api.entity.Notification;
import com.biterush.api.entity.User;
import com.biterush.api.security.BusinessSecurityUtil;
import com.biterush.api.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.biterush.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final BusinessSecurityUtil businessSecurity;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getMyNotifications() {

        Long userId = getCurrentUserId();
        List<NotificationDTO> response = notificationService.getMyNotifications(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {

        Long userId = getCurrentUserId();
        List<NotificationDTO> response = notificationService.getUnreadNotifications(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnread() {

        Long userId = getCurrentUserId();
        long count = notificationService.countUnread(userId);

        return ResponseEntity.ok(count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {

        verifyOwner(notificationService.getNotificationEntity(id));

        notificationService.markAsRead(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {

        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {

        verifyOwner(notificationService.getNotificationEntity(id));

        notificationService.deleteNotification(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Un utilisateur ne peut marquer comme lue / supprimer que ses propres
     * notifications. ADMIN non restreint. Même patron que
     * AddressController.verifyOwner() / ClientController.verifyOwner().
     * (Avant : markAsRead/deleteNotification n'importe quel utilisateur
     * authentifié pouvait agir sur la notification de n'importe qui.)
     */
    private void verifyOwner(Notification notification) {
        User currentUser = businessSecurity.getCurrentUser();

        if (businessSecurity.isAdmin(currentUser)) {
            return;
        }

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette notification");
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }

        String email = (String) auth.getPrincipal();
        
        return userRepository.findByEmail(email)
                .map(com.biterush.api.entity.User::getId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Utilisateur introuvable"
                ));
    }
}
