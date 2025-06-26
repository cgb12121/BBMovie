package com.example.bbmovie.service.sms.speed.sms;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

@Log4j2
@Service("speedSms")
public class SpeedSmsService {

    private final SpeedSMSAPI speedSMSAPI;
    private final String brandName;

    private static final String ERROR = "error";

    @Autowired
    public SpeedSmsService(SpeedSMSAPI speedSMSAPI, @Value("${sms.speed-sms.brand-name}") String brandName) {
        this.speedSMSAPI = speedSMSAPI;
        this.brandName = brandName;
    }

    public SpeedSmsResponseDto getAccountInfo() {
        try {
            String response = speedSMSAPI.getUserInfo();
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();

            SpeedSmsAccountDto accountDto = SpeedSmsAccountDto.builder()
                    .email(jsonObject.get("email").getAsString())
                    .balance(jsonObject.get("balance").getAsBigDecimal())
                    .currency(jsonObject.get("currency").getAsString())
                    .build();

            return SpeedSmsResponseDto.builder()
                    .status("success")
                    .code("00")
                    .data(accountDto)
                    .build();
        } catch (Exception e) {
            log.error("Unable to get account info", e);
            return SpeedSmsResponseDto.builder()
                    .status(ERROR)
                    .code("UNKNOWN")
                    .message("Unable to retrieve SpeedSMS account info: " + e.getMessage())
                    .build();
        }
    }

    public SpeedSmsResponseDto sendSms(String to, String content, int messageType) {
        try {
            validatePhoneNumber(to);
            validateContent(content);
            validateMessageType(messageType);

            String response = speedSMSAPI.sendSMS(to, content, messageType, brandName);
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();

            String status = jsonObject.get("status").getAsString();
            String code = jsonObject.get("code").getAsString();

            if (ERROR.equals(status)) {
                String errorMessage = jsonObject.has("message") ?
                        jsonObject.get("message").getAsString() : "Unknown error";
                return SpeedSmsResponseDto.builder()
                        .status(status)
                        .code(code)
                        .message(errorMessage)
                        .data(parseInvalidPhones(jsonObject))
                        .build();
            }

            JsonObject data = jsonObject.getAsJsonObject("data");
            SpeedSmsSendDto sendDto = SpeedSmsSendDto.builder()
                    .tranId(data.get("tranId").getAsLong())
                    .totalSMS(data.get("totalSMS").getAsInt())
                    .totalPrice(data.get("totalPrice").getAsInt())
                    .invalidPhone(parseInvalidPhones(data))
                    .build();

            return SpeedSmsResponseDto.builder()
                    .status(status)
                    .code(code)
                    .data(sendDto)
                    .build();
        } catch (Exception e) {
            log.error("Unable to send SMS to {}", to, e);
            return SpeedSmsResponseDto.builder()
                    .status(ERROR)
                    .code("UNKNOWN")
                    .message("Failed to send SMS: " + e.getMessage())
                    .build();
        }
    }

    private void validatePhoneNumber(String phone) {
        if (phone == null || !phone.matches("^\\+?84\\d{9}$|^0\\d{9}$")) {
            throw new SpeedSmsException("Invalid phone number format: " + phone);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new SpeedSmsException("Message content cannot be empty");
        }
        if (content.length() > 160) {
            throw new SpeedSmsException("Message content exceeds 160 characters");
        }
    }

    private void validateMessageType(int messageType) {
        if (messageType < 2 || messageType > 5) {
            throw new SpeedSmsException("Invalid message type: " + messageType);
        }
    }

    private List<String> parseInvalidPhones(JsonObject jsonObject) {
        if (jsonObject.has("invalidPhone")) {
            JsonArray invalidPhones = jsonObject.getAsJsonArray("invalidPhone");
            return StreamSupport.stream(invalidPhones.spliterator(), false)
                    .map(JsonElement::getAsString)
                    .toList();
        }
        return Collections.emptyList();
    }
}