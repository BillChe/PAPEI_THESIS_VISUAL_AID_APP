package com.example.visual_aid_app;

import android.content.Context;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.lifecycle.ViewModel;

public class CameraActivityViewModel extends BaseObservable {
    private Context context;
    @Bindable
    private boolean flashOn;
    @Bindable
    private boolean greyScaleOn;
    @Bindable
    private boolean saveImageOn;
    @Bindable
    private boolean timerOn;
    @Bindable
    private boolean autoDetectOn;
    @Bindable
    private boolean numberDetectionOn;
    @Bindable
    private boolean autoSave;
    @Bindable
    private boolean zoomOn;
    @Bindable
    private boolean faceDetectOn;
    @Bindable
    private boolean noteOn;
    @Bindable
    private boolean textDetection;

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
        notifyPropertyChanged(BR.saveImageOn);
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
        notifyPropertyChanged(BR.autoSave);
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
        notifyPropertyChanged(BR.zoomOn);
    }

    public boolean isFaceDetectOn() {
        return faceDetectOn;
    }

    public void setFaceDetectOn(boolean faceDetectOn) {
        this.faceDetectOn = faceDetectOn;
        notifyPropertyChanged(BR.faceDetectOn);
    }

    public boolean isNoteOn() {
        return noteOn;
    }

    public void setNoteOn(boolean noteOn) {
        this.noteOn = noteOn;
        notifyPropertyChanged(BR.noteOn);
    }

    public boolean isTextDetection() {
        return textDetection;
    }

    public void setTextDetection(boolean textDetection) {
        this.textDetection = textDetection;
        notifyPropertyChanged(BR.textDetection);
    }
}
