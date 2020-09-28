package com.proxy.sensors.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxy.sensors.model.DataNotification;
import com.proxy.sensors.service.ServiceEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/notifications")
public class SensorsServlet extends HttpServlet {

    private final ObjectMapper mapper;
    private final ServiceEventProcessor serviceEventProcessor;
    public SensorsServlet() throws Exception {
        serviceEventProcessor = ServiceEventProcessor.getInstance();
        mapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sensor = request.getParameter("sensor");
        DataNotification dataNotification = mapper.readValue(sensor, DataNotification.class);
        serviceEventProcessor.process(dataNotification);
    }
}
