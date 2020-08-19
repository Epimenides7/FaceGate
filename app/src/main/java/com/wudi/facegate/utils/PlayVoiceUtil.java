package com.wudi.facegate.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.SoundPool;

import com.apkfuns.logutils.LogUtils;

import java.io.IOException;
import java.security.acl.LastOwnerException;

/**
 * 语音播放工具类
 * Created by wudi on 2020/3/31.
 */

public class PlayVoiceUtil {
    private static Context mContext;
    private static MediaPlayer mediaPlayer;
    private static long lastPlayTime = 0;//上次播放时间
    private static String lastPath = "";//上次播放的语音

    public static void init(Context context){
        mContext = context;
        createMP();
    }

    private static void createMP(){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                LogUtils.d("播放出错了："+extra);
                return false;
            }
        });
    }

    /**
     * 语音播放 assets
     * @param name  资源名称
     */
    public static void playAssets(String name){
        try {
            //播放 assets/a2.mp3 音乐文件
            AssetFileDescriptor fd = mContext.getAssets().openFd(name);
            if(mediaPlayer == null){
                createMP();
            }else{
                mediaPlayer.reset();
            }
            mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 语音播放raw
     * @param source
     */
    public static void playRaw(int source){
        mediaPlayer = MediaPlayer.create(mContext,source);
        mediaPlayer.start();
    }

    /**
     * 播放本地资源文件
     * @param path
     */
    public static  void playFile(String path,Context context){
        if(path.equals(lastPath) && (System.currentTimeMillis() - lastPlayTime < 2000)){
            //同样的语音2秒之内不重复播放
            return;
        }else{
            lastPath = path;
            lastPlayTime = System.currentTimeMillis();
        }
        float volume = SPU.getSound(context)/100f;
        try {
            if(mediaPlayer == null){
                createMP();
            }else{
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
            mediaPlayer.setVolume(volume,volume);
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (IllegalStateException e){
            e.printStackTrace();
            mediaPlayer = null;
            playFile(path,context);//重新播放
        }
    }

    /**
     * 清理占用
     */
    public static void clear(){
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

}
