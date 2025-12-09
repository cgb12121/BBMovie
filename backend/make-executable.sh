#!/bin/bash
# Make all bash scripts executable
# Run this script to set proper permissions on Unix/Linux/macOS systems

chmod +x launcher.sh
chmod +x eureka-sever/run.sh
chmod +x auth-service/run.sh
chmod +x gateway/run.sh
chmod +x file-service/run.sh
chmod +x ai-service/run.sh
chmod +x watchlist-quarkus/run.sh
chmod +x payment-service/run.sh
chmod +x search-service/run.sh
chmod +x email-service/run.sh
chmod +x rust-ai-context-refinery/run.sh

echo "All .sh files are now executable!"