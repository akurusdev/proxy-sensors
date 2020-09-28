package com.proxy.sensors.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxy.sensors.model.DataNotification;
import com.proxy.sensors.service.SendEventsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestServlet extends HttpServlet {

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
    private CountDownLatch countDownLatch;
    private ObjectMapper mapper = new ObjectMapper();
    private Integer delay;
    private boolean exception;

    public TestServlet(CountDownLatch countDownLatch, Integer delay, Boolean exception) {
        this.countDownLatch = countDownLatch;
        this.delay = delay;
        this.exception = exception;
    }

    final static Logger logger = LoggerFactory.getLogger(TestServlet.class);
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final AsyncContext ctxt = req.startAsync();

        List<DataNotification> dataNotification = mapper.readValue(req.getInputStream(), new TypeReference<List<DataNotification>>(){});
        ctxt.start(() -> {
            executorService.schedule(() -> {
                ctxt.getResponse().getWriter().write("Ok!");
                ctxt.complete();
                if(exception)
                    throw new Exception("exeption");
                if(dataNotification !=null && !dataNotification.isEmpty()){
                    dataNotification.forEach(it->{
                        countDownLatch.countDown();
                        logger.info("count downIterator"+countDownLatch.toString());
                    });
                }
                return null;

            }, delay, TimeUnit.MILLISECONDS);
        });
    }


    @Override
    public void destroy() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Executor service shutdown interrupted");
        }
     }
}
