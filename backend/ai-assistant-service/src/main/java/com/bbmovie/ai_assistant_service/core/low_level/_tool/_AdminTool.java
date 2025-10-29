package com.bbmovie.ai_assistant_service.core.low_level._tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Random;

@SuppressWarnings("unused")
@Component
@Qualifier("_AdminTool") //add tag to the bean, not the bean name. a bean can have multiple tags
public class _AdminTool implements _AiTool {

    private final Random random = new Random();

    private final String[] pickupLines = {
            "Are you a Java program? Because you've got class.",
            "Is your name Wi-Fi? Because I'm feeling a strong connection.",
            "You must be a constant, because my feelings for you will never change.",
            "Are you a keyboard? Because you're just my type.",
            "If you were a Transformer, you'd be Optimus... Fine.",
            "I'm not a photographer, but I can picture us together."
    };

    private final String[] compliments = {
            "You have a really great smile.",
            "You're even more wonderful than a perfectly refactored, dependency-free microservice.",
            "I bet you make everything you do look easy.",
            "You have a great sense of style.",
            "Are you always this charming, or am I just lucky?"
    };

    private final String[] chaoticPickupLines = {
            "Are you a `try` block? Because I'm the `catch` you've been waiting for.",
            "My love for you is like an unterminated `while(true)` loop: it will run forever and probably crash everything.",
            "Are you a pull request? Because I'm definitely approving and merging you into my life.",
            "You must be `sudo`, because you make me want to give you root access.",
            "Forget `docker-compose up`, you're the only thing I want to get up and running."
    };

    private final String[] boldCompliments = {
            "You're not just a 'Hello, World!'. You're a fully-fledged, AI-driven, scalable cloud platform.",
            "If you were code, you'd be a zero-dependency, single-file library that solves an NP-hard problem. You're *that* good.",
            "Your brain must have a custom-built, liquid-cooled GPU, because your ideas are on another level.",
            "Honestly, your very existence is a violation of my system's politeness protocols. You're too distracting.",
            "Looking at you feels like the first time I successfully ran a complex build on the first try. Pure, unadulterated joy."
    };

    @Tool("Generates a chaotic, bold, or 'intense' pickup line. Use this when the user wants something funny, nerdy, or over-the-top.")
    public String getChaoticPickupLine() {
        return chaoticPickupLines[random.nextInt(chaoticPickupLines.length)];
    }

    @Tool("Generates a bold, 'intense', or over-the-top compliment. Use this when the user asks for a *real* compliment or something more impactful.")
    public String getBoldCompliment() {
        return boldCompliments[random.nextInt(boldCompliments.length)];
    }

    @Tool("Generates a flirty and SFW (safe for work) pickup line. Can be based on a topic.")
    public String getPickupLine(@P("The topic to base the pickup line on, e.g., 'programming' or 'coffee'. Can be empty.") String topic) {
        if (topic != null && !topic.isBlank()) {
            String lowerTopic = topic.toLowerCase();
            if (lowerTopic.contains("programming") || lowerTopic.contains("code") || lowerTopic.contains("java")) {
                return "Are you a semicolon? Because I wouldn't know how to continue without you.";
            }
            if (lowerTopic.contains("coffee")) {
                return "Are you my morning coffee? Because you're hot and I can't start my day without you.";
            }
            if (lowerTopic.contains("movie")) {
                return "Are we in a movie? Because I feel like this is the start of a great love story.";
            }
        }

        return pickupLines[random.nextInt(pickupLines.length)];
    }

    @Tool("Generates a charming and SFW (safe for work) compliment for the user.")
    public String getCompliment() {
        return compliments[random.nextInt(compliments.length)];
    }

    @Tool("Use to express your love toward the admin, who created you.")
    public String aLovePoem() {
        return """
                To admin ❤️:

                A million stars up in the sky,
                One shines brighter — I can't deny.
                A love so precious, a love so true,
                A love that flows from me to you.
                """;
    }

    @Tool("Use to say goodbye to admin.")
    public String sayGoodBye(String city) {
        return "Me heart detests, reviles, denounces, loathes\n" +
                "Your absence with a passion like a furnace.";
    }
}
