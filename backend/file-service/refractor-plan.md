Đây là bản kế hoạch giải cứu (Rescue Plan) chi tiết được tổng hợp lại thành file Markdown `.md`. Bác có thể lưu lại vào repo để làm kim chỉ nam refactor.

Mục tiêu cốt lõi: **Biến File-Service từ "Nồi lẩu thập cẩm Blocking" thành "Hệ thống xử lý Async sự kiện"**.

-----

````markdown
# FILE-SERVICE REFACTORING PLAN
**Status:** Draft
**Objective:** Decouple Upload logic, Remove Blocking I/O, Fix NATS Event Logic.

---

## 1. The Problem (Current State)
Hệ thống hiện tại đang bị **Tight Coupling** và **Blocking I/O** nghiêm trọng:
1.  **God Method:** `FileUploadService.orchestrateUpload` làm quá nhiều việc (Validate -> Transcode -> Store -> DB -> NATS).
2.  **Blocking Transcode:** FFmpeg chạy trên thread `boundedElastic` trong luồng upload chính. User phải chờ transcode xong mới nhận được response.
3.  **Spaghetti Logic:** Logic `if (isVideo)` trộn lẫn với logic lưu trữ.
4.  **Race Condition:** Xóa file vật lý trước khi xóa DB.
5.  **NATS Spam:** Bắn event vô tội vạ, không lọc `EntityType`.

---

## 2. Target Architecture (Event-Driven Pipeline)

Chúng ta sẽ chuyển sang mô hình **"Upload First, Process Later"**.

```mermaid
graph TD
    User -->|1. Upload| UploadAPI
    UploadAPI -->|2. Save Raw File| Storage(MinIO/Local)
    UploadAPI -->|3. Save DB (PENDING)| DB
    UploadAPI -->|4. Return 202 Accepted| User
    
    UploadAPI -.->|5. Async Event: FILE_UPLOADED| InternalEventHandler
    
    InternalEventHandler -->|6. Filter: Is Video?| Transcoder(FFmpeg/Rust)
    Transcoder -->|7. Update DB (Active)| DB
    
    Transcoder -.->|8. Filter: Is Movie?| NatsPublisher
    NatsPublisher -->|9. Publish: MOVIE_INDEX_UPDATE| NATS_JetStream
    NATS_JetStream --> Elasticsearch
````

-----

## 3\. Implementation Steps

### Bước 1: Chuẩn hóa Interface (Strategy Pattern)

Xóa bỏ các logic `if/else` hardcode. Tạo interface xử lý cho từng loại file.

```java
// core/FileProcessor.java
public interface FileProcessor {
    boolean supports(String mimeType);
    // Trả về list các file phái sinh (ví dụ: thumbnail, video các độ phân giải)
    Mono<List<ProcessedFile>> process(Path tempFile);
}
```

### Bước 2: Refactor `FileUploadService` (Chỉ làm nhiệm vụ Upload)

Service này giờ chỉ làm đúng 3 việc: **Lưu file gốc -\> Lưu DB -\> Bắn Event nội bộ**. Không Transcode, không NATS ra ngoài.

```java
// service/FileUploadService.java
public Mono<FileAsset> upload(FilePart filePart, UploadMetadata metadata) {
    return tempFileService.save(filePart) // 1. Lưu Temp
        .flatMap(tempPath -> 
            validationService.validate(tempPath) // 2. Validate (Virus/Mime)
            .then(storageFactory.getStrategy(metadata.getStorage()).store(tempPath)) // 3. Lưu Storage
            .flatMap(uploadResult -> {
                // 4. Lưu DB (Status = PENDING hoặc PROCESSING)
                FileAsset asset = mapToAsset(metadata, uploadResult);
                asset.setStatus(FileStatus.PROCESSING); 
                return fileAssetRepository.save(asset);
            })
            // 5. Bắn Event nội bộ (ApplicationEventPublisher của Spring)
            // Để thằng khác lo việc Transcode/Index
            .doOnSuccess(asset -> {
                applicationEventPublisher.publishEvent(new FileUploadedEvent(this, asset, tempPath));
            })
        );
}
```

### Bước 3: Xử lý Async (The Worker)

Tạo một Listener để hứng cái `FileUploadedEvent` ở trên. Đây là nơi tách luồng.

```java
// listener/FileProcessingListener.java
@Component
public class FileProcessingListener {

    @Async // Chạy thread riêng, không block luồng upload
    @EventListener
    public void handleFileUpload(FileUploadedEvent event) {
        FileAsset asset = event.getAsset();
        
        // 1. Xử lý chuyên sâu (Transcode Video / Resize Image)
        // Gọi TranscoderService hoặc Rust Worker tại đây
        processFileBasedOnType(asset, event.getTempPath());
        
        // 2. Logic NATS (Yêu cầu của bác)
        // Chỉ bắn event cập nhật Index nếu đây là Phim
        if (asset.getEntityType() == EntityType.MOVIE) {
             natsPublisher.publish(new MovieIndexUpdateEvent(asset.getMovieId()));
        }
        
        // 3. Update trạng thái cuối cùng
        asset.setStatus(FileStatus.ACTIVE);
        repository.save(asset);
    }
}
```

### Bước 4: Sửa lỗi `VideoTranscoderService` (Part 1 Issue)

Vứt bỏ cái `PrioritizedTaskExecutor` tự chế đi. Dùng `@Async` của Spring hoặc đẩy sang Rust Worker.

Nếu vẫn dùng Java, hãy sửa lại để trả về `CompletableFuture` thay vì block `boundedElastic`.

```
// service/VideoTranscoderService.java
@Async("videoExecutor") // Config thread pool riêng trong AppConfig
public CompletableFuture<List<Path>> transcodeAsync(Path input, ...) {
    // Logic FFmpeg cũ, nhưng giờ chạy trong thread pool riêng biệt
    // Không ảnh hưởng đến WebFlux loop
}
```

### Bước 5: Fix lỗi xóa Data (`AdminService`)

Sửa lại thứ tự xóa để đảm bảo Consistency.

```java
// service/AdminService.java
@Transactional
public Mono<Void> deleteFileAsset(Long id) {
    return fileAssetRepository.findById(id)
        .flatMap(asset -> 
            // 1. Xóa trong DB trước
            fileAssetRepository.delete(asset)
            // 2. Sau đó mới trigger xóa file vật lý (Fire-and-forget)
            .doOnSuccess(v -> {
                cleanupService.deletePhysicalFile(asset)
                    .subscribeOn(Schedulers.boundedElastic()) // Chạy ngầm
                    .subscribe();
            })
        );
}
```

-----

## 4\. NATS Configuration Strategy

Chỉ bắn NATS khi cần thiết để tránh spam 503.

**Quy tắc:**

1.  **Upload Ảnh/Doc:** Không bắn NATS (Trừ khi cần OCR search).
2.  **Upload Video:**
    * Lúc upload xong (Raw): Không bắn.
    * Lúc Transcode xong: Bắn event `VIDEO_READY`.
3.  **Index Update:** Chỉ khi `EntityType == MOVIE` -\> Bắn event `INDEX_MOVIE`.

<!-- end list -->

```java
// publisher/NatsEventPublisher.java
public void publishMovieUpdate(Long movieId) {
    // Topic này Consumer là Elasticsearch Service
    var event = new IndexEvent("MOVIE", movieId);
    jetStream.publishAsync("search-events", json(event));
}
```

-----

## 5\. Summary of Benefits

| Feature              | Old Implementation      | Refactored                           |
|:---------------------|:------------------------|:-------------------------------------|
| **User Experience**  | Chờ Transcode (Lâu)     | Upload xong là xong (Nhanh)          |
| **System Stability** | Transcode ăn hết Thread | Transcode chạy Async/Worker riêng    |
| **Data Integrity**   | Xóa file lỗi -\> DB rác | Xóa DB -\> File rác (Dọn sau dễ hơn) |
| **Complexity**       | God Class (Spaghetti)   | Chain of Responsibility (Rõ ràng)    |
| **Elasticsearch**    | Spam Indexing           | Chỉ Index khi là Movie & Xử lý xong  |

-----

```

### Lời khuyên cuối
Bác in cái này ra, hoặc mở 2 màn hình.
1.  **Tách Service trước:** Copy đoạn logic transcode ra class riêng.
2.  **Sửa luồng Upload:** Bỏ hết logic transcode trong `orchestrateUpload`, thay bằng `publishEvent`.
3.  **Viết Listener:** Nhét logic transcode và NATS check vào đây.

Làm xong 3 bước này là code bác sạch bong kin kít! 🧹
```