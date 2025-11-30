#!/bin/bash
# ============================================
# File: run-jooq-codegen.sh
# ============================================

set -e  # Exit on error

echo "=== JOOQ Codegen Runner ==="

# Check for config file
CONFIG_FILE="${HOME}/.m2/jooq-codegen-dev.properties"

if [ ! -f "$CONFIG_FILE" ]; then
    echo " Error: Config file not found: $CONFIG_FILE"
    echo ""
    echo "Create the config file with your credentials:"
    echo ""
    echo "cat > $CONFIG_FILE << 'EOF'"
    echo "db.driver=com.mysql.cj.jdbc.Driver"
    echo "db.url=jdbc:mysql://localhost:3306/bbmovie_ai_chat?useSSL=false&serverTimezone=UTC"
    echo "db.username=YOUR_USERNAME"
    echo "db.password=YOUR_PASSWORD"
    echo "EOF"
    echo ""
    echo "chmod 600 $CONFIG_FILE"
    exit 1
fi

# Secure file permissions
chmod 600 "$CONFIG_FILE"

echo " Config file found: $CONFIG_FILE"
echo ""

# Parse arguments
PROFILE="dev"
CLEAN="true"

while [[ $# -gt 0 ]]; do
    case $1 in
        --profile|-p)
            PROFILE="$2"
            shift 2
            ;;
        --no-clean)
            CLEAN="false"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -p, --profile PROFILE   Maven profile to use (dev, ci, prod)"
            echo "  --no-clean              Skip 'clean' phase"
            echo "  -h, --help              Show this help"
            echo ""
            echo "Examples:"
            echo "  $0                      # Run with dev profile"
            echo "  $0 -p prod              # Run with prod profile"
            echo "  $0 --no-clean           # Skip clean phase"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "Profile: $PROFILE"
echo "Clean: $CLEAN"
echo ""

# Build Maven command
MVN_CMD="mvn"
if [ "$CLEAN" = "true" ]; then
    MVN_CMD="$MVN_CMD clean"
fi
MVN_CMD="$MVN_CMD generate-sources -P$PROFILE"

echo "=== Running Maven ==="
echo "Command: $MVN_CMD"
echo ""

$MVN_CMD

echo ""
echo " JOOQ Codegen completed successfully!"