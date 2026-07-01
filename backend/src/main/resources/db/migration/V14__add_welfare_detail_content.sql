-- CrisFin V14: Add detail_content column for full welfare-service text (RAG ingest source)
ALTER TABLE welfare_benefits ADD COLUMN detail_content TEXT;
