import axios from 'axios';
import { getAccessToken } from '../utils/AccessTokenUtil';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    withCredentials: true,
    headers: {
        "Content-Type": 'application/json'
    }
});

const serviceBaseUrls: Record<string, string | undefined> = {
    '/api/homepage': import.meta.env.VITE_HOMEPAGE_RECOMMENDATIONS_SERVICE_URL,
    '/api/personalization': import.meta.env.VITE_PERSONALIZATION_SERVICE_URL,
    '/api/watch-history': import.meta.env.VITE_WATCH_HISTORY_SERVICE_URL,
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
            const token = getAccessToken();

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
