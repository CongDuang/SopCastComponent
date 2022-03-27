package com.laifeng.sopcastsdk.stream.sender.local;

import android.content.Context;
import android.os.Environment;

import com.laifeng.sopcastsdk.BuildConfig;
import com.laifeng.sopcastsdk.stream.sender.Sender;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @Title: LocalSender
 * @Package com.laifeng.sopcastsdk.stream.sender.local
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午5:10
 * @Version
 */
public class LocalSender implements Sender {
    private File mFile;
    private FileOutputStream mOutStream;
    private static BufferedOutputStream mBuffer;

    private Context mContext;
    private String mFileName = "SopCastFlv";
    private String mDirName = BuildConfig.LIBRARY_PACKAGE_NAME;
    private String mFilePath = "";
    private OnLocalSenderListener mOnLocalSenderListener;

    public LocalSender(Context context) {
        mContext = context;
    }

    public void setOnLocalSenderListener(OnLocalSenderListener onLocalSenderListener) {
        mOnLocalSenderListener = onLocalSenderListener;
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    public void setDirName(String dirName) {
        mDirName = dirName;
    }

    @Override
    public void start() {
        StringBuilder builder = new StringBuilder()
                .append(mContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES))
                .append("/")
                .append(mDirName);

        // 创建文件夹
        File dir = new File(builder.toString());
        if (!dir.exists()) {
            dir.mkdir();
        }

        mFilePath = builder.append("/").append(mFileName)
                .append(".flv").toString();

        mFile = new File(mFilePath);

        if (mFile.exists()) {
            mFile.delete();
        }

        if (mOnLocalSenderListener != null) {
            mOnLocalSenderListener.onLocalSendStart();
        }

        try {
            mFile.createNewFile();
            mOutStream = new FileOutputStream(mFile);
            mBuffer = new BufferedOutputStream(mOutStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onData(byte[] data, int type) {
        if (mBuffer != null) {
            try {
                mBuffer.write(data);
                mBuffer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        if (mBuffer != null) {
            try {
                mBuffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBuffer = null;
        }
        if (mOutStream != null) {
            try {
                mOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBuffer = null;
            mOutStream = null;
        }
        if (mOnLocalSenderListener != null) {
            mOnLocalSenderListener.onLocalSendFinish(mFile);
        }
    }

    public interface OnLocalSenderListener {
        /**
         * 本地录制开始
         */
        void onLocalSendStart();

        /**
         * 本地录制结束
         *
         * @param flvVideoFile flv文件
         */
        void onLocalSendFinish(File flvVideoFile);
    }
}
