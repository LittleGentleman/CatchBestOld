package com.catchbest;

/**
 * Created by gmm on 2018/1/10.
 */

public class CameraBean {
    private int startX;
    private int startY;
    private int width;
    private int height;
    private int bayerMode;
    private int redGain;
    private int greenGain;
    private int blueGain;
    private int triggerMode;
    private int whiteBalance;
    private int exposureTime;
    private boolean isMirror;
    private boolean isLut;
    private int sensitivity;

    public int getStartX() {
        return startX;
    }

    public void setStartX(int startX) {
        this.startX = startX;
    }

    public int getStartY() {
        return startY;
    }

    public void setStartY(int startY) {
        this.startY = startY;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getBayerMode() {
        return bayerMode;
    }

    public void setBayerMode(int bayerMode) {
        this.bayerMode = bayerMode;
    }

    public int getRedGain() {
        return redGain;
    }

    public void setRedGain(int redGain) {
        this.redGain = redGain;
    }

    public int getGreenGain() {
        return greenGain;
    }

    public void setGreenGain(int greenGain) {
        this.greenGain = greenGain;
    }

    public int getBlueGain() {
        return blueGain;
    }

    public void setBlueGain(int blueGain) {
        this.blueGain = blueGain;
    }

    public int getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(int triggerMode) {
        this.triggerMode = triggerMode;
    }

    public int getWhiteBalance() {
        return whiteBalance;
    }

    public void setWhiteBalance(int whiteBalance) {
        this.whiteBalance = whiteBalance;
    }

    public int getExposureTime() {
        return exposureTime;
    }

    public void setExposureTime(int exposureTime) {
        this.exposureTime = exposureTime;
    }

    public boolean isMirror() {
        return isMirror;
    }

    public void setMirror(boolean mirror) {
        isMirror = mirror;
    }

    public boolean isLut() {
        return isLut;
    }

    public void setLut(boolean lut) {
        isLut = lut;
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    @Override
    public String toString() {
        return "CameraBean{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", width=" + width +
                ", height=" + height +
                ", bayerMode=" + bayerMode +
                ", redGain=" + redGain +
                ", greenGain=" + greenGain +
                ", blueGain=" + blueGain +
                ", triggerMode=" + triggerMode +
                ", whiteBalance=" + whiteBalance +
                ", exposureTime=" + exposureTime +
                ", isMirror=" + isMirror +
                '}';
    }
}
