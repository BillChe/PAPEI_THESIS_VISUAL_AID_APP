package com.example.visual_aid_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.visual_aid_app.utils.ColorFinder;

public class ColorDetectionActivity extends AppCompatActivity {
    private ImageView captureImageView;
    private TextView textview;
    private Button snapBtn, detectBtn;
    private Bitmap imageBitmap;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_detection);
        initViews();
    }

    private void initViews() {
    captureImageView = findViewById(R.id.captureImageView);
    textview = findViewById(R.id.textview);
    snapBtn = findViewById(R.id.snapBtn);
    detectBtn = findViewById(R.id.detectBtn);

        snapBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            if(checkPermission())
            {
                captureImage();
            }
            else
            {
                requestPermission();
            }

        }
    });
        detectBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            detectText();
        }
    });
}

    private boolean checkPermission(){
        int cameraPerm = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA);
        return cameraPerm == PackageManager.PERMISSION_GRANTED;

    }

    private void requestPermission()
    {
        int PERMISSION_CODE= 200;
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},PERMISSION_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0)
        {
            boolean cameraPerm = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if(cameraPerm)
            {
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
                captureImage();

            }
            else
            {
                Toast.makeText(this,"Permission Not Granted",Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void captureImage() {
        Intent camera = new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if(camera.resolveActivity(getPackageManager())!=null)
        {
            startActivityForResult(camera,REQUEST_IMAGE_CAPTURE);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {
            Bundle bundle = data.getExtras();
            imageBitmap = (Bitmap) bundle.get("data");
            captureImageView.setImageBitmap(imageBitmap);

        }
        else
        {

        }
    }

    private void detectText() {
        new ColorFinder(new ColorFinder.CallbackInterface() {
            @Override
            public void onCompleted(String color) {
                Toast.makeText(ColorDetectionActivity.this, "Your Color : " + color, Toast.LENGTH_SHORT).show();
            }
        }).findDominantColor(imageBitmap);
    }
}