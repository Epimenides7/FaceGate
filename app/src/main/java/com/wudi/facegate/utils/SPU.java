package com.wudi.facegate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.DecorToolbar;

import com.apkfuns.logutils.LogUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wudi.facegate.module.ParamDTO;
import com.wudi.facegate.module.ScreenDTO;

import java.net.FileNameMap;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * SP本地存储工具类
 */
public class SPU {
    public static final String NAME = "face_GATE";
    public static final String PROJECT_NAME = "project_name";
    public static final String IS_FIRST = "is_first";
    public static final String SCREEN_LIST = "screen_list";
    public static final String DIST_CD = "dist_cd";
    public static final String MATCH_RATE = "match_rate";
    public static final String MATCH_TIME = "match_time";
    public static final String TIME_WINDOW = "time_window";
    public static final String MODE = "mode";
    public static final String RELAY_TIME = "relay_time";
    public static final String SWITCH_SIGNAL = "switch_signal";
    public static final String WIEGAND_SIGNAL = "wiegand_signal";
    public static final String PERSON_SIGNAL = "person_signal";
    public static final String VOICE_SWITCH = "voice_switch";
    public static final String SOUND = "sound";
    public static final String SCREEN_SWITCH = "screen_switch";
    public static final String STRANGER_SWITCH = "stranger_switch";
    public static final String STRANGER_ALARM = "stranger_alarm";
    public static final String STRANGER_SWITCH_SIGNAL = "stranger_switch_signal";
    public static final String STRANGER_WEIGAND_SIGNAL = "stranger_weigand_signal";
    public static final String ALIVE_SWITCH = "alive_switch";
    public static final String HELMET_SWITCH = "helmet_switch";
    public static final String PASSWORD = "password";

    /**
     * 创建sp
     *
     * @param context
     * @return
     */
    private static SharedPreferences createSP(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    /**
     * 设置是否初始化过
     *
     * @param context
     * @param isFirst
     */
    public static void setIsFirst(Context context, boolean isFirst) {
        createSP(context)
                .edit()
                .putBoolean(IS_FIRST, isFirst)
                .commit();
    }

    /**
     * 是否初始化
     *
     * @param context
     * @return
     */
    public static boolean isFirst(Context context) {
        return createSP(context).getBoolean(IS_FIRST, true);
    }

    /**
     * 更新配置参数
     *
     * @param dto
     */
    public static void setParam(Context context, ParamDTO dto) {
        createSP(context)
                .edit()
                .putString(SCREEN_LIST, DataUtils.object2JsonString(dto.getScreenList()))
                .putString(DIST_CD, dto.getDistCd())
                .putInt(MATCH_RATE, dto.getMatchRate())
                .putInt(MATCH_TIME, dto.getMatchTime())
                .putInt(TIME_WINDOW, dto.getTimeWindow())
                .putInt(MODE, dto.getMode())
                .putFloat(RELAY_TIME, dto.getRelayTime())
                .putInt(SWITCH_SIGNAL, dto.getSwitchSignal())
                .putInt(WIEGAND_SIGNAL, dto.getWiegandSignal())
                .putInt(PERSON_SIGNAL, dto.getPersonSignal())
                .putInt(VOICE_SWITCH, dto.getVoiceSwitch())
                .putInt(SOUND, dto.getSound())
                .putInt(SCREEN_SWITCH, dto.getScreenSwitch())
                .putInt(STRANGER_SWITCH, dto.getStrangerSwitch())
                .putInt(STRANGER_ALARM, dto.getStrangerAlarm())
                .putInt(STRANGER_SWITCH_SIGNAL, dto.getStrangerSwitchSignal())
                .putInt(STRANGER_WEIGAND_SIGNAL, dto.getStrangerWiegandSignal())
                .putInt(ALIVE_SWITCH, dto.getAliveSwitch())
                .putInt(HELMET_SWITCH, dto.getHelmetSwitch())
                .commit();
    }

    /**
     * 获取屏幕提示文本列表
     *
     * @param context
     * @return
     */
    public static List<ScreenDTO> getScreenList(Context context) {
        String jsonScreenList = createSP(context).getString(SCREEN_LIST, "");
        if (jsonScreenList == null || jsonScreenList.isEmpty()) {
            return new ArrayList<>();
        } else {
            return DataUtils.jsonStringToList(jsonScreenList, ScreenDTO.class);
        }
    }

    /**
     * 获取识别距离编码  默认1m
     *
     * @param context
     * @return
     */
    public static float getDistCd(Context context) {
        String distCd = createSP(context).getString(DIST_CD, "");
        float dist = 1f;
        if (distCd.equals("30000.100.100")) {
            dist = 0.5f;
        } else if (distCd.equals("30000.100.120")) {
            dist = 1f;
        } else if (distCd.equals("30000.100.140")) {
            dist = 1.5f;
        } else if (distCd.equals("30000.100.160")) {
            dist = 2f;
        } else if (distCd.equals("30000.100.180")) {
            dist = 3f;
        }
        return dist;
    }

    /**
     * 获取识别准确率
     *
     * @param context
     * @return
     */
    public static int getMatchRate(Context context) {
        return createSP(context).getInt(MATCH_RATE, 0);
    }

    /**
     * 获取识别时间
     *
     * @param context
     * @return
     */
    public static int getMatchTime(Context context) {
        return createSP(context).getInt(MATCH_TIME, 0);
    }

    /**
     * 获取时间窗（分钟）
     *
     * @param context
     * @return
     */
    public static int getTimeWindow(Context context) {
        return createSP(context).getInt(TIME_WINDOW, 0);
    }

    /**
     * 获取识别模式[0-刷脸 1-刷卡 2-刷卡或刷脸 3-刷卡加刷脸 4-设备禁用,禁止开门]
     *
     * @param context
     * @return
     */
    public static int getMode(Context context) {
        return createSP(context).getInt(MODE, 0);
    }

    /**
     * 获取继电器控制时间 秒
     *
     * @param context
     * @return
     */
    public static float getRelayTime(Context context) {
        return createSP(context).getFloat(RELAY_TIME, 0);
    }

    /**
     * 获取串口输出-开关信号[ 0-不输出 1-输出]
     *
     * @param context
     * @return
     */
    public static int getSwitchSignal(Context context) {
        return createSP(context).getInt(SWITCH_SIGNAL, 0);
    }

    /**
     * 获取串口输出-韦根信号[ 0-不输出 1-输出]
     *
     * @param context
     * @return
     */
    public static int getWiegandSignal(Context context) {
        return createSP(context).getInt(WIEGAND_SIGNAL, 0);
    }

    /**
     * 获取串口输出-人员信号[ 0-不输出 1-输出]
     *
     * @param context
     * @return
     */
    public static int getPersonSignal(Context context) {
        return createSP(context).getInt(PERSON_SIGNAL, 0);
    }

    /**
     * 获取语音播报开关[ 0-关闭 1-打开]
     *
     * @param context
     * @return
     */
    public static int getVoiceSwitch(Context context) {
        return createSP(context).getInt(VOICE_SWITCH, 0);
    }

    /**
     * 获取语音播报声音大小
     *
     * @param context
     * @return
     */
    public static int getSound(Context context) {
        return createSP(context).getInt(SOUND, 0);
    }

    /**
     * 获取屏幕显示开关[ 0-关闭 1-打开]
     *
     * @param context
     * @return
     */
    public static int getScreenSwitch(Context context) {
        return createSP(context).getInt(SCREEN_SWITCH, 0);
    }

    /**
     * 获取陌生人开关[ 0-关闭 1-打开]
     *
     * @param context
     * @return
     */
    public static int getStrangerSwitch(Context context) {
        return createSP(context).getInt(STRANGER_SWITCH, 0);
    }

    /**
     * 获取陌生人警报[ 0-关闭 1-打开]
     *
     * @param context
     * @return
     */
    public static int getStrangerAlarm(Context context) {
        return createSP(context).getInt(STRANGER_ALARM, 0);
    }

    /**
     * 获取陌生人开关-开关信号[ 0-不输出 1-输出]
     *
     * @param context
     * @return
     */
    public static int getStrangerSwitchSignal(Context context) {
        return createSP(context).getInt(STRANGER_SWITCH_SIGNAL, 0);
    }

    /**
     * 获取陌生人开关-韦根信号[ 0-不输出 1-输出]
     *
     * @param context
     * @return
     */
    public static int getStrangerWiegandSignal(Context context) {
        return createSP(context).getInt(STRANGER_WEIGAND_SIGNAL, 0);
    }

    /**
     * 获取活体检测开关[ 0-关闭 1-打开]
     *
     * @param context
     * @return
     */
    public static int getAliveSwitch(Context context) {
        return createSP(context).getInt(ALIVE_SWITCH, 0);
    }

    /**
     * 获取安全帽检测开关
     *
     * @param context
     * @return
     */
    public static int getHelmetSwitch(Context context) {
        return createSP(context).getInt(HELMET_SWITCH, 0);
    }

    /**
     * 设置密码
     *
     * @param context
     * @param password
     */
    public static void setPassword(Context context, String password) {
        createSP(context)
                .edit()
                .putString(PASSWORD, password)
                .commit();
    }

    /**
     * 获取密码  默认123456
     *
     * @param context
     * @return
     */
    public static String getPassword(Context context) {
        return createSP(context).getString(PASSWORD, "123456");
    }

    /**
     * 设置首页项目名称
     * @param context
     * @param name
     * @return
     */
    public static void setProjectName(Context context,String name){
        createSP(context)
                .edit()
                .putString(PROJECT_NAME,name)
                .commit();
    }

    /**
     * 获取首页项目名称
     * @param context
     * @return
     */
    public static String getProjectName(Context context){
        return createSP(context).getString(PROJECT_NAME,"");
    }

}
