# Payment Service

The Payment Service is a comprehensive payment processing system for the BBMovie platform. It supports multiple payment providers, subscription management, and integrates with various external services for payment processing, tax calculations, and event notifications.

## Architecture Overview

The service is built with Spring Boot and supports multiple payment providers through a unified interface. It includes subscription management, discount campaigns, voucher systems, and integrates with NATS for event-driven architecture. The service uses MySQL for persistent storage and Redis for caching.

## External Communications & Services

### 1. Payment Provider Integrations

The service supports multiple payment providers through a unified interface:

#### PayPal Integration
- **Client Library**: com.paypal.sdk:rest-api-sdk (version 1.14.0)
- **Configuration**:
  - Client ID and Secret from configuration
  - Sandbox/production mode support
  - Return and cancel URLs for payment flow
- **Features**: Payment creation, verification, and refund processing

#### Stripe Integration
- **Client Library**: com.stripe:stripe-java (version 26.2.0)
- **Configuration**:
  - Publishable and Secret keys from configuration
  - Payment intent creation and confirmation
- **Features**: Payment processing, subscription management, refund capabilities

#### VNPay Integration
- **API Endpoint**: `https://sandbox.vnpayment.vn/merchant_webapi/api/transaction`
- **Features**:
  - Payment URL generation with secure hashing
  - Transaction verification using hash secret
  - Sandbox and production environments

#### ZaloPay Integration
- **API Version**: Version 1
- **Configuration**:
  - App ID, Key1, and Key2 for authentication
  - Sandbox mode support
  - Redirect and callback URLs
- **Features**: Payment creation, callback handling, and verification

#### MoMo Integration
- **Configuration**:
  - Partner code, access key, and secret key
  - Sandbox mode support
  - IPN (Instant Payment Notification) URL
- **Features**: Payment processing and webhook handling

### 2. NATS Messaging System

The service publishes payment events to NATS for event-driven architecture.

**Connection Details**:
- **Host**: `nats://localhost:4222`
- **Client Library**: io.nats:jnats (version 2.19.0)
- **Stream**: `PAYMENTS`

**Publications**:
- **Subjects**: Various payment-related events
- Publishes payment status updates to other services
- Supports event-driven subscription management

### 3. MySQL Database

The service uses MySQL for persistent storage of payment records, subscriptions, and related data.

**Connection Details**:
- **URL**: Configured via `DATABASE_URL` environment variable
- **Username**: Configured via `DATABASE_USERNAME` environment variable
- **Password**: Configured via `DATABASE_PASSWORD` environment variable
- **Driver**: MySQL Connector/J

**Features**:
- JPA-based entity management
- Payment records and transaction history
- Subscription plans and user subscriptions
- Discount campaigns and vouchers
- Audit trails and data consistency

### 4. Eureka Service Discovery

The service registers itself with Eureka for service discovery in the microservices architecture.

**Connection Details**:
- **Eureka Server**: `http://localhost:8761/eureka/`
- **Service Name**: `payment-service`

**Configuration**:
- Service registration enabled
- Registry fetching enabled
- Heartbeat and renewal intervals configured

### 5. Redis Caching

The service uses Redis for caching and session management.

**Features**:
- Payment session caching
- Subscription status caching
- Voucher and discount code caching
- Rate limiting and throttling

### 6. Tax Rate API

The service integrates with external tax calculation services.

**API Endpoint**: `https://api.apilayer.com/tax_data`
**Configuration**:
- API key from `API_LAYER_TAX_API_KEY` environment variable
- Used for tax calculations on payments

### 7. JWT OAuth2 Authentication

The service validates JWT tokens for secure API access.

**Configuration**:
- Validates JWT tokens from the authentication service
- Secured endpoints require valid authentication tokens
- User-based access control for payment operations

## API Endpoints

### Payment Endpoints
- **POST** `/api/payment/initiate`
  - Initiates a new payment using the specified provider
  - Requires authentication and payment request details
  - Returns payment creation response with provider-specific details

- **GET** `/api/payment/{provider}/callback`
  - Handles payment callbacks from providers (for testing)
  - Processes payment verification and updates status

- **POST** `/api/payment/zalopay/callback`
  - Handles ZaloPay-specific callback with minimal JSON response
  - Required format per ZaloPay documentation

- **POST** `/api/payment/{provider}/ipn`
  - Handles Instant Payment Notifications from providers
  - Processes payment status updates

- **POST** `/api/payment/{provider}/webhook`
  - Handles webhooks from payment providers
  - Processes payment events and updates system state

- **POST** `/api/payment/refund`
  - Processes payment refunds
  - Requires authentication and payment ID

- **GET** `/api/payment/query`
  - Queries payment status from provider
  - Requires authentication and payment ID

### Subscription Endpoints
- **GET** `/api/v1/subscription/plans`
  - Lists available subscription plans
  - Returns active plans with pricing and features

- **GET** `/api/v1/subscription/`
  - Quotes price for a specific plan and billing cycle
  - Requires plan name and cycle parameters

- **GET** `/api/subscription/mine`
  - Lists user's current subscriptions
  - Requires authentication

- **POST** `/api/subscription/{id}/auto-renew`
  - Toggles auto-renewal for a subscription
  - Requires authentication and request body

- **POST** `/api/subscription/{id}/cancel`
  - Cancels a subscription
  - Requires authentication and cancellation reason

### Administrative Endpoints
- **Discount Campaign Admin**: Management of discount campaigns
- **Payment Admin**: Payment record management
- **Subscription Plan Admin**: Plan configuration and management
- **Voucher Admin**: Voucher creation and management

## Subscription Management

### Billing Cycles
- Monthly, quarterly, annual billing options
- Automatic renewal with user control
- Cancellation and proration handling

### Pricing Service
- Dynamic pricing based on user location and discounts
- Tax calculation integration
- Currency conversion support

## Security Features

- JWT-based authentication for all endpoints
- Secure parameter validation
- Payment provider credential isolation
- Hash-based verification for payment callbacks
- Rate limiting and anti-fraud measures

## Configuration

### Environment Variables
- `DATABASE_URL` - MySQL database URL
- `DATABASE_USERNAME` - Database username
- `DATABASE_PASSWORD` - Database password
- `PAYPAL_CLIENT_ID` - PayPal client ID
- `PAYPAL_CLIENT_SECRET` - PayPal client secret
- `STRIPE_PUBLISHABLE_KEY` - Stripe publishable key
- `STRIPE_SECRET_KEY` - Stripe secret key
- `VNPAY_HASH_SECRET` - VNPay hash secret
- `VNPAY_TMN_CODE` - VNPay terminal merchant code
- `ZALOPAY_APP_ID` - ZaloPay app ID
- `ZALOPAY_KEY1` - ZaloPay key 1
- `ZALOPAY_KEY2` - ZaloPay key 2
- `MOMO_PARTNER_CODE` - MoMo partner code
- `MOMO_ACCESS_KEY` - MoMo access key
- `MOMO_SECRET_KEY` - MoMo secret key
- `API_LAYER_TAX_API_KEY` - Tax API key

### Application Properties
```
server.port: 8088
spring.application.name: payment-service
spring.jpa.hibernate.ddl-auto: update
eureka.client.service-url.defaultZone: http://localhost:8761/eureka/
payment.providers.paypal.enabled: true
payment.providers.stripe.enabled: true
payment.providers.momo.enabled: true
payment.providers.vnpay.enabled: true
payment.providers.zalopay.enabled: true
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

## Quartz Scheduling

The service uses Quartz for subscription management tasks:
- Scheduled subscription renewals
- Payment reminders
- Subscription status updates
- Recurring billing processing