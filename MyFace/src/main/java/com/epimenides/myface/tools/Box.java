package com.epimenides.myface.tools;

import android.graphics.Rect;

/**
 * @author Epimenides
 * @Package com.epimenides.myface.tools
 * @date 2020/8/15 16:12
 * description : 人脸框工具类
 */

public class Box {
    public int[] landmarks = new int[10]; // 人脸的五个landmark值
    private int[] box;                    // 人脸框左上角和右下角的点
    
    
    public Box(){
        box = new int[4];
    }

    public void setBoxLeft(int left) {
        this.box[0] = left;
    }

    public void setBoxTop(int top) {
        this.box[1] = top;
    }

    public void setBoxRight(int right) {
        this.box[2] = right;
    }

    public void setBoxDown(int bottom) {
        this.box[3] = bottom;
    }

    public int width() {
       return this.box[2] - this.box[0] + 1; 
    }

    public int height() {
        return this.box[3] - this.box[1] + 1;
    }

    public int getBoxLeft() { return this.box[0]; }

    public int getBoxTop() { return this.box[1]; }

    public int getBoxRight() {return this.box[2]; }

    public int getBoxDown() { return this.box[3]; }

    public int[] getCenter() { return new int[]{this.box[0]+this.width()/2+200, this.box[1] +this.height()/2+200}; }

    public Rect transform2Rect() {
        Rect rect = new Rect();
        rect.left = box[0];
        rect.top = box[1];
        rect.right = box[2];
        rect.bottom = box[3];
        return rect;
    }
}
