package com.finfive.crisfin.domain.welfare;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WelfareBenefitRepository extends JpaRepository<WelfareBenefit, Long> {

    Page<WelfareBenefit> findByIsActiveTrue(Pageable pageable);

    /**
     * Finds active welfare benefits whose JSONB {@code crisis_tags} array contains
     * the given string tag.
     *
     * <p>Uses a PostgreSQL-native {@code @>} containment check via
     * {@code jsonb_build_array} to avoid JPA confusion with the {@code ?} operator.
     */
    @Query(
        value = "SELECT * FROM welfare_benefits " +
                "WHERE is_active = true " +
                "  AND crisis_tags @> jsonb_build_array(CAST(:tag AS TEXT))",
        countQuery = "SELECT COUNT(*) FROM welfare_benefits " +
                     "WHERE is_active = true " +
                     "  AND crisis_tags @> jsonb_build_array(CAST(:tag AS TEXT))",
        nativeQuery = true
    )
    Page<WelfareBenefit> findByIsActiveTrueAndCrisisTagsContaining(
            @Param("tag") String tag, Pageable pageable);

    Optional<WelfareBenefit> findByExternalServiceId(String externalServiceId);
}
