package com.example.visual_aid_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private SurfaceView mCameraView;
    private TextView mTextView;
    private CameraSource mCameraSource;
    private TextToSpeech textToSpeech;
    private TextRecognizer textRecognizer;
    private final int cameraPermissionID = 101;
    FirebaseFunctions mFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.text_view);

        // Init TextToSpeech and set language
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });


        mFunctions = FirebaseFunctions.getInstance();
        startCamera();
    }


    /**
     * Starts camera source after camera permission is granted
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == getCameraPermissionID()) {

            if (checkHasCameraPermission()) {

                Log.i("onRequestResult", "Permission has been granted");
                try {
                    mCameraSource.start(mCameraView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

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
                    mTextView.post(new Runnable() {
                        @Override
                        public void run() {

                            //Gets strings from TextBlock and adds to StringBuilder
                            final StringBuilder stringBuilder = new StringBuilder();
                            for(int i=0; i<items.size(); i++)
                                stringBuilder.append(items.valueAt(i).getValue());

                            //Set Text to screen and speaks it if button clicked
                            mTextView.setText(stringBuilder.toString());
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

    /**
     * Init camera source with needed properties,
     * then set camera view to surface view.
     */
    private void startCamera() {

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

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
                    if(checkHasCameraPermission()){

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

    public void createJSONRequest (String base64encoded)
    {
        // Create json request to cloud vision
        JsonObject request = new JsonObject();
        // Add image to request
        JsonObject image = new JsonObject();
        image.add("content", new JsonPrimitive(base64encoded));
        request.add("image", image);
        //Add features to the request
        JsonObject feature = new JsonObject();
        feature.add("type", new JsonPrimitive("TEXT_DETECTION"));
        // Alternatively, for DOCUMENT_TEXT_DETECTION:
        //feature.add("type", new JsonPrimitive("DOCUMENT_TEXT_DETECTION"));
        JsonArray features = new JsonArray();
        features.add(feature);
        request.add("features", features);
        //Optionally, provide language hints to assist with language detection
        JsonObject imageContext = new JsonObject();
        JsonArray languageHints = new JsonArray();
        languageHints.add("en");
        imageContext.add("languageHints", languageHints);
        request.add("imageContext", imageContext);

    }

    @Override
    public void onPause(){
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    /**
     * Check whether camera permission is granted
     * @return true if camera permission is granted, false otherwise
     */
    boolean checkHasCameraPermission() {

        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
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

    int getCameraPermissionID() {
        return cameraPermissionID;
    }
}