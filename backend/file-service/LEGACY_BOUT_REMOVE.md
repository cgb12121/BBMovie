# File Service (Legacy)

The File Service is a legacy component responsible for handling file uploads, storage, and streaming for the BBMovie platform. It supports multiple storage backends, file validation, video transcoding, and integrates with various external services. **Note: This service is marked for removal in future versions or will be repurposed to store images only.**

## Architecture Overview

The service is built with Spring WebFlux for reactive programming, supports multiple storage strategies (Cloudinary and local), and integrates with external services for file processing and security. It uses NATS JetStream for event-driven communication and JWT-based authentication for security.

## External Communications & Services

### 1. Cloudinary Storage Service

The service can upload and store files to Cloudinary's cloud storage platform.

**API Endpoint**: `https://api.cloudinary.com/`
**Authentication**:
- Cloud Name, API Key, and API Secret from configuration
- Uses `cloudinary.cloud-name`, `cloudinary.api-key`, and `cloudinary.api-secret` environment variables

**Features**:
- File upload with public ID assignment
- Access mode set to authenticated
- Content type and file size metadata extraction
- File deletion by public ID

**Dependencies**:
- `com.cloudinary:cloudinary-core:2.2.0`
- `com.cloudinary:cloudinary-http5:2.2.0`

### 2. NATS JetStream Messaging System

The service communicates with other services through NATS JetStream, a high-performance messaging system with streaming capabilities.

**Connection Details**:
- **Host**: `nats://localhost:4222`
- **Client Library**: io.nats:jnats (version 2.19.0)
- **Connection Name**: `file-service`

**Resilience Features**:
- Infinite reconnection attempts (`maxReconnects(-1)`)
- Exponential backoff retry mechanism (2s → 4s → 8s → ... capped at 30s)
- Connection timeout: 5 seconds
- Reconnect wait: 10 seconds
- Ping interval: 30 seconds

**JetStream Configuration**:
- Uses JetStream for persistent message storage and delivery guarantees
- Events published with subjects for file upload notifications
- Reactive publisher implementation for non-blocking operations

**Dependencies**:
- `io.nats:jnats:2.19.0`
- `io.github.resilience4j:resilience4j-retry:2.2.0`

### 3. ClamAV Antivirus Service

The service integrates with ClamAV for virus scanning of uploaded files.

**Connection Details**:
- **Host**: `localhost:3310` (default, configurable via `clamav.host` and `clamav.port`)
- **Client Library**: clamav-client (version 2.1.2)

**Features**:
- File virus scanning before processing
- Configurable host and port settings

**Dependencies**:
- `xyz.capybara:clamav-client:2.1.2`

### 4. Eureka Service Discovery

The service registers itself with Eureka for service discovery in the microservices architecture.

**Connection Details**:
- **Eureka Server**: `http://localhost:8761/eureka/`
- **Service Name**: `bbmovie-upload-file`

**Configuration**:
- Service registration enabled
- Registry fetching enabled
- Heartbeat and renewal intervals configured

**Dependencies**:
- `org.springframework.cloud:spring-cloud-starter-netflix-eureka-client`

### 5. JWT OAuth2 Authentication

The service validates JWT tokens for secure API access.

**Configuration**:
- **JWK Set URI**: `http://localhost:8761/.well-known/jwks.json`
- Validates JWT tokens from the authentication service
- Secured endpoints require valid authentication tokens

### 6. FFmpeg Video Processing

The service uses FFmpeg for video transcoding and metadata extraction.

**Configuration**:
- **FFmpeg Path**: Configured via `FFMPEG_PATH` environment variable
- **FFprobe Path**: Configured via `FFPROBE_PATH` environment variable

**Features**:
- Video transcoding to multiple resolutions (240p, 360p, 480p, 720p, 1080p)
- Video metadata extraction (width, height, duration, etc.)
- Multi-resolution video streaming support

**Dependencies**:
- `net.bramp.ffmpeg:ffmpeg:0.7.0`

### 7. Apache Tika Content Detection

The service uses Apache Tika for content type detection and validation.

**Features**:
- File content type detection
- File format validation
- Content inspection for security

**Dependencies**:
- `org.apache.tika:tika-core:2.9.2`
- `org.apache.tika:tika-parsers-standard-package:2.9.2`

### 8. R2DBC MySQL Reactive Database

The service uses R2DBC for reactive database access to MySQL.

**Configuration**:
- **Database URL**: Configured via `DATABASE_URL` environment variable
- **Username**: Configured via `DATABASE_USERNAME` environment variable
- **Password**: Configured via `DATABASE_PASSWORD` environment variable

**Features**:
- Reactive database operations
- File asset metadata storage
- Temp file record management
- Change Data Capture (CDC) support

**Dependencies**:
- `io.asyncer:r2dbc-mysql:1.4.1`
- `org.hibernate.reactive:hibernate-reactive-core:4.1.2.Final`
- `org.springframework.boot:spring-boot-starter-data-r2dbc`

## API Endpoints

### Image Upload
- **POST** `/image/upload`
- Accepts multipart form data with file and metadata
- Processes image uploads with validation

### Video Upload
- **POST** `/video/upload`
- Accepts multipart form data with file and metadata
- Processes video uploads with transcoding

### Video Streaming
- **GET** `/stream/{movieId}?quality={quality}&range={range}`
- Streams video files by movie ID
- Supports quality selection (240p, 360p, 480p, 720p, 1080p)
- Supports range requests for partial content delivery

### Admin Endpoints
- Various admin endpoints for file management and system monitoring

## Storage Strategies

The service implements a strategy pattern for file storage with multiple options:

1. **Cloudinary Storage** - Cloud-based storage with CDN
2. **Local Storage** - File system storage on the server

Configuration allows selecting the storage strategy based on upload metadata.

## Security Features

- File validation before processing
- Virus scanning with ClamAV
- JWT-based authentication for API endpoints
- Content type validation with Apache Tika
- Secure file upload with temporary storage and cleanup

## Configuration

### Environment Variables
- `UPLOAD_DIR` - Directory for file uploads
- `DATABASE_URL` - MySQL database URL
- `DATABASE_USERNAME` - Database username
- `DATABASE_PASSWORD` - Database password
- `TEMP_PATH` - Directory for temporary file storage
- `FFMPEG_PATH` - Path to FFmpeg executable
- `FFPROBE_PATH` - Path to FFprobe executable
- `CLOUDINARY_CLOUD_NAME` - Cloudinary cloud name
- `CLOUDINARY_API_KEY` - Cloudinary API key
- `CLOUDINARY_API_SECRET` - Cloudinary API secret
- `CLAMAV_HOST` - ClamAV server host (default: localhost)
- `CLAMAV_PORT` - ClamAV server port (default: 3310)

### Application Properties
```
server.port: 8081
spring.application.name: bbmovie-upload-file
```

## Running the Service

Execute one of the following commands:

Windows:
```
.\run.bat
```

Linux/macOS:
```
./run.sh
```

## Legacy Status

This service is marked as legacy and will be removed in future versions or repurposed to store images only. The functionality may be migrated to other services in the platform.