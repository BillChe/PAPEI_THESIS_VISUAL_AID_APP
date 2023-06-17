package com.example.visual_aid_app;

import static com.example.visual_aid_app.utils.Util.checkHasCameraPermission;
import static com.example.visual_aid_app.utils.Util.checkHasWritgeExternalStoragePermission;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.example.visual_aid_app.activities.ChooserActivity;

import java.util.Locale;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_SCREEN_DELAY = 1000;
    TextToSpeech tts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if(getSupportActionBar()!=null)
            this.getSupportActionBar().hide();
        //get LANGUAGE configuration and adjust flow and texts
        String currentLanguage = Locale.getDefault().getDisplayLanguage();
        Locale locale = new Locale(currentLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

        if(checkHasCameraPermission(SplashActivity.this) &&
                checkHasWritgeExternalStoragePermission(SplashActivity.this))
        {
            playWelcomeMessage();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, ChooserActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, SPLASH_SCREEN_DELAY);
        }
        else
        {
            requestCameraPermission();
        }

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
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0)
        {
            if (checkHasCameraPermission(SplashActivity.this)
                    && checkHasWritgeExternalStoragePermission(SplashActivity.this)) {
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
                playWelcomeMessage();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(SplashActivity.this, CameraActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }, SPLASH_SCREEN_DELAY);

            }
            else
            {
                Toast.makeText(this,"Permission Not Granted",Toast.LENGTH_SHORT).show();
            }

        }
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


}