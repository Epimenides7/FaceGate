package com.wudi.facegate.module;

/**
 * 初始化页显示log的DTO
 * Created by wudi on 2020/5/13.
 */
public class ShowLogDTO {
    private String log;//log文本
    private boolean isError;//是否报错

    public ShowLogDTO(String log, boolean isError) {
        this.log = log;
        this.isError = isError;
    }

    public String getLog() {
        return log == null ? "" : log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }
}
