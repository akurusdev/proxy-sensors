package com.proxy.sensors.model;

public class Property {
    private final Integer batchService;
    private final String  remoteUrl;
    private final Integer schedulerTimeSec;
    private final Integer durationTimeSec;

    public Property(Integer batchService, String remoteUrl, Integer schedulerTimeSec, Integer durationTimeSec) {
        this.batchService = batchService;
        this.remoteUrl = remoteUrl;
        this.schedulerTimeSec = schedulerTimeSec;
        this.durationTimeSec = durationTimeSec;
    }

    public Integer getBatchService() {
        return batchService;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public Integer getSchedulerTimeSec() {
        return schedulerTimeSec;
    }

    public Integer getDurationTimeSec() {
        return durationTimeSec;
    }
}
