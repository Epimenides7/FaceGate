package com.wudi.facegate.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.util.Log;

import com.apkfuns.logutils.LogUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

/**
 * 工具类
 * Created by wudi on 2020/5/12.
 */
public class Utils {
    /**
     * 签名生成方法
     * @param data
     * @return
     */
    public static String encode(String data) {
        String ret = null;
        try {
            if ((data != null) && (data.length() > 0)) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(data.getBytes("UTF-8"));
                ret = DataUtils.byteArrayToHex(md.digest());
            }
        } catch (Exception e) {
        }
        return ret;
    }

    /**
     * 获取IP
     *
     * @param context
     * @return
     */
    public static String getIP(Context context) {
        String ip = "0.0.0.0";
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if(info == null){
            return "无网络";
        }
        int type = info.getType();
        if (type == ConnectivityManager.TYPE_ETHERNET) {
            ip = getEtherNetIP();
        } else if (type == ConnectivityManager.TYPE_WIFI) {
            ip = getWifiIP(context);
        }
        return ip;
    }

    /**
     * 获取有线地址
     *
     * @return
     */
    @SuppressLint("LongLogTag")
    public static String getEtherNetIP() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("WifiPreference IpAddress", ex.toString());
        }
        return "0.0.0.0";
    }

    /**
     * 获取wifiIP地址
     *
     * @param context
     * @return
     */
    public static String getWifiIP(Context context) {
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) context
                .getSystemService(android.content.Context.WIFI_SERVICE);
        WifiInfo wifiinfo = wifi.getConnectionInfo();
        int intaddr = wifiinfo.getIpAddress();
        byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff),
                (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff),
                (byte) (intaddr >> 24 & 0xff) };
        InetAddress addr = null;
        try {
            addr = InetAddress.getByAddress(byteaddr);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        String mobileIp = addr.getHostAddress();
        return mobileIp;
    }

    /**
     * bitmap保存到本地
     * @param path
     * @param bm
     */
    public static File saveBitmap(String path, String name, Bitmap bm) {
        File f = new File(path,name+".jpg");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return f;
    }

    /**
     * 字符串日期转long
     * @param date
     * @param sdf
     * @return
     */
    public static long stringDateToLong(String date, SimpleDateFormat sdf){
        long time = 0;
        try {
            time = sdf.parse(date).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }

    /**
     * 根据文件路径截取文件名
     * @param path
     * @return
     */
    public static String getNameFromPath(String path){
        String name = path.substring(path.lastIndexOf("/")+1,path.lastIndexOf("."));
        return name;
    }

}
