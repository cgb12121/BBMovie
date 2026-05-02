# VIS — Video Inspection Service

| Short name | **VIS** |
| Full name | **Video Inspection Service** |

Ported from `transcode-worker`: `FastProbeService`, `PresignedUrlProbeStrategy`, `PartialDownloadProbeStrategy`, `MetadataService` (as `VisMetadataService` + strategies). Uses **LGS** (`bbmovie.transcode:lgs`) for ladder suffixes and cost after each successful probe.

No Temporal task queue: use `VisFastProbeService` from Spring context or wire into CAS later. MinIO + `ffprobe` required when running probes.
