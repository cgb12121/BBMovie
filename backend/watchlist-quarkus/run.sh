#!/bin/bash
# Script to run Watchlist Quarkus
# Equivalent to run.bat for Unix/Linux/macOS systems

# 1. Load environment variables from .env file
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

# 2. Run Quarkus application
echo
echo "[2/2] Building and Running Watchlist-Quarkus..."
mvn quarkus:dev