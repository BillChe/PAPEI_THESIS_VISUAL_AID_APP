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

package com.example.visual_aid_app.preference;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.visual_aid_app.R;


/**
 * Hosts the preference fragment to configure settings
 */
public class SettingsActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_settings);

    try {
      getFragmentManager()
          .beginTransaction()
          .replace(
              R.id.settings_container,
              CameraXLivePreviewPreferenceFragment.class.getDeclaredConstructor().newInstance())
          .commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
