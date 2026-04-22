#!/bin/bash
# Script to run Rust AI Context Refinery
# Equivalent to run.bat for Unix/Linux/macOS systems

# Setup environment for Rust
export LIBCLANG_PATH="/usr/lib/llvm/bin"  # Adjust path as needed
export RUST_LOG=info

echo "[1/2] Loading .env variables..."
if [ -f ".env" ]; then
    while IFS= read -r line; do
        # Skip comments and empty lines
        if [[ ! "$line" =~ ^#.*$ ]] && [[ -n "$line" ]]; then
            export "$line"
            echo "Loaded: $line"
        fi
    done < .env
else
    echo ".env file not found! Skipping..."
fi

# Environment variables to prevent deadlock in Whisper
export OMP_NUM_THREADS=1
export MKL_NUM_THREADS=1

echo
echo "[RUST] Environment Ready! Building and Running..."
cargo run --release

# Check exit status
if [ $? -ne 0 ]; then
    echo "[ERROR] Rust crashed!"
    exit 1
fi