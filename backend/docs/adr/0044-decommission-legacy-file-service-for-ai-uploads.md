# 44. Decommission legacy file-service for AI uploads

**Status:** Accepted  
**Date:** 2025-12-22  
**Deciders:** AI platform team  
**Commit:** pending branch push

## Context

The legacy `file-service` API was still referenced by `ai-service` for AI-related uploads, while the newer `media-upload-service` already provided presigned URL flows backed by MinIO. The Rust context refinery consumes files directly from object storage via presigned downloads. To reduce surface area, remove unused clients, and align AI ingestion with the standard media pipeline, we needed to decommission the remaining `file-service` hooks and formalize an AI-specific upload purpose.

## Decision

- Route all AI asset uploads through `media-upload-service` using the new `AI_ASSET` purpose and dedicated MinIO bucket/path.
- Enable the Rust context refinery to download via presigned URLs produced by `media-upload-service`.
- Remove/deprecate `file-service` WebClient configuration and client stubs from `ai-service` so no new code paths rely on the legacy service.

## Consequences

- Positive: Single, audited upload flow for AI assets; fewer moving parts to secure and operate.
- Positive: Rust context refinery can fetch artifacts without direct MinIO creds, using time-scoped presigned URLs.
- Positive: Clear extension list for AI assets (`md`, `pdf`, `json`, `xml`, `csv`, common audio/image types), reducing ingestion failures.
- Negative: Any remaining callers to legacy `file-service` must migrate or will fail once the legacy endpoint is removed.
- Negative: Additional coordination required to ensure all environments provision the AI assets bucket.

## Alternatives Considered

- Keep `file-service` for AI uploads while adding AI_ASSET to `media-upload-service`: rejected—would preserve duplicate flows and security overhead.
- Direct MinIO credentials in `ai-service` and Rust refinery: rejected—expands secret distribution and bypasses upload validation.

## Notes on the three services

- `media-upload-service`: Adds `AI_ASSET` purpose, bucket selection, and object key strategy for AI assets; presigned URLs used for both upload and download.
- `ai-service`: Drops legacy `file-service` client/config usage; uses presigned URLs and updated MIME allowances for AI assets.
- `rust-ai-context-refinery`: Consumes the presigned download URLs produced by `media-upload-service` for service-to-service retrieval.

