package com.bbmovie.referralservice.service;

import com.bbmovie.referralservice.entity.Referral;
import com.bbmovie.referralservice.entity.ReferralUser;
import com.bbmovie.referralservice.repository.ReferralRepository;
import com.bbmovie.referralservice.repository.ReferralUserRepository;
import com.bbmovie.referralservice.service.nats.AbstractNatsJetStreamService;
import io.nats.client.JetStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReferralService extends AbstractNatsJetStreamService {

    private final ReferralRepository referralRepository;
    private final ReferralUserRepository userRepository;

    @Transactional
    public void saveUser(UUID userId, String email, String referralCode) {
        ReferralUser user = ReferralUser.builder()
                .userId(userId)
                .email(email)
                .referralCode(referralCode)
                .build();
        userRepository.save(user);
        log.info("Saved local referral user info: {} ({})", email, referralCode);
    }

    @Transactional
    public void handleUserRegistered(UUID userId, String referredByCode) {
        if (referredByCode == null || referredByCode.isBlank()) {
            return;
        }

        userRepository.findByReferralCode(referredByCode).ifPresentOrElse(referrer -> {
            if (referralRepository.existsByReferredId(userId)) {
                log.warn("User {} already has a referral record", userId);
                return;
            }

            Referral referral = Referral.builder()
                    .referrerId(referrer.getUserId())
                    .referredId(userId)
                    .status("JOINED")
                    .build();
            referralRepository.save(referral);
            log.info("Recorded referral: User {} referred by {}", userId, referrer.getUserId());
        }, () -> log.warn("Invalid referral code used: {}", referredByCode));
    }

    @Transactional
    public void handleSubscriptionSuccess(String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        referralRepository.findByReferredId(userId).ifPresent(referral -> {
            if ("REWARDED".equals(referral.getStatus())) {
                log.info("User {} already rewarded for referral", userId);
                return;
            }

            // Trigger reward for referrer
            publishRewardEvent(referral.getReferrerId());

            referral.setStatus("REWARDED");
            referral.setRewardedAt(LocalDateTime.now());
            referralRepository.save(referral);
            log.info("Referrer {} rewarded for user {} subscription", referral.getReferrerId(), userId);
        });
    }

    private void publishRewardEvent(UUID referrerId) {
        JetStream jetStream = getJetStream();
        if (jetStream == null) {
            log.warn("NATS JetStream not available for rewards");
            return;
        }

        try {
            Map<String, String> event = Map.of(
                    "userId", referrerId.toString(),
                    "type", "REFERRAL_REWARD"
            );
            byte[] data = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(event);
            jetStream.publish("rewards.referral", data);
            log.info("Published reward event for user {}", referrerId);
        } catch (Exception e) {
            log.error("Failed to publish reward event: {}", e.getMessage());
        }
    }
}
