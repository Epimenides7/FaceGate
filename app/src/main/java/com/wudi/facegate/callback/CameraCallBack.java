package com.wudi.facegate.callback;

import android.graphics.Rect;

import com.wudi.facegate.greenDao.Person;

import java.io.File;

/**
 * 相机数据返回到主线程的处理
 * Created by wudi on 2020/5/8.
 */
public interface CameraCallBack {
    void discernSuccess(File file,String key);//识别成功
    void discernFailed(int code, File file);//识别失败
    void noFace(boolean retainRect);//没有人脸
    void faceMapIsNull();//人脸库为空
    void cardInvalid();//无效卡
    void cardValid(Person person);//刷卡成功
    void haveFace();//有人脸
    void skewing();//人脸偏移
    void changeText(String text);//修改文本
}
