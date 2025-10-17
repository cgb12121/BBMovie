const decodeBase64Url = (input: string): string => {
    const base64 = input.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - (base64.length % 4 || 4)) % 4, '=');

    if (typeof atob === 'function') {
        return atob(padded);
    }

    if (typeof Buffer !== 'undefined') {
        return Buffer.from(padded, 'base64').toString('binary');
    }

    throw new Error('No base64 decoder available');
};

type JwtPayload = Record<string, unknown>;

export const decodeAccessTokenPayload = (token: string): JwtPayload | null => {
    if (!token) return null;
    const segments = token.split('.');
    if (segments.length < 2) return null;

    try {
        const payloadJson = decodeBase64Url(segments[1]);
        return JSON.parse(payloadJson);
    } catch (error) {
        console.error('Failed to decode access token payload', error);
        return null;
    }
};

export const getTokenExpiry = (token: string): number | null => {
    const payload = decodeAccessTokenPayload(token);
    if (!payload || typeof payload.exp !== 'number') {
        return null;
    }
    return payload.exp * 1000;
};

export const isTokenExpired = (token: string): boolean => {
    const expiry = getTokenExpiry(token);
    if (!expiry) return false;

    const LEEWAY_IN_MILLISECONDS = 6000;

    return expiry - LEEWAY_IN_MILLISECONDS <= Date.now();
};

export const getAccessToken = (): string | null => {
    const auth = localStorage.getItem('auth');
    if (auth) {
        try {
            const parsed = JSON.parse(auth);
            if (parsed?.accessToken) {
                return parsed.accessToken as string;
            }
        } catch {
            // ignore parse errors and fallback
        }
    }

    const directToken = localStorage.getItem('accessToken');
    return directToken ?? null;
};
