package com.example.visual_aid_app;

import static android.content.ContentValues.TAG;

import static com.example.visual_aid_app.Util.checkHasCameraPermission;
import static com.example.visual_aid_app.ZoomActivity.decodeFile;
import static com.example.visual_aid_app.ZoomActivity.decodeStrem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ZoomControls;


import com.example.visual_aid_app.databinding.ActivityCameraBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class CameraActivity extends AppCompatActivity {

    private Camera camera;
    Camera.Parameters parameters;
/*    private CameraPreview mPreview;*/
    Button captureButton ;
    protected String imageFilePath;
    private SurfaceView mCameraView;
/*
    private TextView mTextView;
*/
    ZoomControls zoomControls;

    private final int cameraPermissionID = 101;
    int currentZoomLevel = 0, maxZoomLevel = 0;
    boolean isPreviewing, isZoomSupported, isSmoothZoomSupported, flashOn;
    private Button zoomBtn;
    private ImageView flashtBtn,flashOff;
    ImageView showImageView;
    private CameraActivityViewModel mViewModel;
    ActivityCameraBinding binding;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_camera);
        binding= DataBindingUtil.setContentView(this,R.layout.activity_camera);
        //viewModel Setup
        mViewModel = new CameraActivityViewModel(CameraActivity.this);
        context = CameraActivity.this;

        setViews();
        setListeners();
        //camera init
        // Create an instance of Camera
        getCameraInstance();
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            FileOutputStream pictureFile = null;
            try {
                File miDirs = new File(
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/myphotos");
                if (!miDirs.exists())
                    miDirs.mkdirs();

                final Calendar c = Calendar.getInstance();
                String new_Date = c.get(Calendar.DAY_OF_MONTH) + "-"
                        + ((c.get(Calendar.MONTH)) + 1) + "-"
                        + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR)
                        + "-" + c.get(Calendar.MINUTE) + "-"
                        + c.get(Calendar.SECOND);

                imageFilePath = String.format(
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/myphotos"
                                + "/%s.jpg", "te1t(" + new_Date + ")");

                Uri selectedImage = Uri.parse(imageFilePath);
                File file = new File(imageFilePath);
                String path = file.getAbsolutePath();
                Bitmap bitmap = null;

                pictureFile = new FileOutputStream(file);
                pictureFile.write(data);
                pictureFile.close();

                if (path != null) {
                    if (path.startsWith("content")) {
                        bitmap = decodeStrem(file, selectedImage,
                                CameraActivity.this);
                    } else {
                        bitmap = decodeFile(file, 10);
                    }
                }
                if (bitmap != null) {
                    showImageView.setImageBitmap(bitmap);
                    Toast.makeText(CameraActivity.this,
                                    "Picture Captured Successfully:", Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(CameraActivity.this,
                            "Failed to Capture the picture. kindly Try Again:",
                            Toast.LENGTH_LONG).show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }


        }
    };


    private void setViews() {
        captureButton = (Button) findViewById(R.id.button_capture);
        mCameraView = findViewById(R.id.surfaceView);
        flashtBtn = findViewById(R.id.flashtBtn);
        zoomControls = (ZoomControls) findViewById(R.id.CAMERA_ZOOM_CONTROLS);
        zoomBtn = findViewById(R.id.zoomBtn);
        showImageView = findViewById(R.id.showImageView);


    }

    private void setListeners() {
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        camera.takePicture(null, null, mPicture);
                    }
                }
        );
    }

    /** A safe way to get an instance of the Camera object. */
    public void getCameraInstance(){
        //If permission is granted cameraSource started and passed it to surfaceView
        mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (checkHasCameraPermission(CameraActivity.this)) {

                    camera = Camera.open();
                    parameters = camera.getParameters();
                    setCameraDisplayOrientation(CameraActivity.this,0,camera);
                    parameters.setPreviewSize(camera.getParameters().getSupportedPreviewSizes().get(0).width, camera.getParameters().getSupportedPreviewSizes().get(0).height);


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
                setCameraDisplayOrientation(CameraActivity.this,0,camera);
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
                flashtBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!flashOn)
                        {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            flashOn = true;
                            camera.setParameters(parameters);
                            mViewModel.setFlashOn(false);
                            flashtBtn.setBackground(getDrawable(R.drawable.flash_on_icon));
                        }
                        else
                        {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            flashOn = false;
                            camera.setParameters(parameters);
                            mViewModel.setFlashOn(true);
                            flashtBtn.setBackground(getDrawable(R.drawable.flashoff));

                        }

                    }
                });

                zoomBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mViewModel.setZoomOn(true);
                        zoomControls.setVisibility(View.VISIBLE);
                      //  camera.takePicture(null, null, mPicture);
                    }
                });
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
                if(camera!=null){
                    camera.stopPreview();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mViewModel.setFlashOn(flashOn);
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

}