package com.epimenides.myface.mobilefacenet;

import android.util.Log;

import com.epimenides.myface.FaceLib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Epimenides
 * @Package com.epimenides.myface.mobilefacenet
 * @date 2020/8/15 15:44
 * description :
 */
public class FaceFeature {
    private static final int DIMS=128;
    private float[] feature;
    // 声名一个人脸识别对象
    FaceLib mFace = new FaceLib();

    public FaceFeature() {
        feature = new float[DIMS];
    }

    public FaceFeature(float[] feature){
        this();
        this.feature = feature;
    }

    // 为属性feature设置get方法
    public float[] getFeature(){
        return feature;
    }

    // 比较当前特征和另一个特征之间的相似度
    public double compare(FaceFeature faceFeature){
        double score = 0;
        score = mFace.compareFeature(feature, faceFeature.feature);
        return score;
    }


}
