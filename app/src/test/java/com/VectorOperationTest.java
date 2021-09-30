package com;

import com.common.VectorOperations;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Unit test for vector operations.
 */
public class VectorOperationTest {
    /**
     * Test of vector normalization by l2 norm.
     */
    @Test
    public void l2NormTest() {
        float[] array = {3, 4, 1, 2, 8, 6, 4, 1, 9, 0};
        float[] expected = {0.19867985F, 0.26490647F, 0.06622662F, 0.13245323F,
                0.52981293F, 0.3973597F, 0.26490647F, 0.06622662F,
                0.59603953F, 0};
        float[] result = VectorOperations.l2Normalize(array);
        assertArrayEquals(expected, result, 0.00001F);
    }

    /**
     * Test of euclidean distance between 2 vectors.
     */
    @Test
    public void euclideanDistanceTest() {
        float[] array = {3, 4, 1, 2, 8, 6, 4, 1, 9};
        float[] array1 = {5, 1, 4, 2, 1, 7, 40, 11, 2};
        double expected = 38.948684188;
        double result = VectorOperations.euclideanDistance(array, array1);
        assertEquals(expected, result, 0.00001);
    }

    /**
     * Test of cosine angle between 2 vectors.
     */
    @Test
    public void cosineAngleTest() {
        float[] array = {3, 4, 1, 2, 8, 6, 4, 1, 9};
        float[] array1 = {5, 1, 4, 2, 1, 7, 40, 11, 2};
        double expected = 0.5871814;
        double result = VectorOperations.cosineSimilarity(array, array1);
        assertEquals(expected, result, 0.00001);
    }
}