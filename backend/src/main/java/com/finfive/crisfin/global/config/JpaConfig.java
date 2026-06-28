package com.finfive.crisfin.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Provides a thread-safe {@link JPAQueryFactory} backed by a Spring-managed
     * {@link EntityManager} proxy. QueryDSL repository classes can simply
     * {@code @RequiredArgsConstructor}-inject this bean.
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
