import api from "./api";
import type {
    ApiResponse,
    LoginResponse,
    AccessTokenResponse,
    LoginCredentials,
    UserAgentResponse,
    RegisterRequest,
    ResetPasswordRequest,
    ForgotPasswordRequest,
    SendVerificationEmailRequest,
    ChangePasswordRequest,
    LoggedInDeviceResponse,
    MfaSetupResponse,
    MfaVerifyResponse,
    MfaVerifyRequest,
    StudentVerificationRequest,
    StudentVerificationResponse,
    StudentApplicationObject,
    VerificationOutcome,
    JwkKeySet
} from '../types/auth';
import {
    decodeAccessTokenPayload,
    getAccessToken as getStoredAccessToken,
    isTokenExpired
} from '../utils/AccessTokenUtil';
import { store } from '../redux/store';
import { logout as logoutAction } from '../redux/authSlice';
import mfaService from './mfaService';

class AuthService {
    [x: string]: any;
    private static instance: AuthService;
    private accessToken: string | null = null;
    private refreshAccessTokenPromise: Promise<ApiResponse<AccessTokenResponse>> | null = null;
    private refreshAccessTokenAbacPromise: Promise<string> | null = null;
    private currentDeviceInfo: UserAgentResponse | null = null;
    private tokenExpiryTimer: number | null = null;

    private constructor() {
        this.accessToken = localStorage.getItem('accessToken');
        const storedDeviceInfo = localStorage.getItem('deviceInfo');
        if (storedDeviceInfo) {
            this.currentDeviceInfo = JSON.parse(storedDeviceInfo);
        }

        if (this.accessToken) {
            this.scheduleTokenCleanup(this.accessToken);
            if (isTokenExpired(this.accessToken)) {
                this.forceLogoutClientSide();
            }
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
        if ((api.interceptors.response as any)._bbmovieAuthResponse) {
            return;
        }

        const responseInterceptor = api.interceptors.response.use(
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

        (api.interceptors.response as any)._bbmovieAuthResponse = responseInterceptor;
    }

    private getHeaders(): Record<string, string> {
        const headers: Record<string, string> = {};

        const token = this.accessToken || getStoredAccessToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        return headers;
    }

    public setTokens(accessToken: string) {
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

        this.scheduleTokenCleanup(accessToken);
    }

    public setDeviceInfo(deviceInfo: UserAgentResponse) {
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
            '/api/auth/logout',
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

    async register(payload: RegisterRequest): Promise<ApiResponse<unknown>> {
        const response = await api.post<ApiResponse<unknown>>('/api/auth/register', payload);
        return response.data;
    }

    async sendVerification(payload: SendVerificationEmailRequest): Promise<ApiResponse<void>> {
        const response = await api.post<ApiResponse<void>>('/api/auth/send-verification', payload, {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async forgotPassword(payload: ForgotPasswordRequest): Promise<ApiResponse<void>> {
        const response = await api.post<ApiResponse<void>>('/api/auth/forgot-password', payload);
        return response.data;
    }

    async resetPassword(token: string, payload: ResetPasswordRequest): Promise<ApiResponse<void>> {
        const response = await api.post<ApiResponse<void>>('/api/auth/reset-password', payload, {
            params: { token }
        });
        return response.data;
    }

    async changePassword(payload: ChangePasswordRequest): Promise<ApiResponse<void>> {
        const response = await api.post<ApiResponse<void>>('/api/auth/change-password', payload, {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async getOAuth2LoginResponse(): Promise<ApiResponse<LoginResponse>> {
        // OAuth2 callback should rely on fresh server-side context/cookies.
        // Sending an old Authorization header can break callback when keys rotate.
        const response = await api.get<ApiResponse<LoginResponse>>('/api/auth/oauth2-callback');
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

    async logoutDevice(deviceName: string, ipAddress: string): Promise<void> {
        await api.post(
            '/api/device/v1/sessions/revoke',
            {
                devices: [
                    {
                        deviceName,
                        ip: ipAddress
                    }
                ]
            },
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
            '/api/auth/access-token',
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
        // Clear timers
        if (this.tokenExpiryTimer) {
            window.clearTimeout(this.tokenExpiryTimer);
            this.tokenExpiryTimer = null;
        }

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

    async getAllLoggedInDevices(): Promise<ApiResponse<LoggedInDeviceResponse[]>> {
        try {
            const response = await api.get<ApiResponse<LoggedInDeviceResponse[]>>('/api/device/v1/sessions/all', {
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

    async setupMfa(): Promise<MfaSetupResponse> {
        const response = await api.post<MfaSetupResponse>('/api/mfa/setup', {}, {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async verifyMfa(payload: MfaVerifyRequest): Promise<MfaVerifyResponse> {
        const response = await api.post<MfaVerifyResponse>('/api/mfa/verify', payload, {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async applyStudentVerification(payload: StudentVerificationRequest, document: File): Promise<ApiResponse<StudentVerificationResponse>> {
        const formData = new FormData();
        formData.append('payload', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
        formData.append('document', document);
        const response = await api.post<ApiResponse<StudentVerificationResponse>>('/api/student-program/apply', formData, {
            headers: {
                ...this.getHeaders(),
                'Content-Type': 'multipart/form-data'
            }
        });
        return response.data;
    }

    async getStudentApplication(applicationId: string): Promise<ApiResponse<StudentApplicationObject>> {
        const response = await api.get<ApiResponse<StudentApplicationObject>>(`/api/student-program/applications/${applicationId}`, {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async getAllStudentApplications(): Promise<ApiResponse<StudentApplicationObject[]>> {
        const response = await api.get<ApiResponse<StudentApplicationObject[]>>('/api/student-program/applications', {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async getPendingStudentApplications(): Promise<ApiResponse<StudentApplicationObject[]>> {
        const response = await api.get<ApiResponse<StudentApplicationObject[]>>('/api/student-program/applications/pending', {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async getRejectedStudentApplications(): Promise<ApiResponse<StudentApplicationObject[]>> {
        const response = await api.get<ApiResponse<StudentApplicationObject[]>>('/api/student-program/applications/rejected', {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async getStudentAccounts(): Promise<ApiResponse<StudentApplicationObject[]>> {
        const response = await api.get<ApiResponse<StudentApplicationObject[]>>('/api/student-program/students', {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async decideStudentApplication(userId: string, approve: boolean): Promise<ApiResponse<StudentVerificationResponse>> {
        const response = await api.post<ApiResponse<StudentVerificationResponse>>(`/api/student-program/applications/${userId}/decision`, null, {
            params: { approve },
            headers: this.getHeaders()
        });
        return response.data;
    }

    async finalizeStudentApplication(applicationId: string, status: VerificationOutcome, message?: string): Promise<ApiResponse<void>> {
        const response = await api.post<ApiResponse<void>>(`/api/student-program/internal/applications/${applicationId}/finalize`, null, {
            params: { status, message },
            headers: this.getHeaders()
        });
        return response.data;
    }

    async getPublicJwks(): Promise<JwkKeySet> {
        const response = await api.get<JwkKeySet>('/.well-known/jwks.json');
        return response.data;
    }

    async getAdminJwksAll(): Promise<ApiResponse<JwkKeySet>> {
        const response = await api.get<ApiResponse<JwkKeySet>>('/api/admin/jwks/all', {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async getAdminJwksActive(): Promise<ApiResponse<JwkKeySet>> {
        const response = await api.get<ApiResponse<JwkKeySet>>('/api/admin/jwks/active', {
            headers: this.getHeaders()
        });
        return response.data;
    }

    private scheduleTokenCleanup(accessToken: string): void {
        if (this.tokenExpiryTimer) {
            window.clearTimeout(this.tokenExpiryTimer);
            this.tokenExpiryTimer = null;
        }

        const payload = decodeAccessTokenPayload(accessToken);
        const exp = payload?.exp;
        if (!exp || typeof exp !== 'number') {
            return;
        }

        const expirationTime = exp * 1000;
        const now = Date.now();
        const timeUntilExpiry = expirationTime - now;

        if (timeUntilExpiry <= 0) {
            this.forceLogoutClientSide();
            return;
        }

        this.tokenExpiryTimer = window.setTimeout(() => {
            this.forceLogoutClientSide();
        }, timeUntilExpiry);
    }
}

export default AuthService.getInstance();
