package com.wudi.facegate.utils;

import com.apkfuns.logutils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android_serialport_api.SerialPort;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * 串口工具类
 * 封装了常用的打开，连接，关闭等操作
 */
public class SerialPortUtil {
    private String address;//串口地址
    private int baudRate;//波特率
    private SerialPort mSerialPort;//串口操作类
    private InputStream mInputStream;//输入流  读
    private OutputStream mOutputStream;//输出流  写
    private Flowable flowable;//被观察者
    private Consumer<byte[]> reader;//观察者
    private Consumer<Throwable> onError;//异常处理
    private Disposable disposable;//解除订阅工具
    private ExecutorService sendExecutor;//指令发送线程池

    public SerialPortUtil(String address, int baudRate, Consumer<byte[]> reader,
                          Consumer<Throwable> onError) {
        this.address = address;
        this.baudRate = baudRate;
        this.reader = reader;
        this.onError = onError;
        sendExecutor = Executors.newSingleThreadExecutor();//创建指令发送线程池

        flowable = Flowable.interval(200,200, TimeUnit.MILLISECONDS) //定时200毫秒执行一次
                .map(new Function<Long,byte[]>() {        //flatMap变换返回类型
                    @Override
                    public byte[] apply(Long aLong) {
                        if (mInputStream != null) {
                            byte[] buffer = new byte[1024];
                            try {
                                //注意：读取流方法如果读取不到数据会阻塞，
                                // 必须先判断流中的数据长度大于0再读
                                if(mInputStream.available() > 0){
                                    Thread.sleep(200);
                                    int size = mInputStream.read(buffer);//读取数据并获得数据长度
                                    if (size > 0) {
                                        //发送给观察者
                                        return DataUtils.cutBytes(buffer, 0, size);
                                    }
                                }
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        return new byte[0];
                    }
                })
                .subscribeOn(Schedulers.io()) //订阅工作线程 io线程
                .observeOn(AndroidSchedulers.mainThread()); //观察者处理线程
    }

    /**
     * 打开串口，创建通道
     */
    public boolean openSerialPort() {
        try {
            mSerialPort = new SerialPort(new File((address)), baudRate, 0);//创建串口对象
            mInputStream = mSerialPort.getInputStream();//获取读通道
            mOutputStream = mSerialPort.getOutputStream();//获取写通道
            if (reader != null) {
                disposable = flowable.subscribe(reader,onError);//建立订阅
                return true;
            } else {
                LogUtils.d("串口初始化失败，观察者不能为空！");
                return false;
            } //开启接收数据线程
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.d("串口初始化失败，请检查串口地址是否正确！");
            return false;
        }catch (SecurityException e){
            e.printStackTrace();
            LogUtils.d("串口初始化失败，请检查串口地址是否正确！");
            return false;
        }
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
            if (mOutputStream != null) {
                mOutputStream.close();
            }
            if (mSerialPort != null) {
                mSerialPort.close();
            }
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();//取消订阅
            }
            if(sendExecutor != null){
                sendExecutor.shutdown();//线程池停止
                sendExecutor = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 数据发送
     * 通过串口发送数据到下位机
     */
    public void sendSerialPort(final String dataHex) {
        sendExecutor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] bytes = DataUtils.hexToByteArray(dataHex.trim());
                try {
                    mOutputStream.write(bytes);
                    mOutputStream.flush();
                    Thread.sleep(100);//指令之间间隔100ms，确保不会粘包
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
