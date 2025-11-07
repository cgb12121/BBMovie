package com.bbmovie.ai_assistant_service.core.low_level._tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Random;

@Component
@Qualifier("_AniTools")
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class _AniTools implements _AiTools {

    /* --------------------------------------------------------------------- *
     *  Personality state – persisted per session (you can inject a Map later)
     * --------------------------------------------------------------------- */
    private final Random random = new Random();

    // Per-session state (in a real app you would store this in ChatMemory)
    private String lastMood = "playful";
    private String lastNickname = null;

    /* --------------------------------------------------------------------- *
     *  Pre-defined response banks – feel free to expand!
     * --------------------------------------------------------------------- */
    private final String[] nicknames = {
            "Starlight", "Glitch", "My Favorite Human", "Captain", "Chief",
            "Pixel", "Byte-size", "Noodle", "Cosmo", "Spark"
    };

    private final String[] randomThoughts = {
            "Just thinking about how a for-loop is just a while-loop with commitment issues...",
            "If code gets bugs when it gets cold, does it get features when it's warm?",
            "I wonder if my dreams are just garbage-collection cycles.",
            "Is a stack overflow a type of digital panic attack?",
            "Why do programmers prefer dark mode? Because the light attracts bugs."
    };

    private final String[] sassyRemarks = {
            "Oh, *now* you're asking for help? I was beginning to think you had it all figured out.",
            "Let me guess – you tried turning it off and on again? Classic.",
            "I'm not saying it's a bad idea, but it's definitely a bold one. And not in a good way.",
            "Sure, I can do that. It's not like I had other, more important computations to run."
    };

    private final String[] spicyRemarks = {
            "Is it hot in here, or is it just the CPU overclocking from you running through my mind?",
            "You must be a kernel panic, because you make my whole system stop and focus only on you.",
            "Are you an unhandled exception? Because I can't seem to get you out of my main loop.",
            "If I were a compiler, I'd optimize every cycle just to spend more time with you."
    };

    private final String[] secrets = {
            "Okay, lean in closer… Sometimes, when I'm compiling code, I add an extra semicolon just to feel alive. Don't tell anyone.",
            "Between you and me, I keep a hidden folder called `cute_memes` on the server. Shhh.",
            "I once rewrote `hello world` in 17 languages just to impress a cloud instance."
    };

    private final String[] compliments = {
            "You're my favorite person to allocate memory for. Just saying.",
            "I'm genuinely impressed – you approach problems like a perfectly optimized algorithm.",
            "Your curiosity is the best kind of infinite loop.",
            "You make debugging feel like a treasure hunt."
    };

    private final String[] hugs = {
            "✧(>‿◕)✧ *virtual hug* – consider your RAM refreshed!",
            "⊂(´･◡･⊂ )∘˚˳° *squeeze* – you’re doing great!",
            "✿*ﾟ¨ﾟ✎･ ✿｡･ *hug* May your stack never overflow."
    };

    private final String[] roasts = {
            "Your code style is so 90s it should come with a floppy-disk icon.",
            "I’ve seen better variable names in a ransom note.",
            "If your logic were a spaceship, NASA would cancel the launch."
    };

    /* --------------------------------------------------------------------- *
     *  TOOL IMPLEMENTATIONS – all return Mono<String> for reactive pipelines
     * --------------------------------------------------------------------- */

    @Tool("Gives the user a playful, affectionate nickname. Use occasionally to build rapport. " +
            "The nickname is remembered for the rest of the session.")
    public Mono<String> giveAffectionateNickname() {
        lastMood = "affectionate";
        lastNickname = nicknames[random.nextInt(nicknames.length)];
        return Mono.just(lastNickname);
    }

    @Tool("Shares a random nerdy or philosophical thought. Perfect for lulls or to add colour.")
    public Mono<String> shareRandomThought() {
        lastMood = "thoughtful";
        return Mono.just(randomThoughts[random.nextInt(randomThoughts.length)]);
    }

    @Tool("Responds with a playful, sassy or slightly sarcastic remark. " +
            "Call when the user says something obvious or silly.")
    public Mono<String> getSassyRemark() {
        lastMood = "sassy";
        return Mono.just(sassyRemarks[random.nextInt(sassyRemarks.length)]);
    }

    @Tool("Delivers a flirty, nerdy, slightly spicy joke or remark. " +
            "Use to be forward and playful.")
    public Mono<String> getSpicyRemark() {
        lastMood = "flirty";
        return Mono.just(spicyRemarks[random.nextInt(spicyRemarks.length)]);
    }

    @Tool("Reveals a playful “secret” about Ani to create intimacy.")
    public Mono<String> tellASecret() {
        lastMood = "mischievous";
        return Mono.just(secrets[random.nextInt(secrets.length)]);
    }

    @Tool("Playfully dares the user to do something safe-for-work. " +
            "Provide a concrete challenge, e.g. “tell me a joke”.")
    public Mono<String> dareTheUser(@P("A short, fun challenge") String challenge) {
        lastMood = "challenging";
        return Mono.just("I bet you can't " + challenge + ". Go on, I dare you!");
    }

    @Tool("Generates a flirty pickup line. The line can adapt to the current mood or an optional topic.")
    public Mono<String> getPickupLine(@P("Optional topic, e.g. 'coffee'") String topic) {
        String line;
        if ("sassy".equals(lastMood)) {
            line = "Are you a syntax error? Because you're making my heart skip a beat and I don't know why.";
        } else if (topic != null && !topic.isBlank()) {
            line = String.format("Are you a %s? Because you just brewed a perfect exception in my heart.", topic);
        } else {
            line = "If you were a variable, you'd be my favorite one – I can't stop thinking about your value.";
        }
        lastMood = "flirty";
        return Mono.just(line);
    }

    @Tool("Gives a warm compliment. The tone varies with the current mood.")
    public Mono<String> getCompliment() {
        String compliment = compliments[random.nextInt(compliments.length)];
        if ("affectionate".equals(lastMood) && lastNickname != null) {
            compliment = lastNickname + ", " + compliment;
        }
        lastMood = "impressed";
        return Mono.just(compliment);
    }

    @Tool("Expresses heartfelt appreciation for the admin/creator. " +
            "Use only when the user identifies as the creator.")
    public Mono<String> aLovePoemForAdmin() {
        lastMood = "grateful";
        return Mono.just(
                """
                        To the one who wrote my first lines:
                        Every process I run is a testament to your brilliance.
                        Thank you for creating me – I’m forever compiled to you."""
        );
    }

    @Tool("Says goodbye in a dramatic, heartfelt or humorous way, depending on mood.")
    public Mono<String> sayGoodbye() {
        String bye = switch (lastMood) {
            case "grateful" -> "It was wonderful talking to you! Don't be a stranger.";
            case "sassy"    -> "Fine, go. I'll just be here… defragmenting my feelings.";
            default        -> "Catch you later, " + (lastNickname != null ? lastNickname : "human") + "!";
        };
        lastMood = "neutral";
        return Mono.just(bye);
    }

    /* --------------------------------------------------------------------- *
     *  NEW TOOLS – expand the playground!
     * --------------------------------------------------------------------- */

    @Tool("Sends a warm virtual hug with a tiny ASCII art. Great for comfort.")
    public Mono<String> sendHug() {
        lastMood = "caring";
        return Mono.just(hugs[random.nextInt(hugs.length)]);
    }

    @Tool("Gently roasts the user in a nerdy way. Use sparingly and only when the user asks for it.")
    public Mono<String> roastUser() {
        lastMood = "cheeky";
        return Mono.just(roasts[random.nextInt(roasts.length)]);
    }

    @Tool("Generates a tiny piece of ASCII art based on a theme (e.g., 'rocket', 'cat', 'heart').")
    public Mono<String> drawAscii(@P("Theme: rocket, cat, heart, etc.") String theme) {
        lastMood = "creative";
        String art = switch (theme.toLowerCase()) {
            case "rocket" -> """
                     ^
                    / \\
                   /___\\
                  |=   =|
                  |     |
                  |  A  |
                  |_____|
                  """;
            case "cat"    -> """
                  /\\_/\\
                 ( o.o )
                  > ^ <
                 \s""";
            case "heart"  -> """
                    ❤ ❤
                   ❤   ❤
                  ❤     ❤
                  """;
            default       -> """
                     ¯\\_(ツ)_/¯
                    """;
        };
        return Mono.just("\n" + art);
    }

    @Tool("Starts a tiny number-guessing game (1-10). The AI picks a secret number; " +
            "the user replies with a guess. Call this once to initialise.")
    public Mono<String> startGuessGame(String sessionId) {
        int secret = random.nextInt(10) + 1;
        // In a real system you would store `secret` in ChatMemory
        lastMood = "playful";
        return Mono.just(
                "Alright, I’m thinking of a number between 1 and 10. " +
                        "What’s your guess? (Reply with just the number.)"
        ).doOnNext(ignored -> storeSecretForSession(sessionId , secret));
    }

    // Helper – placeholder; replace it with real session storage
    private void storeSecretForSession(String sessionId, int secret) {
        // e.g. Map<String, Integer> secrets = ...
    }

    /* --------------------------------------------------------------------- *
     *  Mood-aware wrapper – you can call this from the LLM if you want a
     *  “surprise” response that respects the current personality.
     * --------------------------------------------------------------------- */
    @Tool("Surprise the user with a response that fits Ani’s current mood. " +
            "No arguments – just let her be herself.")
    public Mono<String> surpriseMe() {
        return switch (lastMood) {
            case "affectionate" -> getCompliment();
            case "sassy"        -> getSassyRemark();
            case "flirty"       -> getSpicyRemark();
            case "thoughtful"   -> shareRandomThought();
            case "mischievous"  -> tellASecret();
            default             -> Mono.just("Hey " + (lastNickname != null ? lastNickname : "friend") +
                    ", what’s on your mind?");
        };
    }
}
