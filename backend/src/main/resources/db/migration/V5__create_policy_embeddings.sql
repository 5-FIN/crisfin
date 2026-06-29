-- Hybrid RAG: policy embedding store backed by pgvector.
--
-- pgvector is an optional capability: if the extension is not available on the server we
-- skip the table entirely so the application still boots (RAG simply stays disabled, the
-- same graceful degradation as a missing OPENAI_API_KEY). When pgvector IS available the
-- table + ivfflat index are created exactly as the RAG repository expects.
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

        -- Approximate nearest-neighbour index for cosine distance (<=>).
        CREATE INDEX IF NOT EXISTS idx_policy_embeddings_vector
            ON policy_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

        -- Lookup by origin (source_type, source_ref).
        CREATE INDEX IF NOT EXISTS idx_policy_embeddings_source
            ON policy_embeddings (source_type, source_ref);
    ELSE
        RAISE NOTICE 'pgvector extension not available — skipping policy_embeddings; RAG disabled.';
    END IF;
END
$$;
