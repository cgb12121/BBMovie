package com.bbmovie.ai_assistant_service.core.low_level._config._ai;

import com.bbmovie.ai_assistant_service.core.low_level._utils._AiModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class _ModelConfig {

    /**
     * These builder options control how the LLM behaves —
     * affecting creativity, determinism, context size, repetition handling and runtime diagnostics.
     *
     * <p>All options are optional. If not specified, sensible defaults are used.</p>
     *
     * <b> A. Creativity and Randomness Control</b>
     *
     * <ul>
     *   <li><b>temperature(double)</b> — Randomness in token sampling.
     *       <ul>
     *         <li>Range: 0.0 – 2.0 (default ≈ 0.7)</li>
     *         <li>Low values → deterministic, factual responses</li>
     *         <li>High values → creative, diverse responses</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>topK(int)</b> — Limit candidate tokens to top-K most probable.
     *       <ul>
     *         <li>Range: 0–100 (default: 40)</li>
     *         <li>0 disables this feature</li>
     *         <li>Smaller K = more focused; larger K = more diverse</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>topP(double)</b> — Nucleus sampling; only consider the smallest
     *       probability mass ≥ topP.
     *       <ul>
     *         <li>Range: 0.0–1.0 (default: 0.9)</li>
     *         <li>Combines well with temperature</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>minP(double)</b> — Discard tokens with probability lower than minP.
     *       <ul>
     *         <li>Range: 0.0–1.0</li>
     *         <li>Prevents nonsensical token choices</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     *  <b>B. Adaptive Sampling (Mirostat)</b>
     *
     * <ul>
     *   <li><b>mirostat(boolean)</b> — Enable Mirostat adaptive sampling.
     *       <ul>
     *         <li>Maintains constant "entropy" (creativity level) dynamically.</li>
     *         <li>Disables temperature/topK/topP internally.</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>mirostatTau(double)</b> — Target entropy for Mirostat.
     *       <ul>
     *         <li>Typical range: 2.0 – 8.0</li>
     *         <li>Lower = more focused, higher = more creative</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     *  <b>C. Context, Repetition, and Determinism</b>
     *
     * <ul>
     *   <li><b>numCtx(int)</b> — Context window size (max input and output tokens).
     *       <ul>
     *         <li>Varies per model: 4k, 8k, 32k, etc.</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>numPredict(int)</b> — Max tokens to generate.
     *       <ul>
     *         <li>Default ≈ 256–512</li>
     *         <li>Controls response length</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>repeatLastN(int)</b> — Number of last tokens considered for repetition penalty.
     *       <ul>
     *         <li>Default: 64</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>repeatPenalty(double)</b> — Penalize repeated tokens.
     *       <ul>
     *         <li>Range: 1.0–2.0 (default ≈ 1.1)</li>
     *         <li>Higher = stronger anti-repetition effect</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>seed(long)</b> — Random seed for deterministic generation.
     *       <ul>
     *         <li>Same input + same seed = identical output</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     *  <b>D. Output Formatting & Stopping</b>
     *
     * <ul>
     *   <li><b>stop(List&lt;String&gt;)</b> — Stop generation when any substring matches.
     *       <ul>
     *         <li>Useful in structured chat or tool-calling mode.</li>
     *         <li>Example: stop("User:", "Assistant:")</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>responseFormat(String)</b> — Ask model to output a specific format.
     *       <ul>
     *         <li>Examples: "json", "text", "markdown"</li>
     *       </ul>
     *   </li>
     *
     *   <li><b>think(boolean)</b> / <b>returnThinking(boolean)</b> —
     *       Return or include reasoning ("chain-of-thought") tokens,
     *       if supported by the model.
     *   </li>
     * </ul>
     *
     *  <b>E. Runtime & Debugging</b>
     *
     * <ul>
     *   <li><b>timeout(Duration)</b> — Request timeout (default 30s–60s typical).</li>
     *   <li><b>customHeaders(Map&lt;String,String&gt;)</b> — Add HTTP headers for custom APIs.</li>
     *   <li><b>logRequests(boolean)</b> / <b>logResponses(boolean)</b> —
     *       Enable raw payload logging (for debugging only).</li>
     *   <li><b>supportedCapabilities(Set&lt;Capability&gt;)</b> —
     *       Declare supported model capabilities (e.g. streaming, JSON mode, tool-calling).</li>
     * </ul>
     *
     *  <b>Typical Configurations</b>
     *
     * <pre>
     * // Deterministic reasoning
     * .temperature(0.3)
     * .topP(0.9)
     * .repeatPenalty(1.1)
     * .numPredict(512)
     *
     * // Creative chat
     * .temperature(0.8)
     * .topK(50)
     * .topP(0.95)
     * .repeatPenalty(1.05)
     *
     * // Debug or test runs
     * .temperature(0.0)
     * .seed(42)
     * .numPredict(128)
     * </pre>
     *
     *  <b>Notes</b>
     *
     * <ul>
     *   <li>These parameters interact — avoid setting all at once.</li>
     *   <li>For small VRAM (≤6GB), keep <code>numCtx</code> low (2k–4k).</li>
     *   <li>For reproducibility, use <code>seed()</code> and fixed prompts.</li>
     *   <li>Mirostat is optional — best for long-form creative text.</li>
     * </ul>
     *
     */

    @Bean("_StreamingChatModel")
    public StreamingChatModel _StreamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName(_AiModel.QWEN3.getModelName())
                .temperature(0.7)
                .topK(40)
                .topP(0.9)
                .minP(0.05)
                .numCtx(4096)
                .numPredict(1024)
                .seed(2004)
                .responseFormat(ResponseFormat.TEXT) // Can be customized
                .think(true)
                .returnThinking(true)
                .timeout(Duration.ofMinutes(1))
                .logRequests(true)
                .logResponses(true)
                .listeners(List.of(new _ChatListener()))
                .build();
    }
}
