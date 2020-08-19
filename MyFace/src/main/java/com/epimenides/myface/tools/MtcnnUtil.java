package com.epimenides.myface.tools;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * @author kyle-luo
 * @create 2020-05-25-21:27
 */
public class MtcnnUtil {

    public static MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * 从assets中读取图片
     *
     * @param context
     * @param filename
     * @return
     */
    public static Bitmap readFromAssets(Context context, String filename) {
        Bitmap bitmap;
        AssetManager asm = context.getAssets();
        try {
            InputStream is = asm.open(filename);
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**
     * 缩放图片
     *
     * @param bitmap (Bitmap)要被缩放的原图片
     * @param scale  (float)缩放系数，将长和宽乘以该系数
     * @return (Bitmap)缩放后的图片
     */
    public static Bitmap bitmapResize(Bitmap bitmap, float scale) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
//        return Bitmap.createBitmap(
//                bitmap, 0, 0, width, height, matrix, true);
//
//        Bitmap.createBitmap(bitmap, box.getBoxLeft(), box.getBoxTop(), box.width(), box.getBoxDown() - box.getBoxTop())

        SoftReference<Bitmap> soft = new SoftReference<>(Bitmap.createBitmap(
                bitmap, 0, 0, width, height, matrix, true));
        return soft.get();
    }

    /**
     * 将图片转换成数组形式，并且归一化到[-1, 1]
     *
     * @param bitmap 将要归一化的位图
     * @return 归一化后并且转换为数组格式的图片
     */
    public static float[][][] normalizeImage(Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        float[][][] floatValues = new float[h][w][3];

        float imageMean = 127.5f;
        float imageStd = 128;

        int[] pixels = new int[h * w];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, w, h);
        for (int i = 0; i < h; i++) { // 注意是先高后宽
            for (int j = 0; j < w; j++) {
                final int val = pixels[i * w + j];
                float r = (((val >> 16) & 0xFF) - imageMean) / imageStd;
                float g = (((val >> 8) & 0xFF) - imageMean) / imageStd;
                float b = ((val & 0xFF) - imageMean) / imageStd;
                float[] arr = {r, g, b};
                floatValues[i][j] = arr;
            }
        }
        return floatValues;
    }

    /**
     * 4维图片batch矩阵宽高转置
     *
     * @param in 需要被转置的数组
     * @return 转置后的矩阵数组
     */
    public static float[][][][] transposeBatch(float[][][][] in) {
        int batch = in.length;
        int h = in[0].length;
        int w = in[0][0].length;
        int channel = in[0][0][0].length;
        float[][][][] out = new float[batch][w][h][channel];
        for (int i = 0; i < batch; i++) {
            for (int j = 0; j < h; j++) {
                for (int k = 0; k < w; k++) {
                    out[i][k][j] = in[i][j][k];
                }
            }
        }
        return out;
    }




    /**
     * 图片矩阵宽高转置
     *
     * @param in 输入的将要被转置的图片
     * @return 转置后的图片
     */
    public static float[][][] transposeImage(float[][][] in) {
        int h = in.length;
        int w = in[0].length;
        int channel = in[0][0].length;
        float[][][] out = new float[w][h][channel];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                out[j][i] = in[i][j];
            }
        }
        return out;
    }

    /**
     * 按照rect的大小裁剪出人脸
     *
     * @param bitmap 原图
     * @param box    要裁剪的矩形框
     * @return Bitmap格式的裁剪后的人脸框
     */
    public static Bitmap crop(Bitmap bitmap, Box box) {
        SoftReference<Bitmap> soft = new SoftReference<>(Bitmap.createBitmap(bitmap, box.getBoxLeft(), box.getBoxTop(), box.width(), box.height()));
        return soft.get();
    }



    public static Bitmap cropHat(Bitmap bitmap, Box box) {

        int top = box.getBoxTop() - box.height() / 2;
        if (top < 0) {
            top = 0;
        }
//        int down = top + box.height();
        SoftReference<Bitmap> soft = new SoftReference<>(Bitmap.createBitmap(bitmap, box.getBoxLeft(), top, box.width(), box.height()));
        return soft.get();
    }

    public static ArrayList<Bitmap> cropAllImg(Bitmap bitmap, ArrayList<Box> boxes) {

        ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();

        for (Box box : boxes) {
            bitmapArrayList.add(Bitmap.createBitmap(bitmap, box.getBoxLeft(), box.getBoxTop(), box.width(), box.getBoxDown() - box.getBoxTop()));
        }

        return bitmapArrayList;
    }

    //在bitmap中画矩形
    public static void drawRect(Bitmap bitmap, Rect rect, int thick) {
        try {
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            int r = 255;//(int)(Math.random()*255);
            int g = 0;//(int)(Math.random()*255);
            int b = 0;//(int)(Math.random()*255);
            paint.setColor(Color.rgb(r, g, b));
            paint.setStrokeWidth(thick);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rect, paint);
            //Log.i("Util","[*]draw rect");
        } catch (Exception e) {
            Log.i("Utils", "[*] error" + e);
        }
    }

    //在图中画点
    public static void drawPoints(Bitmap bitmap, Point[] landmark, int thick) {
        for (Point point : landmark) {
            int x = point.x;
            int y = point.y;
            //Log.i("Utils","[*] landmarkd "+x+ "  "+y);
            drawRect(bitmap, new Rect(x - 1, y - 1, x + 1, y + 1), thick);
        }
    }

}
