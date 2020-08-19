package com.wudi.facegate.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.wudi.facegate.R;
import com.wudi.facegate.base.BaseActivity;
import com.wudi.facegate.utils.SPU;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 设置页
 * Created by wudi on 2020/5/21.
 */
public class SettingActivity extends BaseActivity {
    @BindView(R.id.tv_distCd)
    TextView tvDistCd;
    @BindView(R.id.tv_matchRate)
    TextView tvMatchRate;
    @BindView(R.id.tv_matchTime)
    TextView tvMatchTime;
    @BindView(R.id.tv_timeWindow)
    TextView tvTimeWindow;
    @BindView(R.id.tv_mode)
    TextView tvMode;
    @BindView(R.id.tv_relayTime)
    TextView tvRelayTime;
    @BindView(R.id.tv_switchSignal)
    TextView tvSwitchSignal;
    @BindView(R.id.tv_wiegandSignal)
    TextView tvWiegandSignal;
    @BindView(R.id.tv_personSignal)
    TextView tvPersonSignal;
    @BindView(R.id.tv_voiceSwitch)
    TextView tvVoiceSwitch;
    @BindView(R.id.tv_sound)
    TextView tvSound;
    @BindView(R.id.tv_screenSwitch)
    TextView tvScreenSwitch;
    @BindView(R.id.tv_strangerSwitch)
    TextView tvStrangerSwitch;
    @BindView(R.id.tv_strangerAlarm)
    TextView tvStrangerAlarm;
    @BindView(R.id.tv_strangerSwitchSignal)
    TextView tvStrangerSwitchSignal;
    @BindView(R.id.tv_strangerWiegandSignal)
    TextView tvStrangerWiegandSignal;
    @BindView(R.id.tv_aliveSwitch)
    TextView tvAliveSwitch;
    @BindView(R.id.tv_helmetSwitch)
    TextView tvHelmetSwitch;
    @BindView(R.id.et_pwd)
    EditText etPwd;

    @OnClick({R.id.bt_back, R.id.bt_init,R.id.bt_affirm})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.bt_back:
                finish();
                break;
            case R.id.bt_init:
                startActivity(new Intent(mActivity,InitActivity.class));
                finish();
                break;
            case R.id.bt_affirm:
                String pwd = etPwd.getText().toString().trim();
                if(pwd.isEmpty()){
                    show_Toast("密码不能为空！");
                    return;
                }
                SPU.setPassword(mActivity,pwd);
                show_Toast("修改成功，已保存！");
                break;
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_setting;
    }

    @Override
    protected void init() {
        etPwd.setText(SPU.getPassword(mActivity));
        tvDistCd.setText("最远识别距离：" +SPU.getDistCd(mActivity)+"米");
        tvMatchRate.setText(tvMatchRate.getText().toString()+ SPU.getMatchRate(mActivity)+"%");
        tvMatchTime.setText(tvMatchTime.getText().toString()+ SPU.getMatchTime(mActivity)+"秒");
        tvTimeWindow.setText(tvTimeWindow.getText().toString()+ SPU.getTimeWindow(mActivity)+"分钟");
        String mode = "";
        switch (SPU.getMode(mActivity)){
            case 0:
                //刷脸
                mode = "刷脸";
                break;
            case 1:
                //刷卡
                mode = "刷卡";
                break;
            case 2:
                //刷脸或刷卡
                mode = "刷脸或刷卡";
                break;
            case 3:
                //刷卡加刷脸
                mode = "刷卡加刷脸";
                break;
            case 4:
                //设备禁用，禁止开门
                mode = "设备禁用，禁止开门";
                break;
        }
        tvMode.setText(tvMode.getText().toString()+ mode);
        tvRelayTime.setText(tvRelayTime.getText().toString()+ SPU.getRelayTime(mActivity)+"秒");
        tvSwitchSignal.setText(tvSwitchSignal.getText().toString()+switchToString(SPU.getSwitchSignal(mActivity),0));
        tvWiegandSignal.setText(tvWiegandSignal.getText().toString()+ switchToString(SPU.getWiegandSignal(mActivity),0));
        tvPersonSignal.setText(tvPersonSignal.getText().toString()+ switchToString(SPU.getPersonSignal(mActivity),0));
        tvVoiceSwitch.setText(tvVoiceSwitch.getText().toString()+ switchToString(SPU.getVoiceSwitch(mActivity),1));
        tvSound.setText(tvSound.getText().toString()+ SPU.getSound(mActivity));
        tvScreenSwitch.setText(tvScreenSwitch.getText().toString()+ switchToString(SPU.getScreenSwitch(mActivity),1));
        tvStrangerSwitch.setText(tvStrangerSwitch.getText().toString()+ switchToString(SPU.getStrangerSwitch(mActivity),1));
        tvStrangerAlarm.setText(tvStrangerAlarm.getText().toString()+ switchToString(SPU.getStrangerAlarm(mActivity),1));
        tvStrangerSwitchSignal.setText(tvStrangerSwitchSignal.getText().toString()+ switchToString(SPU.getStrangerSwitchSignal(mActivity),0));
        tvStrangerWiegandSignal.setText(tvStrangerWiegandSignal.getText().toString()+ switchToString(SPU.getStrangerWiegandSignal(mActivity),0));
        tvAliveSwitch.setText(tvAliveSwitch.getText().toString()+ switchToString(SPU.getAliveSwitch(mActivity),1));
        tvHelmetSwitch.setText(tvHelmetSwitch.getText().toString()+ switchToString(SPU.getHelmetSwitch(mActivity),1));
    }

    /**
     * 标识转文字
     * @param sch 标识
     * @param type 0.信号 1.开关
     * @return
     */
    private String switchToString(int sch,int type){
        String str = "";
        switch (sch){
            case 0:
                if(type == 0){
                    //信号
                    str = "不输出";
                }else{
                    //开关
                    str = "已关闭";
                }
                break;
            case 1:
                if(type == 0){
                    //信号
                    str = "输出";
                }else{
                    //开关
                    str = "已开启";
                }
                break;
        }
        return str;
    }

}
