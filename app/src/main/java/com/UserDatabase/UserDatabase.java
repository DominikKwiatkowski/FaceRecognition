package com.UserDatabase;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public abstract class UserDatabase {
    protected final String LogTag = "Database";

    // Path of database file
    private final File Filepath;

    // Database identifier, to ensure safe loading
    private final String Id;
    // Vector target length, to validate database inputs
    private final int VectorLength;

    // protected ArrayList<UserRecord> usersRecords;
    protected Map<String, float[]> usersRecords;


    UserDatabase(File filepath, String id, int vectorLength) {
        this.Filepath = filepath;
        this.Id = id;
        this.VectorLength = vectorLength;

        usersRecords = new HashMap<String, float[]>();

        // Load Database on creation
        loadDatabase();
    }

    /**
     * Find closest record from the database. Algorithm and time complexity is dependent
     * on database type.
     *
     * @param vector of n-dimensions, for which the closest equivalent wil be found.
     * @return closest UserRecord.
     */
    abstract UserRecord findClosestRecord(float[] vector);

    /**
     * Add new UserRecord to the database.
     *
     * @param userRecord to add to database.
     * @return True if record was added. False if user already exists.
     */
    public boolean addUserRecord(UserRecord userRecord) {
        // Check correctness of vector length
        if(userRecord.vector.length == VectorLength){

            // Check if user already exists in database
            if (!usersRecords.containsKey(userRecord.username)) {
                usersRecords.put(userRecord.username, userRecord.vector);

                // Serialize database immediately
                // TODO: Later on it might be reasonable to save database on application closure (faster)
                saveDatabase();

                return true;
            }
        }
        else{
            throw new AssertionError("Incorrect vector length");
        }

        return false;
    }

    /**
     * Add UserRecord to the database. If user already exists, override it's data.
     *
     * @param userRecord to add to database.
     */
    public void forceAddUserRecord(UserRecord userRecord) {
        // Check correctness of vector length
        if(userRecord.vector.length == VectorLength){
            usersRecords.put(userRecord.username, userRecord.vector);

            // Serialize database immediately
            // TODO: Later on it might be reasonable to save database on application closure (faster)
            saveDatabase();
        }
        else {
            throw new AssertionError("Incorrect vector length");
        }
    }

    /**
     * Remove userRecord by user name.
     * Grants that there will be no user with given user name.
     *
     * @param userName of the user to remove.
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
     * @param userRecord to remove.
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
     * @param userName of the searched user.
     * @return UserRecord if found. Null if user does not exist.
     */
    public UserRecord getUserRecord(String userName) {
        float[] vector = usersRecords.get(userName);

        // If vector was null, no user of given name exists
        if (vector == null) {
            return null;
        } else {
            return new UserRecord(userName, vector);
        }
    }

    /**
     * Get vector by user name.
     *
     * @param userName of the searched user.
     * @return vector if found. Null if user does not exist.
     */
    public float[] getUserVector(String userName) {
        return usersRecords.get(userName);
    }

    /**
     * Get list of all users.
     *
     * @return array of users names.
     */
    public String[] getUsersArray() {
        // Receive set of keys and convert it to array
        Set<String> usersSet = usersRecords.keySet();

        return usersSet.toArray(new String[0]);
    }

    /**
     * Deserialize user database.
     *
     */
    public void loadDatabase() {
        StringBuilder databaseString = new StringBuilder();

        // Read the content of file
        try (FileInputStream fileInputStream = new FileInputStream(Filepath)) {
            int ch = fileInputStream.read();

            while (ch != -1) {
                databaseString.append((char) ch);
                ch = fileInputStream.read();
            }
        } catch (IOException e) {
            Log.e(LogTag, "Cannot load database");
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
        assertEquals("Wrong type of database", Id, loadedId);
        assertEquals("Wrong size of database", VectorLength, loadedVectorLength);

        // Load users records
        usersRecords.clear();
        Type userDatabaseType = new TypeToken<Map<String, float[]>>() {}.getType();
        usersRecords = gson.fromJson(serializedUserRecords, userDatabaseType);

        Log.d(LogTag, "Database file successfully loaded");
    }

    /**
     * Serialize user database.
     *
     */
    public void saveDatabase() {

         // Serialize userRecords to Json
        Gson gson = new Gson();
        String serializedUserRecords = gson.toJson(usersRecords);

        // Complete Json object with database header data
        JsonObject databaseJson = new JsonObject();
        databaseJson.addProperty("Id", Id);
        databaseJson.addProperty("VectorLength", VectorLength);
        databaseJson.addProperty("UserRecords", serializedUserRecords);

        // Write json object to file
        String databaseString = databaseJson.toString();
        try(FileOutputStream fos = new FileOutputStream(Filepath)) {
            fos.write(databaseString.getBytes());
        } catch (IOException e) {
            Log.e(LogTag, "Cannot save database");
            e.printStackTrace();
            return;
        }

        Log.d(LogTag, "Database file successfully saved");
    }

    /**
     * Temporary function to test database functionality.
     *
     */
    public void generateDatabase() {
        // Erase current database
        usersRecords.clear();

        // Generate sample vector
        float[] sampleVector = new float[VectorLength];
        Random r = new Random();
        for(int i = 0; i < VectorLength; i++){
            sampleVector[i] = r.nextFloat();
        }

        // Add sample data
        usersRecords.put("Kuba", sampleVector);
        usersRecords.put("Maciej", sampleVector);
        usersRecords.put("Dominik", sampleVector);

        // Save database in internal memory
        saveDatabase();
    }
}
