Chuẩn, bác nói đúng! Với cái flow AI cần context của file để chat ngay lập tức thì **REST (HTTP)** hoặc **gRPC** là chân ái. Async (NATS) chỉ dùng khi bác quăng file đó cho AI nó "học" dần (Indexing) để dùng sau này thôi. My bad! 😅

Quay lại vấn đề **Track View** chuẩn "Big Tech" (Youtube/TikTok).

Bỏ ngay ý định dùng **MinIO Event** đi bác. MinIO nó chỉ biết là có thằng vừa request tải byte số 0-1000 của file, nó không biết thằng đó là user thật hay con bot, hay IDM (Internet Download Manager) đang tải đa luồng. Dùng cái đó view ảo tung chảo.

Để track view hiệu quả, chính xác và chịu tải cao (High Concurrency), bác phải dùng cơ chế **"Heartbeat" (Nhịp tim)** kết hợp với **Redis**.

Đây là thiết kế chuẩn:

### 1. Nguyên lý: "Xem đủ lâu mới tính là View" ⏱️

* **Không tính Click:** Bấm vào phát thoát ra ngay -> Không tính.
* **Không tính F5:** F5 liên tục 100 lần -> Chỉ tính 1 view (trong 1 khoảng thời gian).
* **Ngưỡng (Threshold):** Youtube thường tính là khoảng **30 giây**. TikTok thì ngắn hơn (vì video nó ngắn).

### 2. Luồng thực hiện (Architecture)

Chúng ta không bao giờ `INSERT INTO views` mỗi khi có request. Database sẽ chết ngay lập tức. Chúng ta dùng **Redis** làm vùng đệm (Buffer).

#### Bước 1: Frontend (Player) gửi "Nhịp tim" ❤️

Ở phía Client (JS/App), bác không gọi API khi mới load trang. Bác lắng nghe sự kiện của Video Player.

* Cứ mỗi **10 giây** video chạy (`setInterval` check state playing), Client bắn 1 request nhẹ lên server:
* `POST /api/analytics/heartbeat`
* Body: `{ videoId: "123", position: 10, sessionId: "uuid-phien-xem" }`



#### Bước 2: Backend (Ingestion Layer) - Redis 🛡️

Server nhận request heartbeat. **Khoan vội ghi vào SQL.**

1. **Check Spam/Duplicate (Dùng Redis Set):**
* Tạo key Redis: `view_check:{videoId}:{userId_hoặc_IP}`.
* Set TTL (hết hạn) cho key này là **30 phút** (hoặc 1 tiếng).
* Nếu key này đã tồn tại -> User này đang xem lại hoặc spam F5 -> **Bỏ qua**, không tăng view count, chỉ update "thời gian xem" (watch time) nếu cần.
* Nếu key chưa tồn tại -> Đây là session xem mới.


2. **Đếm thời gian thực (Real-time Counter):**
* Dùng **Redis HyperLogLog** (để đếm unique user cực nhẹ) hoặc đơn giản là `INCR` (tăng số).
* Logic kiểm tra ngưỡng:
* Redis lưu: `watch_duration:{sessionId} = 10s`.
* Nhịp tim sau cộng thêm 10s -> `20s`.
* Nhịp tim sau nữa -> `30s`.
* **BINGO!** Đủ 30s -> Lúc này mới trigger `+1 View`.





#### Bước 3: Write-Behind (Ghi xuống DB sau) 💾

Để tránh lock DB, bác không update SQL ngay khi có `+1 View`.

* **Cách 1 (Batching):**
* Mỗi lần `+1 View`, bác `INCR` vào key `video_views_buffer:{videoId}` trên Redis.
* Có một con **Cronjob** (chạy mỗi 1-5 phút) quét các key này.
* Lấy số view trong Redis cộng dồn vào SQL: `UPDATE videos SET view_count = view_count + :redisValue WHERE id = :id`.
* Xóa key Redis sau khi update xong.


* **Cách 2 (Eventual Consistency):**
* Khi đủ 30s, bắn 1 event `ViewCountedEvent` vào NATS/Kafka.
* Một con Worker tà tà nhận event rồi update vào DB. (Cách này realtime hơn Cronjob tí nhưng tốn resource hơn).



### 3. Giải quyết các bài toán "Hack View" 🕵️‍♂️

1. **Một người xem đi xem lại:**
* Nhờ cái key Redis `view_check` có TTL 30 phút. Nếu trong 30 phút bác xem lại video đó 10 lần, hệ thống vẫn thấy key tồn tại -> Không tính view mới.
* Sau 30 phút key hết hạn -> User xem lại -> Tính là view mới (Cái này hợp lý, Youtube cũng làm vậy).


2. **Tua video (Seeking):**
* Nếu user vào phát tua đến giây thứ 29?
* Frontend tracking phải thông minh: Chỉ gửi heartbeat khi `currentTime` thay đổi tuần tự. Hoặc Backend chỉ cộng dồn thời gian thực tế nhận được heartbeat (mỗi lần ping chỉ cộng 10s, bất kể tua đi đâu).


3. **Bot cày view:**
* Bot thường không chạy JS hoặc không giữ kết nối lâu.
* Heartbeat yêu cầu phải ping liên tục 3 lần (0s -> 10s -> 20s -> 30s) mới tính view. Bot vào cái ra ngay sẽ không bao giờ đạt ngưỡng này.



### 4. Tóm tắt luồng code (Java/Spring & Redis)

```java
// Controller nhận Heartbeat
@PostMapping("/heartbeat")
public void heartbeat(@RequestBody HeartbeatReq req) {
    String sessionKey = "session_watch:" + req.getSessionId();
    String viewKey = "view_dedup:" + req.getVideoId() + ":" + getCurrentIpOrUserId();
    
    // 1. Cộng dồn thời gian xem cho session này
    long totalWatched = redisTemplate.opsForValue().increment(sessionKey, 10); // +10 giây
    
    // 2. Check xem đã tính view chưa & có bị trùng trong 30p không
    if (totalWatched >= 30 && !redisTemplate.hasKey(viewKey)) {
        
        // 3. Đánh dấu đã tính view cho user này (trong 30p tới ko tính nữa)
        redisTemplate.opsForValue().set(viewKey, "counted", Duration.ofMinutes(30));
        
        // 4. Tăng bộ đếm view trong Redis (Buffer)
        redisTemplate.opsForValue().increment("video_pending_views:" + req.getVideoId(), 1);
    }
}

// Scheduled Task (Chạy mỗi phút)
@Scheduled(fixedRate = 60000)
public void flushViewsToDb() {
    // Scan các key "video_pending_views:*"
    // Gom lại thành batch update SQL
    // UPDATE videos SET view_count = view_count + ? WHERE id = ?
}

```

**Kết luận:**

* Dùng **Heartbeat** từ Client.
* Dùng **Redis** để lọc trùng và đệm (buffer).
* **Batch Update** xuống SQL.
* MinIO chỉ để lưu file, đừng lôi nó vào việc đếm view, sai logic đấy bác!