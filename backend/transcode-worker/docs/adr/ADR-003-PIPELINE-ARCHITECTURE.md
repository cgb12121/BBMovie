# ADR-003: 3-Stage Pipeline Architecture

## Status
**Accepted** - 2025-12-25

## Context

### Vấn đề ban đầu: Chicken-and-Egg Problem

Từ ADR-002, chúng ta đã xác định vấn đề cốt lõi:
- Để biết COST → cần download và analyze video
- Để quyết định có nên fetch không → cần biết COST

### Giải pháp đề xuất ban đầu: Sequential Consumer

Claude đề xuất "Single-threaded consumer" để giải quyết:
1. Block consumer thread
2. Fetch 1 message
3. Download + FFprobe
4. Calculate cost
5. Acquire semaphore
6. Dispatch async

### Vấn đề: HEAD-OF-LINE BLOCKING

Sau khi được review bởi AI Council (Grok, DeepSeek, Qwen, ChatGPT, Gemini, Kimi, ZAI), phát hiện vấn đề nghiêm trọng:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    HEAD-OF-LINE BLOCKING PROBLEM                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  NATS Queue: [Job A (4K), Job B (144p), Job C (144p)]                   │
│                                                                          │
│  Với SERIAL CONSUMER:                                                    │
│  1. Consumer fetch Job A (4K)                                            │
│  2. Consumer BLOCK để probe (2-3s qua network)                          │
│  3. Trong lúc đó, Job B + C PHẢI CHỜ dù:                                │
│     - B chỉ cần 1 slot (144p)                                           │
│     - System còn 13 slot trống                                          │
│     - B có thể chạy NGAY nhưng bị block bởi A                          │
│  4. ❌ CPU RẢNH trong khi chờ I/O của A                                 │
│                                                                          │
│  👉 "Xe container 4K chặn đường, xe máy 144p không qua được"            │
└─────────────────────────────────────────────────────────────────────────┘
```

### Kết quả bỏ phiếu hội đồng

| AI | Verdict | Lý do |
|----|---------|-------|
| Grok | ✅ Đồng ý (refine) | Đúng hướng, cần pipe stream |
| DeepSeek | ⚠️ Nửa đồng ý | Trade-off lớn, traffic thấp OK |
| Qwen | ❌ Phản đối | "Lỗi thiết kế nghiêm trọng" |
| ChatGPT | ❌ Phản đối | Head-of-Line Blocking |
| Gemini | ❌ Phản đối | Dùng "Chiến thuật Sân Bay" |
| Kimi | ❌ Phản đối | Nút thắt chai |
| ZAI | ❌ Phản đối | ANTI-PATTERN |

**Kết quả: 1/1/5 (Đồng ý/Nửa đồng ý/Phản đối)**

## Diagrams

### So sánh Architecture cũ vs mới

| Old Architecture | New Architecture |
|------------------|------------------|
| ![Old Flow](../img/OLD_TRANSCODE_FLOW.svg) | ![New Flow](../img/NEW_FLOW.svg) |

**Xem chi tiết tại:** [ADR-004: Class Refactoring](./ADR-004-CLASS-REFACTORING-FOR-PIPELINE.md)

## Decision

Chấp nhận **3-Stage Pipeline Architecture** được đa số hội đồng đề xuất.

### Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     3-STAGE PIPELINE ARCHITECTURE                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ STAGE 1: FETCHER (Fast, Non-blocking)                              │  │
│  │                                                                     │  │
│  │  - Fetch messages từ NATS (batch 5-10)                             │  │
│  │  - Đẩy vào Internal Queue (BlockingQueue)                          │  │
│  │  - KHÔNG xử lý gì nặng                                             │  │
│  │  - Backpressure: chỉ fetch khi queue.size() < threshold            │  │
│  │  - 1 Virtual Thread                                                │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                              │                                           │
│                              ▼                                           │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ STAGE 2: PROBER/PLANNER (Parallel, I/O-bound)                      │  │
│  │                                                                     │  │
│  │  - N virtual threads (10-20) chạy SONG SONG                        │  │
│  │  - Lấy message từ Internal Queue                                   │  │
│  │  - Probe metadata:                                                 │  │
│  │    * Option A: Presigned URL + ffprobe (không download)            │  │
│  │    * Option B: Partial download (1-10MB) + ffprobe                 │  │
│  │    * Option C: Pipe MinIO stream vào ffprobe stdin                 │  │
│  │  - Calculate exact cost                                            │  │
│  │  - tryAcquire(cost, timeout):                                      │  │
│  │    ✓ Success → đẩy sang Stage 3 queue                             │  │
│  │    ✗ Fail → NAK với delay (NATS redeliver sau 30s)                │  │
│  │                                                                     │  │
│  │  ⚡ KEY: Job B (144p) có thể "vượt" Job A (4K) nếu A đang probe    │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                              │                                           │
│                              ▼                                           │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ STAGE 3: EXECUTOR (CPU-bound, Scheduler-controlled)                │  │
│  │                                                                     │  │
│  │  - Virtual Threads (hoặc Fixed Thread Pool)                        │  │
│  │  - Start heartbeat cho NATS message                                │  │
│  │  - Download full file (đã acquire slot)                            │  │
│  │  - Validate (Tika, ClamAV)                                         │  │
│  │  - Transcode video                                                 │  │
│  │  - Upload results                                                  │  │
│  │  - Release semaphore                                               │  │
│  │  - ACK message                                                     │  │
│  │  - Stop heartbeat                                                  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Key Components

#### 1. Internal Queues

```java
// Queue giữa Stage 1 và Stage 2
private final BlockingQueue<Message> probeQueue = new LinkedBlockingQueue<>(100);

// Queue giữa Stage 2 và Stage 3 (hoặc dùng Spring Event)
private final BlockingQueue<TranscodeTask> executeQueue = new LinkedBlockingQueue<>(50);
```

#### 2. FFprobe Options (theo thứ tự ưu tiên)

| Option | Pros | Cons | Latency |
|--------|------|------|---------|
| **Presigned URL** | Không download, nhanh nhất | Cần MinIO hỗ trợ range request | ~40-100ms |
| **Pipe Stream** | Không lưu file, memory efficient | Complex setup | ~100-500ms |
| **Partial Download** | Đơn giản, reliable | Tốn disk I/O | ~500-1000ms |

```java
// Option A: Presigned URL (Recommended by Gemini)
String url = minioClient.getPresignedObjectUrl(...);
ProcessBuilder pb = new ProcessBuilder(
    "ffprobe", "-v", "error",
    "-select_streams", "v:0",
    "-show_entries", "stream=width,height",
    "-of", "json",
    url
);

// Option B: Pipe Stream (Recommended by Grok)
try (InputStream stream = minioClient.getObject(...)) {
    ProcessBuilder pb = new ProcessBuilder(
        "ffprobe", "-v", "error", "-i", "pipe:0", ...
    );
    Process p = pb.start();
    stream.transferTo(p.getOutputStream());
}

// Option C: Partial Download (Simplest)
byte[] header = downloadPartial(bucket, key, 10 * 1024 * 1024); // 10MB
Path tempFile = Files.createTempFile("probe", ".tmp");
Files.write(tempFile, header);
// ffprobe tempFile
```

#### 3. tryAcquire with Timeout

```java
/**
 * Non-blocking acquire với timeout.
 * Trả về Optional.empty() nếu không đủ capacity trong timeout.
 */
public Optional<ResourceHandle> tryAcquire(int cost, Duration timeout) {
    try {
        int actualCost = Math.min(cost, maxCapacity);
        boolean acquired = semaphore.tryAcquire(actualCost, timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (acquired) {
            currentUsage.addAndGet(actualCost);
            return Optional.of(new ResourceHandle(cost, actualCost));
        }
        return Optional.empty();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return Optional.empty();
    }
}
```

### Flow Diagram

```
Message arrives at NATS
        │
        ▼
┌───────────────────┐
│ STAGE 1: FETCHER  │
│ fetch() + offer() │
└─────────┬─────────┘
          │
          ▼
    [probeQueue]
          │
    ┌─────┼─────┐
    │     │     │
    ▼     ▼     ▼
┌─────┐┌─────┐┌─────┐
│ P1  ││ P2  ││ P3  │  STAGE 2: PROBERS (Parallel)
└──┬──┘└──┬──┘└──┬──┘
   │      │      │
   │  tryAcquire()
   │      │      │
   ▼      ▼      ▼
Success? ─┬─ No ──► NAK(delay=30s) ──► NATS redeliver
          │
         Yes
          │
          ▼
   [executeQueue]
          │
    ┌─────┼─────┐
    │     │     │
    ▼     ▼     ▼
┌─────┐┌─────┐┌─────┐
│ E1  ││ E2  ││ E3  │  STAGE 3: EXECUTORS
└──┬──┘└──┬──┘└──┬──┘
   │      │      │
   ▼      ▼      ▼
  Transcode + ACK + Release
```

## Consequences

### ✅ Advantages

| Aspect | Benefit |
|--------|---------|
| **No Head-of-Line Blocking** | Job 144p có thể "vượt" Job 4K đang probe |
| **High Throughput** | 50-200 jobs/phút thay vì 2-10 jobs/phút |
| **CPU Utilization** | 80-95% thay vì 5-30% |
| **Solves Chicken-and-Egg** | Probe TRƯỚC khi acquire |
| **Scalable** | N probers có thể scale independent |
| **Fairness** | Scheduler vẫn hoạt động đúng |
| **Natural Backpressure** | Queue size limits + NAK delay |

### ⚠️ Trade-offs

| Aspect | Trade-off | Mitigation |
|--------|-----------|------------|
| **Complexity** | Thêm 2 queues, 3 stages | Clean separation of concerns |
| **Memory** | Messages trong queues | Bounded queues (100, 50) |
| **NAK overhead** | Jobs bị reject phải redeliver | Delay 30s giảm spam |
| **Probe time** | Vẫn tốn I/O cho probe | Parallel probing amortizes |

### So sánh với Sequential Consumer (đề xuất cũ)

| Tiêu chí | Sequential (Rejected) | Pipeline (Accepted) |
|----------|----------------------|---------------------|
| **Throughput** | 2-10 jobs/phút | 50-200 jobs/phút |
| **Head-of-Line** | ❌ 4K block 144p | ✅ 144p vượt được |
| **CPU Utilization** | 5-30% | 80-95% |
| **Scalability** | ❌ 1 job at a time | ✅ N parallel |
| **Cost Discovery** | ✅ Exact | ✅ Exact |
| **Complexity** | Low | Medium |

## Implementation Plan

### Phase 1: Add Queues & Stages
1. Tạo `probeQueue` và `executeQueue`
2. Tạo `FetcherStage` class
3. Tạo `ProberStage` class
4. Tạo `ExecutorStage` class

### Phase 2: Implement FFprobe
1. Chọn strategy (Presigned URL recommended)
2. Implement `FastProbeService`
3. Benchmark latency

### Phase 3: Add tryAcquire
1. Thêm `tryAcquire(cost, timeout)` vào `TranscodeScheduler`
2. Update Probers để dùng tryAcquire

### Phase 4: Migration
1. Refactor `MediaEventConsumer` thành 3 stages
2. Keep heartbeat logic trong Stage 3
3. Testing với mixed workload (4K + 144p)

## Configuration

```properties
# Stage 1: Fetcher
app.pipeline.fetcher.batch-size=10
app.pipeline.fetcher.timeout-seconds=5

# Stage 2: Probers
app.pipeline.prober.thread-count=20
app.pipeline.prober.queue-capacity=100
app.pipeline.prober.acquire-timeout-seconds=10

# Stage 3: Executors
app.pipeline.executor.queue-capacity=50

# FFprobe Strategy
app.probe.strategy=presigned-url  # presigned-url | pipe-stream | partial-download
app.probe.partial-download-size-mb=10
```

## References

- ADR-001: Pull Subscription với Scheduler Integration
- ADR-002: AI Council Review - Phân tích các vấn đề tiềm ẩn
- [AI Council Reports](../../issues/block-natsjs-first-then-queue-job/)
  - Grok: Pipe stream approach
  - DeepSeek: Hybrid Serial/Parallel
  - Qwen: "Parallel Pre-Acquisition"
  - ChatGPT: "Two-lane pipeline"
  - Gemini: "Chiến thuật Sân Bay" (Presigned URL)
  - Kimi: Fetch theo available
  - ZAI: 3-Stage Pipeline

