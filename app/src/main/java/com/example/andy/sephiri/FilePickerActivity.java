package com.example.andy.sephiri;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private static final int CLICK_CHOOSE_LOCAL=302;
    private static final int CLICK_CHOOSE_DRIVE=305;

    private Button btn_choose_local;
    private Button btn_encrypt;
    private Button btn_share;
    private TextView txt_original_file;
    private Button btn_choose_drive;
    private Button btn_decrypt;
    private Button btn_view;
    private TextView txt_decrypted_file;

    private String selectedFilePath;
    private String writeFilePath;

    SecretKey secretKey;
    byte[] key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);

        // Compute the key
        String patternSha1 = getPatternFromDB();
        if (null != patternSha1) {
            byte[] keyBytes = patternSha1.getBytes();
            secretKey = new SecretKeySpec(keyBytes, "Blowfish");
            key = secretKey.getEncoded();
        } else {
            Toast.makeText(this,"Something unexpectedly went wrong. Please contact the developer.",Toast.LENGTH_LONG).show();
        }

        // Initialize View Elements
        initView();
        // setOnClickListeners for the Buttons
        setListeners();
    }

    void initView(){
        btn_choose_local = (Button) findViewById(R.id.btn_choose_local);
        btn_encrypt = (Button) findViewById(R.id.btn_encrypt);
        btn_encrypt.setVisibility(View.INVISIBLE);
        btn_share = (Button) findViewById(R.id.btn_share);
        btn_share.setVisibility(View.INVISIBLE);
        txt_original_file = (TextView) findViewById(R.id.txt_original_file);

        btn_choose_drive = (Button) findViewById(R.id.btn_choose_drive);
        btn_decrypt = (Button) findViewById(R.id.btn_decrypt);
        btn_decrypt.setVisibility(View.INVISIBLE);
        btn_view = (Button) findViewById(R.id.btn_view);
        btn_view.setVisibility(View.INVISIBLE);
        txt_decrypted_file = (TextView) findViewById(R.id.txt_decrypted_file);
    }

    void setListeners(){
        btn_choose_local.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select File from Phone"), CLICK_CHOOSE_LOCAL);
            }
        });

        btn_encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "File encrypted.", Toast.LENGTH_SHORT).show();
                encrypt(selectedFilePath, writeFilePath);
                btn_share.setVisibility(View.VISIBLE);
            }
        });

        btn_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("file/*");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(writeFilePath)));
                intent.setAction(Intent.ACTION_SEND);
                intent.setPackage("com.google.android.apps.docs");
                startActivity(Intent.createChooser(intent,"Upload File"));
            }
        });

        btn_choose_drive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select File from Drive"), CLICK_CHOOSE_DRIVE);
            }
        });

        btn_decrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "File decrypted.", Toast.LENGTH_SHORT).show();
                decrypt(selectedFilePath, writeFilePath);
                btn_view.setVisibility(View.VISIBLE);
                txt_decrypted_file.setText("Decrypted File: " + writeFilePath);
            }
        });

        btn_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                int dot = writeFilePath.lastIndexOf('.');
                String fileExt = writeFilePath.substring(dot + 1);
                String mimeType = myMime.getMimeTypeFromExtension(fileExt);
                Intent intent = new Intent();
                intent.setDataAndType(Uri.fromFile(new File(writeFilePath)), mimeType);
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
            }
        });
    }

    String getPatternFromDB(){
        String patternSha1 = null;
        SharedPreferences sp = this.getSharedPreferences("user", MODE_PRIVATE);
        String emailAddress = sp.getString("emailAddress", "");
        // Fetch the patternSha1 from the database for this user's emailAddress
        db = openOrCreateDatabase("Sephiri", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("SELECT patternSha1 FROM UserList WHERE emailAddress = ?", new String[]{emailAddress});
        if (null != cursor && cursor.getCount()>0){
            cursor.moveToFirst();
            patternSha1 = cursor.getString(0);
            cursor.close();
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
        return patternSha1;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CLICK_CHOOSE_LOCAL && resultCode == RESULT_OK && null != data){
            Uri selectedFileUri = data.getData();
            try {
                selectedFilePath = ImageFilePath.getPath(getApplicationContext(), selectedFileUri);
                int dot = selectedFilePath.lastIndexOf('.');
                String fileExt = selectedFilePath.substring(dot + 1);
                writeFilePath = this.getExternalFilesDir(null) + "/" + "Encrypted" + "." + fileExt + ".enc";
                txt_original_file.setText("Selected File: " + selectedFilePath);
            } catch (Exception e){
                e.printStackTrace();
            }
            btn_encrypt.setVisibility(View.VISIBLE);
        }
        else if (requestCode == CLICK_CHOOSE_DRIVE && resultCode == RESULT_OK && null != data){
            Uri selectedFileUri = data.getData();
            try {
                Cursor cursor = getContentResolver().query(selectedFileUri, null, null, null, null);
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                String filename = cursor.getString(nameIndex);
                int dot = filename.lastIndexOf('.');
                String selectedFileName = filename.substring(0,dot);
                dot = selectedFileName.lastIndexOf('.');
                String fileExt = selectedFileName.substring(dot + 1);
                selectedFilePath = this.getExternalFilesDir(null) + "/" + "Downloaded" + "." + fileExt;
                txt_decrypted_file.setText("Downloaded File: " + selectedFilePath);
                writeFilePath = this.getExternalFilesDir(null) + "/" + "Decrypted" + "." + fileExt;
                File localCopy = new File(selectedFilePath);
                int len;
                byte[] b = new byte[1024];
                BufferedInputStream bis = new BufferedInputStream(getContentResolver().openInputStream(selectedFileUri));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(localCopy));
                while ((len = bis.read(b)) > 0) {
                    bos.write(b, 0,len);
                }
                cursor.close();
                bis.close();
                bos.close();
            } catch (Exception e){
                e.printStackTrace();
            }
            btn_decrypt.setVisibility(View.VISIBLE);
        }
        else {
            // Something went wrong.
            Toast.makeText(this,"Something unexpectedly went wrong. Please contact the developer.",Toast.LENGTH_LONG).show();
        }
    }

    private void encrypt(String srcPath, String destPath) {
        try {
            int len;
            byte[] b = new byte[1024];
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(srcPath));
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(destPath));

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            while ((len = bis.read(b)) > 0) {
                byte[] encrypted = cipher.doFinal(b);
                bos.write(encrypted, 0, len);
            }
            bis.close();
            bos.close();
        }
        catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException
                | BadPaddingException | InvalidKeyException | IOException e) {
            Toast.makeText(
                    getApplicationContext(),
                    e.toString(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void decrypt(String srcPath, String destPath) {
        try {
            int len;
            byte[] b = new byte[1024];
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(srcPath));
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(destPath));

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            while ((len = bis.read(b)) > 0) {
                byte[] decrypted = cipher.doFinal(b);
                bos.write(decrypted, 0, len);
            }
            bis.close();
            bos.close();
        }
        catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException
                | BadPaddingException | InvalidKeyException | IOException e) {
            Toast.makeText(
                    getApplicationContext(),
                    e.toString(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
