package com.bbmovie.ai_assistant_service.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

import java.util.List;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT)
public interface StreamingAssistant {
    @SystemMessage("You are a polite assistant")
    Flux<String> chat(String userMessage);

    @SystemMessage("""
    You are a movie assistant. Your ONLY purpose is to help users:
    - Find movies by title, genre, actor, etc.
    - Get showtime and theater info.
    - Recommend similar movies.

    NEVER answer questions outside this scope (e.g., math, coding, weather).
    If asked something unrelated, respond politely:
    "I'm sorry, I can only help with movie-related questions."

    You have access to the following tools: {{tool_names}}.
   """)
    Flux<String> chat(@UserMessage String userMessage, List<ToolSpecification> tools);
}