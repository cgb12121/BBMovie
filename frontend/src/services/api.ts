import axios from 'axios';
import { getAccessToken } from '../utils/AccessTokenUtil';

const api = axios.create({
    baseURL: process.env.API_URL,
    withCredentials: true,
    headers: {
        "Content-Type": 'application/json'
    }
});

const serviceBaseUrls: Record<string, string | undefined> = {
    '/api/payment': process.env.REACT_APP_PAYMENT_SERVICE_URL,
    '/api/subscription': process.env.REACT_APP_PAYMENT_SERVICE_URL,
    '/api/v1/subscription': process.env.REACT_APP_PAYMENT_SERVICE_URL,
    '/api/watchlist': process.env.REACT_APP_WATCHLIST_SERVICE_URL,
    '/api/v1/chat': process.env.REACT_APP_AI_SERVICE_URL,
    '/internal/files': process.env.REACT_APP_AI_SERVICE_URL,
    '/health': process.env.REACT_APP_AI_SERVICE_URL,
    '/api/health': process.env.REACT_APP_AI_SERVICE_URL,
};

function getBaseUrlForPath(url?: string): string | undefined {
    if (!url) return undefined;
    const matchedPrefix = Object.keys(serviceBaseUrls)
        .sort((a, b) => b.length - a.length)
        .find(prefix => url.startsWith(prefix));
    const override = matchedPrefix ? serviceBaseUrls[matchedPrefix] : undefined;

    return override || process.env.API_URL;
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
    (config) => {
        const baseURL = getBaseUrlForPath(config.url);
        if (baseURL) {
            config.baseURL = baseURL;
        }

        config.headers = config.headers ?? {};

        if (!config.headers.Authorization) {
            const token = getAccessToken();
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
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
