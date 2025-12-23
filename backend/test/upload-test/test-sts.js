const fileInput = document.getElementById('fileInput');
const fileInputArea = document.getElementById('fileInputArea');
const logsDiv = document.getElementById('logs');
const progressBarFill = document.getElementById('progressBarFill');
const progressText = document.getElementById('progressText');
const uploadBtn = document.getElementById('uploadBtn');
const fileInfo = document.getElementById('fileInfo');
const credentialsInfo = document.getElementById('credentialsInfo');

function log(message, data = null) {
    const timestamp = new Date().toLocaleTimeString();
    let logMessage = `[${timestamp}] ${message}`;
    if (data) {
        logMessage += '\n' + JSON.stringify(data, null, 2);
    }
    logsDiv.textContent += logMessage + '\n';
    logsDiv.scrollTop = logsDiv.scrollHeight;
    console.log(logMessage);
}

function updateProgress(percent, message) {
    progressBarFill.style.width = `${percent}%`;
    progressText.textContent = `${Math.round(percent)}% - ${message}`;
}

fileInputArea.addEventListener('click', () => fileInput.click());
fileInputArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    fileInputArea.style.borderColor = 'var(--netflix-red)';
});
fileInputArea.addEventListener('dragleave', () => {
    fileInputArea.style.borderColor = 'var(--netflix-gray-light)';
});
fileInputArea.addEventListener('drop', (e) => {
    e.preventDefault();
    fileInputArea.style.borderColor = 'var(--netflix-gray-light)';
    if (e.dataTransfer.files.length) {
        fileInput.files = e.dataTransfer.files;
        updateFileInfo();
    }
});

fileInput.addEventListener('change', updateFileInfo);

function updateFileInfo() {
    const file = fileInput.files[0];
    if (file) {
        const sizeMB = (file.size / (1024 * 1024)).toFixed(2);
        fileInfo.textContent = `Selected: ${file.name} (${sizeMB} MB)`;
    } else {
        fileInfo.textContent = '';
    }
}

async function startUpload() {
    const jwt = document.getElementById('jwt').value.trim();
    const apiHost = document.getElementById('apiHost').value.replace(/\/$/, '');
    const purpose = document.getElementById('purpose').value;
    const file = fileInput.files[0];

    if (!file) {
        alert('Please select a file!');
        return;
    }
    if (!jwt) {
        alert('Please enter a JWT token!');
        return;
    }

    uploadBtn.disabled = true;
    updateProgress(0, 'Initializing...');
    log('🚀 Starting STS upload...');

    try {
        // 1. Get STS credentials
        log('📥 Requesting STS credentials...');
        const credentialsResponse = await fetch(`${apiHost}/upload/sts/credentials`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                purpose: purpose,
                filename: file.name,
                sizeBytes: file.size,
                contentType: file.type || 'application/octet-stream',
                durationSeconds: 3600
            })
        });

        if (!credentialsResponse.ok) {
            const errorText = await credentialsResponse.text();
            throw new Error(`Failed to get credentials: ${credentialsResponse.status} - ${errorText}`);
        }

        const credentials = await credentialsResponse.json();
        log('✅ Credentials received:', credentials);

        // Display credentials info
        document.getElementById('uploadId').textContent = credentials.uploadId;
        document.getElementById('bucket').textContent = credentials.bucket;
        document.getElementById('objectKey').textContent = credentials.objectKey;
        document.getElementById('endpoint').textContent = credentials.endpoint;
        document.getElementById('expiresAt').textContent = new Date(credentials.expiration).toLocaleString();
        credentialsInfo.style.display = 'block';

        // 2. Initialize MinIO client (browser SDK) with temporary credentials
        log('🔧 Initializing MinIO client (browser SDK)...');

        const endpointUrl = new URL(credentials.endpoint);
        const endPoint = endpointUrl.hostname;
        const port = endpointUrl.port ? parseInt(endpointUrl.port, 10) : (endpointUrl.protocol === 'https:' ? 443 : 9000);
        const useSSL = endpointUrl.protocol === 'https:';

        log(`Endpoint: ${endPoint}, Port: ${port}, SSL: ${useSSL}`);

        const minioClient = new Minio.Client({
            endPoint: endPoint,
            port: port,
            useSSL: useSSL,
            accessKey: credentials.accessKey,
            secretKey: credentials.secretKey,
            region: credentials.region || 'us-east-1'
        });

        log('✅ MinIO client initialized');

        // 3. Upload file using MinIO SDK (putObject with File/Blob)
        log(`📤 Uploading file to ${credentials.bucket}/${credentials.objectKey}...`);
        updateProgress(10, 'Uploading...');

        const startTime = Date.now();

        await new Promise((resolve, reject) => {
            minioClient.putObject(
                credentials.bucket,
                credentials.objectKey,
                file,
                file.size,
                {
                    'Content-Type': file.type || 'application/octet-stream'
                },
                (err, etag) => {
                    if (err) {
                        reject(err);
                        return;
                    }
                    const elapsed = (Date.now() - startTime) / 1000;
                    log(`✅ MinIO putObject completed in ${elapsed.toFixed(2)}s, etag=${etag}`);
                    resolve(etag);
                }
            );
        });

        updateProgress(100, 'Upload complete!');
        log('🎉 File uploaded successfully!');
        log(`📦 Location: ${credentials.bucket}/${credentials.objectKey}`);
        log(`⏰ Upload ID: ${credentials.uploadId}`);

    } catch (error) {
        log(`❌ Upload error: ${error.message}`);
        updateProgress(0, 'Upload failed');
        alert(`Upload failed: ${error.message}`);
    } finally {
        uploadBtn.disabled = false;
    }
}

// Auto-load JWT if available
async function loadJwtToken() {
    try {
        const response = await fetch('jwt-dev.txt');
        if (response.ok) {
            const token = (await response.text()).trim();
            document.getElementById('jwt').value = token;
            log('✅ JWT token loaded from jwt-dev.txt');
        }
    } catch (error) {
        // Ignore if file doesn't exist
    }
}

loadJwtToken();

// Expose startUpload for inline onclick handler
window.startUpload = startUpload;