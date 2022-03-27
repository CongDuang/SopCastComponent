package com.laifeng.sopcastsdk.video;

import android.opengl.EGLContext;

/**
 * @author msp
 * @description SopCastComponent
 * @time 2021/6/9
 */
public interface RtcVideoListener {

    void onRtcVideo(int textureId, float[] mTransform, EGLContext context, long timeStamp);

}
