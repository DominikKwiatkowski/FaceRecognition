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
import java.util.function.BiFunction;
import java.util.function.Function;

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

    // Enable saving database to internal device's storage
    // Default is true
    private boolean saveToFile;

    private final BiFunction<float[], float[], Double> distanceFunction;
    private final Function<float[], float[]> normalizationFuction;
    private final float threshold;

    public UserDatabase(Context context, String databaseName, int vectorLength, boolean loadOnCreation, Metric metric, float threshold) {
        this.databaseFile = new File(context.getFilesDir(), Tag + "_" + databaseName + ".json");
        Log.d(Tag, databaseFile.getAbsolutePath());
        this.id = databaseName;
        this.vectorLength = vectorLength;
        this.userDatabaseType = new TypeToken<Map<String, UserRecord>>() {
        }.getType();
        this.saveToFile = true;

        this.usersRecords = new HashMap<String, UserRecord>();

        this.distanceFunction = Metric.getDistanceFunction(metric);
        this.normalizationFuction = Metric.getNormalizationFunction(metric);
        this.threshold = threshold;

        if (loadOnCreation) {
            // Load Database on creation
            loadDatabase();
        }
    }

    /**
     * Enable database saving so that after every modification changes are also applied to the
     * copy from device's storage.
     */
    public void enableDatabaseSaving() {
        saveToFile = true;
    }

    /**
     * Disable database saving so that after every modification changes take place only in RAM
     * and won't be saved to device's storage.
     */
    public void disableDatabaseSaving() {
        saveToFile = false;
    }

    /**
     * Find closest record from the database. Algorithm and time complexity is dependent
     * on database type.
     *
     * @param vector of n-dimensions, for which the closest equivalent wil be found
     * @return closest UserRecord - null if not found
     */
    public UserRecord findClosestRecord(float[] vector) {
        if (vector == null) {
            return null;
        }
        if (validateVector(vector)) {
            UserRecord closestRecord = null;
            double minDist = Double.MAX_VALUE;

            for (String user : usersRecords.keySet()) {
                double prevMinDist = minDist;

                minDist = Math.min(minDist, distanceFunction.apply(
                        normalizationFuction.apply(vector),
                        normalizationFuction.apply(usersRecords.get(user).vector)));

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
     * Find closest record from the database taking threshold into account.
     *
     * @param vector of n-dimensions, for which the closest equivalent wil be found
     * @return closest UserRecord - null if not found
     */
    public UserRecord findClosestRecordAboveThreshold(float[] vector) {
        UserRecord result = findClosestRecord(vector);
        if(distanceFunction.apply(normalizationFuction.apply(vector), normalizationFuction.apply(result.vector)) <= threshold){
            return result;
        }
        return new UserRecord("?", null);
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
                // If user exists in database, update the record
                usersRecords.get(userRecord.username).correctVector(userRecord.vector);
            }

            // Serialize database immediately
            if (saveToFile) {
                saveDatabase();
            }
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
            if (saveToFile) {
                saveDatabase();
            }
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
        if (saveToFile) {
            saveDatabase();
        }
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
        if (saveToFile) {
            saveDatabase();
        }
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
     * Get the number of currently added Users.
     *
     * @return int Number of users
     */
    public int getNumberOfUsers() {
        return getUsersArray().length;
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
        if (!databaseFile.exists()) {
            Log.w(Tag + "_" + id, "Unable to load database. File not found");
            return;
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
    public void clear() {
        usersRecords.clear();

        if (databaseFile.exists()) {
            if (databaseFile.delete()) {
                Log.d(Tag + "_" + id, "Database file removed.");
            } else {
                throw new AssertionError("Cannot remove the database file");
            }
        }
    }
}
