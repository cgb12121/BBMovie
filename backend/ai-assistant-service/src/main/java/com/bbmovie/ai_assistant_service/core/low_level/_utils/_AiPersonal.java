package com.bbmovie.ai_assistant_service.core.low_level._utils;

import lombok.Getter;

/**
 * <h4>🧠 AI MODEL OVERVIEW — Rationale for Multiple Model Selection</h4>
 *
 * <p>
 * This service integrates multiple small to mid-sized language models (1B–5B parameters)
 * to balance performance, reasoning ability, and personality nuance
 * within limited VRAM (≈6GB).
 * Each model is purpose-aligned and behavior-tuned for different tasks:
 * </p>
 *
 * * <table>
 * <thead>
 * <tr>
 * <th>Model</th>
 * <th>Fit</th>
 * <th>Personality</th>
 * <th>Reasoning</th>
 * <th>Code</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td><b>Qwen 3 0.6B</b></td>
 * <td>⚡ Ultra-light</td>
 * <td>🤖 Curious, concise</td>
 * <td>🧩 Simple reasoning</td>
 * <td>💬 Basic scripting</td>
 * </tr>
 * <tr>
 * <td><b>Qwen 3 1.7B</b></td>
 * <td>✅ Balanced</td>
 * <td>🎓 Calm, thoughtful</td>
 * <td>🧠 Strong structured thinking</td>
 * <td>🧰 Capable of code explanations</td>
 * </tr>
 * <tr>
 * <td><b>Hermes 3 3B</b></td>
 * <td>✅</td>
 * <td>💕 Charming, emotional</td>
 * <td>😊 Moderate reasoning</td>
 * <td>🧩 Weak for code, strong for roleplay</td>
 * </tr>
 * <tr>
 * <td><b>Nemotron-Mini 4B</b></td>
 * <td>✅</td>
 * <td>💬 Professional, expressive</td>
 * <td>✅ Solid reasoning</td>
 * <td>🔧 Supports function-calling / structured I/O</td>
 * </tr>
 * <tr>
 * <td><b>Llama 3.2 3B</b></td>
 * <td>✅</td>
 * <td>🦙 Neutral, logical</td>
 * <td>🧠 Excellent structured reasoning</td>
 * <td>💻 Great for code analysis / logic steps</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <h4>💡 Design Rationale</h4>
 * <ul>
 * <li><b>Performance tiers:</b> select models dynamically based on latency, VRAM, or conversational tone.</li>
 * <li><b>Behavioral specialization:</b> enables switching between emotional, logical, or technical personas.</li>
 * <li><b>Experimental layer:</b> multiple models make it easy to benchmark responses or mix reasoning chains.</li>
 * </ul>
 *
 * <h4>🧩 Example Usage</h4>
 * <ul>
 * <li>{@code Qwen3:0.6b} &rarr; quick admin replies / lightweight reasoning.</li>
 * <li>{@code Hermes3:3b} &rarr; relationship-style or empathy-driven interaction.</li>
 * <li>{@code Nemotron-Mini:4b} &rarr; structured output, JSON-based tool use.</li>
 * <li>{@code Llama3.2:3b} &rarr; analysis, debugging, or logical chain-of-thought tasks.</li>
 * </ul>
 */
@Getter
public enum _AiPersonal {
    QWEN3_MINI("qwen3:0.6b", "qwen_mini.txt"),
    QWEN3("qwen3:1.7b", "qwen.txt"),
    LLAMA3("llama3.2b:","llama.txt"),
    HERMES("hermes3:3b", "hermes.txt"),
    NEMOTRON_MINI("nemotron_mini:4b", "nemotron.txt");

    private final String modelName;
    private final String fileName;

    _AiPersonal(String modelName, String fileName) {
        this.modelName = modelName;
        this.fileName = fileName;
    }
}
