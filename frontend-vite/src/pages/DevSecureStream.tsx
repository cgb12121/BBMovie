import React, { useEffect, useRef, useState, useCallback } from 'react';
import Hls from 'hls.js';
import 'plyr/dist/plyr.css';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Grid,
  Chip,
  Paper,
  Select,
  MenuItem,
  InputLabel,
  FormControl,
} from '@mui/material';

type LogType = 'info' | 'success' | 'error' | 'warning' | 'key' | 'segment';

interface LogEntry {
  id: number;
  type: LogType;
  message: string;
  timestamp: string;
}

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:1205').replace(/\/$/, '');

const DevSecureStream: React.FC = () => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const plyrRef = useRef<any>(null);
  const hlsRef = useRef<Hls | null>(null);

  const [jwt, setJwt] = useState('');
  const [movieId, setMovieId] = useState('');
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [autoScrollLogs, setAutoScrollLogs] = useState(true);
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [statusMessage, setStatusMessage] = useState('Ready');
  const [keysLoadedCount, setKeysLoadedCount] = useState(0);
  const [segmentsLoadedCount, setSegmentsLoadedCount] = useState(0);
  const [hlsVersion] = useState<string>(() => (Hls as any)?.version || 'Unknown');
  const [streamUrl, setStreamUrl] = useState<string>('-');
  const [availableLevels, setAvailableLevels] = useState<any[]>([]);
  const [selectedQuality, setSelectedQuality] = useState<'auto' | number>('auto');

  const logsContainerRef = useRef<HTMLDivElement | null>(null);
  const logIdRef = useRef(0);

  const pushLog = useCallback((message: string, type: LogType = 'info') => {
    const entry: LogEntry = {
      id: ++logIdRef.current,
      type,
      message,
      timestamp: new Date().toLocaleTimeString(),
    };
    setLogs((prev) => [...prev, entry].slice(-500)); // keep last 500
    // eslint-disable-next-line no-console
    console.log(`[${type.toUpperCase()}]`, message);
  }, []);

  const updateStatus = useCallback(
    (message: string, newStatus: typeof status) => {
      setStatus(newStatus);
      setStatusMessage(message);
    },
    []
  );

  const clearLogs = () => {
    setLogs([]);
    setKeysLoadedCount(0);
    setSegmentsLoadedCount(0);
    pushLog('Logs cleared', 'info');
  };

  useEffect(() => {
    if (autoScrollLogs && logsContainerRef.current) {
      logsContainerRef.current.scrollTop = logsContainerRef.current.scrollHeight;
    }
  }, [logs, autoScrollLogs]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
      }
      if (plyrRef.current) {
        plyrRef.current.destroy();
      }
    };
  }, []);

  // Dynamically load Plyr from its pre-bundled dist file to avoid ESM tooling issues.
  const attachPlyr = useCallback(async () => {
    if (!videoRef.current) return;
    const { default: Plyr } = await import('plyr');


    if (plyrRef.current) {
      plyrRef.current.destroy();
      plyrRef.current = null;
    }
    plyrRef.current = new Plyr(videoRef.current, {
      controls: [
        'play-large',
        'play',
        'progress',
        'current-time',
        'mute',
        'volume',
        'settings',
        'fullscreen',
      ],
      settings: ['quality', 'speed', 'loop'],
    });
  }, []);

  const handleLoadStream = async () => {
    if (!movieId.trim()) {
      pushLog('Please enter a Movie ID', 'error');
      updateStatus('Enter Movie ID', 'error');
      return;
    }
    if (!jwt.trim()) {
      pushLog('Please enter a JWT token', 'error');
      updateStatus('Enter JWT token', 'error');
      return;
    }

    // Reset state
    setKeysLoadedCount(0);
    setSegmentsLoadedCount(0);
    setAvailableLevels([]);
    setSelectedQuality('auto');

    // Destroy previous instances
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    const video = videoRef.current;
    if (!video) return;

    const url = `${API_BASE_URL}/api/stream/${movieId}/master.m3u8`;
    setStreamUrl(url);

    updateStatus('Initializing secure stream...', 'loading');
    pushLog(`Starting stream for Movie ID: ${movieId}`, 'info');
    pushLog(`Stream URL: ${url}`, 'info');

    await attachPlyr();

    if (Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        backBufferLength: 90,
        xhrSetup: (xhr, srcUrl) => {
          // 1. Key file from backend
          if (srcUrl.includes('/keys/') && srcUrl.includes('.key')) {
            const keyFileName = srcUrl.split('/').pop();
            if (keyFileName) {
              pushLog(`Loading encryption key: ${keyFileName}`, 'key');
            }
            xhr.setRequestHeader('Authorization', `Bearer ${jwt}`);
            setKeysLoadedCount((prev) => prev + 1);
            return;
          }
          // 2. Playlist from media service (port 1205 or API gateway)
          if (srcUrl.includes('.m3u8') && srcUrl.includes(':1205')) {
            xhr.setRequestHeader('Authorization', `Bearer ${jwt}`);
            return;
          }
          // 3. Segments from backend
          if (srcUrl.includes('.ts') && srcUrl.includes(':1205')) {
            setSegmentsLoadedCount((prev) => prev + 1);
            return;
          }
          // 4. Direct MinIO URLs (port 9000) – never send auth header
          if (srcUrl.includes(':9000')) {
            if (srcUrl.includes('.ts')) {
              setSegmentsLoadedCount((prev) => prev + 1);
            } else if (srcUrl.includes('.key')) {
              setKeysLoadedCount((prev) => prev + 1);
            }
          }
        },
      });

      hlsRef.current = hls;

      hls.loadSource(url);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        updateStatus('Manifest parsed, starting playback...', 'success');
        pushLog('Manifest parsed successfully', 'success');
        pushLog(`Found ${hls.levels.length} quality levels`, 'info');
        setAvailableLevels(hls.levels);

        hls.levels.forEach((level, index) => {
          const resolution = level.height ? `${level.width || 'unknown'}x${level.height}` : 'unknown';
          const bitrate = level.bitrate ? (level.bitrate / 1_000_000).toFixed(1) : 'unknown';
          pushLog(`Quality [${index}]: ${resolution} @ ${bitrate}Mbps`, 'info');
        });

        video
          .play()
          .catch((e) => {
            pushLog(`Auto-play blocked: ${e.message}`, 'warning');
            updateStatus('Click play button to start', 'success');
          });
      });

      hls.on(Hls.Events.LEVEL_SWITCHED, (_, data) => {
        const level = hls.levels[data.level];
        if (level) {
          const resolution = level.height ? `${level.width || 'unknown'}x${level.height}` : 'unknown';
          const bitrate = level.bitrate ? (level.bitrate / 1_000_000).toFixed(1) : 'unknown';
          pushLog(`Switched to quality: ${resolution} @ ${bitrate}Mbps`, 'info');
          setSelectedQuality(data.level);
        }
      });

      hls.on(Hls.Events.KEY_LOADED, () => {
        pushLog('Decryption key loaded successfully', 'success');
      });

      hls.on(Hls.Events.FRAG_LOADED, (_, data) => {
        const stats = (data as any).stats;
        const kb = stats && typeof stats.loaded === 'number' ? (stats.loaded / 1024).toFixed(0) : '0';
        pushLog(`Segment ${data.frag.sn} loaded (${kb}KB)`, 'segment');
      });

      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) {
          pushLog(`Fatal error: ${data.type} - ${data.details}`, 'error');
          updateStatus(`Error: ${data.details}`, 'error');
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              pushLog('Retrying connection...', 'warning');
              hls.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              pushLog('Recovering media error...', 'warning');
              hls.recoverMediaError();
              break;
            default:
              pushLog('Cannot recover, destroying HLS instance', 'error');
              hls.destroy();
              break;
          }
        } else {
          pushLog(`Non-fatal error: ${data.details}`, 'warning');
        }
      });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      updateStatus('Using native HLS (Safari)', 'success');
      pushLog('Using native HLS (Safari). Note: Key authentication may not work', 'warning');
      video.src = url;
      video.addEventListener('loadedmetadata', () => {
        pushLog('Native HLS loaded', 'success');
        video.play().catch(() => {
          updateStatus('Click play button to start', 'success');
        });
      });
    } else {
      updateStatus('HLS not supported in this browser', 'error');
      pushLog('HLS is not supported in this browser', 'error');
    }
  };

  const handleQualityChange = (value: 'auto' | number) => {
    setSelectedQuality(value);
    const hls = hlsRef.current;
    if (!hls) return;
    if (value === 'auto') {
      hls.currentLevel = -1;
      pushLog('Switched to auto quality selection', 'info');
    } else {
      hls.currentLevel = value;
      pushLog(`Manually switched to quality level ${value}`, 'info');
    }
  };

  return (
    <Box sx={{ maxWidth: 1400, mx: 'auto', p: 3, display: 'flex', gap: 2, height: '100vh', boxSizing: 'border-box' }}>
      {/* Left: Logs and configuration */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
        {/* Header */}
        <Card>
          <CardContent sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Typography variant="h5">
                BBMovie <span style={{ color: '#e50914' }}>Secure</span> Stream (Dev)
              </Typography>
            </Box>
            <Chip
              label={statusMessage}
              color={status === 'success' ? 'success' : status === 'error' ? 'error' : 'default'}
              variant="outlined"
            />
          </CardContent>
        </Card>

        {/* Logs */}
        <Card sx={{ flex: 1, minHeight: 260, display: 'flex', flexDirection: 'column' }}>
          <CardContent sx={{ pb: 1, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="subtitle1">System Logs</Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button size="small" variant="outlined" onClick={() => setAutoScrollLogs((v) => !v)}>
                {autoScrollLogs ? 'Auto-scroll' : 'Manual'}
              </Button>
              <Button size="small" variant="outlined" color="error" onClick={clearLogs}>
                Clear
              </Button>
            </Box>
          </CardContent>
          <Box
            ref={logsContainerRef}
            sx={{
              flex: 1,
              p: 2,
              pt: 0,
              bgcolor: 'black',
              color: '#00ff88',
              fontFamily: 'Monaco, Consolas, monospace',
              fontSize: 12,
              overflowY: 'auto',
              borderTop: '1px solid rgba(255,255,255,0.1)',
            }}
          >
            {logs.map((log) => (
              <Box
                key={log.id}
                sx={{
                  mb: 0.5,
                  pb: 0.5,
                  borderBottom: '1px solid rgba(255,255,255,0.05)',
                  color:
                    log.type === 'error'
                      ? '#ff6b6b'
                      : log.type === 'success'
                      ? '#4ecdc4'
                      : log.type === 'key'
                      ? '#ffe66d'
                      : undefined,
                }}
              >
                [{log.timestamp}] {log.message}
              </Box>
            ))}
          </Box>
        </Card>

        {/* Configuration */}
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Stream Configuration
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  label="JWT Token"
                  multiline
                  minRows={2}
                  fullWidth
                  value={jwt}
                  onChange={(e) => setJwt(e.target.value)}
                  placeholder="Paste your JWT bearer token here..."
                />
              </Grid>
              <Grid item xs={12} sm={8}>
                <TextField
                  label="Movie ID"
                  fullWidth
                  value={movieId}
                  onChange={(e) => setMovieId(e.target.value)}
                  placeholder="Enter upload ID (e.g., 671b5af6-0858-4ab7-9bce-19d7a5d4087c)"
                />
              </Grid>
              <Grid item xs={12} sm={4} sx={{ display: 'flex', alignItems: 'stretch' }}>
                <Button
                  variant="contained"
                  color="primary"
                  fullWidth
                  onClick={handleLoadStream}
                  sx={{ fontWeight: 600 }}
                >
                  PLAY STREAM
                </Button>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      </Box>

      {/* Right: Video and stats */}
      <Box sx={{ flex: 1.2, display: 'flex', flexDirection: 'column', gap: 2 }}>
        <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ flex: 1, position: 'relative', mb: 2 }}>
              <video
                ref={videoRef}
                className="plyr-react plyr"
                controls
                style={{ width: '100%', height: '100%', backgroundColor: 'black' }}
              />
              {!streamUrl || streamUrl === '-' ? (
                <Box
                  sx={{
                    position: 'absolute',
                    inset: 0,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'rgba(255,255,255,0.7)',
                    pointerEvents: 'none',
                    textAlign: 'center',
                    p: 2,
                  }}
                >
                  <Typography variant="h6" gutterBottom>
                    Secure Video Stream
                  </Typography>
                  <Typography variant="body2">
                    Enter Movie ID and JWT Token to start encrypted streaming
                  </Typography>
                  <Typography variant="caption" sx={{ mt: 1, opacity: 0.7 }}>
                    AES-128 Encryption • JWT Authentication • HLS Streaming
                  </Typography>
                </Box>
              ) : null}
            </Box>

            {/* Stats */}
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <Paper sx={{ p: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Stream Statistics
                  </Typography>
                  <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1 }}>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Stream URL
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'Monaco, monospace', wordBreak: 'break-all' }}>
                        {streamUrl}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        HLS Version
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'Monaco, monospace' }}>
                        {hlsVersion}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Keys Loaded
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'Monaco, monospace' }}>
                        {keysLoadedCount}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Segments
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'Monaco, monospace' }}>
                        {segmentsLoadedCount}
                      </Typography>
                    </Box>
                  </Box>
                </Paper>
              </Grid>
              <Grid item xs={12} md={6}>
                <Paper sx={{ p: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Quality Selector
                  </Typography>
                  <FormControl fullWidth size="small">
                    <InputLabel>Quality</InputLabel>
                    <Select
                      label="Quality"
                      value={selectedQuality === 'auto' ? 'auto' : String(selectedQuality)}
                      onChange={(e) =>
                        handleQualityChange(
                          e.target.value === 'auto' ? 'auto' : parseInt(e.target.value as string, 10)
                        )
                      }
                    >
                      <MenuItem value="auto">Auto (Recommended)</MenuItem>
                      {availableLevels.map((level, index) => {
                        const resolution = level.height
                          ? `${level.width || 'unknown'}x${level.height}`
                          : 'unknown';
                        const bitrate = level.bitrate ? (level.bitrate / 1_000_000).toFixed(1) : 'unknown';
                        return (
                          <MenuItem key={index} value={String(index)}>
                            {resolution} ({bitrate} Mbps)
                          </MenuItem>
                        );
                      })}
                    </Select>
                  </FormControl>
                </Paper>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
};

export default DevSecureStream;


