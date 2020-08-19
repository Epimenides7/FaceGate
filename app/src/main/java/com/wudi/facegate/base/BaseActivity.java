package com.wudi.facegate.base;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.wudi.facegate.BuildConfig;
import com.wudi.facegate.utils.MyConstants;
import com.wudi.facegate.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.Executors;

import butterknife.ButterKnife;


/**
 * Created by admin on 2016/7/27.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private Toast mToast;
    private ConnectivityManager manager; //网络状态管理器
    private ProgressDialog progressDialog;
    protected BaseActivity mActivity;
    protected String SN;//序列号
    protected String ipAddress;//ip地址
    protected String version;//版本
    protected File recordDir;//识别记录目录
    protected File voiceLibraryDir;//提示音目录
    protected File apkDir;//apk保存文件夹

    @SuppressLint({"InlinedApi", "SourceLockedOrientationActivity"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);//竖屏
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mActivity = this;
        initBaseData();
        setContentView(getLayout());
        ButterKnife.bind(this);
//        ViewGroup root = (ViewGroup) mActivity.getWindow().getDecorView();
//        setTypeface(root);
        init();
    }

    /**
     * 初始化基本参数
     */
    private void initBaseData(){
        SN = Build.SERIAL;
        ipAddress = Utils.getIP(mActivity);
        version = BuildConfig.VERSION_NAME;
    }

    /**
     * 获取公共请求参数参数JSONObject
     * @return
     */
    protected JSONObject getPublicJson() {
        JSONObject jsonObject = new JSONObject();
        String timestamp = System.currentTimeMillis()+"";
        String sign = Utils.encode(MyConstants.KEY_ID+MyConstants.KEY+timestamp);
        try {
            jsonObject.put("accessKeyId",MyConstants.KEY_ID);//密钥ID
            jsonObject.put("timestamp",timestamp);//时间戳
            jsonObject.put("sign",sign);//时间戳
            jsonObject.put("sn",SN);//设备序列号
            jsonObject.put("ip",ipAddress);//ip地址
            jsonObject.put("version",version);//版本号
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }



    /**
     * 全局设置字体
     * @param root
     */
    private void setTypeface(ViewGroup root){
        Typeface tf = Typeface.createFromAsset(getAssets(),"tt.otf");
        for (int i = 0; i < root.getChildCount(); i++) {
            View v = root.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface(tf);
            } else if (v instanceof Button) {
                ((Button) v).setTypeface(tf);
            } else if (v instanceof EditText) {
                ((EditText) v).setTypeface(tf);
            } else if (v instanceof ViewGroup) {
                setTypeface((ViewGroup) v);
            }
        }
    }

    /**
     * 新建文件夹用来存储人脸照片
     */
    protected void createFolder(){
        recordDir = new File(Environment.getExternalStorageDirectory().toString() + "/FaceGATE/Record");
        voiceLibraryDir = new File(Environment.getExternalStorageDirectory().toString() + "/FaceGATE/Voice");
        apkDir = new File(Environment.getExternalStorageDirectory().toString() + "/FaceGATE/APK");
        if(!recordDir.exists()){
            recordDir.mkdirs();//创建识别记录文件夹
        }
        if(!voiceLibraryDir.exists()){
            voiceLibraryDir.mkdirs();//创建存储提示音文件夹
        }
        if(!apkDir.exists()){
            apkDir.mkdir();//创建apk存储文件夹
        }
    }

    /**
     * 清除文件夹中所有文件  或直接删除某一个文件
     * @param file
     */
    protected void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                deleteFile(f);
            }
        } else if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 移动文件 并重命名
     * @param srcFileName    源文件完整路径
     * @param destDirName    目的目录完整路径
     * @return 文件移动成功返回true，否则返回false
     */
    protected boolean moveFile(String srcFileName, String destDirName,String newName) {
        File srcFile = new File(srcFileName);
        if(!srcFile.exists() || !srcFile.isFile())
            return false;
        File destDir = new File(destDirName);
        if (!destDir.exists())
            destDir.mkdirs();
        return srcFile.renameTo(new File(destDirName + File.separator + newName));
    }

    @Override
    protected void onDestroy() {
        //销毁资源
        clearHave();
        super.onDestroy();
    }


    /*监测网络状态*/
    @SuppressLint("MissingPermission")
    protected boolean checkNet() {
        if (manager == null) {
            manager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        if (manager.getActiveNetworkInfo() == null) {
            return false;
        } else {
            return true;
        }
    }

    /*清理占用*/
    protected void clearHave() {
        manager = null;
        mToast = null;
        progressDialog = null;
    }

    protected abstract int getLayout();

    protected abstract void init();


    /**
     * Toast弹出
     *
     * @param msg
     */
    public void show_Toast(String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(msg == null ? "程序出错" : msg);
        }
        mToast.show();
    }


    /**
     * 显示进度条dialog
     */
    public void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setTitle("正在请求..");
        }
        progressDialog.show();
    }

    /**
     * 隐藏进度条dialog
     */
    public void hintProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }


}
