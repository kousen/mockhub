-- Remove unused vector store table.
-- Embeddings were scaffolded but never populated — all search
-- uses PostgreSQL tsvector full-text search instead.
DROP INDEX IF EXISTS idx_vector_store_embedding;
DROP TABLE IF EXISTS vector_store;
