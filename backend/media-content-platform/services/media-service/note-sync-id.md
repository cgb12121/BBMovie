Vấn đề của bác nằm ở tư duy **"Tuần tự" (Sequential)**: Phải có cái này xong mới có cái kia.
Trong kiến trúc bất đồng bộ (Async Architecture), chúng ta phải tư duy theo kiểu **"Placeholder" (Giữ chỗ)** hay còn gọi là chiến thuật **"Bộ khung xương"**.

Bác không cần đợi transcode, cũng không cần đợi upload xong. Bác chỉ cần **cái ID** để định danh thôi.

Đây là giải pháp **"Draft & Link" (Tạo nháp và Liên kết)** chuẩn chỉ cho trường hợp này:

### 1. Quy trình UX/Backend chuẩn (Không bắt user đợi)

Hãy tưởng tượng luồng đi như Youtube Studio:

#### Bước 1: Khởi tạo "Bộ khung" (Draft Movie)

Ngay khi User bấm nút "Upload Video", chưa cần chọn file, hoặc vừa chọn file xong:

1. **Frontend** gọi `POST /api/movies/draft`.
2. **Movie Service** tạo một record rỗng:
* `id`: `movie-101` (Sinh UUID ngay lúc này).
* `status`: `DRAFT`.
* `title`: "Untitled Project".


3. Trả về `movie-101` cho Frontend.

#### Bước 2: Xin slot Upload (Lấy File ID)

1. **Frontend** gọi `POST /api/upload/presign`.
2. **Upload Service** sinh ra:
* `file_id`: `file-555` (Lưu vào DB Upload với trạng thái `PENDING`).
* `presign_url`: `minio.com/bucket/raw/file-555.mp4`.


3. Trả về cho Frontend.

#### Bước 3: "Phép nối" (Sync ID) - Quan trọng nhất 🔗

Ngay khi có `file-555`, **Frontend** gọi ngay lập tức về **Movie Service** (chạy ngầm, không cần user bấm Save):

* `PATCH /api/movies/movie-101`
* Body: `{ "file_id": "file-555" }`.

👉 **Lúc này:**

* **Movie Service** đã biết: "À, tao đang giữ chỗ cho cái file 555".
* **Upload Service** biết: "Tao có cái file 555 sắp được up".
* User vẫn đang ung dung ngồi gõ Title, Description (Metadata).
* File đang được upload ầm ầm lên MinIO (Metadata chưa submit xong cũng không sao).

---

### 2. Xử lý các tình huống "Éo le" (Race Conditions)

Bây giờ chúng ta có 2 luồng chạy song song:

* **Luồng A (User):** Đang gõ Title, Description -> Bấm Submit.
* **Luồng B (System):** Upload xong -> MinIO bắn Event -> Transcode -> Done.

#### Tình huống 1: User gõ chậm, Transcode xong trước 🐢

User đang mải nghĩ Title hay, chưa bấm "Publish". Nhưng Transcode Worker đã chạy xong và bắn event `TranscodeCompleted(file-555)`.

* **Movie Service** nghe event:
* Tìm DB thấy `movie-101` đang link với `file-555`.
* Update trạng thái nội bộ: `is_video_ready = true`.
* Kiểm tra trạng thái phim: Vẫn là `DRAFT` (do User chưa bấm Publish).
* -> **Kết luận:** Chỉ đánh dấu video đã sẵn sàng, **chưa bắn event cho Search Service**.


* **Sau đó User bấm "Publish":**
* Frontend gửi Title/Desc lên.
* Movie Service save metadata.
* Movie Service check thấy `is_video_ready == true`.
* -> **BÙM:** Chuyển status `PUBLISHED` -> Bắn event cho Search index ngay lập tức.



#### Tình huống 2: User gõ nhanh, Transcode chưa xong 🐇

User điền xong hết, bấm "Publish", nhưng file nặng quá Transcode chưa xong.

* **User bấm "Publish":**
* Movie Service save metadata.
* Check `is_video_ready`. Thấy `false`.
* Update status: `PROCESSING` (Đang xử lý).
* Trả về cho User: "Phim của bạn đang được xử lý, sẽ lên sóng sau ít phút".


* **Sau đó Transcode xong:**
* Movie Service nghe event `TranscodeCompleted`.
* Update `is_video_ready = true`.
* Chuyển status `PROCESSING` -> `PUBLISHED`.
* -> Bắn event cho Search index.



---

### 3. Kỹ thuật "Safety Net" (Lưới an toàn) 🕸️

Lỡ Frontend bị crash ngay sau khi lấy Presign URL mà chưa kịp gọi `PATCH` để sync ID thì sao? Lúc này `file-555` sẽ thành file mồ côi (Orphan) và `movie-101` sẽ không có file.

Để chắc chắn 100%, bác dùng kỹ thuật **User Metadata của S3/MinIO**:

1. Lúc Frontend xin Presign URL, gửi kèm `movieId: movie-101`.
2. **Upload Service** tạo Presign URL có kèm metadata header (ví dụ: `x-amz-meta-movie-id: movie-101`).
3. Khi **Transcode Worker** tải file từ MinIO về, nó đọc cái header này.
4. Khi Transcode xong, bắn event `TranscodeCompleted`:
* Payload: `{ "file_id": "file-555", "meta_movie_id": "movie-101" }`.


5. **Movie Service** nghe event:
* Nếu trong DB `movie-101` đã link `file-555` (nhờ Frontend PATCH) -> Tốt.
* Nếu chưa link (do lỗi mạng client) -> **Tự động link luôn** nhờ cái `meta_movie_id` gửi về.



### 🎯 Tóm lại

Để sync ID mà không phải chờ đợi:

1. **Tạo Movie ID trước (Draft).**
2. **Tạo File ID sau.**
3. **Link chúng nó lại ngay lập tức** (qua API PATCH ngầm).
4. Dùng **State Machine** (Draft/Processing/Published) để quản lý việc cái nào xong trước cái nào xong sau.

Cách này user trải nghiệm mượt như Sunsilk, vừa upload vừa điền thông tin, điền xong bấm Publish là xong (hoặc chờ tí tẹo).