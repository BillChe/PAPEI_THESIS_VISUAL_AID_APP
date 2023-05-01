package com.example.visual_aid_app;

import static android.content.ContentValues.TAG;

import static com.example.visual_aid_app.Util.checkHasCameraPermission;
import static com.example.visual_aid_app.ZoomActivity.decodeFile;
import static com.example.visual_aid_app.ZoomActivity.decodeStrem;
import static com.example.visual_aid_app.ZoomActivity.rotateImage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;


import com.example.visual_aid_app.databinding.ActivityCameraBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private Camera camera;
    Camera.Parameters parameters;
/*    private CameraPreview mPreview;*/
    Button captureButton ;
    protected String imageFilePath;
    private SurfaceView mCameraView;
    private TextView textview;
    ZoomControls zoomControls;

    private final int cameraPermissionID = 101;
    int currentZoomLevel = 0, maxZoomLevel = 0;
    boolean isPreviewing, isZoomSupported, isSmoothZoomSupported, flashOn, textDetection;
    private Button zoomBtn, textDetectBtn,
            quickTextDetectBtn,documentDetectBtn, imageDescriptionBtn,faceDetectionBtn,
            colorRecognitionBtn,LightFunctionBtn,noteFunctionBtn,settingsBtn,helpBtn;
    private ImageView flashtBtn, info;
    ImageView showImageView,showImageViewPreview;
    private CameraActivityViewModel mViewModel;
    ActivityCameraBinding binding;
    Context context;
    List <Button> buttonFunctionsList;
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
        //fill Buttons List
        buttonFunctionsList= new ArrayList<>();
        fillButtonList(buttonFunctionsList);

        //camera init
        // Create an instance of Camera
        getCameraInstance();
    }

    private void fillButtonList(List<Button> buttonFunctionsList) {
        buttonFunctionsList.add(textDetectBtn);
        buttonFunctionsList.add(quickTextDetectBtn);
        buttonFunctionsList.add(documentDetectBtn);
        buttonFunctionsList.add(imageDescriptionBtn);
        buttonFunctionsList.add(faceDetectionBtn);
        buttonFunctionsList.add(colorRecognitionBtn);
        buttonFunctionsList.add(LightFunctionBtn);
        buttonFunctionsList.add(noteFunctionBtn);
        buttonFunctionsList.add(settingsBtn);
        buttonFunctionsList.add(helpBtn);

    }

    private void deactivateOtherButtons(String tag)
    {
        for(int i = 0; i < buttonFunctionsList.size();i++)
        {
            if(!buttonFunctionsList.get(i).getTag().equals(tag))
            {
                buttonFunctionsList.get(i).setSelected(false);
            }
        }
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
                         bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

                    }
                }
                if (bitmap != null) {
                    Toast.makeText(CameraActivity.this,
                                    "Picture Captured Successfully:", Toast.LENGTH_LONG)
                            .show();
                    //Pop intent
                    if(textDetection)
                    {
                        bitmap = rotateImage(bitmap, file.getAbsolutePath());
                        detectText(bitmap);
                    }
                    showImageView.setImageBitmap(bitmap);
                    showImageViewPreview.setImageBitmap(bitmap);

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
        info = findViewById(R.id.info);
        zoomControls = (ZoomControls) findViewById(R.id.CAMERA_ZOOM_CONTROLS);
        zoomBtn = findViewById(R.id.zoomBtn);
        showImageView = findViewById(R.id.showImageView);
        showImageViewPreview =  findViewById(R.id.showImageViewPreview);
        textDetectBtn = findViewById(R.id.textDetectBtn);
        textview = findViewById(R.id.textview);


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
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(CameraActivity.this,"Show info for selected function",Toast.LENGTH_SHORT).show();
            }
        });

        textDetectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               /* camera.takePicture(null, null, mPicture);*/
                if(!textDetection)
                {
                    textDetection = true;
                    textDetectBtn.setSelected(true);
                    deactivateOtherButtons(textDetectBtn.getTag().toString());
                }
                else
                {
                    textDetection = false;
                    textDetectBtn.setSelected(false);
                }


            }
        });
    }
    private void detectText(Bitmap imageBitmap) {
        InputImage image = InputImage.fromBitmap(imageBitmap,0);
        TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result = textRecognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(Text text) {
                StringBuilder result = new StringBuilder();
                for(Text.TextBlock textBlock : text.getTextBlocks())
                {
                    String blockText = textBlock.getText();
                    Point[] blockCornerPoint = textBlock.getCornerPoints();
                    Rect blockFrame = textBlock.getBoundingBox();
                    for(Text.Line line : textBlock.getLines())
                    {
                        String lineText = line.getText();
                        Point[] lineCornerPoint = line.getCornerPoints();
                        Rect lineRect = line.getBoundingBox();
                        for(Text.Element element : line.getElements())
                        {
                            String elementText = element.getText();
                            result.append(elementText);
                        }

                    }
                }
                Toast.makeText(CameraActivity.this,result,
                        Toast.LENGTH_LONG).show();
                textview.setText(result);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CameraActivity.this,"Failed to detect text from image"+e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
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

                    List<Camera.Size> supportedSizes = parameters.getSupportedPictureSizes();


                    Camera.Size sizePicture = (supportedSizes.get(0));

                    parameters.setPictureSize(supportedSizes.get(0).width,supportedSizes.get(0).height);

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