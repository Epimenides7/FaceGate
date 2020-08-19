package com.epimenides.myface.facedetection;


import com.epimenides.myface.tools.Box;

/**
 * 保存人脸检测的结果，并且判断是否出现不和要求的人脸
 * @author kyle-luo
 * @create 2020-06-20-13:09
 */
public class DetectResult {

    private Box box;
    /*
    0:检测成功
    1:图片尺寸过小
    2:图片尺寸过大
    3:没有检测到人脸
    4:图片读取失败
     */
    private int flag;

    public DetectResult(Box box, int flag) {
        this.box = box;
        this.flag = flag;
    }

    public DetectResult(int flag) {
        this.flag = flag;
    }

    public int getFlag() {
        return this.flag;
    }

    public Box getBox() {
        return this.box;
    }
}
