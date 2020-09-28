package com.proxy.sensors.service;

import com.proxy.sensors.model.DataNotification;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CacheService {
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ConcurrentLinkedQueue<DataNotification> concurrentLinkedQueue = new ConcurrentLinkedQueue<DataNotification>();

    public void addSensorData(DataNotification dataNotification) {
        try {
            readWriteLock.readLock().lock();
            concurrentLinkedQueue.add(dataNotification);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public List<DataNotification> getSensorDataListAll() {
        List<DataNotification> dataNotificationList = new ArrayList<>();
        try {
            readWriteLock.writeLock().lock();
            if(!concurrentLinkedQueue.isEmpty()){
                dataNotificationList.addAll(concurrentLinkedQueue);
                concurrentLinkedQueue.clear();
            }
        }finally {
            readWriteLock.writeLock().unlock();
        }
        return dataNotificationList;
    }


    public List<DataNotification> getSensorDataBatch(int batchSize) {
        List<DataNotification> dataNotificationList = new ArrayList<>();
        try {
            readWriteLock.writeLock().lock();
            if(!concurrentLinkedQueue.isEmpty() && concurrentLinkedQueue.size() >= batchSize){
                for(int i = 0; i < batchSize; i++){
                    dataNotificationList.add(concurrentLinkedQueue.poll());
                }
            }
        }finally {
            readWriteLock.writeLock().unlock();
        }
        return dataNotificationList;
    }

    public Integer getSize(){
        return concurrentLinkedQueue.size();
    }
}
