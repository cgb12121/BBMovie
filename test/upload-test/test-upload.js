    const NETFLIX_RED = '#e50914';
    const NETFLIX_BLACK = '#141414';
    const NETFLIX_DARK = '#1f1f1f';

    // Configuration
    const CHUNK_SIZE = 1 * 1024 * 1024;
    const SPARSE_CHUNKS_COUNT = 4;
    const UPLOAD_CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks for upload
    const MAX_RETRIES = 3;
    let autoScrollEnabled = true;
    let currentUploadData = null;
    let uploadAbortController = null;

    // DOM Elements
    const fileInput = document.getElementById('fileInput');
    const fileInputArea = document.getElementById('fileInputArea');
    const logsDiv = document.getElementById('logs');
    const notification = document.getElementById('notification');
    const statusDot = document.getElementById('statusDot');
    const statusText = document.getElementById('statusText');
    const progressBarFill = document.getElementById('progressBarFill');
    const progressPercentage = document.getElementById('progressPercentage');
    const progressStatus = document.getElementById('progressStatus');
    const uploadBtn = document.getElementById('uploadBtn');
    const loadingSpinner = document.getElementById('loadingSpinner');
    const autoScrollBtn = document.getElementById('autoScrollBtn');

    // Helper Functions
    function showNotification(message, type = 'success') {
        notification.textContent = message;
        notification.className = `notification ${type}`;
        notification.classList.add('show');

        setTimeout(() => {
            notification.classList.remove('show');
        }, 4000);
    }

    function updateStatus(message, type = 'default') {
        statusDot.className = 'status-dot';
        statusText.textContent = message;

        if (type === 'active') {
            statusDot.classList.add('active');
        } else if (type === 'error') {
            statusDot.classList.add('error');
        }
    }

    function updateProgress(percent, status = '') {
        progressBarFill.style.width = `${percent}%`;
        progressPercentage.textContent = `${Math.round(percent)}%`;
        if (status) {
            progressStatus.textContent = status;
        }
    }

    function arrayBufferToHex(buffer) {
        return Array.from(new Uint8Array(buffer))
            .map(b => b.toString(16).padStart(2, '0'))
            .join('');
    }

    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    // MIME type detection for files that don't have a browser-set MIME type
    function detectMimeType(filename, browserMimeType) {
        // If browser already detected a MIME type, use it (unless it's generic)
        if (browserMimeType && browserMimeType !== 'application/octet-stream') {
            return browserMimeType;
        }

        // Get file extension
        const ext = filename.split('.').pop().toLowerCase();

        // MIME type mapping for AI_ASSET supported file types
        const mimeTypeMap = {
            // Audio formats
            'mp3': 'audio/mpeg',
            'wav': 'audio/wav',
            'm4a': 'audio/mp4',
            // Image formats
            'png': 'image/png',
            'jpg': 'image/jpeg',
            'jpeg': 'image/jpeg',
            // PDF
            'pdf': 'application/pdf',
            // Text formats
            'txt': 'text/plain',
            'md': 'text/markdown',
            'markdown': 'text/markdown',
            'json': 'application/json',
            'xml': 'application/xml',
            'csv': 'text/csv'
        };

        return mimeTypeMap[ext] || 'application/octet-stream';
    }

    function log(message, data = null) {
        const timestamp = new Date().toLocaleTimeString();
        const logEntry = document.createElement('div');
        logEntry.className = 'log-entry';

        let html = `<div class="log-timestamp">[${timestamp}]</div>`;
        html += `<div class="log-message">${message}</div>`;

        if (data) {
            html += `<div class="log-data">${JSON.stringify(data, null, 2)}</div>`;
        }

        logEntry.innerHTML = html;
        logsDiv.appendChild(logEntry);

        if (autoScrollEnabled) {
            logsDiv.scrollTop = logsDiv.scrollHeight;
        }

        console.log(`[${timestamp}]`, message, data);
    }

    function clearLogs() {
        logsDiv.innerHTML = '';
        showNotification('Logs cleared', 'success');
    }

    function toggleAutoScroll() {
        autoScrollEnabled = !autoScrollEnabled;
        autoScrollBtn.innerHTML = autoScrollEnabled ?
            '<span class="icon">📜</span> Auto-scroll: ON' :
            '<span class="icon">📜</span> Auto-scroll: OFF';
        autoScrollBtn.style.background = autoScrollEnabled ? NETFLIX_DARK : NETFLIX_BLACK;
    }

    function resetForm() {
        fileInput.value = '';
        updateFileInfo();
        updateProgress(0, 'Ready');
        updateStatus('Ready');
        uploadBtn.disabled = false;
        showNotification('Form reset successfully', 'success');
    }

    // File Handling
    async function calculateFullHash(file) {
        log("Calculating Full SHA-256 Hash...");
        updateStatus('Calculating full hash...', 'active');

        const buffer = await file.arrayBuffer();
        const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
        const hashHex = arrayBufferToHex(hashBuffer);

        log("Full Hash Calculated:", hashHex);
        return hashHex;
    }

    async function calculateSparseHash(file) {
        log("Calculating Sparse SHA-256 Hash...");
        updateStatus('Calculating sparse hash...', 'active');

        const size = file.size;
        const chunksToHash = [];
        const positions = [0];

        if (size > CHUNK_SIZE) {
            positions.push(Math.floor(size / 3));
            positions.push(Math.floor(size * 2 / 3));
        }
        if (size > CHUNK_SIZE) {
            positions.push(Math.max(0, size - CHUNK_SIZE));
        }

        for (const pos of positions) {
            const slice = file.slice(pos, Math.min(pos + CHUNK_SIZE, size));
            const buffer = await slice.arrayBuffer();
            chunksToHash.push(new Uint8Array(buffer));
        }

        const sizeBuffer = new TextEncoder().encode(size.toString());
        chunksToHash.push(sizeBuffer);

        const combinedBlob = new Blob(chunksToHash);
        const combinedBuffer = await combinedBlob.arrayBuffer();
        const hashBuffer = await crypto.subtle.digest('SHA-256', combinedBuffer);
        const hashHex = arrayBufferToHex(hashBuffer);

        log("Sparse Hash Calculated:", hashHex);
        return hashHex;
    }

    function updateFileInfo() {
        const file = fileInput.files[0];
        const elements = {
            filename: document.getElementById('displayFilename'),
            size: document.getElementById('displaySizeBytes'),
            contentType: document.getElementById('displayContentType'),
            sparseChecksum: document.getElementById('displaySparseChecksum'),
            fullChecksum: document.getElementById('displayFullChecksum')
        };

        if (file) {
            const detectedMimeType = detectMimeType(file.name, file.type);
            elements.filename.textContent = file.name;
            elements.size.textContent = formatFileSize(file.size);
            elements.contentType.textContent = detectedMimeType || 'Unknown';
            elements.sparseChecksum.textContent = 'Calculating...';
            elements.fullChecksum.textContent = 'Calculating...';

            // Store detected MIME type for upload
            file.detectedMimeType = detectedMimeType;

            // Calculate hashes
            calculateHashes(file);
        } else {
            elements.filename.textContent = 'No file selected';
            elements.size.textContent = '-';
            elements.contentType.textContent = '-';
            elements.sparseChecksum.textContent = '-';
            elements.fullChecksum.textContent = '-';
        }
    }

    async function calculateHashes(file) {
        loadingSpinner.style.display = 'block';
        uploadBtn.disabled = true;
        updateStatus('Calculating hashes...', 'active');

        try {
            const [sparseHash, fullHash] = await Promise.all([
                calculateSparseHash(file),
                calculateFullHash(file)
            ]);

            document.getElementById('displaySparseChecksum').textContent = sparseHash;
            document.getElementById('displayFullChecksum').textContent = fullHash;

            showNotification('File hashes calculated successfully', 'success');
        } catch (error) {
            log("Error calculating hashes:", error);
            document.getElementById('displaySparseChecksum').textContent = 'Error';
            document.getElementById('displayFullChecksum').textContent = 'Error';
            showNotification('Error calculating hashes', 'error');
        } finally {
            loadingSpinner.style.display = 'none';
            uploadBtn.disabled = false;
            updateStatus('Ready');
        }
    }

    // Update file input hint based on selected purpose
    function updateFileInputHint() {
        const purpose = document.getElementById('purpose').value;
        const hintElement = document.getElementById('fileInputHint');
        
        const hints = {
            'MOVIE_SOURCE': 'Maximum file size: 2GB • Supported: MP4, MKV, MOV',
            'MOVIE_TRAILER': 'Maximum file size: 2GB • Supported: MP4, WebM',
            'MOVIE_POSTER': 'Maximum file size: 50MB • Supported: JPEG, PNG',
            'USER_AVATAR': 'Maximum file size: 10MB • Supported: JPEG, PNG',
            'AI_ASSET': 'Maximum file size: 100MB • Supported: MP3, WAV, M4A, PNG, JPG, JPEG, PDF, TXT, MD, JSON, XML, CSV'
        };
        
        hintElement.textContent = hints[purpose] || 'Maximum file size: 2GB • All formats supported';
    }

    // Event Listeners
    fileInput.addEventListener('change', updateFileInfo);
    document.getElementById('purpose').addEventListener('change', updateFileInputHint);

    // Drag and Drop
    fileInputArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        fileInputArea.classList.add('drag-over');
    });

    fileInputArea.addEventListener('dragleave', () => {
        fileInputArea.classList.remove('drag-over');
    });

    fileInputArea.addEventListener('drop', (e) => {
        e.preventDefault();
        fileInputArea.classList.remove('drag-over');

        if (e.dataTransfer.files.length) {
            fileInput.files = e.dataTransfer.files;
            updateFileInfo();
            showNotification('File added via drag & drop', 'success');
        }
    });

    async function startUpload() {
        const jwt = document.getElementById('jwt').value.trim();
        const apiHost = document.getElementById('apiHost').value.replace(/\/$/, '');
        const purpose = document.getElementById('purpose').value;
        const file = fileInput.files[0];

        if (!file) {
            showNotification('Please select a file!', 'error');
            return;
        }
        if (!jwt) {
            showNotification('Please enter a JWT token!', 'error');
            return;
        }

        const fullChecksum = document.getElementById('displayFullChecksum').textContent;
        if (!fullChecksum || fullChecksum === '-' || fullChecksum === 'Error') {
            showNotification('Please wait for hashes to be calculated!', 'error');
            return;
        }

        // Prepare for upload
        uploadBtn.disabled = true;
        loadingSpinner.style.display = 'block';
        updateProgress(0, 'Initializing...');
        updateStatus('Initializing upload...', 'active');
        uploadAbortController = new AbortController();

        try {
            // Use detected MIME type if available, otherwise fall back to browser's type
            const contentType = file.detectedMimeType || file.type || detectMimeType(file.name, file.type);
            
            // Calculate chunk size and total chunks
            const totalChunks = Math.ceil(file.size / UPLOAD_CHUNK_SIZE);
            
            // Determine if we should use chunked upload (for files > 10MB)
            const useChunkedUpload = file.size > 10 * 1024 * 1024; // 10MB threshold

            if (useChunkedUpload) {
                log(`📦 File size: ${formatFileSize(file.size)} - Using chunked upload (${totalChunks} chunks)`);
                await uploadFileChunked(file, jwt, apiHost, purpose, contentType, fullChecksum);
            } else {
                log(`📦 File size: ${formatFileSize(file.size)} - Using single upload`);
                await uploadFileSingle(file, jwt, apiHost, purpose, contentType, fullChecksum);
            }

        } catch (error) {
            if (error.name === 'AbortError') {
                log("⚠️ Upload cancelled by user");
                updateStatus('Upload cancelled', 'default');
                showNotification('Upload cancelled', 'error');
            } else {
                log(`❌ Upload error: ${error.message}`);
                updateProgress(0, 'Upload failed');
                updateStatus('Upload failed', 'error');
                showNotification(`Upload failed: ${error.message}`, 'error');
            }
        } finally {
            uploadBtn.disabled = false;
            loadingSpinner.style.display = 'none';
            uploadAbortController = null;
        }
    }

    async function uploadFileSingle(file, jwt, apiHost, purpose, contentType, fullChecksum) {
        const uploadRequest = {
            purpose: purpose,
            contentType: contentType,
            sizeBytes: file.size,
            filename: file.name,
            checksum: fullChecksum,
            sparseChecksum: document.getElementById('displaySparseChecksum').textContent
        };

        log("📤 Initializing single upload with server...", uploadRequest);

        const initResponse = await fetch(`${apiHost}/upload/init`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(uploadRequest),
            signal: uploadAbortController.signal
        });

        if (!initResponse.ok) {
            const errorText = await initResponse.text();
            throw new Error(`Server init failed: ${initResponse.status} - ${errorText}`);
        }

        const initData = await initResponse.json();
        currentUploadData = initData;

        log("✅ Upload initialized successfully:", initData);
        updateProgress(10, 'Server ready, uploading to MinIO...');
        updateStatus('Uploading to MinIO...', 'active');

        await uploadFileToMinIO(file, initData);

        updateProgress(100, 'Upload complete!');
        updateStatus('Upload complete!', 'active');
        log("🎉 Upload process completed successfully!");
        showNotification('File uploaded successfully!', 'success');
    }

    async function uploadFileChunked(file, jwt, apiHost, purpose, contentType, fullChecksum) {
        const totalChunks = Math.ceil(file.size / UPLOAD_CHUNK_SIZE);
        
        // 1. Initialize chunked upload
        const chunkedInitRequest = {
            purpose: purpose,
            contentType: contentType,
            totalSizeBytes: file.size,
            chunkSizeBytes: UPLOAD_CHUNK_SIZE,
            totalChunks: totalChunks,
            filename: file.name,
            checksum: fullChecksum,
            sparseChecksum: document.getElementById('displaySparseChecksum').textContent
        };

        log("📤 Initializing chunked upload with server...", chunkedInitRequest);

        const initResponse = await fetch(`${apiHost}/upload/chunked/init`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(chunkedInitRequest),
            signal: uploadAbortController.signal
        });

        if (!initResponse.ok) {
            const errorText = await initResponse.text();
            throw new Error(`Chunked upload init failed: ${initResponse.status} - ${errorText}`);
        }

        const initData = await initResponse.json();
        currentUploadData = initData;

        log(`✅ Chunked upload initialized: ${initData.totalChunks} chunks`, initData);
        updateProgress(5, `Uploading ${initData.totalChunks} chunks...`);
        updateStatus('Uploading chunks...', 'active');

        // 2. Upload chunks in batches (lazy URL generation)
        const BATCH_SIZE = 10; // Fetch 10 URLs at a time
        let uploadedBytes = 0;

        for (let batchStart = 1; batchStart <= initData.totalChunks; batchStart += BATCH_SIZE) {
            if (uploadAbortController.signal.aborted) {
                throw new Error('Upload aborted');
            }

            const batchEnd = Math.min(batchStart + BATCH_SIZE - 1, initData.totalChunks);
            log(`📥 Fetching chunk URLs for batch ${batchStart}-${batchEnd}...`);

            // Fetch batch URLs
            const batchResponse = await fetch(
                `${apiHost}/upload/${initData.uploadId}/chunks?from=${batchStart}&to=${batchEnd}`,
                {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${jwt}`
                    },
                    signal: uploadAbortController.signal
                }
            );

            if (!batchResponse.ok) {
                const errorText = await batchResponse.text();
                throw new Error(`Failed to get chunk URLs: ${batchResponse.status} - ${errorText}`);
            }

            const batchData = await batchResponse.json();
            log(`✅ Got ${batchData.chunks.length} chunk URLs`);

            // Upload each chunk in this batch
            for (const chunk of batchData.chunks) {
                if (uploadAbortController.signal.aborted) {
                    throw new Error('Upload aborted');
                }

                const fileChunk = file.slice(chunk.startByte, chunk.endByte + 1);
                log(`📤 Uploading chunk ${chunk.partNumber}/${initData.totalChunks} (${formatFileSize(fileChunk.size)})...`);

                let etag = null;
                let retries = 0;

                while (retries < MAX_RETRIES && !etag) {
                    try {
                        etag = await uploadChunkToMinIO(fileChunk, chunk, contentType, retries > 0);
                        
                        // Mark chunk as uploaded
                        await fetch(
                            `${apiHost}/upload/${initData.uploadId}/chunks/${chunk.partNumber}/complete`,
                            {
                                method: 'POST',
                                headers: {
                                    'Authorization': `Bearer ${jwt}`,
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify({ etag: etag }),
                                signal: uploadAbortController.signal
                            }
                        );

                        uploadedBytes += fileChunk.size;
                        const progress = 5 + ((uploadedBytes / file.size) * 90);
                        updateProgress(progress, `Chunk ${chunk.partNumber}/${initData.totalChunks} uploaded (${formatFileSize(uploadedBytes)}/${formatFileSize(file.size)})`);
                        log(`✅ Chunk ${chunk.partNumber}/${initData.totalChunks} uploaded successfully (ETag: ${etag})`);
                        break;
                    } catch (error) {
                        retries++;
                        if (retries >= MAX_RETRIES) {
                            log(`❌ Failed to upload chunk ${chunk.partNumber} after ${MAX_RETRIES} retries`);
                            
                            // Try to get retry URL
                            try {
                                const retryResponse = await fetch(
                                    `${apiHost}/upload/${initData.uploadId}/chunks/${chunk.partNumber}/retry`,
                                    {
                                        method: 'POST',
                                        headers: {
                                            'Authorization': `Bearer ${jwt}`
                                        },
                                        signal: uploadAbortController.signal
                                    }
                                );
                                if (retryResponse.ok) {
                                    const retryChunk = await retryResponse.json();
                                    chunk.uploadUrl = retryChunk.uploadUrl;
                                    retries = 0; // Reset retries for new URL
                                    continue;
                                }
                            } catch (retryError) {
                                log(`⚠️ Failed to get retry URL: ${retryError.message}`);
                            }
                            
                            throw new Error(`Failed to upload chunk ${chunk.partNumber}: ${error.message}`);
                        }
                        log(`⚠️ Retrying chunk ${chunk.partNumber} (attempt ${retries + 1}/${MAX_RETRIES})...`);
                        await new Promise(resolve => setTimeout(resolve, 1000 * retries));
                    }
                }
            }
        }

        // 3. Complete multipart upload
        log("🔗 Completing multipart upload...");
        updateProgress(95, 'Completing upload...');
        updateStatus('Completing upload...', 'active');

        // 3. Complete multipart upload (no need to send parts - backend gets them from tracking)
        const completeRequest = {
            uploadId: initData.uploadId,
            parts: [] // Backend will get parts from chunk tracking
        };

        const completeResponse = await fetch(`${apiHost}/upload/chunked/complete`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(completeRequest),
            signal: uploadAbortController.signal
        });

        if (!completeResponse.ok) {
            const errorText = await completeResponse.text();
            throw new Error(`Complete upload failed: ${completeResponse.status} - ${errorText}`);
        }

        const completeData = await completeResponse.json();
        log("✅ Multipart upload completed successfully!", completeData);

        updateProgress(100, 'Upload complete!');
        updateStatus('Upload complete!', 'active');
        showNotification('File uploaded successfully!', 'success');
    }

    async function uploadChunkToMinIO(chunk, chunkInfo, contentType, isRetry) {
        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open('PUT', chunkInfo.uploadUrl, true);

            xhr.setRequestHeader('Content-Type', contentType);

            xhr.onload = () => {
                if (xhr.status >= 200 && xhr.status < 300) {
                    // Extract ETag from response headers
                    const etag = xhr.getResponseHeader('ETag') || xhr.getResponseHeader('etag');
                    if (!etag) {
                        reject(new Error('ETag not found in response'));
                        return;
                    }
                    // Remove quotes from ETag if present
                    const cleanEtag = etag.replace(/"/g, '');
                    resolve(cleanEtag);
                } else {
                    reject(new Error(`Chunk upload failed: ${xhr.status} - ${xhr.responseText}`));
                }
            };

            xhr.onerror = () => {
                reject(new Error('Network error during chunk upload'));
            };

            xhr.onabort = () => {
                reject(new Error('Chunk upload aborted'));
            };

            if (uploadAbortController) {
                xhr.addEventListener('abort', () => {
                    reject(new Error('Upload aborted'));
                });
            }

            xhr.send(chunk);
        });
    }

    async function uploadFileToMinIO(file, uploadData) {
        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open('PUT', uploadData.uploadUrl, true);

            const contentType = file.detectedMimeType || file.type || detectMimeType(file.name, file.type);
            xhr.setRequestHeader('Content-Type', contentType);

            xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) {
                    const percent = 10 + ((e.loaded / e.total) * 90); // 10-100%
                    updateProgress(percent, `Uploading: ${formatFileSize(e.loaded)} / ${formatFileSize(e.total)}`);
                }
            };

            xhr.onload = () => {
                if (xhr.status >= 200 && xhr.status < 300) {
                    log(`✅ File uploaded to MinIO successfully! Status: ${xhr.status}`);
                    resolve({
                        success: true,
                        status: xhr.status,
                        uploadId: uploadData.uploadId
                    });
                } else {
                    log(`❌ MinIO upload failed: ${xhr.status} - ${xhr.responseText}`);
                    reject(new Error(`MinIO upload failed: ${xhr.status}`));
                }
            };

            xhr.onerror = () => {
                log("❌ Network error during MinIO upload");
                reject(new Error('Network error during MinIO upload'));
            };

            xhr.onabort = () => {
                log("⚠️ MinIO upload aborted");
                reject(new Error('Upload aborted'));
            };

            xhr.send(file);
            log(`🚀 Starting direct upload to MinIO: ${uploadData.objectKey}`);
        });
    }

    function toggleAuthSection() {
        const authContent = document.getElementById('authContent');
        const toggleIcon = document.querySelector('.toggle-icon');

        authContent.classList.toggle('expanded');
        toggleIcon.textContent = authContent.classList.contains('expanded') ? '▲' : '▼';
    }

    // Update loadJwtToken để hiển thị thông báo trong logs
    async function loadJwtToken() {
        try {
            const response = await fetch('jwt-dev.txt');
            if (response.ok) {
                const token = (await response.text()).trim();
                document.getElementById('jwt').value = token;
                log("✅ JWT token loaded from jwt-dev.txt");
                showNotification('JWT token loaded successfully', 'success');
            } else {
                log("⚠️ Could not load jwt-dev.txt. Status: " + response.status);
                showNotification('Could not load JWT from file', 'error');
            }
        } catch (error) {
            log("❌ Error loading jwt-dev.txt: " + error.message);
            showNotification('Error loading JWT: ' + error.message, 'error');
        }
    }

    // Auto-expand auth section nếu JWT chưa có
    function checkAndExpandAuth() {
        const jwt = document.getElementById('jwt').value.trim();
        if (!jwt) {
            // Tự động mở rộng section nếu chưa có JWT
            setTimeout(() => {
                const authContent = document.getElementById('authContent');
                const toggleIcon = document.querySelector('.toggle-icon');
                if (!authContent.classList.contains('expanded')) {
                    authContent.classList.add('expanded');
                    toggleIcon.textContent = '▲';
                }
            }, 500);
        }
    }

    // Initialize
    loadJwtToken();
    updateStatus('Ready');
    updateProgress(0, 'Ready');
    updateFileInputHint(); // Set initial hint based on default purpose
    checkAndExpandAuth();