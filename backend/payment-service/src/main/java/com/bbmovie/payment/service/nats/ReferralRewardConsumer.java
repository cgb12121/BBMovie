package com.bbmovie.payment.service.nats;

import com.bbmovie.payment.dto.event.NatsConnectionEvent;
import com.bbmovie.payment.dto.request.VoucherCreateRequest;
import com.bbmovie.payment.entity.enums.VoucherType;
import com.bbmovie.payment.service.VoucherService;
import com.bbmovie.payment.utils.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Dispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReferralRewardConsumer {

    private final ObjectMapper objectMapper;
    private final VoucherService voucherService;

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected, subscribing to reward events...");
            setupSubscriptions(event.connection());
        }
    }

    private void setupSubscriptions(Connection nats) {
        Dispatcher dispatcher = nats.createDispatcher(msg -> {
            try {
                String subject = msg.getSubject();
                Map<String, Object> event = objectMapper.readValue(msg.getData(), Map.class);
                log.info("Received reward event on subject: {}", subject);

                if ("rewards.referral".equals(subject)) {
                    String userId = (String) event.get("userId");
                    handleReferralReward(userId);
                }
            } catch (Exception e) {
                log.error("Error processing reward event: {}", e.getMessage());
            }
        });

        dispatcher.subscribe("rewards.referral");
        log.info("Subscribed to rewards.referral");
    }

    private void handleReferralReward(String userId) {
        log.info("Creating referral reward voucher for user: {}", userId);
        
        // Create a 10% discount voucher for the user
        String code = "REF-" + RandomUtil.getRandomNumber(8);
        VoucherCreateRequest request = new VoucherCreateRequest(
                code,
                VoucherType.PERCENTAGE,
                new BigDecimal("10.0"),
                null,
                userId,
                false,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30), // Valid for 30 days
                1,
                true
        );

        voucherService.create(request);
        log.info("Referral reward voucher {} created for user {}", code, userId);
    }
}
