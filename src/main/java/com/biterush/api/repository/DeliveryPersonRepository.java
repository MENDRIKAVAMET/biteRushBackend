package com.biterush.api.repository;

import com.biterush.api.entity.DeliveryPerson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Long> {

    java.util.Optional<DeliveryPerson> findByUser_Id(Long userId);
}
