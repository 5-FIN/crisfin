package com.finfive.crisfin.domain.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link AnalysisResult} entities.
 */
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
}
