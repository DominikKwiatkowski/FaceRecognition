package com.UserDatabase;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.Log;
import com.R;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import common.VectorOperations;
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

public class UserDatabase {
  private final String LogTag = "Database";
  // Application context to access resources and directory of app-specific storage
  private final Context AppContext;
  // Path of database file
  private final File DatabaseFile;
  // Database identifier, to ensure safe loading
  private final String Id;
  // Vector target length, to validate database inputs
  private final int VectorLength;
  // Database type stored for json serialization
  private final Type userDatabaseType;
  private Map<String, UserRecord> usersRecords;

  public UserDatabase(Context appContext, String databaseName, int vectorLength) {
    this.AppContext = appContext;
    this.DatabaseFile = new File(appContext.getFilesDir(), LogTag + "_" + databaseName + ".json");
    this.Id = databaseName;
    this.VectorLength = vectorLength;
    this.userDatabaseType = new TypeToken<Map<String, UserRecord>>() {}.getType();

    usersRecords = new HashMap<String, UserRecord>();

    // Load Database on creation
    loadDatabase();
  }

  /**
   * Find closest record from the database. Algorithm and time complexity is dependent on database
   * type.
   *
   * @param vector of n-dimensions, for which the closest equivalent wil be found.
   * @return closest UserRecord.
   */
  public UserRecord findClosestRecord(float[] vector) {
    // Check correctness of vector length
    if (vector.length == VectorLength) {
      UserRecord closestRecord = null;
      double minDist = Double.MAX_VALUE;

      for (String user : usersRecords.keySet()) {
        double prevMinDist = minDist;
        minDist =
            Math.min(
                minDist, VectorOperations.cosineSimilarity(vector, usersRecords.get(user).vector));

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
   * Add new UserRecord to the database.
   *
   * @param userRecord to add to database.
   */
  public void addUserRecord(UserRecord userRecord) {
    // Check correctness of vector length
    if (userRecord.vector.length == VectorLength) {
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
      throw new AssertionError("Incorrect vector length");
    }
  }

  /**
   * Add UserRecord to the database. If user already exists, override it's data.
   *
   * @param userRecord to add to database.
   */
  public void forceAddUserRecord(UserRecord userRecord) {
    // Check correctness of vector length
    if (userRecord.vector.length == VectorLength) {
      usersRecords.put(userRecord.username, userRecord);

      // Serialize database immediately
      // TODO: Later on it might be reasonable to save database on application closure (faster)
      saveDatabase();
    } else {
      throw new AssertionError("Incorrect vector length");
    }
  }

  /**
   * Remove userRecord by user name. Grants that there will be no user with given user name.
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
   * Remove userRecord by UserRecord object. Grants that there will be no user with given user name.
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
    return usersRecords.get(userName);
  }

  /**
   * Get vector by user name.
   *
   * @param userName of the searched user.
   * @return vector if found. Null if user does not exist.
   */
  public float[] getUserVector(String userName) {
    return usersRecords.get(userName).vector;
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

  /** Deserialize user database. */
  public void loadDatabase() {
    StringBuilder databaseString = new StringBuilder();

    // Read the content of file
    try (FileInputStream fileInputStream = new FileInputStream(DatabaseFile)) {
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
    // TODO: Later it might be better to throw exception here (in order to display dialog box or
    // sth)
    assertEquals("Wrong type of database", Id, loadedId);
    assertEquals("Wrong size of database", VectorLength, loadedVectorLength);

    // Load users records
    usersRecords.clear();
    usersRecords = gson.fromJson(serializedUserRecords, userDatabaseType);

    Log.d(LogTag, "Database file successfully loaded");
  }

  /** Serialize user database. */
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
    try (FileOutputStream fos = new FileOutputStream(DatabaseFile)) {
      fos.write(databaseString.getBytes());
    } catch (IOException e) {
      Log.e(LogTag, "Cannot save database");
      e.printStackTrace();
      return;
    }

    Log.d(LogTag, "Database file successfully saved");
  }

  /** Load sample database from resource files. */
  public void loadSampleDatabase() {
    // Load sample_database.json from resources to a String
    String resourceString;
    InputStream is = AppContext.getResources().openRawResource(R.raw.sample_database);
    int size = 0;
    try {
      size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      resourceString = new String(buffer, StandardCharsets.UTF_8);
    } catch (IOException e) {
      Log.e(LogTag, "Cannot load resource");
      e.printStackTrace();
      return;
    }

    // Deserialize database string
    Gson gson = new Gson();
    JsonObject databaseJson = gson.fromJson(resourceString, JsonObject.class);

    String loadedId = databaseJson.get("Id").getAsString();
    int loadedVectorLength = databaseJson.get("VectorLength").getAsInt();
    String serializedUserRecords = databaseJson.get("UserRecords").getAsString();

    // Validate database
    // TODO: Later it might be better to throw exception here (in order to display dialog box or
    // sth)
    assertEquals("Wrong type of database", Id, loadedId);
    assertEquals("Wrong size of database", VectorLength, loadedVectorLength);

    // Load users records
    usersRecords.clear();
    usersRecords = gson.fromJson(serializedUserRecords, userDatabaseType);

    Log.d(LogTag, "Database file successfully loaded form resources");
  }
}
