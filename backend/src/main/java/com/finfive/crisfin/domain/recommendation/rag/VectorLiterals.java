package com.finfive.crisfin.domain.recommendation.rag;

/**
 * Helpers for converting between Java vectors and pgvector text literals.
 *
 * <p>pgvector accepts a vector literal such as {@code [0.1,0.2,0.3]} which is then
 * {@code CAST(... AS vector)} inside native SQL. JPA cannot map the {@code vector} type
 * directly, so all persistence goes through these string literals.</p>
 */
public final class VectorLiterals {

    private VectorLiterals() {
    }

    /** Converts a float vector into a pgvector literal, e.g. {@code [0.1,0.2,0.3]}. */
    public static String toLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
