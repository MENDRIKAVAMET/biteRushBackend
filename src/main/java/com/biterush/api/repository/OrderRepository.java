package com.biterush.api.repository;

import com.biterush.api.entity.Order;
import com.biterush.api.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser_Email(String email);
    List<Order> findByUser_Id(Long userId);
    List<Order> findByStatusOrderByCreateAtDesc(OrderStatus status);
    List<Order> findByAssignedToIdOrderByCreateAtDesc(Long staffId);
    List<Order> findByStatusAndRestaurant_IdOrderByCreateAtDesc(OrderStatus status, Long restaurantId);
    List<Order> findByRestaurant_IdOrderByCreateAtDesc(Long restaurantId);
}
