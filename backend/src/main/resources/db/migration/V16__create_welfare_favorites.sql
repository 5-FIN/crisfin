-- CrisFin V16: 복지 즐겨찾기 (user ↔ welfare_benefit)
CREATE TABLE welfare_favorites (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    welfare_benefit_id  BIGINT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_welfare_favorites UNIQUE (user_id, welfare_benefit_id)
);
CREATE INDEX idx_welfare_favorites_user ON welfare_favorites (user_id);
