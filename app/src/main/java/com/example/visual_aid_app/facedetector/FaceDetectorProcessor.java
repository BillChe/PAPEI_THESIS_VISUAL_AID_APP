/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.visual_aid_app.facedetector;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.visual_aid_app.CameraActivity;
import com.example.visual_aid_app.activities.VisionProcessorBase;
import com.example.visual_aid_app.camera_utils.GraphicOverlay;
import com.example.visual_aid_app.preference.PreferenceUtils;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Face Detector Demo. */
public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {

  private static final String TAG = "FaceDetectorProcessor";

  private final FaceDetector detector;
  AssetFileDescriptor afd = null;
  MediaPlayer player = null;
  Context context;

  public FaceDetectorProcessor(Context context) {
    super(context);
    prepareAudioPlayer(context);
    FaceDetectorOptions faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(context);
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();
    Log.v(MANUAL_TESTING_LOG, "Face detector options: " + faceDetectorOptions);
    detector = FaceDetection.getClient(highAccuracyOpts);
    this.context = context;
  }

  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  protected Task<List<Face>> detectInImage(InputImage image) {
    return detector.process(image);
  }

  @Override
  protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay) {
    for (Face face : faces) {
      graphicOverlay.add(new FaceGraphic(graphicOverlay, face));
      logExtrasForTesting(face);
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
        if(smileProb > 0.50 && smileProb < 0.80)
        {
         /* Toast.makeText(context,
                  "face detected probably smiling?"+ smileProb,Toast.LENGTH_SHORT).show();*/
          if(player!= null && player.isPlaying())
          {
            player.pause();
          }
        }
        else if(smileProb <= 0.50)
        {
          if(player!= null && player.isPlaying())
          {
            player.pause();
          }
         /* Toast.makeText(context,
                  "face detected and why so serious???"+ smileProb,Toast.LENGTH_SHORT).show();*/
        }
        else if (smileProb >= 0.80) {
         /* Toast.makeText(context,
                  "face detected and SMILIIIING!"+ smileProb,Toast.LENGTH_SHORT).show();*/
          if(player!= null && !player.isPlaying())
          {
            playMusic();
          }
        }
      }
      else
      {
        if(player.isPlaying())
        {
          player.pause();

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
  private void playMusic() {
    player.start();
  }

  private void prepareAudioPlayer(Context context) {
    player = new MediaPlayer();
    try {
      afd = context.getAssets().openFd("supersonic.mp3");
    } catch (IOException e) {
      e.printStackTrace();
    }
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
  }
  private static void logExtrasForTesting(Face face) {
    if (face != null) {
      Log.v(MANUAL_TESTING_LOG, "face bounding box: " + face.getBoundingBox().flattenToString());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle X: " + face.getHeadEulerAngleX());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Y: " + face.getHeadEulerAngleY());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Z: " + face.getHeadEulerAngleZ());

      // All landmarks
      int[] landMarkTypes =
          new int[] {
            FaceLandmark.MOUTH_BOTTOM,
            FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_EAR,
            FaceLandmark.RIGHT_CHEEK,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.NOSE_BASE
          };
      String[] landMarkTypesStrings =
          new String[] {
            "MOUTH_BOTTOM",
            "MOUTH_RIGHT",
            "MOUTH_LEFT",
            "RIGHT_EYE",
            "LEFT_EYE",
            "RIGHT_EAR",
            "LEFT_EAR",
            "RIGHT_CHEEK",
            "LEFT_CHEEK",
            "NOSE_BASE"
          };
      for (int i = 0; i < landMarkTypes.length; i++) {
        FaceLandmark landmark = face.getLandmark(landMarkTypes[i]);
        if (landmark == null) {
          Log.v(
              MANUAL_TESTING_LOG,
              "No landmark of type: " + landMarkTypesStrings[i] + " has been detected");
        } else {
          PointF landmarkPosition = landmark.getPosition();
          String landmarkPositionStr =
              String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y);
          Log.v(
              MANUAL_TESTING_LOG,
              "Position for face landmark: "
                  + landMarkTypesStrings[i]
                  + " is :"
                  + landmarkPositionStr);
        }
      }
      Log.v(
          MANUAL_TESTING_LOG,
          "face left eye open probability: " + face.getLeftEyeOpenProbability());
      Log.v(
          MANUAL_TESTING_LOG,
          "face right eye open probability: " + face.getRightEyeOpenProbability());
      Log.v(MANUAL_TESTING_LOG, "face smiling probability: " + face.getSmilingProbability());
      Log.v(MANUAL_TESTING_LOG, "face tracking id: " + face.getTrackingId());
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    if(player!= null && player.isPlaying())
    {
      player.pause();
    }
    Log.e(TAG, "Face detection failed " + e);
  }
}
