use std::path::Path;
use std::sync::OnceLock;
use anyhow::{Result, Context, anyhow};
use whisper_rs::{WhisperContext, WhisperContextParameters, FullParams, SamplingStrategy};
use crate::utils::decode_audio;

pub trait WhisperEngine: Send + Sync {
    fn transcribe(&self, audio_path: &Path) -> Result<String>;
}

struct WhisperCppEngine {
    ctx: WhisperContext,
}

static ENGINE: OnceLock<WhisperCppEngine> = OnceLock::new();

fn get_engine() -> Result<&'static WhisperCppEngine> {
    if let Some(engine) = ENGINE.get() {
        return Ok(engine);
    }

    //If the engine hasn't been loaded yet, load and set it.
    // Because OnceLock::set is only called once, we need to lock the logic again or use lazy_static.
    // But the simplest way for OnceLock is to load it first and then set it.
    
    // NOTE: this might lead to race condition if called multiple times before set() is called.
    //       But using OnceLock is safe, so it's fine.
    let engine = load_model()?;
    let _ = ENGINE.set(engine); // Ignore error if already set
    
    Ok(ENGINE.get().unwrap())
}

fn load_model() -> Result<WhisperCppEngine> {
    let current_dir = std::env::current_dir()?;
    let model_path = current_dir
        .join("models")
        .join("whisper-cpp")
        .join("ggml-base.en.bin");

    if !model_path.exists() {
        return Err(anyhow!(
            "Model not found at {:?}. Please download it first.\n\
             Run: curl -L -o {:?} https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin",
            model_path, model_path
        ));
    }

    tracing::info!("🔄 Loading Whisper model from {:?}", model_path);

    let ctx_params = WhisperContextParameters::default();
    let ctx = WhisperContext::new_with_params(
        model_path.to_str().unwrap(),
        ctx_params
    ).context("Failed to load Whisper model")?;

    tracing::info!("✅ Whisper model loaded successfully");

    Ok(WhisperCppEngine { ctx })
}

impl WhisperEngine for WhisperCppEngine {
    fn transcribe(&self, audio_path: &Path) -> Result<String> {
        tracing::info!("🎧 Transcribing: {:?}", audio_path);

        // Decode audio to PCM 16kHz mono
        let pcm_data = decode_audio(audio_path)?;
        
        // Normalize to [-1, 1]
        let max_abs = pcm_data.iter()
            .map(|x| x.abs())
            .fold(0.0f32, f32::max);
        
        let normalized: Vec<f32> = if max_abs > 0.0 {
            pcm_data.iter().map(|x| x / max_abs).collect()
        } else {
            return Err(anyhow!("Audio is silent or invalid"));
        };

        tracing::debug!("Audio decoded: {} samples (~{:.2}s)", 
            normalized.len(), 
            normalized.len() as f32 / 16000.0
        );

        // Set state for inference
        let mut state = self.ctx.create_state()
            .context("Failed to create Whisper state")?;

        // Configure parameters
        let mut params = FullParams::new(SamplingStrategy::Greedy { best_of: 1 });
        
        // IMPORTANT: Whisper inference is CPU-intensive, so we want to limit the number of threads
        params.set_n_threads(4); // Use 4 threads (adjust by CPU)
        params.set_translate(false); // Transcribe, NO translate
        params.set_language(Some("en")); // Force English
        params.set_print_special(false); // Skip print special tokens
        params.set_print_progress(false); // Skip progress log spam
        params.set_print_realtime(false); // Skip realtime log
        params.set_print_timestamps(false); // Skip timestamps
        params.set_suppress_blank(true); // Skip blank tokens
        params.set_suppress_non_speech_tokens(true); // Skip noise tokens
        
        // Temperature = 0.0 cho deterministic output
        params.set_temperature(0.0);
        params.set_max_len(0); // 0 = auto

        // Run inference
        state.full(params, &normalized[..])
            .context("Whisper inference failed")?;

        // Extract text từ segments
        let num_segments = state.full_n_segments()
            .context("Failed to get segment count")?;

        let mut full_text = String::new();
        
        for i in 0..num_segments {
            let segment_text = state.full_get_segment_text(i)
                .context(format!("Failed to get segment {}", i))?;
            
            let trimmed = segment_text.trim();
            if !trimmed.is_empty() {
                tracing::debug!("Segment {}: {}", i, trimmed);
                full_text.push_str(trimmed);
                full_text.push(' ');
            }
        }

        let result = full_text.trim().to_string();
        tracing::info!("Transcription complete: {} chars", result.len());

        Ok(result)
    }
}

pub fn run_whisper(path: &Path) -> Result<String> {
    let engine = get_engine()?;
    engine.transcribe(path)
        .with_context(|| format!("Failed to transcribe {:?}", path))
}