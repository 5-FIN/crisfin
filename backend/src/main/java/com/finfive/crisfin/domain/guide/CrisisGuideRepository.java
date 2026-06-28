package com.finfive.crisfin.domain.guide;

import com.finfive.crisfin.domain.crisis.CrisisType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrisisGuideRepository extends JpaRepository<CrisisGuide, Long> {

    Optional<CrisisGuide> findByCrisisType(CrisisType crisisType);
}
