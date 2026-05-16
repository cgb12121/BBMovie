package bbmovie.ai_platform.agentic_ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for {@link bbmovie.ai_platform.agentic_ai.service.chat.advisors.ThinkingAdvisor}.
 *
 * <p>Allows customizing which model-specific "thinking" tags are recognized, without
 * recompiling the application. Tags are matched by index — openTags[i] pairs with closeTags[i].
 *
 * <p>Default pairs:
 * <ul>
 *   <li>{@code <think>} ↔ {@code </think>} — DeepSeek-R1, Qwen-Reasoning</li>
 *   <li>{@code <thinking>} ↔ {@code </thinking>} — some Claude-style models</li>
 * </ul>
 *
 * <p>Example override in {@code application.yaml}:
 * <pre>
 * ai:
 *   advisor:
 *     thinking:
 *       open-tags:
 *         - "<think>"
 *         - "<thinking>"
 *         - "<reasoning>"
 *       close-tags:
 *         - "</think>"
 *         - "</thinking>"
 *         - "</reasoning>"
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "ai.advisor.thinking")
public class ThinkingAdvisorProperties {

    /**
     * List of opening tags that mark the start of a thinking block.
     * Must have a corresponding entry at the same index in {@code closeTags}.
     */
    private List<String> openTags = new ArrayList<>(List.of("<think>", "<thinking>"));

    /**
     * List of closing tags that mark the end of a thinking block.
     * Must be the same length as {@code openTags}.
     */
    private List<String> closeTags = new ArrayList<>(List.of("</think>", "</thinking>"));

    public List<String> getOpenTags() { return openTags; }
    public void setOpenTags(List<String> openTags) { this.openTags = openTags; }

    public List<String> getCloseTags() { return closeTags; }
    public void setCloseTags(List<String> closeTags) { this.closeTags = closeTags; }
}
