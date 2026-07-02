package com.finfive.crisfin.domain.welfare;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WelfareFavoriteRepository extends JpaRepository<WelfareFavorite, Long> {

    boolean existsByUserIdAndWelfareBenefitId(Long userId, Long welfareBenefitId);

    void deleteByUserIdAndWelfareBenefitId(Long userId, Long welfareBenefitId);

    List<WelfareFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
