* **Movie Service:** Là cái "Mặt tiền" (Storefront). Phục vụ User xem phim, tìm kiếm, comment, rating. Nó nắm giữ **Business Logic**.
* **Media Upload Service:** Là cái "Kho hàng" (Warehouse). Chỉ phục vụ Admin/Internal System để quản lý tài sản vật lý (files). Nó nắm giữ **Technical Logic** (S3, MinIO, Transcode).

Tuy nhiên, về cái luồng **DELETE (Xóa)**, bác đang tư duy theo hướng:
`Admin vào Kho xóa File -> Kho báo cho Mặt tiền gỡ phim xuống`.

👉 Cách này không sai, nhưng trong thực tế người ta thường làm **ngược lại** để đảm bảo an toàn dữ liệu và logic nghiệp vụ.

Tại sao nên làm ngược lại (`Movie Service` ra lệnh xóa)? Hãy xem phân tích dưới đây:

---

### 1. Luồng xóa chuẩn: Từ "Nghiệp vụ" xuống "Vật lý" (Top-Down) ⬇️

Bình thường, Admin sẽ thao tác trên CMS của **Movie Service**. Họ muốn "Xóa phim Mai" (Logic), chứ họ ít khi quan tâm "Xóa file `video_mai_1080p.mp4`" (Vật lý).

**Quy trình đề xuất:**

1. **Bước 1 (Soft Delete - Movie Service):**
* Admin bấm "Xóa phim" trên CMS.
* **Movie Service:** Đánh dấu `status = DELETED` trong DB của nó (Soft delete). Phim biến mất khỏi trang chủ/search ngay lập tức.
* *Tại sao Soft Delete?* Để lỡ tay xóa nhầm còn khôi phục được. Chưa đụng gì đến file gốc cả.


2. **Bước 2 (Hard Delete - Async Event):**
* Sau 30 ngày (hoặc nếu Admin chọn "Xóa vĩnh viễn"), **Movie Service** xóa record thật.
* 🚀 **Bắn Event:** `MoviePermanentDeletedEvent { fileIds: ["file-123", "file-456"] }`.


3. **Bước 3 (Cleanup - Media Upload Service):**
* **Media Upload Service** nghe event này.
* Nó cầm list `fileIds` -> Gọi MinIO/S3 xóa object -> Xóa record trong DB của nó.



👉 **Ưu điểm:** `Movie Service` là **Source of Truth**. Nó quyết định khi nào file hết giá trị sử dụng thì mới lệnh cho thằng kho vứt đi.

---

### 2. Luồng xóa mà bác hỏi: Từ "Kho" lên "Mặt tiền" (Bottom-Up) ⬆️

Trường hợp bác hỏi: *"Admin xóa file trong Media Upload Service thì bắn event cho Movie xóa theo"* vẫn xảy ra, nhưng nó là **Trường hợp ngoại lệ (Edge Case)**, ví dụ:

* Bị kiện bản quyền (DMCA) nên nhà cung cấp Cloud (AWS) xóa file của bác.
* Admin kỹ thuật vào dọn dẹp ổ cứng thủ công.

Lúc này, đúng là bác cần bắn event ngược:

1. **Media Upload Service:** Xóa file `abc.mp4`.
2. 🚀 **Bắn Event:** `FilePhysicalDeletedEvent { fileId: "555" }`.
3. **Movie Service:** Nghe event -> Tìm xem phim nào đang link vào `fileId: 555` -> Update status phim đó thành `ERROR/MISSING_FILE` (chứ đừng tự động xóa phim, để Admin vào check lại source khác upload bù).

---

### 3. Vấn đề "Update" file 🔄

Bác nói đúng, Media Upload Service "chắc chỉ có xóa thôi chứ không update gì nhiều".
Vì trong thế giới Streaming, **Update File = Upload File Mới**.

Nếu phim bị lỗi sub hoặc chất lượng kém, Admin sẽ:

1. Upload file mới lên (tạo ra `file_id` mới ở Media Service).
2. Sang Movie Service, trỏ cái phim cũ vào `file_id` mới.
3. Cái `file_id` cũ trở thành **"File mồ côi" (Orphaned File)** - không ai dùng nữa.

### 4. Chiến thuật dọn rác (Garbage Collection) 🧹

Để hệ thống hoàn hảo, bác cần một cơ chế dọn dẹp các "File mồ côi" ở **Media Upload Service** (những file upload lên mà user bỏ dở không save, hoặc file cũ bị thay thế).

**Cronjob tại Media Upload Service (Chạy mỗi đêm):**

1. Quét các file trong DB có `created_at < 24h trước` mà trạng thái vẫn là `TEMP` (chưa được confirm sử dụng).
2. Hoặc (nếu bác làm kỹ): Call API sang Movie Service hỏi *"Ê, list file ID này có ai đang dùng không?"* (Cách này hơi tight coupling, nên hạn chế).
3. Tốt nhất là dùng cơ chế **"Confirmation"**:
* Upload xong -> File ở trạng thái `TEMP`.
* Khi Movie Service link file xong -> Bắn event `FileLinkedEvent`.
* Media Service nghe event -> Chuyển file sang `PERMANENT`.
* Cronjob chỉ xóa những thằng `TEMP` quá hạn.



### 🎯 Tóm lại

Mô hình bác đang build rất ổn:

* **Movie Service (Logic):** Quản lý vòng đời (Lifecycle) của phim. Quyết định **XÓA**.
* **Media Upload Service (Physical):** Chỉ là thằng osin giữ đồ. Nghe lệnh xóa thì xóa.
* **Trường hợp hy hữu:** Nếu file mất trước -> Báo ngược lại để Movie Service treo biển "Bảo trì".