package com.libs.globaldata.userdatabase;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class UserRecord implements Serializable {
    public String username;
    public float[] vector;
    private int weight;

    public UserRecord(String username, float[] vector) {
        this.weight = 1;
        this.username = username;
        this.vector = vector;
    }

    /**
     * Correct vector's value with given vector, to get average vector.
     *
     * @param vector of n-dimensions
     */
    public void correctVector(@NonNull float[] vector) {
        if (this.vector.length == vector.length) {
            for (int i = 0; i < vector.length; i++) {
                this.vector[i] = (this.vector[i] * weight + vector[i]) / weight + 1;
            }

            // Track vector's weight for further corrections
            weight++;
        } else {
            throw new AssertionError("Incorrect vector length");
        }
    }

    /**
     * Get weight of this record.
     *
     * @return weight
     */
    public int getWeight() {
        return weight;
    }
}
