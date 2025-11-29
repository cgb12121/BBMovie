# Get process PID
PID=$(jps | grep whisper-app | awk '{print $1}')

# Baseline
jcmd $PID VM.native_memory baseline

# Wait 10 minutes with heavy load
sleep 600

# Compare
jcmd $PID VM.native_memory summary.diff