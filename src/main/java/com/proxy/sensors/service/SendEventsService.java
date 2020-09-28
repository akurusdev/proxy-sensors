package com.proxy.sensors.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxy.sensors.controllers.SensorsServlet;
import com.proxy.sensors.http.DefaultHttpClient;
import com.proxy.sensors.http.HttpClientCallback;
import com.proxy.sensors.model.DataNotification;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SendEventsService implements AutoCloseable {
    private final DefaultHttpClient client = new DefaultHttpClient();
    private final CacheService errorCache = new CacheService();
    private final String url;
    private final Integer schedulerTimeSec;
    private final ScheduledFuture<?> scheduledFuture;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private final ObjectMapper mapper = new ObjectMapper();
    final static Logger logger = LoggerFactory.getLogger(SendEventsService.class);

    public SendEventsService() throws Exception {
        client.init();
        PropertyValuesService instance = PropertyValuesService.getInstance();
        if(instance != null &&  instance.getPropValues() != null) {
            url = instance.getPropValues().getRemoteUrl();
            schedulerTimeSec = instance.getPropValues().getSchedulerTimeSec();
        }else {
            throw new Exception("Exception url initialize");
        }

        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
            List<DataNotification> sensorDataListAll = errorCache.getSensorDataListAll();
            if(!sensorDataListAll.isEmpty()){
                sendBatch(sensorDataListAll);
                logger.info("errorCache send"+ sensorDataListAll.size());
            }
        }, schedulerTimeSec, schedulerTimeSec, TimeUnit.SECONDS);
    }

    public Instant sendBatch(List<DataNotification> sensorDataBatch) {
        try {
            String dataNotificationJsonString = mapper.writeValueAsString(sensorDataBatch);
            client.doPost(url,
                    dataNotificationJsonString,
                    new HttpClientCallback() {
                        @Override
                        public void onError(@NotNull Throwable exception) {
                            sensorDataBatch.forEach(it -> {
                                errorCache.addSensorData(it);
                            });
                        }

                        @Override
                        public void onTimeout(Throwable exception) {
                            exception.printStackTrace();
                            sensorDataBatch.forEach(it -> {
                                errorCache.addSensorData(it);
                            });
                        }
                    }
            );

        } catch (JsonProcessingException e) {
            sensorDataBatch.forEach(it -> {
                errorCache.addSensorData(it);
            });
            e.printStackTrace();
        }
        return Instant.now();
    }

    @Override
    public void close() throws Exception {
        scheduledFuture.cancel(true);
        scheduledExecutorService.shutdown();
    }
}
