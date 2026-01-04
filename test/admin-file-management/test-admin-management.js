const NETFLIX_RED = '#e50914';
const NETFLIX_BLACK = '#141414';
let currentPage = 0;
let totalPages = 1;
const PAGE_SIZE = 10;
let totalFiles = 0;

// DOM Elements
const notification = document.getElementById('notification');
const fileListBody = document.getElementById('fileListBody');
const fileCount = document.getElementById('fileCount');

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
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
}

function log(message, data = null) {
    const logsDiv = document.getElementById('logs');
    const timestamp = new Date().toLocaleTimeString();

    let logEntry = `[${timestamp}] ${message}\n`;
    if (data) {
        logEntry += JSON.stringify(data, null, 2) + '\n';
    }

    logsDiv.textContent += logEntry + '\n';
    logsDiv.scrollTop = logsDiv.scrollHeight;

    console.log(`[${timestamp}]`, message, data);
}

function clearLogs() {
    const logsDiv = document.getElementById('logs');
    logsDiv.textContent = '';
    showNotification('Logs cleared', 'success');
}

// Main Functions
async function listMediaFiles(page = 0) {
    const jwt = document.getElementById('jwt').value.trim();
    const apiHost = document.getElementById('apiHost').value.replace(/\/$/, '');

    if (!jwt) {
        showNotification('Please enter admin JWT token', 'error');
        return;
    }

    // Collect filters
    const filters = {
        search: document.getElementById('filterSearch').value,
        status: document.getElementById('filterStatus').value,
        purpose: document.getElementById('filterPurpose').value,
        userId: document.getElementById('filterUserId').value,
        fromDate: document.getElementById('filterFromDate').value,
        toDate: document.getElementById('filterToDate').value
    };

    // Build query parameters
    const queryParams = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
        if (value) queryParams.append(key, value);
    });
    queryParams.append('page', page);
    queryParams.append('size', PAGE_SIZE);

    const url = `${apiHost}/management/files?${queryParams.toString()}`;
    log("Fetching media files...", { url, filters });

    try {
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        const data = await response.json();
        log("Files fetched successfully", { total: data.totalElements });

        renderFileList(data);
        currentPage = data.number;
        totalPages = data.totalPages;
        totalFiles = data.totalElements;
        updatePaginationControls();
        updateFileCount();

    } catch (error) {
        log("Error fetching files:", error.message);
        showNotification(`Error: ${error.message}`, 'error');
        renderEmptyState('Failed to load files');
    }
}

function renderFileList(data) {
    fileListBody.innerHTML = '';

    if (!data.content || data.content.length === 0) {
        renderEmptyState('No files found');
        return;
    }

    data.content.forEach(file => {
        const row = document.createElement('tr');

        // Upload ID
        const uploadIdCell = document.createElement('td');
        uploadIdCell.textContent = file.uploadId;
        uploadIdCell.style.fontFamily = "'Monaco', 'Consolas', monospace";
        uploadIdCell.style.fontSize = "0.85rem";
        row.appendChild(uploadIdCell);

        // Filename
        const filenameCell = document.createElement('td');
        filenameCell.textContent = file.originalFilename || 'N/A';
        filenameCell.style.maxWidth = "200px";
        filenameCell.style.overflow = "hidden";
        filenameCell.style.textOverflow = "ellipsis";
        row.appendChild(filenameCell);

        // User ID
        const userIdCell = document.createElement('td');
        userIdCell.textContent = file.userId || 'N/A';
        userIdCell.style.fontFamily = "'Monaco', 'Consolas', monospace";
        userIdCell.style.fontSize = "0.85rem";
        row.appendChild(userIdCell);

        // Purpose
        const purposeCell = document.createElement('td');
        purposeCell.textContent = file.purpose;
        row.appendChild(purposeCell);

        // Status
        const statusCell = document.createElement('td');
        const statusBadge = document.createElement('span');
        statusBadge.className = `status-badge status-${file.status}`;
        statusBadge.textContent = file.status;
        statusCell.appendChild(statusBadge);
        row.appendChild(statusCell);

        // Mime Type
        const mimeCell = document.createElement('td');
        mimeCell.textContent = file.mimeType || 'N/A';
        mimeCell.style.fontFamily = "'Monaco', 'Consolas', monospace";
        mimeCell.style.fontSize = "0.85rem";
        row.appendChild(mimeCell);

        // Size
        const sizeCell = document.createElement('td');
        sizeCell.textContent = formatFileSize(file.sizeBytes);
        sizeCell.style.fontFamily = "'Monaco', 'Consolas', monospace";
        row.appendChild(sizeCell);

        // Created At
        const dateCell = document.createElement('td');
        dateCell.textContent = formatDate(file.createdAt);
        row.appendChild(dateCell);

        // Actions
        const actionsCell = document.createElement('td');
        const actionButtons = document.createElement('div');
        actionButtons.className = 'action-buttons';

        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'action-btn delete';
        deleteBtn.innerHTML = '<span class="icon">🗑️</span>';
        deleteBtn.title = 'Delete file';
        deleteBtn.onclick = () => confirmAndDelete(file.uploadId, file.originalFilename);

        actionButtons.appendChild(deleteBtn);
        actionsCell.appendChild(actionButtons);
        row.appendChild(actionsCell);

        fileListBody.appendChild(row);
    });
}

function renderEmptyState(message) {
    fileListBody.innerHTML = `
            <tr>
                <td colspan="9">
                    <div class="empty-state">
                        <div class="icon">📁</div>
                        <div>${message}</div>
                    </div>
                </td>
            </tr>
        `;
}

function updatePaginationControls() {
    document.getElementById('currentPageInfo').textContent =
        `Page ${currentPage + 1} of ${totalPages}`;
    document.getElementById('prevPage').disabled = currentPage === 0;
    document.getElementById('nextPage').disabled = currentPage >= totalPages - 1;
}

function updateFileCount() {
    fileCount.textContent = `${totalFiles} files total`;
}

function changePage(delta) {
    listMediaFiles(currentPage + delta);
}

function clearFilters() {
    document.getElementById('filterSearch').value = '';
    document.getElementById('filterStatus').value = '';
    document.getElementById('filterPurpose').value = '';
    document.getElementById('filterUserId').value = '';
    document.getElementById('filterFromDate').value = '';
    document.getElementById('filterToDate').value = '';

    showNotification('Filters cleared', 'success');
    listMediaFiles();
}

function confirmAndDelete(uploadId, filename) {
    const confirmed = confirm(`Are you sure you want to delete:\n\nUpload ID: ${uploadId}\nFilename: ${filename}\n\nThis action cannot be undone!`);

    if (confirmed) {
        deleteMediaFile(uploadId);
    }
}

async function deleteMediaFile(uploadIdFromInput = null) {
    const jwt = document.getElementById('jwt').value.trim();
    const apiHost = document.getElementById('apiHost').value.replace(/\/$/, '');
    const idToDelete = uploadIdFromInput || document.getElementById('deleteUploadId').value.trim();

    if (!idToDelete) {
        showNotification('Please enter an Upload ID to delete', 'error');
        return;
    }

    if (!jwt) {
        showNotification('Please enter admin JWT token', 'error');
        return;
    }

    const url = `${apiHost}/management/files/${idToDelete}`;
    log(`Deleting file: ${idToDelete}`, { url });

    try {
        const response = await fetch(url, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${jwt}`
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        log(`File ${idToDelete} deleted successfully`);
        showNotification(`File ${idToDelete} deleted successfully`, 'success');

        // Clear delete input if it was used
        if (!uploadIdFromInput) {
            document.getElementById('deleteUploadId').value = '';
        }

        // Refresh the file list
        listMediaFiles(currentPage);

    } catch (error) {
        log("Error deleting file:", error.message);
        showNotification(`Delete failed: ${error.message}`, 'error');
    }
}

// Auto-load JWT
async function loadJwtToken() {
    try {
        const response = await fetch('jwt-dev.txt');
        if (response.ok) {
            const token = (await response.text()).trim();
            document.getElementById('jwt').value = token;
            log("JWT token loaded from jwt-dev.txt");
            showNotification('JWT token loaded automatically', 'success');
        }
    } catch (error) {
        console.log("Could not load jwt-dev.txt:", error);
    }
}

// Initialize
loadJwtToken();
listMediaFiles();

// Add keyboard shortcuts
document.addEventListener('keydown', (e) => {
    // Ctrl + L to clear logs
    if (e.ctrlKey && e.key === 'l') {
        e.preventDefault();
        clearLogs();
    }

    // F5 to refresh list
    if (e.key === 'F5') {
        e.preventDefault();
        listMediaFiles(currentPage);
    }
});