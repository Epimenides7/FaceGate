package com.wudi.facegate.base;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

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
import com.wudi.facegate.utils.DataUtils;
import com.wudi.facegate.utils.FaceUtils;
import com.wudi.facegate.utils.ImageUtil;
import com.wudi.facegate.utils.MyConstants;
import com.wudi.facegate.utils.SPU;
import com.wudi.facegate.utils.Utils;
import com.wudi.facegate.view.FaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import cn.com.armt.sdk.hardware.Wiegand;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * 封装了camera2自定义camera的操作
 * Created by wudi on 2020/4/11.
 */
public abstract class Camera2Activity extends HttpActivity implements CameraCallBack {
    @BindView(R.id.rgb)
    TextureView surfaceView;
    @BindView(R.id.fv)
    protected FaceView faceView;

    private Handler mHandler = new Handler();
    private HandlerThread mThreadHandler;
    private String rgbCameraID;//RGB相机ID
    private String irCameraID;//红外相机ID
    private CameraManager cameraManager;//相机管理者
    private CameraDevice rgbCameraDevice;//rgb相机设备
    private CameraDevice irCameraDevice;//红外相机设备
    private Surface previewSurface;//预览空间
    private Surface previewRGBSurface;//预览处理者空间
    private Surface irSurface;//红外图像空间
    private Size previewSize;//预览尺寸
    private ImageReader previewImageReader;//RGB预览图像处理者
    private ImageReader irImageReader;//红外相机图像处理者
    private CameraCaptureSession rgbCameraSession;//rgb相机捕获会话
    private CameraCaptureSession irCameraSession;//红外相机捕获会话
    private float scaleWidth;//宽度比例
    private float scaleHeight;//高度比例
    private CameraHandler cameraHandler;//运行在主线程中 处理相机返回数据
    private FaceUtils faceUtils;//人脸工具类
    private String lastFaceKey = "";//上个人脸key
    private long lastFaceTime = 0;//上个人脸时间
    private ExecutorService faceExecutors;//人脸处理线程池
    private ExecutorService cardExecutors;//刷卡处理线程池
    private TimerTask timerTask;
    private Timer timer;//计时器控制继电器定时关闭

    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 0); //前置旋转270  后置旋转90
        ORIENTATION.append(Surface.ROTATION_90, 90);
        ORIENTATION.append(Surface.ROTATION_180, 180);
        ORIENTATION.append(Surface.ROTATION_270, 270);
    }

    //预览页状态回调
    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            initCamera();
            initImageReader();
//            changePreviewDisplayOrientation();
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    //捕获成功的回调
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    //用于旋转预览角度
    private void changePreviewDisplayOrientation() {
        int mTextureViewWidth = surfaceView.getWidth();
        int mTextureViewHeight = surfaceView.getHeight();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        LogUtils.d("rotation="+rotation);
        LogUtils.e("mPreviewWidth="+previewSize.getHeight()+" mPreviewHeight="+previewSize.getWidth());
        LogUtils.e("textureWidth="+mTextureViewWidth+" textureHeight="+mTextureViewHeight);

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, mTextureViewWidth, mTextureViewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            LogUtils.e("Surface.ROTATION_90 ROTATION_270");
            /*bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) mTextureViewHeight / mPreviewHeight,
                                    (float) mTextureViewWidth / mPreviewWidth);
            LogHelper.d(TAG,"scale="+scale);
            matrix.postScale(scale, scale, centerX, centerY);*/
            matrix.postRotate((90 * (rotation - 2)) % 360, centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            LogUtils.d("Surface.ROTATION_180 =");
            matrix.postRotate(90, centerX, centerY);
        }
        surfaceView.setTransform(matrix);
    }

    /**
     * 计算比列
     */
    private void countXY() {
        float widthFV = previewSize.getWidth();
        float heightFV = previewSize.getHeight();
        float heightImg = 480;
        float widthImg = 640;
        scaleWidth = widthImg / widthFV;
        scaleHeight = heightImg / heightFV;
    }

    //使用工作线程的looper，队列处理ImageReader的数据，然后传给主线程的Handler处理UI逻辑
    static class ImageSaver implements Runnable {
        private Image mImage;
        private int type;
        private WeakReference<Camera2Activity> reference;//弱引用activity

        public ImageSaver(Image image, int type, Camera2Activity activity) {
            this.mImage = image;
            this.type = type;
            this.reference = new WeakReference<>(activity);
        }

        //这个Run()方法里做的就是把从Image中获得的帧数据转换为bitmap，交给人脸sdk处理
        @Override
        public void run() {
            if (SPU.getMode(reference.get()) == 1 || SPU.getMode(reference.get()) == 4) {
                //当前模式为1刷卡 或者4关闭  不处理回调操作
                mImage.close();//关闭图片资源占用
                return;
            }
//            if(System.currentTimeMillis() - reference.get().lastFPSTime < 100){
//                //帧间隔小于200毫秒，丢弃
//                mImage.close();//关闭图片资源占用
//                return;
//            }
//            byte[] data68 = ImageUtil.getBytesFromImageAsType(mImage,0);
//            int rgb[] = ImageUtil.decodeYUV420SP(data68, mImage.getWidth(), mImage.getHeight());
//            Bitmap bitmap = Bitmap.createBitmap(rgb, 0, mImage.getWidth(),mImage.getWidth(), mImage.getHeight(),
//                    android.graphics.Bitmap.Config.ARGB_8888);
            Bitmap bitmap = reference.get().onImageAvailable(mImage);//YUV 转 RGB
            mImage.close();//关闭图片资源占用
            Box box = reference.get().faceUtils.getBox(bitmap,reference.get());
            Rect rect = reference.get().faceUtils.getRect(box);//截取人脸坐标
            if (rect == null) {
                //未捕获到人脸
                Message message = Message.obtain();
                message.what = MyConstants.NO_FACE;
                reference.get().cameraHandler.sendMessage(message);
                return;
            }
            List<RectF> rectFList = new ArrayList<>();
            float top = rect.top / reference.get().scaleWidth;
            float bottom = rect.bottom / reference.get().scaleWidth;
            float left = rect.left / reference.get().scaleHeight;
            float right = rect.right / reference.get().scaleHeight;
            int faceWidth = (int) (right - left);//计算人脸宽度
            rectFList.add(new RectF(left, top, right, bottom));
            Message message = Message.obtain();
            message.what = MyConstants.DRAW_RECT;
            message.obj = rectFList;
            reference.get().cameraHandler.sendMessage(message);//画矩形
            File file = Utils.saveBitmap(reference.get().recordDir.getPath(), System.currentTimeMillis() + "", bitmap);//保存图片到本地
            switch (type) {
                case 0:
                    //rgb相机
                    //图片清晰度判断
                    if (!reference.get().faceUtils.isDefinition(bitmap)) {
                        Message msg = Message.obtain();
                        msg.what = MyConstants.PHOTO_VAGUE;
                        msg.obj = file;
                        reference.get().cameraHandler.sendMessage(msg);//画面模糊
                        return;
                    }

                    //距离判断
                    if (!reference.get().faceUtils.distance(reference.get(), faceWidth)) {
                        //人脸太小  说明距离过远
                        Message msg = Message.obtain();
                        msg.what = MyConstants.DISTANCE_FAR;
                        msg.obj = file;
                        reference.get().cameraHandler.sendMessage(msg);//距离过远
                        return;
                    }

                    //人脸识别
                    Bitmap crop = reference.get().faceUtils.cropBitmap(box,bitmap);
                    float[] feature = reference.get().faceUtils.getFeature(box,crop);//获取人脸特征值
                    String key = reference.get().faceUtils.evaluate(reference.get(),feature);

                    if (key.equals("stranger")) {
                        //陌生人
                        Message msg = Message.obtain();
                        msg.what = MyConstants.STRANGER;
                        msg.obj = file;
                        reference.get().cameraHandler.sendMessage(msg);//陌生人
                        return;
                    }

                    if (key.equals("null")) {
                        //人脸库为空
                        Message msg = Message.obtain();
                        msg.what = MyConstants.FACE_MAP_IS_NULL;
                        reference.get().cameraHandler.sendMessage(msg);
                        return;
                    }

                    //时间窗检测
                    if (key.equals(reference.get().lastFaceKey)) {
                        //当前脸与上个脸相同
                        int timeWindow = SPU.getTimeWindow(reference.get());
                        long milliSecond = ((long) timeWindow) * 60l * 1000l;
                        if (reference.get().lastFaceTime + milliSecond > System.currentTimeMillis()) {
                            //还在时间窗内，屏蔽本次,做无人脸处理
                            Message msg = Message.obtain();
                            msg.what = MyConstants.NO_FACE;
                            reference.get().cameraHandler.sendMessage(msg);
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
                        return;
                    }

                    //活体检测
                    if (reference.get().faceUtils.isAliveRGB(crop)) {
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
                    break;
                case 1:
                    //ir相机帧抓取

                    break;
            }
//            reference.get().testTime = reference.get().testTime+1;
//            LogUtils.d(reference.get().testTime);
//            reference.get().faceExecutors.execute(new DrawFaceRunnable(bitmap,reference.get()));
//            reference.get().faceExecutors.execute(new FaceRunnable(bitmap,type,faceWidth,reference.get()));
//            reference.get().lastFPSTime = System.currentTimeMillis();
        }
    }

    //运行在Main线程的Handler  处理相机返回数据
    static class CameraHandler extends Handler {
        private WeakReference<Camera2Activity> reference;//弱引用activity

        public CameraHandler(Camera2Activity activity) {
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
                default:
                    //识别失败
                    File f1 = (File) msg.obj;
                    reference.get().discernFailed(msg.what, f1);
                    break;
            }
        }
    }

    @Override
    protected void init() {
        initUsb();
        cameraHandler = new CameraHandler(Camera2Activity.this);
        faceUtils = FaceUtils.getInstance(getAssets(),mActivity);
        surfaceView.setSurfaceTextureListener(textureListener);
        mThreadHandler = new HandlerThread("CAMERA2");
        mThreadHandler.start();
        mHandler = new Handler(mThreadHandler.getLooper());
        faceExecutors = Executors.newFixedThreadPool(1);
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
        if (type == 0 && SPU.getSwitchSignal(mActivity) == 0) {
            //开门信号关
            return;
        }
        if (type == 1 && SPU.getStrangerSwitchSignal(mActivity) == 0) {
            //陌生人开门信号关
            return;
        }
        Intent it = new Intent("com.android.intent.OpenRelay");//继电器开门操作
        sendBroadcast(it);
        delayCloseDoor();
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
     * 延迟关门
     */
    private void delayCloseDoor() {
        long mill = (long) (SPU.getRelayTime(mActivity) * 1000);
        stopTime();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Intent it = new Intent("com.android.intent.CloseRelay");//继电器开门操作
                sendBroadcast(it);
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, mill);
    }

    /**
     * 停止定时器
     */
    private void stopTime() {
        if (timer != null) {
            timer.cancel();
            timer = null;
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

    /**
     * 初始化相机，获取需要打开的相机资源
     */
    private void initCamera() {
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String[] idList = cameraManager.getCameraIdList();
            LogUtils.d(idList);
            rgbCameraID = "0";
            irCameraID = "1";
//        ImageReader：常用来拍照或接收 YUV 数据
//        MediaRecorder：常用来录制视频
//        MediaCodec：常用来录制视频
//        SurfaceHolder：常用来显示预览画面
//        SurfaceTexture：常用来显示预览画面
            previewSize = new Size(999, 800);
            countXY();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraManager != null) {
            openCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rgbCameraDevice != null) {
            rgbCameraDevice.close();
        }
        if (irCameraDevice != null) {
            irCameraDevice.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faceExecutors != null) {
            faceExecutors.shutdown();
            faceExecutors = null;
        }
        if (cardExecutors != null) {
            cardExecutors.shutdown();
            cardExecutors = null;
        }
        if (mHandler != null) {
            mHandler = null;
        }
        if (mThreadHandler != null) {
            mThreadHandler.quitSafely();
            mThreadHandler = null;
        }
        if (cameraManager != null) {
            cameraManager = null;
        }
        if (irImageReader != null) {
            irImageReader.close();
            irImageReader = null;
        }
        if (rgbCameraSession != null) {
            rgbCameraSession.close();
            rgbCameraSession = null;
        }
        if (irCameraSession != null) {
            irCameraSession.close();
            irCameraSession = null;
        }
        stopTime();
    }

    /**
     * 打开相机操作
     */
    protected void openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            EasyPermissions.requestPermissions(this, getString(R.string.app_name) + "缺少重要权限，拒绝则部分功能将无法运行!", 0, Manifest.permission.CAMERA);
            return;
        }
        try {
            if (rgbCameraID == null || rgbCameraID.isEmpty() || irCameraID == null || irCameraID.isEmpty()) {
                show_Toast("没有发现可用相机设备，请检查摄像头连接！");
                return;
            }
            cameraManager.openCamera(rgbCameraID, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    rgbCameraDevice = camera;
                    createSession(0);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    rgbCameraDevice = null;
                    show_Toast("RGB相机打开错误：" + error);
                }
            }, null);
            cameraManager.openCamera(irCameraID, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    irCameraDevice = camera;
                    createSession(1);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    irCameraDevice = null;
                    show_Toast("红外相机打开错误：" + error);
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 创建相机设备会话
     */
    private void createSession(int type) {
        switch (type) {
            case 0:
                //RGB相机 添加两个surface  预览和预览空间
                SurfaceTexture surfaceTexture = surfaceView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                previewSurface = new Surface(surfaceTexture);
                if (rgbCameraDevice != null) {
                    try {
                        rgbCameraDevice.createCaptureSession(Arrays.asList(previewSurface, previewRGBSurface)
                                , new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession session) {
                                        rgbCameraSession = session;
                                        startPreview();
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                                    }
                                }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 1:
                //红外相机 添加一个surface 拍照
                if (irCameraDevice != null) {
                    try {
                        irCameraDevice.createCaptureSession(Arrays.asList(irSurface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                irCameraSession = session;
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }


    /**
     * 开始预览
     */
    private void startPreview() {
        //            TEMPLATE_PREVIEW：适用于配置预览的模板。
        //            TEMPLATE_RECORD：适用于视频录制的模板。
        //            TEMPLATE_STILL_CAPTURE：适用于拍照的模板。
        //            TEMPLATE_VIDEO_SNAPSHOT：适用于在录制视频过程中支持拍照的模板。
        //            TEMPLATE_MANUAL：适用于希望自己手动配置大部分参数的模板。
        try {
            CaptureRequest.Builder builder = rgbCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);//创建捕获预览请求
            builder.addTarget(previewSurface);
            builder.addTarget(previewRGBSurface);
            CaptureRequest captureRequest = builder.build();
            rgbCameraSession.setRepeatingRequest(captureRequest, captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 帧抓取
     */
    protected void takePicture() {
        try {
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //红外相机抓取帧
            CaptureRequest.Builder builder2 = irCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //设置拍照方向
            builder2.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            //聚焦
            builder2.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder2.addTarget(irSurface);
            CaptureRequest captureRequest2 = builder2.build();
            if (irCameraSession != null) {
                irCameraSession.capture(captureRequest2, captureCallback, mHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化图像处理器
     */
    private void initImageReader() {
        previewImageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 1);
        previewImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mHandler.post(new ImageSaver(reader.acquireNextImage(), 0, Camera2Activity.this));
//                faceExecutors.execute(new ImageSaver(reader.acquireNextImage(), 0, CameraActivity.this));
            }
        }, mHandler);
        previewRGBSurface = previewImageReader.getSurface();

        irImageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 1);
        irImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //TODO 暂时不处理ir摄像头帧数据
//                mHandler.post(new ImageSaver(reader.acquireNextImage(), 1, Camera2Activity.this));
            }
        }, mHandler);
        irSurface = irImageReader.getSurface();
    }

    /**
     * YUV转为RGB bitmap
     *
     * @param image
     * @return
     */
    private Bitmap onImageAvailable(Image image) {
        if (image == null) return null;
        ByteArrayOutputStream outputbytes = new ByteArrayOutputStream();
        ByteBuffer bufferY = image.getPlanes()[0].getBuffer();
        byte[] data0 = new byte[bufferY.remaining()];
        bufferY.get(data0);
        ByteBuffer bufferU = image.getPlanes()[1].getBuffer();
        byte[] data1 = new byte[bufferU.remaining()];
        bufferU.get(data1);
        ByteBuffer bufferV = image.getPlanes()[2].getBuffer();
        byte[] data2 = new byte[bufferV.remaining()];
        bufferV.get(data2);
        try {
            outputbytes.write(data0);
            outputbytes.write(data2);
            outputbytes.write(data1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final YuvImage yuvImage = new YuvImage(outputbytes.toByteArray(), ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream outBitmap = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 80, outBitmap);
        SoftReference soft = new SoftReference(BitmapFactory.decodeByteArray(outBitmap.toByteArray(), 0, outBitmap.size()));
        Bitmap bitmap = (Bitmap) soft.get();
        try {
            outputbytes.close();
            outBitmap.close();
            image.close();
            System.gc();//呼叫GC
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }


}
