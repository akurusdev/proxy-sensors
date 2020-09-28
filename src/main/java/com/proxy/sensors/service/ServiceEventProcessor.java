package com.proxy.sensors.service;

import com.proxy.sensors.model.DataNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceEventProcessor implements AutoCloseable {
    private final Integer batchSize;
    private final Integer schedulerTimeSec;
    private final Integer durationTimeSec;
    private final AtomicInteger sizeQueue = new AtomicInteger();
    private final CacheService cacheService = new CacheService();
    private final SendEventsService sendEventsService = new SendEventsService();
    private final ScheduledFuture<?> scheduledFuture;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private volatile Instant lastUpdateDate = Instant.now();
    private static ServiceEventProcessor instance;
    final static Logger logger = LoggerFactory.getLogger(ServiceEventProcessor.class);

    public static synchronized ServiceEventProcessor getInstance() throws Exception {
        if( instance == null )
            instance = new ServiceEventProcessor();
        return instance;
    }

    private ServiceEventProcessor() throws Exception {
        Integer batchSize = 1;
        PropertyValuesService instance = PropertyValuesService.getInstance();
        if(instance != null &&  instance.getPropValues() != null) {
            batchSize = instance.getPropValues().getBatchService();
            schedulerTimeSec = instance.getPropValues().getSchedulerTimeSec();
            durationTimeSec = instance.getPropValues().getDurationTimeSec();
        }else {
            throw new Exception("Exception url initialize");
        }
        this.batchSize = batchSize;

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
            Long delta = Instant.now().getEpochSecond() - lastUpdateDate.getEpochSecond();
            if(durationTimeSec <= delta){
                List<DataNotification> sensorDataListAll = cacheService.getSensorDataListAll();
                if(!sensorDataListAll.isEmpty()){
                    lastUpdateDate = sendEventsService.sendBatch(sensorDataListAll);
                    sizeQueue.getAndAdd(-sensorDataListAll.size());
                }
            }
        }, schedulerTimeSec, schedulerTimeSec, TimeUnit.SECONDS);
    }

    public void process(DataNotification dataNotification) {
        cacheService.addSensorData(dataNotification);
        if(checkQueueSize()){
            List<DataNotification> sensorDataBatch = cacheService.getSensorDataBatch(batchSize);
            lastUpdateDate =  sendEventsService.sendBatch(sensorDataBatch);
        }
    }

    private Boolean checkQueueSize() {
        int currentSize = sizeQueue.incrementAndGet();
        if( (currentSize % batchSize) == 0){
            sizeQueue.getAndAdd(-batchSize);
            return true;
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        scheduledFuture.cancel(true);
        scheduledExecutorService.shutdown();
    }
}
