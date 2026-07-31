package com.biterush.api.repository;

import com.biterush.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.biterush.api.entity.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    boolean existsByEmail(String email);
}