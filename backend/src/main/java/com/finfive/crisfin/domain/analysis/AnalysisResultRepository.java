package com.finfive.crisfin.domain.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link AnalysisResult} entities.
 */
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    Page<AnalysisResult> findByUserId(Long userId, Pageable pageable);
}
