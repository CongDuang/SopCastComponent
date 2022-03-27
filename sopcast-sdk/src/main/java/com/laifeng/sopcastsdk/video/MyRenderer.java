package com.laifeng.sopcastsdk.video;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Looper;

import com.laifeng.sopcastsdk.EglCore;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.camera.CameraListener;
import com.laifeng.sopcastsdk.camera.CameraUtils;
import com.laifeng.sopcastsdk.camera.exception.CameraDisabledException;
import com.laifeng.sopcastsdk.camera.exception.CameraHardwareException;
import com.laifeng.sopcastsdk.camera.exception.CameraNotSupportException;
import com.laifeng.sopcastsdk.camera.exception.NoCameraException;
import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.entity.QrcodeResult;
import com.laifeng.sopcastsdk.entity.Watermark;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.laifeng.sopcastsdk.mediacodec.VideoMediaCodec;
import com.laifeng.sopcastsdk.utils.WeakHandler;
import com.laifeng.sopcastsdk.video.effect.Effect;
import com.laifeng.sopcastsdk.video.effect.NullEffect;

import java.nio.IntBuffer;

/**
 * @Title: MyRenderer
 * @Package com.laifeng.sopcastsdk.video
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:06
 * @Version
 */
@TargetApi(18)
public class MyRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private int mSurfaceTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    private Watermark mWatermark;
    private QrcodeResult mQrcodeResult;
    private RenderScreen mRenderScreen;
    private RtcRenderScreen mRtcRenderScreen;
    private RenderSrfTex mRenderSrfTex;

    private CameraListener mCameraOpenListener;
    private WeakHandler mHandler = new WeakHandler(Looper.getMainLooper());
    private GLSurfaceView mView;
    private boolean isCameraOpen;
    private Effect mEffect;
    private RtcFBO mRtcFBO;
    private int mEffectTextureId;
    private VideoConfiguration mVideoConfiguration;

    private boolean updateSurface = false;
    private final float[] mTexMtx = GlUtil.createIdentityMtx();

    private int mVideoWidth;
    private int mVideoHeight;

    private RtcVideoListener mRtcVideoListener;

    private EglCore mEglCore;
    private boolean mShouldTakePhoto = false;
    private int mScreenWidth;
    private int mScreenHeight;
    private OnScreenShotListener mOnScreenShotListener;

    public void setOnScreenShotListener(OnScreenShotListener onScreenShotListener) {
        mOnScreenShotListener = onScreenShotListener;
    }

    public MyRenderer(GLSurfaceView view) {
        mView = view;
        mEffect = new NullEffect(mView.getContext());
        mRtcFBO = new RtcFBO();
    }

    public void setCameraOpenListener(CameraListener cameraOpenListener) {
        this.mCameraOpenListener = cameraOpenListener;
    }

    public void setVideoConfiguration(VideoConfiguration videoConfiguration) {
        mVideoConfiguration = videoConfiguration;
        mVideoWidth = VideoMediaCodec.getVideoSize(mVideoConfiguration.width);
        mVideoHeight = VideoMediaCodec.getVideoSize(mVideoConfiguration.height);
        if (mRenderScreen != null) {
            mRenderScreen.setVideoSize(mVideoWidth, mVideoHeight);
        }
        if (mRtcRenderScreen != null) {
            mRtcRenderScreen.setVideoSize(mVideoWidth, mVideoHeight);
        }
    }

    public void setRtcVideoListener(RtcVideoListener rtcVideoListener) {
        mRtcVideoListener = rtcVideoListener;
    }

    public void setRecorder(MyRecorder recorder) {
        synchronized (this) {
            if (recorder != null) {
                mRenderSrfTex = new RenderSrfTex(mEffectTextureId, recorder);
                mRenderSrfTex.setVideoSize(mVideoWidth, mVideoHeight);
                if (mWatermark != null) {
                    mRenderSrfTex.setWatermark(mWatermark);
                }
                if (mQrcodeResult != null) {
                    mRenderSrfTex.setQrcodeResult(mQrcodeResult);
                }
            } else {
                mRenderSrfTex = null;
            }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            updateSurface = true;
        }
        mView.requestRender();
        if (mRtcVideoListener != null && mRtcRenderScreen != null && mRtcRenderScreen.getTimestamp() != -1) {
            mRtcVideoListener.onRtcVideo(mRtcRenderScreen.getTextureId(), mTexMtx, mEglCore.getEGLContext(), mRtcRenderScreen.getTimestamp());
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initSurfaceTexture();
        mEglCore = new EglCore(EGL14.eglGetCurrentContext(), 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        startCameraPreview();
        mEglCore = new EglCore(EGL14.eglGetCurrentContext(), 0);
        if (isCameraOpen) {
            if (mRenderScreen == null) {
                initScreenTexture();
            }
            if (mRtcRenderScreen == null) {
                initRtcScreenTexture();
            }
            mRenderScreen.setScreenSize(width, height);
            mRtcRenderScreen.setScreenSize(width, height);
            mScreenWidth = width;
            mScreenHeight = height;
            if (mVideoConfiguration != null) {
                mRenderScreen.setVideoSize(mVideoWidth, mVideoHeight);
            }
            if (mWatermark != null) {
                mRenderScreen.setWatermark(mWatermark);
            }
            if (mQrcodeResult != null) {
                mRenderScreen.setQrcodeResult(mQrcodeResult);
            }

            if (mVideoConfiguration != null) {
                mRtcRenderScreen.setVideoSize(mVideoWidth, mVideoHeight);
            }
            if (mWatermark != null) {
                mRtcRenderScreen.setWatermark(mWatermark);
            }
            if (mQrcodeResult != null) {
                mRtcRenderScreen.setQrcodeResult(mQrcodeResult);
            }
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (updateSurface) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mTexMtx);
                updateSurface = false;
            }

            if (mShouldTakePhoto && mOnScreenShotListener != null) {
                mShouldTakePhoto = false;
                int w = mScreenWidth;
                int h = mScreenHeight;
                int b[] = new int[(int) (w * h)];
                int bt[] = new int[(int) (w * h)];
                IntBuffer buffer = IntBuffer.wrap(b);
                buffer.position(0);
                GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        int pix = b[i * w + j];
                        int pb = (pix >> 16) & 0xff;
                        int pr = (pix << 16) & 0x00ff0000;
                        int pix1 = (pix & 0xff00ff00) | pr | pb;
                        bt[(h - i - 1) * w + j] = pix1;
                    }
                }
                Bitmap mScreenShotBitmap = null;
                if (mScreenShotBitmap == null || !mScreenShotBitmap.isMutable() || mScreenShotBitmap.getWidth() != w || mScreenShotBitmap.getHeight() != h) {
                    mScreenShotBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                }
                mScreenShotBitmap.copyPixelsFromBuffer(buffer);
                mScreenShotBitmap = Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
                mOnScreenShotListener.shotSuccess(mScreenShotBitmap);
            }
        }
        mEffect.draw(mTexMtx);
        mRtcFBO.draw(mTexMtx);
        if (mRenderScreen != null) {
            mRenderScreen.draw();
        }
        if (mRtcRenderScreen != null) {
            mRtcRenderScreen.draw();
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.draw();
        }
    }

    private void initSurfaceTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mSurfaceTextureId = textures[0];
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void initScreenTexture() {
        mEffect.setTextureId(mSurfaceTextureId);
        mEffect.prepare();
        mEffectTextureId = mEffect.getEffertedTextureId();
        mRenderScreen = new RenderScreen(mEffectTextureId);
    }

    private void initRtcScreenTexture() {
        mRtcFBO.setTextureId(mSurfaceTextureId);
        mRtcFBO.prepare();
        mRtcRenderScreen = new RtcRenderScreen(mRtcFBO.getEffertedTextureId());
    }


    private void startCameraPreview() {
        try {
            CameraUtils.checkCameraService(mView.getContext());
        } catch (CameraDisabledException e) {
            postOpenCameraError(CameraListener.CAMERA_DISABLED);
            e.printStackTrace();
            return;
        } catch (NoCameraException e) {
            postOpenCameraError(CameraListener.NO_CAMERA);
            e.printStackTrace();
            return;
        }
        CameraHolder.State state = CameraHolder.instance().getState();
        CameraHolder.instance().setSurfaceTexture(mSurfaceTexture);
        if (state != CameraHolder.State.PREVIEW) {
            try {
                CameraHolder.instance().openCamera();
                CameraHolder.instance().startPreview();
                if (mCameraOpenListener != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCameraOpenListener.onOpenSuccess();
                        }
                    });
                }
                isCameraOpen = true;
            } catch (CameraHardwareException e) {
                e.printStackTrace();
                postOpenCameraError(CameraListener.CAMERA_OPEN_FAILED);
            } catch (CameraNotSupportException e) {
                e.printStackTrace();
                postOpenCameraError(CameraListener.CAMERA_NOT_SUPPORT);
            }
        }
    }

    private void postOpenCameraError(final int error) {
        if (mCameraOpenListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mCameraOpenListener != null) {
                        mCameraOpenListener.onOpenFail(error);
                    }
                }
            });
        }
    }

    public boolean isCameraOpen() {
        return isCameraOpen;
    }

    public void setWatermark(Watermark watermark) {
        mWatermark = watermark;
        if (mRenderScreen != null) {
            mRenderScreen.setWatermark(watermark);
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.setWatermark(watermark);
        }
        if (mRtcRenderScreen != null) {
            mRtcRenderScreen.setWatermark(watermark);
        }
    }

    /**
     * 设置扫码结果
     *
     * @param qrcodeResult 扫码结果
     * @author 马世鹏
     * @date 2021年5月28日15:13:30
     */
    public void setQrcodeResult(QrcodeResult qrcodeResult) {
        mQrcodeResult = qrcodeResult;
        if (mRenderScreen != null) {
            mRenderScreen.setQrcodeResult(qrcodeResult);
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.setQrcodeResult(qrcodeResult);
        }
        if (mRtcRenderScreen != null) {
            mRtcRenderScreen.setQrcodeResult(qrcodeResult);
        }
    }

    public void setEffect(Effect effect) {
        mEffect.release();
        mEffect = effect;
        effect.setTextureId(mSurfaceTextureId);
        effect.prepare();
        mEffectTextureId = effect.getEffertedTextureId();
        if (mRenderScreen != null) {
            mRenderScreen.setTextureId(mEffectTextureId);
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.setTextureId(mEffectTextureId);
        }
    }

    public void takePhoto() {
        mShouldTakePhoto = true;
    }

    public interface OnScreenShotListener {
        /**
         * 截图成功
         *
         * @param screenShotBitmap
         */
        void shotSuccess(Bitmap screenShotBitmap);
    }
}
