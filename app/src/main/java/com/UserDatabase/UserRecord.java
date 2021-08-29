package com.UserDatabase;

public class UserRecord {
    public String username;
    public float[] vector;

    UserRecord(String username, float[] vector) {
        this.username = username;
        this.vector = vector;
    }
}
