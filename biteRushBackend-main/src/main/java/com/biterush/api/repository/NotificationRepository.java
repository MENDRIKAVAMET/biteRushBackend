package com.biterush.api.repository;

import com.biterush.api.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(Long userId);
    long countByRecipientIdAndReadFalse(Long userId);
}
