package com.proxy.sensors.http;


import com.proxy.sensors.service.SendEventsService;
import io.netty.util.HashedWheelTimer;
import org.asynchttpclient.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class DefaultHttpClient implements AutoCloseable{

    public static final int DEFAULT_CONNECTION_TIMEOUT = 1000;
    public static final int DEFAULT_REQUEST_TIMEOUT = 1000;
    public static final int DEFAULT_READ_TIMEOUT = 1000;
    private AsyncHttpClient asyncHttpClient;
    private int connectTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;
    private HashedWheelTimer nettyTimer;
    final static Logger logger = LoggerFactory.getLogger(DefaultHttpClient.class);

    public void doPost(@NotNull String url, String json, @NotNull HttpClientCallback callback) {

        BoundRequestBuilder boundRequestBuilder = asyncHttpClient.preparePost(url).
                setHeader("Content-Type", "application/json").
                setHeader("Content-Length", String.valueOf(json.length())).
                setBody(json);

        execute(boundRequestBuilder, callback);
    }

    private void execute(BoundRequestBuilder boundRequestBuilder, HttpClientCallback callback) {
        Request request = boundRequestBuilder.build();

        asyncHttpClient.executeRequest(request, new AsyncCompletionHandler<Integer>() {

            @Override
            public State onHeadersWritten() {
                return super.onHeadersWritten();
            }

            @Override
            public State onStatusReceived(HttpResponseStatus status) throws Exception {
                return super.onStatusReceived(status);
            }

            @Override
            public Integer onCompleted(Response response) {
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                if (t instanceof TimeoutException) {
                    callback.onTimeout(t);
                } else {
                    t.printStackTrace();
                    callback.onError(t);
                }
            }
        });
    }

    public void init() {
        nettyTimer = new HashedWheelTimer(1, TimeUnit.MILLISECONDS);
        this.asyncHttpClient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setConnectTimeout(connectTimeout)
                        .setRequestTimeout(requestTimeout)
                        .setReadTimeout(readTimeout)
                        .setNettyTimer(nettyTimer)
                        .build()
        );
    }

    public void destroy() {
        nettyTimer.stop();
        try {
            asyncHttpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        destroy();
    }
}
