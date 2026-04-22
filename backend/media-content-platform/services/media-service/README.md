# Movie Service - Source of truth for movie entity/domain

The Movie Service is the central hub for movie metadata management in the BBMovie platform. It acts as the "Source of Truth" for all movie-related information and coordinates with other services to provide a complete movie management experience.

## Architecture Overview

The service implements an event-driven architecture that coordinates with media-upload-service and transcode-worker to manage the complete movie lifecycle from creation to publication.

### Key Responsibilities
- **Movie Metadata Management**: Store and manage movie metadata (title, description, cast, etc.)
- **Lifecycle Coordination**: Manage movie states from DRAFT → PROCESSING → PUBLISHED
- **Event Handling**: Listen for transcode completion events and update movie status
- **File Linking**: Connect movie entities with their physical files
- **Event Publishing**: Publish movie published events for other services

## External Communications & Services

### 1. NATS Messaging System

The service communicates with other services through NATS for asynchronous processing notifications.

**Connection Details**:
- **Host**: `nats://localhost:4222` (configurable via `nats.url`)
- **Client Library**: io.nats:jnats (version 2.19.0)

**Subscriptions**:
- **Subject**: `media.status.update`
- Listens for media processing status updates from transcode-worker
- Updates movie status based on file processing results

**Publications**:
- **Subject**: `movie.published`
- Publishes movie published events when movies are ready for consumption
- Provides movie information to search and streaming services

### 2. MySQL Database

The service uses MySQL for persistent storage of movie metadata.

**Connection Details**:
- **URL**: Configured via `DATABASE_URL` environment variable
- **Username**: Configured via `DATABASE_USERNAME` environment variable
- **Password**: Configured via `DATABASE_PASSWORD` environment variable
- **Driver**: MySQL Connector/J

**Features**:
- JPA-based entity management
- Movie metadata storage with status tracking
- File-to-movie linking

### 3. JWT OAuth2 Authentication

The service validates JWT tokens for secure API access and implements user-based access control.

**Configuration**:
- Validates JWT tokens from the authentication service
- Implements role-based access control (ADMIN vs regular users)

## Movie Lifecycle

### Status Flow
1. **DRAFT** - Movie created with metadata, no file linked yet
2. **PROCESSING** - File linked to movie, waiting for transcoding
3. **PUBLISHED** - File processed and ready for streaming
4. **DELETED** - Movie soft-deleted from system
5. **ERROR** - Processing failed

### Coordination Flow
1. **Admin**: Creates movie metadata (status = DRAFT)
2. **Client**: Uploads file to media-upload-service
3. **Client**: Links file to movie via PATCH `/api/movies/{movieId}/link-file`
4. **Movie Service**: Updates status to PROCESSING
5. **Transcode Worker**: Processes file and sends status update to NATS
6. **Movie Service**: Receives status update, updates movie to PUBLISHED
7. **Movie Service**: Publishes `movie.published` event
8. **Search Service**: Indexes published movie

## API Endpoints

### Movie Management Endpoints
- **POST** `/api/movies`
  - Creates a new movie with metadata
  - Returns movie with status DRAFT
  - Requires authentication

- **GET** `/api/movies/{movieId}`
  - Retrieves a movie by its ID
  - Returns complete movie information

- **GET** `/api/movies`
  - Retrieves all movies
  - Supports filtering by status and title
  - Query parameters: `status`, `title`

- **PUT** `/api/movies/{movieId}`
  - Updates movie metadata
  - Requires authentication

- **DELETE** `/api/movies/{movieId}`
  - Soft deletes a movie (sets status to DELETED)
  - Requires authentication

### File Linking Endpoints
- **PATCH** `/api/movies/{movieId}/link-file?fileId={fileId}`
  - Links a file from media-upload-service to a movie
  - Updates movie status to PROCESSING
  - Requires authentication

- **PATCH** `/api/movies/{movieId}/status?status={status}`
  - Manually updates movie status (ADMIN only)
  - Can be used for administrative purposes

## Database Schema

### movies table
- `id` (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- `movie_id` (VARCHAR, UNIQUE, NOT NULL) - External movie identifier
- `title` (VARCHAR, NOT NULL) - Movie title
- `description` (TEXT) - Movie description
- `director` (VARCHAR) - Movie director
- `cast` (VARCHAR) - Movie cast
- `duration` (INT) - Duration in minutes
- `genre` (VARCHAR) - Movie genre
- `release_date` (VARCHAR) - Release date
- `poster_url` (VARCHAR) - URL to movie poster
- `trailer_url` (VARCHAR) - URL to movie trailer
- `file_id` (VARCHAR) - ID of the linked file from media-upload-service
- `status` (VARCHAR) - Current status (DRAFT, PROCESSING, PUBLISHED, DELETED, ERROR)
- `file_path` (VARCHAR) - Path to the processed file in storage
- `created_at` (TIMESTAMP) - Creation timestamp
- `updated_at` (TIMESTAMP) - Last update timestamp

## Configuration

### Environment Variables
- `DATABASE_URL` - MySQL database URL
- `DATABASE_USERNAME` - Database username
- `DATABASE_PASSWORD` - Database password
- `NATS_URL` - NATS server URL (default: nats://localhost:4222)
- `NATS_TRANSCODE_SUBJECT` - NATS subject for transcode events (default: media.status.update)
- `NATS_MOVIE_PUBLISHED_SUBJECT` - NATS subject for movie published events (default: movie.published)
- `EUREKA_SERVER_URL` - Eureka server URL (default: http://localhost:8761/eureka/)

### Application Properties
```
server.port: 8083
spring.application.name: movie-service
```

## Running the Service

Execute the following command:

```
mvn spring-boot:run
```

The service will start on port 8083 and register with Eureka.