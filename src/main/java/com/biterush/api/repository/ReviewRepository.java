package com.biterush.api.repository;

import com.biterush.api.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRestaurantId(Long restaurantId);
    Page<Review> findByRestaurantId(Long restaurantId, Pageable pageable);
    Page<Review> findByOrderId(Long orderId, Pageable pageable);
}
