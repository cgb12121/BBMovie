Ông cảm nhận **đúng 100%** rồi đấy! 😄 Đây gọi là **"Database Homogeneity Anti-pattern"** — dùng cùng 1 loại database cho mọi bài toán, giống như việc cố gắng dùng tua vít để đóng đinh vậy.

Thực tế: **ClickHouse là Columnar DB**, không phải SQL truyền thống, nên ông đã có 1-2 service dùng NoSQL rồi. Nhưng đúng là hệ sinh thái 27 services mà 90% dùng MySQL/PostgreSQL thì **chưa tối ưu**.

Dưới đây là phân tích thực tế + lộ trình chuyển đổi **không cần rewrite toàn bộ**:

---

## 📊 1. Audit hiện trạng: Service nào nên dùng DB gì?

| Service | DB hiện tại (dự đoán) | **DB đề xuất** | Lý do | Độ ưu tiên |
|---------|---------------------|---------------|-------|-----------|
| **auth-service** | MySQL/Postgres | **Giữ nguyên** | ACID critical, user data cần consistency | 🔴 Không đổi |
| **payment-service** | MySQL/Postgres | **Giữ nguyên** | Financial transaction, không được mất data | 🔴 Không đổi |
| **referral-service** | MySQL/Postgres | **Giữ nguyên** | Ledger-style, cần transaction | 🟡 Không đổi |
| **search-service** | Elasticsearch | **Giữ nguyên** | ✅ Đã là NoSQL (document store) | - |
| **ai-service** | ? | **Qdrant/Weaviate** | Vector search cho embedding | 🟢 Cao |
| **watch-history** | MySQL/Postgres | **Redis + TimescaleDB** | Time-series data, write-heavy, TTL | 🟢 **Rất cao** |
| **notification-service** | MySQL/Postgres | **MongoDB + Redis** | Document flexible schema + queue | 🟢 Cao |
| **comment-service** | MySQL/Postgres | **MongoDB** | Nested comments, schema evolution | 🟢 Cao |
| **movie-analytics-service** | ClickHouse | **Giữ nguyên** | ✅ Columnar OLAP, đúng mục đích | - |
| **revenue-dashboard** | MySQL/Postgres | **ClickHouse** | Aggregation query lớn | 🟢 Trung bình |
| **personalization-recommendation** | MySQL/Postgres | **Redis + Neo4j** | Feature cache + Graph cho relationship | 🟢 Trung bình |
| **media-service** | MySQL/Postgres | **MongoDB** | Metadata flexible (tag, genre, cast) | 🟡 Trung bình |
| **homepage-recommendations** | MySQL/Postgres | **Redis** | Cache + sorted set cho trending | 🟢 Cao |
| **user-preference** (nếu có) | MySQL/Postgres | **Document DB** | Schema thay đổi thường | 🟡 Thấp |
| **session/cache layer** | Redis (có thể có) | **Redis Cluster** | Session, rate limit, distributed lock | 🟢 Rất cao |

---

## 🎯 2. Top 5 Services nên migrate sang NoSQL trước

### 🥇 #1: `watch-history` → Redis + TimescaleDB

**Hiện tại:** MySQL với bảng `watch_history(user_id, movie_id, position, created_at)`
**Vấn đề:** 
- Write-heavy (mỗi user gửi event mỗi 5-10s)
- Query theo thời gian (time-range) nhiều
- Data cũ ít khi query → cần TTL

**Giải pháp:**
```yaml
# Kiến trúc hybrid
- Redis Hash: user:pos:{userId} → resume playback (hot path)
- TimescaleDB: watch_events → analytics, retention calculation
- Auto-archive: data >30 ngày → ClickHouse hoặc S3
```

**Lợi ích:** 
- Write performance tăng 10-50x
- TTL tự động → không cần cron job delete old data
- Time-series query (GROUP BY time bucket) nhanh hơn 100x

---

### 🥈 #2: `notification-service` → MongoDB + Redis

**Hiện tại:** MySQL với bảng `notification(user_id, title, content, type, read_at)`
**Vấn đề:**
- Schema thay đổi thường (thêm channel preference, template variant)
- Query pattern đa dạng (theo user, theo type, theo status)
- Unread count query chậm

**Giải pháp:**
```javascript
// MongoDB document
{
  _id: ObjectId,
  userId: "u_123",
  type: "PAYMENT_SUCCESS",
  title: "Thanh toán thành công",
  content: "Số tiền: 50,000 VNĐ",
  channels: {
    email: { sent: true, sentAt: ISODate },
    push: { sent: true, sentAt: ISODate },
    sms: { sent: false }
  },
  metadata: { paymentId: "p_456", amount: 50000 }, // flexible!
  readAt: null,
  createdAt: ISODate,
  expiresAt: ISODate // TTL index
}

// Index
db.notification.createIndex({ userId: 1, createdAt: -1 })
db.notification.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 })
```

**Lợi ích:**
- Thêm field mới không cần ALTER TABLE
- TTL tự động delete notification cũ
- Query theo metadata linh hoạt

---

### 🥉 #3: `comment-service` → MongoDB

**Hiện tại:** MySQL với bảng `comment(id, parent_id, user_id, content, path)`
**Vấn đề:**
- Nested comment (reply chain) query phức tạp (recursive CTE)
- Schema thay đổi (thêm reaction, mention, attachment)

**Giải pháp:**
```javascript
// MongoDB document với nested structure
{
  _id: "c_001",
  movieId: "m_123",
  userId: "u_456",
  content: "Phim hay quá!",
  reactions: { like: 10, love: 5, haha: 2 },
  mentions: ["u_789"],
  attachments: [{ type: "image", url: "..." }],
  replies: [  // Embedded cho reply cấp 1
    { _id: "c_002", userId: "u_789", content: "Đồng ý!", createdAt: ISODate }
  ],
  replyCount: 15, // Denormalized cho quick count
  createdAt: ISODate
}
```

**Lợi ích:**
- Query comment + reply trong 1 document (không cần JOIN)
- Thêm reaction/attachment không cần schema migration
- Denormalized count → không cần COUNT(*) mỗi lần query

---

### #4: `homepage-recommendations` + `personalization-recommendation` → Redis

**Hiện tại:** MySQL query để lấy trending, affinity
**Vấn đề:** 
- Query aggregation chậm (SUM, COUNT, GROUP BY)
- Real-time requirement cao

**Giải pháp:**
```bash
# Redis data structures
ZSET global:trending → movieId:score (trending toàn hệ thống)
ZSET user:affinity:{userId} → genre:score (sở thích user)
HASH user:recent:{userId} → movieId:timestamp (xem gần đây)
LIST user:queue:{userId} → [movieId1, movieId2, ...] (recommendation queue)
```

**Lợi ích:**
- Read latency <5ms
- Atomic increment (ZINCRBY) cho real-time trending
- Không cần database connection pool

---

### #5: `ai-service` → Vector Database (Qdrant/Weaviate)

**Hiện tại:** Có thể đang lưu embedding trong PostgreSQL (pgvector) hoặc MySQL
**Vấn đề:**
- Similarity search chậm khi scale
- Không có built-in filter + vector search kết hợp

**Giải pháp:**
```yaml
# Qdrant collection
collections:
  - name: movie-embeddings
    vectors:
      size: 768  # BERT embedding size
      distance: Cosine
    indexes:
      - name: genre-filter
        type: keyword
      - name: year-filter
        type: range
```

```python
# Search với filter
qdrant.search(
    collection_name="movie-embeddings",
    query_vector=embedding,
    query_filter=Filter(
        must=[
            FieldCondition(key="genre", match=MatchValue(value="action")),
            FieldCondition(key="year", range=Range(gte=2020))
        ]
    ),
    limit=20
)
```

**Lợi ích:**
- Vector search nhanh hơn 10-100x so với pgvector
- Built-in filtering + vector search
- Scale ngang dễ dàng

---

## 🗺️ 3. Lộ trình migrate (6 tháng)

```
┌─────────────────────────────────────────────────────────────┐
│  THÁNG 1-2: Foundation                                       │
├─────────────────────────────────────────────────────────────┤
│  ✅ Setup Redis Cluster (cho cache + session)               │
│  ✅ Setup MongoDB (cho notification + comment)              │
│  ✅ Viết abstraction layer: DatabaseRepository interface    │
│  ✅ Migrate watch-history → Redis (resume playback)         │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│  THÁNG 3-4: Core Migration                                   │
├─────────────────────────────────────────────────────────────┤
│  ✅ Migrate notification-service → MongoDB                  │
│  ✅ Migrate comment-service → MongoDB                       │
│  ✅ Migrate homepage-recommendations → Redis                │
│  ✅ Setup monitoring per database type                      │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│  THÁNG 5-6: Advanced                                         │
├─────────────────────────────────────────────────────────────┤
│  ✅ Setup Qdrant/Weaviate cho ai-service                    │
│  ✅ Migrate revenue-dashboard → ClickHouse                  │
│  ✅ Implement dual-write (SQL + NoSQL) cho data critical    │
│  ✅ Cut-over và decommission old tables                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ 4. Abstraction Layer: Để migrate không cần rewrite

```java
// src/main/java/com/movie/core/repository/GenericRepository.java
public interface GenericRepository<T, ID> {
    T findById(ID id);
    List<T> findByCriteria(Map<String, Object> criteria);
    T save(T entity);
    void deleteById(ID id);
}

// SQL implementation
@Repository
@Profile("sql")
public class SqlNotificationRepository implements GenericRepository<Notification, Long> {
    @Autowired private JdbcTemplate jdbcTemplate;
    // ... SQL implementation
}

// MongoDB implementation
@Repository
@Profile("mongodb")
public class MongoNotificationRepository implements GenericRepository<Notification, String> {
    @Autowired private MongoTemplate mongoTemplate;
    // ... MongoDB implementation
}

// Service layer không đổi!
@Service
public class NotificationService {
    @Autowired private GenericRepository<Notification, ?> repository;
    // ... business logic không quan tâm DB bên dưới
}
```

```yaml
# application.yml
spring:
  profiles:
    active: mongodb  # Switch dễ dàng giữa sql/mongodb
```

---

## 📈 5. Expected Impact

| Metric | Trước (all SQL) | Sau (polyglot persistence) | Cải thiện |
|--------|----------------|---------------------------|----------|
| **Write throughput (watch-history)** | 1k ops/s | 50k ops/s (Redis) | **50x** |
| **Read latency (recommendation)** | 50-200ms | 5-10ms (Redis) | **10-20x** |
| **Schema migration time** | 1-2 giờ (ALTER TABLE) | 0 (document flexible) | **∞** |
| **Storage cost (analytics)** | $500/tháng (MySQL) | $50/tháng (ClickHouse) | **10x** |
| **Developer velocity** | Chậm (cần DBA review) | Nhanh (dev tự chủ) | **2-3x** |

---

## ⚠️ 6. Lưu ý quan trọng

| Rủi ro | Giảm thiểu |
|--------|-----------|
| **Data consistency** | Dùng dual-write + reconciliation job trong giai đoạn transition |
| **Team learning curve** | Training MongoDB/Redis pattern trước khi migrate |
| **Operational complexity** | Dùng managed service (MongoDB Atlas, Redis Cloud) nếu có budget |
| **Transaction across DB** | Saga pattern + event sourcing cho cross-database operation |
| **Backup/restore khác nhau** | Chuẩn hóa backup procedure cho từng DB type |

---

## 🎯 Kết luận

> **"Right tool for the right job"** — Không phải NoSQL tốt hơn SQL, mà là **mỗi loại phù hợp với use-case khác nhau**.

**Ông không cần migrate tất cả 27 services.** Chỉ cần tập trung vào 5-7 services có pain point rõ ràng nhất:

1. **watch-history** → Redis + TimescaleDB (write-heavy, time-series)
2. **notification-service** → MongoDB (flexible schema, TTL)
3. **comment-service** → MongoDB (nested document)
4. **recommendation services** → Redis (low latency cache)
5. **ai-service** → Qdrant (vector search)

Các service còn lại như `auth`, `payment`, `referral` → **Giữ nguyên SQL** là đúng rồi!

Ông muốn mình viết chi tiết migration plan cho service nào trước không? Hay cần sample code cho abstraction layer? 🔧