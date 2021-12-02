package com.libs.globaldata.userdatabase;

import com.common.VectorOperations;

import java.util.function.BiFunction;
import java.util.function.Function;

public enum Metric {
    EUCLIDEAN, EUCLIDEAN_NORM, COSINE;

    public static BiFunction<float[], float[], Double> getDistanceFunction(Metric metric){
        switch (metric){
            case COSINE:
                return VectorOperations::cosineSimilarity;
            default:
                return VectorOperations::euclideanDistance;
        }
    }

    public static Function<float[], float[]> getNormalizationFunction(Metric metric){
        switch (metric){
            case EUCLIDEAN_NORM:
                return VectorOperations::l2Normalize;
            default:
                return (x) -> x;
        }
    }
}