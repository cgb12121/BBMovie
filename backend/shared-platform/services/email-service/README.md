# Email Service

The Email Service is responsible for sending various types of emails to users, including verification emails, password reset notifications, and subscription updates. It supports multiple email delivery methods and integrates with external services via NATS messaging.

## Architecture Overview

The service follows a microservices architecture and communicates with other services through NATS messaging. It supports multiple email delivery strategies including Gmail API, MailerSend API, and SMTP protocols.

## External Communications & Services

### 1. NATS Messaging System

The email service communicates with other services through NATS, a lightweight messaging system.

**Connection Details:**
- **Host**: `nats://localhost:4222`
- **Client Library**: io.nats:jnats (version 2.19.0)
- **Connection Name**: `email-service`

**Resilience Features:**
- Infinite reconnection attempts (`maxReconnects(-1)`)
- Exponential backoff retry mechanism (2s → 4s → 8s → ... capped at 30s)
- Connection timeout: 5 seconds
- Reconnect wait: 10 seconds
- Ping interval: 30 seconds

**Dependencies:**
- `io.nats:jnats:2.19.0`
- `io.github.resilience4j:resilience4j-retry:2.2.0`

### 2. Gmail API Service

The service can send emails via Gmail's REST API using OAuth2 authentication.

**API Endpoint**: `https://www.googleapis.com/gmail/v1/users/{userId}/messages/send`

**Authentication**:
- Uses Application Default Credentials with Gmail scope (`https://www.googleapis.com/auth/gmail.send`)
- Requires Google Cloud Platform service account setup

**Dependencies:**
- `com.google.api-client:google-api-client:2.7.2`
- `com.google.apis:google-api-services-gmail:v1-rev20240520-2.0.0`
- `com.google.oauth-client:google-oauth-client-jetty:1.36.0`

### 3. MailerSend API Service

The service can send emails via MailerSend's REST API.

**API Endpoint**: `https://api.mailersend.com/v1/email`

**Authentication**:
- Bearer token authentication using `mailer-sender.token` from configuration
- Requires MailerSend account and API token

**Dependencies:**
- `com.mailersend:java-sdk:1.1.3`

### 4. SMTP Services

The service supports sending emails via SMTP protocols:

**Gmail SMTP:**
- Host: `smtp.gmail.com`
- Port: `587`
- Authentication: Required
- TLS: Enabled

**MailerSend SMTP:**
- Configurable host via `mailer-sender.smtp.server`
- Configurable port via `mailer-sender.smtp.port`
- Authentication: Required

**Dependencies:**
- `org.springframework.boot:spring-boot-starter-mail`

## Event Subscriptions

The email service listens to various events via NATS:

### Auth Events
- **Subject Pattern**: `auth.>` 
- **Events Handled**:
  - `auth.registration` - Sends email verification
  - `auth.forgot_password` - Sends password reset email
  - `auth.changed_password` - Sends password change notification
  - `auth.otp` - Handles OTP events

### Subscription Events
- **Subject Pattern**: `payments.subscription.*`
- **Events Handled**:
  - `payments.subscription.subscribe` - Subscription confirmation
  - `payments.subscription.renewal.upcoming` - Upcoming renewal notice
  - `payments.subscription.cancelled` - Cancellation notification

## Email Delivery Strategies

The service implements a strategy pattern for email delivery with multiple fallback options:

1. **gmailSmtp** - Gmail SMTP service
2. **gmailApi** - Gmail REST API service  
3. **mailersendSmtp** - MailerSend SMTP service
4. **mailersendApi** - MailerSend REST API service

Configuration allows defining a default strategy and rotation order for failover.

## Configuration

### Environment Variables
- `EMAIL_USERNAME` - Gmail username
- `EMAIL_PASSWORD` - Gmail password or app-specific password
- `MAIL_SENDER_TOKEN` - MailerSend API token
- `MAIL_SENDER_SMTP_SERVER` - MailerSend SMTP server
- `MAIL_SENDER_SMTP_PORT` - MailerSend SMTP port
- `MAIL_SENDER_SMTP_USERNAME` - MailerSend SMTP username
- `MAIL_SENDER_SMTP_PASSWORD` - MailerSend SMTP password
- `SERVER_PORT` - Service port

### Application Properties
```
app.frontend.url=localhost:3000
app.email.rotation.enabled=false
app.mail.default=gmailSmtp
app.mail.rotation-order=gmailSmtp,gmailApi,mailerSendSmtp,mailerSendApi
```

## Templates

Email templates are stored in `src/main/resources/templates/` and processed using Thymeleaf:
- `verification.html` - Email verification template
- `reset-password.html` - Password reset template
- `password-change.html` - Password change notification template

## Threading Model

- Uses virtual threads for email processing (requires Java 21+)
- Concurrent email processing with semaphore limiting (max 100 concurrent operations)

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