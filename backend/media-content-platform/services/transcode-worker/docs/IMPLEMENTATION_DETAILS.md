# Transcode Worker - Parallel Processing Implementation Details

## 🎯 Overview

Implementation của **Parallel Processing với Resource Management** cho Transcode Worker, đảm bảo CPU usage không vượt quá **80%** và xử lý song song các resolution với weighted semaphore.

---

## 🏗️ Architecture

### 1. **TranscodeScheduler** (Resource Manager)
- **Cơ chế**: Weighted Semaphore với fair ordering
- **Max Capacity**: 8 slots (80% của 10 CPU cores)
- **Chức năng**:
  - Acquire/release resources dựa trên cost weight của resolution
  - Track usage và log real-time
  - Prevent resource leaks với ResourceHandle pattern

### 2. **ResolutionCostCalculator** (Cost Calculator)
- **Cost Weights**:
  - **1080p**: 8 points (max, trừ production = 10)
  - **720p**: 4 points
  - **480p**: 2 points
  - **Others** (360p, 240p, 144p, original): 1 point
- **Profile-aware**: Production profile có thể handle cost cao hơn

### 3. **VideoTranscoderService** (Enhanced)
- **Parallel Processing**: Sử dụng `CompletableFuture` với virtual threads
- **Thread Safety**: Mỗi transcoding job có FFmpegExecutor riêng
- **Resource Management**: Wrap mỗi job với scheduler acquire/release

---

## 📊 Resource Calculation

### CPU Configuration
```
Device: CPU0
Physical Cores: 10
Logical Processors: 16
Target CPU Usage: 80%
```

### Capacity Calculation
```
Max Capacity = 10 cores × 80% = 8 slots
```

### Example Scenarios

#### Scenario 1: Single 1080p Video
```
Resolution: 1080p (cost: 8)
Available: 8 slots
Result: ✅ Can process (uses 100% capacity)
```

#### Scenario 2: Multiple Resolutions
```
Video has: 1080p, 720p, 480p, 360p
Costs: 8 + 4 + 2 + 1 = 15 points
Available: 8 slots
Result: ⚠️ Will queue and process sequentially based on availability
```

#### Scenario 3: Parallel Videos
```
Video 1: 1080p (8) + 720p (4) = 12 points
Video 2: 720p (4) + 480p (2) = 6 points
Total: 18 points
Available: 8 slots
Result: Scheduler will manage queuing and parallel execution
```

---

## 🔄 Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│  MediaEventConsumer (NATS Listener)                     │
│  - Receives transcode event                             │
│  - Offloads to virtual thread executor                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  VideoTranscoderService.transcode()                     │
│  1. Extract metadata                                    │
│  2. Generate master encryption keys                     │
│  3. Determine target resolutions                        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Parallel Processing (CompletableFuture)                │
│                                                          │
│  For each resolution:                                   │
│  ┌──────────────────────────────────────────────┐      │
│  │ 1. Calculate cost weight                     │      │
│  │ 2. scheduler.acquire(costWeight)             │      │
│  │    - Blocks if insufficient resources        │      │
│  │ 3. Create FFmpegExecutor (per task)          │      │
│  │ 4. Execute transcoding                       │      │
│  │ 5. scheduler.release(handle) [finally]       │      │
│  └──────────────────────────────────────────────┘      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Wait for all futures to complete                       │
│  Create master playlist                                 │
└─────────────────────────────────────────────────────────┘
```

---

## 🛡️ Preserved Features

### ✅ Existing Bug Fixes (Maintained)
1. **Virtual Thread Executor**: `Executors.newVirtualThreadPerTaskExecutor()` cho MediaEventConsumer
2. **Robust Cleanup**: `try-finally` với `FileUtils.deleteDirectory`
3. **Tika/ClamAV Resilience**: Profile-based bypass và error handling
4. **Path Safety**: Safe temp file names (`source.mp4`)
5. **Encryption**: Manual static key generation preserved và thread-safe

### ✅ Thread Safety
- Mỗi transcoding job có **FFmpegExecutor riêng** (không shared)
- **ResourceHandle** pattern đảm bảo release trong finally block
- **AtomicInteger** cho usage tracking

---

## 📝 Configuration

### application.properties
```properties
# Transcode Scheduler Configuration
# Max capacity = 8 slots (80% of 10 CPU cores)
app.transcode.max-capacity=8
```

### Environment-based Tuning
- **Development**: `max-capacity=8` (conservative)
- **Production**: Có thể tăng lên 10-12 nếu server mạnh hơn
- **Cost weights**: Production có thể handle 1080p = 10 points

---

## 🔍 Monitoring & Logging

### Key Log Messages
```
✅ Acquired X resource slots (total usage: Y/Z, W%)
🔓 Released X resource slots (total usage: Y/Z, W%)
[1080p] Starting transcoding (cost: 8 slots)
[1080p] Completed transcoding successfully
```

### Metrics to Watch
- **CPU Usage**: Should stay below 80%
- **Resource Slots**: Current usage vs max capacity
- **Queue Depth**: Number of waiting tasks
- **Transcoding Time**: Per resolution

---

## 🧪 Testing Scenarios

### Test 1: Single High-Resolution Video
```bash
# Upload 1080p video
# Expected: Uses 8/8 slots, processes immediately
```

### Test 2: Multiple Concurrent Videos
```bash
# Upload 3 videos simultaneously
# Expected: Scheduler queues and processes based on availability
```

### Test 3: Mixed Resolutions
```bash
# Video with 1080p, 720p, 480p, 360p
# Expected: Parallel processing with resource constraints
```

### Test 4: Resource Exhaustion
```bash
# Upload multiple 1080p videos
# Expected: Queuing, no "Rejected execution" errors
```

---

## ⚠️ Important Notes

1. **FFmpegExecutor**: Created per task để đảm bảo thread safety
2. **ResourceHandle**: **MUST** be released in finally block
3. **Cost Weights**: Có thể adjust dựa trên thực tế CPU usage
4. **Max Capacity**: Có thể tune dựa trên server specs
5. **Virtual Threads**: Sử dụng Java 21+ virtual threads cho scalability

---

## 🚀 Future Enhancements

1. **Dynamic Capacity**: Adjust based on actual CPU usage
2. **Priority Queue**: Prioritize certain resolutions
3. **Metrics Export**: Prometheus/Grafana integration
4. **Adaptive Cost**: Adjust weights based on video complexity
5. **Multi-Node Support**: Distributed resource management

