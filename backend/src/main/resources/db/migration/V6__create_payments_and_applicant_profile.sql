-- Paid analysis: mock payment orders + per-user entitlements,
-- plus an applicant-profile snapshot on analysis_results for reinfer inheritance.

CREATE TABLE payment_orders (
    id         BIGSERIAL PRIMARY KEY,
    order_uid  VARCHAR(64)  NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL,
    amount     INTEGER      NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    paid_at    TIMESTAMP
);
CREATE INDEX idx_payment_orders_user ON payment_orders (user_id);

CREATE TABLE entitlements (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL UNIQUE,
    plan         VARCHAR(30)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    activated_at TIMESTAMP    NOT NULL DEFAULT now(),
    expires_at   TIMESTAMP
);

ALTER TABLE analysis_results ADD COLUMN applicant_profile_json jsonb;
