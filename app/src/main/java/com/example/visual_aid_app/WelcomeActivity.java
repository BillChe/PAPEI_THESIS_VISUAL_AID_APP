package com.example.visual_aid_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {
    TextToSpeech tts;
    private Button captureBtn;

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