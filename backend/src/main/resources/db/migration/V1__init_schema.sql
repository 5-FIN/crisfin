-- CrisFin V1: Initial Schema
-- PostgreSQL 16

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    nickname        VARCHAR(100),
    persona_type    VARCHAR(50),                          -- e.g. OFFICE_WORKER, SELF_EMPLOYED
    role            VARCHAR(50)     NOT NULL DEFAULT 'USER',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ
);

CREATE INDEX idx_users_email       ON users (email);
CREATE INDEX idx_users_role        ON users (role);
CREATE INDEX idx_users_is_active   ON users (is_active);

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token       VARCHAR(512)    NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- ============================================================
-- CRISIS GUIDES
-- ============================================================
CREATE TABLE crisis_guides (
    id              BIGSERIAL       PRIMARY KEY,
    crisis_type     VARCHAR(50)     NOT NULL UNIQUE,      -- HOSPITALIZATION, ACCIDENT, etc.
    title           VARCHAR(255)    NOT NULL,
    coaching_prompt TEXT,
    key_rules       JSONB           NOT NULL DEFAULT '[]'::JSONB,
    source_laws     JSONB           NOT NULL DEFAULT '[]'::JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crisis_guides_crisis_type ON crisis_guides (crisis_type);

-- ============================================================
-- MOCK MYDATA PROFILES
-- ============================================================
CREATE TABLE mock_mydata_profiles (
    id              BIGSERIAL       PRIMARY KEY,
    persona         VARCHAR(50)     NOT NULL UNIQUE,      -- OFFICE_WORKER, FREELANCER, etc.
    financial_data  JSONB           NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mock_mydata_profiles_persona ON mock_mydata_profiles (persona);

-- ============================================================
-- WELFARE BENEFITS
-- ============================================================
CREATE TABLE welfare_benefits (
    id                  BIGSERIAL       PRIMARY KEY,
    external_service_id VARCHAR(50)     UNIQUE,           -- e.g. WF001
    service_name        VARCHAR(255)    NOT NULL,
    summary             TEXT,
    target_description  TEXT,
    selection_criteria  TEXT,
    apply_method        TEXT,
    apply_url           VARCHAR(512),
    ministry_name       VARCHAR(100),
    contact             VARCHAR(100),
    crisis_tags         JSONB           NOT NULL DEFAULT '[]'::JSONB,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    last_synced_at      TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_welfare_benefits_external_service_id ON welfare_benefits (external_service_id);
CREATE INDEX idx_welfare_benefits_crisis_tags         ON welfare_benefits USING GIN (crisis_tags);
CREATE INDEX idx_welfare_benefits_is_active           ON welfare_benefits (is_active);

-- ============================================================
-- ANALYSIS RESULTS
-- ============================================================
CREATE TABLE analysis_results (
    id                      BIGSERIAL       PRIMARY KEY,
    user_id                 BIGINT          REFERENCES users (id) ON DELETE SET NULL,
    crisis_type             VARCHAR(50)     NOT NULL,
    situation_description   TEXT,
    input_mydata_json       JSONB,
    result_json             JSONB,
    llm_provider            VARCHAR(50),
    tokens_used             INT             NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analysis_results_user_id     ON analysis_results (user_id);
CREATE INDEX idx_analysis_results_crisis_type ON analysis_results (crisis_type);
CREATE INDEX idx_analysis_results_created_at  ON analysis_results (created_at DESC);
