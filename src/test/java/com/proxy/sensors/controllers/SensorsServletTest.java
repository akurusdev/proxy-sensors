package com.proxy.sensors.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxy.sensors.model.DataNotification;
import com.proxy.sensors.service.SendEventsService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SensorsServletTest {
    private final ObjectMapper mapper = new ObjectMapper();
    final static Logger logger = LoggerFactory.getLogger(SensorsServletTest.class);

    @ParameterizedTest
    @MethodSource("provideSendMessage")
    public void sensorsServlet(int events, int delay, boolean exception) throws Exception {
        HttpServletRequest request = mock( HttpServletRequest.class );
        HttpServletResponse response = mock( HttpServletResponse.class );
        CountDownLatch countDownLatch = new CountDownLatch(events);
        Server server = createServer(countDownLatch, delay, exception);
        DataNotification data = new DataNotification(
                Instant.now().toEpochMilli(),
                "192.168.1.1",
                "data"
        );
        String dataNotificationJsonString = mapper.writeValueAsString(data);
        when(request.getParameter("sensor")).thenReturn(dataNotificationJsonString);
        for (int i = 0; i < events; i++)
            new SensorsServlet().doPost(request, response);
        assertThat(countDownLatch.await(20000, TimeUnit.MILLISECONDS)).isTrue();
        server.stop();
    }

    @Test
    void testSetMultiNotification() throws Exception {
        HttpServletRequest request = mock( HttpServletRequest.class );
        HttpServletResponse response = mock( HttpServletResponse.class );
        CountDownLatch countDownLatch = new CountDownLatch(100);
        Server server = createServer(countDownLatch, 100, false);
        DataNotification data = new DataNotification(
                Instant.now().toEpochMilli(),
                "192.168.1.1",
                "data"
        );
        String dataNotificationJsonString = mapper.writeValueAsString(data);
        when(request.getParameter("sensor")).thenReturn(dataNotificationJsonString);
        for(int i = 0; i < 10; i++){
            new Thread(() -> {
                for(int j = 0; j < 10; j++){
                    try {
                        new SensorsServlet().doPost(request, response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        assertThat(countDownLatch.await(20000, TimeUnit.MILLISECONDS)).isTrue();
        server.stop();
    }

    private static Stream<Arguments> provideSendMessage() {
        return Stream.of(
                Arguments.of(10, 20, false),
                Arguments.of(9, 20, false),
                Arguments.of(3, 2000, false),
                Arguments.of(2, 200, false)
        );
    }


    private Server createServer(CountDownLatch countDownLatch, Integer delay, Boolean exception) throws Exception {
        Server server = new Server(22222);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        ServletHolder asyncHolder = new ServletHolder(new TestServlet(countDownLatch, delay, exception));
        context.addServlet(asyncHolder, "/test");
        asyncHolder.setAsyncSupported(true);
        server.setHandler(context);
        server.start();
        while (!server.isStarted()) {
            Thread.sleep(10);
        }
        return server;
    }
}
