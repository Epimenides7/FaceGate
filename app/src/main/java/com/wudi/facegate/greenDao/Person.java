package com.wudi.facegate.greenDao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

import java.io.Serializable;

/**
 * 人员信息存储实体
 * Created by wudi on 2020/5/18.
 */
@Entity
public class Person implements Serializable{
    private static final long serialVersionUID = -4621715087305128172L;

    @Id
    Long id;//人员id
    String name;//人员名称
    String number;//人员卡号
    String feature;//人脸特征点
    @Generated(hash = 1608842598)
    public Person(Long id, String name, String number, String feature) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.feature = feature;
    }
    @Generated(hash = 1024547259)
    public Person() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getNumber() {
        return this.number;
    }
    public void setNumber(String number) {
        this.number = number;
    }
    public String getFeature() {
        return this.feature;
    }
    public void setFeature(String feature) {
        this.feature = feature;
    }
}
