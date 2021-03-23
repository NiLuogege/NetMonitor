package com.xianghuanji.http.listener

import android.util.Log
import com.xianghuanji.http.HttpIniter
import com.xianghuanji.http.bean.MonitorResult
import com.xianghuanji.util.utils.system.NetworkUtils
import okhttp3.*
import okhttp3.internal.Version
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

class NetMonitor(
    private val netMonitorCallback: NetMonitorCallback,
    private val openLog: Boolean = false
) :
    EventListener() {


    private val mr = MonitorResult()

    var callStartTime = 0L
    var dnsStartTime = 0L
    var dnsEndTime = 0L
    var connectStartTime = 0L
    var secureConnectStartTime = 0L
    var secureConnectEndTime = 0L
    var connectEndTime = 0L
    var requestHeadersStartTime = 0L
    var requestHeadersEndTime = 0L
    var requestBodyStartTime = 0L
    var requestBodyEndTime = 0L
    var responseHeadersStartTime = 0L
    var responseHeadersEndTime = 0L
    var responseBodyStartTime = 0L
    var responseBodyEndTime = 0L
    var callEndTime = 0L

    /**
     * Invoked as soon as a call is enqueued or executed by a client. In case of thread or stream
     * limits, this call may be executed well before processing the request is able to begin.
     *
     *
     * This will be invoked only once for a single [Call]. Retries of different routes
     * or redirects will be handled within the boundaries of a single callStart and [ ][.callEnd]/[.callFailed] pair.
     */
    override fun callStart(call: Call) {
        super.callStart(call)
        callStartTime = System.currentTimeMillis()

        d("callStart : ${call.request().url()}")

        val request = call.request()
        mr.ua = Version.userAgent()
        mr.url = request.url().toString()
        mr.requestMethod = request.method()
        mr.isHttps = request.isHttps
        mr.netType = NetworkUtils.getNetWorkTypeSimpleName(HttpIniter.context)
    }

    /**
     * Invoked just prior to a DNS lookup. See [Dns.lookup].
     *
     *
     * This can be invoked more than 1 time for a single [Call]. For example, if the response
     * to the [Call.request] is a redirect to a different host.
     *
     *
     * If the [Call] is able to reuse an existing pooled connection, this method will not be
     * invoked. See [ConnectionPool].
     */
    override fun dnsStart(call: Call, domainName: String) {
        super.dnsStart(call, domainName)
        dnsStartTime = System.currentTimeMillis()
        d("dnsStart :  domainName=$domainName")
    }

    /**
     * Invoked immediately after a DNS lookup.
     *
     *
     * This method is invoked after [.dnsStart].
     */
    override fun dnsEnd(call: Call, domainName: String, inetAddressList: MutableList<InetAddress>) {
        super.dnsEnd(call, domainName, inetAddressList)
        dnsEndTime = System.currentTimeMillis()
        d("dnsEnd :  domainName=$domainName  inetAddressList=$inetAddressList")

        mr.dnsResult=inetAddressList.toString()
    }

    /**
     * Invoked just prior to initiating a socket connection.
     *
     *
     * This method will be invoked if no existing connection in the [ConnectionPool] can be
     * reused.
     *
     *
     * This can be invoked more than 1 time for a single [Call]. For example, if the response
     * to the [Call.request] is a redirect to a different address, or a connection is retried.
     */
    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        super.connectStart(call, inetSocketAddress, proxy)
        connectStartTime = System.currentTimeMillis()
        d("connectStart :  inetSocketAddress=$inetSocketAddress  proxy=$proxy")

        mr.ip = inetSocketAddress.address.hostAddress
        mr.port = inetSocketAddress.port.toString()
        mr.isProxy = proxy.type() != Proxy.Type.DIRECT
    }

    /**
     * Invoked just prior to initiating a TLS connection.
     *
     *
     * This method is invoked if the following conditions are met:
     *
     *  * The [Call.request] requires TLS.
     *  * No existing connection from the [ConnectionPool] can be reused.
     *
     *
     *
     * This can be invoked more than 1 time for a single [Call]. For example, if the response
     * to the [Call.request] is a redirect to a different address, or a connection is retried.
     */
    override fun secureConnectStart(call: Call) {
        super.secureConnectStart(call)
        secureConnectStartTime = System.currentTimeMillis()
        d("secureConnectStart")
    }

    /**
     * Invoked immediately after a TLS connection was attempted.
     *
     *
     * This method is invoked after [.secureConnectStart].
     */
    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        super.secureConnectEnd(call, handshake)
        secureConnectEndTime = System.currentTimeMillis()
        d("secureConnectEnd :  handshake=$handshake")

        mr.tlsHandshakeInfo = handshake?.toString() ?: ""
    }

    /**
     * Invoked immediately after a socket connection was attempted.
     *
     *
     * If the `call` uses HTTPS, this will be invoked after
     * [.secureConnectEnd], otherwise it will invoked after
     * [.connectStart].
     */
    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol)
        connectEndTime = System.currentTimeMillis()
        d(
            "connectEnd : inetSocketAddress=$inetSocketAddress " +
                    " proxy=$proxy  " +
                    "protocol=$protocol"
        )

        mr.protocol = protocol.toString()
    }

    /**
     * Invoked when a connection attempt fails. This failure is not terminal if further routes are
     * available and failure recovery is enabled.
     *
     *
     * If the `call` uses HTTPS, this will be invoked after [.secureConnectEnd], otherwise it will invoked after [.connectStart].
     */
    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException
    ) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe)
        callEndTime = System.currentTimeMillis()
        d(
            "connectFailed : inetSocketAddress=$inetSocketAddress  " +
                    "proxy=$proxy  " +
                    "protocol=$protocol  " +
                    "ioe=${ioe.localizedMessage}"
        )

        mr.protocol = protocol.toString()
        onEventError(call, ioe)
    }


    /**
     * Invoked after a connection has been acquired for the `call`.
     *
     *
     * This can be invoked more than 1 time for a single [Call]. For example, if the response
     * to the [Call.request] is a redirect to a different address.
     */
    override fun connectionAcquired(call: Call, connection: Connection) {
        super.connectionAcquired(call, connection)
        d("connectionAcquired :  connection=$connection")
    }

    /**
     * Invoked after a connection has been released for the `call`.
     *
     *
     * This method is always invoked after [.connectionAcquired].
     *
     *
     * This can be invoked more than 1 time for a single [Call]. For example, if the response
     * to the [Call.request] is a redirect to a different address.
     */
    override fun connectionReleased(call: Call, connection: Connection) {
        super.connectionReleased(call, connection)
        d("connectionReleased :   connection=$connection")
    }

    /**
     * Invoked just prior to sending request headers.
     *
     *
     * The connection is implicit, and will generally relate to the last
     * [.connectionAcquired] event.
     *
     *
     * This can be invoked more than 1 time for a single [Call]. For example, if the response
     * to the [Call.request] is a redirect to a different address.
     */
    override fun requestHeadersStart(call: Call) {
        super.requestHeadersStart(call)
        requestHeadersStartTime = System.currentTimeMillis()
        d("requestHeadersStart")
    }

    /**
     * Invoked immediately after sending request headers.
     *
     *
     * This method is always invoked after [.requestHeadersStart].
     *
     * @param request the request sent over the network. It is an error to access the body of this
     * request.
     */
    override fun requestHeadersEnd(call: Call, request: Request) {
        super.requestHeadersEnd(call, request)
        requestHeadersEndTime = System.currentTimeMillis()
        d("requestHeadersEnd : request=$request")
    }

    /**
     * Invoked just prior to sending a request body.  Will only be invoked for request allowing and
     * having a request body to send.
     *
     *
     * The connection is implicit, and will generally relate to the last
     * [.connectionAcquired] event.
     *
     *
     * This can be invoked more than 1 time for a single [Call]. For example, if the response
     * to the [Call.request] is a redirect to a different address.
     */
    override fun requestBodyStart(call: Call) {
        super.requestBodyStart(call)
        requestBodyStartTime = System.currentTimeMillis()
        d("requestBodyStart")
    }

    /**
     * Invoked immediately after sending a request body.
     *
     *
     * This method is always invoked after [.requestBodyStart].
     */
    override fun requestBodyEnd(call: Call, byteCount: Long) {
        super.requestBodyEnd(call, byteCount)
        requestBodyEndTime = System.currentTimeMillis()
        d("requestBodyEnd  byteCount=$byteCount")

        mr.requestBodyByteCount = byteCount
    }

    /**
     * Invoked when a request fails to be written.
     *
     *
     * This method is invoked after [.requestHeadersStart] or [.requestBodyStart]. Note
     * that request failures do not necessarily fail the entire call.
     */
    override fun requestFailed(call: Call, ioe: IOException) {
        super.requestFailed(call, ioe)
        callEndTime = System.currentTimeMillis()
        d("requestFailed :  ioe=${ioe.localizedMessage}")

        onEventError(call, ioe)
    }

    /**
     * Invoked just prior to receiving response headers.
     *
     *
     * The connection is implicit, and will generally relate to the last
     * [.connectionAcquired] event.
     *
     *
     * This can be invoked more than 1 time for a single [Call]. For example, if the response
     * to the [Call.request] is a redirect to a different address.
     */
    override fun responseHeadersStart(call: Call) {
        super.responseHeadersStart(call)
        responseHeadersStartTime = System.currentTimeMillis()
        d("responseHeadersStart")
    }

    /**
     * Invoked immediately after receiving response headers.
     *
     *
     * This method is always invoked after [.responseHeadersStart].
     *
     * @param response the response received over the network. It is an error to access the body of
     * this response.
     */
    override fun responseHeadersEnd(call: Call, response: Response) {
        super.responseHeadersEnd(call, response)
        responseHeadersEndTime = System.currentTimeMillis()
        d("responseHeadersEnd :  response=$response")

        mr.responseCode = response.code()
    }

    /**
     * Invoked just prior to receiving the response body.
     *
     *
     * The connection is implicit, and will generally relate to the last
     * [.connectionAcquired] event.
     *
     *
     * This will usually be invoked only 1 time for a single [Call],
     * exceptions are a limited set of cases including failure recovery.
     */
    override fun responseBodyStart(call: Call) {
        super.responseBodyStart(call)
        responseBodyStartTime = System.currentTimeMillis()
        d("responseBodyStart")
    }

    /**
     * Invoked immediately after receiving a response body and completing reading it.
     *
     *
     * Will only be invoked for requests having a response body e.g. won't be invoked for a
     * websocket upgrade.
     *
     *
     * This method is always invoked after [.requestBodyStart].
     */
    override fun responseBodyEnd(call: Call, byteCount: Long) {
        super.responseBodyEnd(call, byteCount)
        responseBodyEndTime = System.currentTimeMillis()
        d("responseBodyEnd :  byteCount=$byteCount")

        mr.responseBodyByteCount = byteCount
    }

    /**
     * Invoked when a response fails to be read.
     *
     *
     * This method is invoked after [.responseHeadersStart] or [.responseBodyStart].
     * Note that response failures do not necessarily fail the entire call.
     */
    override fun responseFailed(call: Call, ioe: IOException) {
        super.responseFailed(call, ioe)
        callEndTime = System.currentTimeMillis()
        d("responseFailed : ioe=${ioe.localizedMessage}")

        onEventError(call, ioe)
    }

    /**
     * Invoked immediately after a call has completely ended.  This includes delayed consumption
     * of response body by the caller.
     *
     *
     * This method is always invoked after [.callStart].
     */
    override fun callEnd(call: Call) {
        super.callEnd(call)
        callEndTime = System.currentTimeMillis()
        d("callEnd : ${call.request().url()}")

        calculateTime()
        netMonitorCallback.onSuccess(call, mr)
    }

    /**
     * Invoked when a call fails permanently.
     *
     *
     * This method is always invoked after [.callStart].
     */
    override fun callFailed(call: Call, ioe: IOException) {
        super.callFailed(call, ioe)
        callEndTime = System.currentTimeMillis()
        d("callFailed : ioe=${ioe.localizedMessage}")
        onEventError(call, ioe)
    }


    /**
     * 计算各个阶段耗时
     */
    private fun calculateTime() {
        mr.dnsCost = (dnsEndTime - dnsStartTime).toInt()
        mr.tcpCost = (secureConnectStartTime - connectStartTime).toInt()
        mr.tlsCost = (secureConnectEndTime - secureConnectEndTime).toInt()
        mr.conectTotalCost = mr.tcpCost + mr.tlsCost
        mr.requestHeaderCost = (requestHeadersEndTime - requestHeadersStartTime).toInt()
        mr.requestBodyCost = (requestBodyEndTime - requestBodyStartTime).toInt()
        mr.requestTotalCost = mr.requestHeaderCost + mr.requestBodyCost
        mr.responseHeaderCost = (responseHeadersEndTime - responseHeadersStartTime).toInt()
        mr.responseBodyCost = (responseBodyEndTime - responseBodyStartTime).toInt()
        mr.responseTotalCost = mr.responseHeaderCost + mr.responseBodyCost
        mr.callCoat = (callEndTime - callStartTime).toInt()
    }


    /**
     * 链路中 所有异常 都会调用到这个方法中
     */
    private fun onEventError(call: Call, ioe: IOException) {
        calculateTime()
        netMonitorCallback.onError(call, mr, ioe)
    }

    private fun d(log: String) {
        if (openLog) {
            Log.d("NetMonitor", log)
        }
    }
}

