package com.epimenides.myface;

/**
 * @author Epimenides
 * @Package com.epimenides.myface
 * @date 2020/8/15 15:46
 * description :
 */
public class FaceLib {
    // 初始化人脸检测模型
    public native boolean FaceDetectionModelInit(String faceDetectionModelPath);

    // 检测最大人脸
    public native int[] MaxFaceDetect(byte[] imageDate, int imageWidth, int ImageHeight, int imageChannel);

    // 设置检测的最小人脸
    public native boolean SetMinFaceSize(int minSize);

    // 设置模型推理所用的线程数量1, 2, 4, 8
    public native boolean SetThreadsNumber(int threadsNumber);

    // 循环测试次数
    public native boolean SetTimeCount(int timeCount);

    // 提取人脸特征值
    public native float[] extractFeature(byte[] faceDate, int w, int h, int[] landmarks);

    // 人脸特征值比对
    public native double compareFeature(float[] feature1, float[] feature2);
    
    static {
        System.loadLibrary("facedetect");
    }
}
