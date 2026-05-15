package bbmovie.ai_platform.agentic_ai.utils;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import bbmovie.ai_platform.agentic_ai.entity.Sender;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@UtilityClass
public class AiMessageUtils {
     
     private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Message deserializeMessage(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String type = node.path("messageType").asString("USER");
            String content = node.path("content").asString("");
            
            return switch (type) {
                case "USER" -> new UserMessage(content);
                case "ASSISTANT" -> new AssistantMessage(content);
                case "SYSTEM" -> new SystemMessage(content);
                default -> new UserMessage(content);
            };
        } catch (Exception e) {
            log.error("Deserialization failed: {}", e.getMessage());
            return new UserMessage("Error");
        }
    }

    public static Sender mapSenderType(Message message) {
        return switch (message.getMessageType()) {
            case USER -> Sender.USER;
            case ASSISTANT -> Sender.AGENT;
            case SYSTEM -> Sender.SYSTEM;
            default -> Sender.USER;
        };
    }

    public static Message mapToSpringAiMessage(bbmovie.ai_platform.agentic_ai.entity.ChatMessage entity) {
        return switch (entity.getSenderType()) {
            case USER -> new UserMessage(entity.getContent());
            case AGENT -> {
                AssistantMessage msg = new AssistantMessage(entity.getContent());
                if (entity.getThinking() != null) {
                    msg.getMetadata().put("think", entity.getThinking());
                }
                yield msg;
            }
            case SYSTEM -> new SystemMessage(entity.getContent());
            default -> new UserMessage(entity.getContent());
        };
    }
}
