package com.example.visual_aid_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {
    TextToSpeech tts;
    private Button captureBtn,textDetectBtn,textDetectBtnJav,flashtBtn,flashOffBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        //get LANGUAGE configuration and adjust flow and texts
        String currentLanguage = Locale.getDefault().getDisplayLanguage();
        Locale locale = new Locale(currentLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        playWelcomeMessage();

        initView();
    }

    private void initView() {
        captureBtn = findViewById(R.id.captureBtn);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent captureIntent = new Intent(WelcomeActivity.this, ScannerActivity.class);
                startActivity(captureIntent);
            }
        });
        textDetectBtn = findViewById(R.id.textDetectBtn);
        textDetectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent captureIntent = new Intent(WelcomeActivity.this, CameraView.class);
                startActivity(captureIntent);
            }
        });
        textDetectBtnJav = findViewById(R.id.textDetectBtnJav);
        textDetectBtnJav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent captureIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                startActivity(captureIntent);
            }
        });
        flashtBtn = findViewById(R.id.flashtBtn);
        flashtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    String cameraId = null;
                    try {
                        cameraId = camManager.getCameraIdList()[0];
                        camManager.setTorchMode(cameraId, true);   //Turn ON
                        flashOffBtn.setVisibility(View.VISIBLE);
                        flashOffBtn.setEnabled(true);
                        flashtBtn.setVisibility(View.GONE);
                        flashtBtn.setEnabled(false);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        flashOffBtn = findViewById(R.id.flashOffBtn);
        flashOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    String cameraId = null;
                    try {
                        cameraId = camManager.getCameraIdList()[0];
                        camManager.setTorchMode(cameraId, false);   //Turn OFF
                        flashOffBtn.setVisibility(View.GONE);
                        flashOffBtn.setEnabled(false);
                        flashtBtn.setVisibility(View.VISIBLE);
                        flashtBtn.setEnabled(true);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });



    }

    private void playWelcomeMessage() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.US);
                    tts.speak(getString(R.string.welcome_message), TextToSpeech.QUEUE_ADD, null);
                }
            }
        });

    }
}