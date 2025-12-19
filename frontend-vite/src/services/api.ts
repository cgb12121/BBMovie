import axios from 'axios';
import { getAccessToken } from '../utils/AccessTokenUtil';

// Determine current profile/environment.
// In Vite, env vars must be prefixed with VITE_ and accessed via import.meta.env.
// We treat anything that is NOT explicit production as "dev-like"
// and allow loading jwt-dev.txt in those profiles.
const appProfile =
    import.meta.env.VITE_PROFILE ||
    import.meta.env.MODE ||
    'unknown';

const isDevLikeProfile =
    appProfile.toLowerCase() !== 'prod' &&
    appProfile.toLowerCase() !== 'production';

// Cached dev JWT (loaded from /jwt-dev.txt in the frontend root for dev/test/default/unknown profiles)
let devJwtToken: string | null = null;
let devJwtLoaded = false;

async function loadDevJwtToken(): Promise<string | null> {
    if (!isDevLikeProfile) {
        return null;
    }

    if (devJwtLoaded) {
        return devJwtToken;
    }

    // For dev-like profiles, read a static JWT from env instead of jwt-dev.txt.
    // Define VITE_DEV_JWT in your .env (or .env.development) for local testing.
    const raw = import.meta.env.VITE_DEV_JWT ?? '';
    const token = raw.trim();

    devJwtToken = token || null;
    devJwtLoaded = true;
    return devJwtToken;
}

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    withCredentials: true,
    headers: {
        "Content-Type": 'application/json'
    }
});

const serviceBaseUrls: Record<string, string | undefined> = {
    '/api/payment': import.meta.env.VITE_PAYMENT_SERVICE_URL,
    '/api/subscription': import.meta.env.VITE_PAYMENT_SERVICE_URL,
    '/api/subscriptions': import.meta.env.VITE_PAYMENT_SERVICE_URL,
    '/api/v1/subscription': import.meta.env.VITE_PAYMENT_SERVICE_URL,
    '/api/v1/subscriptions': import.meta.env.VITE_PAYMENT_SERVICE_URL,
    '/api/watchlist': import.meta.env.VITE_WATCHLIST_SERVICE_URL,
    '/api/v1/watchlist': import.meta.env.VITE_WATCHLIST_SERVICE_URL,
    '/api/v1/chat': import.meta.env.VITE_AI_SERVICE_URL,
    '/internal/files': import.meta.env.VITE_AI_SERVICE_URL,
    '/health': import.meta.env.VITE_AI_SERVICE_URL,
    '/api/health': import.meta.env.VITE_AI_SERVICE_URL,
};

function getBaseUrlForPath(url?: string): string | undefined {
    if (!url) return undefined;

    // Sort prefixes by length (descending) to match longer prefixes first
    const matchedPrefix = Object.keys(serviceBaseUrls)
        .sort((a, b) => b.length - a.length)
        .find(prefix => url.startsWith(prefix));
    const override = matchedPrefix ? serviceBaseUrls[matchedPrefix] : undefined;

    return override || import.meta.env.VITE_API_URL;
}

function appendDeviceHeaders(headers: Record<string, unknown>): void {
    try {
        const stored = localStorage.getItem('userAgent');
        if (!stored) return;
        const userAgent = JSON.parse(stored ?? '{}');
        headers['X-DEVICE-NAME'] = userAgent?.deviceName ?? '';
        headers['X-DEVICE-OS'] = userAgent?.deviceOs ?? '';
        headers['X-DEVICE-IP-ADDRESS'] = userAgent?.deviceIpAddress ?? '';
        headers['X-BROWSER'] = userAgent?.browser ?? '';
        headers['X-BROWSER-VERSION'] = userAgent?.browserVersion ?? '';
    } catch {
        headers['X-DEVICE-NAME'] ??= '';
        headers['X-DEVICE-OS'] ??= '';
        headers['X-DEVICE-IP-ADDRESS'] ??= '';
        headers['X-BROWSER'] ??= '';
        headers['X-BROWSER-VERSION'] ??= '';
    }
}

api.interceptors.request.use(
    async (config) => {
        const baseURL = getBaseUrlForPath(config.url);
        if (baseURL) {
            config.baseURL = baseURL;
        }

        config.headers = config.headers ?? {};

        if (!config.headers.Authorization) {
            // 1. Try normal access token (used in real app flows)
            let token = getAccessToken();

            // 2. For dev-like profiles (dev/test/default/unknown),
            //    fall back to jwt-dev.txt from the frontend root.
            if (!token && isDevLikeProfile) {
                const devToken = await loadDevJwtToken();
                if (devToken) {
                    token = devToken;
                }
            }

            if (token) {
                // Allow token to already include "Bearer " prefix or be raw JWT
                config.headers.Authorization = token.startsWith('Bearer ')
                    ? token
                    : `Bearer ${token}`;
            }
        }

        appendDeviceHeaders(config.headers);

        return config;
    },
    (error) => {
        return Promise.reject(new Error(error));
    }
);

// api.interceptors.request.use(
//     (config) => {
//         const token = getCookie('XSRF-TOKEN');
//         if (token) {
//             config.headers = config.headers ?? {};
//             config.headers['X-XSRF-TOKEN'] = token;
//         }
//         return config;
//     },
//     (error) => {
//         return Promise.reject(new Error(error));
//     }
// );


export default api;
