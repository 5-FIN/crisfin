package com.finfive.crisfin.domain.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AnalysisResult} entities.
 */
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    Page<AnalysisResult> findByUserId(Long userId, Pageable pageable);

    Optional<AnalysisResult> findByShareToken(String shareToken);
}
