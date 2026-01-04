* **Phase 1: The Monolith Foundation**
    * Built a functional MVP using monolithic Spring Boot architecture to validate core streaming features.
    * Implemented standard RESTful APIs for media upload and playback using MySQL and local file storage.
    * *Limitation:* Identified bottlenecks in scalability and single-point-of-failure risks during high-load simulations.

* **Phase 2: Microservices Migration (The "Lift & Shift" Challenge)**
    * Decomposed the monolith into 10+ microservices (Auth, Media, Payment, Streaming) using Spring Cloud Ecosystem (Eureka, Gateway).
    * **Payment Service Integration:** Implemented unified payment gateway supporting **5 payment providers** (PayPal, Stripe, VNPay, ZaloPay, MoMo) using Strategy Pattern for provider abstraction, enabling flexible payment processing with subscription management and automated billing.
    * **Challenge:** Encountered the "Distributed Monolith" anti-pattern where synchronous (blocking) code caused high latency and resource contention.
    * *Lesson:* Learned that splitting services without optimizing I/O models leads to **Head-of-Line Blocking** and inefficient resource usage.

* **Phase 3: Event-Driven Architecture & Performance Optimization (Current State)**
    * **Refactored Core Pipeline:** Designed a **3-Stage Event-Driven Pipeline** (Fetcher -> Prober -> Executor) using **NATS JetStream** to replace synchronous calls, eliminating thread blocking and achieving 10-25x throughput improvement.
    * **Concurrency Optimization:** Leveraged **Java 21 Virtual Threads** combined with **Weighted Semaphores** for resource management, increasing transcoding throughput by **200%** on limited hardware.
    * **Resource Optimization:**
        * Replaced Elasticsearch (JVM-heavy, 1GB+ RAM) with **Qdrant (Rust-based Vector DB, ~80MB RAM)** for semantic search, reducing memory footprint by **90%**.
        * Implemented **"Fast Probing"** strategy using MinIO Presigned URLs to inspect video metadata without full downloads, saving significant network bandwidth.
    * **AI Feature Integration:** Integrated AI-powered semantic search and multimodal content processing using Rust-based workers for CPU-intensive tasks, leveraging native memory management for optimal performance.