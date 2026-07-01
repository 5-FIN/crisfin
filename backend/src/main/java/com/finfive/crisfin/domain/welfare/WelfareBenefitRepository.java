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

    /**
     * Combined optional-filter search over active welfare benefits.
     *
     * <p>Each of {@code ctpvNm} (시도), {@code sggNm} (시군구), and {@code tag}
     * (crisis tag) is optional: a {@code null} value disables that filter. The
     * explicit {@code CAST(... AS TEXT)} wrappers avoid PostgreSQL bind-parameter
     * type-inference errors when a null is passed for a {@code text} comparison.
     *
     * <p>When {@code sggNm} is supplied it is <em>inclusive</em>: rows matching the
     * given 시군구 <strong>or</strong> having no 시군구 ({@code sgg_nm IS NULL}, i.e.
     * 시도-level / 전국 공통 제도) are returned, so a user's district-specific and
     * broader regional benefits show together rather than the former hiding the latter.
     */
    @Query(value =
        "SELECT * FROM welfare_benefits WHERE is_active = true " +
        "AND (CAST(:ctpvNm AS TEXT) IS NULL OR ctpv_nm = CAST(:ctpvNm AS TEXT)) " +
        "AND (CAST(:sggNm AS TEXT) IS NULL OR sgg_nm = CAST(:sggNm AS TEXT) OR sgg_nm IS NULL) " +
        "AND (CAST(:tag AS TEXT) IS NULL OR crisis_tags @> jsonb_build_array(CAST(:tag AS TEXT)))",
        countQuery =
        "SELECT COUNT(*) FROM welfare_benefits WHERE is_active = true " +
        "AND (CAST(:ctpvNm AS TEXT) IS NULL OR ctpv_nm = CAST(:ctpvNm AS TEXT)) " +
        "AND (CAST(:sggNm AS TEXT) IS NULL OR sgg_nm = CAST(:sggNm AS TEXT) OR sgg_nm IS NULL) " +
        "AND (CAST(:tag AS TEXT) IS NULL OR crisis_tags @> jsonb_build_array(CAST(:tag AS TEXT)))",
        nativeQuery = true)
    Page<WelfareBenefit> search(@Param("ctpvNm") String ctpvNm,
                                @Param("sggNm") String sggNm,
                                @Param("tag") String tag,
                                Pageable pageable);
}
