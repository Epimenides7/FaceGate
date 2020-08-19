package com.wudi.facegate.module;

/**
 * 版本更新DTO
 * Created by wudi on 2020/5/17.
 */
public class UpdateDTO {
    private String apkUrl;//更细apk的URL

    public String getApkUrl() {
        return apkUrl == null ? "" : apkUrl;
    }

    public void setApkUrl(String apkUrl) {
        this.apkUrl = apkUrl;
    }
}
