package com.UserDatabase;

import android.content.Context;

import java.io.File;

public class UserDatabaseList extends UserDatabase {

    public UserDatabaseList(File appSpecFilesDir, String databaseName, int vectorLength, Context appContext) {
        super(
            new File(appSpecFilesDir, "database-" + databaseName + ".json"),
            databaseName,
            vectorLength
        );
    }

    @Override
    UserRecord findClosestRecord(float[] vector) {
        // TODO: Use Maciek's algorithm to find closest vector

        return null;
    }
}
