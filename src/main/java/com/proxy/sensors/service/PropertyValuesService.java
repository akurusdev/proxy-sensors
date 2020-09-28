package com.proxy.sensors.service;

import com.proxy.sensors.model.Property;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyValuesService {
    Property result = null;
    private static PropertyValuesService instance;

    public static synchronized PropertyValuesService getInstance() throws IOException {
        if( instance == null ) {
            instance = new PropertyValuesService();
        }
        return instance;
    }

    private PropertyValuesService() throws IOException {
        String propFileName = "config.properties";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
        try {
            Properties prop = new Properties();
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }


            Integer batchService = Integer.parseInt(prop.getProperty("batchService"));
            String remoteUrl = prop.getProperty("remoteUrl");
            Integer schedulerTimeSec = Integer.parseInt(prop.getProperty("schedulerTimeSec"));
            Integer durationTimeSec = Integer.parseInt(prop.getProperty("durationTimeSec"));
            result = new Property(batchService, remoteUrl, schedulerTimeSec, durationTimeSec);

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if(inputStream != null)
                inputStream.close();
        }
    }

    public Property getPropValues() {
        return result;
    }
}
