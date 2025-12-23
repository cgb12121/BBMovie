// Configuration
let autoScrollEnabled = true;
let keysLoadedCount = 0;
let segmentsLoadedCount = 0;
let hls = null;

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

        hls.on(Hls.Events.MANIFEST_PARSED, function() {
            updateStatus('Manifest parsed, starting playback...', 'success');
            log('Manifest parsed successfully', 'success');
            log(`Found ${hls.levels.length} quality levels`, 'info');

            // Populate quality selector dropdown
            populateQualitySelector();

            // Log quality info for each level
            hls.levels.forEach((level, index) => {
                log(`Quality [${index}]: ${level.width}x${level.height} @ ${(level.bitrate/1000000).toFixed(1)}Mbps`, 'info');
            });

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
        video.addEventListener('loadedmetadata', function() {
            log('Native HLS loaded', 'success');
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
loadJwt();
updateStatus('Ready to play', 'info');

// Auto-clear logs every 5 minutes
setInterval(() => {
    if (document.getElementById('logs').childElementCount > 100) {
        clearLogs();
        log("Auto-cleared old logs", 'info');
    }
}, 300000);