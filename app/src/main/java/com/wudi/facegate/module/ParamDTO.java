package com.wudi.facegate.module;

import java.util.ArrayList;
import java.util.List;

/**
 * 闸机配置参数DTO
 * Created by wudi on 2020/5/11.
 */
public class ParamDTO {
    private List<VoiceDTO> voiceList;//语音DTO
    private List<ScreenDTO> screenList;//屏幕提示DTO
    private String distCd;//识别距离编码: 30000.100.100-0.5米;30000.100.120-1米；30000.100.140-1.5
    private int matchRate;//识别准确率(%)
    private int matchTime;//识别时间(秒)
    private int timeWindow;//时间窗(分钟)
    private int mode;//识别模式[0-刷脸 1-刷卡 2-刷卡或刷脸 3-刷卡加刷脸]
    private float relayTime;//继电器控制时间(%)
    private int switchSignal;//串口输出-开关信号[ 0-不输出 1-输出]
    private int wiegandSignal;//串口输出-韦根信号[ 0-不输出 1-输出]
    private int personSignal;//串口输出-人员信号[ 0-不输出 1-输出]
    private int voiceSwitch;//语音播报开关[ 0-关闭 1-打开]
    private int sound;//语音播报声音大小
    private int screenSwitch;//屏幕显示开关[ 0-关闭 1-打开]
    private int strangerSwitch;//陌生人开关[ 0-关闭 1-打开]
    private int strangerAlarm;//陌生人警报[ 0-关闭 1-打开]
    private int strangerSwitchSignal;//陌生人开关-开关信号[ 0-不输出 1-输出]
    private int strangerWiegandSignal;//陌生人开关-韦根信号[ 0-不输出 1-输出]
    private int aliveSwitch;//活体检测开关[ 0-关闭 1-打开]
    private int helmetSwitch;//安全帽识别开关[ 0-关闭 1-打开]
    private int updFlag;//是否需要更新设备参数,0-否 1-是
    private String name;//首页显示项目名称

    public List<VoiceDTO> getVoiceList() {
        if (voiceList == null) {
            return new ArrayList<>();
        }
        return voiceList;
    }

    public void setVoiceList(List<VoiceDTO> voiceList) {
        this.voiceList = voiceList;
    }

    public List<ScreenDTO> getScreenList() {
        if (screenList == null) {
            return new ArrayList<>();
        }
        return screenList;
    }

    public void setScreenList(List<ScreenDTO> screenList) {
        this.screenList = screenList;
    }

    public String getDistCd() {
        return distCd == null ? "" : distCd;
    }

    public void setDistCd(String distCd) {
        this.distCd = distCd;
    }

    public int getMatchRate() {
        return matchRate;
    }

    public void setMatchRate(int matchRate) {
        this.matchRate = matchRate;
    }

    public int getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(int matchTime) {
        this.matchTime = matchTime;
    }

    public int getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(int timeWindow) {
        this.timeWindow = timeWindow;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public float getRelayTime() {
        return relayTime;
    }

    public void setRelayTime(float relayTime) {
        this.relayTime = relayTime;
    }

    public int getSwitchSignal() {
        return switchSignal;
    }

    public void setSwitchSignal(int switchSignal) {
        this.switchSignal = switchSignal;
    }

    public int getWiegandSignal() {
        return wiegandSignal;
    }

    public void setWiegandSignal(int wiegandSignal) {
        this.wiegandSignal = wiegandSignal;
    }

    public int getPersonSignal() {
        return personSignal;
    }

    public void setPersonSignal(int personSignal) {
        this.personSignal = personSignal;
    }

    public int getVoiceSwitch() {
        return voiceSwitch;
    }

    public void setVoiceSwitch(int voiceSwitch) {
        this.voiceSwitch = voiceSwitch;
    }

    public int getSound() {
        return sound;
    }

    public void setSound(int sound) {
        this.sound = sound;
    }

    public int getScreenSwitch() {
        return screenSwitch;
    }

    public void setScreenSwitch(int screenSwitch) {
        this.screenSwitch = screenSwitch;
    }

    public int getStrangerSwitch() {
        return strangerSwitch;
    }

    public void setStrangerSwitch(int strangerSwitch) {
        this.strangerSwitch = strangerSwitch;
    }

    public int getStrangerAlarm() {
        return strangerAlarm;
    }

    public void setStrangerAlarm(int strangerAlarm) {
        this.strangerAlarm = strangerAlarm;
    }

    public int getStrangerSwitchSignal() {
        return strangerSwitchSignal;
    }

    public void setStrangerSwitchSignal(int strangerSwitchSignal) {
        this.strangerSwitchSignal = strangerSwitchSignal;
    }

    public int getStrangerWiegandSignal() {
        return strangerWiegandSignal;
    }

    public void setStrangerWiegandSignal(int strangerWiegandSignal) {
        this.strangerWiegandSignal = strangerWiegandSignal;
    }

    public int getAliveSwitch() {
        return aliveSwitch;
    }

    public void setAliveSwitch(int aliveSwitch) {
        this.aliveSwitch = aliveSwitch;
    }

    public int getHelmetSwitch() {
        return helmetSwitch;
    }

    public void setHelmetSwitch(int helmetSwitch) {
        this.helmetSwitch = helmetSwitch;
    }

    public int getUpdFlag() {
        return updFlag;
    }

    public void setUpdFlag(int updFlag) {
        this.updFlag = updFlag;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
