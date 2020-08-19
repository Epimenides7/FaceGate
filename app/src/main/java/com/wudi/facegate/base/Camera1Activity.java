package com.wudi.facegate.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Picture;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.FaceDetector;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.apkfuns.logutils.LogUtils;
import com.epimenides.myface.tools.Box;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.wudi.facegate.R;
import com.wudi.facegate.callback.CameraCallBack;
import com.wudi.facegate.greenDao.GreenDaoManager;
import com.wudi.facegate.greenDao.Person;
import com.wudi.facegate.module.ShowLogDTO;
import com.wudi.facegate.utils.DataUtils;
import com.wudi.facegate.utils.FaceUtils;
import com.wudi.facegate.utils.MyConstants;
import com.wudi.facegate.utils.NV21ToBitmap;
import com.wudi.facegate.utils.SPU;
import com.wudi.facegate.utils.Utils;
import com.wudi.facegate.view.FaceView;
import com.wudi.facegate.view.IdentifyAreasView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Mac;

import butterknife.BindView;
import cn.com.armt.sdk.hardware.Wiegand;

import static java.lang.Thread.currentThread;

/**
 * 封装了camera1自定义camera的操作
 * Created by wudi on 2020/6/6.
 */

public abstract class Camera1Activity extends HttpActivity implements CameraCallBack {
    @BindView(R.id.rgb)
    SurfaceView rgbSurface;
    @BindView(R.id.iv)
    ImageView iv;
    @BindView(R.id.iav)
    IdentifyAreasView iav;
    @BindView(R.id.fv)
    protected FaceView faceView;

    private Camera.Size previewSize;//预览尺寸
    private int rgbCameraId;//rgb相机id
    private int irCameraId;//ir相机id
    private Camera.CameraInfo rgbCameraInfo;//rgb摄像头详情
    private Camera.CameraInfo irCameraInfo;//ir摄像头详情
    private Camera rgbCamera;//rgb相机
    private Camera irCamera;//ir相机
    private SurfaceHolder rgbHolder;
    private NV21ToBitmap nv21ToBitmap;
    private FaceUtils faceUtils;//人脸工具类
    private float scaleWidth;//宽度比例
    private float scaleHeight;//高度比例
    private CameraHandler cameraHandler;//运行在主线程中 处理相机返回数据
    private String lastFaceKey = "";//上个人脸key
    private long lastFaceTime = 0;//上个人脸时间
    private ExecutorService faceExecutors;//人脸处理线程池
    private ExecutorService aliveExecutor;//活体检测线程池
    private ExecutorService hatExecutor;//安全帽检测线程池
    private ExecutorService cardExecutors;//刷卡处理线程池
    private ExecutorService recognizeFaceExecutor; // 人脸识别线程
    private float[] rightArea;//正确识别区域
    private List<float[]> featureCacheList = new ArrayList<>();//特征列表
    private List<Box> boxCacheList = new ArrayList<>(); //人脸框列表
    private List<String> irCacheList = new ArrayList<>();//活体检测结果列表
    private List<Float> hatCacheList = new ArrayList<>();//安全帽检测结果列表
    private long firstFPSTime;//第一帧时间
    private boolean frameSwitch;//帧处理开关
    private Timer httpTimer;//接口请求定时器
    private OpenDoorThread openDoorThread;//开门线程
    private int picNum; //图片数量
    private Lock lock = new ReentrantLock();
    private Condition notEmptyCondition = lock.newCondition();
    private Condition notFullCondition = lock.newCondition();


    private class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                rgbCamera.setPreviewDisplay(holder);
                rgbCamera.setDisplayOrientation(270);//设置旋转角度
                rgbCamera.startPreview();
//                irCamera.setPreviewDisplay(holder);
//                irCamera.setDisplayOrientation(270);
//                irCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            rgbCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (rgbCamera == null) {
                        return;
                    }
                    if (frameSwitch) {
                        frameSwitch = false;
                        faceExecutors.execute(new DetectFaceThread(data, Camera1Activity.this));
                        rgbCamera.addCallbackBuffer(data);
                    }

                }
            });
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    }


    /**
     * 人脸检测线程类
     * 在设定的时间内进行人脸检测
     * 将检测到的人脸框坐标返回，并画在ui上
     */
    static class DetectFaceThread extends Thread {

        private byte[] data; // 摄像机获取的图像数据
        private WeakReference<Camera1Activity> reference;

        DetectFaceThread(byte[] data, Camera1Activity reference) {
            this.data = data;
            this.reference = new WeakReference<>(reference);
        }

        @Override
        public void run() {
            if (SPU.getMode(reference.get()) == 1 || SPU.getMode(reference.get()) == 4) {
                //当前模式为1刷卡 或者4关闭  不处理回调操作
                reference.get().nextImage(data);
                return;
            }

            int faceTime = SPU.getMatchTime(reference.get());
            int picNum = faceTime * 3;
            if (picNum > 3) {
                picNum = picNum - 1;
            }

            long start = System.currentTimeMillis();
            // 获取bitmap图像
            Bitmap bitmap = this.getBitmap();
            // 获取人脸框，如果没有检测到人脸，则为null
            Box box = reference.get().faceUtils.getBox(bitmap, reference.get());

            long end = System.currentTimeMillis();

//            Log.d("myFace", "     看看多少: " + (end - start));

            // 未检测到人脸
            if (box == null) {
                Message message = Message.obtain();
                message.what = MyConstants.NO_FACE;
                reference.get().cameraHandler.sendMessage(message);
                reference.get().clearFaceCache();
                reference.get().boxCacheList.clear();
                reference.get().hatCacheList.clear();
                reference.get().nextImage(data);
                return;
            } else {
                if (reference.get().isInRightArea(box)) {
                    //在正确区域内
                    LogUtils.d("有人脸！！！");
                    double area = box.width() * box.height();
                    Log.d("MTCNN_Area", "面积:" + area);
                    if (area < 150000) {
                        Message message = Message.obtain();
                        message.what = MyConstants.NO_FACE;
                        reference.get().cameraHandler.sendMessage(message);
                        reference.get().clearFaceCache();
                        reference.get().boxCacheList.clear();
                        reference.get().hatCacheList.clear();
                        reference.get().nextImage(data);
                        return;
                    }
                }
                else {
                    //在错误区域内
                    Message message = Message.obtain();
                    message.what = MyConstants.SKEWING;
                    reference.get().cameraHandler.sendMessage(message);
                    reference.get().nextImage(data);
                    return;
                }
            }

            // 检测到人脸后画出人脸框
            this.drawRect(box);

            // 丢弃第一帧图片
            if (reference.get().firstFPSTime == 0) {
                reference.get().boxCacheList.clear();
                reference.get().firstFPSTime = System.currentTimeMillis();
                reference.get().nextImage(data);
                return;
            }


            // 从第二帧图片开始存储下来
            reference.get().boxCacheList.add(box);

            int listSize = reference.get().boxCacheList.size();


            if (listSize > 1) {
                int[] center1 = reference.get().boxCacheList.get(listSize - 1).getCenter();
                int[] center2 = reference.get().boxCacheList.get(listSize - 2).getCenter();
                int xChange = Math.abs(center1[0] - center2[0]);
                int yChange = Math.abs(center1[1] - center2[1]);
                double change = Math.sqrt(Math.pow(xChange, 2) + Math.pow(yChange, 2));


                Log.d("centerChange", "两帧之间的变化量为:" + change);
                if (change > 100) {
                    Message message = Message.obtain();
                    message.what = MyConstants.NO_FACE;
                    reference.get().cameraHandler.sendMessage(message);
                    reference.get().clearFaceCache();
                    reference.get().boxCacheList.clear();
                    reference.get().hatCacheList.clear();
                    reference.get().nextImage(data);
                    return;
                }
            }


            // 安全帽线程
            reference.get().hatExecutor.execute(new HatThread(bitmap, reference.get(), box));

            // 人脸识别线程
            reference.get().recognizeFaceExecutor.execute(new RecognizeFaceThread(bitmap, reference.get(), box));

//            reference.get().aliveExecutor.execute(new AliveThread(data, reference.get()));
            reference.get().irCameraTakePicture(false, box);
            /*
            如果当前存储的人脸少于时间数乘以3，则继续人脸检测
             */
            if (reference.get().boxCacheList.size() < picNum) {
                //没有超过识别时间
                reference.get().nextImage(data);
            } else {
//                Log.d("myMTCNN", "检测到的人脸数量是：" + reference.get().boxCacheList.size() + "时间：" + (System.currentTimeMillis() - reference.get().firstFPSTime));

                while (true) {
                    if (reference.get().featureCacheList.size() >= picNum) {
                        break;
                    }
                }
                Log.d("myMTCNN", "检测到的人脸数量是：" + reference.get().boxCacheList.size() + "时间：" + (System.currentTimeMillis() - reference.get().firstFPSTime));
                reference.get().firstFPSTime = 0;
                List<float[]> featureList = new ArrayList<>(reference.get().featureCacheList);
                List<Float> hatList = new ArrayList<>(reference.get().hatCacheList);
                List<String> irList = new ArrayList<>(reference.get().irCacheList);
                reference.get().boxCacheList.clear(); // 清空人脸检测的列表
                reference.get().hatCacheList.clear(); // 清空安全帽的列表
                reference.get().featureCacheList.clear(); // 清空人脸识别的列表

                File file = Utils.saveBitmap(reference.get().recordDir.getPath(), System.currentTimeMillis() + "", bitmap);//保存图片到本地

                // 人脸比对
                String key = reference.get().faceUtils.evaluate(reference.get(), featureList);

                if (key.equals("stranger")) {
                    //陌生人
                    Message msg = Message.obtain();
                    msg.what = MyConstants.STRANGER;
                    msg.obj = file;
                    reference.get().cameraHandler.sendMessage(msg);//陌生人
                    reference.get().nextImage(data);
                    return;
                }

                if (key.equals("null")) {
                    //人脸库为空
                    Message msg = Message.obtain();
                    msg.what = MyConstants.FACE_MAP_IS_NULL;
                    reference.get().cameraHandler.sendMessage(msg);
                    reference.get().nextImage(data);
                    reference.get().deleteFile(file);
                    return;
                }
//            时间窗检测
                if (key.equals(reference.get().lastFaceKey)) {
                    //当前脸与上个脸相同
                    int timeWindow = SPU.getTimeWindow(reference.get());
                    long milliSecond = ((long) timeWindow) * 60l * 1000l;
                    if (reference.get().lastFaceTime + milliSecond > System.currentTimeMillis()) {
                        //还在时间窗内，屏蔽本次,做无人脸处理
                        Message msg = Message.obtain();
                        msg.what = MyConstants.NO_FACE;
                        reference.get().cameraHandler.sendMessage(msg);
                        reference.get().deleteFile(file);
                        reference.get().nextImage(data);
                        reference.get().deleteFile(file);
                        return;
                    }
                }

                if (SPU.getHelmetSwitch(reference.get()) != 0) {
                    //安全帽检测未关闭
                    if (!reference.get().faceUtils.haveHat(hatList)) {
                        //没有安全帽
                        Message msg = Message.obtain();
                        msg.what = MyConstants.WITHOUT_HAT;
                        msg.obj = file;
                        reference.get().cameraHandler.sendMessage(msg);//安全帽检测未通过
                        reference.get().nextImage(data);
                        return;
                    }
                }
//                reference.get().lastFaceKey = key;//记录时间窗
//                reference.get().lastFaceTime = System.currentTimeMillis();

                if (SPU.getAliveSwitch(reference.get()) == 0) {
                    //活体检测关闭,直接通过
                    Message msg = Message.obtain();
                    msg.what = MyConstants.DISCERN_SUCCESS;
                    msg.obj = file;
                    Bundle bundle = new Bundle();
                    bundle.putString(MyConstants.KEY, key);
                    msg.setData(bundle);
                    reference.get().cameraHandler.sendMessage(msg);
                    reference.get().nextImage(data);
                    return;
                }

                if (reference.get().faceUtils.isAliveIr(irList)) {
                    //活体检测通过
                    Message msg = Message.obtain();
                    msg.what = MyConstants.DISCERN_SUCCESS;
                    msg.obj = file;
                    Bundle bundle = new Bundle();
                    bundle.putString(MyConstants.KEY, key);
                    msg.setData(bundle);
                    reference.get().cameraHandler.sendMessage(msg);//活体通过
                    reference.get().lastFaceKey = key;//记录时间窗
                    reference.get().lastFaceTime = System.currentTimeMillis();
                } else {
                    //活体检测未通过
                    Message msg = Message.obtain();
                    msg.what = MyConstants.NOT_ALIVE;
                    msg.obj = file;
                    reference.get().cameraHandler.sendMessage(msg);//活体未通过
                }

                reference.get().nextImage(data);
            }

        }

        /**
         * 将data数据转换成bitmap
         *
         * @return 该帧对应的bitmap
         */
        private Bitmap getBitmap() {
            return reference.get().nv21ToBitmap.nv21ToBitmap(this.data,
                    reference.get().previewSize.width, reference.get().previewSize.height);
        }

        /**
         * 绘制ui，将检测到的人脸框在界面画出来
         *
         * @param box
         */
        private void drawRect(Box box) {

            List<RectF> rectFList = new ArrayList<>();
            float top = box.getBoxTop() * reference.get().scaleWidth;
            float bottom = box.getBoxDown() * reference.get().scaleWidth;
            float left = box.getBoxLeft() * reference.get().scaleHeight;
            float right = box.getBoxRight() * reference.get().scaleHeight;

            rectFList.add(new RectF(left, top, right, bottom));

            Message message = Message.obtain();
            message.what = MyConstants.DRAW_RECT;
            message.obj = rectFList;
            reference.get().cameraHandler.sendMessage(message); // 画矩形框
//            reference.get().nextImage(data);
        }
    }

    /**
     * 人脸特征提取线程类
     */
    static class RecognizeFaceThread extends Thread {
        private Bitmap bitmap;
        private WeakReference<Camera1Activity> reference;
        private Box box;

        public RecognizeFaceThread(Bitmap bitmap, Camera1Activity activity, Box box) {
            this.bitmap = bitmap;
            this.reference = new WeakReference<>(activity);
            this.box = box;
        }

        @Override
        public void run() {
//            Bitmap crop = reference.get().faceUtils.cropBitmap(box, bitmap);
            long recog_start = System.currentTimeMillis();
            float[] feature = reference.get().faceUtils.getFeature(box, bitmap);
            long recog_end  = System.currentTimeMillis();
            Log.d("识别一张的时间", "识别时间:"+(recog_end - recog_start));
            reference.get().featureCacheList.add(feature);
        }
    }


    /**
     * 获取下一帧
     */
    private void nextImage(byte[] data) {
        frameSwitch = true;
        if (rgbCamera != null) {
            rgbCamera.addCallbackBuffer(data);
        }
    }

    /**
     * 清除人脸缓存，重置
     */
    private void clearFaceCache() {
        firstFPSTime = 0;
        featureCacheList.clear();
        irCacheList.clear();
        hatCacheList.clear();
    }


    /**
     * 检测人脸是否在正确区域内
     *
     * @return
     */
    private boolean isInRightArea(Box box) {
        float top = box.getBoxTop() * scaleWidth;
        float bottom = box.getBoxDown() * scaleWidth;
        float left = box.getBoxLeft() * scaleHeight;
        float right = box.getBoxRight() * scaleHeight;

        float lineCenterX = 400f;
        float lineCenterY = 500f;
        float faceCenterX = left + ((right - left) / 2);
        float faceCenterY = bottom - ((bottom - top) / 2);
        if (faceCenterX < lineCenterX - 50 || faceCenterX > lineCenterX + 50) {
            return false;
        }
        if (faceCenterY < lineCenterY - 50 || faceCenterY > lineCenterY + 50) {
            return false;
        }
        return true;
    }

    /**
     * 开门线程
     */
    static class OpenDoorThread extends Thread {
        private WeakReference<Camera1Activity> reference;
        private int type;
        private String cardNo;

        public OpenDoorThread(Camera1Activity activity, int type, String cardNo) {
            this.reference = new WeakReference<>(activity);
            this.type = type;
            this.cardNo = cardNo;
        }

        @Override
        public void run() {
            super.run();
            LogUtils.d("进入开门方法！！！");
            if (type == 0 && SPU.getSwitchSignal(reference.get()) == 0) {
                //开门信号关
                return;
            }
            if (type == 1 && SPU.getStrangerSwitchSignal(reference.get()) == 0) {
                //陌生人开门信号关
                return;
            }
            Intent it = new Intent("com.android.intent.OpenRelay");//继电器开门操作
            reference.get().sendBroadcast(it);
            reference.get().sendWG(type, cardNo);
            long mill = (long) (SPU.getRelayTime(reference.get()) * 1000);
            try {
                Thread.sleep(mill);
                it = new Intent("com.android.intent.CloseRelay");//继电器开门操作
                reference.get().sendBroadcast(it);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送韦根信号
     *
     * @param type
     * @param cardNo
     */
    private void sendWG(int type, String cardNo) {
        if (type == 0 && SPU.getWiegandSignal(mActivity) == 0) {
            //韦根信号关
            return;
        }
        if (type == 1 && SPU.getStrangerWiegandSignal(mActivity) == 0) {
            //陌生人韦根信号关
            return;
        }
        if (cardNo == null || cardNo.isEmpty()) {
            return;
        }
        //韦根输出打开,发送韦根信号
        List<Integer> octList = DataUtils.hex2OctList(cardNo);//转换为10进制列表，高位在前
        String wg34 = DataUtils.octList2BinStr(octList);//获取韦根34的中间32位
        octList.remove(0);//删除最高位卡号字节
        String wg26 = DataUtils.octList2BinStr(octList);//获取韦根26中间24位
        Wiegand.wiegandWrite(Wiegand.WG_34_MODE, DataUtils.getChars(wg34.getBytes()));//发送韦根34
        Wiegand.wiegandWrite(Wiegand.WG_26_MODE, DataUtils.getChars(wg26.getBytes()));//发送韦根26
    }


    /**
     * 人脸耗时操作处理线程
     */
    static class FaceThread extends Thread {
        private byte[] data;
        private WeakReference<Camera1Activity> reference;

        public FaceThread(byte[] data, Camera1Activity activity) {
            this.data = data;
            this.reference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            super.run();
            if (SPU.getMode(reference.get()) == 1 || SPU.getMode(reference.get()) == 4) {
                //当前模式为1刷卡 或者4关闭  不处理回调操作
                reference.get().nextImage(data);
                return;
            }

            Bitmap bitmap = reference.get().nv21ToBitmap.nv21ToBitmap(data,
                    reference.get().previewSize.width, reference.get().previewSize.height);
            Utils.saveBitmap(reference.get().recordDir.getPath(), System.currentTimeMillis() + "", bitmap);//保存图片到本地
            Log.d("PATH", "run: " + reference.get().recordDir.getPath());
            Box box = reference.get().faceUtils.getBox(bitmap, reference.get());
            Rect rect = reference.get().faceUtils.getRect(box);//截取人脸坐标
            if (rect == null) {
                //未捕获到人脸
                Message message = Message.obtain();
                message.what = MyConstants.NO_FACE;
                reference.get().cameraHandler.sendMessage(message);
                reference.get().clearFaceCache();
                reference.get().nextImage(data);
                return;
            }
            List<RectF> rectFList = new ArrayList<>();
            float top = rect.top * reference.get().scaleWidth;
            float bottom = rect.bottom * reference.get().scaleWidth;
            float left = rect.left * reference.get().scaleHeight;
            float right = rect.right * reference.get().scaleHeight;
            int faceWidth = (int) (right - left);//计算人脸宽度
            boolean isInArea = reference.get().isInRightArea(box);
            if (isInArea) {
                rectFList.add(new RectF(left, top, right, bottom));
            }
            Message message = Message.obtain();
            message.what = MyConstants.DRAW_RECT;
            message.obj = rectFList;
            reference.get().cameraHandler.sendMessage(message);//画矩形

            if (!isInArea) {
                //没有在正确的区域内
                reference.get().clearFaceCache();
                reference.get().nextImage(data);
            }
            //距离判断
            if (!reference.get().faceUtils.distance(reference.get(), faceWidth)) {
                //人脸太小  说明距离过远
                Message msg = Message.obtain();
                msg.what = MyConstants.DISTANCE_FAR;
                File file = Utils.saveBitmap(reference.get().recordDir.getPath(), System.currentTimeMillis() + "", bitmap);//保存图片到本地
                Log.d("PATH", "run: " + reference.get().recordDir.getPath());
                msg.obj = file;
                reference.get().cameraHandler.sendMessage(msg);//距离过远
                reference.get().clearFaceCache();
                reference.get().nextImage(data);
                return;
            }

            if (reference.get().firstFPSTime == 0) {
                reference.get().firstFPSTime = System.currentTimeMillis();//记录第一帧时间,并丢弃本帧
                reference.get().nextImage(data);
                reference.get().haveFace();
                return;
            }
            reference.get().irCameraTakePicture(false, box);//获取ir帧
            reference.get().hatExecutor.execute(new HatThread(bitmap, reference.get(), box));//检查安全帽
//            Bitmap crop = reference.get().faceUtils.cropBitmap(box, bitmap);
//          Utils.saveBitmap(reference.get().recordDir.getPath(), "RGB_crop"+System.currentTimeMillis(), crop);//保存图片到本地
//            float[] feature = reference.get().faceUtils.getFeature(box, crop);//获取人脸特征值
//            reference.get().featureCacheList.add(feature);//添加特征值帧记录
            reference.get().recognizeFaceExecutor.execute(new RecognizeFaceThread(bitmap, reference.get(), box));

            long faceTime = SPU.getMatchTime(reference.get()) * 1000;//获取识别时间
            if (System.currentTimeMillis() - reference.get().firstFPSTime < faceTime) {
//                没有超过识别时间
                reference.get().nextImage(data);
                return;
            }
            List<float[]> featureList = new ArrayList<>();
            List<String> irList = new ArrayList<>();
            List<Float> hatList = new ArrayList<>();
            featureList.addAll(reference.get().featureCacheList);
            irList.addAll(reference.get().irCacheList);
            hatList.addAll(reference.get().hatCacheList);
            //清除人脸特征与活体检测缓存列表
            reference.get().clearFaceCache();

            File file = Utils.saveBitmap(reference.get().recordDir.getPath(), System.currentTimeMillis() + "", bitmap);//保存图片到本地
            String key = reference.get().faceUtils.evaluate(reference.get(), featureList);

            if (key.equals("stranger")) {
                //陌生人
                Message msg = Message.obtain();
                msg.what = MyConstants.STRANGER;
                msg.obj = file;
                reference.get().cameraHandler.sendMessage(msg);//陌生人
                reference.get().nextImage(data);
                return;
            }

            if (key.equals("null")) {
                //人脸库为空
                Message msg = Message.obtain();
                msg.what = MyConstants.FACE_MAP_IS_NULL;
                reference.get().cameraHandler.sendMessage(msg);
                reference.get().nextImage(data);
                reference.get().deleteFile(file);
                return;
            }

//            时间窗检测
            if (key.equals(reference.get().lastFaceKey)) {
                //当前脸与上个脸相同
                int timeWindow = SPU.getTimeWindow(reference.get());
                long milliSecond = ((long) timeWindow) * 60l * 1000l;
                if (reference.get().lastFaceTime + milliSecond > System.currentTimeMillis()) {
                    //还在时间窗内，屏蔽本次,做无人脸处理
                    Message msg = Message.obtain();
                    msg.what = MyConstants.NO_FACE;
                    reference.get().cameraHandler.sendMessage(msg);
                    reference.get().deleteFile(file);
                    reference.get().nextImage(data);
                    reference.get().deleteFile(file);
                    return;
                }
            }

            if (SPU.getHelmetSwitch(reference.get()) != 0) {
                //安全帽检测未关闭
                if (!reference.get().faceUtils.haveHat(hatList)) {
                    //没有安全帽
                    Message msg = Message.obtain();
                    msg.what = MyConstants.WITHOUT_HAT;
                    msg.obj = file;
                    reference.get().cameraHandler.sendMessage(msg);//安全帽检测未通过
                    reference.get().nextImage(data);
                    return;
                }
            }

            if (SPU.getAliveSwitch(reference.get()) == 0) {
                //活体检测关闭,直接通过
                Message msg = Message.obtain();
                msg.what = MyConstants.DISCERN_SUCCESS;
                msg.obj = file;
                Bundle bundle = new Bundle();
                bundle.putString(MyConstants.KEY, key);
                msg.setData(bundle);
                reference.get().cameraHandler.sendMessage(msg);
                reference.get().nextImage(data);
                return;
            }
            if (reference.get().faceUtils.isAliveIr(irList)) {
                //活体检测通过
                Message msg = Message.obtain();
                msg.what = MyConstants.DISCERN_SUCCESS;
                msg.obj = file;
                Bundle bundle = new Bundle();
                bundle.putString(MyConstants.KEY, key);
                msg.setData(bundle);
                reference.get().cameraHandler.sendMessage(msg);//活体通过
                reference.get().lastFaceKey = key;//记录时间窗
                reference.get().lastFaceTime = System.currentTimeMillis();
            } else {
                //活体检测未通过
                Message msg = Message.obtain();
                msg.what = MyConstants.NOT_ALIVE;
                msg.obj = file;
                reference.get().cameraHandler.sendMessage(msg);//活体未通过
            }
            reference.get().nextImage(data);
        }
    }

    /**
     * 安全帽检测线程
     */
    static class HatThread extends Thread {
        private Bitmap bitmap;
        private WeakReference<Camera1Activity> reference;
        private Box box;

        public HatThread(Bitmap bitmap, Camera1Activity activity, Box box) {
            this.bitmap = bitmap;
            this.reference = new WeakReference<>(activity);
            this.box = box;
        }

        @Override
        public void run() {
            super.run();
            float hatScore = reference.get().faceUtils.getHatScore(bitmap, box);
            reference.get().hatCacheList.add(hatScore);
        }
    }

    /**
     * 活体检测处理线程
     */
    static class AliveThread extends Thread {
        private byte[] data;
        private WeakReference<Camera1Activity> reference;

        public AliveThread(byte[] data, Camera1Activity activity) {
            this.data = data;
            this.reference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            super.run();
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            bitmap = reference.get().nv21ToBitmap.rotateBitmap(bitmap, 270);
            bitmap = Bitmap.createBitmap(bitmap, 300, 700, 650, 800);
            List<Integer> ret = FaceUtils.faceUtils.ultraNetForward(bitmap);
            Utils.saveBitmap(reference.get().recordDir.getPath(), "IR:" + System.currentTimeMillis(), bitmap);//保存图片到本地

            if (ret.get(0) == 0) {
                reference.get().irCacheList.add("False");
                return;
            } else {
                reference.get().irCacheList.add("True");
                Bitmap crop = reference.get().faceUtils.cropIrBitmap(bitmap, ret);
                Utils.saveBitmap(reference.get().recordDir.getPath(), "IR_crop" + System.currentTimeMillis(), crop);
            }
        }
    }

    //运行在Main线程的Handler  处理相机返回数据
    static class CameraHandler extends Handler {
        private WeakReference<Camera1Activity> reference;//弱引用activity

        public CameraHandler(Camera1Activity activity) {
            this.reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MyConstants.DRAW_RECT:
                    //画人脸框
                    List<RectF> rectFList = (List<RectF>) msg.obj;
                    reference.get().setFace(rectFList);
                    reference.get().changeText("检测到人脸，正在识别...");
                    break;
                case MyConstants.FACE_MAP_IS_NULL:
                    //人脸库为空
                    reference.get().faceMapIsNull();
                    break;
                case MyConstants.DISCERN_SUCCESS:
                    //识别成功
                    File f0 = (File) msg.obj;
                    String key = msg.getData().getString(MyConstants.KEY);
                    reference.get().discernSuccess(f0, key);
                    break;
                case MyConstants.NO_FACE:
                    //无人脸
                    reference.get().noFace(false);
                    break;
                case MyConstants.CARD_INVALID:
                    //无效卡
                    reference.get().cardInvalid();
                    break;
                case MyConstants.CARD_VALID:
                    //刷卡成功
                    Person person = (Person) msg.getData().getSerializable(MyConstants.KEY);
                    reference.get().cardValid(person);
                    break;
                case MyConstants.SKEWING:
                    //人脸偏移
                    reference.get().skewing();
                    break;
                default:
                    //识别失败
                    File f1 = (File) msg.obj;
                    reference.get().discernFailed(msg.what, f1);
                    break;
            }
        }
    }

    /**
     * 画人脸
     *
     * @param rectFList
     */
    private void setFace(List<RectF> rectFList) {
        faceView.setFaces(rectFList);
    }

    @Override
    protected void init() {
        initUsb();
        cameraHandler = new CameraHandler(Camera1Activity.this);
        initCamera();
    }

    /**
     * 计算比列
     */
    private void countXY() {
        float widthFV = (float) previewSize.width;
        float heightFV = (float) previewSize.height;
        float heightImg = 800;
        float widthImg = 999;
        scaleWidth = widthImg / widthFV;
        scaleHeight = heightImg / heightFV;
    }

    /**
     * 初始化相机
     */
    private void initCamera() {
        nv21ToBitmap = new NV21ToBitmap(mActivity);
        rgbCameraId = 0;
        irCameraId = 1;
        rgbCameraInfo = new Camera.CameraInfo();
        irCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(rgbCameraId, rgbCameraInfo);
        Camera.getCameraInfo(irCameraId, irCameraInfo);
    }

    /**
     * ir相机帧截取
     */
    private void irCameraTakePicture(final boolean isDiscard, final Box box) {
        if (irCamera != null) {
            irCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    if (isDiscard) {
                        //ir拍照第一帧丢弃，失真
                        return;
                    }
                    aliveExecutor.execute(new AliveThread(data, Camera1Activity.this));
                }
            });
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        frameSwitch = true;
        faceUtils = FaceUtils.getInstance(getAssets(), mActivity);
        faceExecutors = Executors.newFixedThreadPool(10);
        recognizeFaceExecutor = Executors.newFixedThreadPool(5);
        aliveExecutor = Executors.newFixedThreadPool(1);
        hatExecutor = Executors.newFixedThreadPool(10);
        openCamera();
        startTimerHttp();
    }

    @Override
    protected void onPause() {
        super.onPause();
        frameSwitch = false;
        if (faceExecutors != null) {
            faceExecutors.shutdown();
            faceExecutors = null;
        }
        closeCamera();
        faceUtils.clear();
        if (httpTimer != null) {
            httpTimer.cancel();
        }
    }

    /**
     * 开始循环接口请求
     */
    private void startTimerHttp() {
        httpTimer = new Timer();
        TimerTask httpTask = new TimerTask() {
            @Override
            public void run() {
                startHttp();
            }
        };
        httpTimer.schedule(httpTask, MyConstants.HTTP_INTERVAL, MyConstants.HTTP_INTERVAL);
    }

    /**
     * 开启相机
     */
    private void openCamera() {
        rgbCamera = Camera.open(rgbCameraId);
        irCamera = Camera.open(irCameraId);
        setParameters();
        rgbHolder = rgbSurface.getHolder();
        rgbHolder.addCallback(new SurfaceCallback());
        irCameraTakePicture(true, null);//ir第一帧拍照丢掉，图像失真
    }

    /**
     * 设置参数
     */
    @SuppressLint("WrongConstant")
    private void setParameters() {
        int previewFormat = ImageFormat.NV21;//预览数据格式
        Camera.Parameters parameters = rgbCamera.getParameters();
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        for (Camera.Size size : sizeList) {
            float ratio = (float) size.width / (float) size.height;
        //构造方法的字符格式这里如果小数不足2位,会以0补足
            DecimalFormat decimalFormat = new DecimalFormat(".00");
        //format 返回的是字符串
            String p = decimalFormat.format(ratio);
            if (p.equals("1.33") && size.width > 640) {
                previewSize = size;
                LogUtils.d(previewSize.width + "," + previewSize.height);
                break;
            }
        }
        parameters.setPreviewFormat(previewFormat);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        try {
            rgbCamera.setParameters(parameters);
            parameters.setPreviewFormat(ImageFormat.FLEX_RGBA_8888);
//            parameters.setPictureSize(previewSize.width, previewSize.height);
            irCamera.setParameters(parameters);
        } catch (RuntimeException e) {
            LogUtils.d("捕获到设置失败异常！");
        }
        PixelFormat pixelFormat = new PixelFormat();
        PixelFormat.getPixelFormatInfo(previewFormat, pixelFormat);
        int bufferSize = ((previewSize.width * previewSize.height * pixelFormat.bitsPerPixel) / 8) + 1;
        rgbCamera.addCallbackBuffer(new byte[bufferSize]);//设置缓存
        countXY();
    }


    /**
     * 画面旋转
     *
     * @param cameraInfo
     * @return
     */
    private int getCameraDisplayOrientation(Camera.CameraInfo cameraInfo) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 关闭相机
     */
    private void closeCamera() {
        if (rgbCamera != null) {
            rgbCamera.stopPreview();
            rgbCamera.release();
            rgbCamera = null;
        }
        if (irCamera != null) {
            irCamera.stopPreview();
            irCamera.release();
            irCamera = null;
        }
    }

    /**
     * 初始化usb读卡器
     */
    private void initUsb() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            LogUtils.d("无USB设备！");
            return;
        }
        UsbSerialDriver driver = availableDrivers.get(0);//获取usb驱动
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());//获取usb设备连接
        if (connection == null) {
            //如果为空，在此处申请usb权限
            LogUtils.d("无USB权限！");
            return;
        }
        UsbSerialPort port = driver.getPorts().get(0);//获取断点  大多数设备只有一个端点
        try {
            port.open(connection);//打开端口
            //设置串口的波特率、数据位，停止位，校验位
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        cardExecutors = Executors.newSingleThreadExecutor();
        cardExecutors.submit(new SerialInputOutputManager(port, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
                if (SPU.getMode(mActivity) == 0 || SPU.getMode(mActivity) == 4) {
                    //当前模式为0刷脸 或者4关闭  不处理回调操作
                    return;
                }
                String hexString = DataUtils.byteArrayToHex(data).toUpperCase();
                if (hexString.length() == 14) {
                    String cardNo = hexString.substring(4, 12);//获取4字节卡号
                    slotCard(cardNo);
                }
            }

            @Override
            public void onRunError(Exception e) {
                LogUtils.e(e);
            }
        }));
    }

    /**
     * 刷卡后操作
     *
     * @param cardNo
     */
    private void slotCard(String cardNo) {
        List<Person> personList = GreenDaoManager.getPersonByNumber(mActivity, cardNo);
        if (personList == null || personList.size() == 0) {
            //无效卡
            Message msg = Message.obtain();
            msg.what = MyConstants.CARD_INVALID;
            cameraHandler.sendMessage(msg);
        } else {
            //查询到卡号
            Person person = personList.get(0);
            Message msg = Message.obtain();
            msg.what = MyConstants.CARD_VALID;
            Bundle bundle = new Bundle();
            bundle.putSerializable(MyConstants.KEY, person);
            msg.setData(bundle);
            cameraHandler.sendMessage(msg);
        }
    }

    /**
     * 开门操作
     *
     * @param type   0.普通 1.陌生人
     * @param cardNo
     */
    protected void openDoor(int type, String cardNo) {
        if (openDoorThread != null) {
            openDoorThread.interrupt();
            openDoorThread = null;
        }
        openDoorThread = new OpenDoorThread(Camera1Activity.this, type, cardNo);
        openDoorThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cardExecutors != null) {
            cardExecutors.shutdown();
            cardExecutors = null;
        }
        if (openDoorThread != null) {
            openDoorThread.interrupt();
            openDoorThread = null;
        }
    }

}
