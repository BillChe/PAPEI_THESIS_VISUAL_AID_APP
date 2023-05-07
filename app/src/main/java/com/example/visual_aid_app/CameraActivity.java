package com.example.visual_aid_app;

import static android.content.ContentValues.TAG;

import static com.example.visual_aid_app.Util.checkHasCameraPermission;
import static com.example.visual_aid_app.Util.checkHasWritgeExternalStoragePermission;
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
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
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


import com.example.visual_aid_app.databinding.ActivityCameraBinding;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
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

public class CameraActivity extends AppCompatActivity {
    private CameraSource mCameraSource;
    private Camera camera;
    Camera.Parameters parameters;
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
            colorRecognitionBtn, lightFunctionBtn,noteFunctionBtn,settingsBtn,helpBtn,
            button_switch_camera, button_savenote;
    private ImageView flashBtn, info, blackwhite;
    ImageView showImageView,showImageViewPreview;
    private CameraActivityViewModel mViewModel;
    ActivityCameraBinding binding;
    Context context;
    List <Button> buttonFunctionsList;
    private FirebaseFunctions mFunctions;
    private TextToSpeech textToSpeech;
    private int activeCamera = CAMERA_FACING_BACK;
    private EditText noteET;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_camera);
        binding= DataBindingUtil.setContentView(this,R.layout.activity_camera);
        //viewModel Setup
        mViewModel = new CameraActivityViewModel(CameraActivity.this);
        context = CameraActivity.this;
        binding.setViewModel(mViewModel);

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
        mViewModel.setSaveImageOn(true);
        mViewModel.setNoteOn(false);
        //init view with text detection
        textDetectBtn.setSelected(true);;
        mViewModel.setTextDetection(true);
        //camera init
        // Create an instance of Camera
        if (checkHasCameraPermission(CameraActivity.this)
                && checkHasWritgeExternalStoragePermission(CameraActivity.this)) {
            getCameraInstance();
        }
        else
        {
            requestCameraPermission();
        }

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
                final Calendar c = Calendar.getInstance();
                String new_Date = c.get(Calendar.DAY_OF_MONTH) + "-"
                        + ((c.get(Calendar.MONTH)) + 1) + "-"
                        + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR)
                        + "-" + c.get(Calendar.MINUTE) + "-"
                        + c.get(Calendar.SECOND);
                File miDirs = new File(
                        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/myphotos"+ "/%s.jpg", "te1t(" + new_Date + ")");
                if (!miDirs.exists())
                    miDirs.mkdirs();



                imageFilePath = String.format(
                        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/myphotos"
                                + "/%s.jpg", "te1t(" + new_Date + ")");

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
    };

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
    /**
     * Init camera source with needed properties,
     * then set camera view to surface view.
     */
    private void startCamera() {

        textRecognizer = new com.google.android.gms.vision.text.TextRecognizer.Builder(getApplicationContext()).build();

        if (textRecognizer.isOperational()) {

            mCameraSource = new CameraSource.Builder(getApplicationContext(),textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            //If permission is granted cameraSource started and passed it to surfaceView
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    if(checkHasCameraPermission(CameraActivity.this)){

                        try {
                            mCameraSource.start(mCameraView.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    else {

                        Log.i("surfaceCreated","Permission request sent");
                        requestCameraPermission();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });
            final Handler handler = new Handler();
            final int delay = 1000;

            handler.postDelayed(new Runnable() {
                public void run() {
                    detectText();
                    handler.postDelayed(this, delay);

                }
            }, delay);


        }
    }
    private void quickTextDetection(Bitmap bitmap, boolean isDocument)
    {
        Util.scaleBitmapDown(bitmap,640);
        String convertedBitmapResult=Util.convertBitmap(bitmap);

// ...
        mFunctions = FirebaseFunctions.getInstance();
        // Create json request to cloud vision
        JsonObject request = new JsonObject();
// Add image to request
        JsonObject image = new JsonObject();
        image.add("content", new JsonPrimitive(convertedBitmapResult));
        request.add("image", image);
//Add features to the request
        JsonObject feature = new JsonObject();

// Alternatively, for DOCUMENT_TEXT_DETECTION:
        if(isDocument)
        {
            feature.add("type", new JsonPrimitive("DOCUMENT_TEXT_DETECTION"));
        }
        else
        {
            feature.add("type", new JsonPrimitive("TEXT_DETECTION"));
        }

        JsonArray features = new JsonArray();
        features.add(feature);
        request.add("features", features);
        //provide language hints to assist with language detection
        JsonObject imageContext = new JsonObject();
        JsonArray languageHints = new JsonArray();
        languageHints.add("en");
        imageContext.add("languageHints", languageHints);
        request.add("imageContext", imageContext);

        annotateImage(request.toString())
                .addOnCompleteListener(new OnCompleteListener<JsonElement>() {
                    @Override
                    public void onComplete(@NonNull Task<JsonElement> task) {
                        if (!task.isSuccessful()) {
                            // Task failed with an exception
                            // ...
                            quickCaptureText = "no text found";
                            textview.setText(quickCaptureText);
                            textToSpeech.speak(quickCaptureText.toString(), TextToSpeech.QUEUE_FLUSH, null);
                        } else {
                            // Task completed successfully
                            // ...
                            JsonObject annotation = task.getResult().getAsJsonArray().get(0).getAsJsonObject().get("fullTextAnnotation").getAsJsonObject();
                            System.out.format("%nComplete annotation:%n");
                            System.out.format("%s%n", annotation.get("text").getAsString());
                            quickCaptureText = annotation.get("text").getAsString();
                            textToSpeech.speak(quickCaptureText.toString(), TextToSpeech.QUEUE_FLUSH, null);
                            textview.setText(quickCaptureText);
                        }
                    }
                });
    }
    private Task<JsonElement> annotateImage(String requestJson) {
        return mFunctions
                .getHttpsCallable("annotateImage")
                .call(requestJson)
                .continueWith(new Continuation<HttpsCallableResult, JsonElement>() {
                    @Override
                    public JsonElement then(@NonNull Task<HttpsCallableResult> task) {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        return JsonParser.parseString(new Gson().toJson(task.getResult().getData()));
                    }
                });
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
        helpBtn = findViewById(R.id.helpBtn);
        //text detections result textview
        textview = findViewById(R.id.textview);
        noteET = findViewById(R.id.noteET);
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
        helpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(CameraActivity.this,"Show info for selected function",Toast.LENGTH_SHORT).show();
            }
        });

        textDetectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewModel.setZoomOn(false);
                mViewModel.setFaceDetectOn(false);
                mViewModel.setTextDetection(true);
                textDetectBtn.setSelected(true);
                deactivateOtherButtons(textDetectBtn.getTag().toString());

            }
        });

        quickTextDetectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewModel.setZoomOn(false);
                mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(true);
                quickTextDetectBtn.setSelected(true);
                deactivateOtherButtons(quickTextDetectBtn.getTag().toString());
                startCamera();

            }
        });
        documentDetectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewModel.setZoomOn(false);
                mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(true);
                documentDetectBtn.setSelected(true);
                deactivateOtherButtons(documentDetectBtn.getTag().toString());
            }
        });
        faceDetectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewModel.setZoomOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(false);
                faceDetectionBtn.setSelected(true);
                deactivateOtherButtons(faceDetectionBtn.getTag().toString());
                negativeCam = false;
                textDetection = false;
                mViewModel.setFaceDetectOn(true);
            }
        });

        button_switch_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                button_switch_camera.setSelected(true);
                if(activeCamera == CAMERA_FACING_BACK)
                {
                    activeCamera = CAMERA_FACING_FRONT;
                }
                else
                {
                    activeCamera = CAMERA_FACING_BACK;
                }

                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                int cameraCount = Camera.getNumberOfCameras();

                for (int i = 0; i < cameraCount; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        camera.release();
                        camera = Camera.open(i);
                        parameters = camera.getParameters();
                        setCameraDisplayOrientation(CameraActivity.this,0,camera);
                        parameters.setPreviewSize(camera.getParameters().getSupportedPreviewSizes().get(0).width, camera.getParameters().getSupportedPreviewSizes().get(0).height);

                        List<Camera.Size> supportedSizes = parameters.getSupportedPictureSizes();


                        Camera.Size sizePicture = (supportedSizes.get(0));

                        parameters.setPictureSize(supportedSizes.get(0).width,supportedSizes.get(0).height);
                        camera.setParameters(parameters);
                        try {
                            camera.setPreviewDisplay(mCameraView.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        camera.startPreview();
                        break;
                    }
                }

            }
        });
        zoomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activeCamera = CAMERA_FACING_BACK;
                mViewModel.setTextDetection(false);

                negativeCam = false;
                textDetection = false;
                mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setZoomOn(true);
                zoomBtn.setSelected(true);
                zoomControls.setVisibility(View.VISIBLE);
                blackwhite.setVisibility(View.VISIBLE);
                deactivateOtherButtons(zoomBtn.getTag().toString());
               }
        });
        blackwhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!negativeCam)
                {
                    negativeCam = true;
                    parameters = camera.getParameters();
                    parameters.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
                    camera.setParameters(parameters);
                }
                else
                {
                    negativeCam = false;
                    parameters = camera.getParameters();
                    parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
                    camera.setParameters(parameters);
                }

            }
        });
        colorRecognitionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activeCamera = CAMERA_FACING_BACK;
                mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(false);
                mViewModel.setZoomOn(false);
                negativeCam = false;
                textDetection = false;
                colorRecognitionBtn.setSelected(true);
                deactivateOtherButtons(colorRecognitionBtn.getTag().toString());

            }
        });
        lightFunctionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lightFunctionBtn.setSelected(true);
                deactivateOtherButtons(lightFunctionBtn.getTag().toString());
                activeCamera = CAMERA_FACING_BACK;
                mViewModel.setFaceDetectOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setTextDetection(false);
                mViewModel.setZoomOn(false);
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
                mViewModel.setZoomOn(false);
                mViewModel.setNoteOn(false);
                mViewModel.setFaceDetectOn(false);

            }
        });
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsBtn.setSelected(true);
                deactivateOtherButtons(settingsBtn.getTag().toString());
                if (isPreviewing){
                    camera.stopPreview();
                }
                activeCamera = CAMERA_FACING_BACK;
                mViewModel.setFaceDetectOn(false);
                mViewModel.setTextDetection(false);
                mViewModel.setZoomOn(false);
                negativeCam = false;
                textDetection = false;
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
                mViewModel.setFaceDetectOn(false);
                mViewModel.setTextDetection(false);
                mViewModel.setZoomOn(false);
                negativeCam = false;
                textDetection = false;
                noteET.setVisibility(View.VISIBLE);
                button_savenote.setEnabled(true);
                mViewModel.setNoteOn(true);

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

    private void showSettingsActivity() {
        Intent captureIntent = new Intent(CameraActivity.this, WelcomeActivity.class);
        startActivity(captureIntent);
        finish();
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
    /** A safe way to get an instance of the Camera object. */
    public void getCameraInstance(){
        //If permission is granted cameraSource started and passed it to surfaceView
        mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (checkHasCameraPermission(CameraActivity.this)) {
                    textRecognizer = new com.google.android.gms.vision.text.TextRecognizer.Builder(getApplicationContext()).build();
                    camera = Camera.open(activeCamera);
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
                flashBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!flashOn)
                        {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            flashOn = true;
                            camera.setParameters(parameters);
                            mViewModel.setFlashOn(false);
                            flashBtn.setBackground(getDrawable(R.drawable.flash_on_icon));
                        }
                        else
                        {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            flashOn = false;
                            camera.setParameters(parameters);
                            mViewModel.setFlashOn(true);
                            flashBtn.setBackground(getDrawable(R.drawable.flashoff));

                        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0)
        {
            if(checkHasCameraPermission(CameraActivity.this) && checkHasWritgeExternalStoragePermission(CameraActivity.this))
            {
               getCameraInstance();

            }
            else
            {
                Toast.makeText(this,"Permission Not Granted",Toast.LENGTH_SHORT).show();
                requestCameraPermission();
            }
        }

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
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION},
                cameraPermissionID
        );
    }

}