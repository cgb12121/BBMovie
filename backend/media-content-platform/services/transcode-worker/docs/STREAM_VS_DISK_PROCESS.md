# Stream vs Disk Processing Analysis

## Current Flow vs Stream Flow

### Current: Disk-based Processing
```
┌─────────────────────────────────────────────────────────────────────┐
│  CURRENT: Disk-based                                                 │
├─────────────────────────────────────────────────────────────────────┤
│  MinIO ──download──> Temp Disk ──> ClamAV ──> Tika ──> FFmpeg       │
│                          │                        │                  │
│                          │                        ▼                  │
│                          │              HLS segments (disk)          │
│                          │                        │                  │
│                          └────────────────────────┼──> MinIO         │
└─────────────────────────────────────────────────────────────────────┘
```

### Proposed: Stream-based Processing
```
┌─────────────────────────────────────────────────────────────────────┐
│  PROPOSED: Stream-based                                              │
├─────────────────────────────────────────────────────────────────────┤
│  MinIO ──stream──> ClamAV ──stream──> Tika ──stream──> FFmpeg       │
│                                                   │                  │
│                                                   ▼                  │
│                                         HLS segments (???)           │
│                                                   │                  │
│                                                   ▼                  │
│                                                MinIO                 │
└─────────────────────────────────────────────────────────────────────┘
```

## Component Analysis

| Component | Stream Possible? | Notes |
|-----------|------------------|-------|
| ClamAV | ✅ Yes | `clamAVClient.scan(InputStream)` |
| Tika | ✅ Yes | `tika.detect(InputStream)` |
| FFmpeg Input | ✅ Yes | `pipe:0 (stdin)` |
| FFmpeg HLS Output | ❌ NO | Must write multiple files: master.m3u8, playlist.m3u8, seg_000.ts, seg_001.ts... |
| MinIO Upload | ✅ Yes | `putObject(InputStream)` |

## ⚠️ Main Issue: FFmpeg HLS Output

FFmpeg HLS output creates multiple files on disk:
```
output/
├── master.m3u8           # Master playlist
├── 240p/
│   ├── playlist.m3u8     # Resolution playlist
│   ├── seg_000.ts        # Segment 0
│   ├── seg_001.ts        # Segment 1
│   ├── ...               # 20-100+ segments
│   └── key_1.key         # Encryption key
└── 144p/
    └── ...
```

FFmpeg CANNOT stream output for HLS because:
1. HLS requires multiple separate files
2. Playlist references segment files
3. Key rotation requires multiple key files

## 🤔 Multi-Consumer Stream Problem

Stream can only be read ONCE:
```
MinIO Stream ──> ClamAV ──> ❌ Stream already consumed!
                     │
                     ▼
              Tika needs to read again
              FFmpeg needs to read again
```

### Solutions:
1. **TeeInputStream** - Copy stream while reading (complex, memory-intensive)
2. **Download twice** - Wasteful bandwidth
3. **Save to disk once** - Current approach ✅

## 📈 Practical Optimization Options

### Option A: Hybrid Approach (Recommended)
```
┌─────────────────────────────────────────────────────────────────────┐
│  HYBRID: Best of Both Worlds                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. PROBE STAGE (Stream - no disk):                                  │
│     MinIO ──presigned URL──> FFprobe (already implemented)           │
│                                                                      │
│  2. EXECUTE STAGE (Disk required):                                   │
│     MinIO ──download──> Temp File                                   │
│         │                                                            │
│         ├──stream──> ClamAV (scan from disk stream)                 │
│         ├──stream──> Tika (validate from disk stream)               │
│         └──────────> FFmpeg ──> HLS segments (disk)                 │
│                                      │                               │
│                                      ▼                               │
│     Parallel stream upload ──────> MinIO                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Option B: RAM Disk (If disk I/O is bottleneck)
```bash
# Mount tmpfs for temp files
mount -t tmpfs -o size=2G tmpfs /app/temp

# Files in RAM, not SSD/HDD
# ✅ Fast I/O
# ❌ Limited by RAM
# ❌ Lost on crash
```

### Option C: Stream Upload Segments Immediately
```java
// Upload each segment as soon as FFmpeg creates it
// Use WatchService to monitor output directory
WatchService watcher = FileSystems.getDefault().newWatchService();
outputDir.register(watcher, ENTRY_CREATE);

// Upload immediately when segment appears
while (transcoding) {
    WatchKey key = watcher.poll(100, MILLISECONDS);
    for (WatchEvent<?> event : key.pollEvents()) {
        Path segment = (Path) event.context();
        if (segment.toString().endsWith(".ts")) {
            uploadToMinioAsync(segment);  // Non-blocking upload
            Files.delete(segment);        // Free disk immediately
        }
    }
}
```

## 🎯 Conclusion

| Approach | Disk I/O | Complexity | Recommendation |
|----------|----------|------------|----------------|
| Current (all disk) | High | Low | ✅ Simple, works |
| Full stream | N/A | Impossible for HLS | ❌ |
| Hybrid + parallel upload | Medium | Medium | ✅ Good balance |
| RAM disk | Very Low | Low | ✅ If RAM available |
| Stream upload on-the-fly | Low | High | 🤔 Complex but optimal |

## 💡 Recommendation

1. **Keep** probe via presigned URL (already implemented) ✅
2. **Keep** temp disk for FFmpeg (required)
3. **Add** parallel upload - Upload segments as soon as FFmpeg creates them
4. **Consider** RAM disk if disk I/O is a real bottleneck