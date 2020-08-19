package com.wudi.facegate.http;

import com.google.gson.Gson;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.request.base.Request;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 自定义callback  处理http返回
 * 兼容写法 不传type和clazz的时候  默认解析当前类父类上的泛型真实类型 这种不适用多个DTO嵌套
 * 如有多个DTO嵌套 请传入type或clazz
 * @param <T>
 */
public abstract class JsonCallBack<T> extends AbsCallback<T> {
    private Type type;
    private Class<T> clazz;

    public JsonCallBack() {
    }

    public JsonCallBack(Type type, Class<T> clazz) {
        this.type = type;
        this.clazz = clazz;
    }

    @Override
    public void onStart(Request<T, ? extends Request> request) {
        //发起请求前的回调  在这里处理添加公共参数
        super.onStart(request);
    }

    @Override
    public T convertResponse(Response response) throws Throwable {
        if(response.code() != 200){
            throw new IllegalStateException("网络请求错误:"+response.code());
        }
        ResponseBody body = response.body();
        if(body == null){
            return null;
        }
        T data = null;
        Gson gson = new Gson();
        if(type != null){
            data = gson.fromJson(body.charStream(),type);
        }else if(clazz != null){
            data = gson.fromJson(body.charStream(),clazz);
        }else{
            Type genType = getClass().getGenericSuperclass();
            Type type = ((ParameterizedType)genType).getActualTypeArguments()[0];
            data = gson.fromJson(body.charStream(),type);
        }
        return data;
    }

}
