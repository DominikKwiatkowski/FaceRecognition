package com.libs.globaldata.userdatabase;

import android.content.Context;
import android.util.Log;

import com.R;
import com.common.VectorOperations;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class UserDatabase {
    private final String Tag = "Database";

    // Path of database file
    private final File databaseFile;

    // Database identifier, to ensure safe loading
    private final String id;

    // Vector target length, to validate database inputs
    private final int vectorLength;

    // Database type stored for json serialization
    private final Type userDatabaseType;
    private Map<String, UserRecord> usersRecords;

    public UserDatabase(Context context, String databaseName, int vectorLength) {
        this.databaseFile = new File(context.getFilesDir(), Tag + "_" + databaseName + ".json");
        Log.d(Tag, databaseFile.getAbsolutePath());
        this.id = databaseName;
        this.vectorLength = vectorLength;
        this.userDatabaseType = new TypeToken<Map<String, UserRecord>>() {
        }.getType();

        usersRecords = new HashMap<String, UserRecord>();

        // Load Database on creation
        loadDatabase();
    }

    /**
     * Find closest record from the database. Algorithm and time complexity is dependent
     * on database type.
     *
     * @param vector of n-dimensions, for which the closest equivalent wil be found
     * @return closest UserRecord
     */
    public UserRecord findClosestRecord(float[] vector) {
        if (validateVector(vector)) {
            UserRecord closestRecord = null;
            double minDist = Double.MAX_VALUE;

            for (String user : usersRecords.keySet()) {
                double prevMinDist = minDist;
                minDist = Math.min(minDist, VectorOperations.cosineSimilarity(
                        VectorOperations.l2Normalize(vector),
                        VectorOperations.l2Normalize(usersRecords.get(user).vector)));

                if (minDist < prevMinDist) {
                    closestRecord = usersRecords.get(user);
                }
            }

            return closestRecord;
        } else {
            throw new AssertionError("Incorrect vector length");
        }
    }

    /**
     * Validate given vector's data to check, if it matches database characteristics:
     * validate vector's size in relation to database's vector size
     * validate vector values to make sure that there are no infinities and nans in it.
     *
     * @param vector to validate
     * @return True if userRecord is valid, False otherwise
     */
    private boolean validateVector(float[] vector) {

        if (vector.length != vectorLength) {
            Log.e(Tag + "_" + id, "Invalid vector's length!");
            return false;
        }

        // Validate vector's values
        for (int i = 0; i < vector.length; i++) {
            if (!Float.isFinite(vector[i])) {
                Log.e(Tag + "_" + id, "Vector value not finite!");
                return false;
            }
        }

        return true;
    }


    /**
     * Add new UserRecord to the database.
     *
     * @param userRecord to add to database
     */
    public void addUserRecord(UserRecord userRecord) {
        if (validateVector(userRecord.vector)) {
            if (!usersRecords.containsKey(userRecord.username)) {
                // If user doesn't exist in database, insert the record
                usersRecords.put(userRecord.username, userRecord);
            } else {
                // If user exists in database, correct the record
                usersRecords.get(userRecord.username).correctVector(userRecord.vector);
            }

            // Serialize database immediately
            // TODO: Later on it might be reasonable to save database on application closure (faster)
            saveDatabase();
        } else {
            throw new AssertionError("Invalid vector.");
        }
    }

    /**
     * Add UserRecord to the database. If user already exists, override it's data.
     *
     * @param userRecord to add to database
     */
    public void forceAddUserRecord(UserRecord userRecord) {
        if (validateVector(userRecord.vector)) {
            usersRecords.put(userRecord.username, userRecord);

            // Serialize database immediately
            // TODO: Later on it might be reasonable to save database on application closure (faster)
            saveDatabase();
        } else {
            throw new AssertionError("Invalid vector.");
        }
    }

    /**
     * Remove userRecord by user name.
     * Grants that there will be no user with given user name.
     *
     * @param userName of the user to remove
     */
    public void removeUserRecord(String userName) {
        usersRecords.remove(userName);

        // Serialize database immediately
        // TODO: Later on it might be reasonable to save database on application closure (faster)
        saveDatabase();
    }

    /**
     * Remove userRecord by UserRecord object.
     * Grants that there will be no user with given user name.
     *
     * @param userRecord to remove
     */
    public void removeUserRecord(UserRecord userRecord) {
        usersRecords.remove(userRecord.username);

        // Serialize database immediately
        // TODO: Later on it might be reasonable to save database on application closure (faster)
        saveDatabase();
    }

    /**
     * Get userRecord by user name.
     *
     * @param userName of the searched user
     * @return UserRecord if found. Null if user does not exist
     */
    public UserRecord getUserRecord(String userName) {
        return usersRecords.get(userName);
    }

    /**
     * Get vector by user name.
     *
     * @param userName of the searched user
     * @return vector if found. Null if user does not exist
     */
    public float[] getUserVector(String userName) {
        return usersRecords.get(userName).vector;
    }

    /**
     * Get list of all users.
     *
     * @return array of users names
     */
    public String[] getUsersArray() {
        // Receive set of keys and convert it to array
        Set<String> usersSet = usersRecords.keySet();

        return usersSet.toArray(new String[0]);
    }

    /**
     * Deserialize user database.
     */
    public void loadDatabase() {
        if (databaseFile.exists()) {
            Log.w(Tag + "_" + id, "Unable to load database. File not found");
        }

        StringBuilder databaseString = new StringBuilder();

        // Read the content of file
        try (FileInputStream fileInputStream = new FileInputStream(databaseFile)) {
            int ch = fileInputStream.read();

            while (ch != -1) {
                databaseString.append((char) ch);
                ch = fileInputStream.read();
            }
        } catch (IOException e) {
            Log.e(Tag + "_" + id, "Unable to load database. Cannot properly read the file");
            e.printStackTrace();
            return;
        }

        // Deserialize database string
        Gson gson = new Gson();
        JsonObject databaseJson = gson.fromJson(databaseString.toString(), JsonObject.class);

        String loadedId = databaseJson.get("Id").getAsString();
        int loadedVectorLength = databaseJson.get("VectorLength").getAsInt();
        String serializedUserRecords = databaseJson.get("UserRecords").getAsString();

        // Validate database
        // TODO: Later it might be better to throw exception here (in order to display dialog box or sth)
        assertEquals("Wrong type of database", id, loadedId);
        assertEquals("Wrong size of database", vectorLength, loadedVectorLength);

        // Load users records
        usersRecords.clear();
        usersRecords = gson.fromJson(serializedUserRecords, userDatabaseType);

        Log.d(Tag + "_" + id, "Database file loaded");
    }

    /**
     * Serialize user database.
     */
    public void saveDatabase() {

        // Serialize userRecords to Json
        Gson gson = new Gson();
        String serializedUserRecords = gson.toJson(usersRecords);

        // Complete Json object with database header data
        JsonObject databaseJson = new JsonObject();
        databaseJson.addProperty("Id", id);
        databaseJson.addProperty("VectorLength", vectorLength);
        databaseJson.addProperty("UserRecords", serializedUserRecords);

        // Write json object to file
        String databaseString = databaseJson.toString();
        try (FileOutputStream fos = new FileOutputStream(databaseFile)) {
            fos.write(databaseString.getBytes());
        } catch (IOException e) {
            Log.e(Tag + "_" + id, "Cannot save database");
            e.printStackTrace();
            throw new AssertionError("Cannot save database");
        }

        Log.d(Tag + "_" + id, "Database file saved");
    }

    /**
     * Removes database file if created.
     */
    public void clearFile() {
        if (databaseFile.exists()) {
            if (databaseFile.delete()) {
                Log.d(Tag + "_" + id, "Database file removed.");
            } else {
                throw new AssertionError("Cannot remove the database file");
            }
        }
    }

    /**
     * Load sample database from resource files.
     */
    public void loadSampleDatabase(Context context) {
        // Load sample_database.json from resources to a String
        String resourceString;
        InputStream is = context.getResources().openRawResource(R.raw.sample_database);
        int size = 0;

        try {
            size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            resourceString = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(Tag + "_" + id, "Cannot load sample database");
            e.printStackTrace();
            throw new AssertionError("Cannot load sample database");
        }

        // Deserialize database string
        Gson gson = new Gson();
        JsonObject databaseJson = gson.fromJson(resourceString, JsonObject.class);

        String loadedId = databaseJson.get("Id").getAsString();
        int loadedVectorLength = databaseJson.get("VectorLength").getAsInt();
        String serializedUserRecords = databaseJson.get("UserRecords").getAsString();

        // Validate database
        // TODO: Later it might be better to throw exception here (in order to display dialog box or sth)
        assertEquals("Wrong type of database", id, loadedId);
        assertEquals("Wrong size of database", vectorLength, loadedVectorLength);

        // Load users records
        usersRecords.clear();
        usersRecords = gson.fromJson(serializedUserRecords, userDatabaseType);

        Log.d(Tag + "_" + id, "Sample database file loaded form resources");
    }
}
