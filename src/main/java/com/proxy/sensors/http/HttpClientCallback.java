package com.proxy.sensors.http;

public interface HttpClientCallback {
    void onError(Throwable exception);
    void onTimeout(Throwable exception);
}
