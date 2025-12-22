# 11. Update eureka client for quarkus notification service and micronaut review servi

**Status:** Proposed  
**Date:** 2025-07-20  
**Deciders:** Cao Gia Bao  
**Commit:** cc0b8e62

## Context

The BBMovie platform needs to decompose the monolithic 'file-service' (legacy 'god service') into three specialized microservices: media-service for metadata management and source of truth, media-upload-service for upload presign URLs and file handling, and transcode-worker for CPU-intensive tasks. To ensure a smooth transition and minimize data flow inconsistencies, it is crucial to analyze the current model using Auto (Cursor AI Agent) to identify the decomposed services that require migration.

**Commit Message:** update eureka client for quarkus notification service and micronaut review service





## Decision

The analysis reveals that both media-service and media-upload-service need to be migrated from their legacy implementations in Spring Boot to Rust for better performance due to CPU-intensive tasks. The transcode-worker service should also be considered for a migration towards Rust as it handles transcoding tasks.

## Consequences

- Positive: Migrating to Rust can potentially improve the platform's overall performance, especially for CPU-intensive tasks such as AI processing and media transcoding.
- Positive: Using Rust services allows better separation of concerns and scalability in handling these tasks.
- Negative: The migration process may be complex due to differences in programming languages and frameworks between Spring Boot and Rust.
- Negative: It is essential to monitor the performance impact after the migration and ensure that it aligns with the platform's requirements.
## Alternatives Considered

- Staying with the current implementation using Spring Boot
- Migrating only media-upload-service to Rust for better handling of file operations

## Technical Details

- **Commit Hash:** `cc0b8e6223772cc9349217fe03ed124befe85848`
- **Files Changed:** 4
- **Lines Added:** 19
- **Lines Removed:** 0
- **Affected Areas:** service, technology, infrastructure

## Related Files

- `backend/notification/pom.xml` (+0/-0)
- `backend/notification/src/main/resources/application.properties` (+0/-0)
- `backend/review/pom.xml` (+0/-0)
- `backend/review/src/main/resources/application.properties` (+0/-0)

