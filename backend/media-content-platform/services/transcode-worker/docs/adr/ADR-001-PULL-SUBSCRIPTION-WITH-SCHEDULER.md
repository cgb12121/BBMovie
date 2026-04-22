# ADR-001: Pull Subscription với Scheduler Integration

## Status
**Accepted** - 2025-01-XX

## Context

### Vấn đề ban đầu
1. **NATS Redelivery Loop**: 
   - Consumer nhận message → submit to worker → `scheduler.acquire()` block
   - NATS không nhận ACK → timeout → redelivery loop

2. **Flow Control không hiệu quả**:
   - Nếu dùng `max_ack_pending=1` → chỉ nhận 1 message tại một thời điểm
   - Phá mất logic scheduler: 1 job 144p (cost=1) ngang hàng với 1 job 4K (cost=64)
   - Không tận dụng được parallel processing

3. **Resource Utilization kém**:
   - Server 16 cores chỉ chạy 1 job tại một thời điểm
   - Lãng phí tài nguyên CPU

## Decision

Chúng ta sẽ sử dụng **Pull Subscription Pattern với batch processing** kết hợp với **Scheduler Integration**:

1. **NATS JetStream Pull Subscription**:
   - `max_ack_pending = maxCapacity` (14 slots cho 16 cores)
   - Fetch batch messages (5 messages mỗi lần)
   - Mỗi message xử lý trong virtual thread riêng

2. **Heartbeat Mechanism**:
   - Mỗi virtual thread có heartbeat riêng
   - Gửi `inProgress()` mỗi 30s để reset NATS timeout
   - Cho phép long-running jobs (4K transcode 5 tiếng)

3. **Scheduler Integration**:
   - Scheduler acquire/release ở **level resolution** (không phải level file)
   - Mỗi resolution có cost riêng (144p=1, 4K=64)
   - Multiple threads có thể block tại `scheduler.acquire()`
   - Cho phép parallel processing: nhiều job nhỏ chạy song song, job lớn chờ slot

## Consequences

### ✅ Advantages

1. **Flow Control tự động**:
   - NATS chỉ gửi tối đa `maxCapacity` messages
   - Không bị spam, không bị redelivery loop

2. **Tận dụng tài nguyên**:
   - Multiple small jobs (144p) chạy song song
   - Large jobs (4K) chờ slot nhưng không block NATS
   - CPU được sử dụng tối đa

3. **Không timeout**:
   - Heartbeat reset timeout mỗi 30s
   - Long-running jobs không bị timeout

4. **Scheduler hoạt động đúng**:
   - Cost weights được tôn trọng
   - Resource allocation dựa trên resolution complexity
   - Không bị phá lock

5. **Không cần Database**:
   - State nằm ở NATS
   - Đơn giản hơn persistent queue solution

### ⚠️ Trade-offs

1. **Phụ thuộc vào NATS JetStream**:
   - Cần JetStream (không phải Core NATS)
   - Cần setup Stream và Consumer

2. **Memory usage**:
   - Nhiều messages trong RAM (tối đa `maxCapacity`)
   - Nhưng metadata nhỏ, chấp nhận được

3. **Complexity**:
   - Cần hiểu Pull Subscription pattern
   - Cần quản lý heartbeat threads

## Implementation Details

### Architecture

```
┌─────────────┐
│ NATS Server │
│ max_ack_pending = 14
└──────┬──────┘
       │ Batch (5 messages)
       ▼
┌─────────────────────────────────┐
│ Main Thread (Pull Loop)         │
│ - fetch(5, 2s)                   │
│ - Offload to virtual threads     │
│ - Continue fetching              │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│ Virtual Threads (Per Message)    │
│ - Heartbeat (inProgress every 30s)
│ - Parse message                  │
│ - Download & Validate            │
│ - Call processFile()             │
│ - ACK when done                  │
└──────┬───────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│ processFile()                    │
│ → videoTranscoderService         │
│   .transcode()                   │
└──────┬───────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│ For Each Resolution:             │
│ - scheduler.acquire(cost)        │
│   [MAY BLOCK HERE]               │
│ - Transcode with threads         │
│ - scheduler.release()            │
└─────────────────────────────────┘
```

### Key Components

1. **MediaEventConsumer**:
   - Pull Subscription với batch fetch
   - Virtual threads cho mỗi message
   - Heartbeat mechanism

2. **TranscodeScheduler**:
   - Weighted semaphore
   - Auto-detect capacity
   - Clamp threads to maxCapacity

3. **VideoTranscoderService**:
   - Parallel processing per resolution
   - Scheduler acquire/release per resolution
   - FFmpeg thread limiting

## Example Scenarios

### Scenario 1: Multiple Small Jobs
```
NATS sends: 14x 144p jobs (cost=1 each)
Scheduler: 14 slots available
Result: All 14 jobs run in parallel ✅
```

### Scenario 2: Large Job + Small Jobs
```
NATS sends: 1x 4K (cost=64) + 10x 144p (cost=1 each)
Scheduler: 14 slots available

4K job: acquire(64) → clamp to 14 → uses 14 slots
10x 144p: acquire(1) → BLOCK (waiting for slots)

When 4K done: Release 14 slots
10x 144p: All acquire slots → run in parallel ✅
```

### Scenario 3: Mixed Resolutions
```
NATS sends: 1x 1080p (cost=32) + 5x 360p (cost=4 each)
Scheduler: 14 slots available

1080p: acquire(32) → clamp to 14 → uses 14 slots
5x 360p: acquire(4) → BLOCK (waiting)

When 1080p done: Release 14 slots
5x 360p: Total cost = 20 → Only 3 can run (12 slots)
         Remaining 2 wait for next slot release ✅
```

## Configuration

```properties
# NATS JetStream
nats.minio.subject=minio.events
nats.stream.name=BBMOVIE
nats.consumer.durable=transcode-worker
nats.consumer.ack-wait-minutes=5
nats.consumer.heartbeat-interval-seconds=30
nats.consumer.fetch-batch-size=5
nats.consumer.fetch-timeout-seconds=2

# Scheduler
app.transcode.max-capacity=0  # Auto-detect
```

## Alternatives Considered

### Alternative 1: Database-Backed Queue
- **Pros**: Persistent, recovery mechanism
- **Cons**: Cần database, phức tạp hơn, overhead
- **Rejected**: Overkill cho use case này

### Alternative 2: max_ack_pending=1
- **Pros**: Đơn giản, flow control chặt chẽ
- **Cons**: Phá mất scheduler logic, không tận dụng parallel
- **Rejected**: Không phù hợp với weighted scheduler

### Alternative 3: Internal Queue (In-Memory)
- **Pros**: Fast, đơn giản
- **Cons**: Mất jobs khi crash, không persistent
- **Rejected**: Không đảm bảo reliability

## References

- [NATS JetStream Pull Subscription](https://docs.nats.io/nats-concepts/jetstream/consumers#pull-consumers)
- [NATS Heartbeat/InProgress](https://docs.nats.io/nats-concepts/jetstream/consumers#acknowledgements)
- [Java Virtual Threads](https://openjdk.org/jeps/444)

