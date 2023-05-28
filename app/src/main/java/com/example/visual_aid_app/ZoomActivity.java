package com.example.visual_aid_app;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class ZoomActivity extends AppCompatActivity {
    private SurfaceView mCameraView;
    ZoomControls zoomControls;

    private final int cameraPermissionID = 101;
    int currentZoomLevel = 0, maxZoomLevel = 0;
    Camera camera;
    Camera.Parameters parameters;
    boolean isPreviewing, isZoomSupported, isSmoothZoomSupported, flashOn;
    private Button flashtBtn,captureBtn;
    ImageView showImageView;
    protected String imageFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoom);

        setViews();
        setListeners();

        startCamera();
    }

    private void setViews() {
        mCameraView = findViewById(R.id.surfaceView);
        flashtBtn = findViewById(R.id.flashtBtn);
        zoomControls = (ZoomControls) findViewById(R.id.CAMERA_ZOOM_CONTROLS);
        captureBtn = findViewById(R.id.captureBtn);
        showImageView = findViewById(R.id.showImageView);
    }

    private void setListeners() {

    }
    /**
     * Init camera source with needed properties,
     * then set camera view to surface view.
     */
    private void startCamera() {
            //If permission is granted cameraSource started and passed it to surfaceView
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    if (checkHasCameraPermission()) {

                        camera = Camera.open();
                        parameters = camera.getParameters();
                        setCameraDisplayOrientation(ZoomActivity.this,0,camera);
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
                    flashtBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(!flashOn)
                            {
                                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                flashOn = true;
                                camera.setParameters(parameters);
                            }
                            else
                            {
                                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                flashOn = false;
                                camera.setParameters(parameters);
                            }

                        }
                    });
                    captureBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            camera.takePicture(null, null, jpegCallback);
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
    /** The jpeg callback. */
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            FileOutputStream outStream = null;
            try {
                File miDirs = new File(
                        Environment.getExternalStorageDirectory() + "/myphotos");
                if (!miDirs.exists())
                    miDirs.mkdirs();

                final Calendar c = Calendar.getInstance();
                String new_Date = c.get(Calendar.DAY_OF_MONTH) + "-"
                        + ((c.get(Calendar.MONTH)) + 1) + "-"
                        + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR)
                        + "-" + c.get(Calendar.MINUTE) + "-"
                        + c.get(Calendar.SECOND);

                imageFilePath = String.format(
                        Environment.getExternalStorageDirectory() + "/myphotos"
                                + "/%s.jpg", "te1t(" + new_Date + ")");

                Uri selectedImage = Uri.parse(imageFilePath);
                File file = new File(imageFilePath);
                String path = file.getAbsolutePath();
                Bitmap bitmap = null;

                outStream = new FileOutputStream(file);
                outStream.write(data);
                outStream.close();

                if (path != null) {
                    if (path.startsWith("content")) {
                        bitmap = decodeStrem(file, selectedImage,
                                ZoomActivity.this);
                    } else {
                        bitmap = decodeFile(file, 10);
                    }
                }
                if (bitmap != null) {
                    showImageView.setImageBitmap(bitmap);
                    Toast.makeText(ZoomActivity.this,
                                    "Picture Captured Successfully:", Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(ZoomActivity.this,
                            "Failed to Capture the picture. kindly Try Again:",
                            Toast.LENGTH_LONG).show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
        }

    };
    /**
     * Decode strem.
     *
     * @param fil
     *            the fil
     * @param selectedImage
     *            the selected image
     * @param mContext
     *            the m context
     * @return the bitmap
     */
    public static Bitmap decodeStrem(File fil, Uri selectedImage,
                                     Context mContext) {

        Bitmap bitmap = null;
        try {

            bitmap = BitmapFactory.decodeStream(mContext.getContentResolver()
                    .openInputStream(selectedImage));

            final int THUMBNAIL_SIZE = getThumbSize(bitmap);

            bitmap = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_SIZE,
                    THUMBNAIL_SIZE, false);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(baos
                    .toByteArray()));

            return bitmap = rotateImage(bitmap, fil.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * Rotate image.
     *
     * @param bmp
     *            the bmp
     * @param imageUrl
     *            the image url
     * @return the bitmap
     */
    public static Bitmap rotateImages(Bitmap bmp, String imageUrl) {
        if (bmp != null) {
            ExifInterface ei;
            int orientation = 0;
            try {
                ei = new ExifInterface(imageUrl);
                orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);

            } catch (IOException e) {
                e.printStackTrace();
            }
            int bmpWidth = bmp.getWidth();
            int bmpHeight = bmp.getHeight();
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    break;
            }
            Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmpWidth,
                    bmpHeight, matrix, true);
            return resizedBitmap;
        } else {
            return bmp;
        }
    }

    /**
     * Decode file.
     *
     * @param f
     *            the f
     * @param sampling
     *            the sampling
     *            the check
     * @return the bitmap
     */
    public static Bitmap decodeFile(File f, int sampling) {
        try {
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(
                    new FileInputStream(f.getAbsolutePath()), null, o2);

            o2.inSampleSize = sampling;
            o2.inTempStorage = new byte[48 * 1024];

            o2.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeStream(
                    new FileInputStream(f.getAbsolutePath()), null, o2);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            bitmap = rotateImage(bitmap, f.getAbsolutePath());
            return bitmap;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Rotate image.
     *
     * @param bmp
     *            the bmp
     * @param imageUrl
     *            the image url
     * @return the bitmap
     */
    public static Bitmap rotateImage(Bitmap bmp, String imageUrl) {
        if (bmp != null) {
            ExifInterface ei;
            int orientation = 0;
            try {
                ei = new ExifInterface(imageUrl);
                orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);

            } catch (IOException e) {
                e.printStackTrace();
            }
            int bmpWidth = bmp.getWidth();
            int bmpHeight = bmp.getHeight();
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    break;
            }
            Log.i("resizedBitmap w [%d]" , String.valueOf(bmpWidth));
            Log.i("resizedBitmap h [%d]" , String.valueOf(bmpHeight));

            Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmpWidth,
                    bmpHeight, matrix, true);
            return resizedBitmap;
        } else {
            return bmp;
        }
    }


    /**
     * Gets the thumb size.
     *
     * @param bitmap
     *            the bitmap
     * @return the thumb size
     */
    public static int getThumbSize(Bitmap bitmap) {

        int THUMBNAIL_SIZE = 250;
        if (bitmap.getWidth() < 300) {
            THUMBNAIL_SIZE = 250;
        } else if (bitmap.getWidth() < 600) {
            THUMBNAIL_SIZE = 500;
        } else if (bitmap.getWidth() < 1000) {
            THUMBNAIL_SIZE = 750;
        } else if (bitmap.getWidth() < 2000) {
            THUMBNAIL_SIZE = 1500;
        } else if (bitmap.getWidth() < 4000) {
            THUMBNAIL_SIZE = 2000;
        } else if (bitmap.getWidth() > 4000) {
            THUMBNAIL_SIZE = 2000;
        }
        return THUMBNAIL_SIZE;
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