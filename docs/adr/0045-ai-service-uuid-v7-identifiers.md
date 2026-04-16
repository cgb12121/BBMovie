# 0045 - Adopt UUIDv7 for AI Service Identifiers

## Status

Accepted

## Context

The `ai-service` persists chat sessions, messages, RAG audit data and approval requests in a PostgreSQL database.
Historically, identifiers were a mix of:

- Auto-increment `BIGINT` (e.g. `chat_message.id`)
- Random UUIDv4 (`UUID.randomUUID()`) for some entities and tokens

We want:

- **Time-ordered IDs** for better index locality and query performance
- **Globally unique identifiers** that work well in distributed environments
- A **clear separation** between user/account identifiers (coming from auth) and internal AI identifiers

UUIDv7 provides monotonic, time-ordered UUIDs, making it a good fit for new identifiers generated inside `ai-service`.

## Decision

We adopt **UUIDv7 (time-ordered)** for all **new IDs generated inside `ai-service`**, while keeping external/user IDs as
they are provided by upstream systems (typically UUIDv4).

Concretely:

- **Chat sessions**
  - **Type**: `UUID` (Postgres `uuid`)
  - **Generation**: `UuidCreator.getTimeOrderedEpoch()` (UUIDv7-like)
  - **Code**:
    - `ChatSession.id` (entity)
    - `SessionServiceImpl.createSession(...)`

- **RAG conversation fragments (Elasticsearch documents)**
  - **Type**: `String`
  - **Generation**: `UuidCreator.getTimeOrderedEpoch().toString()` (UUIDv7-like)
  - **Code**:
    - `RagServiceImpl.indexConversationFragment(...)`
    - `RagServiceImpl.indexMessageWithFiles(...)`

- **Approval requests**
  - **Database ID (`approval_requests.id`)**
    - **Type**: `String`
    - **Generation**: `UuidCreator.getTimeOrderedEpoch().toString()` (UUIDv7-like)
    - **Code**: `ApprovalServiceImpl.createRequest(...)`
  - **Approval token (`approval_requests.approval_token`)**
    - **Type**: `String`
    - **Generation**: `UUID.randomUUID().toString()` (**UUIDv4**, intentionally random for security)

- **Audit records, chat messages and other entities**
  - `chat_message.id` remains an auto-increment `BIGINT`.
  - Any `userId` fields stored in `ai-service` are **passed through** from the authentication/identity service and are
    currently **UUIDv4**. `ai-service` does **not** change user ID format.

Summary:

- **UUIDv7 (time-ordered)** is used for **internal AI identifiers**:
  - Chat session IDs
  - RAG memory document IDs
  - Approval request IDs
- **UUIDv4 (random)** is still used for:
  - User IDs (coming from auth / upstream services)
  - Short-lived approval tokens
  - Any other security-sensitive random values

## Consequences

- **Operational**
  - Better index locality and performance for queries that are naturally time-ordered (sessions, RAG memories, approvals).
  - No breaking change for existing user IDs; they remain UUIDv4.
  - Database schema remains compatible: UUIDv7 fits into standard `uuid` columns and string columns without migration.

- **Codebase**
  - `ai-service` depends on `com.github.f4b6a3:uuid-creator` for UUIDv7 generation.
  - When introducing new internal entities that need globally unique IDs, we should prefer
    `UuidCreator.getTimeOrderedEpoch()` over `UUID.randomUUID()` unless there is a security reason to use pure randomness.

- **Documentation / onboarding**
  - New contributors should understand:
    - **Which IDs are UUIDv7 (internal, time-ordered)**.
    - **Which IDs are UUIDv4 (user IDs, security tokens)**.
    - That UUID type is part of the contract with other services (e.g. auth) and should not be changed casually.


