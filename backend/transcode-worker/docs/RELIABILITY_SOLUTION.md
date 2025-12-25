# Reliability Solution: Persistent Queue with Delayed ACK

## 🚨 Problem với giải pháp trước

### Vấn đề 1: ACK ngay → Mất message nếu lỗi
```
NATS → Consumer → Queue → ACK ✅
                    ↓
              Worker Thread
                    ↓
            [Transcode lỗi] ❌
                    ↓
            Message đã mất! 💀
```

### Vấn đề 2: Queue trong RAM → Mất khi server crash
```
Server crash → Queue trong RAM → Mất hết jobs 💀
```

---

## ✅ Giải pháp: Persistent Queue + Delayed ACK

### Kiến trúc mới

```
┌─────────────┐
│ NATS Server │
└──────┬──────┘
       │ Message
       ▼
┌─────────────────────────────────┐
│ NATS Consumer                    │
│ 1. Parse message                 │
│ 2. Create TranscodeJob           │
│ 3. Persist to DB (QUEUED)        │
│ 4. Add to in-memory queue        │
│ 5. ACK NATS ✅                   │
└──────┬───────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│ Database (Persistent)            │
│ - Job status: QUEUED            │
│ - Recovery on restart            │
└─────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│ BlockingQueue (In-Memory)        │
│ - Fast processing                │
│ - Lost on crash (OK, DB backup)  │
└──────┬───────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│ Worker Threads                   │
│ 1. Take job from queue           │
│ 2. Update DB: PROCESSING         │
│ 3. scheduler.acquire()           │
│ 4. Transcode                     │
│ 5. Update DB: COMPLETED/FAILED   │
│ 6. Release scheduler             │
└─────────────────────────────────┘
```

---

## 📋 Implementation Plan

### Option 1: Database-Backed Queue (Recommended)

**Pros:**
- ✅ Persistent - Không mất khi crash
- ✅ Recovery mechanism
- ✅ Status tracking
- ✅ Retry logic

**Cons:**
- ⚠️ Cần database (JPA/Hibernate)
- ⚠️ Slightly slower (DB writes)

### Option 2: Hybrid (In-Memory + DB Backup)

**Pros:**
- ✅ Fast (in-memory queue)
- ✅ Persistent (DB backup)
- ✅ Best of both worlds

**Cons:**
- ⚠️ More complex
- ⚠️ Need to sync queue and DB

---

## 🎯 Recommended: Option 1 - Database-Backed Queue

### Step 1: Create TranscodeJob Entity

```java
@Entity
@Table(name = "transcode_jobs")
public class TranscodeJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String uploadId;
    private String bucket;
    private String key;
    
    @Enumerated(EnumType.STRING)
    private UploadPurpose purpose;
    
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String recordJson; // Store JsonNode as JSON string
    
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    private String errorMessage;
    private Integer retryCount;
    
    // Getters, setters...
}

public enum JobStatus {
    QUEUED,      // In queue, waiting
    PROCESSING,  // Currently processing
    COMPLETED,   // Success
    FAILED,      // Failed (can retry)
    CANCELLED    // Cancelled
}
```

### Step 2: Create TranscodeJobRepository

```java
@Repository
public interface TranscodeJobRepository extends JpaRepository<TranscodeJob, UUID> {
    List<TranscodeJob> findByStatusOrderByCreatedAtAsc(JobStatus status);
    List<TranscodeJob> findByStatusAndRetryCountLessThan(JobStatus status, int maxRetries);
}
```

### Step 3: Create TranscodeJobQueue Service

```java
@Service
@Slf4j
public class TranscodeJobQueue {
    
    private final TranscodeJobRepository repository;
    private final BlockingQueue<TranscodeJob> inMemoryQueue;
    private final ObjectMapper objectMapper;
    
    public TranscodeJobQueue(
            TranscodeJobRepository repository,
            @Value("${app.transcode.queue.capacity:10}") int capacity,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.inMemoryQueue = new LinkedBlockingQueue<>(capacity);
        this.objectMapper = objectMapper;
    }
    
    /**
     * Adds job to queue (persists to DB first, then in-memory queue)
     */
    @Transactional
    public TranscodeJob enqueue(JsonNode record, String bucket, String key, 
                                UploadPurpose purpose, String uploadId) {
        // 1. Create and persist job to DB
        TranscodeJob job = new TranscodeJob();
        job.setUploadId(uploadId);
        job.setBucket(bucket);
        job.setKey(key);
        job.setPurpose(purpose);
        job.setStatus(JobStatus.QUEUED);
        job.setRecordJson(record.toString());
        job.setCreatedAt(LocalDateTime.now());
        job.setRetryCount(0);
        
        job = repository.save(job);
        log.info("Job persisted to DB: {}", job.getId());
        
        // 2. Try to add to in-memory queue (non-blocking)
        if (!inMemoryQueue.offer(job)) {
            log.warn("In-memory queue full, job {} will be picked up by recovery", job.getId());
            // Job is still in DB, recovery will pick it up
        }
        
        return job;
    }
    
    /**
     * Takes job from queue (blocks until available)
     */
    public TranscodeJob take() throws InterruptedException {
        return inMemoryQueue.take();
    }
    
    /**
     * Recovery: Load QUEUED jobs from DB on startup
     */
    @PostConstruct
    public void recoverJobs() {
        List<TranscodeJob> queuedJobs = repository.findByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);
        List<TranscodeJob> processingJobs = repository.findByStatusOrderByCreatedAtAsc(JobStatus.PROCESSING);
        
        log.info("Recovering {} QUEUED and {} PROCESSING jobs", queuedJobs.size(), processingJobs.size());
        
        // Re-queue QUEUED jobs
        for (TranscodeJob job : queuedJobs) {
            if (inMemoryQueue.offer(job)) {
                log.info("Recovered job: {}", job.getId());
            }
        }
        
        // Reset PROCESSING jobs to QUEUED (assume crash during processing)
        for (TranscodeJob job : processingJobs) {
            job.setStatus(JobStatus.QUEUED);
            job.setRetryCount(job.getRetryCount() + 1);
            repository.save(job);
            if (inMemoryQueue.offer(job)) {
                log.info("Reset and recovered job: {}", job.getId());
            }
        }
    }
}
```

### Step 4: Refactor MediaEventConsumer

```java
private void handleMessage(Message msg) {
    try {
        String json = new String(msg.getData(), StandardCharsets.UTF_8);
        log.info("Received MinIO event: {}", json);
        JsonNode rootNode = objectMapper.readTree(json);

        List<TranscodeJob> jobs = new ArrayList<>();
        
        // Parse and create jobs
        for (JsonNode record : extractRecords(rootNode)) {
            String bucket = record.path("s3").path("bucket").path("name").asText();
            String key = record.path("s3").path("object").path("key").asText();
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            
            // Get metadata from MinIO
            StatObjectResponse stat = minioClient.statObject(...);
            Map<String, String> meta = stat.userMetadata();
            UploadPurpose purpose = extractPurpose(meta);
            String uploadId = extractUploadId(meta);
            
            // Enqueue job (persists to DB)
            TranscodeJob job = jobQueue.enqueue(record, bucket, key, purpose, uploadId);
            jobs.add(job);
        }
        
        // ACK after all jobs are persisted
        msg.ack();
        log.info("ACKed message, {} jobs enqueued", jobs.size());
        
    } catch (Exception e) {
        log.error("Error handling message", e);
        msg.nak(); // Retry on error
    }
}
```

### Step 5: Worker Threads

```java
@PostConstruct
public void startWorkers() {
    int workerThreads = calculateWorkerThreads();
    
    for (int i = 0; i < workerThreads; i++) {
        workerExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Block until job available
                    TranscodeJob job = jobQueue.take();
                    
                    // Update status to PROCESSING
                    job.setStatus(JobStatus.PROCESSING);
                    job.setStartedAt(LocalDateTime.now());
                    jobRepository.save(job);
                    
                    // Process job (can block at scheduler.acquire())
                    processJob(job);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error processing job", e);
                    // Job status will be updated in processJob
                }
            }
        });
    }
}

private void processJob(TranscodeJob job) {
    try {
        // Parse record from JSON
        JsonNode record = objectMapper.readTree(job.getRecordJson());
        
        // Process (calls scheduler.acquire() internally)
        processRecord(record, job);
        
        // Update status to COMPLETED
        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
        
    } catch (Exception e) {
        log.error("Job {} failed", job.getId(), e);
        
        // Update status to FAILED
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(e.getMessage());
        job.setRetryCount(job.getRetryCount() + 1);
        jobRepository.save(job);
        
        // Optional: Retry logic
        if (job.getRetryCount() < MAX_RETRIES) {
            // Re-queue for retry
            job.setStatus(JobStatus.QUEUED);
            jobRepository.save(job);
            jobQueue.offer(job);
        }
    }
}
```

---

## 🔄 Recovery on Startup

```java
@PostConstruct
public void init() {
    // 1. Subscribe to NATS
    Dispatcher dispatcher = natsConnection.createDispatcher(this::handleMessage);
    dispatcher.subscribe("minio.events");
    
    // 2. Recover jobs from DB
    jobQueue.recoverJobs();
    
    // 3. Start worker threads
    startWorkers();
}
```

---

## 📊 Benefits

### ✅ Reliability
- **Persistent**: Jobs survive server crashes
- **Recovery**: Auto-recover on startup
- **Status tracking**: Know exactly what's happening
- **Retry logic**: Failed jobs can be retried

### ✅ No Message Loss
- **ACK after persist**: Message only ACKed after job saved to DB
- **Error handling**: Failed jobs marked in DB
- **Recovery**: Can reprocess failed jobs

### ✅ Still Fast
- **In-memory queue**: Fast processing (not blocking on DB)
- **DB writes**: Only for persistence (async if needed)

---

## ⚙️ Configuration

```properties
# Queue capacity (in-memory)
app.transcode.queue.capacity=10

# Worker threads
app.transcode.worker.threads=0  # 0 = auto

# Retry configuration
app.transcode.retry.max-attempts=3
app.transcode.retry.delay-seconds=60
```

---

## 🎯 Trade-offs

| Aspect | ACK Immediately | ACK After Persist |
|--------|------------------|-------------------|
| **Speed** | ⚡ Fastest | ⚡ Fast (DB write) |
| **Reliability** | ❌ Lose on crash | ✅ Survive crash |
| **Complexity** | ✅ Simple | ⚠️ Need DB |
| **Message Loss** | ❌ Yes | ✅ No |

**Recommendation**: **ACK After Persist** - Worth the small complexity for reliability.

---

## 🚀 Next Steps

1. Add JPA dependency (if not already)
2. Create `TranscodeJob` entity
3. Create `TranscodeJobRepository`
4. Create `TranscodeJobQueue` service
5. Refactor `MediaEventConsumer`
6. Add recovery mechanism
7. Test with crash scenarios

