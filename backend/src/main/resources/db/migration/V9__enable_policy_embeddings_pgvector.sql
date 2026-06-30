-- RAG 활성화 재시도: V5가 적용될 당시 pgvector가 없던 DB(예: postgres:16-alpine)에서는
-- policy_embeddings 테이블이 생성되지 않은 채 V5가 "적용됨"으로 기록된다. 이후 DB 이미지를
-- pgvector 포함(pgvector/pgvector:pg16)으로 교체하면, 이 마이그레이션이 다시 한 번
-- 조건부로 테이블/인덱스를 생성한다. 모든 구문은 IF NOT EXISTS라 V5에서 이미 만든 경우 무해.
--
-- pgvector가 여전히 없으면(순정 postgres 유지) 조용히 스킵 — 부팅·기존 동작에 영향 없음.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
        CREATE EXTENSION IF NOT EXISTS vector;

        CREATE TABLE IF NOT EXISTS policy_embeddings (
            id          BIGSERIAL PRIMARY KEY,
            source_type VARCHAR(20)  NOT NULL,
            source_ref  VARCHAR(64)  NOT NULL,
            crisis_tags TEXT         NOT NULL DEFAULT '',
            content     TEXT         NOT NULL,
            embedding   vector(1536),
            created_at  TIMESTAMP    NOT NULL DEFAULT now()
        );

        CREATE INDEX IF NOT EXISTS idx_policy_embeddings_vector
            ON policy_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

        CREATE INDEX IF NOT EXISTS idx_policy_embeddings_source
            ON policy_embeddings (source_type, source_ref);
    ELSE
        RAISE NOTICE 'pgvector extension not available — policy_embeddings still skipped; RAG disabled.';
    END IF;
END
$$;
