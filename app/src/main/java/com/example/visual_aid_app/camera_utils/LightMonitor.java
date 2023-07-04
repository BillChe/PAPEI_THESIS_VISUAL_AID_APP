package com.example.visual_aid_app.camera_utils;

import static com.example.visual_aid_app.CameraActivity.lightMonitorOn;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Monitors device temperature.
 */
public final class LightMonitor implements SensorEventListener {

  private static final String TAG = "LightMonitor";

  public Map<String, Float> sensorReadings = new HashMap<>();

  private final SensorManager sensorManager;
  Context context;
  float Dark = 1.0f;
  float Dim = 10.0f;
  float Lowindoorlight =20.0f;
  float averageLight = 30.0f;
  float Well_lit_indoor_area = 40.0f;
  float Bright_indoor_light = 50.0f;
  float Overcast_daylight = 60.0f;
  float Direct_sunlight_through_windows = 70.0f;
  float Bright_sunlight = 80.0f;
  float Direct_sunlight_in_tropical_areas = 90.0f;

  TextView textView;
  
  public LightMonitor(Context context,TextView textView) {
    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    List<Sensor> allSensors = sensorManager.getSensorList(Sensor.TYPE_LIGHT);
    this.textView = textView;
    for (Sensor sensor : allSensors) {
      // Assumes sensors with "temperature" substring in their names are temperature sensors.
      // Those sensors may measure the temperature of different parts of the device. It makes more
      // sense to track the change of themselves, e.g. compare the reading before and after running
      // a detector for a certain amount of time, rather than relying on their absolute values at a
      // certain time.
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        this.context = context;
    }
  }

  public void stop() {
    sensorManager.unregisterListener(this);
  }

  public void logReadings() {
    for (Map.Entry<String, Float> entry : sensorReadings.entrySet()) {
      float light = entry.getValue();
      // Skips likely invalid sensor readings
      if (light < 0) {
        continue;
      }

      Log.i(TAG, String.format(Locale.US, "%s:\t%.1f", entry.getKey(), light));
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    sensorReadings.put(sensorEvent.sensor.getName(), sensorEvent.values[0]);
    float lightValue = sensorEvent.values[0];
    String lightValueText = "";

    if(lightMonitorOn)
    {
      if (lightValue < Dim && lightValue >= Dark) {
        // Dark image
        lightValueText = "Dark";
      }
      else if(lightValue >= Dim && lightValue<Lowindoorlight) {
        lightValueText = "Dim";
      }
      else if(lightValue >= Lowindoorlight && lightValue<averageLight) {
        lightValueText = "Low indoor light";
      }
      else if(lightValue>=averageLight && lightValue < Well_lit_indoor_area) {
        lightValueText = "Average light";
      }
      else if(lightValue>=Well_lit_indoor_area && lightValue < Bright_indoor_light) {
        lightValueText = "Well lit indoor area";

      }
      else if(lightValue>=Bright_indoor_light && lightValue < Overcast_daylight) {
        lightValueText = "Bright indoor light";
      }
      else if(lightValue>=Overcast_daylight && lightValue < Direct_sunlight_through_windows) {
        lightValueText = "Overcast daylight";
      }
      else if(lightValue>=Direct_sunlight_through_windows && lightValue < Bright_sunlight) {
        lightValueText = "Direct sunlight through windows";
      }
      else if(lightValue>=Bright_sunlight && lightValue < Direct_sunlight_in_tropical_areas) {
        lightValueText = "Bright sunlight";
      }
      else if(lightValue>=Direct_sunlight_in_tropical_areas) {
        lightValueText = "Direct sunlight in tropical areas";
      }
      if(lightValueText.length()>0)
      textView.setText(lightValueText);

      Log.i("lightValue", lightValue + "\n" + lightValueText);
    }

  }
}
