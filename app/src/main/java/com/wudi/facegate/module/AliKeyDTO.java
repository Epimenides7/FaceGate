package com.wudi.facegate.module;

/**
 * 阿里云所需要密钥
 * Created by wudi on 2020/5/20.
 */
public class AliKeyDTO {
    private AliKey credentials;

    public AliKey getCredentials() {
        if (credentials == null) {
            return new AliKey();
        }
        return credentials;
    }

    public void setCredentials(AliKey credentials) {
        this.credentials = credentials;
    }
}
