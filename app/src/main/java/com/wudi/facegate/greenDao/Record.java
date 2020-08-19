package com.wudi.facegate.greenDao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * 刷脸记录
 * Created by wudi on 2020/5/21.
 */
@Entity
public class Record {
    @Id
    Long timestamp;//时间戳
    long personId;//人员id
    int code;//0-通过；1-陌生人；2-未带安全帽；3-画面模糊；4-距离过远；5-活体检测未通过
    int way;//0-刷脸 1-刷卡 2-刷脸+刷卡
    String number;//刷卡号,way=1或2时，该字段不为空
    String imgPath;//抓取图片本地保存路径

    @Generated(hash = 2016581586)
    public Record(Long timestamp, long personId, int code, int way, String number,
            String imgPath) {
        this.timestamp = timestamp;
        this.personId = personId;
        this.code = code;
        this.way = way;
        this.number = number;
        this.imgPath = imgPath;
    }

    @Generated(hash = 477726293)
    public Record() {
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public long getPersonId() {
        return personId;
    }

    public void setPersonId(long personId) {
        this.personId = personId;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getWay() {
        return way;
    }

    public void setWay(int way) {
        this.way = way;
    }

    public String getNumber() {
        return number == null ? "" : number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getImgPath() {
        return imgPath == null ? "" : imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }
}
