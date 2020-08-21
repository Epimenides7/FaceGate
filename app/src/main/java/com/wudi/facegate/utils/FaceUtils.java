package com.wudi.facegate.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import com.apkfuns.logutils.LogUtils;
import com.epimenides.myface.FaceLib;
import com.epimenides.myface.UltraNet.FaceSDKNative;
import com.epimenides.myface.faceantispoofing.Ir_Fas;
import com.epimenides.myface.facedetection.DetectRegulationFace;
import com.epimenides.myface.facedetection.DetectResult;
import com.epimenides.myface.hat.Helmet;
import com.epimenides.myface.hat.RecognitionHelmet;
import com.epimenides.myface.mobilefacenet.FaceEvaluator;
import com.epimenides.myface.mobilefacenet.RecognitionFace;
import com.epimenides.myface.tools.Box;
import com.epimenides.myface.tools.MtcnnUtil;
import com.wudi.facegate.base.BaseActivity;
import com.wudi.facegate.greenDao.GreenDaoManager;
import com.wudi.facegate.greenDao.Person;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * 人脸操作工具类
 * Created by wudi on 2020/5/19.
 */
public class FaceUtils {
    public volatile static FaceUtils faceUtils;//全局唯一实例

    private Helmet helmet;           //安全帽检测
    private FaceEvaluator faceEvaluator; //人脸特征相关类
    private FaceSDKNative ultraNet = new FaceSDKNative();
    private FaceLib mFace = new FaceLib();
    private static Map<String, float[]> featureMap;//人脸特征库
    private AssetManager assetManager;
    private BaseActivity activity;

    // 初始参数设置，可以按需修改
    private int minFaceSize  = 50;
    private int testTimeCount = 1;
    private int threadsNumber = 2;
    private double threshold = 0.5;            // 人脸余弦距离的阈值
    private Ir_Fas irFas;

    /**
     * 单例获取工具类
     *
     * @param assetManager
     * @return
     */
    public static FaceUtils getInstance(AssetManager assetManager, BaseActivity activity) {
        if (faceUtils == null) {
            synchronized (FaceUtils.class) {
                if (faceUtils == null) {
                    faceUtils = new FaceUtils(assetManager, activity);
                }
            }
        }
        return faceUtils;
    }

    /**
     * 私有构造器
     *
     * @param assetManager
     */
    private FaceUtils(AssetManager assetManager, BaseActivity activity) {
        this.assetManager = assetManager;
        this.activity = activity;
        try {
            faceEvaluator = new FaceEvaluator();
            helmet = new Helmet(activity, 4);
            copyBigDataToSDMNN("RFB-320.mnn", assetManager);
            copyBigDataToSDMNN("RFB-320-quant-ADMM-32.mnn", assetManager);
            copyBigDataToSDMNN("RFB-320-quant-KL-5792.mnn", assetManager);
            copyBigDataToSDMNN("slim-320.mnn", assetManager);
            copyBigDataToSDMNN("slim-320-quant-ADMM-50.mnn", assetManager);

            File sdDirMNN = Environment.getExternalStorageDirectory();//找到模型的存储位置
            String sdPathMNN = sdDirMNN.toString() + "/facesdk/";
            ultraNet.FaceDetectionModelInit(sdPathMNN);


            copyBigDataToSDNCNN("det1.bin", assetManager);
            copyBigDataToSDNCNN("det2.bin", assetManager);
            copyBigDataToSDNCNN("det3.bin", assetManager);
            copyBigDataToSDNCNN("det1.param", assetManager);
            copyBigDataToSDNCNN("det2.param", assetManager);
            copyBigDataToSDNCNN("det3.param", assetManager);
            copyBigDataToSDNCNN("mobilefacenet.bin", assetManager);
            copyBigDataToSDNCNN("mobilefacenet.param", assetManager);

            // mFace加载模型
            File sdDirNCNN = Environment.getExternalStorageDirectory();
            String sdPathNCNN = sdDirNCNN.toString() + "/facem/";
            mFace.FaceDetectionModelInit(sdPathNCNN);

            // 设置线程数量
            Log.i("minface", "最小人脸："+minFaceSize);
            mFace.SetMinFaceSize(minFaceSize);
            mFace.SetTimeCount(testTimeCount);
            mFace.SetThreadsNumber(threadsNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public List<Integer> ultraNetForward(Bitmap irBitmap){
        int width = irBitmap.getWidth();
        int height= irBitmap.getHeight();
        byte[] imageDate = getPixelsRGBA(irBitmap);
        long start = System.currentTimeMillis();
        int[] faceInfo =  ultraNet.FaceDetect(imageDate, width, height,4);
        Log.d("Ir_time", "ir 检测一张人脸耗时:" + (System.currentTimeMillis() - start)+"ms");
        List<Integer> Ir_info = new ArrayList<>();
        //faceInfo第一个元素是有无人脸
        Ir_info.add(faceInfo[0]);
        //如果有人脸就将bbox的坐标值放入list中
        int left, top, right, bottom;
        if (faceInfo.length > 1){
            left  = faceInfo[1];
            top   = faceInfo[2];
            right = faceInfo[3];
            bottom= faceInfo[4];
            Ir_info.add(faceInfo[1]);
            Ir_info.add(faceInfo[2]);
            Ir_info.add(faceInfo[3]);
            Ir_info.add(faceInfo[4]);
            Log.d("ir_detect", "人脸的左上角坐标为："+ "("+left+","+top+")" +"长度为:" + (right-left) + "宽度为:" + (bottom - top));
            Bitmap crop = cropIrBitmap(irBitmap, Ir_info);
            int laplacian = laplacian(crop);
            Log.d("Ir_laplacian", "清晰度为:" + laplacian);
//            if (laplacian < 400){
//                Ir_info.set(0, 0);
//            }
            Log.d("Ir_Result", "单张结果为:" + Ir_info.get(0));
        }
        return Ir_info;
    }

    public Bitmap cropIrBitmap (Bitmap irImage, List<Integer> Ir_info){
        Bitmap Ir_crop = Bitmap.createBitmap(irImage, Ir_info.get(1), Ir_info.get(2), Ir_info.get(3)-Ir_info.get(1), Ir_info.get(4)-Ir_info.get(2));
        return Ir_crop;
    }

    private byte[] getPixelsRGBA(Bitmap irBitmap) {
        // 计算图像由多少字节组成
        int bytes = irBitmap.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // 创建一个新的缓存区域
        irBitmap.copyPixelsToBuffer(buffer);            // 将字节数据移动到缓冲区
        byte[] temp = buffer.array();                   // 对象的底层数组
        return temp;
    }

    public int laplacian(Bitmap bitmap) {
        // 将人脸resize为256X256大小的，因为下面需要feed数据的placeholder的形状是(1, 256, 256, 3)
        Bitmap bitmapScale = Bitmap.createScaledBitmap(bitmap, 256, 256, true);

        double[][] laplace = {{0, 1, 0}, {1, -4, 1}, {0, 1, 0}};
        int size = laplace.length;
        int[][] img = convertGreyImg(bitmapScale);
        int height = img.length;
        int width = img[0].length;

        int score = 0;
        for (int x = 0; x < height - size + 1; x++){
            for (int y = 0; y < width - size + 1; y++){
                int result = 0;
                // 对size*size区域进行卷积操作
                for (int i = 0; i < size; i++){
                    for (int j = 0; j < size; j++){
                        result += (img[x + i][y + j] & 0xFF) * laplace[i][j];
                    }
                }
                if (result > 50) {
                    score++;
                }
            }
        }
        return score;
    }

    private int[][] convertGreyImg(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pixels = new int[h * w];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        int[][] result = new int[h][w];
        int alpha = 0xFF << 24;
        for(int i = 0; i < h; i++)	{
            for(int j = 0; j < w; j++) {
                int val = pixels[w * i + j];

                int red = ((val >> 16) & 0xFF);
                int green = ((val >> 8) & 0xFF);
                int blue = (val & 0xFF);

                int grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                result[i][j] = grey;
            }
        }
        return result;
    }



    public FaceSDKNative getUltraNet(){
        return this.ultraNet;
    }

    private void copyBigDataToSDMNN(String strOutFileName, AssetManager assetManager) throws IOException {
        Log.i(TAG, "start copy file " + strOutFileName);
        File sdDir = Environment.getExternalStorageDirectory();//get root dir
        File file = new File(sdDir.toString()+"/facesdk/");
        if (!file.exists()) {
            file.mkdir();
        }

        String tmpFile = sdDir.toString()+"/facesdk/" + strOutFileName;
        File f = new File(tmpFile);
        if (f.exists()) {
            Log.i(TAG, "file exists " + strOutFileName);
            return;
        }
        InputStream myInput;
        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString()+"/facesdk/"+ strOutFileName);
        myInput = assetManager.open(strOutFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
        Log.i(TAG, "end copy file " + strOutFileName);

    }

    private void copyBigDataToSDNCNN(String strOutFilename, AssetManager assetManager) throws IOException {
        Log.i("文件拷贝", "开始拷贝模型文件" + strOutFilename);
        File sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        File file = new File(sdDir.toString()+"/facem/");
        if (!file.exists()) {
            file.mkdir();
        }

        String tmpFile = sdDir.toString()+"/facem/" + strOutFilename;
        File f = new File(tmpFile);
        if (f.exists()) {
            Log.i("文件拷贝", "文件已经存在"+strOutFilename);
            return;
        }

        InputStream myInput;
        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString()+"/facem/"+strOutFilename);
        myInput = assetManager.open(strOutFilename);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while(length > 0){
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
        Log.i("文件拷贝", "end copy file" + strOutFilename);
    }

    /**
     * 添加人脸特征到人脸库
     *
     * @param id
     * @param feature
     */
    public void addFeatureToMap(String id, float[] feature) {
        if (featureMap == null) {
            featureMap = new HashMap<>();
        }
//        FaceFeature faceFeature = new FaceFeature(feature);
        featureMap.put(id, feature);
    }

    /**
     * 从人脸库中中删除
     * @param id
     */
    public void deleteFeatureToMap(String id){
        if (featureMap == null) {
            featureMap = new HashMap<>();
        }
        featureMap.remove(id);
    }

    /**
     * featureMap
     * bitmap获取特征
     *
     * @param crop
     * @return
     */
    public float[] getFeature(Box box, Bitmap crop) {

        long startRecog = System.currentTimeMillis();
        float[] feature = new RecognitionFace(crop, box).recognize();
        long endRecog   = System.currentTimeMillis();
        Log.d("人脸识别", "人脸识别一帧时间:"+(endRecog - startRecog));
        return feature;
    }




    /**
     * 获取Box
     *
     * @param bitmap
     * @param context
     * @return
     */
    public Box getBox(Bitmap bitmap, Context context) {
        int minWidth = getMinWidth(context);
//        int minWidth = 150;
//        DetectResult detectResult = new DetectRegulationFace(mtcnn, bitmap).detection();
        DetectResult detectResult = null;
        long start = System.currentTimeMillis();
        detectResult = new DetectRegulationFace(bitmap).detection();
        long end = System.currentTimeMillis();
        Log.d("人脸检测时间:", "人脸检测一帧时间:"+(end - start));
//            Log.d("fps", "帧率 " + 1/(end-start ));
        if (detectResult == null) {
            return null;
        }
        if (detectResult.getFlag() == 0) {
            return detectResult.getBox();
        }
        return null;
    }


    /**
     * 获取固定box，初始化获取人脸时使用
     *
     * @param bitmap
     * @return
     */
    public DetectResult getBoxFixed(Bitmap bitmap) {
        DetectResult detectResult = null;
        detectResult = new DetectRegulationFace(bitmap).detection();
        return detectResult;
    }



    /**
     * 获取最小人脸
     *
     * @param context
     * @return
     */
    private int getMinWidth(Context context) {
        float distCd = SPU.getDistCd(context);//获取设置参数最大识别距离
        int minWidth = MyConstants.DISTANCE_100;//默认100
        if (distCd == 0.5f) {
            minWidth = MyConstants.DISTANCE_50;
        } else if (distCd == 1.0f) {
            minWidth = MyConstants.DISTANCE_100;
        } else if (distCd == 1.5f) {
            minWidth = MyConstants.DISTANCE_150;
        } else if (distCd == 2.0f) {
            minWidth = MyConstants.DISTANCE_200;
        } else if (distCd == 3.0f) {
            minWidth = MyConstants.DISTANCE_300;
        }
        return minWidth;
    }

    /**
     * 人脸比对
     *
     * @return 返回人员名称  如果是陌生人返回 stranger
     */
    public String evaluate(Context context, float[] faceFeature) {
        float matchRate = SPU.getMatchRate(context);//准确率
//        float addend = 5f * matchRate;
        if (featureMap == null || featureMap.size() == 0) {
            return "null";
        }
        String name = faceEvaluator.evaluate(featureMap, faceFeature, matchRate);
        return name;
    }

    /**
     * 多帧人脸识别
     *
     * @param context
     * @param features
     * @return
     */
    public String evaluate(Context context, List<float[]> features) {
        LogUtils.d("list长度：" + features.size());
        if (featureMap == null || featureMap.size() == 0) {
            return "null";
        }
        List<Person> personList = GreenDaoManager.getPersonList(context);
        HashMap<Long, String> stringStringHashMap = new HashMap<>();
        for (Person p: personList) {
            stringStringHashMap.put(p.getId(), p.getName());
//            Log.d("myFace", "ID:"+p.getId()+" name:"+p.getName());
        }
        int distance = SPU.getMatchRate(context);
        String name = FaceEvaluator.evaluate(featureMap, stringStringHashMap, features, distance, 0.6f);
        return name;
    }

    /**
     * ir 判断多帧活体
     * @param scoresList
     * @return
     */
    public boolean isAliveIr(List<String> scoresList) {
        LogUtils.d(scoresList);
        String str = irFas.Fas_ir_evaluate(scoresList);
        if (str.equals("Real")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取安全帽佩戴分数
     *
     * @param bitmap
     * @return
     */
    public float getHatScore(Bitmap bitmap, Box box) {
        Bitmap cropHat = MtcnnUtil.cropHat(bitmap, box);
//        Bitmap warpAffineHat = MtcnnUtil.warpAffine(cropHat, box.landmark);
//        com.wudi.facegate.utils.Utils.saveBitmap("/storage/emulated/0/FaceGATE/Record/" , System.currentTimeMillis() + "warp", warpAffineHat);//保存图片到本地

        Helmet helmet = null;
        try {
            helmet = new Helmet(activity, 4);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        return new RecognitionHelmet(helmet, warpAffineHat).recognize()[0];
        return new RecognitionHelmet(helmet, bitmap).recognize()[0];
    }


    /**
     * 多帧检测安全帽
     * @param scores
     * @return
     */
    public boolean haveHat(List<Float> scores) {
        return helmet.haveHat(scores,0.6f);
    }

    /**
     * 判断距离是否超出  经过实际计算 1m人脸宽度为260左右
     *
     * @param faceWidth
     * @return
     */
    public boolean distance(Context context, int faceWidth) {
        int minWidth = getMinWidth(context);
        if (faceWidth < minWidth) {
            //如果人脸宽度小于设置的最小宽度，则距离过远
            return false;
        } else {
            return true;
        }
    }

    public void clear() {

    }

    public Rect getRect(Box box) {
        if (box == null) {
            return null;
        }
        Rect rect = box.transform2Rect();
        return rect;
    }

    public Bitmap cropBitmap(Box box, Bitmap bitmap) {
        Bitmap crop = MtcnnUtil.crop(bitmap, box);//人脸截取
        return crop;
    }

    public boolean isDefinition(Bitmap bitmap) {
        return true;
    }


    public boolean isAliveRGB(Bitmap crop) {
        return true;
    }
}
