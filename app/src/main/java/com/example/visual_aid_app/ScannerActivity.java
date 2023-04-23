package com.example.visual_aid_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.List;

public class ScannerActivity extends AppCompatActivity {

    private ImageView captureImageView;
    private TextView textview;
    private Button snapBtn, detectBtn;
    private Bitmap imageBitmap;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

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
                //detectText();
                //todo vasilis move this logic on a separate functionality
                detectFace();
            }
        });
    }

    private void detectFace() {
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
                                                    Toast.makeText(ScannerActivity.this,
                                                            "face detected probably smiling?"+ smileProb,Toast.LENGTH_SHORT).show();
                                                } else if (smileProb > 0.80) {

                                                    Toast.makeText(ScannerActivity.this,
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
                                                    Toast.makeText(ScannerActivity.this,
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
                                    }
                                });
    }

    private boolean checkPermission(){
        int cameraPerm = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
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
                        textview.setText(blockText);

                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ScannerActivity.this,"Failed to detect text from image"+e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}