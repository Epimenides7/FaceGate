package com.wudi.facegate.utils;

import android.os.Environment;

/**
 * 常量
 * Created by wudi on 2020/5/7.
 */
public class MyConstants {
    //参数配置
    public static final String HTTP_ADDRESS = "http://face.91jtg.com/face/touch/";//服务器地址
    public static final String KEY_ID = "JTGFACEID";//密钥ID
    public static final String KEY = "06660b3e-523e-43d7-86b8-4e94578afd86";//密钥
    public static final String ALI_PATH = "https://jtg-face.oss-cn-hangzhou.aliyuncs.com/";//阿里云图片上传地址


    //人脸错误码
    public static final int NO_FACE = -1;//无人脸
    public static final int DISCERN_SUCCESS = 0;//识别成功
    public static final int STRANGER = 1;//陌生人
    public static final int WITHOUT_HAT = 2;//未带安全帽
    public static final int PHOTO_VAGUE = 3;//画面模糊
    public static final int DISTANCE_FAR = 4;//距离过远
    public static final int NOT_ALIVE = 5;//活体检测未通过
    public static final int FACE_MAP_IS_NULL = 6;//人脸库为空
    public static final int CARD_INVALID  = 7;//无效卡
    public static final int CARD_VALID = 8;//有效卡
    public static final int SKEWING = 9;//人脸偏移
    public static final int DRAW_RECT = 100;//画人脸框
    //阈值
    public static final float THRESHOLD_AFR = 0.6f;//人脸识别精度阈值 5~10
    public static final int DISTANCE_50 = 390;//人脸识别距离 50cm-390
    public static final int DISTANCE_100 = 210;//1m-210
    public static final int DISTANCE_150 = 140;//1.5m-140
    public static final int DISTANCE_200 = 100;//2m-100
    public static final int DISTANCE_300 = 70;//3m-70
    public static final float THRESHOLD = 0.8f; // 活体检测阈值  默认0.2  需要加大通过率可上调阈值
    public static final int LAPLACIAN_THRESHOLD = 650; //清晰度判断普拉斯阈值  默认1000 往下调为降低标准
    public static final long HTTP_INTERVAL = 10*1000;//接口间隔请求时间
    public static final float HAT_THRESHOLD = 0.5f;//安全帽检测阈值 0.5  降低为放低标准
}
