package com.biterush.api.repository;

import com.biterush.api.entity.RestaurantStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantStaffRepository extends JpaRepository<RestaurantStaff, Long> {
    List<RestaurantStaff> findByRestaurantId(Long restaurantId);
}
