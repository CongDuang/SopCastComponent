package com.laifeng.sopcastsdk.controller;

import android.media.MediaCodec;

import com.laifeng.sopcastsdk.audio.AudioRealTimeListener;
import com.laifeng.sopcastsdk.audio.OnAudioEncodeListener;
import com.laifeng.sopcastsdk.camera.CameraPreviewListener;
import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.controller.audio.IAudioController;
import com.laifeng.sopcastsdk.controller.video.IVideoController;
import com.laifeng.sopcastsdk.stream.packer.Packer;
import com.laifeng.sopcastsdk.stream.sender.Sender;
import com.laifeng.sopcastsdk.utils.SopCastUtils;
import com.laifeng.sopcastsdk.video.OnVideoEncodeListener;

import java.nio.ByteBuffer;

/**
 * @Title: StreamController
 * @Package com.laifeng.sopcastsdk.controller
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 上午11:44
 * @Version
 */
public class StreamController implements OnAudioEncodeListener, OnVideoEncodeListener, Packer.OnPacketListener {
    private Packer mPacker;
    private Sender mSender;
    private Packer mLocalPacker;
    private Sender mLocalSender;
    private IVideoController mVideoController;
    private IAudioController mAudioController;

    public StreamController(IVideoController videoProcessor, IAudioController audioProcessor) {
        mAudioController = audioProcessor;
        // todo 马世鹏 更改 想要的用途就是一进去 就能听到声音
        mAudioController.start();
        mVideoController = videoProcessor;
    }

    public void setVideoConfiguration(VideoConfiguration videoConfiguration) {
        mVideoController.setVideoConfiguration(videoConfiguration);
    }

    public void setAudioConfiguration(AudioConfiguration audioConfiguration) {
        mAudioController.setAudioConfiguration(audioConfiguration);
    }

    public void setAudioRealTimeListener(AudioRealTimeListener listener) {
        mAudioController.setAudioRealTimeListener(listener);
    }

    public void setPacker(Packer packer) {
        mPacker = packer;
        mPacker.setPacketListener(false, this);
    }

    public void setSender(Sender sender) {
        mSender = sender;
    }

    public void setLocalPacker(Packer packer) {
        mLocalPacker = packer;
        mLocalPacker.setPacketListener(true, this);
    }

    public void setLocalSender(Sender sender) {
        mLocalSender = sender;
    }

    public synchronized void start() {
        SopCastUtils.processNotUI(new SopCastUtils.INotUIProcessor() {
            @Override
            public void process() {
                if (mPacker == null || mSender == null) {
                    return;
                }
                if (mLocalPacker == null || mLocalSender == null) {
                    return;
                }
                mPacker.start();
                mSender.start();

                mLocalPacker.start();
                mLocalSender.start();

                mVideoController.setVideoEncoderListener(StreamController.this);
                mAudioController.setAudioEncodeListener(StreamController.this);
                mAudioController.start();
                mVideoController.start();
            }
        });
    }

    /**
     * 控制 AudioController的开始 和 停止，防止多种sdk冲突
     *
     * @param enable true 启用 false 禁用
     */
    public synchronized void enableLocalAudio(boolean enable) {
        if (enable) {
            mAudioController.setAudioEncodeListener(StreamController.this);
            mAudioController.start();
        } else {
            mAudioController.setAudioEncodeListener(null);
            mAudioController.stop();
        }
    }

    public synchronized void stop() {
        SopCastUtils.processNotUI(new SopCastUtils.INotUIProcessor() {
            @Override
            public void process() {
                mVideoController.setVideoEncoderListener(null);
//                mAudioController.setAudioEncodeListener(null);
//                mAudioController.stop();
                mVideoController.stop();
                if (mSender != null) {
                    mSender.stop();
                }
                if (mLocalSender != null) {
                    mLocalSender.stop();
                }
                if (mPacker != null) {
                    mPacker.stop();
                }
                if (mLocalPacker != null) {
                    mLocalPacker.stop();
                }
            }
        });
    }

    public synchronized void pause() {
        SopCastUtils.processNotUI(new SopCastUtils.INotUIProcessor() {
            @Override
            public void process() {
                mAudioController.pause();
                mVideoController.pause();
            }
        });
    }

    public synchronized void resume() {
        SopCastUtils.processNotUI(new SopCastUtils.INotUIProcessor() {
            @Override
            public void process() {
                mAudioController.resume();
                mVideoController.resume();
            }
        });
    }

    public void mute(boolean mute) {
        mAudioController.mute(mute);
    }

    public int getSessionId() {
        return mAudioController.getSessionId();
    }

    public boolean setVideoBps(int bps) {
        return mVideoController.setVideoBps(bps);
    }

    @Override
    public void onAudioEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if (mPacker != null) {
            mPacker.onAudioData(bb, bi);
        }
        if (mLocalPacker != null) {
            mLocalPacker.onAudioData(bb, bi);
        }
    }

    @Override
    public void onVideoEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if (mPacker != null) {
            mPacker.onVideoData(bb, bi);
        }
        if (mLocalPacker != null) {
            mLocalPacker.onVideoData(bb, bi);
        }
    }

    @Override
    public void onPacket(boolean isLocal, byte[] data, int packetType) {
        if (!isLocal) {
            if (mSender != null) {
                mSender.onData(data, packetType);
            }
        } else {
            if (mLocalSender != null) {
                mLocalSender.onData(data, packetType);
            }
        }
    }
}
