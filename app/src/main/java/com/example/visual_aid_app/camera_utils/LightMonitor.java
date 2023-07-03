package com.example.visual_aid_app.camera_utils;

import static com.example.visual_aid_app.CameraActivity.lightMonitorOn;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Monitors device temperature.
 */
public final class LightMonitor implements SensorEventListener {

  private static final String TAG = "TemperatureMonitor";

  public Map<String, Float> sensorReadingsCelsius = new HashMap<>();

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
    /*  if (sensor.getName().toLowerCase().contains("temperature")) {*/
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);

        this.context = context;
     /* }*/
    }
  }

  public void stop() {
    sensorManager.unregisterListener(this);
  }

  public void logTemperature() {
    for (Map.Entry<String, Float> entry : sensorReadingsCelsius.entrySet()) {
      float tempC = entry.getValue();
      // Skips likely invalid sensor readings
      if (tempC < 0) {
        continue;
      }
      float tempF = tempC * 1.8f + 32f;
      Log.i(TAG, String.format(Locale.US, "%s:\t%.1fC\t%.1fF", entry.getKey(), tempC, tempF));
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    sensorReadingsCelsius.put(sensorEvent.sensor.getName(), sensorEvent.values[0]);
    float lightValue = sensorEvent.values[0];
    String lightValueText = "";

    if(lightMonitorOn)
    {
      if (lightValue < Dark) {
        // Dark image
        lightValueText = "Dark";
       // Toast.makeText(context, "Dark image", Toast.LENGTH_SHORT).show();
      }
      else if(lightValue>=Dim) {
        lightValueText = "Dim";
        // Light image
       // Toast.makeText(context, "Dim image", Toast.LENGTH_SHORT).show();

      }
      else if(lightValue>=Lowindoorlight) {
        lightValueText = "Low indoor light";
        // Light image
       // Toast.makeText(context, "Lowindoorlight image", Toast.LENGTH_SHORT).show();

      }
      else if(lightValue>=averageLight) {
        lightValueText = "Average light";
        // Light image
       // Toast.makeText(context, "averageLight image", Toast.LENGTH_SHORT).show();

      }
      else if(lightValue>=Well_lit_indoor_area) {
        lightValueText = "Well lit indoor area";
        // Light image
        Toast.makeText(context, "Well_lit_indoor_area image", Toast.LENGTH_SHORT).show();

      }
      else if(lightValue>=Bright_indoor_light) {
        lightValueText = "Bright indoor light";
        // Light image
      //  Toast.makeText(context, "Bright_indoor_light image", Toast.LENGTH_SHORT).show();

      }
      else if(lightValue>=Overcast_daylight) {
        lightValueText = "Overcast daylight";
        // Light image
      //  Toast.makeText(context, "Overcast_daylight image", Toast.LENGTH_SHORT).show();

      }
      else if(lightValue>=Direct_sunlight_through_windows) {
        lightValueText = "Direct sunlight through_windows";
        // Light image
       // Toast.makeText(context, "Direct_sunlight_through_windows image", Toast.LENGTH_SHORT).show();

      }
      else if(lightValue>=Bright_sunlight) {
        lightValueText = "Bright sunlight";
        // Light image
       // Toast.makeText(context, "Bright_sunlight image", Toast.LENGTH_SHORT).show();

      }
      else if(lightValue>=Direct_sunlight_in_tropical_areas) {
        lightValueText = "Direct sunlight in tropical areas";
        // Light image
       // Toast.makeText(context, "Direct_sunlight_in_tropical_areas image", Toast.LENGTH_SHORT).show();

      }
      textView.setText(lightValueText);

      Log.i("lightValue", String.valueOf(lightValue) + "\n" + lightValueText);
    }

  }
}
