package com.epimenides.myface.facedetection;

import android.graphics.Bitmap;

import com.epimenides.myface.FaceLib;
import com.epimenides.myface.tools.Box;

import java.nio.ByteBuffer;

/**
 * @author Epimenides
 * @Package com.epimenides.myface.facedetection
 * @date 2020/8/15 16:23
 * description :
 * 运行时检测人脸
 */
public class DetectRegulationFace {
    private Bitmap bitmap;

    public DetectRegulationFace(Bitmap bitmap){
        this.bitmap = bitmap;
    }

    // 返回检测的结果
    public DetectResult detection(){
        byte[] imageDate = getPixelsRGBA(this.bitmap);
        int[] faceInfo = (new FaceLib()).MaxFaceDetect(imageDate, bitmap.getWidth(), bitmap.getHeight(), 4);
        int left = 0, top = 0, right = 0, bottom = 0;
        for (int i = 0; i < faceInfo[0]; i++) {
            left = faceInfo[1 + 14 * i];
            top = faceInfo[2 + 14 * i];
            right = faceInfo[3 + 14 * i];
            bottom = faceInfo[4 + 14 * i];
        }
        Box box = new Box();
        if (faceInfo[0] == 0) {
            return new DetectResult(0);
        }
        for (int i = 0; i < 10; i++) {
            box.landmarks[i] = faceInfo[5+i];
        }
        box.setBoxLeft(left);
        box.setBoxTop(top);
        box.setBoxRight(right);
        box.setBoxDown(bottom);

        return new DetectResult(box, 0);
    }

    private byte[] getPixelsRGBA(Bitmap image) {
        // 计算图像由多少个像素点组成
        int bytes =image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);  // 创建一个新的buffer
        image.copyPixelsToBuffer(buffer);                // 将数据赋值给buffer
        byte[] temp = buffer.array();                    // 得到该对象的底层数组
        return temp;
    }
}
