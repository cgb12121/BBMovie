use std::os::raw::c_int;
use std::path::{Path, PathBuf};
use once_cell::sync::{Lazy, OnceCell};
use num_cpus;
use anyhow::{Result, Context, anyhow};
use whisper_rs::{WhisperContext, WhisperContextParameters, FullParams, SamplingStrategy, WhisperState};
use crate::utils::decode_audio;

static WHISPER_THREADS: Lazy<c_int> = Lazy::new(|| {
    let threads: usize = num_cpus::get().max(1); // Ensure at least 1 thread
    tracing::info!("Configuring Whisper to use {} threads", threads);
    threads as c_int
});

pub trait WhisperEngine: Send + Sync {
    fn transcribe(&self, audio_path: &Path) -> Result<String>;
}

struct WhisperCppEngine {
    ctx: WhisperContext,
}

// Issue 9: Use once_cell::sync::OnceCell for robust lazy initialization
static ENGINE: OnceCell<WhisperCppEngine> = OnceCell::new();

fn get_engine() -> Result<&'static WhisperCppEngine> {
    // Issue 9: get_or_try_init ensures the init closure runs only once,
    // preventing multiple expensive model loads.
    ENGINE.get_or_try_init(load_model)
}

fn load_model() -> Result<WhisperCppEngine> {
    // Get a model path from env or fallback to a default relative path
    let model_path_str: String = std::env::var("WHISPER_MODEL_PATH")
        .unwrap_or_default();
    let model_name: String = std::env::var("WHISPER_MODEL_NAME")
        .unwrap_or_else(|_| { "ggml-base.en.bin".to_string() });
    let model_path: PathBuf = if !model_path_str.is_empty() {
        PathBuf::from(model_path_str)
    } else {
        let current_dir: PathBuf = std::env::current_dir()?;
        current_dir
            .join("models")
            .join("whisper-cpp")
            .join(model_name)
    };

    if !model_path.exists() {
        return Err(anyhow!(
            "Model not found at {:?}. Please download it first or set WHISPER_MODEL_PATH.\n\
             Run: curl -L -o {:?} https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin",
            model_path, model_path
        ));
    }

    tracing::info!("Loading Whisper model from {:?}", model_path);

    let ctx_params: WhisperContextParameters = WhisperContextParameters::default();
    let ctx: WhisperContext = WhisperContext::new_with_params(
        model_path.to_str().ok_or_else(|| anyhow!("Model path is not valid UTF-8"))?,
        ctx_params
    ).context("Failed to load Whisper model")?;

    tracing::info!("Whisper model loaded successfully");

    Ok(WhisperCppEngine { ctx })
}

impl WhisperEngine for WhisperCppEngine {
    fn transcribe(&self, audio_path: &Path) -> Result<String> {
        tracing::info!("Transcribing: {:?}", audio_path);

        // Decode audio to PCM 16kHz mono
        let pcm_data: Vec<f32> = decode_audio(audio_path)?;

        // Normalize to [-1, 1]
        let max_abs: f32 = pcm_data.iter()
            .map(|x: &f32| x.abs())
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
        let mut state: WhisperState = self.ctx.create_state()
            .context("Failed to create Whisper state")?;

        // Configure parameters
        let mut params: FullParams = FullParams::new(SamplingStrategy::Greedy {
            best_of: 1
        });

        tracing::debug!("Your cpu has {} core(s), the current code only use 4 cores", *WHISPER_THREADS);
        params.set_n_threads(4); // Replace with WHISPER_THREADS in a highland device
        params.set_translate(false);
        params.set_language(Some("en"));
        params.set_print_special(false);
        params.set_print_progress(false);
        params.set_print_realtime(false);
        params.set_print_timestamps(false);
        params.set_suppress_blank(true);
        params.set_suppress_non_speech_tokens(true);

        params.set_temperature(0.0);
        params.set_max_len(0);

        // Run inference
        state.full(params, &normalized[..])
            .context("Whisper inference failed")?;

        // Extract text from segments
        let num_segments: c_int = state.full_n_segments()
            .context("Failed to get segment count")?;

        let mut full_text: String = String::new();

        for i in 0..num_segments {
            let segment_text: String = state.full_get_segment_text(i)
                .context(format!("Failed to get segment {}", i))?;

            let trimmed: &str = segment_text.trim();
            if !trimmed.is_empty() {
                tracing::debug!("Segment {}: {}", i, trimmed);
                full_text.push_str(trimmed);
                full_text.push(' ');
            }
        }

        let result: String = full_text.trim().to_string();
        tracing::info!("Transcription complete: {} chars", result.len());

        Ok(result)
    }
}

pub fn run_whisper(path: &Path) -> Result<String> {
    let engine: &WhisperCppEngine = get_engine()?;
    engine.transcribe(path)
        .with_context(|| format!("Failed to transcribe {:?}", path))
}

pub fn eager_init() -> Result<()> {
    let _ = get_engine()?; // Call first to trigger load_model
    Ok(())
}
