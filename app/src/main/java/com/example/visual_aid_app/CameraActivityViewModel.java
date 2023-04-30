package com.example.visual_aid_app;

import android.content.Context;

import androidx.lifecycle.ViewModel;

public class CameraActivityViewModel extends ViewModel {
    private Context context;
    private boolean flashOn,greyScaleOn,saveImageOn,timerOn,autoDetectOn,numberDetectionOn,autoSave,zoomOn;
    //active camera is front (selfie) or back
    private int activeCamera = 1;



    public CameraActivityViewModel() {
    }

    public CameraActivityViewModel(Context context) {
        this.context = context;

    }

    public CameraActivityViewModel(Context context, boolean flashOn, boolean greyScaleOn,
                                   boolean saveImageOn, boolean timerOn, boolean autoDetectOn,
                                   boolean numberDetectionOn, boolean autoSave, int activeCamera,
                                   boolean zoomOn) {
        this.context = context;
        this.flashOn = flashOn;
        this.greyScaleOn = greyScaleOn;
        this.saveImageOn = saveImageOn;
        this.timerOn = timerOn;
        this.autoDetectOn = autoDetectOn;
        this.numberDetectionOn = numberDetectionOn;
        this.autoSave = autoSave;
        this.activeCamera = activeCamera;
        this.zoomOn = zoomOn;
    }

    public boolean isFlashOn() {
        return flashOn;
    }

    public void setFlashOn(boolean flashOn) {
        this.flashOn = flashOn;
    }

    public boolean isGreyScaleOn() {
        return greyScaleOn;
    }

    public void setGreyScaleOn(boolean greyScaleOn) {
        this.greyScaleOn = greyScaleOn;
    }

    public boolean isSaveImageOn() {
        return saveImageOn;
    }

    public void setSaveImageOn(boolean saveImageOn) {
        this.saveImageOn = saveImageOn;
    }

    public boolean isTimerOn() {
        return timerOn;
    }

    public void setTimerOn(boolean timerOn) {
        this.timerOn = timerOn;
    }

    public boolean isAutoDetectOn() {
        return autoDetectOn;
    }

    public void setAutoDetectOn(boolean autoDetectOn) {
        this.autoDetectOn = autoDetectOn;
    }

    public boolean isNumberDetectionOn() {
        return numberDetectionOn;
    }

    public void setNumberDetectionOn(boolean numberDetectionOn) {
        this.numberDetectionOn = numberDetectionOn;
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public int getActiveCamera() {
        return activeCamera;
    }

    public void setActiveCamera(int activeCamera) {
        this.activeCamera = activeCamera;
    }

    public boolean isZoomOn() {
        return zoomOn;
    }

    public void setZoomOn(boolean zoomOn) {
        this.zoomOn = zoomOn;
    }
}
