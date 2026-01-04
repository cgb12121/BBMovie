// Configuration
const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks for upload
let autoScrollEnabled = true;
let currentFileId = null;
let currentMovieId = null;
let uploadAbortController = null;

// DOM Elements
const fileInput = document.getElementById('fileInput');
const fileInputArea = document.getElementById('fileInputArea');
const logsDiv = document.getElementById('logs');
const notification = document.getElementById('notification');
const uploadBtn = document.getElementById('uploadBtn');
const createMovieBtn = document.getElementById('createMovieBtn');
const linkFileBtn = document.getElementById('linkFileBtn');
const loadingSpinner = document.getElementById('loadingSpinner');

// Helper Functions
function showNotification(message, type = 'success') {
    notification.textContent = message;
    notification.className = `notification ${type}`;
    notification.classList.add('show');

    setTimeout(() => {
        notification.classList.remove('show');
    }, 4000);
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
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
    const btn = document.getElementById('autoScrollBtn');
    btn.innerHTML = autoScrollEnabled ?
        '<span class="icon">📜</span> Auto-scroll: ON' :
        '<span class="icon">📜</span> Auto-scroll: OFF';
}

function resetUpload() {
    fileInput.value = '';
    currentFileId = null;
    currentMovieId = null;
    updateFileInfo();
    document.getElementById('displayUploadStatus').textContent = 'Not started';
    document.getElementById('displayMovieId').textContent = '-';
    document.getElementById('movieInfo').style.display = 'none';
    uploadBtn.disabled = false;
    linkFileBtn.disabled = true;
    showNotification('Upload form reset', 'success');
}

// File Handling
function updateFileInfo() {
    const file = fileInput.files[0];
    const filenameEl = document.getElementById('displayFilename');
    const sizeEl = document.getElementById('displaySizeBytes');

    if (file) {
        filenameEl.textContent = file.name;
        sizeEl.textContent = formatFileSize(file.size);
    } else {
        filenameEl.textContent = 'No file selected';
        sizeEl.textContent = '-';
        document.getElementById('displayFileId').textContent = '-';
    }
}

// ==========================================
// ONE-CLICK FLOW: Upload → Create → Link
// ==========================================
async function runFullFlow() {
    const jwt = document.getElementById('jwt').value.trim();
    const uploadApiHost = document.getElementById('uploadApiHost').value.replace(/\/$/, '');
    const mediaApiHost = document.getElementById('mediaApiHost').value.replace(/\/$/, '');
    const purpose = document.getElementById('purpose').value;
    const file = fileInput.files[0];
    const title = document.getElementById('movieTitle').value.trim();
    const description = document.getElementById('movieDescription').value.trim();

    // Validation
    if (!file) {
        showNotification('Please select a file!', 'error');
        return;
    }
    if (!jwt) {
        showNotification('Please enter a JWT token!', 'error');
        return;
    }
    if (!title || !description) {
        showNotification('Title and Description are required!', 'error');
        return;
    }

    // Disable all buttons
    uploadBtn.disabled = true;
    createMovieBtn.disabled = true;
    linkFileBtn.disabled = true;
    document.getElementById('fullFlowBtn').disabled = true;
    loadingSpinner.style.display = 'block';
    uploadAbortController = new AbortController();

    try {
        log('🚀 ========== STARTING FULL FLOW ==========');
        
        // ==========================================
        // STEP 1: Upload File
        // ==========================================
        log('📤 STEP 1/3: Uploading file to MinIO...');
        document.getElementById('displayUploadStatus').textContent = 'Step 1/3: Initializing upload...';

        const contentType = file.type || 'application/octet-stream';
        const fileSize = file.size;
        const checksum = await calculateChecksum(file);

        const initRequest = {
            purpose: purpose,
            contentType: contentType,
            sizeBytes: fileSize,
            filename: file.name,
            checksum: checksum,
            sparseChecksum: checksum.substring(0, 16)
        };

        log('📤 Upload init request:', initRequest);

        const initResponse = await fetch(`${uploadApiHost}/upload/init`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(initRequest),
            signal: uploadAbortController.signal
        });

        if (!initResponse.ok) {
            const errorText = await initResponse.text();
            throw new Error(`Upload init failed: ${initResponse.status} - ${errorText}`);
        }

        const initData = await initResponse.json();
        log('✅ Upload initialized:', initData);
        
        currentFileId = initData.uploadId;
        document.getElementById('displayFileId').textContent = currentFileId;
        document.getElementById('displayUploadStatus').textContent = 'Step 1/3: Uploading to MinIO...';

        await uploadFileToMinIO(file, initData.uploadUrl, contentType);
        log('✅ STEP 1 COMPLETE: File uploaded!');

        // ==========================================
        // STEP 2: Create Movie
        // ==========================================
        log('🎬 STEP 2/3: Creating movie draft...');
        document.getElementById('displayUploadStatus').textContent = 'Step 2/3: Creating movie...';

        const movieRequest = {
            title: title,
            description: description,
            director: document.getElementById('movieDirector').value.trim() || null,
            duration: parseInt(document.getElementById('movieDuration').value) || null,
            genre: document.getElementById('movieGenre').value.trim() || null,
            cast: document.getElementById('movieCast').value.trim() || null,
            status: 'DRAFT'
        };

        log('📤 Movie creation request:', movieRequest);

        const movieResponse = await fetch(`${mediaApiHost}/movies`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(movieRequest),
            signal: uploadAbortController.signal
        });

        if (!movieResponse.ok) {
            const errorText = await movieResponse.text();
            throw new Error(`Movie creation failed: ${movieResponse.status} - ${errorText}`);
        }

        const movieData = await movieResponse.json();
        log('✅ Movie created:', movieData);
        
        currentMovieId = movieData.movieId;
        document.getElementById('displayMovieId').textContent = currentMovieId;
        document.getElementById('movieInfo').style.display = 'block';
        log('✅ STEP 2 COMPLETE: Movie created!');

        // ==========================================
        // STEP 3: Link File to Movie
        // ==========================================
        log('🔗 STEP 3/3: Linking file to movie...');
        document.getElementById('displayUploadStatus').textContent = 'Step 3/3: Linking file...';

        const linkResponse = await fetch(`${mediaApiHost}/movies/${currentMovieId}/file`, {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ fileId: currentFileId }),
            signal: uploadAbortController.signal
        });

        if (!linkResponse.ok) {
            const errorText = await linkResponse.text();
            throw new Error(`Link file failed: ${linkResponse.status} - ${errorText}`);
        }

        const linkedMovieData = await linkResponse.json();
        log('✅ File linked to movie:', linkedMovieData);
        log('✅ STEP 3 COMPLETE: File linked!');

        // ==========================================
        // SUCCESS!
        // ==========================================
        document.getElementById('displayUploadStatus').textContent = '✅ Complete! Waiting for transcode...';
        log('🎉 ========== FULL FLOW COMPLETE ==========');
        log('📋 Summary:', {
            fileId: currentFileId,
            movieId: currentMovieId,
            filename: file.name,
            title: title
        });
        showNotification('🎉 Full flow completed! File uploaded and linked.', 'success');

    } catch (error) {
        if (error.name === 'AbortError') {
            log('⚠️ Flow cancelled by user');
            document.getElementById('displayUploadStatus').textContent = 'Cancelled';
            showNotification('Flow cancelled', 'error');
        } else {
            log(`❌ Flow error: ${error.message}`);
            document.getElementById('displayUploadStatus').textContent = 'Failed';
            showNotification(`Flow failed: ${error.message}`, 'error');
        }
    } finally {
        uploadBtn.disabled = false;
        createMovieBtn.disabled = false;
        document.getElementById('fullFlowBtn').disabled = false;
        loadingSpinner.style.display = 'none';
        uploadAbortController = null;
        
        // Enable link button for manual retry if needed
        if (currentFileId && currentMovieId) {
            linkFileBtn.disabled = false;
        }
    }
}

// ==========================================
// Individual Step Functions (for manual use)
// ==========================================
async function startUpload() {
    const jwt = document.getElementById('jwt').value.trim();
    const apiHost = document.getElementById('uploadApiHost').value.replace(/\/$/, '');
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

    uploadBtn.disabled = true;
    loadingSpinner.style.display = 'block';
    document.getElementById('displayUploadStatus').textContent = 'Initializing...';
    uploadAbortController = new AbortController();

    try {
        log('📤 Step 1: Requesting presigned URL from upload-service...');
        
        const contentType = file.type || 'application/octet-stream';
        const fileSize = file.size;
        const checksum = await calculateChecksum(file);
        
        const initRequest = {
            purpose: purpose,
            contentType: contentType,
            sizeBytes: fileSize,
            filename: file.name,
            checksum: checksum,
            sparseChecksum: checksum.substring(0, 16)
        };

        log('📤 Upload init request:', initRequest);

        const initResponse = await fetch(`${apiHost}/upload/init`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(initRequest),
            signal: uploadAbortController.signal
        });

        if (!initResponse.ok) {
            const errorText = await initResponse.text();
            throw new Error(`Upload init failed: ${initResponse.status} - ${errorText}`);
        }

        const initData = await initResponse.json();
        log('✅ Upload initialized successfully:', initData);
        
        currentFileId = initData.uploadId;
        document.getElementById('displayFileId').textContent = currentFileId;
        document.getElementById('displayUploadStatus').textContent = 'Got presigned URL, uploading...';

        log('📤 Step 2: Uploading file to MinIO...');
        await uploadFileToMinIO(file, initData.uploadUrl, contentType);

        document.getElementById('displayUploadStatus').textContent = 'Upload complete!';
        log('🎉 File uploaded successfully! File ID: ' + currentFileId);
        showNotification('File uploaded successfully!', 'success');
        
        if (currentMovieId) {
            linkFileBtn.disabled = false;
        }

    } catch (error) {
        if (error.name === 'AbortError') {
            log('⚠️ Upload cancelled by user');
            document.getElementById('displayUploadStatus').textContent = 'Cancelled';
            showNotification('Upload cancelled', 'error');
        } else {
            log(`❌ Upload error: ${error.message}`);
            document.getElementById('displayUploadStatus').textContent = 'Upload failed';
            showNotification(`Upload failed: ${error.message}`, 'error');
        }
    } finally {
        uploadBtn.disabled = false;
        loadingSpinner.style.display = 'none';
        uploadAbortController = null;
    }
}

async function calculateChecksum(file) {
    const buffer = await file.arrayBuffer();
    const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

async function uploadFileToMinIO(file, uploadUrl, contentType) {
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('PUT', uploadUrl, true);
        xhr.setRequestHeader('Content-Type', contentType);

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                const currentStatus = document.getElementById('displayUploadStatus').textContent;
                // Only update if not in full flow mode
                if (!currentStatus.startsWith('Step')) {
                    document.getElementById('displayUploadStatus').textContent = `Uploading: ${percent}%`;
                }
                log(`📤 Upload progress: ${percent}% (${formatFileSize(e.loaded)} / ${formatFileSize(e.total)})`);
            }
        };

        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                log(`✅ File uploaded to MinIO successfully! Status: ${xhr.status}`);
                resolve();
            } else {
                log(`❌ MinIO upload failed: ${xhr.status} - ${xhr.responseText}`);
                reject(new Error(`MinIO upload failed: ${xhr.status}`));
            }
        };

        xhr.onerror = () => {
            log('❌ Network error during MinIO upload');
            reject(new Error('Network error during MinIO upload'));
        };

        xhr.onabort = () => {
            log('⚠️ MinIO upload aborted');
            reject(new Error('Upload aborted'));
        };

        xhr.send(file);
        log(`🚀 Starting upload to MinIO: ${uploadUrl.substring(0, 80)}...`);
    });
}

// Movie Functions
async function createMovie() {
    const jwt = document.getElementById('jwt').value.trim();
    const apiHost = document.getElementById('mediaApiHost').value.replace(/\/$/, '');
    const title = document.getElementById('movieTitle').value.trim();
    const description = document.getElementById('movieDescription').value.trim();

    if (!jwt) {
        showNotification('Please enter a JWT token!', 'error');
        return;
    }

    if (!title || !description) {
        showNotification('Title and Description are required!', 'error');
        return;
    }

    createMovieBtn.disabled = true;
    loadingSpinner.style.display = 'block';

    try {
        log('🎬 Creating movie draft...');
        
        const movieRequest = {
            title: title,
            description: description,
            director: document.getElementById('movieDirector').value.trim() || null,
            duration: parseInt(document.getElementById('movieDuration').value) || null,
            genre: document.getElementById('movieGenre').value.trim() || null,
            cast: document.getElementById('movieCast').value.trim() || null,
            status: 'DRAFT'
        };

        log('📤 Movie creation request:', movieRequest);

        const response = await fetch(`${apiHost}/movies`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(movieRequest)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Movie creation failed: ${response.status} - ${errorText}`);
        }

        const movieData = await response.json();
        log('✅ Movie created successfully:', movieData);
        
        currentMovieId = movieData.movieId;
        document.getElementById('displayMovieId').textContent = currentMovieId;
        document.getElementById('movieInfo').style.display = 'block';
        
        if (currentFileId) {
            linkFileBtn.disabled = false;
        }

        showNotification('Movie draft created successfully!', 'success');

    } catch (error) {
        log(`❌ Movie creation error: ${error.message}`);
        showNotification(`Movie creation failed: ${error.message}`, 'error');
    } finally {
        createMovieBtn.disabled = false;
        loadingSpinner.style.display = 'none';
    }
}

async function linkFileToMovie() {
    const jwt = document.getElementById('jwt').value.trim();
    const apiHost = document.getElementById('mediaApiHost').value.replace(/\/$/, '');

    if (!jwt) {
        showNotification('Please enter a JWT token!', 'error');
        return;
    }

    if (!currentMovieId) {
        showNotification('Please create a movie first!', 'error');
        return;
    }

    if (!currentFileId) {
        showNotification('Please upload a file first!', 'error');
        return;
    }

    linkFileBtn.disabled = true;
    loadingSpinner.style.display = 'block';

    try {
        log(`🔗 Linking file ${currentFileId} to movie ${currentMovieId}...`);

        const response = await fetch(`${apiHost}/movies/${currentMovieId}/file`, {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ fileId: currentFileId })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Link file failed: ${response.status} - ${errorText}`);
        }

        const movieData = await response.json();
        log('✅ File linked to movie successfully:', movieData);
        
        showNotification('File linked to movie successfully!', 'success');
        document.getElementById('displayUploadStatus').textContent = 'Linked to movie';

    } catch (error) {
        log(`❌ Link file error: ${error.message}`);
        showNotification(`Link file failed: ${error.message}`, 'error');
    } finally {
        linkFileBtn.disabled = false;
        loadingSpinner.style.display = 'none';
    }
}

// Authentication Functions
function toggleAuthSection() {
    const authContent = document.getElementById('authContent');
    const toggleIcon = document.querySelector('.toggle-icon');

    authContent.classList.toggle('expanded');
    toggleIcon.textContent = authContent.classList.contains('expanded') ? '▲' : '▼';
}

async function loadJwtToken() {
    try {
        const response = await fetch('jwt-dev.txt');
        if (response.ok) {
            const token = (await response.text()).trim();
            document.getElementById('jwt').value = token;
            log('✅ JWT token loaded from jwt-dev.txt');
            showNotification('JWT token loaded successfully', 'success');
        } else {
            log('⚠️ Could not load jwt-dev.txt. Status: ' + response.status);
            showNotification('Could not load JWT from file', 'error');
        }
    } catch (error) {
        log('❌ Error loading jwt-dev.txt: ' + error.message);
        showNotification('Error loading JWT: ' + error.message, 'error');
    }
}

function checkAndExpandAuth() {
    const jwt = document.getElementById('jwt').value.trim();
    if (!jwt) {
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

// Event Listeners
fileInput.addEventListener('change', updateFileInfo);

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

// Initialize
loadJwtToken();
checkAndExpandAuth();
