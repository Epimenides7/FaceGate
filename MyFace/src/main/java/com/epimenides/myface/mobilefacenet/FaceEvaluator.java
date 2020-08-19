package com.epimenides.myface.mobilefacenet;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Epimenides
 * @Package com.epimenides.myface.mobilefacenet
 * @date 2020/8/15 16:52
 * description :
 */
public class FaceEvaluator {
    /**
     * 静态方法，用于1对n的人脸比对，
     * 将特征值和数据库中的所有值进行一一比较，两两相减
     * 得出距离最小，并且小于阈值的，判断为同一个人
     * 若判断最小的距离也大于给定的distance，
     * 则判断为陌生人
     * distance根据实际情况调整
     *
     * @param mapList  包含名字与特征值对应的键值对
     * @param fea      人脸的特征值
     * @param distance 人脸距离阈值
     * @return 返回检测到的人的名字，或者返回陌生人
     */
    public static String evaluate(Map mapList, float[] fea, float distance) {
        // 根据输入的数值，计算实际的阈值大小
        float trueDistance = distance * -0.25f + 30f;

        FaceFeature faceFeature = new FaceFeature(fea);
        List<Double> scores = new ArrayList<>();
        List<String> bitmap2_path = new ArrayList<>();

        Set<String> keys = mapList.keySet();
        for (String key : keys) {
            FaceFeature faceFeatureTemp = new FaceFeature((float[]) mapList.get(key));
            double compare = faceFeatureTemp.compare(faceFeature);
            scores.add(compare);
            bitmap2_path.add(key);
        }
        double min = 20000;

        for (int m = 0; m < scores.size(); m++) {
            System.out.println(scores.get(m));
            if (scores.get(m) < min) {
                min = scores.get(m);
            }
        }
        if (min > trueDistance) {
            return "stranger";
        }
        int index = scores.indexOf(min);

        return bitmap2_path.get(index);
    }


    /**
     * 多帧人脸判断
     *
     * @param mapList  人脸与对应的特征的键值对
     * @param features 传入包含多张人脸特征的List
     * @param distance 0-100,越高越严格
     * @param scale    0-1之间，百分比，高于此百分比才判断为该人，建议设置大于0.5
     * @return 返回识别出来的人的名字，陌生人返回Unknown
     */
    public static String evaluate(Map mapList, Map<Long, String> stringStringMap, List<float[]> features, float distance, float scale) {
        // 键值对，用于存放每个特征出现的次数
        Map<String, Integer> namesMap = new HashMap<>();

        // 根据输入的数值，计算实际的阈值大小
        float trueDistance = distance * 0.01f - 0.4f;

        Log.d("myFace", "trueDistance:"+trueDistance+":"+distance);
        /*
        遍历List中的每一个特征值，分别与数据库中的数据进行比较
        1.得到距离最小的特征
        2.判断该距离值是否小于阈值，是的话得到该人的名字，否则返回“Unknown”
        3.将名字与出现的次数存放在namesMap中
        4.遍历namesMap中的value值，是否达到scale的比例
         */
        for (int i = 0; i < features.size(); i++) {

            Log.d("myFace", "evaluate: *************");
            FaceFeature faceFeature = new FaceFeature(features.get(i));
            List<Double> scores = new ArrayList<>();
            List<String> bitmap_path = new ArrayList<>();

            Set<String> keys = mapList.keySet();
            for (String key : keys) {
                FaceFeature faceFeatureTemp = new FaceFeature((float[]) mapList.get(key));
                double compare = faceFeatureTemp.compare(faceFeature);
                Log.d("myFace", "特征是：" + compare + " : 名字是： " + stringStringMap.get(new Long(key)));
                scores.add(compare);
                bitmap_path.add(key);
            }

            double max = trueDistance;
            int index = -1;

            // 获得该张人脸对应的距离得分
            for (int m = 0; m < scores.size(); m++) {
                if (scores.get(m) > max) {
                    max = scores.get(m);
                    index = m;
                }
            }
            String name = null;

            if (index == -1) {
                name = "stranger";
            }
            // 如果距离大于设定阈值，判断为陌生人
            else { // 否则获得该人的名字
                name = bitmap_path.get(index);
            }
            // 统计名字出现的次数
            Integer count = namesMap.get(name);
            namesMap.put(name, ((count == null) ? 1 : count + 1));
        }

        // 遍历名字，如果出现的概率大于scale，则判断为该人
        String finalName = "stranger";
        for (Map.Entry<String, Integer> stringIntegerEntry : namesMap.entrySet()) {
            Object key = ((Map.Entry) stringIntegerEntry).getKey();
            Object val = ((Map.Entry) stringIntegerEntry).getValue();
            if ((int) val >= scale * features.size()) {
                finalName = (String) key;
//                Log.d("myFace", "最后选择：" + stringStringMap.get(new Long((String) key)) + ":出现次数：" + val + " :一共张数：" + features.size());
            }
        }

        return finalName;
    }
}
