package com.finfive.crisfin.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

    Optional<Entitlement> findByUserId(Long userId);
}
