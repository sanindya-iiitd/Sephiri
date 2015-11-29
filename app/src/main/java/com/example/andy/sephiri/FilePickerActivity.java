package com.example.andy.sephiri;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class FilePickerActivity extends Activity {
    SQLiteDatabase db;

    private static final int CLICK_ENCRYPT=302;
    private static final int CLICK_DECRYPT=303;
    private Button btn_encrypt;
    private Button btn_decrypt;
    private Button btn_share;
    private TextView txt_file_path;
    private String selectedFilePath;
    private String writeFilePath;
    private String rewriteFilePath;
    private String fileExt;

    SecretKey skey;
    byte[] key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);

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
            finish();
        }
        else{
            // The cursor is null.
            Toast.makeText(getApplicationContext(), "DB Query returned NULL", Toast.LENGTH_SHORT).show();
            finish();
        }
        db.close();

        byte[] keyBytes = patternSha1.getBytes();
        skey = new SecretKeySpec(keyBytes, "Blowfish");
        key = skey.getEncoded();

        txt_file_path = (TextView) findViewById(R.id.txt_file_path);

        btn_encrypt = (Button) findViewById(R.id.btn_encrypt);
        btn_encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select File"), CLICK_ENCRYPT);
            }
        });

        btn_decrypt = (Button) findViewById(R.id.btn_decrypt);
        btn_decrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select File"), CLICK_DECRYPT);
            }
        });

        btn_share = (Button) findViewById(R.id.btn_share);
        btn_share.setVisibility(View.GONE);
        btn_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(writeFilePath);
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                intent.setAction(Intent.ACTION_SEND);
                startActivity(Intent.createChooser(intent,"Upload File"));
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CLICK_ENCRYPT && resultCode == RESULT_OK && null != data){
            Toast.makeText(this,"Encrypting...",Toast.LENGTH_LONG).show();
            Uri selectedFileUri = data.getData();
            try {
                selectedFilePath = ImageFilePath.getPath(getApplicationContext(), selectedFileUri);
                int dot = selectedFilePath.lastIndexOf('.');
                fileExt = selectedFilePath.substring(dot + 1);
                writeFilePath = this.getExternalFilesDir(null) + "/" + "Encrypted" + "." + fileExt + ".enc";
                // int slash = selectedFilePath.lastIndexOf('/');
                // String temp = selectedFilePath.substring(0, slash);
                // writeFilePath = temp + "/" + "Encrypted" + "." + fileExt;
                //rewriteFilePath = this.getExternalFilesDir(null) + "/" + "Decrypted" + "." + fileExt;
                txt_file_path.setText("File Path : \n" + writeFilePath);
            } catch (Exception e){
                e.printStackTrace();
            }
            encrypt(selectedFilePath, writeFilePath);
            btn_share.setVisibility(View.VISIBLE);
        }
        else if (requestCode == CLICK_DECRYPT && resultCode == RESULT_OK && null != data){
//            Toast.makeText(this,"Decrypting...",Toast.LENGTH_LONG).show();
            Uri selectedFileUri = data.getData();
            try {
                Cursor cursor =
                        getContentResolver().query(selectedFileUri, null, null, null, null);
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                String filename = cursor.getString(nameIndex);
                int dot = filename.lastIndexOf('.');
                String selectedFileName = filename.substring(0,dot);
                dot = selectedFileName.lastIndexOf('.');
                fileExt = selectedFileName.substring(dot + 1);

                selectedFilePath = this.getExternalFilesDir(null) + "/" + "ToBeDecrypted" + "." + fileExt;
                File localCopy = new File(selectedFilePath);
                int len = 0;
                byte[] b = new byte[1024];
                BufferedInputStream bis = new BufferedInputStream(getContentResolver().openInputStream(selectedFileUri));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(localCopy));
                while ((len = bis.read(b)) > 0) {
                    bos.write(b,0,len);
                }
                cursor.close();
                bis.close();
                bos.close();

                Toast.makeText(this,"File copied.",Toast.LENGTH_LONG).show();

                writeFilePath = this.getExternalFilesDir(null) + "/" + "Decrypted" + "." + fileExt;

                /*selectedFilePath = ImageFilePath.getPath(getApplicationContext(), selectedFileUri);
                if (null != selectedFilePath) {
                    int dot = selectedFilePath.lastIndexOf('.');
                    fileExt = selectedFilePath.substring(dot + 1);
                    writeFilePath = this.getExternalFilesDir(null) + "/" + "Decrypted" + "." + fileExt;
                    //rewriteFilePath = this.getExternalFilesDir(null) + "/" + "Decrypted" + "." + fileExt;
                    txt_file_path.setText("File Path : \n" + fileExt);
                }*/
            } catch (Exception e){
                e.printStackTrace();
            }
             decrypt (selectedFilePath, writeFilePath);
        }
        else {
            Toast.makeText(this,"Wait, what???",Toast.LENGTH_LONG).show();
        }
    }

    private void encrypt(String srcPath, String destPath) {
        try {
            int len = 0;
            byte[] b = new byte[1024];
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(srcPath));
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(destPath));

            SecretKeySpec skeySpec = new SecretKeySpec(key, "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

            while ((len = bis.read(b)) > 0) {
                byte[] encrypted = cipher.doFinal(b);
                bos.write(encrypted, 0, len);
            }

            bis.close();
            bos.close();
        } catch (NoSuchPaddingException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (NoSuchAlgorithmException ex){
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (IllegalBlockSizeException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (BadPaddingException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (InvalidKeyException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void decrypt(String srcPath, String destPath) {
        try {
            int len = 0;
            byte[] b = new byte[1024];
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(srcPath));
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(destPath));

            SecretKeySpec skeySpec = new SecretKeySpec(key, "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);

            while ((len = bis.read(b)) > 0) {
                byte[] decrypted = cipher.doFinal(b);
                bos.write(decrypted, 0, len);
            }

            bis.close();
            bos.close();
        } catch (NoSuchPaddingException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (NoSuchAlgorithmException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (IllegalBlockSizeException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (BadPaddingException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (InvalidKeyException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException ex) {
            Toast.makeText(getApplicationContext(), ex.toString(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
