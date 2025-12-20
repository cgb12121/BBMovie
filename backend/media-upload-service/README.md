# Media Upload Service

The Media Upload Service is responsible for handling media file uploads for the BBMovie platform. It provides secure upload capabilities with presigned URLs, manages media file metadata, and integrates with external services for processing and storage.

## Architecture Overview

The service implements a secure upload system that generates presigned URLs for direct client-to-MinIO uploads. It maintains metadata in a MySQL database and communicates with other services via NATS messaging for media processing workflows.

## External Communications & Services

### 1. MinIO Object Storage

The service stores and manages media content in MinIO, an S3-compatible object storage system.

**Connection Details**:
- **Endpoint**: Configured via `MINIO_API_URL` environment variable
- **Authentication**: Access key and secret key from configuration
- **Client Library**: io.minio:minio (version 8.5.7)

**Buckets Used**:
- `bbmovie-raw` - Stores raw uploaded files
- `bbmovie-hls` - Stores HLS transcoded files for streaming
- `bbmovie-secure` - Stores encryption keys for secure streaming
- `bbmovie-public` - Stores public media files (avatars, posters)

**Features**:
- Presigned URL generation for secure client uploads
- Download URL generation with time-limited access
- File deletion with cascade deletion for related files
- Metadata storage with custom S3 metadata

### 2. NATS Messaging System

The service communicates with other services through NATS for asynchronous processing notifications.

**Connection Details**:
- **Host**: `nats://localhost:4222` (configurable via `nats.url`)
- **Client Library**: io.nats:jnats (version 2.19.0)

**Subscriptions**:
- **Subject**: `media.status.update`
- Listens for media processing status updates from workers
- Updates file status in database based on processing results
- Handles validation, rejection, and failure states

### 3. MySQL Database

The service uses MySQL for persistent storage of media file metadata.

**Connection Details**:
- **URL**: Configured via `DATABASE_URL` environment variable
- **Username**: Configured via `DATABASE_USERNAME` environment variable
- **Password**: Configured via `DATABASE_PASSWORD` environment variable
- **Driver**: MySQL Connector/J

**Features**:
- JPA-based entity management
- Liquibase for database migrations
- File metadata storage with status tracking
- User ownership and access control

### 4. JWT OAuth2 Authentication

The service validates JWT tokens for secure API access and implements user-based access control.

**Configuration**:
- Validates JWT tokens from the authentication service
- Extracts user ID and role information from JWT claims
- Implements role-based access control (ADMIN vs regular users)

**Security Features**:
- Upload initiation requires authentication
- Download access control based on file ownership
- Administrative functions restricted to ADMIN role

## API Endpoints

### Upload Endpoints
- **POST** `/upload/init`
  - Initializes a new file upload
  - Generates presigned URL for direct client upload to MinIO
  - Requires authentication and file metadata
  - Returns upload ID, object key, and presigned URL

- **GET** `/upload/files/{uploadId}/url`
  - Generates a time-limited download URL for a file
  - Requires authentication
  - Access control based on file ownership or ADMIN role

### Management Endpoints
- **GET** `/management/files`
  - Lists all media files (ADMIN only)
  - Supports filtering and pagination
  - Returns detailed file information

- **DELETE** `/management/files/{uploadId}`
  - Deletes a media file (ADMIN only)
  - Performs cascade deletion of related files
  - Updates database status to DELETED

- **GET** `/management/files/my-files`
  - Lists files owned by the current user
  - Supports filtering and pagination
  - Requires authentication

- **DELETE** `/management/files/my-files/{uploadId}`
  - Deletes a file owned by the current user
  - Requires authentication
  - Prevents deletion of other users' files

## File Management and Status Flow

### Upload Process
1. **INITIATED** - Upload request created, presigned URL generated
2. **UPLOADED** - File uploaded to MinIO, awaiting processing
3. **VALIDATED** - File processed and validated by worker
4. **READY** - File is ready for streaming/downloading
5. **REJECTED** - File failed validation, deleted from storage

### File Purposes
- **MOVIE_SOURCE** - Movie source files for transcoding
- **MOVIE_TRAILER** - Movie trailer files
- **USER_AVATAR** - User profile images
- **MOVIE_POSTER** - Movie poster images

### Bucket Organization
```
bbmovie-raw/ (raw uploads)
├── users/
│   └── avatars/{uploadId}.{ext}
└── movies/
    ├── posters/{uploadId}.{ext}
    ├── trailers/{uploadId}/{filename}
    └── sources/{uploadId}/{filename}

bbmovie-hls/ (streaming files)
└── movies/{movieId}/
    ├── master.m3u8
    ├── {resolution}/
    │   ├── playlist.m3u8
    │   └── segment_*.ts
    └── thumbnail.jpg

bbmovie-secure/ (encryption keys)
└── movies/{movieId}/{resolution}/
    └── *.key

bbmovie-public/ (public files)
├── users/avatars/{uploadId}.{ext}
└── movies/posters/{uploadId}.{ext}
```

## Security Features

- Presigned URL generation with time limits (1 hour)
- User-based access control for file operations
- Role-based administrative functions
- File checksum validation
- Automatic cleanup of orphaned files after 24 hours
- Cascade deletion for related content

## Configuration

### Environment Variables
- `DATABASE_URL` - MySQL database URL
- `DATABASE_USERNAME` - Database username
- `DATABASE_PASSWORD` - Database password
- `MINIO_API_URL` - MinIO server URL
- `MINIO_ACCESS_KEY` - MinIO access key
- `MINIO_SECRET_KEY` - MinIO secret key
- `NATS_URL` - NATS server URL (default: nats://localhost:4222)

### Application Properties
```
server.port: 6969
spring.application.name: media-upload-service
minio.bucket.movie-raw: bbmovie-raw
eureka.client.enabled: false
```

## Running the Service

Execute the following command:

```
.\run.bat
```

The service will start on port 6969.