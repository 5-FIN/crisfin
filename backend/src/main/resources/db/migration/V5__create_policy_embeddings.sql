-- Hybrid RAG: policy embedding store backed by pgvector.
-- Requires the pgvector extension to be available on the PostgreSQL server.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE policy_embeddings (
    id          BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(20)  NOT NULL,
    source_ref  VARCHAR(64)  NOT NULL,
    crisis_tags TEXT         NOT NULL DEFAULT '',
    content     TEXT         NOT NULL,
    embedding   vector(1536),
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- Approximate nearest-neighbour index for cosine distance (<=>).
CREATE INDEX idx_policy_embeddings_vector
    ON policy_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Lookup by origin (source_type, source_ref).
CREATE INDEX idx_policy_embeddings_source
    ON policy_embeddings (source_type, source_ref);
