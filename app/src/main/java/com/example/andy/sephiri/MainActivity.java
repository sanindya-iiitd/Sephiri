package com.example.andy.sephiri;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;

public class MainActivity extends Activity {

    final int PICK_ACCOUNT_REQUEST = 0x01;
    final int CHECK_PASSWORD_REQUEST = 0x02;
    String TAG = "Sephiri";
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createDatabase();
        pickAccountGoogle();
    }

    /* Creates database if it's not already created and creates UserList table if not already exists. */
    void createDatabase(){
        try {
            db = openOrCreateDatabase("Sephiri", MODE_PRIVATE, null);
            // db.execSQL("DROP TABLE IF EXISTS UserList;");
            db.execSQL("CREATE TABLE IF NOT EXISTS UserList (emailAddress VARCHAR, patternSha1 VARCHAR);");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /* Creates an AccountPicker for all the Google Accounts on the system. User can add a Google
     * account as well. */
    private void pickAccountGoogle (){
        startActivityForResult(
                AccountPicker.newChooseAccountIntent(
                        null,
                        null,
                        new String[]{"com.google"},
                        true, // alwaysPromptForAccount
                        null,
                        null,
                        null,
                        null),
                PICK_ACCOUNT_REQUEST);
    }

    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == PICK_ACCOUNT_REQUEST && resultCode == RESULT_OK) {
            // EmailAddress is a unique identifier.
            String emailAddress = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            SharedPreferences sp = this.getSharedPreferences("user", MODE_APPEND);
            sp.edit().putString("emailAddress", emailAddress).apply();

            // Fetch all the registered email addresses from the database.
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("Select emailAddress from UserList", null);
            } catch (Exception e){
                e.printStackTrace();
            }

            if (null != cursor && cursor.getCount()>0) {
                String emails[] = new String[cursor.getCount()];
                cursor.moveToFirst();
                for (int i = 0; !cursor.isAfterLast(); cursor.moveToNext()) {
                    emails[i++] = cursor.getString(0);
                }
                cursor.close();
                db.close();

                // Check if the email address is already there in the UserList or not.
                Boolean registered = false;
                for (String email : emails) {
                    if (email.equals(emailAddress)) {
                        registered = true;
                        break;
                    }
                }

                // If it's there, start RegisteredUserConfirmPatternActivity to authenticate the user.
                if (registered) {
                    startActivityForResult(new Intent(this, RegisteredUserConfirmPatternActivity.class), CHECK_PASSWORD_REQUEST);
                }
                // If it's not there, start NewUserPatternActivity to register the user.
                else {
                    startActivity(new Intent(this, NewUserSetPatternActivity.class));
                }
            }
            else if (null != cursor && cursor.getCount() == 0){
                // The table is empty, start NewUserSetPatternActivity to register the user.
                startActivity(new Intent(this, NewUserSetPatternActivity.class));
            }
            else {
                // The cursor is null.
                Toast.makeText(getApplicationContext(), "DB Query returned NULL", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            pickAccountGoogle();
        }

        if (requestCode == CHECK_PASSWORD_REQUEST && resultCode == RESULT_OK) {
            // RESULT_OK here means the pattern was confirmed.
            startActivity(new Intent(this, FilePickerActivity.class));
        }
    }
}
