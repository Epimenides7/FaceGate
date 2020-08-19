package com.wudi.facegate.module;

/**
 * Created by wudi on 2020/5/20.
 */
public class AliKey {
    private String securityToken;//STS 令牌,令牌目前有效时间1800S
    private String accessKeySecret;//accessKeySecret
    private String accessKeyId;//accessKeyId
    private String expiration;//令牌失效时间点,北京时间，格式:yyyy-MM-dd HH:mm:ss

    public String getSecurityToken() {
        return securityToken == null ? "" : securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    public String getAccessKeySecret() {
        return accessKeySecret == null ? "" : accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getAccessKeyId() {
        return accessKeyId == null ? "" : accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getExpiration() {
        return expiration == null ? "" : expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }
}
