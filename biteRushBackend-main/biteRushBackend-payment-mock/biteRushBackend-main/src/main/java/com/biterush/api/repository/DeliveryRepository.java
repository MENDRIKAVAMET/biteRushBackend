package com.biterush.api.repository;

import com.biterush.api.entity.Delivery;
import com.biterush.api.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByLivreurId(Long livreurId);
    List<Delivery> findByStatus(DeliveryStatus status);
    boolean existsByOrder_Id(Long orderId);
    List<Delivery> findByLivreurEmail(String email);
}
