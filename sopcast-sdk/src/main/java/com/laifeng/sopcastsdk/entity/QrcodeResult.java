package com.laifeng.sopcastsdk.entity;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * @author msp
 * @description SopCastComponent
 * @time 2021/5/27
 */
public class QrcodeResult {

    public QrcodeResult(Bitmap textImg, float x, float y) {
        this.textImg = textImg;
        this.x = x;
        this.y = y;
    }

    // pixel
    private Bitmap textImg;
    private float x;
    private float y;

    public Bitmap getTextImg() {
        return textImg;
    }

    public void setTextImg(Bitmap textImg) {
        this.textImg = textImg;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }
}
