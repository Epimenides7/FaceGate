#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include "UltraFace.hpp"

#define TAG "FaceSDKNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

using namespace std;

static UltraFace *ultra;
bool detection_sdk_init_ok = false;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_epimenides_myface_UltraNet_FaceSDKNative_FaceDetectionModelInit(JNIEnv *env, jobject thiz,
                                                                         jstring face_detection_model_path) {
    LOGD("JNI init native sdk");
    if (detection_sdk_init_ok) {
        LOGD("sdk already init");
        return true;
    }
    jboolean tRet = false;
    if (NULL == face_detection_model_path) {
        LOGD("model dir is empty");
        return tRet;
    }

    //获取模型的绝对路径的目录（不是/aaa/bbb.bin这样的路径，是/aaa/)
    const char *faceDetectionModelPath = env->GetStringUTFChars(face_detection_model_path, 0);
    if (NULL == faceDetectionModelPath) {
        LOGD("model dir is empty");
        return tRet;
    }

    string tFaceModelDir = faceDetectionModelPath;
    string tLastChar = tFaceModelDir.substr(tFaceModelDir.length()-1, 1);
    //RFB-320
    //RFB-320-quant-ADMM-32
    //RFB-320-quant-KL-5792
    //slim-320
    //slim-320-quant-ADMM-50
    //量化模型需要使用CPU方式 net.cpp中修改 sch_config.type = (MNNForwardType)MNN_FORWARD_CPU
    // change names
    string str = tFaceModelDir + "RFB-320-quant-ADMM-32.mnn";

    ultra = new  UltraFace(str, 320, 240, 4, 0.65 ); // config model input

    env->ReleaseStringUTFChars(face_detection_model_path, faceDetectionModelPath);
    detection_sdk_init_ok = true;
    tRet = true;

    return tRet;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_epimenides_myface_UltraNet_FaceSDKNative_FaceDetect(JNIEnv *env, jobject thiz,
                                                             jbyteArray image_date,
                                                             jint image_width, jint image_height,
                                                             jint image_channel) {
    if(!detection_sdk_init_ok){
        LOGD("sdk not init");
        return NULL;
    }

    int tImageDateLen = env->GetArrayLength(image_date);
    if(image_channel == tImageDateLen / image_height / image_height){
        LOGD("imgW=%d, imgH=%d,imgC=%d",image_width,image_height,image_channel);
    }
    else{
        LOGD("img data format error");
        return NULL;
    }

    jbyte *imageDate = env->GetByteArrayElements(image_date, NULL);
    if (NULL == imageDate){
        LOGD("img data is null");
        return NULL;
    }

    if(image_width<200||image_width<200){
        LOGD("img is too small");
        return NULL;
    }


    std::vector<FaceInfo> face_info;
    //detect face
    ultra ->detect((unsigned char*)imageDate, image_width, image_height, image_channel, face_info );

    int32_t num_face = static_cast<int32_t>(face_info.size());

    int out_size = 1+num_face*4;
    int *allfaceInfo = new int[out_size];
    allfaceInfo[0] = num_face;
    for (int i=0; i<num_face; i++) {

        allfaceInfo[4*i+1] = face_info[i].x1;//left
        allfaceInfo[4*i+2] = face_info[i].y1;//top
        allfaceInfo[4*i+3] = face_info[i].x2;//right
        allfaceInfo[4*i+4] = face_info[i].y2;//bottom

    }

    jintArray tFaceInfo = env->NewIntArray(out_size);
    env->SetIntArrayRegion(tFaceInfo, 0, out_size, allfaceInfo);
    env->ReleaseByteArrayElements(image_date, imageDate, 0);


    delete [] allfaceInfo;

    return tFaceInfo;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_epimenides_myface_UltraNet_FaceSDKNative_FaceDetectionModelUnInit(JNIEnv *env,
                                                                           jobject thiz) {
    jboolean tDetectionUnInit = false;

    if (!detection_sdk_init_ok) {
        LOGD("sdk not inited, do nothing");
        return true;
    }
    delete ultra;
    detection_sdk_init_ok = false;
    tDetectionUnInit = true;
    LOGD("sdk release ok");
    return tDetectionUnInit;
}

