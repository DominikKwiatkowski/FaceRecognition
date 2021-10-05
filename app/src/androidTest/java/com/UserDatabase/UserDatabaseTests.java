package com.UserDatabase;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.platform.app.InstrumentationRegistry;

import com.R;
import com.libs.globaldata.GlobalData;
import com.libs.globaldata.ModelObject;
import com.libs.globaldata.userdatabase.UserDatabase;
import com.libs.globaldata.userdatabase.UserRecord;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

public class UserDatabaseTests {

    @Test
    public void saveAndLoadTest() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Resources res = appContext.getResources();
        // Sample instrumented database test to present functionality - will redo later
        ModelObject modelObject = GlobalData.getModel(appContext,
                res.getStringArray(R.array.models)[0],
                res.getStringArray(R.array.models)[0]);

        UserDatabase userDatabase = modelObject.userDatabase;

        Random random = new Random();
        float[] vector = new float[modelObject.neuralModel.getOutputSize()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = random.nextFloat();
        }

        userDatabase.addUserRecord(new UserRecord("user", vector));
        userDatabase.saveDatabase();
        userDatabase.loadDatabase();
        String[] users = userDatabase.getUsersArray();

        assertArrayEquals(users, new String[]{"user"});
    }
}