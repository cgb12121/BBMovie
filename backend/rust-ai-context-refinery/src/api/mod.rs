pub mod process; // New module for generic batch processing

// New Architecture URL-based handlers - now generic batch
pub use process::handle_batch_process;