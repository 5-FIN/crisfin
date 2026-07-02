package com.finfive.crisfin.domain.welfare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

/** 로그인 사용자가 즐겨찾기한 복지 제도. (user ↔ welfare_benefit) */
@Entity
@Table(name = "welfare_favorites")
@Getter
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
@Builder
public class WelfareFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "welfare_benefit_id")
    private Long welfareBenefitId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 즐겨찾기 생성용 팩토리 — createdAt을 현재 시각으로 채운다. */
    public static WelfareFavorite of(Long userId, Long welfareBenefitId) {
        return WelfareFavorite.builder()
                .userId(userId)
                .welfareBenefitId(welfareBenefitId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
