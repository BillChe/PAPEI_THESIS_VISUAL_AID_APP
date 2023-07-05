package com.example.visual_aid_app.activities;

import static android.os.Environment.getExternalStoragePublicDirectory;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.example.visual_aid_app.R;

import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteActivity extends AppCompatActivity {

    private EditText editTextTitle;
    private EditText editTextNote;
    private AppCompatButton buttonSave;
    private AppCompatButton buttonPreview;
    private AppCompatButton buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextNote = findViewById(R.id.editTextNote);

        buttonSave = findViewById(R.id.buttonSave);
        buttonPreview = findViewById(R.id.buttonPreview);
        buttonBack = findViewById(R.id.buttonBack);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });
        buttonPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPreview();
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    private File getOutputDirectory(String titleCustom) {

        File miDirs = new File(
                getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/myNotes/"
                        +getFileNameWithTimeStamp(titleCustom));
        if (!miDirs.exists())
            miDirs.mkdirs();


        File file = new File( String.format(getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/myNotes/"
                +getFileNameWithTimeStamp(titleCustom)));
        return file;
    }

    private void saveNote() {
        String titleCustom = editTextTitle.getText().toString().trim();
        String note = editTextNote.getText().toString().trim();
        if (TextUtils.isEmpty(note)) {
            Toast.makeText(this, "Please enter a note", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(note)) {
            Toast.makeText(this, "Please enter a Title", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = getFileNameWithTimeStamp(titleCustom);
        String fileContent = "Title: " + titleCustom + "\n\n" + note;

        if (isExternalStorageWritable()) {
            File file = new File(getOutputDirectory(titleCustom), fileName);

            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(fileContent.getBytes());
                outputStream.close();
                Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                editTextTitle.setText("");
                editTextNote.setText("");
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "External storage is not available", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private String getFileNameWithTimeStamp(String titleCustom) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timeStamp = dateFormat.format(new Date());
        return titleCustom + timeStamp + ".txt";
    }

    private void openPreview() {
        if (isExternalStorageReadable()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                    + "/myNotes/");
            intent.setDataAndType(uri, "text/*");
            startActivity(Intent.createChooser(intent, "Open folder"));
        } else {
            Toast.makeText(this, "External storage is not available", Toast.LENGTH_SHORT).show();
        }
    }
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}