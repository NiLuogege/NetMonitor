package com.xianghuanji.luxury;

import com.xianghuanji.http.bean.MonitorResult;
import com.xianghuanji.http.listener.NetMonitor;
import com.xianghuanji.http.listener.NetMonitorCallback;

import org.jetbrains.annotations.NotNull;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class Demo {
    public static void main(String[] args) {


        //伪代码

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.eventListenerFactory(call -> new NetMonitor(new NetMonitorCallback() {
            @Override
            public void onSuccess(@NotNull Call call, @NotNull MonitorResult monitorResult) {
                //网络请求成功后回调
            }

            @Override
            public void onError(@NotNull Call call, @NotNull MonitorResult monitorResult, @NotNull Exception ioe) {
                //网络请求失败回调
            }
        }, true));
    }
}
