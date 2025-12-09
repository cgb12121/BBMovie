#!/bin/bash
# Script to run Search Service
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

# 2. Run Maven with optimization flags for low-resource machines
echo
echo "[2/2] Building and Running Search-Service..."
mvn spring-boot:run -DskipTests -Dspring-boot.run.jvmArguments="-Xmx512m -Dspring.output.ansi.enabled=ALWAYS"