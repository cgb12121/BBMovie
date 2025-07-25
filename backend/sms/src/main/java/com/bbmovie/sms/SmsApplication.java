package com.bbmovie.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsApplication.class, args);
    }

//    @KafkaListener(topics = KafkaTopicConfig.OTP_SMS_TOPIC, groupId = "email-service-group")
//    public void consumeOtp(@Payload Map<String, String> event,
//                           @Header(KafkaHeaders.RECEIVED_KEY) String phoneNumber,
//                           Acknowledgment acknowledgment) {
//        try {
//            String otp = event.get("otp");
//            smsService.sendSms(phoneNumber, "Your OTP is: " + otp);
//            log.info("Sent SMS OTP to phone: {}", phoneNumber);
//        } catch (Exception e) {
//            log.error("Failed to send SMS OTP to phone {}: {}", phoneNumber, e.getMessage());
//            // No retry for SMS, as per fire-and-forget
//        } finally {
//            acknowledgment.acknowledge(); // Commit offset even on failure
//        }
//    }
}
