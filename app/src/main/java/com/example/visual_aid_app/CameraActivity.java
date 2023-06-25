package com.example.visual_aid_app;

import static android.content.ContentValues.TAG;

import static com.example.visual_aid_app.utils.Util.checkHasCameraPermission;
import static com.example.visual_aid_app.utils.Util.checkHasWritgeExternalStoragePermission;
import static com.example.visual_aid_app.ZoomActivity.decodeStrem;
import static com.example.visual_aid_app.ZoomActivity.rotateImage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;


import com.example.visual_aid_app.camera_utils.GraphicOverlay;
import com.example.visual_aid_app.camera_utils.VisionImageProcessor;
import com.example.visual_aid_app.databinding.ActivityCameraBinding;
import com.example.visual_aid_app.facedetector.FaceDetectorProcessor;
import com.example.visual_aid_app.labeldetector.LabelDetectorProcessor;
import com.example.visual_aid_app.objectdetector.ObjectDetectorProcessor;
import com.example.visual_aid_app.posedetector.PoseDetectorProcessor;
import com.example.visual_aid_app.preference.PreferenceUtils;
import com.example.visual_aid_app.preference.SettingsActivity;
import com.example.visual_aid_app.segmenter.SegmenterProcessor;
import com.example.visual_aid_app.textdetector.TextRecognitionProcessor;
import com.example.visual_aid_app.utils.ColorFinder;
import com.example.visual_aid_app.utils.Util;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
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
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import androidx.camera.core.Camera;

public class CameraActivity extends AppCompatActivity {
    private CameraSource mCameraSource;
    private Camera camera;
/*
    Camera.Parameters parameters;
*/
/*    private CameraPreview mPreview;*/
private com.google.android.gms.vision.text.TextRecognizer textRecognizer;
    Button captureButton ;
    protected String imageFilePath,quickCaptureText;
    private SurfaceView mCameraView;
    private TextView textview;
    ZoomControls zoomControls;
    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;
    private final int cameraPermissionID = 101;
    int currentZoomLevel = 0, maxZoomLevel = 0;
    boolean isPreviewing, isZoomSupported, isSmoothZoomSupported, flashOn, textDetection, negativeCam;
    private Button zoomBtn, textDetectBtn,
            quickTextDetectBtn,documentDetectBtn, imageDescriptionBtn,faceDetectionBtn,
            colorRecognitionBtn, lightFunctionBtn,noteFunctionBtn,settingsBtn,
            button_switch_camera, button_savenote;
    private ImageView flashBtn, info, blackwhite;
    ImageView showImageView,showImageViewPreview;
    private CameraActivityViewModel mViewModel;
    ActivityCameraBinding binding;
    Context context;
    List <Button> buttonFunctionsList;
    private TextToSpeech textToSpeech;
    private int activeCamera = CAMERA_FACING_BACK;
    private EditText noteET;

    ApplicationInfo applicationInfo;

    String applicationName = "";
    //ML Kit staff
    private static final String TAG = "CameraXLivePreview";

    private static final String OBJECT_DETECTION = "Object Detection";
    private static final String OBJECT_DETECTION_CUSTOM = "Custom Object Detection";
    private static final String CUSTOM_AUTOML_OBJECT_DETECTION =
            "Custom AutoML Object Detection (Flower)";
    private static final String FACE_DETECTION = "Face Detection";
    private static final String BARCODE_SCANNING = "Barcode Scanning";
    private static final String IMAGE_LABELING = "Image Labeling";
    private static final String IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)";
    private static final String CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)";
    private static final String POSE_DETECTION = "Pose Detection";
    private static final String SELFIE_SEGMENTATION = "Selfie Segmentation";
    private static final String TEXT_RECOGNITION_LATIN = "Text Recognition Latin";

    private static final String STATE_SELECTED_MODEL = "selected_model";

    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;

    @Nullable
    private ProcessCameraProvider cameraProvider;
    @Nullable private Preview previewUseCase;
    @Nullable private ImageAnalysis analysisUseCase;
    @Nullable private VisionImageProcessor imageProcessor;
    private boolean needUpdateGraphicOverlayImageSourceInfo;

    private String selectedModel = TEXT_RECOGNITION_LATIN;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private CameraSelector cameraSelector;
    private ImageCapture imageCapture;
    public static boolean quickText;
    Bitmap savedImageBitmap;
    String new_Date = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        //viewModel Setup
        context = CameraActivity.this;
        //get app info
        //package name to create folder of images and files
        PackageManager pm = getApplicationContext().getPackageManager();
        try {
            applicationInfo = pm.getApplicationInfo( this.getPackageName(), 0);
        } catch (final PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        applicationName = (String) (applicationInfo != null ? pm.getApplicationLabel(applicationInfo) : "VisualAidApp");
        // Init TextToSpeech and set language
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });

        setViews();
        setListeners();
        //fill Buttons List
        buttonFunctionsList= new ArrayList<>();
        fillButtonList(buttonFunctionsList);

        //init of view model attrs
        new ViewModelProvider(this, (ViewModelProvider.Factory) ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(
                        this,
                        provider -> {
                            cameraProvider = provider;
                            bindAllCameraUseCases();
                        });

      /*  mViewModel.setSaveImageOn(true);
        mViewModel.setNoteOn(false);*/
        //init view with text detection
        textDetectBtn.setSelected(true);
        /*mViewModel.setTextDetection(true);*/
        //camera init
        // Create an instance of Camera
        if (checkHasCameraPermission(CameraActivity.this)
                && checkHasWritgeExternalStoragePermission(CameraActivity.this)) {
            //getCameraInstance();
        }
        else
        {
            requestCameraPermission();
        }

        //ML Kit staff initialization
        if (savedInstanceState != null) {
            //selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, OBJECT_DETECTION);
        }
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        previewView = findViewById(R.id.previewView);
        if (previewView == null) {
            Log.d(TAG, "previewView is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }
        imageCapture = new ImageCapture.Builder().build();

    }

    private void fillButtonList(List<Button> buttonFunctionsList) {
        buttonFunctionsList.add(textDetectBtn);
        buttonFunctionsList.add(quickTextDetectBtn);
        buttonFunctionsList.add(documentDetectBtn);
        buttonFunctionsList.add(imageDescriptionBtn);
        buttonFunctionsList.add(faceDetectionBtn);
        buttonFunctionsList.add(colorRecognitionBtn);
        buttonFunctionsList.add(lightFunctionBtn);
        buttonFunctionsList.add(noteFunctionBtn);
        buttonFunctionsList.add(zoomBtn);
        buttonFunctionsList.add(settingsBtn);

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

    /*private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            FileOutputStream pictureFile = null;
            try {
                final Calendar c = Calendar.getInstance();
                String new_Date = c.get(Calendar.DAY_OF_MONTH) + "-"
                        + ((c.get(Calendar.MONTH)) + 1) + "-"
                        + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR)
                        + "-" + c.get(Calendar.MINUTE) + "-"
                        + c.get(Calendar.SECOND);
                File miDirs = new File(
                        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/myphotos/"
                                +applicationName+ "/%s.jpg", "te1t(" + new_Date + ")");
                if (!miDirs.exists())
                    miDirs.mkdirs();



                imageFilePath = String.format(
                        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/myphotos/"
                                +applicationName+ "/%s.jpg", "te1t(" + new_Date + ")");

                Uri selectedImage = Uri.parse(imageFilePath);
                File file = new File(imageFilePath);
                if(!file.exists()){
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }else{
                    file.delete();
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
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
                    MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, new_Date , "image:"+new_Date);
                    Toast.makeText(CameraActivity.this,
                                    "Picture Captured Successfully:", Toast.LENGTH_LONG)
                            .show();
                    //Pop intent
                    if(textDetectBtn.isSelected())
                    {
                        bitmap = rotateImage(bitmap, file.getAbsolutePath());
                        detectText(bitmap);
                    }
                    else if(quickTextDetectBtn.isSelected())
                    {
                        bitmap = rotateImage(bitmap, file.getAbsolutePath());
                        //quickTextDetection(bitmap,false);
                        detectText(bitmap);

                    }
                    else if(documentDetectBtn.isSelected())
                    {
                        bitmap = rotateImage(bitmap, file.getAbsolutePath());
                        quickTextDetection(bitmap,true);
                    }
                    else if(faceDetectionBtn.isSelected())
                    {
                        if(activeCamera == CAMERA_FACING_BACK)
                        {
                            bitmap = rotateImage(bitmap, file.getAbsolutePath());

                        }
                        detectFace(bitmap);

                    }
                    else if(colorRecognitionBtn.isSelected())
                    {
                        bitmap = rotateImage(bitmap, file.getAbsolutePath());
                        detectColor(bitmap);

                    }
                    else if(lightFunctionBtn.isSelected())
                    {
                        bitmap = rotateImage(bitmap, file.getAbsolutePath());

                    }


                    showImageView.setImageBitmap(bitmap);
                    showImageViewPreview.setImageBitmap(bitmap);


                } else {
                    quickCaptureText = "no text found";
                    textview.setText(quickCaptureText);
                    textToSpeech.speak(quickCaptureText.toString(), TextToSpeech.QUEUE_FLUSH, null);
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
    };*/

    private void detectColor(Bitmap imageBitmap) {
        new ColorFinder(new ColorFinder.CallbackInterface() {
            @Override
            public void onCompleted(String color) {
                Toast.makeText(CameraActivity.this, "Your Color : " + color, Toast.LENGTH_SHORT).show();
            }
        }).findDominantColor(imageBitmap);
    }

    private void detectFace(Bitmap imageBitmap) {
        InputImage image = InputImage.fromBitmap(imageBitmap,0);
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
        FaceDetector detector = FaceDetection.getClient(highAccuracyOpts);
// Or use the default options:
// FaceDetector detector = FaceDetection.getClient();
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        // ...

                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                            // nose available):
                                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                            if (leftEar != null) {
                                                PointF leftEarPos = leftEar.getPosition();
                                            }

                                            // If contour detection was enabled:
                                     /*       List<PointF> leftEyeContour =
                                                    face.getContour(FaceContour.LEFT_EYE).getPoints();
                                            List<PointF> upperLipBottomContour =
                                                    face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();*/
                                            // If classification was enabled:
                                            if (face.getSmilingProbability() != null) {
                                                float smileProb = face.getSmilingProbability();

                                                //todo vasilis add here optimization on this
                                                if(smileProb > 0.50 && smileProb <= 0.80)
                                                {
                                                    Toast.makeText(CameraActivity.this,
                                                            "face detected probably smiling?"+ smileProb,Toast.LENGTH_SHORT).show();
                                                } else if (smileProb > 0.80) {

                                                    Toast.makeText(CameraActivity.this,
                                                            "face detected and SMILIIIING!"+ smileProb,Toast.LENGTH_SHORT).show();
                                                    AssetFileDescriptor afd = null;
                                                    try {
                                                        afd = getAssets().openFd("supersonic.mp3");
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    MediaPlayer player = new MediaPlayer();
                                                    try {
                                                        player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    try {
                                                        player.prepare();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    player.start();
                                                }
                                                else if(smileProb < 0.50)
                                                {
                                                    Toast.makeText(CameraActivity.this,
                                                            "face detected and why so serious???"+ smileProb,Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            if (face.getRightEyeOpenProbability() != null) {
                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                            }

                                            // If face tracking was enabled:
                                            if (face.getTrackingId() != null) {
                                                int id = face.getTrackingId();
                                            }
                                        }


                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                        Toast.makeText(CameraActivity.this,
                                                "no face detected",Toast.LENGTH_SHORT).show();
                                    }
                                });
    }

    private void quickTextDetection(Bitmap bitmap, boolean isDocument)
    {

    }

    private void setViews() {
        //bottom buttons
        captureButton = (Button) findViewById(R.id.button_capture);
        button_switch_camera = findViewById(R.id.button_switch_camera);
        mCameraView = findViewById(R.id.surfaceView);
        flashBtn = findViewById(R.id.flashBtn);
        info = findViewById(R.id.info);
        blackwhite = findViewById(R.id.blackwhite);
        showImageView = findViewById(R.id.showImageView);
        button_savenote = findViewById(R.id.button_savenote);
        showImageViewPreview =  findViewById(R.id.showImageViewPreview);
        //zoom controls
        zoomControls = (ZoomControls) findViewById(R.id.CAMERA_ZOOM_CONTROLS);
        //function buttons
        zoomBtn = findViewById(R.id.zoomBtn);
        textDetectBtn = findViewById(R.id.textDetectBtn);
        quickTextDetectBtn = findViewById(R.id.quickTextDetectBtn);
        documentDetectBtn = findViewById(R.id.documentDetectBtn);
        faceDetectionBtn = findViewById(R.id.faceDetectionBtn);
        colorRecognitionBtn = findViewById(R.id.colorRecognitionBtn);
        lightFunctionBtn = findViewById(R.id.lightFunctionBtn);
        imageDescriptionBtn = findViewById(R.id.imageDescriptionBtn);
        noteFunctionBtn = findViewById(R.id.noteFunctionBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        //text detections result textview
        textview = findViewById(R.id.textview);
        noteET = findViewById(R.id.noteET);
    }

    private void takePhoto() {
        File photoFile = getOutputDirectory();

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String msg = "Photo captured successfully: " + photoFile.getAbsolutePath();
               // Toast.makeText(CameraActivity.this, msg, Toast.LENGTH_SHORT).show();
                savedImageBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                if (savedImageBitmap != null) {
                    MediaStore.Images.Media.insertImage(getContentResolver(), savedImageBitmap,
                            photoFile.getPath(),
                            "image:" + new_Date);
                    Toast.makeText(CameraActivity.this,
                                    msg, Toast.LENGTH_LONG)
                            .show();
                    showImageViewPreview.setImageBitmap(savedImageBitmap);

                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("MainActivity", "Photo capture failed: " + exception.getMessage());
            }
        });
    }

    private File getOutputDirectory() {
        final Calendar c = Calendar.getInstance();
        new_Date = c.get(Calendar.DAY_OF_MONTH) + "-"
                + ((c.get(Calendar.MONTH)) + 1) + "-"
                + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR)
                + "-" + c.get(Calendar.MINUTE) + "-"
                + c.get(Calendar.SECOND);
        File miDirs = new File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/myphotos/"
                        +applicationName+ "/%s.jpg", "te1t(" + new_Date + ")");
        if (!miDirs.exists())
            miDirs.mkdirs();



        imageFilePath = String.format(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/myphotos/"
                        +applicationName+ "/%s.jpg", "te1t(" + new_Date + ")");
        File file = new File(imageFilePath);
        return file;
    }

    private void setListeners() {
        flashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!flashOn)
                {
                    flashOn = true;
                    flashBtn.setBackground(getDrawable(R.drawable.flash_on_icon));
                }
                else
                {
                    flashOn = false;
                    flashBtn.setBackground(getDrawable(R.drawable.flashoff));
                }
                bindPreviewUseCase();
            }
        });
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                       // camera.takePicture(null, null, mPicture);
                        takePhoto();
                    }
                }
        );
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                quickText = false;
                Toast.makeText(CameraActivity.this,"Show info for selected function",Toast.LENGTH_SHORT).show();
            }
        });

        textDetectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               /* mViewModel.setZoomOn(false);
                mViewModel.setFaceDetectOn(false);
                mViewModel.setTextDetection(true);*/
                quickText = false;
                textDetectBtn.setSelected(true);
                selectedModel = TEXT_RECOGNITION_LATIN;
                deactivateOtherButtons(textDetectBtn.getTag().toString());
                bindAnalysisUseCase();

            }
        });

        quickTextDetectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             /*   mViewModel.setZoomOn(false);
                mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(true);*/
                quickText = true;
                selectedModel = TEXT_RECOGNITION_LATIN;
                quickTextDetectBtn.setSelected(true);
                deactivateOtherButtons(quickTextDetectBtn.getTag().toString());
                bindAnalysisUseCase();

            }
        });
        documentDetectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              /*  mViewModel.setZoomOn(false);
                mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(true);*/
                quickText = false;
                selectedModel = TEXT_RECOGNITION_LATIN;
                documentDetectBtn.setSelected(true);
                deactivateOtherButtons(documentDetectBtn.getTag().toString());
                bindAnalysisUseCase();
            }
        });
        faceDetectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
         /*       mViewModel.setZoomOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(false);*/
                quickText = false;
                faceDetectionBtn.setSelected(true);
                selectedModel = FACE_DETECTION;
                deactivateOtherButtons(faceDetectionBtn.getTag().toString());
                negativeCam = false;
                textDetection = false;
                //mViewModel.setFaceDetectOn(true);
                bindAnalysisUseCase();
            }
        });

        button_switch_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (cameraProvider == null) {
                    return;
                }
                int newLensFacing =
                        lensFacing == CameraSelector.LENS_FACING_FRONT
                                ? CameraSelector.LENS_FACING_BACK
                                : CameraSelector.LENS_FACING_FRONT;
                CameraSelector newCameraSelector =
                        new CameraSelector.Builder().requireLensFacing(newLensFacing).build();
                try {
                    if (cameraProvider.hasCamera(newCameraSelector)) {
                        Log.d(TAG, "Set facing to " + newLensFacing);
                        lensFacing = newLensFacing;
                        cameraSelector = newCameraSelector;
                        bindAllCameraUseCases();
                        return;
                    }
                    bindAnalysisUseCase();
                } catch (CameraInfoUnavailableException e) {
                    // Falls through
                }
                Toast.makeText(
                                getApplicationContext(),
                                "This device does not have lens with facing: " + newLensFacing,
                                Toast.LENGTH_SHORT)
                        .show();
            }


        });
        zoomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activeCamera = CAMERA_FACING_BACK;
               /* mViewModel.setTextDetection(false);*/

                negativeCam = false;
                textDetection = false;
            /*    mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setZoomOn(true);*/
                zoomBtn.setSelected(true);
                zoomControls.setVisibility(View.VISIBLE);
                blackwhite.setVisibility(View.VISIBLE);
                deactivateOtherButtons(zoomBtn.getTag().toString());
                bindAnalysisUseCase();
               }
        });
        blackwhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //showSurfaceView();
                if(!negativeCam)
                {
                    negativeCam = true;
                  /*  parameters = camera.getParameters();
                    parameters.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
                    camera.setParameters(parameters);*/
                }
                else
                {
                    negativeCam = false;
                  /*  parameters = camera.getParameters();
                    parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
                    camera.setParameters(parameters);*/
                }

            }
        });
        colorRecognitionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activeCamera = CAMERA_FACING_BACK;
           /*     mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(false);
                mViewModel.setZoomOn(false);*/
                quickText = false;
                negativeCam = false;
                textDetection = false;
                colorRecognitionBtn.setSelected(true);
                deactivateOtherButtons(colorRecognitionBtn.getTag().toString());
                bindAnalysisUseCase();

            }
        });
        lightFunctionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lightFunctionBtn.setSelected(true);
                deactivateOtherButtons(lightFunctionBtn.getTag().toString());
                activeCamera = CAMERA_FACING_BACK;
             /*   mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(false);
                mViewModel.setZoomOn(false);*/
                quickText = false;
                negativeCam = false;
                textDetection = false;
            }
        });
        imageDescriptionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageDescriptionBtn.setSelected(true);
                deactivateOtherButtons(imageDescriptionBtn.getTag().toString());
                activeCamera = CAMERA_FACING_BACK;
                negativeCam = false;
                textDetection = false;
                quickText = false;
          /*      mViewModel.setZoomOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setFaceDetectOn(false);*/
                startImageDescription();
                bindAnalysisUseCase();

            }
        });
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //settingsBtn.setSelected(true);
                //deactivateOtherButtons(settingsBtn.getTag().toString());
          /*      if (isPreviewing){
                    camera.stopPreview();
                }
                activeCamera = CAMERA_FACING_BACK;
                mViewModel.setFaceDetectOn(false);
                mViewModel.setTextDetection(false);
                mViewModel.setZoomOn(false);
                negativeCam = false;
                textDetection = false;*/
                //quickText = false;
                showSettingsActivity();
                //getCameraInstance();
            }
        });
        noteFunctionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                noteFunctionBtn.setSelected(true);
                deactivateOtherButtons(noteFunctionBtn.getTag().toString());
                activeCamera = CAMERA_FACING_BACK;
             /*   mViewModel.setFaceDetectOn(false);
                mViewModel.setTextDetection(false);
                mViewModel.setZoomOn(false);
                negativeCam = false;
                textDetection = false;*/
                noteET.setVisibility(View.VISIBLE);
                button_savenote.setEnabled(true);
                quickText = false;
               /* mViewModel.setNoteOn(true);*/

            }
        });
        button_savenote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(noteET.getText()!=null &&
                        noteET.getText().length()>0)
                {
                    Toast.makeText(CameraActivity.this,noteET.getText(),
                            Toast.LENGTH_LONG).show();
                    textview.setText(noteET.getText());
                }
                else
                {
                    Toast.makeText(CameraActivity.this,"Please add a note",
                            Toast.LENGTH_LONG).show();
                    textToSpeech.speak("Please add a note",TextToSpeech.QUEUE_FLUSH, null);
                }

            }
        });
    }

    private void startImageDescription() {

        selectedModel = OBJECT_DETECTION_CUSTOM;
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {

            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();

        }
    }
    private void bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return;
        }
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewUseCase);
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase, imageCapture);
        // Check if the camera has a flash unit
        boolean hasFlash = camera.getCameraInfo().hasFlashUnit();

        // Control flash if available and facing back
        if (hasFlash) {

            if (flashOn) {
                // Enable flash
                camera.getCameraControl().enableTorch(true);
            } else {
                // Disable flash
                camera.getCameraControl().enableTorch(false);
            }
        }
    }
    private void showSettingsActivity() {
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(intent);

    }

    private void showHelpActivity() {
        Intent captureIntent = new Intent(CameraActivity.this, WelcomeActivity.class);
        startActivity(captureIntent);

    }

    /**
     * Gets TextBlock from TextRecognizer, set Text to TextView
     * and Speaks it if listen button is clicked
     */
    private void detectText(){

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                final SparseArray<TextBlock> items = detections.getDetectedItems();
                if (items.size() != 0 ){
                    textview.post(new Runnable() {
                        @Override
                        public void run() {

                            //Gets strings from TextBlock and adds to StringBuilder
                            final StringBuilder stringBuilder = new StringBuilder();
                            for(int i=0; i<items.size(); i++)
                                stringBuilder.append(items.valueAt(i).getValue());

                            //Set Text to screen and speaks it if button clicked
                            textview.setText(stringBuilder.toString());
                            textToSpeech.speak(stringBuilder.toString(), TextToSpeech.QUEUE_FLUSH, null);
                         /*   findViewById(R.id.voice).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Log.i("OnClickListener","Text is reading");
                                    textToSpeech.speak(stringBuilder.toString(), TextToSpeech.QUEUE_FLUSH, null);
                                }
                            });*/
                        }
                    });
                }
            }
            @Override
            public void release() {
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

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        try {

            switch (selectedModel) {
                case OBJECT_DETECTION:
                    Log.i(TAG, "Using Object Detector Processor");
                    ObjectDetectorOptions objectDetectorOptions =
                            PreferenceUtils.getObjectDetectorOptionsForLivePreview(this);
                    imageProcessor = new ObjectDetectorProcessor(this, objectDetectorOptions);
                    break;
                case OBJECT_DETECTION_CUSTOM:
                    Log.i(TAG, "Using Custom Object Detector Processor");
                    LocalModel localModel =
                            new LocalModel.Builder()
                                    .setAssetFilePath("custom_models/object_labeler.tflite")
                                    .build();
                    CustomObjectDetectorOptions customObjectDetectorOptions =
                            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel);
                    imageProcessor = new ObjectDetectorProcessor(this, customObjectDetectorOptions);
                    break;
                case CUSTOM_AUTOML_OBJECT_DETECTION:
                    Log.i(TAG, "Using Custom AutoML Object Detector Processor");
                    LocalModel customAutoMLODTLocalModel =
                            new LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build();
                    CustomObjectDetectorOptions customAutoMLODTOptions =
                            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                                    this, customAutoMLODTLocalModel);
                    imageProcessor = new ObjectDetectorProcessor(this, customAutoMLODTOptions);
                    break;
                case TEXT_RECOGNITION_LATIN:
                    Log.i(TAG, "Using on-device Text recognition Processor for Latin.");
                    imageProcessor =
                            new TextRecognitionProcessor(this, new TextRecognizerOptions.Builder().build());
                    break;
                case FACE_DETECTION:
                    Log.i(TAG, "Using Face Detector Processor");
                    imageProcessor = new FaceDetectorProcessor(this);
                    break;

                case IMAGE_LABELING:
                    Log.i(TAG, "Using Image Label Detector Processor");
                    imageProcessor = new LabelDetectorProcessor(this, ImageLabelerOptions.DEFAULT_OPTIONS);
                    break;
                case IMAGE_LABELING_CUSTOM:
                    Log.i(TAG, "Using Custom Image Label (Birds) Detector Processor");
                    LocalModel localClassifier =
                            new LocalModel.Builder()
                                    .setAssetFilePath("custom_models/bird_classifier.tflite")
                                    .build();
                    CustomImageLabelerOptions customImageLabelerOptions =
                            new CustomImageLabelerOptions.Builder(localClassifier).build();
                    imageProcessor = new LabelDetectorProcessor(this, customImageLabelerOptions);
                    break;
                case CUSTOM_AUTOML_LABELING:
                    Log.i(TAG, "Using Custom AutoML Image Label Detector Processor");
                    LocalModel customAutoMLLabelLocalModel =
                            new LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build();
                    CustomImageLabelerOptions customAutoMLLabelOptions =
                            new CustomImageLabelerOptions.Builder(customAutoMLLabelLocalModel)
                                    .setConfidenceThreshold(0)
                                    .build();
                    imageProcessor = new LabelDetectorProcessor(this, customAutoMLLabelOptions);
                    break;
                case POSE_DETECTION:
                    PoseDetectorOptionsBase poseDetectorOptions =
                            PreferenceUtils.getPoseDetectorOptionsForLivePreview(this);
                    boolean shouldShowInFrameLikelihood =
                            PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this);
                    boolean visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this);
                    boolean rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this);
                    boolean runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this);
                    imageProcessor =
                            new PoseDetectorProcessor(
                                    this,
                                    poseDetectorOptions,
                                    shouldShowInFrameLikelihood,
                                    visualizeZ,
                                    rescaleZ,
                                    runClassification,
                                    /* isStreamMode = */  true);
                    break;
                case SELFIE_SEGMENTATION:
                    imageProcessor = new SegmenterProcessor(this);
                    break;

                default:
                    throw new IllegalStateException("Invalid model name");
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + selectedModel, e);
            Toast.makeText(
                            getApplicationContext(),
                            "Can not create image processor: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG)
                    .show();
            return;
        }

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        analysisUseCase = builder.build();

        needUpdateGraphicOverlayImageSourceInfo = true;
        analysisUseCase.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
                        } else {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
                        }
                        needUpdateGraphicOverlayImageSourceInfo = false;
                    }
                    try {
                        imageProcessor.processImageProxy(imageProxy, graphicOverlay);
                    } catch (MlKitException e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisUseCase);


    }
    @Override
    public void onResume() {
        super.onResume();
        bindAllCameraUseCases();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0)
        {
            if(checkHasCameraPermission(CameraActivity.this) && checkHasWritgeExternalStoragePermission(CameraActivity.this))
            {
              // getCameraInstance();

            }
            else
            {
                Toast.makeText(this,"Permission Not Granted",Toast.LENGTH_SHORT).show();
                requestCameraPermission();
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
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION},
                cameraPermissionID
        );
    }

}