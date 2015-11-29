package com.example.andy.sephiri;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.List;

import me.zhanghai.patternlock.ConfirmPatternActivity;
import me.zhanghai.patternlock.PatternUtils;
import me.zhanghai.patternlock.PatternView;

public class RegisteredUserConfirmPatternActivity extends ConfirmPatternActivity {
    SQLiteDatabase db;

    @Override
    protected boolean isPatternCorrect(List<PatternView.Cell> patternDrawn) {
        String patternSha1 = null;

        SharedPreferences sp = this.getSharedPreferences("user", MODE_PRIVATE);
        String emailAddress = sp.getString("emailAddress", "");

        // Fetch the patternSha1 from the database for this user's emailAddress
        db = openOrCreateDatabase("Sephiri", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("SELECT patternSha1 FROM UserList WHERE emailAddress = ?", new String[]{emailAddress});
        if (null != cursor && cursor.getCount()>0){
            cursor.moveToFirst();
            patternSha1 = cursor.getString(0);
        }
        else if (null != cursor && cursor.getCount() == 0){
            // The cursor is empty which is not possible
            Toast.makeText(getApplicationContext(), "DB Query returned 0 rows", Toast.LENGTH_SHORT).show();
            return false;
        }
        else{
            // The cursor is null.
            Toast.makeText(getApplicationContext(), "DB Query returned NULL", Toast.LENGTH_SHORT).show();
            return false;
        }
        db.close();

        return TextUtils.equals(PatternUtils.patternToSha1String(patternDrawn), patternSha1);
    }
}