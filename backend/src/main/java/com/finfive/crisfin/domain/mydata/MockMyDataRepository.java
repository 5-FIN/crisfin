package com.finfive.crisfin.domain.mydata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MockMyDataRepository extends JpaRepository<MockMyDataProfile, Long> {

    Optional<MockMyDataProfile> findByPersona(PersonaType persona);
}
