package com.laifeng.sopcastsdk.audio;

/**
 * @author 马世鹏
 * @description "自定义中间件" 实时监听器
 * @time 2021/5/18
 */
public interface AudioRealTimeListener {

    /**
     * 实时监听 音频数据
     *
     * @param data 音频数据
     */
    void onAudioDate(byte[] data);

}
