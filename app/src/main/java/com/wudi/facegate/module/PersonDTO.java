package com.wudi.facegate.module;

/**
 * 人员信息
 * Created by wudi on 2020/5/11.
 */
public class PersonDTO {
    private long id;//ID
    private String name;//名字
    private String imgUrl;//照片地址
    private String number;//卡号

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgUrl() {
        return imgUrl == null ? "" : imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getNumber() {
        return number == null ? "" : number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

}
