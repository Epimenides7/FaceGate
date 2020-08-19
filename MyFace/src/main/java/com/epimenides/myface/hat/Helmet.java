package com.epimenides.myface.hat;

import android.app.Activity;


import com.epimenides.myface.env.ClassifierFloat;

import java.io.IOException;
import java.util.List;

/**
 * 用于读取安全帽的TensorFlow文件(初始化)
 * @author kyle-luo
 * @create 2020-06-11-7:51
 */
public class Helmet extends ClassifierFloat {

    public Helmet(Activity activity, int numThreads) throws IOException {
        super(activity, numThreads);
    }

    @Override
    protected String getModelPath() {
        return "hat_v1.tflite";
    }


    public static boolean haveHat(List<Float> scores, float scale) {
        int i = 0;
        for (float score: scores) {
            if(score > 0.5) {
                i += 1;
            }
        }
        return i >= scores.size() * scale;
    }

}
