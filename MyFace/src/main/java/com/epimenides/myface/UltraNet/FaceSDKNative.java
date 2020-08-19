package com.epimenides.myface.UltraNet;

/**
 * @author Epimenides
 * @Package com.epimenides.myface.UltraNet
 * @date 2020/8/15 11:48
 * description :
 */
public class FaceSDKNative {
    //SDK 初始化
    public native boolean FaceDetectionModelInit(String faceDetectionModelPath);

    //SDK 人脸检测接口
    public native int[] FaceDetect(byte[] imageDate, int imageWidth , int imageHeight, int imageChannel);

    //SDK 销毁
    public native boolean FaceDetectionModelUnInit();

    static {
        System.loadLibrary("facedetect");
    }

}
