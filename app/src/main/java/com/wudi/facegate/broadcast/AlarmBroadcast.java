package com.wudi.facegate.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wudi.facegate.event.AlarmEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * 定时器启动广播
 * Created by wudi on 2020/6/2.
 */

public class AlarmBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("alarm_start")){
            //定时器触发
            EventBus.getDefault().post(new AlarmEvent());
        }
    }

}
