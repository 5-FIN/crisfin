-- Rule-engine threshold tables data-ised from hardcoded BenefitRuleEngine constants.
-- One row per (category, bracket_key). Values must stay 1:1 with the rule engine's
-- previous Java constants so deterministic amount estimates are unchanged.

CREATE TABLE benefit_criteria (
    id           BIGSERIAL     PRIMARY KEY,
    category     VARCHAR(40)   NOT NULL,   -- MEDIAN_INCOME, EMERGENCY_SUPPORT, UI_DAILY, EMERGENCY_ASSET_CAP, OOP_CAP, CARE_LIMIT
    bracket_key  VARCHAR(20)   NOT NULL,   -- 가구원수 "1".."6"/"EXTRA", 등급 "1".."5", "MIN"/"MAX"/"CAP", 또는 OOP 소득상한
    amount       BIGINT        NOT NULL,
    note         VARCHAR(255),
    CONSTRAINT uq_benefit_criteria UNIQUE (category, bracket_key)
);

-- 2024 기준 중위소득 (월, 원) — index by household size
INSERT INTO benefit_criteria (category, bracket_key, amount, note) VALUES
    ('MEDIAN_INCOME', '1',     2228445, '2024 기준 중위소득 1인'),
    ('MEDIAN_INCOME', '2',     3682609, '2024 기준 중위소득 2인'),
    ('MEDIAN_INCOME', '3',     4714657, '2024 기준 중위소득 3인'),
    ('MEDIAN_INCOME', '4',     5729913, '2024 기준 중위소득 4인'),
    ('MEDIAN_INCOME', '5',     6695735, '2024 기준 중위소득 5인'),
    ('MEDIAN_INCOME', '6',     7618369, '2024 기준 중위소득 6인'),
    ('MEDIAN_INCOME', 'EXTRA',  922634, '7인 이상 1인당 추가액');

-- 긴급복지 생계지원 (월, 원) — index by household size
INSERT INTO benefit_criteria (category, bracket_key, amount, note) VALUES
    ('EMERGENCY_SUPPORT', '1',      713100, '긴급복지 생계지원 1인'),
    ('EMERGENCY_SUPPORT', '2',     1178400, '긴급복지 생계지원 2인'),
    ('EMERGENCY_SUPPORT', '3',     1508600, '긴급복지 생계지원 3인'),
    ('EMERGENCY_SUPPORT', '4',     1841700, '긴급복지 생계지원 4인'),
    ('EMERGENCY_SUPPORT', '5',     2072101, '긴급복지 생계지원 5인'),
    ('EMERGENCY_SUPPORT', '6',     2348359, '긴급복지 생계지원 6인'),
    ('EMERGENCY_SUPPORT', 'EXTRA',  230000, '7인 이상 1인당 추가액');

-- 긴급복지 금융재산 기준 (원) — 단순화한 상한
INSERT INTO benefit_criteria (category, bracket_key, amount, note) VALUES
    ('EMERGENCY_ASSET_CAP', 'CAP', 100000000, '긴급복지 금융재산 상한');

-- 실업급여 1일 구직급여 상·하한 (2024, 원)
INSERT INTO benefit_criteria (category, bracket_key, amount, note) VALUES
    ('UI_DAILY', 'MAX', 66000, '1일 구직급여 상한'),
    ('UI_DAILY', 'MIN', 63104, '1일 구직급여 하한');

-- 본인부담상한 (연, 원) — bracket_key = 월소득 상한, amount = 연 본인부담상한
INSERT INTO benefit_criteria (category, bracket_key, amount, note) VALUES
    ('OOP_CAP', '1500000',    870000, '1분위'),
    ('OOP_CAP', '2500000',   1080000, '2~3분위'),
    ('OOP_CAP', '3500000',   1550000, '4~5분위'),
    ('OOP_CAP', '5000000',   2890000, '6~7분위'),
    ('OOP_CAP', '7000000',   3600000, '8분위'),
    ('OOP_CAP', '9000000',   4430000, '9분위'),
    ('OOP_CAP', '9999999999', 5980000, '10분위 (그 이상 전부)');

-- 장기요양 등급별 재가급여 월 한도 (2024, 원)
INSERT INTO benefit_criteria (category, bracket_key, amount, note) VALUES
    ('CARE_LIMIT', '1', 2069900, '장기요양 1등급'),
    ('CARE_LIMIT', '2', 1869600, '장기요양 2등급'),
    ('CARE_LIMIT', '3', 1455800, '장기요양 3등급'),
    ('CARE_LIMIT', '4', 1341800, '장기요양 4등급'),
    ('CARE_LIMIT', '5', 1151600, '장기요양 5등급');
