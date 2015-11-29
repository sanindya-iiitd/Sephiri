package com.example.andy.sephiri;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import me.zhanghai.patternlock.PatternUtils;
import me.zhanghai.patternlock.PatternView;
import me.zhanghai.patternlock.SetPatternActivity;

public class NewUserSetPatternActivity extends SetPatternActivity {

    SQLiteDatabase db;

    @Override
    protected void onSetPattern(List<PatternView.Cell> patternDrawn) {
        String patternSha1 = PatternUtils.patternToSha1String(patternDrawn);

        SharedPreferences sp = this.getSharedPreferences("user", MODE_PRIVATE);
        String emailAddress = sp.getString("emailAddress", "");

        try {
            // Store "emailAddress" in the UserList table along with the "patternSha1" to register the user.
            db = openOrCreateDatabase("Sephiri", MODE_PRIVATE, null);
            db.execSQL("INSERT INTO UserList (emailAddress, patternSha1) " +
                    "VALUES ( ? , ? );", new String[]{emailAddress, patternSha1});
            db.close();
        } catch (Exception e){
            e.printStackTrace();
        }
//        Toast.makeText(getApplicationContext(), "DB Query returned NULL", Toast.LENGTH_SHORT).show();

        // User Registered. Now, take the user to the Cryptography Menu.
        startActivity(new Intent(this, FilePickerActivity.class));
    }
}