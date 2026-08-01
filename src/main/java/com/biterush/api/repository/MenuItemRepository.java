package com.biterush.api.repository;

import com.biterush.api.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantId(Long restaurantId);
    List<MenuItem> findByNameContainingIgnoreCase(String name);
    List<MenuItem> findByCategory(String category);
    List<MenuItem> findByAvailable(boolean available);
    List<MenuItem> findByMenuCategory_Id(Long menuCategoryId);
}
