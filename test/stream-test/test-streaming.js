// Configuration
let autoScrollEnabled = true;
let keysLoadedCount = 0;
let segmentsLoadedCount = 0;
let hls = null;
let watchHistoryFlushTimerId = null;
let watchHistoryEndedHandler = null;
let resumeSeekCleanup = null;
let playbackWs = null;
let wsSendChain = Promise.resolve();
let trackingTimeUpdateFn = null;
let trackingPauseFn = null;
let trackingSeekedFn = null;
let trackingPageHideFn = null;
let trackingVisibilityFn = null;

// DOM Elements
const statusDot = document.getElementById('statusDot');
const statusText = document.getElementById('statusText');
const configStatusDot = document.getElementById('configStatusDot');
const configStatusText = document.getElementById('configStatusText');
const loadingSpinner = document.getElementById('loadingSpinner');
const streamUrlElement = document.getElementById('streamUrl');
const hlsVersionElement = document.getElementById('hlsVersion');
const keysLoadedElement = document.getElementById('keysLoaded');
const segmentsLoadedElement = document.getElementById('segmentsLoaded');
const videoPlaceholder = document.getElementById('videoPlaceholder');
const autoScrollBtn = document.getElementById('autoScrollBtn');
const qualitySelector = document.getElementById('qualitySelector');
const watchHistoryStatusEl = document.getElementById('watchHistoryStatus');
const resumePositionEl = document.getElementById('resumePosition');

// Initialize
hlsVersionElement.textContent = Hls.version || 'Unknown';

// Helper Functions
function updateStatus(message, type = 'info') {
    statusText.textContent = message;
    configStatusText.textContent = message;

    // Update both status dots
    [statusDot, configStatusDot].forEach(dot => {
        dot.className = 'status-dot';
        if (type === 'loading') {
            dot.classList.add('active');
            loadingSpinner.style.display = 'block';
        } else if (type === 'success') {
            dot.style.background = '#00e054';
            loadingSpinner.style.display = 'none';
        } else if (type === 'error') {
            dot.style.background = '#ff4757';
            loadingSpinner.style.display = 'none';
        } else {
            loadingSpinner.style.display = 'none';
        }
    });
}

function log(msg, type = 'info') {
    const el = document.getElementById('logs');
    const timestamp = new Date().toLocaleTimeString();
    const logEntry = document.createElement('div');
    logEntry.className = 'log-entry';

    let icon = '📝';
    if (type === 'success') icon = '✅';
    else if (type === 'error') icon = '❌';
    else if (type === 'warning') icon = '⚠️';
    else if (type === 'key') icon = '🔑';
    else if (type === 'segment') icon = '📦';

    logEntry.textContent = `[${timestamp}] ${icon} ${msg}`;

    // Color code based on type
    if (type === 'error') {
        logEntry.style.color = '#ff6b6b';
    } else if (type === 'success') {
        logEntry.style.color = '#4ecdc4';
    } else if (type === 'key') {
        logEntry.style.color = '#ffe66d';
    }

    el.appendChild(logEntry);

    if (autoScrollEnabled) {
        el.scrollTop = el.scrollHeight;
    }

    console.log(`[${type.toUpperCase()}]`, msg);
}

function clearLogs() {
    const el = document.getElementById('logs');
    el.innerHTML = '';
    keysLoadedCount = 0;
    segmentsLoadedCount = 0;
    updateCounters();
    log("Logs cleared", 'info');
}

function toggleAutoScroll() {
    autoScrollEnabled = !autoScrollEnabled;
    autoScrollBtn.innerHTML = autoScrollEnabled ?
        '<span>📜</span> Auto' : '<span>📜</span> Manual';
    autoScrollBtn.style.background = autoScrollEnabled ?
        'rgba(255, 255, 255, 0.1)' : 'rgba(229, 9, 20, 0.2)';
}

function updateCounters() {
    keysLoadedElement.textContent = keysLoadedCount;
    segmentsLoadedElement.textContent = segmentsLoadedCount;
}

function clearResumeSeekListeners() {
    if (typeof resumeSeekCleanup === 'function') {
        resumeSeekCleanup();
        resumeSeekCleanup = null;
    }
}

function closePlaybackWs() {
    resetWsSendChain();
    if (playbackWs) {
        try {
            playbackWs.close();
        } catch (e) {
            /* ignore */
        }
        playbackWs = null;
    }
}

function resetWsSendChain() {
    wsSendChain = Promise.resolve();
}

function clearWatchHistoryTracking(video) {
    clearResumeSeekListeners();
    closePlaybackWs();
    if (watchHistoryFlushTimerId != null) {
        clearInterval(watchHistoryFlushTimerId);
        watchHistoryFlushTimerId = null;
    }
    if (video && trackingTimeUpdateFn) {
        video.removeEventListener('timeupdate', trackingTimeUpdateFn);
        trackingTimeUpdateFn = null;
    }
    if (video && trackingPauseFn) {
        video.removeEventListener('pause', trackingPauseFn);
        trackingPauseFn = null;
    }
    if (video && trackingSeekedFn) {
        video.removeEventListener('seeked', trackingSeekedFn);
        trackingSeekedFn = null;
    }
    if (trackingPageHideFn) {
        window.removeEventListener('pagehide', trackingPageHideFn);
        trackingPageHideFn = null;
    }
    if (trackingVisibilityFn) {
        document.removeEventListener('visibilitychange', trackingVisibilityFn);
        trackingVisibilityFn = null;
    }
    if (video && watchHistoryEndedHandler) {
        video.removeEventListener('ended', watchHistoryEndedHandler);
        watchHistoryEndedHandler = null;
    }
}

function isValidUuid(str) {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(str);
}

function normalizeGatewayBase(url) {
    return (url || '').trim().replace(/\/+$/, '');
}

function readLocalResume(movieId) {
    try {
        const raw = localStorage.getItem(`bbmovie_wh:${movieId}`);
        if (!raw) {
            return null;
        }
        return JSON.parse(raw);
    } catch (e) {
        return null;
    }
}

function writeLocalResume(movieId, video, completed, useLocal) {
    if (!useLocal) {
        return;
    }
    try {
        localStorage.setItem(
            `bbmovie_wh:${movieId}`,
            JSON.stringify({
                positionSec: video.currentTime,
                durationSec: video.duration > 0 ? video.duration : 0,
                completed: !!completed,
                clientTs: Date.now()
            })
        );
    } catch (e) {
        /* quota / private mode */
    }
}

function mergeServerAndLocalResume(serverResume, movieId, useLocal) {
    if (!useLocal) {
        return serverResume;
    }
    const local = readLocalResume(movieId);
    if (!local || local.completed) {
        return serverResume;
    }
    const lp = Number(local.positionSec);
    if (!Number.isFinite(lp) || lp <= 0.5) {
        return serverResume;
    }
    if (!serverResume) {
        return {
            completed: false,
            positionSec: lp,
            durationSec: Number(local.durationSec) || 0
        };
    }
    if (serverResume.completed) {
        return serverResume;
    }
    const sp = Number(serverResume.positionSec);
    if (!Number.isFinite(sp) || lp > sp + 1.5) {
        return {
            completed: false,
            positionSec: lp,
            durationSec:
                Number(local.durationSec) > 0
                    ? Number(local.durationSec)
                    : Number(serverResume.durationSec) || 0
        };
    }
    return serverResume;
}

async function fetchResumePayload(gatewayBase, jwt, movieId) {
    resumePositionEl.textContent = '—';
    if (!isValidUuid(movieId)) {
        watchHistoryStatusEl.textContent = 'Invalid movie UUID';
        log('Watch-history resume skipped: movieId must be a UUID', 'warning');
        return null;
    }
    const base = normalizeGatewayBase(gatewayBase);
    const url = `${base}/api/watch-history/v1/resume/${movieId}`;
    try {
        const res = await fetch(url, {
            headers: { Authorization: `Bearer ${jwt}` }
        });
        if (res.status === 404) {
            watchHistoryStatusEl.textContent = 'No saved position';
            log('No resume data for this movie', 'info');
            return null;
        }
        if (!res.ok) {
            watchHistoryStatusEl.textContent = `HTTP ${res.status}`;
            log(`Resume request failed: ${res.status}`, 'error');
            return null;
        }
        const data = await res.json();
        return {
            completed: Boolean(data.completed),
            positionSec: Number(data.positionSec),
            durationSec: Number(data.durationSec)
        };
    } catch (e) {
        watchHistoryStatusEl.textContent = 'Error';
        log(`Resume fetch error: ${e.message}`, 'error');
        return null;
    }
}

function applySmartResume(video, hlsInstance, resume) {
    clearResumeSeekListeners();
    if (!resume) {
        return;
    }
    if (resume.completed) {
        watchHistoryStatusEl.textContent = 'Marked completed';
        log('Watch history: title marked completed; starting from beginning', 'info');
        return;
    }
    const pos = Number(resume.positionSec);
    if (!Number.isFinite(pos) || pos <= 0.5) {
        watchHistoryStatusEl.textContent = 'Start from beginning';
        log('Resume position not applied (too close to start)', 'info');
        return;
    }
    const savedDur = Number(resume.durationSec);

    const clampSeekSeconds = () => {
        const mediaDur = video.duration;
        if (!Number.isFinite(mediaDur) || mediaDur <= 0) {
            return null;
        }
        const endMargin = 0.75;
        let maxSeek = mediaDur - endMargin;
        if (Number.isFinite(savedDur) && savedDur > 0) {
            maxSeek = Math.min(maxSeek, savedDur - endMargin);
        }
        if (!Number.isFinite(maxSeek) || maxSeek <= 1) {
            return null;
        }
        const target = Math.min(pos, maxSeek);
        if (target <= 0.5) {
            return null;
        }
        return target;
    };

    const trySeek = () => {
        const t = clampSeekSeconds();
        if (t == null) {
            return false;
        }
        try {
            video.currentTime = t;
            resumePositionEl.textContent = `${t.toFixed(1)}s`;
            watchHistoryStatusEl.textContent = 'Resumed';
            log(`Seeking to saved position ${t.toFixed(1)}s`, 'success');
            return true;
        } catch (e) {
            log(`Resume seek failed: ${e.message}`, 'warning');
            return false;
        }
    };

    if (trySeek()) {
        return;
    }

    watchHistoryStatusEl.textContent = 'Waiting for media…';

    const onMediaReady = () => {
        if (trySeek()) {
            cleanup();
        }
    };

    let timeoutId = null;
    const cleanup = () => {
        video.removeEventListener('loadedmetadata', onMediaReady);
        video.removeEventListener('durationchange', onMediaReady);
        video.removeEventListener('canplay', onMediaReady);
        video.removeEventListener('canplaythrough', onMediaReady);
        if (hlsInstance && typeof hlsInstance.off === 'function') {
            hlsInstance.off(Hls.Events.LEVEL_LOADED, onMediaReady);
            hlsInstance.off(Hls.Events.FRAG_PARSED, onMediaReady);
        }
        if (timeoutId != null) {
            clearTimeout(timeoutId);
            timeoutId = null;
        }
        resumeSeekCleanup = null;
    };

    resumeSeekCleanup = cleanup;

    video.addEventListener('loadedmetadata', onMediaReady);
    video.addEventListener('durationchange', onMediaReady);
    video.addEventListener('canplay', onMediaReady);
    video.addEventListener('canplaythrough', onMediaReady);
    if (hlsInstance && typeof hlsInstance.on === 'function') {
        hlsInstance.on(Hls.Events.LEVEL_LOADED, onMediaReady);
        hlsInstance.on(Hls.Events.FRAG_PARSED, onMediaReady);
    }

    timeoutId = setTimeout(() => {
        cleanup();
        if (resumePositionEl.textContent === '—') {
            watchHistoryStatusEl.textContent = 'Resume timeout';
            log('Could not apply resume: media duration not ready in time', 'warning');
        }
    }, 20000);

    queueMicrotask(() => onMediaReady());
}

async function postPlaybackTrackBody(gatewayBase, jwt, body, fetchOpts = {}) {
    const base = normalizeGatewayBase(gatewayBase);
    const url = `${base}/api/watch-history/v1/playback`;
    const res = await fetch(url, {
        method: 'POST',
        headers: {
            Authorization: `Bearer ${jwt}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(body),
        ...fetchOpts
    });
    if (!res.ok) {
        const text = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status} ${text}`);
    }
    return res.json();
}

function sendPlaybackOverWs(body) {
    const step = () =>
        new Promise((resolve, reject) => {
            if (!playbackWs || playbackWs.readyState !== WebSocket.OPEN) {
                reject(new Error('WebSocket not open'));
                return;
            }
            const onMessage = (ev) => {
                playbackWs.removeEventListener('message', onMessage);
                try {
                    const data = JSON.parse(ev.data);
                    if (data.status === 'error') {
                        reject(new Error(data.message || 'Server error'));
                    } else {
                        resolve(data);
                    }
                } catch (e) {
                    reject(e);
                }
            };
            playbackWs.addEventListener('message', onMessage);
            playbackWs.send(JSON.stringify(body));
        });
    const pending = wsSendChain.then(step);
    wsSendChain = pending.catch(() => {});
    return pending;
}

async function dispatchPlaybackTrack(gatewayBase, jwt, body, fetchOpts) {
    if (playbackWs && playbackWs.readyState === WebSocket.OPEN) {
        try {
            return await sendPlaybackOverWs(body);
        } catch (e) {
            log(`WS track fallback to HTTP: ${e.message}`, 'warning');
        }
    }
    return postPlaybackTrackBody(gatewayBase, jwt, body, fetchOpts || {});
}

function openPlaybackWsAsync(gatewayBase, jwt) {
    return new Promise((resolve, reject) => {
        const httpBase = normalizeGatewayBase(gatewayBase);
        const baseForUrl = httpBase.endsWith('/') ? httpBase : `${httpBase}/`;
        const wsUrl = new URL('/api/watch-history/v1/ws', baseForUrl);
        wsUrl.protocol = wsUrl.protocol === 'https:' ? 'wss:' : 'ws:';
        wsUrl.searchParams.set('token', jwt);
        const ws = new WebSocket(wsUrl.href);
        const to = setTimeout(() => {
            try {
                ws.close();
            } catch (e) {
                /* ignore */
            }
            reject(new Error('WebSocket open timeout'));
        }, 8000);
        ws.onopen = () => {
            clearTimeout(to);
            resolve(ws);
        };
        ws.onerror = () => {
            clearTimeout(to);
            reject(new Error('WebSocket error'));
        };
    });
}

async function startWatchHistoryTracking(video, gatewayBase, jwt, movieId) {
    clearWatchHistoryTracking(video);
    watchHistoryStatusEl.textContent = 'Tracking…';
    if (!isValidUuid(movieId)) {
        watchHistoryStatusEl.textContent = 'Invalid UUID';
        return;
    }

    const useWebSocket = document.getElementById('useWebSocket')?.checked ?? false;
    const useLocal = document.getElementById('localStorageBackup')?.checked ?? true;
    const flushIntervalSec = Math.max(
        5,
        parseInt(document.getElementById('flushIntervalSec')?.value || '30', 10) || 30
    );
    const flushMs = flushIntervalSec * 1000;

    resetWsSendChain();
    if (useWebSocket) {
        try {
            playbackWs = await openPlaybackWsAsync(gatewayBase, jwt);
            log('Watch-history WebSocket connected (Level 1)', 'success');
        } catch (e) {
            playbackWs = null;
            log(`WebSocket unavailable, using HTTP (Level 2): ${e.message}`, 'warning');
        }
    }

    const buildBody = (completed, reason) => ({
        movieId,
        positionSec: video.currentTime,
        durationSec: video.duration > 0 ? video.duration : null,
        eventType: completed ? 'ended' : 'heartbeat',
        completed: !!completed,
        metadata: {
            source: 'test-streaming',
            reason: reason || 'flush',
            transport: playbackWs && playbackWs.readyState === WebSocket.OPEN ? 'ws' : 'http'
        }
    });

    const flushWithReason = async (reason, completed, fetchOpts) => {
        if (
            !completed &&
            reason === 'interval' &&
            (video.paused || !Number.isFinite(video.duration) || video.duration <= 0)
        ) {
            return;
        }
        if (reason === 'pagehide') {
            const b = buildBody(false, reason);
            postPlaybackTrackBody(gatewayBase, jwt, b, { keepalive: true }).catch(() => {});
            return;
        }
        try {
            const r = await dispatchPlaybackTrack(
                gatewayBase,
                jwt,
                buildBody(completed, reason),
                fetchOpts
            );
            watchHistoryStatusEl.textContent =
                playbackWs && playbackWs.readyState === WebSocket.OPEN ? 'Synced (WS)' : 'Synced (HTTP)';
            if (r && typeof r.nextTrackAtEpochSec === 'number') {
                log(
                    `Server flush hint: nextTrackAtEpochSec=${r.nextTrackAtEpochSec} (Level 2 batching)`,
                    'info'
                );
            }
            if (!completed) {
                writeLocalResume(movieId, video, false, useLocal);
            }
        } catch (e) {
            watchHistoryStatusEl.textContent = 'Sync error';
            log(`Watch-history track error: ${e.message}`, 'error');
        }
    };

    let tuLast = 0;
    trackingTimeUpdateFn = () => {
        const now = Date.now();
        if (now - tuLast < 900) {
            return;
        }
        tuLast = now;
        if (!Number.isFinite(video.duration) || video.duration <= 0) {
            return;
        }
        writeLocalResume(movieId, video, false, useLocal);
    };
    video.addEventListener('timeupdate', trackingTimeUpdateFn);

    trackingPauseFn = () => flushWithReason('pause', false, {});
    video.addEventListener('pause', trackingPauseFn);

    trackingSeekedFn = () => flushWithReason('seeked', false, {});
    video.addEventListener('seeked', trackingSeekedFn);

    trackingVisibilityFn = () => {
        if (document.visibilityState === 'hidden') {
            flushWithReason('hidden', false, { keepalive: true });
        }
    };
    document.addEventListener('visibilitychange', trackingVisibilityFn);

    trackingPageHideFn = () => flushWithReason('pagehide', false, {});
    window.addEventListener('pagehide', trackingPageHideFn);

    watchHistoryFlushTimerId = setInterval(() => flushWithReason('interval', false, {}), flushMs);
    log(
        `Watch-history Level 2: flush every ${flushIntervalSec}s, pause/seek/hidden immediate; tab close uses fetch(keepalive) (Bearer cannot use sendBeacon)`,
        'info'
    );

    watchHistoryEndedHandler = async () => {
        try {
            await flushWithReason('ended', true, { keepalive: true });
            writeLocalResume(movieId, video, true, useLocal);
            watchHistoryStatusEl.textContent = 'Completed';
            log('Watch-history: marked completed', 'success');
        } catch (e) {
            log(`Watch-history completion error: ${e.message}`, 'error');
        }
    };
    video.addEventListener('ended', watchHistoryEndedHandler);
}

// Function to populate the quality selector dropdown
function populateQualitySelector() {
    if (!qualitySelector || !hls) return;

    // Clear existing options except the first "Auto" option
    qualitySelector.innerHTML = '<option value="auto">Auto (Recommended)</option>';

    // Add options for each available quality level
    hls.levels.forEach((level, index) => {
        const resolution = level.height ? `${level.width || 'unknown'}x${level.height}` : 'unknown';
        const bitrate = level.bitrate ? (level.bitrate/1000000).toFixed(1) : 'unknown';
        const option = document.createElement('option');
        option.value = index;
        option.textContent = `${resolution} (${bitrate}Mbps)`;
        qualitySelector.appendChild(option);
    });

    // Add event listener for manual quality selection
    qualitySelector.onchange = function() {
        if (!hls) return;

        if (this.value === 'auto') {
            // Enable auto quality selection
            hls.currentLevel = -1; // -1 means auto
            log('Switched to auto quality selection', 'info');
        } else {
            // Set manual quality level
            const levelIndex = parseInt(this.value);
            hls.currentLevel = levelIndex;
            log(`Manually switched to quality level ${levelIndex}`, 'info');
        }
    };
}

// Main Function
async function loadVideo() {
    const video = document.getElementById('video');
    const movieId = document.getElementById('movieId').value.trim();
    const jwt = document.getElementById('jwt').value.trim();
    const apiGateway = document.getElementById('apiGateway').value.trim();
    const watchHistoryOn = document.getElementById('watchHistoryEnabled').checked;

    if (!movieId) {
        log("Please enter a Movie ID", 'error');
        updateStatus("Enter Movie ID", 'error');
        return;
    }

    if (!jwt) {
        log("Please enter a JWT token", 'error');
        updateStatus("Enter JWT token", 'error');
        return;
    }

    clearWatchHistoryTracking(video);
    watchHistoryStatusEl.textContent = '—';
    resumePositionEl.textContent = '—';

    // Cleanup previous HLS instance
    if (hls) {
        hls.destroy();
        hls = null;
    }

    // Reset counters
    keysLoadedCount = 0;
    segmentsLoadedCount = 0;
    updateCounters();

    // Show video and hide placeholder
    video.style.display = 'block';
    videoPlaceholder.style.display = 'none';

    const streamUrl = `http://localhost:1205/api/stream/${movieId}/master.m3u8`;
    streamUrlElement.textContent = streamUrl;

    updateStatus('Initializing secure stream...', 'loading');
    log(`Starting stream for Movie ID: ${movieId}`, 'info');
    log(`Stream URL: ${streamUrl}`, 'info');

    if (Hls.isSupported()) {
        hls = new Hls({
            enableWorker: true,
            lowLatencyMode: true,
            backBufferLength: 90,
            xhrSetup: function(xhr, url) {
                console.log("Requesting:", url);

                // 1. Nếu là Key file -> Gửi JWT
                if (url.includes('/keys/') && url.includes('.key')) {
                    const keyFileName = url.split('/').pop();
                    log(`Loading encryption key: ${keyFileName}`, 'key');
                    xhr.setRequestHeader('Authorization', `Bearer ${jwt}`);
                    keysLoadedCount++;
                    updateCounters();
                    return; // QUAN TRỌNG: return sớm
                }

                // 2. Nếu là Playlist (.m3u8) từ backend của bạn
                if (url.includes('.m3u8') && url.includes(':1205')) {
                    // Gửi auth cho playlist (tùy backend yêu cầu)
                    xhr.setRequestHeader('Authorization', `Bearer ${jwt}`);
                    return; // QUAN TRỌNG: return sớm
                }

                // 3. Nếu là Segment (.ts) từ backend của bạn
                if (url.includes('.ts') && url.includes(':1205')) {
                    // KHÔNG gửi auth cho segments (nếu không cần)
                    // xhr.setRequestHeader('Authorization', `Bearer ${jwt}`);
                    segmentsLoadedCount++;
                    updateCounters();
                    return;
                }

                // 4. Nếu là request đến MinIO (:9000) - cả segment và key
                if (url.includes(':9000')) {
                    // KHÔNG BAO GIỜ gửi auth cho MinIO direct URL
                    // MinIO sẽ dùng query string auth hoặc public access
                    if (url.includes('.ts')) {
                        segmentsLoadedCount++;
                        updateCounters();
                    } else if (url.includes('.key')) {
                        keysLoadedCount++;
                        updateCounters();
                    }
                }

                // 5. Default: không làm gì cả
            }
        });

        hls.loadSource(streamUrl);
        hls.attachMedia(video);

        hls.on(Hls.Events.MANIFEST_PARSED, async function() {
            updateStatus('Manifest parsed, starting playback...', 'success');
            log('Manifest parsed successfully', 'success');
            log(`Found ${hls.levels.length} quality levels`, 'info');

            // Populate quality selector dropdown
            populateQualitySelector();

            // Log quality info for each level
            hls.levels.forEach((level, index) => {
                log(`Quality [${index}]: ${level.width}x${level.height} @ ${(level.bitrate/1000000).toFixed(1)}Mbps`, 'info');
            });

            if (watchHistoryOn && jwt) {
                try {
                    const resumePayload = await fetchResumePayload(apiGateway, jwt, movieId);
                    const useLocal = document.getElementById('localStorageBackup')?.checked ?? true;
                    const merged = mergeServerAndLocalResume(resumePayload, movieId, useLocal);
                    applySmartResume(video, hls, merged);
                    await startWatchHistoryTracking(video, apiGateway, jwt, movieId);
                } catch (e) {
                    log(`Watch-history setup error: ${e.message}`, 'error');
                    watchHistoryStatusEl.textContent = 'Setup error';
                }
            } else {
                watchHistoryStatusEl.textContent = watchHistoryOn ? 'Need JWT' : 'Off';
            }

            video.play().catch(e => {
                log(`Auto-play blocked: ${e.message}`, 'warning');
                updateStatus('Click play button to start', 'info');
            });
        });

        hls.on(Hls.Events.LEVEL_SWITCHED, function(event, data) {
            const level = hls.levels[data.level];
            if (level) {
                const resolution = level.height ? `${level.width || 'unknown'}x${level.height}` : 'unknown';
                const bitrate = level.bitrate ? (level.bitrate/1000000).toFixed(1) : 'unknown';
                log(`Switched to quality: ${resolution} @ ${bitrate}Mbps`, 'info');

                // Update the quality selector to reflect the current level
                if (qualitySelector) {
                    qualitySelector.value = data.level;
                }
            }
        });

        hls.on(Hls.Events.KEY_LOADED, function(event, data) {
            log('Decryption key loaded successfully', 'success');
        });

        hls.on(Hls.Events.FRAG_LOADED, function(event, data) {
            log(`Segment ${data.frag.sn} loaded (${(data.stats.loaded/1024).toFixed(0)}KB)`, 'segment');
        });

        hls.on(Hls.Events.ERROR, function(event, data) {
            if (data.fatal) {
                log(`Fatal error: ${data.type} - ${data.details}`, 'error');
                updateStatus(`Error: ${data.details}`, 'error');

                switch (data.type) {
                    case Hls.ErrorTypes.NETWORK_ERROR:
                        log('Retrying connection...', 'warning');
                        hls.startLoad();
                        break;
                    case Hls.ErrorTypes.MEDIA_ERROR:
                        log('Recovering media error...', 'warning');
                        hls.recoverMediaError();
                        break;
                    default:
                        log('Cannot recover, destroying HLS instance', 'error');
                        clearWatchHistoryTracking(document.getElementById('video'));
                        hls.destroy();
                        break;
                }
            } else {
                log(`Non-fatal error: ${data.details}`, 'warning');
            }
        });

    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        // Native HLS support (Safari)
        updateStatus('Using native HLS (Safari)', 'info');
        log('Using native HLS (Safari). Note: Key authentication may not work', 'warning');
        video.src = streamUrl;
        video.addEventListener('loadedmetadata', async function onMeta() {
            log('Native HLS loaded', 'success');
            if (watchHistoryOn && jwt) {
                const resumePayload = await fetchResumePayload(apiGateway, jwt, movieId);
                const useLocal = document.getElementById('localStorageBackup')?.checked ?? true;
                const merged = mergeServerAndLocalResume(resumePayload, movieId, useLocal);
                applySmartResume(video, null, merged);
                await startWatchHistoryTracking(video, apiGateway, jwt, movieId);
            }
            video.removeEventListener('loadedmetadata', onMeta);
            video.play();
        });
    } else {
        updateStatus('HLS not supported in this browser', 'error');
        log('HLS is not supported in this browser', 'error');
    }
}

// Auto-load JWT on page load
async function loadJwt() {
    try {
        const res = await fetch('jwt-dev.txt');
        if(res.ok) {
            const token = (await res.text()).trim();
            document.getElementById('jwt').value = token;
            log("JWT token loaded from jwt-dev.txt", 'success');
            updateStatus('JWT loaded, ready to play', 'success');
        } else {
            log("Could not load jwt-dev.txt", 'warning');
        }
    } catch(e) {
        log("Error loading JWT: " + e, 'error');
    }
}

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
    // Ctrl/Cmd + L to clear logs
    if ((e.ctrlKey || e.metaKey) && e.key === 'l') {
        e.preventDefault();
        clearLogs();
    }
    // Enter to play video when movieId input is focused
    if (e.key === 'Enter' && document.activeElement.id === 'movieId') {
        loadVideo();
    }
    // Space to play/pause video
    if (e.key === ' ' && !['TEXTAREA', 'INPUT'].includes(document.activeElement.tagName)) {
        e.preventDefault();
        const video = document.getElementById('video');
        if (video.paused) video.play();
        else video.pause();
    }
});

// Initialize
(function applyMovieIdFromQuery() {
    try {
        const mid = new URLSearchParams(window.location.search).get('movieId');
        if (mid) {
            document.getElementById('movieId').value = mid.trim();
        }
    } catch (e) {
        /* ignore */
    }
})();
loadJwt();
updateStatus('Ready to play', 'info');

// Auto-clear logs every 5 minutes
setInterval(() => {
    if (document.getElementById('logs').childElementCount > 100) {
        clearLogs();
        log("Auto-cleared old logs", 'info');
    }
}, 300000);