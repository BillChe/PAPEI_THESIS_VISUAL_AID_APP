package com.example.visual_aid_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.lifecycle.LifecycleOwner;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import android.Manifest;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraXActivity extends AppCompatActivity {
    PreviewView previewView;
    private static final int REQUEST_CAMERA_PERMISSION = 123;
    private ExecutorService cameraExecutor;
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_xactivity);

        // Find the SurfaceView in your layout
        //surfaceView = findViewById(R.id.surfaceView);
        previewView = findViewById(R.id.surfaceView);
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        // Create an ExecutorService for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor();
        bindCameraUseCases();
       /* // Set up SurfaceHolder.Callback to get notified when the SurfaceView is ready
        previewView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Start the camera when the SurfaceView is ready
                bindCameraUseCases();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // Unused, but required method
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // Release the camera when the SurfaceView is destroyed
                cameraExecutor.shutdown();
            }
        });*/
    }

    private void bindCameraUseCases() {
        // Set up CameraProvider
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
/*        ProcessCameraProvider cameraProvider;
        try {
            cameraProvider = ProcessCameraProvider.getInstance(this);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to create camera provider.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set up Preview
        PreviewConfig previewConfig = new PreviewConfig.Builder().build();
        Preview preview = new Preview(previewConfig);

        // Set the surface provider to display the camera preview
        preview.setSurfaceProvider(surfaceView.getHolder().getSurfaceProvider());

        // Select the back camera as the default camera
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        // Attach the preview use case and start the camera
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);*/
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }

        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis, imageCapture);


/*
        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                File file = new File(getBatchDirectoryName(), mDateFormat.format(new Date())+ ".jpg");

                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
                imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback () {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Image Saved successfully", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        error.printStackTrace();
                    }
                });
            }
        });*/
    }
}