# AI Assistant Service

## Overview

The AI Assistant Service is a Spring Boot application that provides intelligent chat capabilities for the BBMovie platform. It enables users to interact with an AI assistant for movie-related queries, supports file attachments, and integrates with various AI models for enhanced user experiences.

### Key Features

- Real-time chat with AI assistant
- File attachment processing and content extraction
- Session management with archiving capabilities
- Integration with multiple AI models (Ollama, custom models)
- RAG (Retrieval Augmented Generation) for movie context
- Human-in-the-loop (HITL) approval system
- Comprehensive audit logging and monitoring
- External service integration for file processing

## Architecture

The service follows a reactive programming model using Spring WebFlux and Project Reactor for high-performance, non-blocking operations. It utilizes reactive database access through R2DBC and integrates with various external services for enhanced functionality.

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker and Docker Compose (for containerized deployment)
- MySQL 8.0+
- Redis Stack
- Elasticsearch 8.9+
- Ollama (for AI model serving)

## Environment Variables

The service requires the following environment variables to be configured:

```bash
# Application Settings
SERVER_PORT=8888
SPRING_PROFILES_ACTIVE=prod

# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bbmovie_ai_chat
DB_USERNAME=bbmovie_user
DB_ROOT_PASSWORD=your_secure_password

# Authentication Service
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://auth-service/.well-known/jwks.json

# AI Configuration
AI_MODE=NORMAL
AI_MODEL=mistral
OLLAMA_URL=http://ollama:11434/api
EMBEDDING_MODEL=all-minilm
ELASTICSEARCH_MOVIES_INDEX=movies
ELASTICSEARCH_CHAT_HISTORY_INDEX=chat_history
EMBEDDING_DIMENSION=384
EMBEDDING_FIELD=embedding

# External Services
FILE_SERVICE_URL=http://file-service:8081
RUST_AI_SERVICE_URL=http://rust-ai-refinery:8686

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=redis_password
REDIS_DATABASE=0

# Elasticsearch Configuration
ELASTICSEARCH_HOST=elasticsearch
ELASTICSEARCH_PORT=9200
ELASTICSEARCH_SCHEME=http
```

## Installation

### Local Development

1. Clone the repository:
```bash
git clone <repository-url>
cd BBMovie/backend/ai-service
```

2. Configure environment variables in `.env` file

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

### Docker Deployment

1. Ensure Docker and Docker Compose are installed

2. Build and start the services:
```bash
docker-compose up --build
```

## API Documentation

### Base URL
`http://localhost:8888/api/v1`

### Authentication
All endpoints require JWT authentication in the Authorization header:
```
Authorization: Bearer <JWT_TOKEN>
```

### Endpoints

#### Chat Endpoints

##### Stream Chat Messages
- **POST** `/chat/{sessionId}`
- **Description**: Send a message to the AI assistant and receive a streaming response
- **Content-Type**: `application/json`
- **Produces**: `text/event-stream`
- **Parameters**:
  - `sessionId` (path): UUID of the chat session
- **Request Body**:
```json
{
  "message": "Hello, how can you help me?",
  "aiMode": "NORMAL",
  "assistantType": "MOVIE_ASSISTANT",
  "attachments": [
    {
      "id": 123,
      "filename": "movie_info.pdf",
      "url": "http://file-service/files/123",
      "fileType": "application/pdf"
    }
  ],
  "internalApprovalToken": "optional_approval_token"
}
```
- **Response**: Server-sent events with `ChatStreamChunk` objects

#### Session Management Endpoints

##### Get Active Sessions
- **GET** `/sessions`
- **Description**: Retrieve active chat sessions for the authenticated user
- **Query Parameters**:
  - `cursor` (optional): Pagination cursor
  - `size` (optional, default: 15): Number of sessions per page
- **Response**: Paginated list of active sessions

##### Create Session
- **POST** `/sessions`
- **Description**: Create a new chat session
- **Request Body**:
```json
{
  "sessionName": "My Movie Discussion"
}
```
- **Response**: Created session details

##### Update Session Name
- **PATCH** `/sessions/{sessionId}/name`
- **Description**: Update the name of a chat session
- **Request Body**:
```json
{
  "newName": "Updated Session Name"
}
```
- **Response**: Updated session details

##### Archive/Unarchive Session
- **PATCH** `/sessions/{sessionId}/archived`
- **Description**: Toggle archived status of a session
- **Request Body**:
```json
{
  "archived": true
}
```
- **Response**: Updated session details

##### Get Archived Sessions
- **GET** `/sessions/archived`
- **Description**: Retrieve archived sessions for the authenticated user
- **Query Parameters**:
  - `cursor` (optional): Pagination cursor
  - `size` (optional, default: 15): Number of sessions per page
- **Response**: Paginated list of archived sessions

##### Delete Session
- **DELETE** `/sessions/{sessionId}`
- **Description**: Permanently delete a chat session
- **Response**: Success message

##### Delete Archived Sessions
- **DELETE** `/sessions/archived`
- **Description**: Delete multiple archived sessions
- **Request Body**:
```json
{
  "sessionIds": ["uuid1", "uuid2", "uuid3"]
}
```
- **Response**: Success message

#### Message Endpoints

##### Get Messages
- **GET** `/messages/{sessionId}`
- **Description**: Retrieve messages for a specific session
- **Query Parameters**:
  - `cursor` (optional): Pagination cursor
  - `size` (optional, default: 20): Number of messages per page
- **Response**: Paginated list of chat messages

#### Admin Endpoints

##### Get Token Usage Dashboard
- **GET** `/admin/dashboard/token-usage`
- **Description**: Retrieve token usage statistics for administrative purposes
- **Response**: Token usage dashboard data

##### Get Audit Trail
- **GET** `/admin/audit/query`
- **Description**: Query audit trail for AI interactions
- **Query Parameters**:
  - `interactionType` (optional): Type of interaction to filter
  - `sessionId` (optional): Specific session to filter
  - `startDate` (optional): Start date for filtering (ISO 8601 format)
  - `endDate` (optional): End date for filtering (ISO 8601 format)
- **Response**: Stream of audit records

#### Approval Endpoints

##### Handle Approval Decision
- **POST** `/chat/{sessionId}/approve/{requestId}`
- **Description**: Handle human approval decisions for AI responses
- **Content-Type**: `application/json`
- **Parameters**:
  - `sessionId` (path): UUID of the chat session
  - `requestId` (path): ID of the request requiring approval
- **Request Body**:
```json
{
  "approved": true,
  "feedback": "Optional feedback for the decision"
}
```
- **Response**: Streaming response with `ChatStreamChunk` objects

## External Service Dependencies

### File Service
- **URL**: Configured via `FILE_SERVICE_URL` environment variable
- **Purpose**: Handles file uploads and storage
- **Integration**: Used to confirm file usage after processing
- **Endpoint**: `PUT /internal/files/{id}/confirm`

### Rust AI Context Refinery Service
- **URL**: Configured via `RUST_AI_SERVICE_URL` environment variable
- **Purpose**: Advanced file content processing and context refinement
- **Integration**: Batch processing of file attachments
- **Endpoint**: `POST /api/process-batch`

### Authentication Service
- **JWKS URI**: Configured via `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI`
- **Purpose**: JWT token validation and user authentication
- **Integration**: Validates incoming JWT tokens in authorization headers

### Database (MySQL)
- **Connection**: Configured via `DB_*` environment variables
- **Purpose**: Persistent storage for chat sessions, messages, and user data
- **Technology**: R2DBC for reactive database access

### Redis
- **Connection**: Configured via `REDIS_*` environment variables
- **Purpose**: Caching and temporary data storage
- **Integration**: Used for session management and temporary data

### Elasticsearch
- **Connection**: Configured via `ELASTICSEARCH_*` environment variables
- **Purpose**: Vector search for RAG (Retrieval Augmented Generation)
- **Indices**:
  - Movies index for movie context retrieval
  - Chat history index for conversation context

### Ollama
- **URL**: Configured via `OLLAMA_URL` environment variable
- **Purpose**: AI model serving for chat completions
- **Integration**: Provides AI model inference capabilities

## Technology Stack

- **Framework**: Spring Boot 3.5.4
- **Language**: Java 21
- **Reactive Programming**: Project Reactor
- **Web Framework**: Spring WebFlux
- **Security**: Spring Security OAuth2 Resource Server
- **Database Access**: R2DBC with MySQL
- **ORM**: JOOQ
- **JSON Processing**: Jackson
- **AI Framework**: LangChain4j, Spring AI
- **Document Processing**: Apache POI (for Word documents)
- **Logging**: Custom RGB Logger
- **Configuration**: Spring Configuration Properties

## Configuration

### AI Modes
- `FAST`: Quick responses with minimal processing
- `NORMAL`: Balanced quality and speed
- `ADVANCED`: Higher quality responses with more processing

### Assistant Types
- `MOVIE_ASSISTANT`: Specialized for movie-related queries
- Other assistant types may be available depending on configuration

## Health Checks

The application provides Spring Boot Actuator endpoints for health monitoring:
- **Health**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Info**: `GET /actuator/info`

## Logging

The service uses a custom RGB logger for colorful, structured logging. Log levels can be configured in `application.properties`:
- `DEBUG`: Detailed debug information
- `INFO`: General operational information
- `WARN`: Warning conditions
- `ERROR`: Error conditions

## Testing

### Unit Tests
Run unit tests using Maven:
```bash
mvn test
```

### Integration Tests
Run integration tests using Maven:
```bash
mvn verify
```

## Deployment

### Docker Compose
The service includes a `docker-compose.yml` file for easy deployment of the entire stack including dependent services.

### Kubernetes
For production deployments, consider using Kubernetes with the provided Docker image.

## Monitoring and Observability

- **Metrics**: Available through Spring Boot Actuator
- **Audit Trail**: Comprehensive logging of AI interactions
- **Performance**: Response time and token usage tracking
- **Error Tracking**: Detailed error logging and reporting

## Security

- **Authentication**: JWT-based authentication
- **Authorization**: Role-based access control
- **Data Protection**: Secure handling of user data and file uploads
- **API Keys**: Secure storage and handling of AI service credentials

## Troubleshooting

### Common Issues

1. **Database Connection Errors**: Verify database connection parameters and ensure MySQL is running
2. **AI Model Unavailable**: Check Ollama service and model availability
3. **File Processing Failures**: Verify file service connectivity and permissions
4. **Authentication Failures**: Check JWT configuration and auth service availability

### Logs
Check application logs for detailed error information:
```bash
tail -f logs/application.log
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the [LICENSE] license.

## Support

For support, please contact the development team or submit an issue in the repository.