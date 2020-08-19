package com.wudi.facegate.module;

/**
 * 屏幕显示
 * Created by wudi on 2020/5/11.
 */
public class ScreenDTO {
    private String catCd;//屏幕显示场景 0-识别成功;1-陌生人;2-未带安全帽;3-画面模糊;4-距离过远;5-活体检测未通过
    private String content;//显示文本

    public String getCatCd() {
        return catCd == null ? "" : catCd;
    }

    public void setCatCd(String catCd) {
        this.catCd = catCd;
    }

    public String getContent() {
        return content == null ? "" : content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
