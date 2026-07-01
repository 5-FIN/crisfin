-- CrisFin V11: Add welfare region columns (시도/시군구) for region filtering

ALTER TABLE welfare_benefits ADD COLUMN ctpv_nm VARCHAR(60);
ALTER TABLE welfare_benefits ADD COLUMN sgg_nm  VARCHAR(60);

CREATE INDEX idx_welfare_benefits_ctpv ON welfare_benefits (ctpv_nm);
