package com.epimenides.myface.mobilefacenet;

import android.graphics.Bitmap;

import com.epimenides.myface.FaceLib;
import com.epimenides.myface.tools.Box;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Epimenides
 * @Package com.epimenides.myface.mobilefacenet
 * @date 2020/8/15 16:11
 * description :
 */
public class RecognitionFace {
   private Box box;
   private Bitmap bitmap;

   public RecognitionFace(Bitmap bitmap, Box box){
       this.bitmap = bitmap;
       this.box    = box;
   }

   // 得到人脸图像的特征值
   public float[] recognize(){
       return new FaceLib().extractFeature(this.getPixelsRGBA(this.bitmap),this.bitmap.getWidth(), this.bitmap.getHeight(), this.box.landmarks);
   }


    // 将人脸的Bitmap 转换成可以用来底层数组
    private byte[] getPixelsRGBA(Bitmap image) {
        // 计算图像由多少个像素点组成
        int bytes =image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);  // 创建一个新的buffer
        image.copyPixelsToBuffer(buffer);                // 将数据赋值给buffer
        byte[] temp = buffer.array();                    // 得到该对象的底层数组
        return temp;
    }
}
