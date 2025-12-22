// Configuration
const CHUNK_SIZE = 1 * 1024 * 1024; // 1MB for sparse hash
let currentSessionId = null;
let jwtToken = null;
let attachments = [];
let eventSource = null;
let selectedFile = null;
let sessions = [];

// DOM Elements
const apiHostInput = document.getElementById('apiHost');
const uploadApiHostInput = document.getElementById('uploadApiHost');
const jwtInput = document.getElementById('jwtInput');
const sessionIdInput = document.getElementById('sessionId');
const assistantTypeSelect = document.getElementById('assistantType');
const aiModeSelect = document.getElementById('aiMode');
const messageInput = document.getElementById('messageInput');
const fileInput = document.getElementById('fileInput');
const fileInputLabel = document.getElementById('fileInputLabel');
const uploadFileBtn = document.getElementById('uploadFileBtn');
const messagesArea = document.getElementById('messagesArea');
const attachmentsPreview = document.getElementById('attachmentsPreview');
const logsDiv = document.getElementById('logs');
const statusDot = document.getElementById('statusDot');
const statusText = document.getElementById('statusText');
const sendBtn = document.getElementById('sendBtn');
const notification = document.getElementById('notification');
const sessionsList = document.getElementById('sessionsList');

// Helper Functions
function showNotification(message, type = 'success') {
    notification.textContent = message;
    notification.className = `notification ${type}`;
    notification.classList.add('show');
    setTimeout(() => {
        notification.classList.remove('show');
    }, 4000);
}

function log(message, type = 'info') {
    const timestamp = new Date().toLocaleTimeString();
    const logEntry = document.createElement('div');
    logEntry.className = `log-entry ${type}`;
    logEntry.textContent = `[${timestamp}] ${message}`;
    logsDiv.appendChild(logEntry);
    logsDiv.scrollTop = logsDiv.scrollHeight;
    console.log(`[${timestamp}]`, message);
}

function updateStatus(message, connected = false) {
    statusText.textContent = message;
    statusDot.className = 'status-dot' + (connected ? ' connected' : '');
}

function updateSessionDisplay() {
    document.getElementById('sessionDisplay').textContent = currentSessionId || 'Not set';
}

function formatTime(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
}

// Sessions Management
async function loadSessions() {
    if (!jwtToken) {
        log('Cannot load sessions: JWT token not available', 'error');
        return;
    }

    const apiHost = apiHostInput.value.replace(/\/$/, '');
    try {
        log('Loading sessions...');
        const response = await fetch(`${apiHost}/api/v1/sessions?size=50`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load sessions: ${response.status}`);
        }

        const result = await response.json();
        // ApiResponse<CursorPageResponse<ChatSessionResponse>>
        // data.content = list of sessions
        if (result.success && result.data && Array.isArray(result.data.content)) {
            sessions = result.data.content;
            renderSessions();
            log(`Loaded ${sessions.length} sessions`, 'success');
        } else {
            sessions = [];
            renderSessions();
            log('No sessions found', 'info');
        }
    } catch (error) {
        log(`Error loading sessions: ${error.message}`, 'error');
        sessions = [];
        renderSessions();
    }
}

function renderSessions() {
    if (!sessionsList) return;

    sessionsList.innerHTML = '';

    if (sessions.length === 0) {
        const emptyDiv = document.createElement('div');
        emptyDiv.className = 'session-item-empty';
        emptyDiv.textContent = 'No sessions yet. Create a new one!';
        sessionsList.appendChild(emptyDiv);
        return;
    }

    sessions.forEach(session => {
        const sessionItem = document.createElement('div');
        sessionItem.className = 'session-item';
        if (currentSessionId === session.sessionId) {
            sessionItem.classList.add('active');
        }
        sessionItem.dataset.sessionId = session.sessionId;

        const nameDiv = document.createElement('div');
        nameDiv.className = 'session-item-name';
        nameDiv.textContent = session.sessionName || 'Untitled Session';

        const timeDiv = document.createElement('div');
        timeDiv.className = 'session-item-time';
        timeDiv.textContent = formatTime(session.updatedAt);

        sessionItem.appendChild(nameDiv);
        sessionItem.appendChild(timeDiv);

        sessionItem.addEventListener('click', () => {
            loadSessionMessages(session.sessionId);
        });

        sessionsList.appendChild(sessionItem);
    });
}

async function loadSessionMessages(sessionId) {
    if (!jwtToken) {
        showNotification('JWT token not loaded', 'error');
        return;
    }

    if (!sessionId) {
        showNotification('Invalid session ID', 'error');
        return;
    }

    const apiHost = apiHostInput.value.replace(/\/$/, '');
    try {
        log(`Loading messages for session ${sessionId}...`);
        updateStatus('Loading messages...', false);

        // Update active session in sidebar
        document.querySelectorAll('.session-item').forEach(item => {
            item.classList.remove('active');
            if (item.dataset.sessionId === sessionId) {
                item.classList.add('active');
            }
        });

        // Set current session
        currentSessionId = sessionId;
        sessionIdInput.value = sessionId;
        updateSessionDisplay();

        // Clear current messages
        messagesArea.innerHTML = '';

        // Load messages (load more pages if needed)
        let cursor = null;
        let allMessages = [];
        let hasMore = true;

        while (hasMore) {
            const cursorParam = cursor ? `&cursor=${encodeURIComponent(cursor)}` : '';
            const url = `${apiHost}/api/v1/messages/${sessionId}?size=50${cursorParam}`;
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${jwtToken}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to load messages: ${response.status}`);
            }

            const result = await response.json();
            // ApiResponse<CursorPageResponse<ChatMessage>>
            if (result.success && result.data && Array.isArray(result.data.content)) {
                const messages = result.data.content;
                allMessages = [...messages, ...allMessages]; // Prepend older messages
                cursor = result.data.nextCursor;
                hasMore = !!cursor && result.data.hasMore && messages.length > 0;
            } else {
                hasMore = false;
            }
        }

        // Sort messages by timestamp (oldest first)
        allMessages.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));

        // Display messages
        allMessages.forEach(msg => {
            const sender = msg.sender === 'USER' ? 'user' : 'assistant';
            let fileAttachments = null;

            // Parse file content if available
            if (msg.fileContentJson) {
                try {
                    const fileContent = JSON.parse(msg.fileContentJson);
                    if (fileContent.file_references && fileContent.file_references.length > 0) {
                        fileAttachments = fileContent.file_references.map(ref => {
                            // Extract filename from URL or use a default
                            const urlParts = ref.split('/');
                            return urlParts[urlParts.length - 1] || 'File';
                        });
                    }
                } catch (e) {
                    log(`Error parsing file content JSON: ${e.message}`, 'error');
                }
            }

            addMessage(sender, msg.content || '', fileAttachments);
        });

        // Scroll to bottom
        messagesArea.scrollTop = messagesArea.scrollHeight;
        log(`Loaded ${allMessages.length} messages`, 'success');
        updateStatus('Ready', false);

        // Refresh sessions list to update timestamps
        loadSessions();

    } catch (error) {
        log(`Error loading messages: ${error.message}`, 'error');
        showNotification(`Failed to load messages: ${error.message}`, 'error');
        updateStatus('Error', false);
    }
}

// Update JWT Token from manual input
function updateJwtToken() {
    const token = jwtInput.value.trim();
    if (token) {
        jwtToken = token;
        document.getElementById('jwtStatus').textContent = 'Manual ✓';
        log('JWT token updated from manual input', 'success');
        updateStatus('Ready');
        loadSessions(); // Load sessions after JWT is set
    } else {
        jwtToken = null;
        document.getElementById('jwtStatus').textContent = 'Not set';
    }
}

// Load JWT Token from file
async function loadJwtToken() {
    try {
        const response = await fetch('jwt-dev.txt');
        if (response.ok) {
            const token = (await response.text()).trim();
            jwtToken = token;
            jwtInput.value = token; // Also populate the input field
            document.getElementById('jwtStatus').textContent = 'Loaded from file ✓';
            log('JWT token loaded successfully from jwt-dev.txt', 'success');
            updateStatus('Ready');
            showNotification('JWT token loaded from file', 'success');
            loadSessions(); // Load sessions after JWT is loaded
        } else {
            document.getElementById('jwtStatus').textContent = 'File not found';
            log('Could not load jwt-dev.txt. Please enter JWT manually.', 'error');
            showNotification('Could not load JWT from file. Please enter manually.', 'error');
        }
    } catch (error) {
        document.getElementById('jwtStatus').textContent = 'Error loading file';
        log(`Error loading jwt-dev.txt: ${error.message}. Please enter JWT manually.`, 'error');
        showNotification('Error loading JWT file. Please enter manually.', 'error');
    }
}

// Create New Session
async function createSession() {
    if (!jwtToken) {
        showNotification('Please load JWT token first', 'error');
        return;
    }

    const apiHost = apiHostInput.value.replace(/\/$/, '');
    try {
        log('Creating new session...');
        const response = await fetch(`${apiHost}/api/v1/sessions`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ sessionName: 'New Chat' })
        });

        if (!response.ok) {
            throw new Error(`Failed to create session: ${response.status}`);
        }

        const result = await response.json();
        if (result.success && result.data) {
            const session = result.data;
            currentSessionId = session.sessionId;
            sessionIdInput.value = currentSessionId;
            updateSessionDisplay();
            log(`Session created: ${currentSessionId}`, 'success');
            showNotification('Session created successfully', 'success');
            
            // Clear messages
            messagesArea.innerHTML = '';
            
            // Refresh sessions list and load the new session
            await loadSessions();
            await loadSessionMessages(currentSessionId);
        }
    } catch (error) {
        log(`Error creating session: ${error.message}`, 'error');
        showNotification(`Failed to create session: ${error.message}`, 'error');
    }
}

// MIME type detection
function detectMimeType(filename, browserMimeType) {
    if (browserMimeType && browserMimeType !== 'application/octet-stream') {
        return browserMimeType;
    }
    const ext = filename.split('.').pop().toLowerCase();
    const mimeTypeMap = {
        'mp3': 'audio/mpeg', 'wav': 'audio/wav', 'm4a': 'audio/mp4',
        'png': 'image/png', 'jpg': 'image/jpeg', 'jpeg': 'image/jpeg',
        'pdf': 'application/pdf',
        'txt': 'text/plain', 'md': 'text/markdown', 'json': 'application/json',
        'xml': 'application/xml', 'csv': 'text/csv'
    };
    return mimeTypeMap[ext] || 'application/octet-stream';
}

// Hash calculation functions
function arrayBufferToHex(buffer) {
    return Array.from(new Uint8Array(buffer))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('');
}

async function calculateFullHash(file) {
    log('Calculating full SHA-256 hash...');
    const buffer = await file.arrayBuffer();
    const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
    return arrayBufferToHex(hashBuffer);
}

async function calculateSparseHash(file) {
    log('Calculating sparse SHA-256 hash...');
    const size = file.size;
    const chunksToHash = [];
    const positions = [0];
    
    if (size > CHUNK_SIZE) {
        positions.push(Math.floor(size / 3));
        positions.push(Math.floor(size * 2 / 3));
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
    return arrayBufferToHex(hashBuffer);
}

// Handle File Selection
fileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
        selectedFile = file;
        file.detectedMimeType = detectMimeType(file.name, file.type);
        fileInputLabel.textContent = `📎 ${file.name}`;
        fileInputLabel.classList.add('uploading');
        uploadFileBtn.style.display = 'flex';
        log(`File selected: ${file.name} (${formatFileSize(file.size)})`);
        showNotification(`File selected: ${file.name}. Click "Upload File" to upload.`, 'success');
    } else {
        selectedFile = null;
        fileInputLabel.textContent = '📎 Select File';
        fileInputLabel.classList.remove('uploading');
        uploadFileBtn.style.display = 'none';
    }
});

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Upload File to media-upload-service
async function uploadSelectedFile() {
    if (!selectedFile) {
        showNotification('No file selected', 'error');
        return;
    }

    if (!jwtToken) {
        showNotification('JWT token required for upload', 'error');
        return;
    }

    uploadFileBtn.disabled = true;
    fileInputLabel.textContent = '⏳ Calculating hashes...';
    updateStatus('Calculating hashes...', false);

    try {
        // Calculate hashes
        const [sparseHash, fullHash] = await Promise.all([
            calculateSparseHash(selectedFile),
            calculateFullHash(selectedFile)
        ]);

        log(`Hashes calculated - Sparse: ${sparseHash.substring(0, 16)}..., Full: ${fullHash.substring(0, 16)}...`);

        // Initialize upload
        const uploadApiHost = uploadApiHostInput.value.replace(/\/$/, '');
        const contentType = selectedFile.detectedMimeType || selectedFile.type || detectMimeType(selectedFile.name, selectedFile.type);
        
        const uploadRequest = {
            purpose: 'AI_ASSET',
            contentType: contentType,
            sizeBytes: selectedFile.size,
            filename: selectedFile.name,
            checksum: fullHash,
            sparseChecksum: sparseHash
        };

        log('Initializing upload with media-upload-service...', uploadRequest);
        fileInputLabel.textContent = '⏳ Initializing upload...';
        updateStatus('Initializing upload...', false);

        const initResponse = await fetch(`${uploadApiHost}/upload/init`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(uploadRequest)
        });

        if (!initResponse.ok) {
            const errorText = await initResponse.text();
            throw new Error(`Upload init failed: ${initResponse.status} - ${errorText}`);
        }

        const initData = await initResponse.json();
        log('Upload initialized:', { uploadId: initData.uploadId, objectKey: initData.objectKey });

        // Upload to MinIO
        fileInputLabel.textContent = '⏳ Uploading to MinIO...';
        updateStatus('Uploading to MinIO...', false);

        await uploadFileToMinIO(selectedFile, initData);

        // Success - add as attachment
        addAttachment(initData.uploadId, selectedFile.name);
        log(`File uploaded successfully! Upload ID: ${initData.uploadId}`, 'success');
        showNotification(`File uploaded: ${selectedFile.name}`, 'success');

        // Reset file input
        selectedFile = null;
        fileInput.value = '';
        fileInputLabel.textContent = '📎 Select File';
        fileInputLabel.classList.remove('uploading');
        uploadFileBtn.style.display = 'none';
        updateStatus('Ready', false);

    } catch (error) {
        log(`Upload error: ${error.message}`, 'error');
        showNotification(`Upload failed: ${error.message}`, 'error');
        fileInputLabel.textContent = '📎 Select File';
        fileInputLabel.classList.remove('uploading');
        updateStatus('Error', false);
    } finally {
        uploadFileBtn.disabled = false;
    }
}

async function uploadFileToMinIO(file, uploadData) {
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('PUT', uploadData.uploadUrl, true);

        const contentType = file.detectedMimeType || file.type || detectMimeType(file.name, file.type);
        xhr.setRequestHeader('Content-Type', contentType);

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                fileInputLabel.textContent = `⏳ Uploading... ${percent}%`;
            }
        };

        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                log(`File uploaded to MinIO successfully! Status: ${xhr.status}`);
                resolve({ success: true, status: xhr.status });
            } else {
                reject(new Error(`MinIO upload failed: ${xhr.status}`));
            }
        };

        xhr.onerror = () => reject(new Error('Network error during MinIO upload'));
        xhr.onabort = () => reject(new Error('Upload aborted'));

        xhr.send(file);
    });
}

// Add Attachment (by uploadId)
function addAttachment(uploadId, filename) {
    attachments.push({ uploadId, filename });
    updateAttachmentsPreview();
}

// Add Attachment by Upload ID (manual input)
function addAttachmentById() {
    const uploadIdInput = document.getElementById('uploadIdInput');
    const filenameInput = document.getElementById('filenameInput');
    const uploadId = uploadIdInput.value.trim();
    const filename = filenameInput.value.trim();

    if (!uploadId) {
        showNotification('Please enter an upload ID', 'error');
        return;
    }

    addAttachment(uploadId, filename || null);
    uploadIdInput.value = '';
    filenameInput.value = '';
    log(`Attachment added: ${filename || uploadId}`, 'success');
}

function removeAttachment(index) {
    attachments.splice(index, 1);
    updateAttachmentsPreview();
}

function updateAttachmentsPreview() {
    attachmentsPreview.innerHTML = '';
    attachments.forEach((att, index) => {
        const preview = document.createElement('div');
        preview.className = 'attachment-preview';
        preview.innerHTML = `
            <span>📎 ${att.filename || att.uploadId}</span>
            <span class="remove" onclick="removeAttachment(${index})">×</span>
        `;
        attachmentsPreview.appendChild(preview);
    });
}

// Send Message
async function sendMessage() {
    const message = messageInput.value.trim();
    if (!message) {
        showNotification('Please enter a message', 'error');
        return;
    }

    if (!currentSessionId) {
        showNotification('Please create or select a session', 'error');
        return;
    }

    if (!jwtToken) {
        showNotification('JWT token not loaded', 'error');
        return;
    }

    // Add user message to UI
    addMessage('user', message, attachments.length > 0 ? attachments.map(a => a.filename || a.uploadId) : null);
    
    // Clear input
    messageInput.value = '';
    const messageToSend = message;

    // Prepare request
    const apiHost = apiHostInput.value.replace(/\/$/, '');
    const requestBody = {
        message: messageToSend,
        assistantType: assistantTypeSelect.value,
        aiMode: aiModeSelect.value,
        attachments: attachments.map(att => ({
            uploadId: att.uploadId,
            filename: att.filename
        }))
    };

    log(`Sending message to session ${currentSessionId}...`);
    log(`Request: ${JSON.stringify(requestBody, null, 2)}`);

    // Clear attachments
    attachments = [];
    updateAttachmentsPreview();

    // Disable send button
    sendBtn.disabled = true;
    updateStatus('Sending...', false);

    try {
        // Close previous event source if exists
        if (eventSource) {
            eventSource.close();
        }

        // Create assistant message placeholder
        const assistantMessageId = 'msg-' + Date.now();
        const assistantMessageEl = addMessage('assistant', '', null, assistantMessageId);
        const assistantTextEl = assistantMessageEl.querySelector('.message-text');

        // Send POST request and handle SSE stream
        const response = await fetch(`${apiHost}/api/v1/chat/${currentSessionId}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${await response.text()}`);
        }

        // Read SSE stream
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let hasReceivedData = false;

        updateStatus('Receiving...', true);
        log('Starting to read SSE stream...');

        while (true) {
            const { done, value } = await reader.read();
            if (done) {
                log('Stream reader done');
                break;
            }

            const chunk = decoder.decode(value, { stream: true });
            buffer += chunk;
            
            // Process complete SSE events (separated by double newlines)
            const eventBlocks = buffer.split('\n\n');
            buffer = eventBlocks.pop() || ''; // Keep incomplete event in buffer

            for (const eventBlock of eventBlocks) {
                const trimmedBlock = eventBlock.trim();
                if (!trimmedBlock) continue;
                
                let eventData = null;
                
                // Check if the entire block is a data line (common case)
                if (trimmedBlock.startsWith('data:')) {
                    const colonIndex = trimmedBlock.indexOf(':');
                    const jsonStr = trimmedBlock.substring(colonIndex + 1).trim();
                    
                    if (jsonStr && jsonStr !== '[DONE]') {
                        try {
                            eventData = JSON.parse(jsonStr);
                            log(`✓ Parsed JSON: type=${eventData.type}, content="${eventData.content ? eventData.content.substring(0, 30) : 'none'}..."`);
                            hasReceivedData = true;
                            handleStreamChunk(eventData, assistantTextEl);
                            continue; // Successfully processed, move to next block
                        } catch (e) {
                            log(`JSON parse error: ${e.message}`, 'error');
                            log(`Raw JSON string: ${jsonStr.substring(0, 200)}`, 'error');
                        }
                    }
                }
                
                // If not processed as whole block, try parsing line by line
                const lines = trimmedBlock.split('\n');
                for (const line of lines) {
                    const trimmedLine = line.trim();
                    if (!trimmedLine) continue;
                    
                    // Handle both "data: {...}" and "data:{...}" formats
                    if (trimmedLine.startsWith('data:')) {
                        const colonIndex = trimmedLine.indexOf(':');
                        const jsonStr = trimmedLine.substring(colonIndex + 1).trim();
                        
                        if (!jsonStr || jsonStr === '[DONE]') {
                            continue;
                        }
                        
                        try {
                            eventData = JSON.parse(jsonStr);
                            log(`✓ Parsed JSON: type=${eventData.type}, content="${eventData.content ? eventData.content.substring(0, 30) : 'none'}..."`);
                            hasReceivedData = true;
                            handleStreamChunk(eventData, assistantTextEl);
                        } catch (e) {
                            log(`JSON parse error: ${e.message}`, 'error');
                        }
                    } else if (trimmedLine.startsWith('event:')) {
                        const colonIndex = trimmedLine.indexOf(':');
                        const eventType = trimmedLine.substring(colonIndex + 1).trim();
                        log(`SSE Event type: ${eventType}`);
                    } else if (trimmedLine.startsWith('id:')) {
                        const colonIndex = trimmedLine.indexOf(':');
                        const eventId = trimmedLine.substring(colonIndex + 1).trim();
                        log(`SSE Event ID: ${eventId}`);
                    }
                }
            }
        }

        // Remove streaming indicator
        assistantMessageEl.classList.remove('message-streaming');
        updateStatus('Ready', false);
        
        if (hasReceivedData) {
            log('Message sent and response received', 'success');
            // Refresh sessions list to update timestamp
            loadSessions();
        } else {
            log('Warning: Stream ended but no data was received', 'error');
            if (!assistantTextEl.textContent.trim()) {
                assistantTextEl.textContent = '[No response received from AI. Check logs for details.]';
            }
        }

    } catch (error) {
        log(`Error sending message: ${error.message}`, 'error');
        showNotification(`Failed to send message: ${error.message}`, 'error');
        updateStatus('Error', false);
        
        // Update assistant message with error
        const assistantMessageEl = document.getElementById(assistantMessageId);
        if (assistantMessageEl) {
            assistantMessageEl.classList.remove('message-streaming');
            assistantMessageEl.querySelector('.message-text').textContent = `Error: ${error.message}`;
        }
    } finally {
        sendBtn.disabled = false;
    }
}

function handleStreamChunk(chunk, textElement) {
    if (!chunk) {
        log('handleStreamChunk called with null/undefined chunk', 'error');
        return;
    }
    
    const chunkType = chunk.type || 'unknown';
    const hasContent = !!(chunk.content && chunk.content.trim());
    
    log(`Processing chunk - type: ${chunkType}, has content: ${hasContent}, content preview: ${chunk.content ? chunk.content.substring(0, 50) : 'none'}...`);
    
    if (chunkType === 'assistant' && chunk.content) {
        textElement.textContent += chunk.content;
        // Auto-scroll to bottom
        messagesArea.scrollTop = messagesArea.scrollHeight;
        log(`Added ${chunk.content.length} chars to assistant message`);
    } else if (chunkType === 'approval_required' || (chunk.permissionRequired && chunk.approvalRequest)) {
        // Handle approval request
        log(`Approval required: ${chunk.content || 'Action requires approval'}`);
        if (chunk.content) {
            textElement.textContent += chunk.content + '\n\n';
        }
        displayApprovalRequest(chunk.approvalRequest, textElement);
    } else if (chunkType === 'system') {
        log(`System message: ${chunk.content || 'No content'}`);
        if (chunk.content) {
            textElement.textContent += `[System] ${chunk.content}\n`;
        }
    } else if (chunkType === 'tool') {
        log(`Tool message: ${chunk.content || 'No content'}`);
    } else if (chunkType === 'rag_result') {
        log(`RAG Results: ${chunk.ragResults ? chunk.ragResults.length : 0} movies found`);
    } else {
        log(`Unknown/empty chunk type: ${chunkType}`, 'error');
        log(`Full chunk: ${JSON.stringify(chunk)}`, 'error');
        // Try to display any content field regardless of type
        if (chunk.content && chunk.content.trim()) {
            textElement.textContent += chunk.content;
            log(`Displayed content from unknown chunk type`);
        }
    }
}

function displayApprovalRequest(approvalRequest, textElement) {
    if (!approvalRequest || !approvalRequest.requestId) {
        log('Invalid approval request data', 'error');
        return;
    }

    const approvalContainer = document.createElement('div');
    approvalContainer.className = 'approval-request';
    approvalContainer.dataset.requestId = approvalRequest.requestId;

    const riskLevel = approvalRequest.riskLevel || 'UNKNOWN';
    const riskColors = {
        'LOW': '#4CAF50',
        'MEDIUM': '#FF9800',
        'HIGH': '#F44336',
        'EXTREME': '#9C27B0',
        'UNKNOWN': '#757575'
    };
    const riskColor = riskColors[riskLevel] || riskColors['UNKNOWN'];

    approvalContainer.innerHTML = `
        <div class="approval-header" style="border-left: 4px solid ${riskColor};">
            <div class="approval-title">
                <span class="approval-icon">⚠️</span>
                <span>Action Requires Approval</span>
            </div>
            <div class="approval-risk" style="color: ${riskColor};">
                Risk Level: <strong>${riskLevel}</strong>
            </div>
        </div>
        <div class="approval-details">
            <div class="approval-detail-item">
                <span class="detail-label">Action Type:</span>
                <span class="detail-value">${approvalRequest.actionType || 'N/A'}</span>
            </div>
            <div class="approval-detail-item">
                <span class="detail-label">Description:</span>
                <span class="detail-value">${approvalRequest.description || 'No description provided'}</span>
            </div>
            ${approvalRequest.displayParams && Object.keys(approvalRequest.displayParams).length > 0 ? `
                <div class="approval-detail-item">
                    <span class="detail-label">Parameters:</span>
                    <div class="detail-params">
                        ${Object.entries(approvalRequest.displayParams).map(([key, value]) => 
                            `<div class="param-item"><strong>${key}:</strong> ${value}</div>`
                        ).join('')}
                    </div>
                </div>
            ` : ''}
            <div class="approval-detail-item">
                <span class="detail-label">Request ID:</span>
                <span class="detail-value code">${approvalRequest.requestId}</span>
            </div>
        </div>
        <div class="approval-actions">
            <button class="approval-btn approve-btn" onclick="handleApprovalDecision('${approvalRequest.requestId}', 'APPROVE')">
                ✓ Approve
            </button>
            <button class="approval-btn reject-btn" onclick="handleApprovalDecision('${approvalRequest.requestId}', 'REJECT')">
                ✗ Reject
            </button>
        </div>
    `;

    // Insert approval UI after the text content (append to message-content, not message-text)
    const messageContent = textElement.closest('.message-content');
    if (messageContent) {
        messageContent.appendChild(approvalContainer);
    } else {
        // Fallback: append to parent of textElement
        textElement.parentElement.appendChild(approvalContainer);
    }
    messagesArea.scrollTop = messagesArea.scrollHeight;
    log(`Approval request displayed: ${approvalRequest.requestId}`);
}

async function handleApprovalDecision(requestId, decision) {
    if (!currentSessionId) {
        showNotification('No active session', 'error');
        return;
    }

    if (!jwtToken) {
        showNotification('JWT token not loaded', 'error');
        return;
    }

    const apiHost = apiHostInput.value.replace(/\/$/, '');
    const decisionValue = decision === 'APPROVE' ? 'APPROVE' : 'REJECT';
    
    log(`Sending ${decisionValue} decision for request ${requestId}...`);
    updateStatus(`Processing ${decisionValue.toLowerCase()}...`, false);

    // Disable buttons
    const approvalContainer = document.querySelector(`.approval-request[data-request-id="${requestId}"]`);
    if (approvalContainer) {
        const buttons = approvalContainer.querySelectorAll('.approval-btn');
        buttons.forEach(btn => btn.disabled = true);
    }

    try {
        const response = await fetch(`${apiHost}/api/v1/chat/${currentSessionId}/approve/${requestId}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ decision: decisionValue })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        // Update approval UI to show decision
        if (approvalContainer) {
            approvalContainer.classList.add(`decision-${decisionValue.toLowerCase()}`);
            const actionsDiv = approvalContainer.querySelector('.approval-actions');
            if (actionsDiv) {
                actionsDiv.innerHTML = `<div class="decision-status">Decision: <strong>${decisionValue}</strong></div>`;
            }
        }

        // Read SSE stream for the response
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        
        // Find or create assistant message element for the response
        let assistantMessageEl = document.querySelector('.message.assistant:last-child');
        if (!assistantMessageEl || assistantMessageEl.classList.contains('message-streaming')) {
            const assistantMessageId = 'msg-' + Date.now();
            assistantMessageEl = addMessage('assistant', '', null, assistantMessageId);
        }
        const assistantTextEl = assistantMessageEl.querySelector('.message-text');
        assistantMessageEl.classList.add('message-streaming');

        updateStatus('Receiving response...', true);

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });
            buffer += chunk;
            
            const eventBlocks = buffer.split('\n\n');
            buffer = eventBlocks.pop() || '';

            for (const eventBlock of eventBlocks) {
                const trimmedBlock = eventBlock.trim();
                if (!trimmedBlock) continue;
                
                let eventData = null;
                
                if (trimmedBlock.startsWith('data:')) {
                    const colonIndex = trimmedBlock.indexOf(':');
                    const jsonStr = trimmedBlock.substring(colonIndex + 1).trim();
                    
                    if (jsonStr && jsonStr !== '[DONE]') {
                        try {
                            eventData = JSON.parse(jsonStr);
                            handleStreamChunk(eventData, assistantTextEl);
                        } catch (e) {
                            log(`JSON parse error: ${e.message}`, 'error');
                        }
                    }
                }
            }
        }

        assistantMessageEl.classList.remove('message-streaming');
        updateStatus('Ready', false);
        log(`Approval decision processed: ${decisionValue}`, 'success');
        showNotification(`Decision ${decisionValue.toLowerCase()}d successfully`, 'success');

    } catch (error) {
        log(`Error processing approval decision: ${error.message}`, 'error');
        showNotification(`Failed to process decision: ${error.message}`, 'error');
        updateStatus('Error', false);
        
        // Re-enable buttons on error
        if (approvalContainer) {
            const buttons = approvalContainer.querySelectorAll('.approval-btn');
            buttons.forEach(btn => btn.disabled = false);
        }
    }
}

function addMessage(type, text, fileAttachments = null, id = null) {
    const messageEl = document.createElement('div');
    messageEl.className = `message ${type}`;
    if (id) messageEl.id = id;
    if (type === 'assistant' && !text) messageEl.classList.add('message-streaming');

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.textContent = type === 'user' ? '👤' : '🤖';

    const content = document.createElement('div');
    content.className = 'message-content';

    const meta = document.createElement('div');
    meta.className = 'message-meta';
    meta.textContent = type === 'user' ? 'You' : 'AI Assistant';

    const textEl = document.createElement('div');
    textEl.className = 'message-text';
    textEl.textContent = text;

    content.appendChild(meta);
    content.appendChild(textEl);

    if (fileAttachments && fileAttachments.length > 0) {
        const attachmentsEl = document.createElement('div');
        attachmentsEl.className = 'attachments';
        fileAttachments.forEach(filename => {
            const attEl = document.createElement('div');
            attEl.className = 'attachment-item';
            attEl.textContent = `📎 ${filename}`;
            attachmentsEl.appendChild(attEl);
        });
        content.appendChild(attachmentsEl);
    }

    messageEl.appendChild(avatar);
    messageEl.appendChild(content);
    messagesArea.appendChild(messageEl);
    messagesArea.scrollTop = messagesArea.scrollHeight;

    return messageEl;
}

// Handle Enter key
messageInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

// Handle session ID input
sessionIdInput.addEventListener('change', (e) => {
    const value = e.target.value.trim();
    if (value) {
        currentSessionId = value;
        updateSessionDisplay();
        log(`Session ID set to: ${currentSessionId}`);
        loadSessionMessages(currentSessionId);
    }
});

// Initialize
loadJwtToken();
updateSessionDisplay();
updateStatus('Ready', false);

