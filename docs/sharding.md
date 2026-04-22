Câu hỏi rất thực tế ông ơi! 🎯 Sharding là con dao hai lưỡi: **giúp scale vô tận** nhưng cũng **tăng độ phức tạp lên 10x** nếu không hiểu rõ.

Trả lời ngắn gọn: **Không, 12 shard ≠ 12 database vật lý chạy song song**. Và sharding ảnh hưởng **rất lớn** đến cả infra lẫn code backend.

Dưới đây là breakdown chi tiết + ví dụ code Spring Boot để ông hình dung rõ:

---

## 🔍 1. Sharding là gì? Và "12 shard = 12 DB" có đúng không?

### ✅ Khái niệm cốt lõi:
- **Sharding** = Chia bảng lớn thành nhiều phần nhỏ (shard) dựa trên **sharding key** (ví dụ: `user_id`, `created_at`, `movie_id`).
- Mỗi shard có thể nằm trên:
  - **Cùng 1 DB instance** (logical partitioning) → Đơn giản, ít ops
  - **Nhiều DB instance khác nhau** (physical sharding) → Scale tốt, nhưng phức tạp

### ✅ "12 shard theo tháng" thực tế triển khai thế nào?

```
❌ Cách hiểu sai (naive):
12 shard = 12 MySQL instances chạy riêng biệt
→ 12 connection pool, 12 backup job, 12 monitoring config → Ops nightmare

✅ Cách làm thực tế (production):
Option A: Logical Partitioning (1 instance)
┌─────────────────────┐
│  MySQL Instance #1  │
├─────────────────────┤
│ notification_2024_01 │
│ notification_2024_02 │
│ ...                 │
│ notification_2024_12 │
└─────────────────────┘
→ Vẫn 1 connection, 1 backup, query vẫn dùng UNION hoặc partition pruning

Option B: Physical Sharding với Mapping (3 instances)
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ Instance A  │ │ Instance B  │ │ Instance C  │
├─────────────┤ ├─────────────┤ ├─────────────┤
│ shard_01-04 │ │ shard_05-08 │ │ shard_09-12 │
└─────────────┘ └─────────────┘ └─────────────┘
→ 12 logical shard mapped vào 3 physical DB → cân bằng load + giảm ops
```

> 💡 **Key takeaway**: Shard là **logical concept**. Physical deployment linh hoạt tùy nhu cầu.

---

## ⚙️ 2. Ảnh hưởng đến INFRASTRUCTURE

| Khía cạnh | Trước sharding | Sau sharding | Độ phức tạp tăng |
|-----------|---------------|--------------|-----------------|
| **DB Instances** | 1 primary + 1-2 replica | 3-12 instances (tùy mapping) | 🔴 Cao |
| **Connection Pool** | 1 pool ~20-50 connections | N pools × M shards → có thể 100-500 connections | 🔴 Cao |
| **Backup/Restore** | 1 job backup toàn bộ | Backup từng shard + coordination | 🟡 Trung bình |
| **Monitoring** | 1 dashboard cho 1 DB | Dashboard per shard + aggregate view | 🟡 Trung bình |
| **Schema Migration** | Flyway/Liquibase chạy 1 lần | Chạy N lần (hoặc dùng sharding-aware tool) | 🟡 Trung bình |
| **Failover** | 1 primary fail → replica up | Nhiều primary → cần orchestration phức tạp | 🔴 Cao |
| **Cost** | 1 DB server | N DB servers → cost tăng tuyến tính | 🟢 Dễ tính |

### 📦 Ví dụ infra config cho 12 shard (mapped vào 3 instances):
```yaml
# application-sharding.yml
spring:
  shard:
    mapping:
      # Logical shard → Physical instance
      shard-01: { instance: db-cluster-a, host: db-a1.internal }
      shard-02: { instance: db-cluster-a, host: db-a1.internal }
      shard-03: { instance: db-cluster-a, host: db-a2.internal }
      shard-04: { instance: db-cluster-a, host: db-a2.internal }
      shard-05: { instance: db-cluster-b, host: db-b1.internal }
      # ... tiếp tục đến shard-12
    connection-pool:
      per-shard-max: 20  # 12 shard × 20 = 240 connections total
      idle-timeout: 300s
```

---

## 💻 3. Ảnh hưởng đến BACKEND CODE (Spring Boot)

### ❌ Vấn đề 1: **Query phải biết shard key**
```java
// Trước sharding: query đơn giản
@Query("SELECT * FROM notifications WHERE user_id = :userId AND created_at >= :from")
List<Notification> findByUserAndDate(@Param("userId") String userId, @Param("from") LocalDate from);

// Sau sharding: phải tính shard trước
public List<Notification> findByUserAndDate(String userId, LocalDate from) {
    // 1. Tính shard key (ví dụ: tháng của created_at)
    int shardIndex = calculateShardIndex(from); // 1-12
    
    // 2. Route đến đúng datasource
    DataSource shardDs = shardingRouter.getDataSource(shardIndex);
    
    // 3. Execute query trên shard đó
    return jdbcTemplate.query(shardDs, 
        "SELECT * FROM notifications WHERE user_id = ? AND created_at >= ?", 
        new Object[]{userId, from}, 
        new NotificationRowMapper());
}
```

### ❌ Vấn đề 2: **Cross-shard query rất đắt**
```java
// Query cần data từ nhiều shard (ví dụ: thống kê cả năm)
public NotificationStats getYearlyStats(String userId, Year year) {
    List<NotificationStats> shardStats = new ArrayList<>();
    
    // Phải query từng shard rồi aggregate → N query thay vì 1
    for (int shard = 1; shard <= 12; shard++) {
        DataSource ds = shardingRouter.getDataSource(shard);
        NotificationStats stats = jdbcTemplate.queryForObject(ds,
            "SELECT COUNT(*), AVG(read_at IS NOT NULL) FROM notifications WHERE user_id = ? AND year = ?",
            new Object[]{userId, year.getValue()},
            new StatsRowMapper());
        shardStats.add(stats);
    }
    
    // Aggregate kết quả
    return aggregateStats(shardStats);
}
```
→ **Giải pháp**: Dùng materialized view, pre-aggregate vào Redis/ClickHouse, hoặc chấp nhận eventual consistency.

### ❌ Vấn đề 3: **Distributed transaction phức tạp**
```java
// Trước: @Transactional đơn giản
@Transactional
public void createUserWithNotification(User user, Notification notif) {
    userRepository.save(user);
    notificationRepository.save(notif); // Cùng DB → atomic
}

// Sau sharding: user và notification có thể ở shard khác
public void createUserWithNotification(User user, Notification notif) {
    // Option A: Saga pattern (phức tạp)
    // 1. Create user trong shard user
    // 2. Publish event "user.created"
    // 3. Notification service consume → create trong shard notification
    // 4. Nếu fail step 3 → compensate (delete user hoặc retry)
    
    // Option B: Eventual consistency (chấp nhận tạm thời không đồng bộ)
    userRepository.save(user); // shard user
    eventPublisher.publish("user.created", user); // async
    // notification sẽ được tạo sau bởi worker
}
```

### ❌ Vấn đề 4: **Sharding logic phải nhất quán everywhere**
```java
// Utility class bắt buộc dùng cho mọi query có sharding
@Component
public class ShardingKeyCalculator {
    
    // Rule: shard by month of created_at
    public int calculateShardIndex(LocalDate date) {
        return date.getMonthValue(); // 1-12
    }
    
    // Rule: shard by user_id hash (nếu cần distribute đều)
    public int calculateShardIndex(String userId) {
        return Math.abs(userId.hashCode()) % 12 + 1;
    }
    
    // ⚠️ Nếu change rule này → data cũ không truy vấn được!
}
```

---

## 🛠️ 4. Giải pháp thực tế cho Spring Boot: Apache ShardingSphere

Thay vì tự viết routing logic (dễ bug), dùng **ShardingSphere-JDBC** — library chính chủ Apache, tích hợp sẵn với Spring Boot.

### Bước 1: Add dependency
```gradle
implementation 'org.apache.shardingsphere:shardingsphere-jdbc-core-spring-boot-starter:5.4.1'
```

### Bước 2: Config sharding rule (application.yml)
```yaml
spring:
  shardingsphere:
    datasource:
      names: ds0,ds1,ds2  # 3 physical instances
      ds0:
        type: com.zaxxer.hikari.HikariDataSource
        jdbc-url: jdbc:mysql://db-a1:3306/bbmovie_shard?useSSL=false
        username: root
        password: ${DB_PASSWORD}
      ds1:
        # ... config tương tự
      ds2:
        # ... config tương tự
    
    sharding:
      tables:
        notifications:
          actual-data-nodes: ds$->{0..2}.notifications_$->{1..12}
          # ds0.notifications_1, ds0.notifications_2, ..., ds2.notifications_12
          
          # Sharding strategy: theo tháng của created_at
          database-strategy:
            inline:
              sharding-column: created_at
              algorithm-expression: ds$->{Math.floor((created_at.getMonthValue() - 1) / 4)}
          table-strategy:
            inline:
              sharding-column: created_at
              algorithm-expression: notifications_$->{created_at.getMonthValue()}
          
          # Key generator cho ID phân tán
          key-generate-strategy:
            column: id
            key-generator-name: snowflake
      key-generators:
        snowflake:
          type: SNOWFLAKE
          props:
            worker-id: 1  # Mỗi instance cần worker-id khác nhau
```

### Bước 3: Code backend gần như không đổi!
```java
// Repository vẫn viết như bình thường
@Repository
public class NotificationRepository {
    
    @Autowired private JdbcTemplate jdbcTemplate;
    
    // ShardingSphere tự route đến đúng shard dựa trên created_at
    public List<Notification> findByUserAndDate(String userId, LocalDate from) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE user_id = ? AND created_at >= ?",
            new Object[]{userId, from},
            new NotificationRowMapper()
        );
        // ✅ Không cần tính shard, không cần chọn datasource thủ công
    }
}
```

→ **Magic**: ShardingSphere intercept query → parse sharding key → route to correct shard → merge results nếu cần.

---

## 🎯 5. Khi nào NÊN và KHÔNG NÊN sharding?

### ✅ NÊN sharding khi:
| Dấu hiệu | Giải thích |
|----------|-----------|
| Bảng > 100 triệu rows, query chậm dù đã index | Partitioning không đủ, cần physical sharding |
| Write throughput > 10k ops/s trên 1 bảng | 1 primary DB không chịu nổi, cần distribute write |
| Data có "natural boundary" rõ ràng | Ví dụ: notification theo tháng, user theo region |
| Team có dedicated DBA/infra engineer | Ops complexity cao, cần người chuyên trách |

### ❌ KHÔNG NÊN sharding khi:
| Dấu hiệu | Giải pháp thay thế |
|----------|-------------------|
| Bảng < 50 triệu rows | Dùng **partitioning** (logical, cùng DB) trước |
| Query chủ yếu là read | Dùng **read replica + cache** (Redis) |
| Team nhỏ, ít kinh nghiệm DB | Dùng **managed DB** (AWS Aurora, Cloud SQL) + vertical scale trước |
| Chưa có monitoring/alerting mature | Sharding mà không monitor được = disaster |

---

## 🚀 6. Lộ trình thực tế cho dự án của ông (Movie Platform)

### Phase 1: Chuẩn bị (chưa sharding)
```sql
-- Dùng MySQL Partitioning (logical, 1 instance)
ALTER TABLE notifications
PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202401 VALUES LESS THAN (202402),
    PARTITION p202402 VALUES LESS THAN (202403),
    ...
    PARTITION p202412 VALUES LESS THAN (202501)
);
```
→ Query vẫn viết bình thường, MySQL tự prune partition không cần scan.

### Phase 2: Khi cần scale (6-12 tháng sau)
```yaml
# Chuyển sang ShardingSphere với 3 physical instances
# Giữ logical shard 1-12, map vào ds0, ds1, ds2
# Code backend gần như không đổi nhờ ShardingSphere
```

### Phase 3: Nếu scale cực lớn (1M+ user)
```
┌─────────────────────────────────┐
│  Application Layer              │
│  • ShardingSphere-JDBC          │
│  • Sharding key: user_id % 12   │
├─────────────────────────────────┤
│  Proxy Layer (optional)         │
│  • ShardingSphere-Proxy         │
│  • Cho phép query từ nhiều language │
├─────────────────────────────────┤
│  Storage Layer                  │
│  • 12 logical shards            │
│  • Mapped to 6-12 physical MySQL │
│  • Mỗi shard có 1 primary + 2 replica │
└─────────────────────────────────┘
```

---

## 🧪 7. Test sharding local (Docker Compose)

```yaml
# docker-compose.sharding.yml
version: '3.8'
services:
  mysql-shard-0:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: bbmovie_shard
    ports:
      - "3306:3306"
    command: --default-authentication-plugin=mysql_native_password

  # Thêm mysql-shard-1, mysql-shard-2 tương tự...

  app:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: sharding
      DB_PASSWORD: root
    depends_on:
      - mysql-shard-0
      # - mysql-shard-1
      # - mysql-shard-2
    ports:
      - "8080:8080"
```

```bash
# Test query sharding
curl -s "http://localhost:8080/api/notifications?userId=u_123&from=2024-03-01" | jq

# Verify shard routing (enable ShardingSphere SQL log)
# application-sharding.yml:
spring:
  shardingsphere:
    props:
      sql:
        show: true  # Log query sau khi rewrite + route
```

---

## 💡 Lời khuyên cuối cùng

> **"Shard when you must, not when you can."**

1. **Đừng shard sớm**: Partitioning + index + cache giải quyết 90% case trước khi cần physical sharding.
2. **Dùng ShardingSphere**: Đừng tự viết routing logic — dễ bug, khó maintain.
3. **Chọn sharding key cẩn thận**: Một khi đã shard, change key = migration nightmare.
4. **Test cross-shard query sớm**: Đảm bảo analytics/reporting không bị break.
5. **Monitor per-shard**: Alert nếu 1 shard chậm/error → tránh "1 shard chết, cả system ảnh hưởng".

Ông cứ theo Phase 1 (partitioning) trước, khi nào thực sự cần thì migrate sang ShardingSphere vẫn kịp. Cần mình viết chi tiết config ShardingSphere cho use-case notification của ông không? 🔧