package com.example.visual_aid_app;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class ZoomActivity extends AppCompatActivity {
    private SurfaceView mCameraView;
    private TextView mTextView;
    private CameraSource mCameraSource;
    private TextRecognizer textRecognizer;
    ZoomControls zoomControls;

    private final int cameraPermissionID = 101;
    int currentZoomLevel = 0, maxZoomLevel = 0;
    Camera camera;
    Camera.Parameters parameters;
    boolean isPreviewing, isZoomSupported, isSmoothZoomSupported;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoom);

        mCameraView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.text_view);
        zoomControls = (ZoomControls) findViewById(R.id.CAMERA_ZOOM_CONTROLS);

 /*       zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maxZoomLevel = parameters.getMaxZoom();
                if(currentZoomLevel < maxZoomLevel){
                    currentZoomLevel++;
                    parameters.setZoom(currentZoomLevel);
                    camera.setParameters(parameters);
                }
            }
        });
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maxZoomLevel = parameters.getMaxZoom();
                if(currentZoomLevel > 0){
                    currentZoomLevel--;
                    camera.startSmoothZoom(currentZoomLevel);
                }
            }
        });*/

        startCamera();
    }
    /**
     * Init camera source with needed properties,
     * then set camera view to surface view.
     */
    private void startCamera() {

       // textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

       // if (textRecognizer.isOperational()) {

        /*    mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();*/

            //If permission is granted cameraSource started and passed it to surfaceView
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    if (checkHasCameraPermission()) {

                        camera = Camera.open();
                        parameters = camera.getParameters();
                        setCameraDisplayOrientation(ZoomActivity.this,0,camera);
                        parameters.setPreviewSize(camera.getParameters().getSupportedPreviewSizes().get(0).width, camera.getParameters().getSupportedPreviewSizes().get(0).height);
                    /*    try {
                            mCameraSource.start(mCameraView.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/

                    } else {

                        Log.i("surfaceCreated", "Permission request sent");
                        requestCameraPermission();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    if (isPreviewing){
                        camera.stopPreview();
                    }
                    camera = Camera.open();
                    parameters = camera.getParameters();
                    setCameraDisplayOrientation(ZoomActivity.this,0,camera);
                    parameters.setPreviewSize(camera.getParameters().getSupportedPreviewSizes().get(0).width, camera.getParameters().getSupportedPreviewSizes().get(0).height);


                    if (parameters.isZoomSupported() && parameters.isSmoothZoomSupported()) {
                        //most phones
                        maxZoomLevel = parameters.getMaxZoom();

                        zoomControls.setIsZoomInEnabled(true);
                        zoomControls.setIsZoomOutEnabled(true);

                        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                if (currentZoomLevel < maxZoomLevel) {
                                    currentZoomLevel++;
                                    camera.startSmoothZoom(currentZoomLevel);

                                }
                            }
                        });

                        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                if (currentZoomLevel > 0) {
                                    currentZoomLevel--;
                                    camera.startSmoothZoom(currentZoomLevel);
                                }
                            }
                        });
                    } else if (parameters.isZoomSupported() && !parameters.isSmoothZoomSupported()){
                        //no smooth zoom, set zoom
                        maxZoomLevel = parameters.getMaxZoom();

                        zoomControls.setIsZoomInEnabled(true);
                        zoomControls.setIsZoomOutEnabled(true);

                        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                if (currentZoomLevel < maxZoomLevel) {
                                    currentZoomLevel++;
                                    parameters.setZoom(currentZoomLevel);
                                    camera.setParameters(parameters);

                                }
                            }
                        });

                        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                if (currentZoomLevel > 0) {
                                    currentZoomLevel--;
                                    parameters.setZoom(currentZoomLevel);
                                    camera.setParameters(parameters);
                                }
                            }
                        });
                    }else{
                        //no zoom on phone
                        zoomControls.setVisibility(View.GONE);
                    }

                    camera.setParameters(parameters);

                    try {
                        camera.setPreviewDisplay(holder);
                    }
                    catch (IOException e) {
                        Log.v(TAG, e.toString());
                    }

                    camera.startPreview(); // begin the preview
                    isPreviewing = true;
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    //mCameraSource.stop();
                    if(camera!=null){
                        camera.stopPreview();
                    }
                }
            });
       // }

    }

    boolean checkHasCameraPermission() {

        return ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Creates a camera permission request
     */
    void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                cameraPermissionID
        );
    }
    private void setCameraDisplayOrientation(Activity activity, int cameraId,
                                             android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}