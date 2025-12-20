# Media Streaming Service

The Media Streaming Service is responsible for serving HLS (HTTP Live Streaming) content for the BBMovie platform. It provides secure streaming capabilities with tier-based access control and integrates with MinIO for object storage.

## Architecture Overview

The service implements a secure HLS streaming system that serves media content from MinIO object storage. It includes JWT-based authentication and authorization to control access to different quality levels based on user subscription tiers.

## External Communications & Services

### 1. MinIO Object Storage

The service retrieves media content from MinIO, an S3-compatible object storage system.

**Connection Details**:
- **Endpoint**: Configured via `MINIO_API_URL` environment variable
- **Authentication**: Access key and secret key from configuration
- **Client Library**: io.minio:minio (version 8.5.7)

**Buckets Used**:
- `bbmovie-hls` - Stores HLS playlist files (master.m3u8, resolution playlists)
- `bbmovie-secure` - Stores encryption keys for secure streaming

**Features**:
- Object retrieval for HLS playlist files
- Secure key access for encrypted streams
- UUID-based movie ID organization in storage paths

### 2. JWT OAuth2 Authentication

The service validates JWT tokens for secure API access and implements tier-based content filtering.

**Configuration**:
- **JWK Endpoint**: Configured via `jose.jwk.endpoint` property
- Validates JWT tokens from the authentication service
- Extracts subscription tier information from JWT claims
- Implements ABAC (Attribute-Based Access Control) based on subscription tier

**Security Features**:
- Master playlist filtering based on user tier (FREE, STANDARD, PREMIUM)
- Quality-based access control (e.g., FREE users limited to 480p)
- Secure key access control for encrypted streams

## HLS Streaming Implementation

### Stream Architecture
- **Master Playlist**: `/api/stream/{movieId}/master.m3u8`
- **Resolution Playlists**: `/api/stream/{movieId}/{resolution}/playlist.m3u8`
- **Encryption Keys**: `/api/stream/keys/{movieId}/{resolution}/{keyFile}`

### Supported Resolutions
- 144p, 240p, 360p, 480p, 720p, 1080p, 1440p, 2160p (4K), 4080p (8K)

### Tier-Based Filtering
- **FREE Tier**: Limited to lower resolutions (≤ 480p)
- **STANDARD Tier**: Up to 1080p resolution
- **PREMIUM Tier**: Full resolution access including 4K

## API Endpoints

### Streaming Endpoints
- **GET** `/api/stream/{movieId}/master.m3u8`
  - Retrieves the master playlist for a movie
  - Filters resolutions based on user subscription tier
  - Requires valid JWT authentication

- **GET** `/api/stream/{movieId}/{resolution}/playlist.m3u8`
  - Retrieves resolution-specific playlist
  - Supports resolutions: 144p, 240p, 360p, 480p, 720p, 1080p, 1440p, 2160p, 4080p
  - Requires valid JWT authentication

- **GET** `/api/stream/keys/{movieId}/{resolution}/{keyFile}`
  - Retrieves AES encryption keys for secure streaming
  - Supports key files matching pattern: `^key_\\d+\\.key$`
  - Requires valid JWT authentication

### Security Features
- All endpoints require JWT authentication
- Path parameter validation using regex patterns
- Access control based on subscription tier
- Secure key access with additional validation

## Configuration

### Environment Variables
- `MINIO_API_URL` - MinIO server URL
- `MINIO_ACCESS_KEY` - MinIO access key
- `MINIO_SECRET_KEY` - MinIO secret key
- `JOSE_JWK_ENDPOINT` - JWT JWK endpoint URL

### Application Properties
```
server.port: 1205
spring.application.name: media-streaming-service
minio.bucket.hls: bbmovie-hls
minio.bucket.secure: bbmovie-secure
```

## Monetization Strategy

The service implements a tier-based monetization model:

| Tier | Resolution Limit | Features |
|------|------------------|----------|
| FREE | Max 480p | Limited content access |
| STANDARD | Max 1080p | Full content library |
| PREMIUM | Original/4K | Full content + early access |

## Running the Service

Execute the following command:

```
.\run.bat
```

The service will start on port 1205 and provide a test streaming interface at the root path.