pub mod process; // New module for generic batch processing
pub mod nats_worker;

// New Architecture URL-based handlers - now generic batch
pub use process::handle_batch_process;