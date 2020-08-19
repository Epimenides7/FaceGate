package com.wudi.facegate.base;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;

import com.apkfuns.logutils.LogUtils;
import com.epimenides.myface.facedetection.DetectResult;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.BitmapCallback;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.wudi.facegate.activity.MainActivity;
import com.wudi.facegate.greenDao.GreenDaoManager;
import com.wudi.facegate.greenDao.Person;
import com.wudi.facegate.http.JsonCallBack;
import com.wudi.facegate.module.AutoAllDTO;
import com.wudi.facegate.module.AutoDTO;
import com.wudi.facegate.module.BaseDTO;
import com.wudi.facegate.module.ParamDTO;
import com.wudi.facegate.module.PersonDTO;
import com.wudi.facegate.module.UpdateDTO;
import com.wudi.facegate.module.VoiceDTO;
import com.wudi.facegate.utils.DataUtils;
import com.wudi.facegate.utils.FaceUtils;
import com.wudi.facegate.utils.MyConstants;
import com.wudi.facegate.utils.SPU;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;

import cn.com.armt.sdk.DeviceManager;

/**
 * 封装常用接口请求
 * Created by wudi on 2020/6/2.
 */

public abstract class HttpActivity extends BaseActivity {
    protected FaceUtils faceUtils;
    private int voiceDownLoadNum;//语音包下载数量
    private int voiceDownLoadCount;//语音包下载总数
    private int imageDownLoadNum;//图片下载数量
    private int imageDownLoadCount;//图片下载总数
    private List<PersonDTO> addList = new ArrayList<>();//需要添加的人员列表
    private List<Long> removeList = new ArrayList<>();//删除人员列表
    private JSONArray notifyList;//授权结果列表
    private boolean isOnUpdateAPK = false;//是否正在更新apk
    private boolean isOnUpdateParam = false;//是否在更新配置参数
    private boolean isOnUpdatePerson = false;//是否在更新人员信息

    protected abstract void addLog(String log, boolean isWaning);

    protected abstract void changeTitle(String title);

    protected abstract void over();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        faceUtils = FaceUtils.getInstance(getAssets(), mActivity);
    }

    /**
     * 开始网络请求
     */
    protected void startHttp() {
        if (!checkNet()) {
            //网络状态不好，结束
            return;
        }
        checkVersion();
    }

    /**
     * 检查软件版本
     */
    protected void checkVersion() {
        if(isOnUpdateAPK){
            return;
        }else{
            isOnUpdateAPK = true;
        }
        addLog("正在检查软件版本，请稍后...", false);
        JSONObject jsonObject = getPublicJson();
        try {
            jsonObject.put("version", version);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OkGo.<BaseDTO<UpdateDTO>>post(MyConstants.HTTP_ADDRESS + "upgrade")
                .tag(mActivity)
                .upJson(jsonObject.toString())
                .execute(new JsonCallBack<BaseDTO<UpdateDTO>>() {
                    @Override
                    public void onSuccess(Response<BaseDTO<UpdateDTO>> response) {
                        BaseDTO<UpdateDTO> dto = response.body();
                        if (dto.getCode() == 0) {
                            UpdateDTO updateDTO = dto.getData();
                            if (updateDTO == null || updateDTO.getApkUrl().isEmpty()) {
                                addLog("已经是最新版本了！", false);
                                isOnUpdateAPK = false;
                                getDeployParam();
                            } else {
                                addLog("检查到新版本，正在下载更新...", false);
//                                TODO 暂时跳过版本更新
                                upLoadAPK(updateDTO.getApkUrl());
//                                getDeployParam();
                            }
                        } else {
                            isOnUpdateAPK = false;
                            addLog(dto.getMsg(), true);
                        }
                    }
                });
    }

    /**
     * apk下载更新
     *
     * @param url
     */
    private void upLoadAPK(String url) {
        OkGo.<File>get(url)
                .tag(mActivity)
                .execute(new FileCallback() {
                    @Override
                    public void onSuccess(Response<File> response) {
                        changeTitle("下载成功，正在安装...");
                        addLog("安装包下载成功！正在安装...", false);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        File file = response.body();
                        boolean isSuccess = moveFile(file.getPath(), apkDir.getPath(), "faceGATE.apk");
//                        DeviceManager deviceManager = new DeviceManager(mActivity);
//                        deviceManager.installPackage(file.getPath(), getPackageName());
                        if (isSuccess) {
                            file = new File(apkDir + "/faceGATE.apk");
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Uri apkUri = FileProvider.getUriForFile(mActivity, getPackageName() + ".fileprovider", file);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        } else {
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            Uri uri = Uri.fromFile(file);
                            intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        }
                        startActivity(intent);
                    }

                    @Override
                    public void onStart(Request<File, ? extends Request> request) {
                        super.onStart(request);
                        changeTitle("正在下载安装包...");
                    }

                    @Override
                    public void onError(Response<File> response) {
                        super.onError(response);
                        addLog("安装包下载失败，请检查网络连接！", true);
                    }

                    @Override
                    public void uploadProgress(Progress progress) {
                        super.uploadProgress(progress);
                        //下载进度
                        long totalSize = progress.totalSize;
                        long currentSize = progress.currentSize;
                        LogUtils.d(progress.currentSize);
                        float f = currentSize / totalSize;
                        float rate = f * 100f;
                        changeTitle("正在下载，进度：" + rate + "%");
                    }
                });
    }

    /**
     * 初始化配置参数
     */
    private void getDeployParam() {
        if(isOnUpdateParam){
            return;
        }else{
            isOnUpdateParam = true;
        }
        addLog("正在获取配置参数,请稍后...", false);
        OkGo.<BaseDTO<ParamDTO>>post(MyConstants.HTTP_ADDRESS + "getParam")
                .tag(mActivity)
                .upJson(getPublicJson().toString())
                .execute(new JsonCallBack<BaseDTO<ParamDTO>>() {
                    @Override
                    public void onSuccess(Response<BaseDTO<ParamDTO>> response) {
                        BaseDTO<ParamDTO> dto = response.body();
                        if (dto.getCode() == 0) {
                            ParamDTO paramDTO = dto.getData();
                            SPU.setProjectName(mActivity, paramDTO.getName());
                            if (SPU.isFirst(mActivity)) {
                                //首次进入，必须更新参数
                                upDateParam(paramDTO);
                            } else {
                                if (paramDTO.getUpdFlag() == 0) {
                                    //不需要更新
                                    addLog("无参数更新！", false);
                                    isOnUpdateParam = false;
                                    getPersonFace();
                                } else {
                                    //需要更新
                                    upDateParam(paramDTO);
                                }
                            }
                        } else {
                            isOnUpdateParam = false;
                            addLog(dto.getMsg(), true);
                        }
                    }

                    @Override
                    public void onError(Response<BaseDTO<ParamDTO>> response) {
                        super.onError(response);
                        addLog("获取配置参数失败！", true);
                    }
                });
    }

    /**
     * 参数更新
     *
     * @param dto
     */
    private void upDateParam(ParamDTO dto) {
        addLog("正在更新参数，请稍后...", false);
        SPU.setParam(mActivity, dto);
        deleteFile(voiceLibraryDir);//删除原有的语音文件
        addLog("语音包下载中，请稍后...", false);
        List<VoiceDTO> voiceDTOList = dto.getVoiceList();
        if (voiceDTOList.size() == 0) {
            changeTitle("语音包列表为空！");
            addLog("语音包列表为空！", true);
            paramNotify();
            return;
        }
        voiceDownLoadCount = voiceDTOList.size();
        changeTitle("语音包下载中，进度：0/" + voiceDownLoadCount);
        voiceDownLoadNum = 0;
        for (VoiceDTO voiceDTO : voiceDTOList) {
            downLoadVoiceList(voiceDTO);
        }
    }

    /**
     * 下载语音文件
     *
     * @param dto
     */
    private void downLoadVoiceList(final VoiceDTO dto) {
        OkGo.<File>get(dto.getVoiceUrl())
                .tag(mActivity)
                .execute(new FileCallback() {
                    @Override
                    public void onSuccess(Response<File> response) {
                        File file = response.body();
                        moveFile(file.getPath(), voiceLibraryDir.getPath(), dto.getCatCd() + ".mp3");
                        checkDownLoad();
                    }
                });
    }


    /**
     * 同步查看语音包是否下载结束
     */
    private synchronized void checkDownLoad() {
        voiceDownLoadNum++;
        changeTitle("语音包下载中，进度：0/" + voiceDownLoadCount);
        if (voiceDownLoadNum == voiceDownLoadCount) {
            //下载结束
            changeTitle("语音包下载完成！");
            addLog("语音包更新完成！", false);
            paramNotify();
        }
    }

    /**
     * 参数更新通知
     */
    private void paramNotify() {
        OkGo.<BaseDTO<String>>post(MyConstants.HTTP_ADDRESS + "paramNotify")
                .tag(mActivity)
                .upJson(getPublicJson().toString())
                .execute(new JsonCallBack<BaseDTO<String>>() {
                    @Override
                    public void onSuccess(Response<BaseDTO<String>> response) {
                        addLog("配置参数更新完毕！", false);
                        BaseDTO dto = response.body();
                        if (dto.getCode() != 0) {
                            addLog(dto.getMsg(), true);
                        }
                        isOnUpdateParam = false;
                        getPersonFace();
                    }
                });
    }

    /**
     * 获取人脸参数
     */
    private void getPersonFace() {
        if(isOnUpdatePerson){
            return;
        }else{
            isOnUpdatePerson = true;
        }
        changeTitle("正在更新人脸库...");
        addLog("正在更新人脸库...", false);
        notifyList = new JSONArray();
        if (SPU.isFirst(mActivity)) {
            //首次进入，直接更新全部
            OkGo.<BaseDTO<AutoAllDTO>>post(MyConstants.HTTP_ADDRESS + "getAuthList")
                    .tag(mActivity)
                    .upJson(getPublicJson().toString())
                    .execute(new JsonCallBack<BaseDTO<AutoAllDTO>>() {
                        @Override
                        public void onSuccess(Response<BaseDTO<AutoAllDTO>> response) {
                            BaseDTO<AutoAllDTO> dto = response.body();
                            if (dto.getCode() == 0) {
                                AutoAllDTO autoAllDTO = dto.getData();
                                addList.addAll(autoAllDTO.getList());
                                checkAuthList();
                            } else {
                                isOnUpdatePerson = false;
                                addLog(dto.getMsg(), true);
                            }
                        }
                    });
        } else {
            //不是首次，检查更新/删除列表
            checkAuthList();
        }
    }

    /**
     * 检查人员更新/删除列表
     */
    private void checkAuthList() {
        OkGo.<BaseDTO<AutoDTO>>post(MyConstants.HTTP_ADDRESS + "getUnAuthList")
                .tag(mActivity)
                .upJson(getPublicJson().toString())
                .execute(new JsonCallBack<BaseDTO<AutoDTO>>() {
                    @Override
                    public void onSuccess(Response<BaseDTO<AutoDTO>> response) {
                        BaseDTO<AutoDTO> dto = response.body();
                        if (dto.getCode() == 0) {
                            AutoDTO autoDTO = dto.getData();
                            addList.addAll(autoDTO.getAddList());
                            removeList.addAll(autoDTO.getRemoveIdList());
                            addPerson();
                        } else {
                            isOnUpdatePerson = false;
                            addLog(dto.getMsg(), true);
                        }
                    }
                });
    }

    /**
     * 添加人员
     */
    private void addPerson() {
        if (addList == null || addList.isEmpty()) {
            deletePerson();
            return;
        }
        imageDownLoadCount = addList.size();
        checkImageDownLoad();
    }

    /**
     * 下载图片
     */
    private void downLoadImage(final PersonDTO dto) {
        OkGo.<Bitmap>get(dto.getImgUrl())
                .tag(mActivity)
                .execute(new BitmapCallback() {
                    @Override
                    public void onSuccess(Response<Bitmap> response) {
                        Bitmap bitmap = response.body();
                        DetectResult detectResult = faceUtils.getBoxFixed(bitmap);
                        if (detectResult.getFlag() == 0) {
                            //有人脸标识
//                            Bitmap crop = faceUtils.cropBitmap(detectResult.getBox(), bitmap);
//                            float[] floats = faceUtils.getFeature(detectResult.getBox(), crop);
                            // 人脸识别特征提取
                            float[] floats = faceUtils.getFeature(detectResult.getBox(), bitmap);
                            // 将bitmap进行回收
                            bitmap.recycle();
//                            crop.recycle();
                            String featureString = DataUtils.floats2String(floats);
                            GreenDaoManager.addPersonToDB(mActivity, new Person(dto.getId(), dto.getName()
                                    , dto.getNumber(), featureString));

                            addNotify(dto.getId(), 1, 0);
                        } else {
                            addNotify(dto.getId(), 1, 3);//TODO 暂时为没有截取到人脸
                        }
                    }

                    @Override
                    public void onError(Response<Bitmap> response) {
                        super.onError(response);
                        addNotify(dto.getId(), 1, 6);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        imageDownLoadNum++;
                        checkImageDownLoad();
                    }
                });
    }


    /**
     * 检查图片下载是否完成
     */
    private synchronized void checkImageDownLoad() {
        if (imageDownLoadNum < imageDownLoadCount) {
            changeTitle("正在下载人脸库：" + imageDownLoadNum + "/" + imageDownLoadCount);
            PersonDTO personDTO = addList.get(imageDownLoadNum);
            if (personDTO.getImgUrl() == null || personDTO.getImgUrl().isEmpty()) {
                addNotify(personDTO.getId(), 1, 3);//图片无人脸
                imageDownLoadNum++;
                checkImageDownLoad();
            } else {
                downLoadImage(personDTO);
            }
        } else {
            changeTitle("人脸库下载完成！");
            addLog("人脸库下载完成！", false);
            deletePerson();
        }
    }

    /**
     * 删除人员
     */
    private void deletePerson() {
        if (removeList == null || removeList.isEmpty()) {
            authNotify();
            return;
        }
        changeTitle("正在同步人员信息...");
        addLog("正在删除无关联信息...", false);
        for (Long id : removeList) {
            //删除人员信息与图片,以及人脸库
            GreenDaoManager.deletePersonById(mActivity, id);
            addNotify(id, 0, 0);
            faceUtils.deleteFeatureToMap(id + "");
//            deleteFile(new File(faceLibraryDir.getPath()+"/"+id+".jpg"));
        }
        changeTitle("人员信息同步完成！");
        addLog("已删除信息：" + removeList.size() + "条！", false);
        authNotify();
    }

    /**
     * 添加授权记录
     *
     * @param id   人员ID
     * @param way  授权方式:0-解除授权 1-增加授权
     * @param code 授权结果:0.授权成功
     *             1.图片尺寸过小
     *             2.图片尺寸过大
     *             3.没有检测到人脸
     *             4.图片读取失败
     *             5.图片格式错误
     *             6.图片下载失败
     */
    private void addNotify(long id, int way, int code) {
        JSONObject object = new JSONObject();
        try {
            object.put("id", id);
            object.put("way", way);
            object.put("code", code);
            notifyList.put(object);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 人员授权结果通知接口
     */
    private void authNotify() {
        if (notifyList == null || notifyList.length() == 0) {
            //无人员授权操作，直接结束
            end();
            return;
        }
        JSONObject object = getPublicJson();
        try {
            object.put("notifyList", notifyList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OkGo.<BaseDTO<String>>post(MyConstants.HTTP_ADDRESS + "authNotify")
                .tag(mActivity)
                .upJson(object.toString())
                .execute(new JsonCallBack<BaseDTO<String>>() {
                    @Override
                    public void onSuccess(Response<BaseDTO<String>> response) {
                        BaseDTO dto = response.body();
                        if (dto.getCode() == 0) {
                            end();
                            notifyList = null;
                        } else {
                            isOnUpdatePerson = false;
                            addLog(dto.getMsg(), true);
                        }
                    }
                });
    }

    /**
     * 最后操作  同步人脸库  跳转页面
     */
    private void end() {
        //上报成功,同步人脸库
        addLog("正在同步人脸库，请稍后...", false);
        changeTitle("正在同步人脸库，请稍后...");
        List<Person> personList = GreenDaoManager.getPersonList(mActivity);
        for (int i = 0; i < personList.size(); i++) {
            changeTitle("正在同步人脸库，进度:" + i + "/" + personList.size());
            Person person = personList.get(i);
            if (person.getFeature() == null || person.getFeature().isEmpty()) {
                continue;
            }
            float[] feature = DataUtils.string2Floats(person.getFeature());
            faceUtils.addFeatureToMap(person.getId() + "", feature);
        }
        addLog("完成！", false);
        changeTitle("操作完成！");
        isOnUpdatePerson = false;
        over();
    }

}
