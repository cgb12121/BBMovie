# 📂 Media Deduplication Strategy & Architecture Refactor

**Date:** 2025-12-20
**Status:** Draft / Planning
**Module:** Media Upload Service & Transcode Worker

---

## 1. 🚩 Current Problems (Vấn đề hiện tại)

1.  **Mô hình 1:1 (One-to-One Mapping):**
    * Hiện tại: `1 User` sở hữu `1 File Record`.
    * Database không phân biệt được đâu là thông tin quản lý (User sở hữu) và đâu là file vật lý (S3 Object).
    * **Hệ quả:** Nếu 100 người upload cùng 1 bộ phim "Đào, Phở và Piano", hệ thống lưu 100 file trên MinIO, transcode 100 lần. -> **Lãng phí Storage & CPU.**

2.  **Missing Pre-transcode Check:**
    * Luồng hiện tại: `Upload -> MinIO Event -> Transcode`.
    * Transcode worker cứ thấy event là làm việc, không biết file này đã từng được xử lý hay chưa.
    * Chưa có cơ chế check Hash MD5/SHA256 để chặn việc transcode trùng lặp.

---

## 2. 🏗️ Solution Architecture (Giải pháp kiến trúc)

### 2.1. Database Schema Refactor (Quan trọng nhất)
Cần tách bảng để hỗ trợ quan hệ **1:N** (1 File vật lý - Nhiều User sở hữu).

#### Bảng `physical_files` (Lưu thông tin file gốc & hash)
*Đây là "Single Source of Truth" cho file vật lý.*
| Column | Type | Description |
| :--- | :--- | :--- |
| **id** | UUID (v7) | Primary Key. |
| **hash_sha256** | String | **UNIQUE INDEX**. Dùng để check duplicate. |
| `s3_bucket` | String | Bucket chứa file gốc (Raw). |
| `s3_object_key` | String | Đường dẫn file gốc. |
| `storage_size` | Long | Kích thước file. |
| `transcode_status`| Enum | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`. |
| `created_at` | Timestamp| |

#### Bảng `user_media_assets` (Lưu quyền sở hữu)
*Đây là bảng User nhìn thấy.*
| Column | Type | Description |
| :--- | :--- | :--- |
| **id** | UUID (v7) | Primary Key (Trả về cho Client/Movie Service). |
| **user_id** | UUID | Ai là người upload? |
| **physical_file_id**| UUID | FK trỏ sang bảng `physical_files`. |
| `original_filename`| String | Tên file user đặt lúc up (e.g., "my_video.mp4"). |
| `created_at` | Timestamp| |

---

### 2.2. The "Deduplication Flow" (Luồng xử lý)

Thay vì Transcode Worker tự chạy khi có Event MinIO, chúng ta sẽ để **Media Upload Service** làm "Gatekeeper" (Người gác cổng).

#### 🛑 Chiến thuật: "Check trước, làm sau"

1.  **Upload:** Client upload file lên MinIO.
2.  **Trigger:**
    * **Cách A (Nhanh):** Client upload xong gửi API `POST /upload/complete` kèm `file_path`.
    * **Cách B (Async):** Media Service nghe event `MinioObjectCreated` từ NATS.
3.  **Hashing (Tại Media Service hoặc Worker nhẹ):**
    * Tính Hash SHA256 của file vừa upload (Stream đọc từ MinIO hoặc tính từ Client gửi lên nếu tin tưởng).
4.  **Deduplication Logic:**

```mermaid
flowchart TD
    A[Start: File Uploaded] --> B{Calculate Hash SHA256}
    B --> C(Query DB: Find physical_files by Hash)
    
    C -- FOUND (Existed) --> D[Mapping Only]
    D --> D1[Create new record: user_media_assets]
    D1 --> D2[Link to EXISTING physical_file_id]
    D2 --> E[Return Success to Client]
    D2 --> F[Skip Transcode]
    
    C -- NOT FOUND (New File) --> G[New Processing]
    G --> G1[Create new record: physical_files]
    G --> G2[Create new record: user_media_assets]
    G --> H[Publish Event: START_TRANSCODE]
    H --> I[Transcode Worker picks up job]