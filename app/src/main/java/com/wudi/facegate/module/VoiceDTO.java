package com.wudi.facegate.module;

/**
 * 音频返回类
 * Created by wudi on 2020/5/11.
 */
public class VoiceDTO {
    private String catCd;//播放场景 0-识别成功;1-陌生人;2-未带安全帽;3-画面模糊;4-距离过远;5-活体检测未通过
    private String voiceUrl;//语音地址

    public String getCatCd() {
        return catCd == null ? "" : catCd;
    }

    public void setCatCd(String catCd) {
        this.catCd = catCd;
    }

    public String getVoiceUrl() {
        return voiceUrl == null ? "" : voiceUrl;
    }

    public void setVoiceUrl(String voiceUrl) {
        this.voiceUrl = voiceUrl;
    }
}
