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

package com.example.visual_aid_app.textdetector;

import static com.example.visual_aid_app.CameraActivity.lightMonitorOn;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.visual_aid_app.activities.VisionProcessorBase;
import com.example.visual_aid_app.camera_utils.GraphicOverlay;
import com.example.visual_aid_app.preference.PreferenceUtils;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.Text.Element;
import com.google.mlkit.vision.text.Text.Line;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;

import java.util.List;

/** Processor for the text detector demo. */
public class TextRecognitionProcessor extends VisionProcessorBase<Text> {

  private static final String TAG = "TextRecProcessor";

  private final TextRecognizer textRecognizer;
  private final Boolean shouldGroupRecognizedTextInBlocks;
  private final Boolean showLanguageTag;
  private final boolean showConfidence;
  TextView textView;

  public TextRecognitionProcessor(
      Context context, TextRecognizerOptionsInterface textRecognizerOptions) {
    super(context);
    shouldGroupRecognizedTextInBlocks = PreferenceUtils.shouldGroupRecognizedTextInBlocks(context);
    showLanguageTag = PreferenceUtils.showLanguageTag(context);
    showConfidence = PreferenceUtils.shouldShowTextConfidence(context);
    textRecognizer = TextRecognition.getClient(textRecognizerOptions);
  }
  public TextRecognitionProcessor(
          Context context,TextView textView) {
    super(context,textView);
    shouldGroupRecognizedTextInBlocks = false;
    showLanguageTag = false;
    showConfidence = false;
    textRecognizer = null;
  }
  @Override
  public void stop() {
    super.stop();
    if(textRecognizer!=null)
    textRecognizer.close();
  }

  @Override
  protected Task<Text> detectInImage(InputImage image) {
    return textRecognizer.process(image);
  }

  @Override
  protected void onSuccess(@NonNull Text text, @NonNull GraphicOverlay graphicOverlay) {

    if(!lightMonitorOn)
    {
      Log.d(TAG, "On-device Text detection successful");
      logExtrasForTesting(text);
      graphicOverlay.add(
              new TextGraphic(
                      graphicOverlay,
                      text,
                      shouldGroupRecognizedTextInBlocks,
                      showLanguageTag,
                      showConfidence));
    }

  }

  private static void logExtrasForTesting(Text text) {
    if (text != null) {
      Log.v(MANUAL_TESTING_LOG, "Detected text has : " + text.getTextBlocks().size() + " blocks");
      for (int i = 0; i < text.getTextBlocks().size(); ++i) {
        List<Line> lines = text.getTextBlocks().get(i).getLines();
        Log.v(
            MANUAL_TESTING_LOG,
            String.format("Detected text block %d has %d lines", i, lines.size()));
        for (int j = 0; j < lines.size(); ++j) {
          List<Element> elements = lines.get(j).getElements();
          Log.v(
              MANUAL_TESTING_LOG,
              String.format("Detected text line %d has %d elements", j, elements.size()));
          for (int k = 0; k < elements.size(); ++k) {
            Element element = elements.get(k);
            Log.v(
                MANUAL_TESTING_LOG,
                String.format("Detected text element %d says: %s", k, element.getText()));
            Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                    "Detected text element %d has a bounding box: %s",
                    k, element.getBoundingBox().flattenToString()));
            Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                    "Expected corner point size is 4, get %d", element.getCornerPoints().length));
            for (Point point : element.getCornerPoints()) {
              Log.v(
                  MANUAL_TESTING_LOG,
                  String.format(
                      "Corner point for element %d is located at: x - %d, y = %d",
                      k, point.x, point.y));
            }
          }
        }
      }
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.w(TAG, "Text detection failed." + e);
  }
}
