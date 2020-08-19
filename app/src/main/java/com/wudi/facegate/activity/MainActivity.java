package com.wudi.facegate.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.apkfuns.logutils.LogUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.Response;
import com.wudi.facegate.R;

import com.wudi.facegate.base.Camera1Activity;
import com.wudi.facegate.broadcast.NetWorkBroadcast;
import com.wudi.facegate.event.AlarmEvent;
import com.wudi.facegate.greenDao.GreenDaoManager;
import com.wudi.facegate.greenDao.Person;
import com.wudi.facegate.greenDao.Record;
import com.wudi.facegate.http.JsonCallBack;
import com.wudi.facegate.module.AliKey;
import com.wudi.facegate.module.AliKeyDTO;
import com.wudi.facegate.module.BaseDTO;
import com.wudi.facegate.module.ScreenDTO;
import com.wudi.facegate.utils.ApkController;
import com.wudi.facegate.utils.MyConstants;
import com.wudi.facegate.utils.PlayVoiceUtil;
import com.wudi.facegate.utils.SPU;
import com.wudi.facegate.utils.SerialPortUtil;
import com.wudi.facegate.utils.SoundPoolUtil;
import com.wudi.facegate.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import butterknife.BindView;
import butterknife.OnClick;
import cn.com.armt.sdk.DeviceManager;
import io.reactivex.functions.Consumer;

public class MainActivity extends Camera1Activity {
    @BindView(R.id.tv_ip_address)
    TextView tvIp;
    @BindView(R.id.tv_SN)
    TextView tvSN;
    @BindView(R.id.ll_waning)
    LinearLayout llWaning;
    @BindView(R.id.iv_status)
    ImageView ivStatus;
    @BindView(R.id.tv_name)
    TextView tvName;
    @BindView(R.id.tv_waning)
    TextView tvWaning;
    @BindView(R.id.tv_version)
    TextView tvVersion;
    @BindView(R.id.tv_person_num)
    TextView tvPersonNum;
    @BindView(R.id.tv_photo_num)
    TextView tvPhotoNum;
    @BindView(R.id.tv_project_name)
    TextView tvProjectName;
    @BindView(R.id.iv_network)
    ImageView ivNetWork;
    @BindView(R.id.tv_hint)
    TextView tvHint;

    private SerialPortUtil serialPortUtil;
    private NetWorkBroadcast broadcast;//网络监听广播
    private AliKey aliKey;//阿里云密钥
    private OSS oss;
    private long assPastTime;//过期时间
    private Map<String, String> textMap = new HashMap<>();//提示文本
    private CountDownTimer cdt;//窗口显示倒计时
    private boolean isFace = false;//是否刷脸
    private boolean isCard = false;//是否刷卡
    private Person card;//刷卡数据
    private File face;//刷脸数据
    private String faceNum;//刷脸人员卡号
    private SoundPoolUtil soundPoolUtil;
    private CountDownTimer faceCDT;//检测到人脸后倒计时


    @OnClick(R.id.iv_bg_r)
    void click() {
        startActivity(new Intent(mActivity, PassWordActivity.class));
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {
        super.init();
        soundPoolUtil = SoundPoolUtil.getInstance();
        createFolder();
        initResource();
        initView();
        SPU.setIsFirst(mActivity, false);
        getAliKey(false);
        broadcast = new NetWorkBroadcast(new NetWorkBroadcast.NetWorkCallBack() {
            @Override
            public void wifiConnect(boolean isConnect) {
                if (isConnect) {
                    ivNetWork.setImageResource(R.drawable.online);
                    if (System.currentTimeMillis() > assPastTime) {
                        getAliKey(true);
                    } else {
                        List<Record> list = GreenDaoManager.getRecordList(mActivity);
                        if (list != null && list.size() > 0) {
                            for (Record record : list) {
                                upLoadImageToAli(record);
                            }
                        }
                    }

                } else {
                    ivNetWork.setImageResource(R.drawable.offline);
                }
            }
        });
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(broadcast, filter);
        initFaceCDT();
    }

    /**
     * 初始化提示器倒计时
     */
    private void initFaceCDT() {
        faceCDT = new CountDownTimer((SPU.getMatchTime(mActivity)+1) * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                LogUtils.d("当前倒计时："+millisUntilFinished);
                int second = (int) (millisUntilFinished / 1000);
                tvHint.setText("检测到人脸，请保持：" + second + "秒");
            }

            @Override
            public void onFinish() {
                tvHint.setText("正在识别，请稍后...");
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PlayVoiceUtil.clear();
        if (broadcast != null) {
            unregisterReceiver(broadcast);
        }
        stopCdt();
        stopFaceCDT();
    }

    /**
     * 初始化资源
     */
    private void initResource() {
        List<ScreenDTO> screenDTOList = SPU.getScreenList(mActivity);
        for (ScreenDTO dto : screenDTOList) {
            if (dto == null || dto.getCatCd().isEmpty()) {
                break;
            }
            textMap.put(dto.getCatCd(), dto.getContent());
        }
        soundPoolUtil.load();
    }

    /**
     * 初始化View显示数据
     */
    private void initView() {
        long count = GreenDaoManager.getPersonListCount(mActivity);
        tvProjectName.setText(SPU.getProjectName(mActivity));
        tvIp.setText("IP：" + ipAddress);
        tvSN.setText("SN：" + SN);
        tvVersion.setText("版本号：" + version);
        tvPersonNum.setText("人数：" + count);
        tvPhotoNum.setText("照片：" + count);
    }


    @Override
    public void discernSuccess(File file, String key) {
        //识别成功,通过id查询人员信息
        String name = key;
        List<Person> list = GreenDaoManager.getPersonListById(mActivity, Long.parseLong(key));
        String number = "";
        if (list != null && list.size() > 0) {
            name = list.get(0).getName();
            number = list.get(0).getNumber();
        }
        long personId = Long.parseLong(key);
        if (SPU.getMode(mActivity) == 3) {
            //TODO 刷脸加刷卡
            isFace = true;
            face = file;
            faceNum = number;
            faceAndCard();
        } else {
            //只刷脸  刷脸或刷卡
            showWindow(false, textMap.get("0"), name);
//            PlayVoiceUtil.playFile(voicePath.get(0),mActivity);
            soundPoolUtil.play(0);
            addRecord(file, MyConstants.DISCERN_SUCCESS, personId, 0, number);
            openDoor(0, number);
        }
    }

    @Override
    public void discernFailed(int code, File file) {
        //识别失败
        if (code == MyConstants.STRANGER) {
            //陌生人单独处理
            disposeStranger(file);
            return;
        }
        String name = "";
        switch (code) {
            case MyConstants.WITHOUT_HAT:
                //未带安全帽
                name = "未带安全帽";
                break;
            case MyConstants.PHOTO_VAGUE:
                //画面模糊
                name = "图像模糊";
                break;
            case MyConstants.DISTANCE_FAR:
                //距离太远
                name = "距离太远";
                break;
            case MyConstants.NOT_ALIVE:
                //活体未通过
                name = "请看屏幕";
                break;
        }
        showWindow(true, textMap.get(code + ""), name);
//        PlayVoiceUtil.playFile(voicePath.get(code),mActivity);
        soundPoolUtil.play(code);
        addRecord(file, code, 0, 0, "");
    }


    @Override
    public void noFace(boolean retainRect) {
        if (!retainRect) {
            faceView.clearFace();
        }
        stopFaceCDT();
        tvHint.setText("人脸识别区域");
    }

    @Override
    public void faceMapIsNull() {
        //人脸库为空
        showWindow(true, "", "本地人脸库为空！");
    }

    @Override
    public void cardInvalid() {
        //无效卡
        showWindow(true, "", "无效卡！");
    }

    @Override
    public void cardValid(Person person) {
        //刷卡成功
        if (SPU.getMode(mActivity) == 3) {
            //TODO 刷脸加刷卡
            isCard = true;
            card = person;
            faceAndCard();
        } else {
            //只刷卡  刷脸或刷卡
            showWindow(false, textMap.get("0"), person.getName());
//            PlayVoiceUtil.playFile(voicePath.get(0),mActivity);
            soundPoolUtil.play(0);
            addRecord(null, 0, person.getId(), 1, person.getNumber());
            openDoor(0, person.getNumber());//开门操作
        }
    }

    @Override
    public void haveFace() {
        //首次出现人脸之后的操作
        stopFaceCDT();
        faceCDT.start();
    }

    @Override
    public void skewing() {
        tvHint.setText("请将脸部正对识别框!");
    }

    @Override
    public void changeText(String text) {
        tvHint.setText(text);
    }


    /**
     * 停止人脸检测倒计时
     */
    private void stopFaceCDT() {
        if (faceCDT != null) {
            faceCDT.cancel();
        }
    }

    /**
     * 刷卡加刷脸
     */
    private synchronized void faceAndCard() {
        if (!isCard || card == null) {
            //未刷卡
            showWindow(false, "刷脸已通过！", "请刷卡!");
            return;
        }
        if (!isFace || face == null) {
            //未刷脸
            showWindow(false, "刷卡已通过！", "请刷脸!");
            return;
        }
        //刷卡和刷脸都完成
        String cardNum = card.getNumber();
        if (faceNum.equals(cardNum)) {
            //人员匹配
            showWindow(false, textMap.get("0"), card.getName());
//            PlayVoiceUtil.playFile(voicePath.get(0),mActivity);
            soundPoolUtil.play(0);
            addRecord(face, 0, card.getId(), 2, cardNum);
            openDoor(0, cardNum);//开门操作
            faceAndCardClear();
        }
    }

    /**
     * 处理陌生人
     */
    private void disposeStranger(File file) {
        addRecord(file, MyConstants.STRANGER, 0, 0, "");
        if (SPU.getStrangerSwitch(mActivity) == 0) {
            //陌生人开关关闭，直接开门
            openDoor(1, "00000000");//陌生人卡号8个0
            return;
        }
        if (SPU.getStrangerAlarm(mActivity) == 1) {
            //陌生人报警打开
//            PlayVoiceUtil.playFile(voicePath.get(MyConstants.STRANGER),mActivity);
            soundPoolUtil.play(MyConstants.STRANGER);
            showWindow(true, textMap.get(MyConstants.STRANGER), "陌生人");
        }
    }

    /**
     * 添加人员记录
     *
     * @param file     抓拍图片
     * @param code     操作类型
     * @param personId 人员Id  失败可以不传
     */
    private void addRecord(File file, int code, long personId, int way, String number) {
        String path = "";
        long timestamp = System.currentTimeMillis();
        if (file != null) {
            path = file.getPath();
        }
        Record record = new Record(timestamp, personId, code, way, number, path);
        GreenDaoManager.addRecord(mActivity, record);//添加记录到本地数据库
        if (!path.isEmpty()) {
            upLoadImageToAli(record);
        } else {
            JSONObject object = getPublicJson();
            JSONArray logList = new JSONArray();
            JSONObject log = new JSONObject();
            try {
                log.put("id", record.getPersonId());
                log.put("code", record.getCode());
                log.put("timestamp", record.getTimestamp());
                log.put("way", record.getWay());
                log.put("number", record.getNumber());
                log.put("picUrl", "");
                logList.put(log);
                object.put("logList", logList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            upLoadLog(object, file, record.getTimestamp());
        }
    }

    /**
     * 显示提示窗口
     *
     * @param isWaning
     * @param showText
     * @param name
     */
    private void showWindow(boolean isWaning, String showText, String name) {
        if (SPU.getScreenSwitch(mActivity) == 0) {
            //判断屏幕显示开关是否为关闭
            return;
        }
        if (isWaning) {
            llWaning.setBackgroundResource(R.drawable.red_bg);
            ivStatus.setImageResource(R.drawable.waing);
        } else {
            llWaning.setBackgroundResource(R.drawable.green_bg);
            ivStatus.setImageResource(R.drawable.ok);
        }
        tvName.setText(name);
        tvWaning.setText(showText);
        llWaning.setVisibility(View.VISIBLE);
        delayHideWindow();
    }

    /**
     * 延迟隐藏window
     */
    private synchronized void delayHideWindow() {
        stopCdt();
        cdt = new CountDownTimer(2 * 1000, 500) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                llWaning.setVisibility(View.GONE);
                faceAndCardClear();
            }
        }.start();
    }

    /**
     * 刷卡+刷脸重置数据
     */
    private void faceAndCardClear() {
        isCard = false;
        isFace = false;
        card = null;
        face = null;
        faceNum = "";
    }

    /**
     * 暂停计时器
     */
    private void stopCdt() {
        if (cdt != null) {
            cdt.cancel();
            cdt = null;
        }
    }

    /**
     * 上传图片
     *
     * @param record
     */
    private void upLoadImageToAli(final Record record) {
        if (System.currentTimeMillis() > assPastTime) {
            //令牌超时
            getAliKey(false);
            return;
        }
        final File file = new File(record.getImgPath());
        String objectKey = "device/" + SN + "/" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "/" + file.getName();
        // 构造上传请求。
        PutObjectRequest put = new PutObjectRequest("jtg-face", objectKey, file.getPath());
        // 异步上传时可以设置进度回调。
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {

            }
        });
        oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                JSONObject object = getPublicJson();
                JSONArray logList = new JSONArray();
                JSONObject log = new JSONObject();
                String url = MyConstants.ALI_PATH + request.getObjectKey();
                try {
                    log.put("id", record.getPersonId());
                    log.put("code", record.getCode());
                    log.put("timestamp", record.getTimestamp());
                    log.put("way", record.getWay());
                    log.put("number", record.getNumber());
                    log.put("picUrl", url);
                    logList.put(log);
                    object.put("logList", logList);
                    upLoadLog(object, file, record.getTimestamp());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常。
                if (clientExcepion != null) {
                    // 本地异常，如网络异常等。
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常。
                    serviceException.printStackTrace();
                }
            }
        });
    }

    /**
     * 动态获取阿里云key
     */
    private void getAliKey(final boolean isNeedUpload) {
        OkGo.<BaseDTO<AliKeyDTO>>post(MyConstants.HTTP_ADDRESS + "getStsToken")
                .tag(mActivity)
                .upJson(getPublicJson().toString())
                .execute(new JsonCallBack<BaseDTO<AliKeyDTO>>() {
                    @Override
                    public void onSuccess(Response<BaseDTO<AliKeyDTO>> response) {
                        BaseDTO<AliKeyDTO> dto = response.body();
                        if (dto.getCode() == 0) {
                            //成功
                            aliKey = dto.getData().getCredentials();
                            assPastTime = Utils.stringDateToLong(aliKey.getExpiration(), new SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss"));
                            initAli(isNeedUpload);
                        }
                    }
                });
    }

    /**
     * 初始化阿里云OSS
     */
    private void initAli(final boolean isNeedUpload) {
        final String endpoint = "http://oss-cn-hangzhou.aliyuncs.com";
        // 在移动端建议使用STS的方式初始化OSSClient。
        final OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider(aliKey.getAccessKeyId()
                , aliKey.getAccessKeySecret(), aliKey.getSecurityToken());
        final ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒。
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒。
        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个。
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次。
        new Thread(new Runnable() {
            @Override
            public void run() {
                oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider, conf);
                if (isNeedUpload) {
                    List<Record> list = GreenDaoManager.getRecordList(mActivity);
                    if (list != null && list.size() > 0) {
                        for (Record record : list) {
                            upLoadImageToAli(record);
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 日志上传
     */
    private void upLoadLog(JSONObject object, final File file, final long timestamp) {
        OkGo.<BaseDTO<String>>post(MyConstants.HTTP_ADDRESS + "log")
                .tag(mActivity)
                .upJson(object.toString())
                .execute(new JsonCallBack<BaseDTO<String>>() {
                    @Override
                    public void onSuccess(Response<BaseDTO<String>> response) {
                        BaseDTO dto = response.body();
                        if (dto.getCode() == 0) {
                            //上传成功，删除数据库记录和本地图片
                            if (file != null) {
                                deleteFile(file);
                            }
                            GreenDaoManager.deleteRecordByTimestamp(mActivity, timestamp);
                        }
                    }
                });
    }


    @Override
    protected void addLog(String log, boolean isWaning) {

    }

    @Override
    protected void changeTitle(String title) {

    }

    @Override
    protected void over() {
        //http操作结束，更新人数与照片数量
        long count = GreenDaoManager.getPersonListCount(mActivity);
        tvPersonNum.setText("人数：" + count);
        tvPhotoNum.setText("照片：" + count);
    }

}
