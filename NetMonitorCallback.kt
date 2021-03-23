package com.xianghuanji.http.listener

import com.xianghuanji.http.bean.MonitorResult
import okhttp3.Call

interface NetMonitorCallback {
    fun onSuccess(call: Call, monitorResult: MonitorResult)

    fun onError(call: Call, monitorResult: MonitorResult, ioe: Exception)
}