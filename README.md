# BBMovie - High-Performance Distributed Video Streaming Platform

[![Java 21](https://img.shields.io/badge/Java-21%2B-blue)](https://www.oracle.com/java/technologies/downloads/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub stars](https://img.shields.io/github/stars/cgb12121/BBMovie?style=social)](https://github.com/cgb12121/BBMovie/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/cgb12121/BBMovie?style=social)](https://github.com/cgb12121/BBMovie/network/members)

**BBMovie** is a modern, scalable video streaming platform built as a microservices architecture. It supports HLS adaptive streaming, AI-powered features like semantic search and multimodal processing, event-driven media transcoding, and secure payment integrations.

This project started as a personal endeavor in March 2025 to explore cutting-edge backend technologies, including Java virtual threads, Rust for performance-critical tasks, and local AI integrations.

> **Note**: This is a solo-developed project (still in active development). It's not a production Netflix clone—yet! 🚀

## Demo (Coming Soon)

<!-- Placeholder for future demo -->
<!-- Once you have screenshots or a short demo video/GIF: -->
<!-- 
<p align="center">
  <img src="docs/screenshots/homepage.png" alt="Homepage" width="800"/>
</p>

<p align="center">
  <video width="800" controls>
    <source src="docs/demo/demo.mp4" type="video/mp4">
    Your browser does not support the video tag.
  </video>
</p>
-->

## Key Features

- **HLS Adaptive Streaming**: Smooth video playback with FFmpeg-based transcoding.
- **Event-Driven Architecture**: NATS JetStream pipeline for scalable media processing (10-25x throughput improvement over synchronous approaches).
- **Concurrency Optimization**: Java 21 Virtual Threads + Weighted Semaphore Scheduler → CPU utilization boosted from ~30% to 90%.
- **AI-Powered Search & Processing**:
  - Semantic search with kNN in Qdrant vector database.
  - Local LLM integration via Ollama + Langchain4j.
  - Multimodal features (e.g., Whisper transcription, OCR via Rust workers).
  - Human-in-the-Loop approval system with low latency.
- **Efficient Storage**: MinIO for distributed object storage; Qdrant replaced Elasticsearch (90% memory reduction).
- **Payments**: Secure integration with VNPAY, MoMo, and Stripe (HMAC-SHA256 + async callbacks).
- **Microservices Design**: 10+ services with Spring Cloud (Eureka discovery, Gateway), evolving from monolith (documented via ADRs).

## Architecture Overview

```
[Frontend (React/Vite?)] ←→ [API Gateway (Spring Cloud Gateway)]
                             ↓
          [Eureka Service Discovery]
                             ↓
┌─────────────────┬─────────────────┬─────────────────┐
│ Media Service   │ AI Service      │ Payment Service │  ... (10+ services)
│ - FFmpeg        │ - Ollama        │ - VNPAY/MoMo    │
│ - HLS           │ - Qdrant        │ - Stripe        │
└─────────────────┴─────────────────┴─────────────────┘
                             ↓
                [NATS JetStream (Event Bus)]
                             ↓
          [MinIO (Object Storage)] [MySQL] [Redis] [Qdrant]
```

- **Rust Workers**: Native modules for CPU-intensive tasks (Whisper, OCR) to avoid JNI leaks.
- **Documentation**: Architecture Decision Records (ADRs) in `/docs/adr/` track major trade-offs.

## Tech Stack

| Category              | Technologies                                                                 |
|-----------------------|------------------------------------------------------------------------------|
| **Core**              | Java 21, Spring Boot 3, Quarkus, Spring WebFlux, Reactor                     |
| **Microservices**     | Spring Cloud (Eureka, Gateway), Spring Security, Hibernate/JPA              |
| **Messaging**         | NATS JetStream                                                               |
| **Databases**         | MySQL, Redis, Qdrant (vector DB), MinIO (S3-compatible)                      |
| **Media**             | FFmpeg, HLS, Tika, ClamAV                                                    |
| **AI**                | Ollama, Langchain4j, Rust native workers (Whisper, OCR)                      |
| **DevOps**            | Docker, Maven, GitHub Actions (CI/CD)                                        |
| **Other**             | Git, IntelliJ IDEA, Postman                                                  |

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose (for local setup)
- Maven
- NATS server (or use Docker)
- MinIO, MySQL, Redis, Qdrant (via Docker Compose recommended)

### Local Development

1. Clone the repo:
   ```bash
   git clone https://github.com/cgb12121/BBMovie.git
   cd BBMovie
   ```

2. Start infrastructure (MinIO, MySQL, Redis, Qdrant, NATS):
   ```bash
   docker-compose -f docker/infra.yml up -d
   ```

3. Build and run services (example for a single service):
   ```bash
   cd services/media-service
   mvn spring-boot:run
   ```

   Repeat for other services or use profiles/scripts (TBD).

4. Access API Gateway at `http://localhost:8080` (configure ports as needed).

### Configuration

- Environment variables or `application.yml` per service.
- See individual service READMEs for details.

## Project Structure

```
BBMovie/
├── services/               # Individual microservices
│   ├── api-gateway/
│   ├── media-service/
│   ├── ai-service/
│   └── ...
├── rust-workers/           # Rust native modules
├── docs/
│   ├── adr/                # Architecture Decision Records
│   └── diagrams/           # Architecture diagrams
├── docker/                 # Dockerfiles & Compose files
└── README.md
```

## Contributing

Contributions welcome! Feel free to open issues or PRs.

1. Fork the repo
2. Create a feature branch
3. Commit changes
4. Push and open a Pull Request

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

## Author

Bùi Thái Bảo  
Backend Engineer | Hanoi, Vietnam  
Email: quanbaoyb@gmail.com  
GitHub: [cgb12121](https://github.com/cgb12121)  
LinkedIn: [bui-thai-bao](https://www.linkedin.com/in/bui-thai-bao)

---

**Star the repo if you find it useful!** ⭐  
Feedback and suggestions appreciated. This project is a learning journey—help make it better!