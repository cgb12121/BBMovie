import axios from 'axios';
import { getAccessToken } from '../utils/AccessTokenUtil';

const api = axios.create({
    baseURL: process.env.REACT_APP_API_GATEWAY_URL || 'http://localhost:8765',
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
};

function getBaseUrlForPath(url?: string): string | undefined {
    if (!url) return undefined;
    const matchedPrefix = Object.keys(serviceBaseUrls)
        .sort((a, b) => b.length - a.length)
        .find(prefix => url.startsWith(prefix));
    const override = matchedPrefix ? serviceBaseUrls[matchedPrefix] : undefined;

    return override || process.env.REACT_APP_API_GATEWAY_URL || 'http://localhost:8765';
}

api.interceptors.request.use(
    (config) => {
        const baseURL = getBaseUrlForPath(config.url);
        if (baseURL) {
            config.baseURL = baseURL;
        }

        const token = getAccessToken();
        if (token) {
            config.headers = config.headers ?? {};
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(new Error(error));
    }
);

api.interceptors.request.use(
    (config) => {
        const userAgent = JSON.parse(localStorage.getItem("userAgent") ?? '{}');
        if (userAgent) {
            config.headers = config.headers ?? {};
            config.headers['X-DEVICE-NAME'] = userAgent.deviceName ?? '';
            config.headers['X-DEVICE-OS'] = userAgent.deviceOs ?? '';
            config.headers['X-DEVICE-IP-ADDRESS'] = userAgent.deviceIpAddress ?? '';
            config.headers['X-BROWSER'] = userAgent.browser ?? '';
            config.headers['X-BROWSER-VERSION'] = userAgent.browserVersion ?? '';
        }
        return config;
    },
    (error) => {
        return Promise.reject(new Error(error));
    }
);

api.interceptors.request.use(
    (config) => {
        const token = getCookie('XSRF-TOKEN');
        if (token) {
            config.headers = config.headers ?? {};
            config.headers['X-XSRF-TOKEN'] = token;
        }
        return config;
    },
    (error) => {
        return Promise.reject(new Error(error));
    }
);

function getCookie(name: string): string | null {
    const match = document.cookie.match(new RegExp('(^|;\\s*)(' + name + ')=([^;]*)'));
    return match ? decodeURIComponent(match[3]) : null;
}

export default api;