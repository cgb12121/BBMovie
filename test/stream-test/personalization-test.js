const jwtEl = document.getElementById('jwt');
const gatewayEl = document.getElementById('apiGateway');
const userIdEl = document.getElementById('userId');
const limitEl = document.getElementById('limit');
const loadBtn = document.getElementById('loadBtn');
const statusEl = document.getElementById('status');
const resultsEl = document.getElementById('results');

function normalizeGatewayBase(url) {
    return (url || '').trim().replace(/\/+$/, '');
}

async function loadJwtFile() {
    try {
        const res = await fetch('jwt-dev.txt');
        if (res.ok) {
            jwtEl.value = (await res.text()).trim();
        }
    } catch (e) {
        // optional
    }
}

function esc(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/"/g, '&quot;');
}

function renderRows(items) {
    if (!items.length) {
        resultsEl.innerHTML = '<p style="color: var(--netflix-text-secondary);">No recommendations returned.</p>';
        return;
    }
    let html =
        '<table style="width:100%;border-collapse:collapse;font-size:12px;">' +
        '<thead><tr style="text-align:left;border-bottom:1px solid var(--netflix-gray-light);">' +
        '<th style="padding:8px;">Movie ID</th>' +
        '<th style="padding:8px;">Score</th>' +
        '<th style="padding:8px;">Reason</th>' +
        '<th style="padding:8px;"></th>' +
        '</tr></thead><tbody>';

    for (const item of items) {
        const streamUrl = `test-streaming.html?movieId=${encodeURIComponent(item.movieId)}`;
        html += '<tr style="border-bottom:1px solid rgba(255,255,255,0.06);">';
        html += `<td style="padding:8px;font-family:monospace;">${esc(item.movieId)}</td>`;
        html += `<td style="padding:8px;">${Number(item.score || 0).toFixed(4)}</td>`;
        html += `<td style="padding:8px;">${esc(item.reason || '')}</td>`;
        html += `<td style="padding:8px;"><a class="logs-btn" href="${streamUrl}" style="text-decoration:none;color:inherit;">Open stream test</a></td>`;
        html += '</tr>';
    }

    html += '</tbody></table>';
    resultsEl.innerHTML = html;
}

async function loadRecommendations() {
    const jwt = jwtEl.value.trim();
    const base = normalizeGatewayBase(gatewayEl.value);
    const userId = userIdEl.value.trim();
    const limit = Math.max(1, Math.min(100, Number(limitEl.value || 20)));

    statusEl.textContent = '';
    resultsEl.innerHTML = '';

    if (!userId) {
        statusEl.textContent = 'User ID is required.';
        return;
    }

    const url = `${base}/api/personalization/v1/users/${encodeURIComponent(userId)}/recommendations?limit=${limit}`;
    const headers = {};
    if (jwt) {
        headers.Authorization = `Bearer ${jwt}`;
    }

    loadBtn.disabled = true;
    statusEl.textContent = 'Loading...';

    try {
        const res = await fetch(url, { headers });
        if (!res.ok) {
            const text = await res.text().catch(() => '');
            throw new Error(`HTTP ${res.status} ${text}`);
        }
        const data = await res.json();
        const items = Array.isArray(data.items) ? data.items : [];
        statusEl.textContent = `Received ${items.length} recommendation(s).`;
        renderRows(items);
    } catch (e) {
        statusEl.textContent = `Error: ${e.message}`;
    } finally {
        loadBtn.disabled = false;
    }
}

loadBtn.addEventListener('click', loadRecommendations);
loadJwtFile();

