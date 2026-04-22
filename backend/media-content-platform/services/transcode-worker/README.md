# Transcode Worker

The Transcode Worker is a private service that processes media files based on events published by NATS. It downloads raw files from MinIO, validates them, and performs transcoding or processing operations before uploading the results back to MinIO.

## Architecture Overview

The service operates as an event-driven worker that listens for MinIO object creation events via NATS. It processes different types of media files (videos, images) based on their upload purpose, validates them for security and format compliance, and uploads the processed results to appropriate MinIO buckets.

## External Communications & Services

### 1. NATS Messaging System

The service listens for events from NATS to trigger processing workflows.

**Connection Details**:
- **Host**: `nats://localhost:4222` (configurable via `nats.url`)
- **Client Library**: io.nats:jnats (version 2.19.0)

**Subscriptions**:
- **Subject**: `minio.events`
- Listens for MinIO object creation events (s3:ObjectCreated:*)
- Processes events based on metadata attached to uploaded files

**Publications**:
- **Subject**: `media.status.update`
- Publishes status updates for processed files (READY, REJECTED, FAILED)
- Provides feedback to other services about processing results

### 2. MinIO Object Storage

The service downloads raw files from and uploads processed files to MinIO storage.

**Connection Details**:
- **Endpoint**: Configured via `MINIO_API_URL` environment variable
- **Authentication**: Access key and secret key from configuration
- **Client Library**: io.minio:minio (version 8.5.7)

**Operations**:
- Downloads files from `bbmovie-raw` bucket for processing
- Uploads HLS transcoded files to `bbmovie-hls` bucket
- Uploads encryption keys to `bbmovie-secure` bucket
- Uploads processed images to `bbmovie-public` bucket

### 3. FFmpeg Processing

The service uses FFmpeg for video transcoding and metadata extraction.

**Configuration**:
- **FFmpeg Path**: Configured via `FFMPEG_PATH` environment variable
- **FFprobe Path**: Configured via `FFPROBE_PATH` environment variable

**Features**:
- Video transcoding to multiple resolutions (240p, 360p, 480p, 720p, 1080p, 1440p, 2160p)
- HLS (HTTP Live Streaming) format generation
- Video metadata extraction (duration, dimensions, bitrate, etc.)
- Adaptive bitrate streaming support

**Dependencies**:
- `net.bramp.ffmpeg:ffmpeg:0.7.0`

### 4. ClamAV Antivirus

The service integrates with ClamAV for malware scanning of uploaded files.

**Configuration**:
- **Host**: `localhost:3310` (default, configurable via configuration)
- **Client Library**: clamav-client (version 2.1.2)

**Features**:
- File malware scanning before processing
- Automatic rejection of infected files
- Integration with file validation pipeline

### 5. Apache Tika Validation

The service uses Apache Tika for content type detection and validation.

**Features**:
- File content type detection
- File format validation against allowed types per upload purpose
- Content inspection for security

**Dependencies**:
- `org.apache.tika:tika-core:2.9.2`
- `org.apache.tika:tika-parsers-standard-package:2.9.2`

## Processing Workflow

### Event Processing Flow
1. **Event Reception**: Receives MinIO object creation events from NATS
2. **Metadata Extraction**: Extracts upload purpose and upload ID from object metadata
3. **File Download**: Downloads the raw file from MinIO to temporary storage
4. **Validation**: Performs content type validation and malware scanning
5. **Processing**: Processes the file based on its upload purpose
6. **Upload**: Uploads processed files back to appropriate MinIO buckets
7. **Status Update**: Publishes status update to NATS

### Upload Purposes

The service handles different types of uploads:

**Video Processing (MOVIE_SOURCE, MOVIE_TRAILER)**:
- Downloads video file from MinIO
- Extracts metadata using FFprobe
- Transcodes to multiple resolutions using FFmpeg
- Generates HLS playlist files and segments
- Uploads to `bbmovie-hls` bucket with organized folder structure
- Uploads encryption keys to `bbmovie-secure` bucket

**Image Processing (USER_AVATAR, MOVIE_POSTER)**:
- Downloads image file from MinIO
- Generates different sizes/resolutions based on purpose
- Uploads to `bbmovie-public` bucket in appropriate folders

### File Validation
- Content type validation based on upload purpose
- Malware scanning using ClamAV
- Automatic rejection of invalid or infected files
- Status updates for rejected files

## Configuration

### Environment Variables
- `MINIO_API_URL` - MinIO server URL
- `MINIO_ACCESS_KEY` - MinIO access key
- `MINIO_SECRET_KEY` - MinIO secret key
- `FFMPEG_PATH` - Path to FFmpeg executable
- `FFPROBE_PATH` - Path to FFprobe executable
- `TEMP_DIR` - Directory for temporary file processing
- `NATS_URL` - NATS server URL (default: nats://localhost:4222)

### Application Properties
```
server.port: 9696
spring.application.name: transcode-worker
app.minio.public-hls-url: ${minio.url}/bbmovie-hls/movies
app.transcode.temp-dir: ${TEMP_DIR}
app.clamav.enabled: false
app.transcode.key-server-url: http://localhost:1205/api/stream
```

## Security Features

- Malware scanning with ClamAV before processing
- Content type validation based on upload purpose
- Automatic rejection of invalid files
- Secure temporary file handling with cleanup
- Virtual thread execution for isolated processing

## Running the Service

Execute the following command:

```
.\run.bat
```

The service will start on port 9696 and begin listening for NATS events.