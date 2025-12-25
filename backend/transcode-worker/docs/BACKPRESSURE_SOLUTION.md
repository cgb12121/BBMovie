# Backpressure Solution: Internal Queue Pattern

## 🎯 Problem Statement

**Current Issue:**
- NATS Consumer receives message → submits to `workerExecutor`
- `processRecord()` calls `scheduler.acquire()` → **BLOCKS** if no capacity
- NATS message is **NOT ACKed** → NATS timeout → **Redelivery Loop** 🔄

**Scenario:**
1. File 4K arrives → Needs 100% capacity → Blocks scheduler
2. New file arrives → NATS sends message → Consumer receives
3. Consumer submits to worker → Worker calls `scheduler.acquire()` → **BLOCKS**
4. NATS waits for ACK → Timeout (30s) → **Redelivers message**
5. Loop continues → NATS spam → System overload

---

## ✅ Solution: Internal Queue Pattern

### Architecture

```
┌─────────────┐
│ NATS Server │
└──────┬──────┘
       │ Message
       ▼
┌─────────────────────────────────┐
│ NATS Consumer (Fast ACK)        │
│ - Parse message                 │
│ - Create TranscodeJob           │
│ - Add to BlockingQueue          │
│ - ACK immediately ✅            │
└──────┬──────────────────────────┘
       │ Job
       ▼
┌─────────────────────────────────┐
│ BlockingQueue<TranscodeJob>     │
│ - Capacity: 10 (configurable)   │
│ - Backpressure: NAK if full     │
└──────┬──────────────────────────┘
       │ Job
       ▼
┌─────────────────────────────────┐
│ Worker Threads (Pool)           │
│ - Take job from queue           │
│ - Call scheduler.acquire()     │
│   (Can block here - OK!)        │
│ - Process transcode             │
│ - Release scheduler             │
└─────────────────────────────────┘
```

### Benefits

1. **NATS Never Blocks**: Consumer ACKs immediately
2. **No Redelivery Loop**: Message is ACKed before processing
3. **Backpressure Control**: Queue full → NAK → NATS retries later
4. **Resource Management**: Scheduler controls actual processing
5. **Scalable**: Can adjust worker threads independently

---

## 📋 Implementation Plan

### Step 1: Create TranscodeJob DTO

```java
public record TranscodeJob(
    String uploadId,
    String bucket,
    String key,
    UploadPurpose purpose,
    JsonNode record
) {}
```

### Step 2: Create TranscodeJobQueue Service

```java
@Service
public class TranscodeJobQueue {
    private final BlockingQueue<TranscodeJob> queue;
    
    public TranscodeJobQueue(@Value("${app.transcode.queue.capacity:10}") int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    public boolean offer(TranscodeJob job) {
        return queue.offer(job); // Non-blocking
    }
    
    public TranscodeJob take() throws InterruptedException {
        return queue.take(); // Blocking
    }
    
    public int size() {
        return queue.size();
    }
}
```

### Step 3: Refactor MediaEventConsumer

**Before:**
```java
private void handleMessage(Message msg) {
    // Parse and submit to workerExecutor
    workerExecutor.submit(() -> processRecord(record));
    // ❌ No ACK!
}
```

**After:**
```java
private void handleMessage(Message msg) {
    try {
        // 1. Parse message
        JsonNode rootNode = parseMessage(msg);
        
        // 2. Create jobs and add to queue
        for (JsonNode record : extractRecords(rootNode)) {
            TranscodeJob job = createJob(record);
            
            // 3. Try to add to queue (non-blocking)
            if (jobQueue.offer(job)) {
                log.info("Job queued: {}", job.uploadId());
            } else {
                // Queue full → NAK → NATS will retry later
                log.warn("Queue full, rejecting job: {}", job.uploadId());
                msg.nak(); // Negative ACK
                return;
            }
        }
        
        // 4. ACK immediately (before processing!)
        msg.ack();
        
    } catch (Exception e) {
        log.error("Error handling message", e);
        msg.nak(); // Retry on error
    }
}
```

### Step 4: Create Worker Thread Pool

```java
@PostConstruct
public void startWorkers() {
    int workerThreads = calculateWorkerThreads();
    
    for (int i = 0; i < workerThreads; i++) {
        workerExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Block until job available
                    TranscodeJob job = jobQueue.take();
                    
                    // Process job (can block at scheduler.acquire())
                    processJob(job);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error processing job", e);
                }
            }
        });
    }
}
```

---

## ⚙️ Configuration

```properties
# Queue capacity (backpressure control)
app.transcode.queue.capacity=10

# Worker threads (auto-calculated or manual)
app.transcode.worker.threads=0  # 0 = auto (maxCapacity)
```

### Auto-calculation Logic

```java
int calculateWorkerThreads() {
    if (configuredThreads > 0) {
        return configuredThreads;
    }
    // Default: Same as maxCapacity (can process max concurrent jobs)
    return scheduler.getMaxCapacity();
}
```

---

## 🔄 Flow Comparison

### Before (Current - Problematic)

```
NATS → Consumer → workerExecutor → processRecord()
                              ↓
                    scheduler.acquire() [BLOCKS]
                              ↓
                    [NATS waiting for ACK...]
                              ↓
                    [Timeout] → Redelivery Loop
```

### After (Solution)

```
NATS → Consumer → Queue → ACK ✅
              ↓
         Worker Thread
              ↓
    scheduler.acquire() [BLOCKS - OK!]
              ↓
         Process...
```

---

## 🛡️ Error Handling

### Queue Full Scenario

```java
if (!jobQueue.offer(job)) {
    // Queue full → Backpressure
    log.warn("Queue full (size: {}), rejecting job: {}", 
             jobQueue.size(), job.uploadId());
    msg.nak(); // Negative ACK → NATS will retry later
    return;
}
```

### Worker Crash Scenario

**Problem**: If worker crashes, job in queue is lost (already ACKed)

**Solution Options:**
1. **Acceptable for now**: Jobs are idempotent (can be reprocessed)
2. **Future enhancement**: Persist jobs to DB before ACK

---

## 📊 Monitoring

### Metrics to Track

1. **Queue Size**: `jobQueue.size()`
2. **Queue Full Events**: Count of NAKs due to full queue
3. **Worker Threads**: Active worker count
4. **Scheduler Usage**: Current capacity usage

### Logging

```java
log.info("Queue status - Size: {}/{}, Workers: {}, Scheduler: {}/{}",
    jobQueue.size(), capacity, activeWorkers, 
    scheduler.getCurrentUsage(), scheduler.getMaxCapacity());
```

---

## ✅ Benefits Summary

1. ✅ **No NATS Blocking**: Consumer ACKs immediately
2. ✅ **No Redelivery Loop**: Messages are ACKed before processing
3. ✅ **Backpressure Control**: Queue full → NAK → Controlled retry
4. ✅ **Resource Management**: Scheduler still controls actual processing
5. ✅ **Scalable**: Independent control of queue size and worker threads
6. ✅ **Simple**: Minimal code changes, clear separation of concerns

---

## 🚀 Next Steps

1. Implement `TranscodeJob` DTO
2. Create `TranscodeJobQueue` service
3. Refactor `MediaEventConsumer` to use queue
4. Create worker thread pool
5. Add configuration properties
6. Test with large files (4K) and concurrent messages

