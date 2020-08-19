package com.wudi.facegate.module;

import android.app.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * 人员授权列表
 * Created by wudi on 2020/5/11.
 */
public class AutoDTO {
    private List<PersonDTO> addList;
    private List<Long> removeIdList;

    public List<PersonDTO> getAddList() {
        if (addList == null) {
            return new ArrayList<>();
        }
        return addList;
    }

    public void setAddList(List<PersonDTO> addList) {
        this.addList = addList;
    }

    public List<Long> getRemoveIdList() {
        if (removeIdList == null) {
            return new ArrayList<>();
        }
        return removeIdList;
    }

    public void setRemoveIdList(List<Long> removeIdList) {
        this.removeIdList = removeIdList;
    }
}
