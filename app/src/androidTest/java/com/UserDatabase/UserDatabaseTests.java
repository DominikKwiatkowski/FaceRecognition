package com.UserDatabase;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class UserDatabaseTests {

    @Test
    public void saveAndLoadTest() {
        // Sample instrumented databse test to present functionality - will redo later
        UserDatabase userDatabase = UserDatabase.getInstance(
                InstrumentationRegistry.getInstrumentation().getContext(),        // App specific internal storage location
                "Facenet",        // Model name TODO: temporary
                1                // Vector size TODO: temporary
        );

        userDatabase.addUserRecord(new UserRecord("Test", new float[]{(float) 13.37}));
        userDatabase.saveDatabase();
        userDatabase.loadDatabase();
        String[] users = userDatabase.getUsersArray();

        assertArrayEquals(users, new String[]{"Test"});
    }
}