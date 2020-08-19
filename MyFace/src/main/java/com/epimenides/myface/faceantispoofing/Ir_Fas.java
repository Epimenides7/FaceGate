package com.epimenides.myface.faceantispoofing;

import android.util.Log;

import java.util.List;

/**
 * @author Epimenides
 * @Package com.epimenides.myface.faceantispoofing
 * @date 2020/8/15 17:06
 * description :
 */
public class Ir_Fas {



    public static String Fas_ir_evaluate(List<String> fas_scores){
        int real_counter = 0;
        int fake_counter = 0;
        for (int i = 0; i < fas_scores.size(); i++) {
            if(fas_scores.get(i) == "True"){
                real_counter += 1;
            }else{
                fake_counter += 1;
            }
        }
        Log.d("Ir_TatalIamge", "ir摄像头共获取到:" + fas_scores.size() + "图像");
        Log.d("Ir_Detected", "共有"+real_counter+"检测到了人脸");
        if(real_counter > fake_counter){
            return "Real";
        }else {
            return "fake";
        }
    }
}
