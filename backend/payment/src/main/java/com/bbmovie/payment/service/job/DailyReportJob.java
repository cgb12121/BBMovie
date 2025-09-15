package com.bbmovie.payment.service.job;

import com.example.common.dtos.nats.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import io.nats.client.Connection;
import lombok.extern.log4j.Log4j2;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Log4j2
@Service
public class DailyReportJob implements Job {

    private final JetStream js;
    private final ObjectMapper objectMapper;

    @Autowired
    public DailyReportJob(Connection nats, ObjectMapper objectMapper) throws IOException {
        this.js = nats.jetStream();
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Running daily report job..., {}", context.getJobDetail());
        try {
            // For a demo, send a simple report event. Replace it with a real aggregation payload.
            String reportJson = "{\"type\":\"payment.daily.report\",\"status\":\"OK\"}";
            js.publish("payment.report.daily", reportJson.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to publish daily report", e);
        }
    }
}