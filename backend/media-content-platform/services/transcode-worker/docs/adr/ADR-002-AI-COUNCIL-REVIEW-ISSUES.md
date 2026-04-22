# ADR-002: AI Council Review - Phân tích các vấn đề tiềm ẩn

## Status
**Proposed** - 2025-12-25

## Context

Sau khi implement ADR-001 (Pull Subscription với Scheduler Integration), hệ thống đã được review bởi nhiều AI models (Grok, ChatGPT, DeepSeek, Qwen, Claude) để phát hiện các vấn đề tiềm ẩn trước khi đưa vào production.

### Tầm quan trọng
Transcode Worker là **1 trong 2 core services** của BBMovie platform. Bất kỳ lỗi nào trong service này đều có thể:
- Gây mất video uploads của users
- Tốn bandwidth và CPU vô ích
- Crash hệ thống khi overload

## Hệ thống hiện tại

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        CURRENT ARCHITECTURE                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐        ┌──────────────────┐        ┌───────────────┐  │
│  │ NATS Server  │        │ MediaEventConsumer│       │ MinIO Storage │  │
│  │              │◄──────▶│ (Pull Consumer)   │◄─────▶│               │  │
│  │ max_ack_     │        │                   │       │               │  │
│  │ pending=14   │        └────────┬──────────┘       └───────────────┘  │
│  └──────────────┘                 │                                      │
│                                   ▼                                      │
│                    ┌──────────────────────────────┐                      │
│                    │ Virtual Threads (per message) │                     │
│                    │ - Download file               │                     │
│                    │ - Validate (Tika, ClamAV)     │                     │
│                    │ - Analyze metadata            │                     │
│                    │ - Call transcode()            │                     │
│                    └──────────────┬───────────────┘                      │
│                                   │                                      │
│                                   ▼                                      │
│                    ┌──────────────────────────────┐                      │
│                    │ VideoTranscoderService        │                     │
│                    │ For each resolution:          │                     │
│                    │   - scheduler.acquire(cost)   │◄──┐                 │
│                    │   - FFmpeg transcode          │   │                 │
│                    │   - scheduler.release()       │───┘                 │
│                    └──────────────────────────────┘    TranscodeScheduler│
│                                                        (Semaphore-based) │
└─────────────────────────────────────────────────────────────────────────┘
```

### Processing Flow (Hiện tại)

```
1. Fetch batch messages từ NATS (5 messages)
2. Với mỗi message → spawn virtual thread:
   a. Start heartbeat (inProgress() mỗi 30s)
   b. Download file từ MinIO
   c. Validate (Tika, ClamAV)
   d. Analyze metadata → xác định target resolutions
   e. Với mỗi resolution:
      - scheduler.acquire(cost) → CÓ THỂ BLOCK
      - FFmpeg transcode
      - scheduler.release()
   f. ACK message
3. Loop back to fetch more
```

### Cost Model

| Resolution | Cost Weight | FFmpeg Threads |
|------------|-------------|----------------|
| 144p       | 1           | 1              |
| 240p       | 2           | 2              |
| 360p       | 4           | 4              |
| 480p       | 8           | 8              |
| 720p       | 16          | 14 (clamped)   |
| 1080p      | 32          | 14 (clamped)   |
| 4K         | 64          | 14 (clamped)   |

### Điểm mạnh của hệ thống hiện tại

| Aspect | Benefit |
|--------|---------|
| **Pull Consumer Pattern** | Đúng bài cho CPU-bound workload |
| **Cost-based Semaphore** | Weighted scheduling dựa trên resolution complexity |
| **Virtual Threads** | Efficient I/O handling cho Java 21 |
| **Heartbeat inProgress()** | Prevent timeout cho long-running jobs |
| **Clamp to maxCapacity** | Đảm bảo không overload CPU |
| **Fair Semaphore** | First-come-first-served ordering |

## Các vấn đề được phát hiện

### 🔴 Issue #1: Heartbeat `initialDelay` không phải 0

**Nguồn phát hiện**: DeepSeek ✓

**Code hiện tại**:
```java
// MediaEventConsumer.java line 274-281
heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
    msg.inProgress();
}, heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
//   ^^^^^^^^^^^^^^^^^^^^^^^^^ INITIAL DELAY = 30s, không phải 0!
```

**Vấn đề**:
- Heartbeat đầu tiên chỉ được gửi sau 30s
- Nếu download + validation mất > 30s (network chậm), NATS không biết message đang được xử lý
- Trong trường hợp lỗi xảy ra trong 30s đầu, không có heartbeat nào được gửi

**Impact**: Medium - Có thể gây confusion về message state

---

### 🔴 Issue #2: `maxAckPending = maxCapacity` - Sai semantics

**Nguồn phát hiện**: ChatGPT ✓

**Code hiện tại**:
```java
// MediaEventConsumer.java line 169-174
int maxAckPending = transcodeScheduler.getMaxCapacity(); // = 14
```

**Phân tích**:
| Concept | Meaning |
|---------|---------|
| `maxAckPending` | Số **MESSAGE** chưa ack tối đa |
| `maxCapacity` | Số **CPU SLOT** (ví dụ: 14 cores) |
| 1 message | 1 video → 6 resolutions → total cost = 1+2+4+8+16+32 = 63 points |

**Vấn đề logic**:
- NATS có thể gửi 14 messages
- 14 messages có thể cần 14 × 63 = 882 CPU points (worst case)
- Scheduler chỉ có 14 slots
- Kết quả: Messages bị queue trong RAM, block tại `acquire()`

**Impact**: Medium - Backpressure xảy ra SAU khi fetch, không phải TRƯỚC

---

### 🔴 Issue #3: Fetch QUÁ SỚM - Backpressure sai vị trí

**Nguồn phát hiện**: ChatGPT ✓, DeepSeek ✓

**Code hiện tại**:
```java
// MediaEventConsumer.java line 220-237
while (!Thread.currentThread().isInterrupted()) {
    // FETCH TRƯỚC, không check capacity
    List<Message> messages = sub.fetch(fetchBatchSize, Duration.ofSeconds(fetchTimeoutSeconds));
    
    for (Message msg : messages) {
        executor.submit(() -> processMessageWithHeartbeat(msg));
    }
}
```

**Vấn đề**:
- NATS quyết định khi nào gửi message (via maxAckPending)
- Worker không chủ động quyết định khi nào nhận
- Messages "nằm trong RAM" ngay cả khi scheduler full
- Heartbeat threads chạy cho messages đang chờ

**Impact**: High - RAM và threads bị tốn không cần thiết

---

### 🔴 Issue #4: Multi-record message có thể block

**Nguồn phát hiện**: Qwen ✓

**Code hiện tại**:
```java
// MediaEventConsumer.java line 288-294
if (rootNode.has("Records")) {
    for (JsonNode record : rootNode.get("Records")) {
        processRecordWithScheduler(record); // TUẦN TỰ, có thể block!
    }
}
msg.ack(); // ACK sau khi TẤT CẢ records xong
```

**Scenario**:
1. MinIO gửi batch event với 3 records trong 1 message
2. Record 1 bắt đầu transcode 4K video → chiếm 14/14 slots
3. Record 2, 3 gọi `scheduler.acquire()` → **BLOCK**
4. Heartbeat vẫn chạy (tốt!), message không timeout
5. Nhưng throughput bị giảm đáng kể

**Impact**: Medium - Không phải deadlock thực sự nhưng giảm throughput

---

### 🟡 Issue #5: Thiếu Graceful Shutdown

**Nguồn phát hiện**: Qwen ✓

**Vấn đề**:
- Không có `@PreDestroy` handler
- ExecutorService không được shutdown properly
- ScheduledExecutorService cho heartbeat không được cleanup
- Scheduler resources có thể không được release

**Impact**: Medium - Resource leak khi application restart

---

### 🟡 Issue #6: Heartbeat per message không tối ưu

**Nguồn phát hiện**: ChatGPT ✓

**Code hiện tại**:
- Mỗi message → 1 ScheduledFuture cho heartbeat
- 14 messages = 14 scheduled tasks

**Tối ưu hơn**:
```java
// 1 global heartbeat loop
ConcurrentHashMap<String, Message> inProgress = new ConcurrentHashMap<>();
scheduler.scheduleAtFixedRate(() -> {
    inProgress.values().forEach(msg -> msg.inProgress());
}, 0, 30, TimeUnit.SECONDS);
```

**Impact**: Low - Performance improvement, không phải bug

---

### 🟡 Issue #7: ackWait có thể quá ngắn

**Nguồn phát hiện**: Qwen ✓

**Config hiện tại**:
```yaml
nats.consumer.ack-wait-minutes: 5
```

**Vấn đề**:
- Transcoding 4K video có thể mất 20-60 phút
- Heartbeat `inProgress()` reset timeout → OK
- Nhưng nếu heartbeat fail (network glitch) → NATS redeliver → duplicate processing

**Impact**: Low - Hiếm xảy ra nếu heartbeat hoạt động tốt

---

### 🔴🔴 Issue #8: CHICKEN-AND-EGG PROBLEM (Critical)

**Nguồn phát hiện**: Claude (trong quá trình discussion)

**Vấn đề cốt lõi**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    THE CHICKEN-AND-EGG PROBLEM                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Để biết COST của message:                                               │
│    1. Fetch message từ NATS        ← Phải fetch trước                   │
│    2. Download file từ MinIO       ← Phải download                      │
│    3. Analyze metadata (ffprobe)   ← Bây giờ mới biết resolution        │
│    4. Calculate target resolutions ← Bây giờ mới tính được cost         │
│                                                                          │
│  Để quyết định có nên FETCH không:                                       │
│    → Cần biết cost TRƯỚC KHI fetch                                       │
│    → KHÔNG THỂ vì chưa có metadata!                                      │
│                                                                          │
│  ❌ CANNOT "check capacity before fetch" because cost is UNKNOWN!        │
└─────────────────────────────────────────────────────────────────────────┘
```

**Đây là lý do các suggestion "check capacity trước khi fetch" không thể áp dụng trực tiếp!**

**Impact**: Critical - Đây là vấn đề kiến trúc cơ bản

## Tổng kết đánh giá AI Council

| AI | Đánh giá | Độ chính xác |
|----|----------|--------------|
| **Grok** | Quá lạc quan, "xuất sắc" | Miss nhiều issues |
| **ChatGPT** | Phân tích sâu về semantics | ✓ Đúng về fetch timing, maxAckPending |
| **DeepSeek** | Đúng về heartbeat, solution phức tạp | ✓ initialDelay, nhưng overkill circuit breaker |
| **Qwen** | Balanced, thực tế | ✓ Multi-record, graceful shutdown |
| **Claude** | Tổng hợp + phát hiện chicken-and-egg | ✓ Core architectural issue |

## Severity Matrix

| # | Issue | Severity | Fixable without redesign? |
|---|-------|----------|---------------------------|
| 1 | Heartbeat initialDelay | 🟡 Medium | ✅ Yes - simple fix |
| 2 | maxAckPending semantics | 🟡 Medium | ✅ Yes - config change |
| 3 | Fetch too early | 🔴 High | ⚠️ Partially |
| 4 | Multi-record block | 🟡 Medium | ✅ Yes - producer change |
| 5 | Graceful shutdown | 🟡 Medium | ✅ Yes - add @PreDestroy |
| 6 | Heartbeat per message | 🟢 Low | ✅ Yes - optimization |
| 7 | ackWait too short | 🟢 Low | ✅ Yes - config change |
| 8 | **Chicken-and-Egg** | 🔴🔴 Critical | ❌ NO - needs redesign |

## Decision

Các issues #1-7 có thể fix incrementally, nhưng **Issue #8 (Chicken-and-Egg Problem)** yêu cầu thiết kế lại architecture.

Xem **ADR-003** để biết giải pháp đề xuất: **3-Stage Pipeline Architecture**.

## References

- [Grok Report](../issues/grok_report.md)
- [ChatGPT Report](../issues/chatgpt_report.md)
- [DeepSeek Report](../issues/deepseek_report.md)
- [Qwen Report](../issues/qwen_report.md)
- ADR-001: Pull Subscription với Scheduler Integration
- ADR-003: 3-Stage Pipeline Architecture (accepted solution)

