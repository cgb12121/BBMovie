# BBMovie: Advanced AI-Driven Features Architecture
**Document Status:** Draft / Planning
**Author:** Gemini 3 pro (System Architect)
**Core Stack:** Java Spring Boot (Business), Rust Axum (AI Compute Worker), Elasticsearch (Vector DB), Kafka/NATS (Event Bus).

---

## 1. Voice-Activated Semantic Search (Voice-to-Action)

### 1.1. Problem Statement
Người dùng muốn tìm kiếm phim hoặc thực hiện tác vụ (navigating) bằng giọng nói tự nhiên thay vì gõ keyword chính xác.
* *Legacy approach:* Keyword search (gõ "hanh dong") -> kết quả hạn chế.
* *Goal:* User nói "Tìm phim hành động nào mà nhân vật chính bị mất trí nhớ" -> Hệ thống hiểu ngữ nghĩa và trả về kết quả chính xác (e.g., *Memento*, *The Bourne Identity*).

### 1.2. Architecture Flow
**Pattern:** *Audio Processing Pipeline & Hybrid Search.*

1.  **Ingestion:** Frontend (React/Mobile) ghi âm và gửi file `.wav/.mp3` (hoặc stream) lên `File-Service`.
2.  **Processing (Rust Worker):**
    * `AI-Service` gửi URL file audio sang `Rust Worker`.
    * Rust sử dụng **Whisper (C++ Binding/Candle)** để Transcribe ra text.
    * *Optimization:* Sử dụng cơ chế Resampling (16kHz) và VAD (Voice Activity Detection) để loại bỏ khoảng lặng, tối ưu tốc độ xử lý (<1s cho lệnh ngắn).
3.  **Understanding (LLM & Tool Calling):**
    * Text được gửi vào LLM (Qwen/Llama) với System Prompt định nghĩa các **Tools**:
        * `search_movie_by_plot(query: string)`
        * `search_movie_by_actor(name: string)`
        * `Maps_to_page(page_name: string)`
    * LLM phân tích ý định (Intent Classification) và quyết định gọi Tool.
4.  **Retrieval (Hybrid Search):**
    * Nếu là tìm phim: `AI-Service` tạo Vector Embedding từ query -> Gọi Elasticsearch.
    * Sử dụng **Hybrid Search**: Kết hợp *KNN Vector Search* (ngữ nghĩa) và *BM25* (từ khóa) để tăng độ chính xác (`reciprocal_rank_fusion`).

### 1.3. Technical Highlights (Interview Focus)
* **Decoupling:** Tách việc xử lý Audio nặng nề sang Rust (CPU/Memory efficient) giúp Spring Boot không bị GC overhead.
* **Latency:** Sử dụng mô hình "Store First" kết hợp với Internal Network Download để giảm băng thông.
* **Fallbacks:** Nếu Rust overload, hệ thống trả về thông báo "Đang xử lý" và dùng Webhook/SSE để push kết quả sau (Async flow).

---

## 2. Personalized Recommendation System (User Persona Vector)

### 2.1. Problem Statement
Hệ thống hiện tại là "One size fits all". Cần cá nhân hóa trải nghiệm dựa trên hành vi người dùng.
* *Challenge:* Chưa có hệ thống tracking, dữ liệu phân tán.

### 2.2. Solution: Event-Driven User Profiling
Xây dựng hệ thống theo mô hình **Lambda Architecture** (hoặc đơn giản hóa là Near-Realtime Processing).

#### A. User Behavior Tracking (Event Collection)
Thêm **`Tracking-Service`** (hoặc module trong Gateway) để hứng sự kiện.
* Frontend bắn event qua WebSocket hoặc HTTP (Fire-and-forget).
* **Events:** `VIEW_DETAILS`, `WATCH_TRAILER` (weight: 1), `ADD_WATCHLIST` (weight: 3), `LIKE/RATE` (weight: 5).
* Đẩy events vào **Message Queue** (Kafka/NATS) để đảm bảo High Throughput, không block user.

#### B. User Vector Building (The Core)
Tạo **`Recommendation-Worker`** (Consumer):
1.  Lắng nghe event từ Queue.
2.  Lấy thông tin phim (Plot/Genre) tương ứng với event.
3.  **Vector Aggregation:**
    * Lấy Vector của phim vừa tương tác.
    * Cập nhật **User Vector** hiện tại bằng công thức *Moving Average* (Trung bình trượt) hoặc *Decay Factor* (Giảm trọng số sở thích cũ, ưu tiên sở thích mới).
    * `NewUserVec = (OldUserVec * 0.9) + (MovieVec * 0.1)`.
4.  Lưu User Vector vào Elasticsearch (hoặc Qdrant/Redis).

#### C. Serving Recommendations
Khi user vào trang chủ:
1.  Lấy User Vector từ DB.
2.  Query Elasticsearch (KNN Search) tìm các phim có vector *gần nhất* với User Vector.
3.  Filter bỏ các phim đã xem.

### 2.3. Technical Challenges
* **Cold Start:** User mới chưa có vector -> Fallback về "Trending" hoặc "Top Rated".
* **Data Drift:** Sở thích user thay đổi. Cần cơ chế Decay (giảm dần giá trị lịch sử) để AI không gợi ý mãi phim hoạt hình nếu user đã chuyển sang xem phim kinh dị.

---

## 3. Spoiler Shield & Toxic Filter (Content Moderation)

### 3.1. Problem Statement
User comment nội dung spoil phim hoặc toxic. Regex thông thường không bắt được ngữ nghĩa (ví dụ: spoil khéo, chửi thâm).

### 3.2. Architecture Flow
**Pattern:** *Async Moderation Queue.*

1.  **Submission:** User post comment -> API lưu vào DB với trạng thái `PENDING_REVIEW` (hoặc `VISIBLE` nhưng có cờ `UNVERIFIED`).
2.  **Analysis:**
    * Đẩy comment ID vào Queue.
    * **Rust Worker** (hoặc Spring AI) consume queue.
    * Prompt LLM: *"Phân tích bình luận sau: '{comment}'. Output JSON: { is_spoiler: boolean, is_toxic: boolean, reasoning: string }"*.
3.  **Decision:**
    * Nếu `is_toxic`: Update DB `status = HIDDEN`. Shadow ban user nếu cần.
    * Nếu `is_spoiler`: Update DB `is_spoiler = true`. Frontend hiển thị lớp phủ mờ (Blur overlay) + nút "Click to reveal".
    * Nếu sạch: `status = APPROVED`.

### 3.3. Optimization
* **Bloom Filter:** Dùng để check nhanh các từ khóa tục tĩu cơ bản trước khi gọi LLM (tiết kiệm chi phí token).
* **Batching:** Gom 10-20 comments để gửi LLM 1 lần thay vì gọi lẻ tẻ.

---

## 4. AI Data Analyst (Admin Assistant)

### 4.1. The Hard Problem (Distributed Data)
Hệ thống Microservices có Database riêng biệt (`AuthDB`, `MovieDB`, `PaymentDB`).
* **Sai lầm:** Ném schema của tất cả DB vào Context cho AI gen SQL.
    * *Risk:* Security (Lộ schema), Context limit (Schema quá to), Consistency (Join bảng giữa các DB vật lý khác nhau là không thể).

### 4.2. Solution: Centralized Analytics Service (The "Data Mesh" approach)

Thay vì để AI chọc vào DB nghiệp vụ (OLTP), ta xây dựng một tầng **Analytics (OLAP)**.

#### A. Data Synchronization (ETL/CDC)
* Sử dụng **Debezium** (CDC - Change Data Capture) hoặc bắn Event từ các Service (`OrderCreated`, `MovieViewed`) về một DB tập trung chuyên cho Analytics (ví dụ: **ClickHouse** hoặc một Schema riêng trong **PostgreSQL/MySQL**).
* DB này chứa dữ liệu đã được làm sạch và phi chuẩn hóa (Denormalized) -> Dễ query.

#### B. Semantic Layer (Lớp ngữ nghĩa)
Không expose tên bảng thô (`tbl_usr_01`) cho AI. Định nghĩa một file metadata (JSON/YAML) mô tả dữ liệu cho AI:
* `"total_revenue"`: Calculate from `orders` table where `status = PAID`.
* `"top_movies"`: Aggregate from `movie_views`.

#### C. AI Workflow (Text-to-SQL controlled)
1.  **Admin:** "Thống kê doanh thu tháng này theo thể loại phim".
2.  **AI Service:**
    * Load schema của **Analytics DB** (đã được rút gọn và an toàn).
    * Prompt: *"Generate SQL for MySQL Analytics DB based on request..."*
3.  **Query Validator (Safety Layer - Bắt buộc):**
    * Code Java chặn các từ khóa nguy hiểm: `DROP`, `DELETE`, `UPDATE`, `GRANT`.
    * Chỉ cho phép `SELECT`.
    * Thêm `LIMIT` nếu AI quên.
4.  **Execution:** Chạy SQL trên Analytics DB -> Trả về JSON -> AI vẽ biểu đồ hoặc tóm tắt text.

### 4.3. Interview Defense (Chống vặn)
* *Q: Tại sao không gọi API từng service?*
    * A: Vì bài toán Analytics cần `Aggregation` (SUM, COUNT, GROUP BY) trên tập dữ liệu lớn. Gọi API rồi tính toán trong memory của Java sẽ gây OOM và độ trễ cao.
* *Q: Data ở Analytics DB có realtime không?*
    * A: Chấp nhận độ trễ (Near-realtime) vài giây đến vài phút (Eventual Consistency), phù hợp với bài toán báo cáo Admin.

---

### Tổng kết Roadmap
1.  **Phase 1:** Implement **Voice Search**. (Tận dụng Rust Whisper sẵn có).
2.  **Phase 2:** Build **Tracking Service** & **User Vector** (Dễ, impact cao).
3.  **Phase 3:** Content Moderation (Async Queue).
4.  **Phase 4:** Analytics Service (Cần dựng thêm hạ tầng ETL/ClickHouse - Long term).