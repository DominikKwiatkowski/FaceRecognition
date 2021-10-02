package com.UserDatabase;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.R;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.userdatabase.UserDatabase;
import com.libs.globaldata.userdatabase.UserRecord;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class UserDatabaseTests {

    @Test
    public void saveAndLoadTest() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Sample instrumented databse test to present functionality - will redo later
        UserDatabase userDatabase = GlobalData.getModel(appContext,
                appContext.getResources().getString(R.string.model_Facenet),
                appContext.getResources().getString(R.string.model_filename_Facenet)).userDatabase;

        userDatabase.addUserRecord(new UserRecord("Test", new float[]{(float) 13.37}));
        userDatabase.saveDatabase();
        userDatabase.loadDatabase();
        String[] users = userDatabase.getUsersArray();

        assertArrayEquals(users, new String[]{"Test"});
    }
}