package com.biterush.api.repository;

import com.biterush.api.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Product> findAllByIdIn(@Param("ids") List<Long> ids);
}
