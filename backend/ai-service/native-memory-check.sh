#!/bin/bash

# Script to detect native memory leaks in Whisper engine
# Run this while your app is running: ./native-memory-check.sh

API_URL="http://localhost:8080/api/whisper/admin"
INTERVAL=5  # seconds

echo "=== Whisper Native Memory Leak Detector ==="
echo "Monitoring: $API_URL"
echo "Interval: ${INTERVAL}s"
echo ""
echo "Watch for these signs of native memory leak:"
echo "  - 'Native Memory' keeps growing"
echo "  - 'Contexts In Use' is 0 but memory doesn't drop"
echo "  - After GC, native memory doesn't decrease"
echo ""

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

    # Get status
    STATUS=$(curl -s "$API_URL/status")
    MEMORY=$(curl -s "$API_URL/memory")

    if [ -z "$STATUS" ] || [ -z "$MEMORY" ]; then
        echo "[$TIMESTAMP] ERROR: Failed to fetch metrics"
        sleep $INTERVAL
        continue
    fi

    # Parse JSON (requires jq)
    POOL_SIZE=$(echo "$STATUS" | jq -r '.poolSize')
    CONTEXTS_IN_USE=$(echo "$STATUS" | jq -r '.contextsInUse')
    QUEUED=$(echo "$STATUS" | jq -r '.queuedTasks')
    TOTAL_PROCESSED=$(echo "$STATUS" | jq -r '.totalProcessed')
    TOTAL_REJECTED=$(echo "$STATUS" | jq -r '.totalRejected')

    HEAP_USED=$(echo "$MEMORY" | jq -r '.heapUsedMB')
    HEAP_MAX=$(echo "$MEMORY" | jq -r '.heapMaxMB')
    NATIVE_MEM=$(echo "$MEMORY" | jq -r '.directMemoryUsedMB')

    # Calculate metrics
    HEAP_PCT=$(echo "scale=1; $HEAP_USED * 100 / $HEAP_MAX" | bc)
    CTX_UTIL_PCT=$(echo "scale=1; $CONTEXTS_IN_USE * 100 / $POOL_SIZE" | bc)

    # Display
    echo "[$TIMESTAMP]"
    echo "  Engine: $POOL_SIZE contexts, $CONTEXTS_IN_USE in use (${CTX_UTIL_PCT}%)"
    echo "  Queue: $QUEUED tasks | Processed: $TOTAL_PROCESSED | Rejected: $TOTAL_REJECTED"
    echo "  Heap: ${HEAP_USED}MB / ${HEAP_MAX}MB (${HEAP_PCT}%)"
    echo "  Native Memory (Direct): ${NATIVE_MEM}MB"

    # Leak detection heuristics
    if [ "$NATIVE_MEM" -gt 500 ] && [ "$CONTEXTS_IN_USE" -eq 0 ]; then
        echo "      WARNING: High native memory with no active contexts!"
        echo "      This may indicate a native memory leak."
    fi

    if [ "$TOTAL_REJECTED" -gt 100 ]; then
        echo "      WARNING: High rejection rate. Consider scaling."
    fi

    echo ""

    sleep $INTERVAL
done
