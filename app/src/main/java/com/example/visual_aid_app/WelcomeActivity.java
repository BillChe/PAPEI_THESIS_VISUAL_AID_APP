package com.example.visual_aid_app;

import static com.example.visual_aid_app.utils.Util.checkHasCameraPermission;
import static com.example.visual_aid_app.utils.Util.checkHasWritgeExternalStoragePermission;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
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
import android.widget.Toast;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {
    TextToSpeech tts;
    private Button captureBtn,textDetectBtn,textDetectBtnJav,flashtBtn,flashOffBtn,zoomBtn,
            colorRecognitionBtn,faceDetectionBtn;

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
        if(checkHasCameraPermission(WelcomeActivity.this) &&
                checkHasWritgeExternalStoragePermission(WelcomeActivity.this))
        {
            playWelcomeMessage();
            initView();
            openCamera();
        }
        else
        {
            requestCameraPermission();
        }



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
                Intent captureIntent = new Intent(WelcomeActivity.this, MainActivity.class);
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

        zoomBtn = findViewById(R.id.zoomBtn);
        zoomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent captureIntent = new Intent(WelcomeActivity.this, ZoomActivity.class);
                startActivity(captureIntent);
            }
        });
        colorRecognitionBtn = findViewById(R.id.colorRecognitionBtn);
        colorRecognitionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent captureIntent = new Intent(WelcomeActivity.this, ColorDetectionActivity.class);
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
    /**
     * Creates a camera permission request
     */
    void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION},
                101
        );
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0)
        {
            if (checkHasCameraPermission(WelcomeActivity.this)
                    && checkHasWritgeExternalStoragePermission(WelcomeActivity.this)) {
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
                playWelcomeMessage();
                initView();
                openCamera();

            }
            else
            {
                Toast.makeText(this,"Permission Not Granted",Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void openCamera() {
        Intent captureIntent = new Intent(WelcomeActivity.this, CameraActivity.class);
        startActivity(captureIntent);
        finish();
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