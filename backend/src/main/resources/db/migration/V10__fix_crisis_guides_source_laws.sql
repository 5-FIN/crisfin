-- 무료 길라잡이 500 버그 수정.
--
-- V2 시드가 crisis_guides.source_laws를 객체 배열({law, article, description})로 넣었는데,
-- 엔티티 CrisisGuide.sourceLaws와 GuideResponse.sourceLaws, 프론트 타입은 모두 List<String>이다.
-- 그래서 Hibernate가 jsonb를 List<String>으로 역직렬화하지 못해 GET /api/v1/guide/{type}가
-- 500(Could not deserialize ... List<String>)을 반환했다. (CI는 flyway off + H2라 미검출.)
--
-- 각 객체를 "법령 조문 — 설명" 형태의 문자열로 변환해 모델과 일치시킨다.
-- 이미 문자열 배열인 경우(재실행/신규 DB에서 이 마이그레이션 재적용 등)에는 가드로 건너뛴다.
UPDATE crisis_guides
SET source_laws = (
    SELECT jsonb_agg(
        (e ->> 'law')
        || COALESCE(' ' || (e ->> 'article'), '')
        || COALESCE(' — ' || (e ->> 'description'), '')
    )
    FROM jsonb_array_elements(source_laws) AS e
)
WHERE jsonb_typeof(source_laws -> 0) = 'object';
