-- CrisFin V17: 분석결과 공유 링크 토큰
ALTER TABLE analysis_results ADD COLUMN share_token VARCHAR(64);
CREATE UNIQUE INDEX uq_analysis_results_share_token ON analysis_results (share_token);
