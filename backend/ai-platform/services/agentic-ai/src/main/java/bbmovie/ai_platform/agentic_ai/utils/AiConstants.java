package bbmovie.ai_platform.agentic_ai.utils;

import lombok.experimental.UtilityClass;

/**
 * Centralized constants for the agentic-ai service.
 *
 * Avoids magic strings scattered across advisors, factories, and workers.
 */
@UtilityClass
public final class AiConstants {

    // --- Spring AI ChatMemory Advisor Keys ---

    /**
     * The key used by {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor}
     * to store and retrieve the active conversation ID in the advisor context.
     *
     * Format: "userId:sessionId" (see {@link bbmovie.ai_platform.agentic_ai.dto.ConversationId})
     */
    public static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    /**
     * The key used by {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor}
     * to control the number of past messages to retrieve from memory.
     */
    public static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_retrieve_size";

    /**
     * Default number of past messages to include in each AI request.
     */
    public static final int DEFAULT_MEMORY_RETRIEVE_SIZE = 20;

    /**
     * Maximum number of explicit memory records an AI can save per user.
     * When this limit is reached, {@code saveMemory} silently skips the save
     * without notifying the AI or the client.
     */
    public static final int MAX_MEMORIES_PER_USER = 20;
}
