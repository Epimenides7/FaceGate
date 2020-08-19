#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <mat.h>

// ncnn
#include "net.h"
#include "recognize.h"
#include "detect.h"

using namespace Face;

#define TAG "MtcnnSo"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)
static Detect *mDetect;
static Recognize *mRecognize;

//sdk是否初始化成功
bool detection_sdk_init_ok = false;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_epimenides_myface_FaceLib_FaceDetectionModelInit(JNIEnv *env, jobject thiz,
                                                          jstring face_detection_model_path) {
    LOGD("人脸检测模型初始化开始");
    // 如果已经初试化就直接返回
    if (detection_sdk_init_ok) {
        // LOGD("人脸检测模型已经导入")
        return true;
    }
    jboolean tRet = false;
    if (NULL == face_detection_model_path){
        // LOGD("导入的人脸检测的目录为空");
        return tRet;
    }

    // 获取模型的绝对路径的目录(不是/aaa/bbb.bin这样的路径，是/aaa/)
    const char* faceDetectionModelPath = env->GetStringUTFChars(face_detection_model_path, 0);
    if (NULL == faceDetectionModelPath){
        return tRet;
    }

    string tFaceModelDir = faceDetectionModelPath;
    string tLastChar     = tFaceModelDir.substr((tFaceModelDir.length() - 1, 1));
    // LOGD("init, tFaceModelDir last =%s", tLastChar.c_str());
    // 目录补齐/
    if ("\\" == tLastChar){
        tFaceModelDir = tFaceModelDir.substr(0, tFaceModelDir.length() - 1) + "/";
    } else if (tLastChar != "/"){
        tFaceModelDir += '/';
    }
    //.c_str()函数从string类中得到c类型的字符数组，返回当前字符串类的首字符地址。
    LOGD("init, tFaceModelDir=%s", tFaceModelDir.c_str());

    // 注意此处没有判断是否正确导入。。。
    mDetect = new Detect(tFaceModelDir);
    mRecognize = new Recognize(tFaceModelDir);
    mDetect->SetMinFace(40);
    mDetect->SetNumThreads(2);
    mRecognize->SetThreadNum(2);

    env->ReleaseStringUTFChars(face_detection_model_path, faceDetectionModelPath);
    detection_sdk_init_ok = true;
    tRet = true;
    return tRet;
}

// 检测最大人脸
extern "C"
JNIEXPORT jintArray JNICALL
Java_com_epimenides_myface_FaceLib_MaxFaceDetect(JNIEnv *env, jobject thiz, jbyteArray image_date, jint image_width, jint image_height,
                                                 jint image_channel) {
    // LOGD ("人脸检测开始")
    if (!detection_sdk_init_ok){
        LOGD("人脸检测MTCNN模型SDK未初始化，直接返回空");
        return NULL;
    }

    int tImageDateLen = env->GetArrayLength(image_date);
    if (image_channel == tImageDateLen / image_width / image_height){
        LOGD("数据宽=%d,高=%d,通道=%d", image_width, image_height, image_channel);
    } else{
        LOGD("数据长宽通道不匹配,直接返回空值");
        return NULL;
    }

    jbyte *imageDate = env->GetByteArrayElements(image_date, NULL);
    if (NULL == imageDate){
        LOGD("导入数据为空，直接返回空");
        env->ReleaseByteArrayElements(image_date, imageDate, 0);
        return NULL;
    }

    if(image_width<20||image_height<20) {
        LOGD("导入数据的宽和高小于20,直接返回空");
        env->ReleaseByteArrayElements(image_date, imageDate, 0);
        return NULL;
    }

    // 通道数量测试
    if (3 == image_channel || 4 == image_channel){
        //图像的通道数量只能是3或者4
    } else{
        LOGD("图像的通道只能是3或4，直接返回空");
        env->ReleaseByteArrayElements(image_date, imageDate, 0);
        return NULL;
    }

    unsigned char *faceImageCharDate = (unsigned char*)imageDate;
    ncnn::Mat ncnn_img;
    if(image_channel==3){
        ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, ncnn::Mat::PIXEL_BGR2RGB,
                                          image_width, image_height);
    } else{
        ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, ncnn::Mat::PIXEL_RGBA2RGB, image_width, image_height);
    }

    std::vector<Bbox> finalBbox;
    mDetect->detectMaxFace(ncnn_img, finalBbox);

    int32_t num_face = static_cast<int32_t>(finalBbox.size());
    LOGD("检测到的人脸数目：%d\n", num_face);

    int out_size = 1+num_face*14;
    //  LOGD("内部人脸检测完成,开始导出数据");
    int *faceInfo = new int[out_size];
    faceInfo[0] = num_face;
    for(int i=0;i<num_face;i++){
        faceInfo[14*i+1] = finalBbox[i].x1;//left
        faceInfo[14*i+2] = finalBbox[i].y1;//top
        faceInfo[14*i+3] = finalBbox[i].x2;//right
        faceInfo[14*i+4] = finalBbox[i].y2;//bottom
        for (int j =0;j<10;j++){
            faceInfo[14*i+5+j]=static_cast<int>(finalBbox[i].ppoint[j]);
        }
    }

    jintArray tFaceInfo = env->NewIntArray(out_size);
    env->SetIntArrayRegion(tFaceInfo, 0, out_size, faceInfo);
    //  LOGD("内部人脸检测完成,导出数据成功");
    delete[] faceInfo;
    env->ReleaseByteArrayElements(image_date, imageDate, 0);
    return tFaceInfo;
}

// 设置人脸检测的最小人脸大小
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_epimenides_myface_FaceLib_SetMinFaceSize(JNIEnv *env, jobject thiz, jint min_size) {
    if(!detection_sdk_init_ok){
        LOGD("人脸检测MTCNN模型SDK未初始化，直接返回");
        return false;
    }

    if (min_size<=20){
        min_size=20;
    }

    mDetect->SetMinFace(min_size);
    return true;
}

// 设置人脸模型推理的线程数量
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_epimenides_myface_FaceLib_SetThreadsNumber(JNIEnv *env, jobject thiz, jint threads_number) {
    if (!detection_sdk_init_ok){
        LOGD("人脸检测MTCNN模型SDK未初始化,直接返回");
        return false;
    }

    if (threads_number!=1 && threads_number!=2 && threads_number!=4 && threads_number!=8){
        LOGD("线程数量只能设置1，2，4，8");
        return false;
    }

    mDetect->SetNumThreads(threads_number);
    return true;
}

// 循环测试时间
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_epimenides_myface_FaceLib_SetTimeCount(JNIEnv *env, jobject thiz, jint time_count) {
    if (!detection_sdk_init_ok){
        LOGD("人脸检测MTCNN模型SDK未初始化，直接返回");
        return false;
    }
    mDetect->SetTimeCount(time_count);
    return true;
}

// 提取人脸特征值
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_epimenides_myface_FaceLib_extractFeature(JNIEnv *env, jobject thiz, jbyteArray face_date,
                                                  jint w, jint h, jintArray landmarks) {
    jbyte *faceDate = env->GetByteArrayElements(face_date, NULL);

    unsigned char *faceImageCharDate = (unsigned char *) faceDate;

    // 得到五个人脸坐标点
    jint *mtcnn_landmarks = env->GetIntArrayElements(landmarks, NULL);

    int *mtcnnLandmarks = (int *)mtcnn_landmarks;

    // 将传入的Mat转换为ncnn的::Mat
    ncnn::Mat ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, ncnn::Mat::PIXEL_RGBA2RGB, w, h);

    //将传入的人脸对齐
    ncnn::Mat det = mRecognize->preprocess(ncnn_img, mtcnnLandmarks);

    // 得到人脸特征(得到vector<float>类型的特征向量)
    std::vector<float> faceFeature;
    mRecognize->start(det, faceFeature);
    // 新建一个float类型的数组用于转换
    jfloatArray faceFeatureArray = env->NewFloatArray(faceFeature.size());
    env->SetFloatArrayRegion(faceFeatureArray,0, faceFeature.size(), faceFeature.data());

    // 将该内存空间释放掉
    env->ReleaseByteArrayElements(face_date, faceDate, 0);
    env->ReleaseIntArrayElements(landmarks, mtcnn_landmarks, 0);

    // 返回float[]类型的数组
    return faceFeatureArray;
}

// 人脸特征值比对
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_epimenides_myface_FaceLib_compareFeature(JNIEnv *env, jobject thiz, jfloatArray feature1,
                                                  jfloatArray feature2) {
    jfloat* featureData1 = (jfloat*)env->GetFloatArrayElements(feature1, 0);
    jsize featureSize1 = env->GetArrayLength(feature1);
    jfloat* featureData2 = (jfloat*)env->GetFloatArrayElements(feature2, 0);
    jsize featureSize2 = env->GetArrayLength(feature2);
    // 创建转换的<float>类型的vector
    std::vector<float> featureVector1(featureSize1), featureVector2(featureSize1);
    if(featureSize1 != featureSize2){
        return 0;
    }
    for(int i=0; i < featureSize1; i++){
        featureVector1.push_back(featureData1[i]);
        featureVector2.push_back(featureData2[i]);
    }
    double score = calculSimilar(featureVector1, featureVector2, 1);
    env->ReleaseFloatArrayElements(feature1, featureData1, 0);
    env->ReleaseFloatArrayElements(feature2, featureData2, 0);
    return score;
}

