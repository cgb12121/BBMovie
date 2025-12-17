minio/data/
│── .minio.sys/  # MinIO system files
│
├── bbmovie-raw/  # Bucket for raw uploads (Ingestion)
│   ├── users/
│   │   └── avatars/
│   │       └── {uploadId}.{ext}      # e.g. avatars/123e4567...jpg
│   │
│   └── movies/
│       ├── posters/
│       │   └── {uploadId}.{ext}      # e.g. posters/890abcde...png
│       │
│       ├── trailers/
│       │   └── {uploadId}/           # Folder per upload to keep filenames clean
│       │       └── {filename}        # e.g. trailers/55555/teaser.mp4
│       │
│       └── sources/
│           └── {uploadId}/           # Folder per upload
│               └── {filename}        # e.g. sources/99999/full_movie_4k.mkv
│
├── bbmovie-hls/  # Bucket for public streaming (Transcoded output)
│   └── movies/
│       └── {movieId}/                # Organized by Movie ID (Business Entity)
│           ├── master.m3u8           # Master playlist
│           ├── 1080p/
│           │   ├── playlist.m3u8
│           │   ├── segment_0.ts
│           │   └── ...
│           ├── 720p/
│           │   └── ...
│           └── thumbnail.jpg         # Generated thumbnail
│
└── bbmovie-secure/  # Bucket for DRM keys (Private/Restricted)
    └── movies/
        └── {movieId}/
            └── hls.key               # Encryption key for the HLS segments