package com.wudi.facegate.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.apkfuns.logutils.LogUtils;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.BitmapCallback;
import com.lzy.okgo.callback.Callback;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.wudi.facegate.R;
import com.wudi.facegate.adapter.SuperAdapter;
import com.wudi.facegate.base.BaseActivity;
import com.wudi.facegate.base.HttpActivity;
import com.wudi.facegate.greenDao.GreenDaoManager;
import com.wudi.facegate.greenDao.Person;
import com.wudi.facegate.http.JsonCallBack;
import com.wudi.facegate.module.AutoAllDTO;
import com.wudi.facegate.module.AutoDTO;
import com.wudi.facegate.module.BaseDTO;
import com.wudi.facegate.module.ParamDTO;
import com.wudi.facegate.module.PersonDTO;
import com.wudi.facegate.module.ShowLogDTO;
import com.wudi.facegate.module.UpdateDTO;
import com.wudi.facegate.module.VoiceDTO;
import com.wudi.facegate.utils.ApkController;
import com.wudi.facegate.utils.DataUtils;
import com.wudi.facegate.utils.FaceUtils;
import com.wudi.facegate.utils.MyConstants;
import com.wudi.facegate.utils.PlayVoiceUtil;
import com.wudi.facegate.utils.SPU;
import com.wudi.facegate.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import butterknife.BindView;
import cn.com.armt.sdk.DeviceManager;
import cn.com.armt.sdk.hardware.Wiegand;
import io.reactivex.functions.Consumer;

/**
 * 初始化页面
 * 每次开机检查：
 * 1.检查权限
 * 2.检查网络状态
 * 3.检查软件版本是否需要更新
 * 4.检查配置参数是否需要更新（首次初始化直接获取设置）
 * 5.检查人员列表是否需要更新（首次初始化需要先获取已授权列表）
 * Created by wudi on 2020/5/13.
 */
public class InitActivity extends HttpActivity {
    @BindView(R.id.rv)
    RecyclerView rv;
    @BindView(R.id.tv_title)
    TextView tvTitle;

    private String[] permissions = new String[]
            {Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE};//需要的危险权限
    private List<ShowLogDTO> logDTOList = new ArrayList<>();
    private SuperAdapter adapter;

    @Override
    protected int getLayout() {
        return R.layout.activity_init;
    }

    @Override
    protected void init() {
        adapter = new SuperAdapter(mActivity, logDTOList, R.layout.item_log) {
            @Override
            protected void setWidget(BaseViewHolder holder, int position) {
                ShowLogDTO dto = logDTOList.get(position);
                holder.setText(R.id.tv_log, dto.getLog());
                if (dto.isError()) {
                    holder.setTextColor(R.id.tv_log, ContextCompat.getColor(mActivity, R.color.red));
                } else {
                    holder.setTextColor(R.id.tv_log, ContextCompat.getColor(mActivity, R.color.white));
                }
            }
        };
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(mActivity));
        checkPermissions();
    }

    /**
     * 打印日志
     *
     * @param log
     * @param isWaning
     */
    protected void addLog(String log, boolean isWaning) {
        logDTOList.add(new ShowLogDTO(log, isWaning));
        adapter.notifyDataSetChanged();
    }

    /**
     * 更新标题
     *
     * @param title
     */
    @Override
    protected void changeTitle(String title) {
        tvTitle.setText(title);
    }

    @Override
    protected void over() {
        startActivity(new Intent(mActivity, MainActivity.class));
        finish();
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        addLog("正在申请所需要的权限，请稍后...", false);
        RxPermissions rxPermissions = new RxPermissions(mActivity);
        rxPermissions.request(permissions)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            //权限申请成功
                            addLog("权限申请成功!", false);
                            createFolder();
                            checkNetWork();
                        } else {
                            //权限申请失败
                            addLog("权限申请失败!", true);
                        }
                    }
                });
    }

    /**
     * 检查当前网络状态
     */
    private void checkNetWork() {
        addLog("正在检查网络状态,请稍后...", false);
        if (checkNet()) {
            addLog("网络状态正常！", false);
            addLog("版本检查中，请稍后...", false);
            checkVersion();
        } else {
            addLog("网络未连接！", true);
        }
    }


}
