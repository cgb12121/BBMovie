# Gateway Service

The Gateway Service acts as the entry point for all client requests to the BBMovie microservices architecture. It routes requests to the appropriate backend services using Spring Cloud Gateway.

## Overview

The gateway provides:
- API routing to backend microservices
- Request/Response filtering and transformation
- Security and authentication layer
- Rate limiting and traffic management
- Cross-origin resource sharing (CORS) configuration

## Routing Configuration

The gateway routes incoming requests based on path patterns to the appropriate backend services.

### Authentication Service Routes

Routes requests to the authentication service for user authentication and session management.

- Path: `/api/auth/**` → Backend: `/auth/**` (auth service)
- Path: `/oauth2/**` → Backend: `/oauth2/**` (auth service)

### Device Management Routes

Handles device session and management operations.

- Path: `/api/device/**` → Backend: `/device/**` (auth service)

### Movie Service Routes (Public)

Routes public movie-related requests to the movie service.

- Path: `/api/public/**` → Backend: `/public/**` (movie service)

### Movie Service Routes

Routes movie and review related requests to the movie service.

- Path: `/api/movies/**` → Backend: `/**` (movie service)
- Path: `/api/reviews/**` → Backend: `/**` (movie service)

### User Service Routes

Routes user and payment-related requests to the user service.

- Path: `/api/users/**` → Backend: `/**` (user service)
- Path: `/api/payments/**` → Backend: `/**` (user service)

### Payment Service Routes

Handles payment-related operations.

- Path: `/api/payment/**` → Backend: `/api/payment/**` (payment service)

### Subscription Service Routes

Manages subscription-related operations.

- Path: `/api/subscription/**` → Backend: `/api/**` (payment service)
- Path: `/api/v1/subscription/**` → Backend: `/api/**` (payment service)

### Search Service Routes

Routes search-related requests to the search service.

- Path: `/api/search/**` → Backend: `/search/**` (search service)

### Watchlist Service Routes

Handles watchlist operations.

- Path: `/api/watchlist/**` → Backend: `/api/watchlist/**` (watchlist service)

### AI Assistant Service Routes

Routes AI assistant and chat-related requests.

- Path: `/api/v1/chat/**` → Backend: `/api/v1/chat/**` (AI service)
- Path: `/api/v1/sessions/**` → Backend: `/api/v1/sessions/**` (AI service)
- Path: `/api/v1/messages/**` → Backend: `/api/v1/messages/**` (AI service)
- Path: `/api/v1/admin/**` → Backend: `/api/v1/admin/**` (AI service)

### JWKS Route

Handles JSON Web Key Set requests for token validation.

- Path: `/.well-known/jwks.json` → Backend: `/.well-known/jwks.json` (auth service)

## Configuration Properties

### Server Configuration
- **Port**: `8765`

### CORS Configuration
- **Allowed Origins**: `http://localhost:5173` (frontend development server)
- **Allowed Methods**: `*` (all HTTP methods)
- **Allowed Headers**: `*` (all headers)
- **Credentials**: `true` (allow credentials)

### Retry Configuration
- **Retries**: 3
- **Statuses**: `BAD_GATEWAY`
- **Methods**: `GET, POST, PUT, DELETE`
- **Backoff**: Exponential with first backoff of 1s, max 5s

### Rate Limiting Configuration

The gateway implements rate limiting using Bucket4j with different limits for different API endpoints:

- **API endpoints**: 
  - Anonymous: 20 requests per minute
  - Free users: 100 requests per hour, 30 per minute
  - Premium users: 5000 requests per hour, 200 per minute

- **Public endpoints**:
  - Anonymous: 100 requests per minute
  - Free users: 500 requests per hour
  - Premium users: 10000 requests per hour

- **Heavy operations** (export endpoints):
  - Anonymous: 5 requests per hour
  - Free users: 10 requests per hour, 3 per minute
  - Premium users: 100 requests per hour, 20 per minute

- **AI service operations** (chat endpoints):
  - Anonymous: 5 requests per hour
  - Free users: 10 requests per hour, 3 per minute
  - Premium users: 100 requests per hour, 20 per minute

- **Other AI service endpoints**:
  - Anonymous: 20 requests per minute
  - Free users: 100 requests per hour, 30 per minute
  - Premium users: 5000 requests per hour, 200 per minute

## Service Discovery

The gateway uses Eureka for service discovery:
- **Eureka Server**: `http://localhost:8761/eureka/`
- Service routes use `lb://` (load balancer) to automatically route to available service instances

## Security Configuration

The gateway includes security configuration with API key headers and security filters. OAuth2 login routes are marked as public to allow authentication flows.

## Usage

The gateway should be started before other services to ensure proper routing. All client requests should be directed to the gateway at the configured port (8765), which will then route them to the appropriate backend services.