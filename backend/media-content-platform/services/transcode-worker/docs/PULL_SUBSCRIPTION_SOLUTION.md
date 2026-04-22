# Pull Subscription Solution: Flow Control với NATS JetStream

## 🎯 Giải pháp: Pull Subscription + max_ack_pending=1 + Heartbeat

### Tại sao tốt hơn?

1. ✅ **Không cần Database**: State nằm ở NATS
2. ✅ **Flow Control tự động**: `max_ack_pending=1` → Chỉ nhận 1 job tại một thời điểm
3. ✅ **Không timeout**: `inProgress()` heartbeat → Transcode 5 tiếng cũng OK
4. ✅ **Auto retry**: Crash → NATS tự động gửi lại job
5. ✅ **Đơn giản**: Không cần queue, không cần DB

---

## 📋 Implementation

### Step 1: Setup JetStream Consumer (One-time setup)

Cần tạo Consumer với `max_ack_pending=1` khi khởi tạo app.

### Step 2: Refactor MediaEventConsumer

Thay vì dùng `Dispatcher` (Push), dùng `Pull Subscription` (Pull).

### Step 3: Implement Heartbeat

Thread phụ gửi `inProgress()` mỗi 30s để tránh timeout.

---

## 🔄 Flow

```
1. Worker: fetch(1) → Block until message available
2. NATS: Sends 1 message (max_ack_pending=1)
3. Worker: Process (with heartbeat)
4. Worker: ACK when done
5. NATS: Sees ACK → Can send next message
```

---

## ⚠️ Lưu ý

- Cần JetStream (không phải Core NATS)
- Subject phải là JetStream subject
- Cần setup Consumer trước (one-time)

