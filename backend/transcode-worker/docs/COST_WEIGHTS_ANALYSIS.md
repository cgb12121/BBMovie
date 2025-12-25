# Cost Weights Analysis & Optimization

## 🎯 Objective
Optimize cost weights for video transcoding to balance:
- **CPU Usage**: Stay below 80% threshold
- **Performance**: Maximize parallel processing efficiency
- **Cost**: Minimize VPS/Cloud expenses (high-end CPU is expensive)
- **Stability**: Prevent CPU spikes and system overload

---

## 📊 Hardware Configuration

```
CPU: Intel i7 Gen 13 K
Physical Cores: 10
Logical Processors: 16
Max Capacity: 8 slots (80% of 10 cores)
```

---

## 🧪 Real-World Test Results

### Test Case: 360p Video (1 hour duration)

**Sequential Processing (Old Code):**
- 360p + 240p + 144p: **5-10 minutes**
- CPU: Sequential, one at a time

**Concurrent Processing (New Code):**
- 360p + 240p + 144p: **2 minutes** (2.5-5x faster!)
- CPU: Parallel, all 3 resolutions simultaneously

**Cost Analysis:**
- 360p: 2 points
- 240p: 1 point
- 144p: 1 point
- **Total: 4 points (50% capacity)**
- **Result: Efficient, fast, CPU well-utilized**

### CPU Spike Observation
- **720p**: High CPU spike observed
- Requires higher cost weight to prevent overload
- Cannot run too many 720p concurrently

---

## 💰 Cost Weights Design (Optimized)

### Current Configuration (Max Capacity = 8 slots)

| Resolution | Cost | % Capacity | Concurrent Limit | Use Case |
|------------|------|------------|------------------|----------|
| **1080p** | 6 | 75% | 1 (with 1x 240p/144p) | High quality, CPU intensive |
| **720p** | 4 | 50% | 2 max | Standard HD, CPU spike |
| **480p** | 2 | 25% | 4 max | Medium quality |
| **360p** | 2 | 25% | 4 max | Low-medium quality |
| **240p** | 1 | 12.5% | 8 max | Low quality |
| **144p** | 1 | 12.5% | 8 max | Minimum quality |

### Production Configuration (Max Capacity = 8-10 slots)

| Resolution | Cost | % Capacity | Notes |
|------------|------|------------|-------|
| **1080p** | 7 | 87.5% | Slightly higher for production |
| **720p** | 5 | 62.5% | Account for production load |
| **480p** | 2 | 25% | Same as dev |
| **360p** | 2 | 25% | Same as dev |
| **240p** | 1 | 12.5% | Same as dev |
| **144p** | 1 | 12.5% | Same as dev |

---

## 📈 Scenario Analysis

### Scenario 1: Single 1080p Video
```
1080p (6) = 75% capacity
Remaining: 2 slots (25%)
Can add: 1x 240p or 1x 144p
Result: ✅ Efficient, fast processing
```

### Scenario 2: Multiple Resolutions (Common Case)
```
360p (2) + 240p (1) + 144p (1) = 4 points (50% capacity)
Remaining: 4 slots (50%)
Can add: 2x 480p or 1x 720p
Result: ✅ Very efficient, 2-5x faster than sequential
```

### Scenario 3: High-Resolution Video
```
1080p (6) + 720p (4) = 10 points
Problem: Exceeds max capacity (8)
Solution: Scheduler queues 720p until 1080p completes
Result: ✅ Prevents overload, processes sequentially
```

### Scenario 4: Multiple 720p Videos
```
2x 720p (4+4) = 8 points (100% capacity)
Result: ✅ Maximum utilization, no overload
```

### Scenario 5: Low-Quality Batch
```
4x 480p (2+2+2+2) = 8 points (100% capacity)
Result: ✅ Maximum throughput for medium quality
```

---

## 🎛️ Dynamic Capacity Adjustment (Future Enhancement)

### Current: Static Capacity
```properties
app.transcode.max-capacity=8
```

### Proposed: Dynamic Capacity
```properties
# Base capacity (80% of cores)
app.transcode.max-capacity=8

# Dynamic adjustment based on actual CPU usage
app.transcode.dynamic-capacity.enabled=true
app.transcode.dynamic-capacity.min-capacity=6  # Minimum (60%)
app.transcode.dynamic-capacity.max-capacity=10 # Maximum (100%)
app.transcode.dynamic-capacity.cpu-threshold-high=85  # Reduce capacity if CPU > 85%
app.transcode.dynamic-capacity.cpu-threshold-low=70   # Increase capacity if CPU < 70%
```

### Benefits
- **Adaptive**: Adjusts based on actual CPU usage
- **Cost-effective**: Uses more capacity when available
- **Safe**: Reduces capacity when CPU spikes
- **Efficient**: Maximizes throughput without overload

---

## 💡 Cost Optimization Tips

### 1. **Prioritize Lower Resolutions**
- Process 360p/240p/144p first (low cost, fast)
- Then process 480p/720p (medium cost)
- Finally process 1080p (high cost)

### 2. **Batch Processing**
- Group similar resolutions together
- Maximize concurrent processing
- Reduce total processing time

### 3. **Monitor CPU Usage**
- Watch for CPU spikes (especially 720p)
- Adjust capacity if needed
- Consider reducing 720p cost if spikes are too high

### 4. **Production vs Development**
- Production: Slightly higher costs (more headroom)
- Development: Lower costs (tighter control)
- Adjust based on actual server performance

---

## 🔧 Configuration Recommendations

### For i7 Gen 13 K (10 cores, 16 logical processors)

**Development:**
```properties
app.transcode.max-capacity=8
# Cost weights: 1080p=6, 720p=4, 480p=2, 360p=2, 240p=1, 144p=1
```

**Production:**
```properties
app.transcode.max-capacity=8
# Cost weights: 1080p=7, 720p=5, 480p=2, 360p=2, 240p=1, 144p=1
```

### For Lower-End CPUs (4-6 cores)

**Adjust max capacity:**
```properties
# 4 cores: 80% = 3.2 → 3 slots
app.transcode.max-capacity=3

# 6 cores: 80% = 4.8 → 4 slots
app.transcode.max-capacity=4
```

**Adjust cost weights proportionally:**
- 1080p: 3-4 points (instead of 6)
- 720p: 2-3 points (instead of 4)
- 480p: 1 point (instead of 2)
- Others: 1 point (same)

---

## 📊 Performance Comparison

### Sequential (Old Code)
```
360p + 240p + 144p: 5-10 minutes
CPU: Sequential, underutilized
```

### Concurrent (New Code)
```
360p + 240p + 144p: 2 minutes
CPU: Parallel, 50% utilization
Speedup: 2.5-5x faster
```

### Cost Efficiency
- **Old**: 1 resolution at a time, slow
- **New**: Multiple resolutions, fast
- **Savings**: 2.5-5x faster = 2.5-5x cost reduction per video

---

## ⚠️ Important Notes

1. **720p CPU Spike**: Monitor closely, may need to increase cost if spikes are too high
2. **1080p is Heavy**: Uses 75% capacity, plan accordingly
3. **Lower Resolutions are Efficient**: 360p/240p/144p can run many concurrently
4. **Dynamic Adjustment**: Consider implementing for better resource utilization
5. **Cost vs Performance**: Balance between speed and CPU usage

---

## 🚀 Future Enhancements

1. **Dynamic Capacity**: Adjust based on actual CPU usage
2. **Priority Queue**: Process lower resolutions first
3. **Metrics Export**: Monitor CPU usage per resolution
4. **Adaptive Costs**: Adjust weights based on historical data
5. **Multi-Node Support**: Distributed transcoding

