package com.bbmovie.payment.service.job;

import com.bbmovie.payment.service.nats.AbstractNatsJetStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class DailyReportJob extends AbstractNatsJetStreamService implements Job {

    private final ObjectMapper objectMapper;

    @Autowired
    public DailyReportJob( ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @Override
    public void execute(JobExecutionContext context) {
        JetStream jetStream = getJetStream();
        log.info("Running daily report job..., {}", context.getJobDetail());
        try {
            //TODO
            // For a demo, send a simple report event. Replace it with a real aggregation payload.
            byte[] reportJson = objectMapper.writeValueAsBytes("{\"type\":\"payment.daily.report\",\"status\":\"OK\"}");
            jetStream.publish("payment.report.daily", reportJson);
        } catch (Exception e) {
            log.error("Failed to publish daily report", e);
        }
    }
}