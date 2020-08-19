package com.wudi.facegate.module;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取所有已授权人员返回
 * Created by wudi on 2020/5/18.
 */
public class AutoAllDTO {
    private List<PersonDTO> list;

    public List<PersonDTO> getList() {
        if (list == null) {
            return new ArrayList<>();
        }
        return list;
    }

    public void setList(List<PersonDTO> list) {
        this.list = list;
    }
}
