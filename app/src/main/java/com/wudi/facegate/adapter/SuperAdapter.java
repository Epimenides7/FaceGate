package com.wudi.facegate.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView 通用适配器
 * Created by Administrator on 2017/8/9.
 */

public abstract class SuperAdapter extends RecyclerView.Adapter<SuperAdapter.BaseViewHolder> {
    private Context mContext;
    private LayoutInflater mInflater;
    private List mData;  //数据源
    private int mLayoutId;  //布局id
    private File file;
    public boolean[] flags;//列表标记

    public SuperAdapter(Context context, List data, int layoutId) {
        mContext = context;
        mData = data;
        mLayoutId = layoutId;
        mInflater = LayoutInflater.from(context);
        flags = new boolean[data.size()];
    }

    public SuperAdapter(Context context, Object[] data, int layoutId) {
        mContext = context;
        if (data != null) {
            mData = new ArrayList();
            for (int i = 0; i < data.length; i++) {
                mData.add(data[i]);
            }
        }
        mLayoutId = layoutId;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BaseViewHolder(mInflater.inflate(mLayoutId, parent, false));
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        setWidget(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);

    }

    @Override
    public int getItemCount() {
        return null == mData ? 0 : mData.size();
    }

    protected abstract void setWidget(BaseViewHolder holder, int position);

    public class BaseViewHolder extends RecyclerView.ViewHolder {
        private View mView;
        private SparseArray<View> mViews;   //视图集合

        public BaseViewHolder(View itemView) {
            super(itemView);
            mViews = new SparseArray<>();
            mView = itemView;
        }

        /*根据控件id获取视图*/
        private View getView(int widgetId) {
            View view = mViews.get(widgetId);
            if (view == null) {
                view = mView.findViewById(widgetId);
                mViews.put(widgetId, view);
            }
            return view;
        }

        /*设置文字控件*/
        public BaseViewHolder setText(int widgetId, String str) {
            ((TextView) getView(widgetId)).setText(str);
            return this;
        }

        /*设置本地图形控件*/
        public BaseViewHolder loadImage(int widgetId, int resourceId) {
            ((ImageView) getView(widgetId)).setImageResource(resourceId);
            return this;
        }

        /*设置本地图形控件*/
//        public BaseViewHolder loadImage(int widgetId, File file) {
//            if (file == null) {
//                return this;
//            }
//            MyImageLoad.loadImg(mContext,((ImageView) getView(widgetId)),file);
//            return this;
//        }

        /*加载本地gif图*/
//        public BaseViewHolder loadGif(int widgetId,int resourceId) {
//            ImageView view = (ImageView) getView(widgetId);
//            MyImageLoad.loadImg(mContext,view,resourceId);
//            return this;
//        }

        /*设置控件点击监听*/
        public BaseViewHolder setClickListner(int widgetId, View.OnClickListener listner) {
            getView(widgetId).setOnClickListener(listner);
            return this;
        }

        /*设置控件长按监听*/
        public BaseViewHolder setLongClickListner(int widgetId, View.OnLongClickListener listener) {
            getView(widgetId).setOnLongClickListener(listener);
            return this;
        }

        /*设置Enable属性*/
        public BaseViewHolder setEnable(int widdgetId, boolean enable) {
            getView(widdgetId).setEnabled(enable);
            return this;
        }

        /*设置空间可见性*/
        public BaseViewHolder setVisible(int widgetId, int visible) {
            getView(widgetId).setVisibility(visible);
            return this;
        }

        /*设置背景*/
        public BaseViewHolder setBackGroundResource(int widgetId, int resourceId) {
            getView(widgetId).setBackgroundResource(resourceId);
            return this;
        }

        /*设置字体颜色*/
        public BaseViewHolder setTextColor(int widgetId, int color) {
            ((TextView) getView(widgetId)).setTextColor(color);
            return this;
        }

        /*添加单选按钮组监听*/
        public BaseViewHolder setOnCheckChangeListener(int widgetId, RadioGroup.OnCheckedChangeListener listener) {
            RadioGroup rg = (RadioGroup) getView(widgetId);
            rg.setOnCheckedChangeListener(listener);
            return this;
        }

        /*添加单选按钮监听*/
        public BaseViewHolder setRadioButtonCheckChangeListener(int widgetId, CompoundButton.OnCheckedChangeListener listener) {
            RadioButton rg = (RadioButton) getView(widgetId);
            rg.setOnCheckedChangeListener(listener);
            return this;
        }

        /*添加多选按钮监听*/
        public BaseViewHolder setOnCheckChangeListener(int widgetId, CompoundButton.OnCheckedChangeListener listener) {
            CheckBox cb = (CheckBox) getView(widgetId);
            cb.setOnCheckedChangeListener(listener);
            return this;
        }


        /*添加文字更改监听*/
        public BaseViewHolder setOnTextWatchListener(int widgetId, TextWatcher textWatcher) {
            EditText editText = (EditText) getView(widgetId);
            editText.addTextChangedListener(textWatcher);
            return this;
        }

        /*设置是否可选*/
        public BaseViewHolder setEnabled(int widgetId, boolean enabled) {
            View view = getView(widgetId);
            view.setEnabled(enabled);
            return this;
        }

        /*设置加载网络图*/
//        public BaseViewHolder loadUrlImage(int widgetId, String url) {
//            ImageView view = (ImageView) getView(widgetId);
//            MyImageLoad.loadImg(mContext,view,ASR.IMG_HEAD + url);
//            return this;
//        }

        /*绝对路径加载图片*/
//        public BaseViewHolder loadUrlImageWithoutHead(int widgetId, String url) {
//            ImageView view = (ImageView) getView(widgetId);
//            MyImageLoad.loadImg(mContext,view,url);
//            return this;
//        }

        /*绑定规格列表适配器*/
//        public TagFlowLayout bindTagAdapter(int widgetId, TagAdapter tagAdapter) {
//            TagFlowLayout tfl = (TagFlowLayout) getView(widgetId);
//            tfl.setAdapter(tagAdapter);
//            return tfl;
//        }

        /*设置选中*/
        public BaseViewHolder setCheck(int widgetId, boolean isCheck) {
            CheckBox checkBox = (CheckBox) getView(widgetId);
            checkBox.setChecked(isCheck);
            return this;
        }

        /*设置单选按钮选中*/
        public BaseViewHolder setCheckRadio(int widgetId, boolean isCheck) {
            RadioButton radioButton = (RadioButton) getView(widgetId);
            radioButton.setChecked(isCheck);
            return this;
        }

        /*设置CheckBox控件文字*/
        public BaseViewHolder setCheckBoxText(int widgetId, String str) {
            ((CheckBox) getView(widgetId)).setText(str);
            return this;
        }

        /*设置RadioButton控件文字*/
        public BaseViewHolder setRadioButtonText(int widgetId, String str) {
            ((RadioButton) getView(widgetId)).setText(str);
            return this;
        }

        /*列表嵌套*/
//        public BaseViewHolder setListAdapter(int widgetId, BaseAdapter baseAdapter, LinearLayoutManager layoutManager) {
//            RecyclerView rv = (RecyclerView) getView(widgetId);
//            rv.setNestedScrollingEnabled(false);
//            rv.setHasFixedSize(true);
//            rv.setAdapter(baseAdapter);
//            rv.setLayoutManager(layoutManager);
//            return this;
//        }

        /**
         * 为流式布局控件设置适配器
         * @param widgetId
         * @param adapter
         * @return
         */
//        public TagFlowLayout setTagAdapter(int widgetId,TagAdapter adapter){
//            TagFlowLayout tfl = (TagFlowLayout) getView(widgetId);
//            tfl.setAdapter(adapter);
//            return tfl;
//        }

        /**
         * 设置选中项
         * @return
         */
//        public BaseViewHolder setMaxSelectCount(TagFlowLayout tfl,int position){
//            tfl.setMaxSelectCount(position);
//            return this;
//        }

    }
}
