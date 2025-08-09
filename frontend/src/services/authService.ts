import api from "./api";
import {
    ApiResponse,
    LoginResponse,
    AccessTokenResponse,
    LoginCredentials,
    UserAgentResponse
} from '../types/auth';
// axios is imported via api; no direct axios usage here
import { getAccessToken as getStoredAccessToken } from '../utils/AccessTokenUtil';
import { store } from '../redux/store';
import { logout as logoutAction } from '../redux/authSlice';
import mfaService from './mfaService';

interface DeviceRevokeEntry {
    deviceName: string;
    ip: string;
}

interface RevokeDeviceRequest {
    devices: DeviceRevokeEntry[];
}

class AuthService {
    [x: string]: any;
    private static instance: AuthService;
    private accessToken: string | null = null;
    private refreshAccessTokenPromise: Promise<ApiResponse<AccessTokenResponse>> | null = null;
    private refreshAccessTokenAbacPromise: Promise<string> | null = null;
    private currentDeviceInfo: UserAgentResponse | null = null;

    private constructor() {
        this.accessToken = localStorage.getItem('accessToken');
        const storedDeviceInfo = localStorage.getItem('deviceInfo');
        if (storedDeviceInfo) {
            this.currentDeviceInfo = JSON.parse(storedDeviceInfo);
        }
        this.setupAxiosInterceptors();
    }

    public static getInstance(): AuthService {
        if (!AuthService.instance) {
            AuthService.instance = new AuthService();
        }
        return AuthService.instance;
    }

    private setupAxiosInterceptors() {
        api.interceptors.request.use(
            (config) => {
                const token = this.accessToken || getStoredAccessToken();
                if (token) {
                    config.headers.Authorization = `Bearer ${token}`;
                }
                return config;
            },
            (error) => {
                throw (error instanceof Error ? error : new Error(String(error)));
            }
        );

        api.interceptors.response.use(
            (response) => response,
            async (error) => {
                const originalRequest = error.config || {};

                if (error.response?.status === 401 && !originalRequest._retry) {
                    originalRequest._retry = true;

                    try {
                        const authError = error.response?.headers?.['x-auth-error'];
                        const isMfaRequired = authError === 'mfa-required';
                        const isAbacPolicyChanged = authError === 'abac-policy-changed';

                        if (isMfaRequired) {
                            // Prompt user to complete MFA, then retry original request
                            await mfaService.promptMfa();
                            originalRequest.headers = originalRequest.headers || {};
                            originalRequest.headers.Authorization = `Bearer ${this.accessToken || getStoredAccessToken()}`;
                            return api(originalRequest);
                        }

                        if (isAbacPolicyChanged) {
                            const newAccessToken = await this.refreshAccessTokenForAbac();
                            originalRequest.headers = originalRequest.headers || {};
                            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                            return api(originalRequest);
                        }

                        // First try the standard refresh
                        const newTokens = await this.refreshAccessToken();
                        originalRequest.headers = originalRequest.headers || {};
                        originalRequest.headers.Authorization = `Bearer ${newTokens.data.accessToken}`;
                        return api(originalRequest);
                    } catch {
                        try {
                            // If standard refresh failed, attempt ABAC-specific refresh
                            const newAccessToken = await this.refreshAccessTokenForAbac();
                            originalRequest.headers = originalRequest.headers || {};
                            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                            return api(originalRequest);
                        } catch {
                            this.forceLogoutClientSide();
                            return Promise.reject(new Error('Unauthorized: unable to refresh access token'));
                        }
                    }
                }

                throw (error instanceof Error ? error : new Error(String(error)));
            }
        );
    }

    private getHeaders(): Record<string, string> {
        const headers: Record<string, string> = {};

        const token = this.accessToken || getStoredAccessToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        return headers;
    }

    private setTokens(accessToken: string) {
        this.accessToken = accessToken;
        // for backward compatibility with components checking this key
        localStorage.setItem('accessToken', accessToken);
        // keep 'auth' in sync if present
        try {
            const auth = JSON.parse(localStorage.getItem('auth') || 'null');
            if (auth && typeof auth === 'object') {
                auth.accessToken = accessToken;
                localStorage.setItem('auth', JSON.stringify(auth));
            }
        } catch {
            // no-op
        }
    }

    private setDeviceInfo(deviceInfo: UserAgentResponse) {
        this.currentDeviceInfo = deviceInfo;
        localStorage.setItem('deviceInfo', JSON.stringify(deviceInfo));
    }

    async login(credentials: LoginCredentials): Promise<ApiResponse<LoginResponse>> {
        const response = await api.post<ApiResponse<LoginResponse>>(
            '/api/auth/login',
            credentials,
            {
                headers: this.getHeaders()
            }
        );

        if (response.data.success && response.data.data) {
            const { userResponse, authResponse, userAgentResponse } = response.data.data;
            if (authResponse?.accessToken) {
                this.setTokens(authResponse.accessToken);
            }
            if (userAgentResponse) {
                this.setDeviceInfo(userAgentResponse);
            }
            // ensure app-wide consistency
            localStorage.setItem('user', JSON.stringify(userResponse));
            localStorage.setItem('auth', JSON.stringify(authResponse));
            localStorage.setItem('userAgent', JSON.stringify(userAgentResponse));
        }

        return response.data;
    }

    async logout(): Promise<ApiResponse<void>> {
        const response = await api.post<ApiResponse<void>>(
            '/api/auth/v2/logout',
            {},
            {
                headers: this.getHeaders()
            }
        );

        if (response.data.success) {
            this.accessToken = null;
            this.currentDeviceInfo = null;
            localStorage.removeItem('accessToken');
            localStorage.removeItem('deviceInfo');
            localStorage.removeItem('auth');
            localStorage.removeItem('user');
            localStorage.removeItem('userAgent');
        }

        return response.data;
    }

    getCurrentDeviceInfo(): UserAgentResponse | null {
        return this.currentDeviceInfo;
    }

    getAccessToken(): string | null {
        return this.accessToken;
    }

    isAuthenticated(): boolean {
        return !!this.accessToken;
    }

    public getUser(): any {
        const userStr = localStorage.getItem('user');
        return userStr ? JSON.parse(userStr) : null;
    }

    public hasRole(role: string): boolean {
        const user = this.getUser();
        return user?.roles?.includes(role) ?? false;
    }

    async logoutDevice(deviceId: string): Promise<void> {
        await api.post(
            '/api/auth/logout-device',
            { deviceId },
            {
                headers: this.getHeaders()
            }
        );
    }

    private async refreshAccessToken(): Promise<ApiResponse<AccessTokenResponse>> {
        const token = this.accessToken || getStoredAccessToken();
        if (!token) {
            throw new Error('No access token available');
        }

        if (this.refreshAccessTokenPromise) {
            return this.refreshAccessTokenPromise;
        }

        this.refreshAccessTokenPromise = api.post<ApiResponse<AccessTokenResponse>>(
            '/api/auth/v2/access-token',
            {},
            {
                headers: { ...this.getHeaders(), Authorization: `Bearer ${token}` }
            }
        ).then(response => {
            if (response.data.success && response.data.data.accessToken) {
                this.setTokens(response.data.data.accessToken);
            }
            return response.data;
        }).finally(() => {
            this.refreshAccessTokenPromise = null;
        });

        return this.refreshAccessTokenPromise;
    }

    private async refreshAccessTokenForAbac(): Promise<string> {
        const token = this.accessToken || getStoredAccessToken();
        if (!token) {
            throw new Error('No access token available');
        }

        if (this.refreshAccessTokenAbacPromise) {
            return this.refreshAccessTokenAbacPromise;
        }

        this.refreshAccessTokenAbacPromise = api.get<string>(
            '/api/auth/abac/new-access-token',
            {
                headers: { ...this.getHeaders(), Authorization: `Bearer ${token}` },
                // ensure axios does not attempt to parse JSON; response is plain string
                transformResponse: [(data) => data]
            }
        ).then(response => {
            const newAccessToken = (response.data || '').toString();
            if (newAccessToken) {
                this.setTokens(newAccessToken);
            }
            return newAccessToken;
        }).finally(() => {
            this.refreshAccessTokenAbacPromise = null;
        });

        return this.refreshAccessTokenAbacPromise;
    }

    private forceLogoutClientSide(): void {
        // Clear in-memory state
        this.accessToken = null;
        this.currentDeviceInfo = null;

        // Clear storage
        try {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('deviceInfo');
            localStorage.removeItem('auth');
            localStorage.removeItem('user');
            localStorage.removeItem('userAgent');
        } catch {}

        // Clear known non-HttpOnly cookies if possible
        try {
            const expires = 'Thu, 01 Jan 1970 00:00:00 GMT';
            document.cookie = `XSRF-TOKEN=; expires=${expires}; path=/`;
        } catch {}

        // Update Redux state
        try {
            store.dispatch(logoutAction());
        } catch {}

        // Redirect to login
        if (typeof window !== 'undefined') {
            window.location.assign('/login');
        }
    }

    async getAllLoggedInDevices(): Promise<ApiResponse<{ deviceName: string; ipAddress: string; current: boolean; }[]>> {
        try {
            const response = await api.get('/api/device/v1/sessions/all', {
                headers: this.getHeaders()
            });
            return response.data;
        } catch (error) {
            console.log(error);
            throw error;
        }
    }

    async revokeDevices(deviceRevokeEntries: { deviceName: string; ip: string; }[]): Promise<ApiResponse<Record<string, string>>> {
        const response = await api.post<ApiResponse<Record<string, string>>>(
            '/api/device/v1/sessions/revoke',
            {
                devices: deviceRevokeEntries
            },
            {
                headers: this.getHeaders()
            }
        );
        return response.data;
    }

    async getUserAgent(): Promise<UserAgentResponse> {
        try {
            const response = await api.get<ApiResponse<UserAgentResponse>>('/api/auth/user-agent', {
                headers: this.getHeaders()
            });
            return response.data.data;
        } catch (error) {
            console.log(error);
            throw error;
        }
    };
}

export default AuthService.getInstance();
