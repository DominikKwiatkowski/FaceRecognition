package com.common;

import java.lang.Math;

public final class VectorOperations {

    /**
     * Normalize vector by it's l2 norm (unit vector).
     *       v
     * v  = ───
     *  n   ‖v‖
     *
     * @param vector vector to normalize
     * @return normalized vector (unit vector)
     * @throws NullPointerException when null vector passed
     */
    static public float[] l2Normalize(float[] vector) {
        if (vector == null)
            throw new NullPointerException("Null vector passed");

        float[] normalizedVector = new float[vector.length];
        double div = 0;

        for (float item : vector)
            div += item * item;

        div = Math.sqrt(div);

        for (int i = 0; i < vector.length; i++)
            normalizedVector[i] = (float) (vector[i] / div);

        return normalizedVector;
    }

    /**
     * Calculate euclidean distance between two vectors.
     *                 ________________________________________________
     *                ╱           2              2                    2
     * d(v1, v2) =   ╱ ⎛v1  - v2 ⎞  + ⎛v1  - v2 ⎞  + ... + ⎛v1  - v2 ⎞
     *             ╲╱  ⎝  1     1⎠    ⎝  2     2⎠          ⎝  n     n⎠
     *
     * @param vector1 first vector
     * @param vector2 second vector
     * @return euclidean distance between the two vectors
     * @throws NullPointerException when null vector(s) passed
     */
    static public double euclideanDistance(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null)
            throw new NullPointerException("Null vector(s) passed");

        double distance = 0;

        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            distance += diff * diff;
        }

        return Math.sqrt(distance);
    }

    /**
     * Calculate cosine distance between vectors.
     *
     * D  = (v1, v2) = 1 - S (v1, v2)
     *  c                   c
     *
     *                 v1 ⋅ v2
     * S (v1, v2) =  ───────────
     *  c            ‖v1‖ ⋅ ‖v2‖
     * @param vector1 first vector
     * @param vector2 second vector
     * @return cosine distance between the two vectors
     * @throws NullPointerException when null vector(s) passed
     */
    static public double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null)
            throw new NullPointerException("Null vector(s) passed");

        double a = 0, b = 0, c = 0;

        for (int i = 0; i < vector1.length; i++) {
            a += vector1[i] * vector2[i];
            b += vector1[i] * vector1[i];
            c += vector2[i] * vector2[i];
        }

        b = Math.sqrt(b);
        c = Math.sqrt(c);
        return 1 - (a / (b * c));
    }

}
