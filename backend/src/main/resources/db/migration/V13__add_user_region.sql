-- CrisFin V13: Add user region columns (시도/시군구) for region-based welfare recommendation

ALTER TABLE users ADD COLUMN region_ctpv VARCHAR(60);
ALTER TABLE users ADD COLUMN region_sgg  VARCHAR(60);
