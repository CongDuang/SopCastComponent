package com.laifeng.sopcastsdk.camera;

import android.hardware.Camera;

/**
 * @author 马世鹏
 * @desc "自定义中间件" 用来监听 onPreviewFrame
 * @date 2021/5/17
 */
public interface CameraPreviewListener {
    /**
     * @param bytes  字节数组 据说是yuv12
     */
    void onPreviewFrame(byte[] bytes);

}
