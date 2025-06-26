package com.example.bbmovie.service.sms.twillio;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class TwilioService {

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    public String sendSms(String toPhoneNumber, String messageBody) {
        try {
            Message message = Message.creator(
                        new PhoneNumber(toPhoneNumber),
                        new PhoneNumber(fromPhoneNumber),
                        messageBody
                    ).create();

            return message.getSid();
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }
}