package com.wudi.facegate.utils;

import android.media.AudioAttributes;
import android.media.SoundPool;

import java.util.Map;

/**
 * 音频池工具
 * Created by wudi on 2020/6/13.
 */

public class SoundPoolUtil {

    private static volatile SoundPoolUtil soundPoolUtil;
    private SoundPool soundPool;//音频播放池
    private AudioAttributes attributes;//音频播放参数
    private int[] voiceIds = new int[6];//音频id

    public static SoundPoolUtil getInstance(){
        if(soundPoolUtil == null){
            synchronized (SoundPoolUtil.class){
                if(soundPoolUtil == null){
                    soundPoolUtil = new SoundPoolUtil();
                }
            }
        }
        return soundPoolUtil;
    }

    public SoundPoolUtil() {
        attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build();
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

            }
        });
    }

    /**
     * 加载音频资源
     */
    public void load(){
        voiceIds[0] = soundPool.load("/sdcard/FaceGATE/Voice/0.mp3",1);
        voiceIds[1] = soundPool.load("/sdcard/FaceGATE/Voice/1.mp3",1);
        voiceIds[2] = soundPool.load("/sdcard/FaceGATE/Voice/2.mp3",1);
        voiceIds[3] = soundPool.load("/sdcard/FaceGATE/Voice/3.mp3",1);
        voiceIds[4] = soundPool.load("/sdcard/FaceGATE/Voice/4.mp3",1);
        voiceIds[5] = soundPool.load("/sdcard/FaceGATE/Voice/5.mp3",1);
    }

    /**
     * 播放
     * @param index
     */
    public void play(int index){
        soundPool.play(voiceIds[index],1,1,1,0,1);
    }

}
