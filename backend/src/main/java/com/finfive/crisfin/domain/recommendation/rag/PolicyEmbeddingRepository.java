package com.finfive.crisfin.domain.recommendation.rag;

import com.finfive.crisfin.domain.crisis.CrisisType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-SQL access to the {@code policy_embeddings} pgvector store.
 *
 * <p>The {@code vector} column type cannot be mapped by JPA, so this repository uses the
 * {@link EntityManager} with native queries and {@code CAST(:emb AS vector)} for both
 * writes and cosine-distance ({@code <=>}) nearest-neighbour search. Vectors are passed as
 * pgvector text literals produced by {@link VectorLiterals#toLiteral(float[])}.</p>
 */
@Repository
public class PolicyEmbeddingRepository {

    @PersistenceContext
    private EntityManager em;

    /** Removes every embedding row (used before a full reindex). */
    @Transactional
    public void deleteAll() {
        em.createNativeQuery("DELETE FROM policy_embeddings").executeUpdate();
    }

    /**
     * Inserts one embedding row.
     *
     * @param sourceType    e.g. {@code "WELFARE"} / {@code "GUIDE"}
     * @param sourceRef     identifier within the source
     * @param crisisTags    comma-wrapped tag string, e.g. {@code ",HOSPITALIZATION,ACCIDENT,"}
     * @param content       chunk text
     * @param vectorLiteral pgvector literal, e.g. {@code [0.1,0.2,...]}
     */
    @Transactional
    public void insert(String sourceType,
                       String sourceRef,
                       String crisisTags,
                       String content,
                       String vectorLiteral) {
        em.createNativeQuery(
                        "INSERT INTO policy_embeddings " +
                        "(source_type, source_ref, crisis_tags, content, embedding) " +
                        "VALUES (:sourceType, :sourceRef, :crisisTags, :content, CAST(:emb AS vector))")
                .setParameter("sourceType", sourceType)
                .setParameter("sourceRef", sourceRef)
                .setParameter("crisisTags", crisisTags)
                .setParameter("content", content)
                .setParameter("emb", vectorLiteral)
                .executeUpdate();
    }

    /**
     * Returns the {@code topK} nearest chunks to the query vector by cosine distance.
     *
     * <p>When {@code crisisType} is non-null, results are restricted to chunks whose
     * comma-wrapped {@code crisis_tags} contain {@code ,TYPE,}.</p>
     */
    @Transactional(readOnly = true)
    public List<RetrievedPolicy> searchNearest(String vectorLiteral, CrisisType crisisType, int topK) {
        boolean filterByType = crisisType != null;

        StringBuilder sql = new StringBuilder(
                "SELECT source_type, source_ref, content, " +
                "(embedding <=> CAST(:emb AS vector)) AS distance " +
                "FROM policy_embeddings ");
        if (filterByType) {
            sql.append("WHERE crisis_tags LIKE :tag ");
        }
        sql.append("ORDER BY embedding <=> CAST(:emb AS vector)");

        Query query = em.createNativeQuery(sql.toString())
                .setParameter("emb", vectorLiteral)
                .setMaxResults(topK);
        if (filterByType) {
            query.setParameter("tag", "%," + crisisType.name() + ",%");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<RetrievedPolicy> results = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            results.add(new RetrievedPolicy(
                    (String) row[0],
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).doubleValue()
            ));
        }
        return results;
    }
}
