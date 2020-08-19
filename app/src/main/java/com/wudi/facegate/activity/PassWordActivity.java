package com.wudi.facegate.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.wudi.facegate.R;
import com.wudi.facegate.base.BaseActivity;
import com.wudi.facegate.utils.SPU;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 密码输入页
 * Created by wudi on 2020/5/21.
 */
public class PassWordActivity extends BaseActivity {
    @BindView(R.id.et_pwd)
    EditText etPwd;
    @BindView(R.id.tv_title)
    TextView tvTitle;

    @Override
    protected int getLayout() {
        return R.layout.activity_password;
    }

    @Override
    protected void init() {

    }

    @OnClick(R.id.bt)
    public void onViewClicked() {
        String pwd = etPwd.getText().toString().trim();
        if(pwd.isEmpty()){
            tvTitle.setText("密码不能为空！");
            return;
        }
        if(!SPU.getPassword(mActivity).equals(pwd)){
            tvTitle.setText("密码错误！");
            return;
        }
        startActivity(new Intent(mActivity,SettingActivity.class));
        finish();
    }
}
