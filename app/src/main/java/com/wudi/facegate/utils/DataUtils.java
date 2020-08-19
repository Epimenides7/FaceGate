package com.wudi.facegate.utils;


import com.apkfuns.logutils.LogUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 数据转换工具类
 */
public class DataUtils {

    /**
     * 一个byte为8个二进制位
     * 通常4个二进制转为一个十六进制
     * 一个byte可转为两个十六进制字符 如 f5
     * @param b
     * @return
     */
    public static String byteToHex(byte b){
        String hex = Integer.toHexString(b & 0xff);
        if(hex.length() < 2){
            hex = "0"+hex;
        }
        return hex;
    }

    /**
     * byte数组转Hex字符串
     * @param b
     * @return
     */
    public static String byteArrayToHex(byte[] b){
        StringBuffer hs = new StringBuffer(b.length);
        String stmp = "";
        int len = b.length;
        for (int n = 0; n < len; n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            if (stmp.length() == 1)
                hs = hs.append("0").append(stmp);
            else {
                hs = hs.append(stmp);
            }
        }
        return String.valueOf(hs);
    }

    /**
     * 十六进制转byte
     * hex必须为十六进制字符串
     * @param hex
     * @return
     */
    public static byte hexToByte(String hex){
        return (byte) Integer.parseInt(hex,16);
    }

    /**
     * 十六进制字符串转byte[]
     * hex必须为十六进制字符串
     * @param hex
     * @return
     */
    public static byte[] hexToByteArray(String hex){
        int hexlen = hex.length();
        byte[] result;
        if (hexlen % 2 == 1){
            //奇数
            hexlen++;
            result = new byte[(hexlen/2)];
            hex+="0";
        }else {
            //偶数
            result = new byte[(hexlen/2)];
        }
        int j=0;
        for (int i = 0; i < hexlen; i+=2){
            result[j]=hexToByte(hex.substring(i,i+2));
            j++;
        }
        return result;
    }

    /**
     * byte数组转字符串，含空值
     * @param src
     * @param dec
     * @param length
     * @return
     */
    public static String bytes2Str(byte[] src, int dec, int length) {
        byte[] temp = new byte[length];
        System.arraycopy(src, dec, temp, 0, length);
        return new String(temp);
    }

    /**
     * byte数组转字符串，不含空值
     * @param src
     * @return
     */
    public static String bytes2Str(byte[] src) {
        return new String(src);
    }

    /**
     * byte数组截取掉后面的空值
     * @param src
     * @param dec
     * @param length
     * @return
     */
    public static byte[] cutBytes(byte[] src, int dec, int length){
        byte[] temp = new byte[length];
        System.arraycopy(src, dec, temp, 0, length);
        return temp;
    }

    /**
     * 异或校验
     * @param data
     * @return
     */
    public static String checkXor(String data) {
        int checkData = 0;
        for (int i = 0; i < data.length(); i = i + 2) {
            //将十六进制字符串转成十进制
            int start = Integer.parseInt(data.substring(i, i + 2), 16);
            //进行异或运算
            checkData = start ^ checkData;
        }
        return integerToHexString(checkData);
    }

    /**
     * 将十进制整数转为十六进制数，并补位
     */
    public static String integerToHexString(int s) {
        String ss = Integer.toHexString(s);
        if (ss.length() % 2 != 0) {
            ss = "0" + ss;//0F格式
        }
        return ss.toUpperCase();
    }

    /**
     * 实体类转json字符串
     * @param o
     * @return
     */
    public static String object2JsonString(Object o){
        Gson gson = new Gson();
        return gson.toJson(o);
    }

    /**
     * Json字符串转为实体类
     * @param jsonStr
     * @param <T>
     * @return
     */
    public static<T> T jsonString2Object(String jsonStr,Class<T> cls){
        Gson gson = new Gson();
        return (T) gson.fromJson(jsonStr,cls);
    }

    /**
     * Json字符串转实体类List
     * @param jsonString
     * @param cls
     * @param <T>
     * @return
     */
    public static <T> ArrayList<T> jsonStringToList(String jsonString, Class<T> cls) {
        ArrayList<T> list = new ArrayList<>();
        Gson gson = new Gson();
        if (gson != null) {
            JsonArray array = new JsonParser().parse(jsonString).getAsJsonArray();
            for (final JsonElement elem : array) {
                list.add(gson.fromJson(elem,cls));
            }
        }
        return list;
    }

    /**
     * 浮点数组转字符串
     * @param floats
     * @return
     */
    public static String floats2String(float[] floats){
        return Arrays.toString(floats);
    }

    /**
     * 字符串转浮点型小数
     * @param string
     * @return
     */
    public static float[] string2Floats(String string){
        String cut = string.substring(1,string.length()-1);
        String[] strings = cut.split(",");
        float[] floats = new float[strings.length];
        for(int i=0;i<strings.length;i++){
            floats[i] = Float.parseFloat(strings[i]);
        }
        return floats;
    }

    /**
     * 十六进制转十进制数组，高字节在前
     * @return
     */
    public static List<Integer> hex2OctList(String hexStr){
        List<Integer> integerList = new ArrayList<>();
        int start = 0;
        //取2位转换为十进制
        for(int i=2;i<hexStr.length()+1;i+=2){
            String hex = hexStr.substring(start,i);
            int oct = Integer.parseInt(hex,16);
            integerList.add(oct);
            start+=2;
        }
        //排序 高位在前
        Collections.sort(integerList);
        Collections.reverse(integerList);
        return integerList;
    }

    /**
     * 十进制列表转换二进制字符串，并拼接
     * @param oct
     * @return
     */
    public static String octList2BinStr(List<Integer> oct){
        String binStr = "";
        for (int i :oct){
            String str = Integer.toBinaryString(i);
            binStr+=str;
        }
        return binStr;
    }

    /**
     * 二进制转char
     * @param bytes
     * @return
     */
    public static char[] getChars(byte[] bytes) {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes).flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

}
