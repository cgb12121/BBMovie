const jwtEl = document.getElementById('jwt');
const gatewayEl = document.getElementById('apiGateway');
const loadBtn = document.getElementById('loadBtn');
const listStatus = document.getElementById('listStatus');
const tableWrap = document.getElementById('tableWrap');

function normalizeGatewayBase(url) {
    return (url || '').trim().replace(/\/+$/, '');
}

function formatTs(epochSec) {
    if (!Number.isFinite(epochSec)) return '—';
    return new Date(epochSec * 1000).toLocaleString();
}

function formatSec(s) {
    if (!Number.isFinite(s) || s <= 0) return '—';
    const m = Math.floor(s / 60);
    const sec = Math.floor(s % 60);
    return `${m}:${String(sec).padStart(2, '0')} (${s.toFixed(1)}s)`;
}

async function loadJwtFile() {
    try {
        const res = await fetch('jwt-dev.txt');
        if (res.ok) {
            jwtEl.value = (await res.text()).trim();
        }
    } catch (e) {
        /* optional */
    }
}

function renderTable(rows) {
    if (!rows.length) {
        tableWrap.innerHTML = '<p style="color: var(--netflix-text-secondary);">No saved resume rows.</p>';
        return;
    }
    const esc = (s) =>
        String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/"/g, '&quot;');
    let html =
        '<table style="width:100%; border-collapse:collapse; font-size:12px;">' +
        '<thead><tr style="text-align:left; border-bottom:1px solid var(--netflix-gray-light);">' +
        '<th style="padding:8px;">Movie ID</th>' +
        '<th style="padding:8px;">Position</th>' +
        '<th style="padding:8px;">Duration</th>' +
        '<th style="padding:8px;">Done</th>' +
        '<th style="padding:8px;">Updated</th>' +
        '<th style="padding:8px;"></th>' +
        '</tr></thead><tbody>';
    for (const r of rows) {
        const id = r.movieId;
        const streamUrl = `test-streaming.html?movieId=${encodeURIComponent(id)}`;
        html += '<tr style="border-bottom:1px solid rgba(255,255,255,0.06);">';
        html += `<td style="padding:8px; font-family:monospace;">${esc(id)}</td>`;
        html += `<td style="padding:8px;">${esc(formatSec(Number(r.positionSec)))}</td>`;
        html += `<td style="padding:8px;">${esc(formatSec(Number(r.durationSec)))}</td>`;
        html += `<td style="padding:8px;">${r.completed ? 'yes' : 'no'}</td>`;
        html += `<td style="padding:8px;">${esc(formatTs(Number(r.updatedAtEpochSec)))}</td>`;
        html += `<td style="padding:8px;"><a class="logs-btn" href="${streamUrl}" style="text-decoration:none;color:inherit;">Open in stream test</a></td>`;
        html += '</tr>';
    }
    html += '</tbody></table>';
    tableWrap.innerHTML = html;
}

async function loadItems() {
    const jwt = jwtEl.value.trim();
    const base = normalizeGatewayBase(gatewayEl.value);
    listStatus.textContent = '';
    tableWrap.innerHTML = '';
    if (!jwt) {
        listStatus.textContent = 'Paste a JWT first.';
        return;
    }
    loadBtn.disabled = true;
    listStatus.textContent = 'Loading…';
    try {
        const all = [];
        let cursor = '0';
        const limit = 50;
        let guard = 0;
        for (;;) {
            const url = `${base}/api/watch-history/v1/items?cursor=${encodeURIComponent(cursor)}&limit=${limit}`;
            const res = await fetch(url, {
                headers: { Authorization: `Bearer ${jwt}` }
            });
            if (!res.ok) {
                const t = await res.text().catch(() => '');
                throw new Error(`HTTP ${res.status} ${t}`);
            }
            const data = await res.json();
            if (!data || !Array.isArray(data.items)) {
                throw new Error('Expected { items, nextCursor }');
            }
            all.push(...data.items);
            const next = data.nextCursor;
            if (next == null || next === '' || next === '0') {
                break;
            }
            cursor = String(next);
            guard += 1;
            if (guard > 200) {
                break;
            }
        }
        all.sort((a, b) => Number(b.updatedAtEpochSec) - Number(a.updatedAtEpochSec));
        listStatus.textContent = `${all.length} item(s) (HSCAN pages merged).`;
        renderTable(all);
    } catch (e) {
        listStatus.textContent = `Error: ${e.message}`;
    } finally {
        loadBtn.disabled = false;
    }
}

loadBtn.addEventListener('click', loadItems);
loadJwtFile();
