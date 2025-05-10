import api from "./api";
import { 
    ApiResponse, 
    LoginResponse, 
    AccessTokenResponse,
    LoginCredentials,
    UserAgentResponse,
    Device
} from '../types/auth';


class AuthService {
    private static instance: AuthService;
    private accessToken: string | null = null;
    private refreshAccessTokenPromise: Promise<ApiResponse<AccessTokenResponse>> | null = null;
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
                if (this.accessToken) {
                    config.headers.Authorization = `Bearer ${this.accessToken}`;
                }
                return config;
            },
            (error) => {
                return Promise.reject(new Error(error));
            }
        );

        api.interceptors.response.use(
            (response) => response,
            async (error) => {
                const originalRequest = error.config;

                if (error.response?.status === 401 && !originalRequest._retry) {
                    originalRequest._retry = true;

                    try {
                        const newTokens = await this.refreshAccessToken();
                        originalRequest.headers.Authorization = `Bearer ${newTokens.data.accessToken}`;
                        return api(originalRequest);
                    } catch (refreshError) {
                        this.logout();
                        return Promise.reject(refreshError as Error);
                    }
                }

                return Promise.reject(new Error(error));
            }
        );
    }

    private getHeaders(): Record<string, string> {
        const headers: Record<string, string> = {};

        if (this.accessToken) {
            headers['Authorization'] = `Bearer ${this.accessToken}`;
        }

        return headers;
    }

    private setTokens(accessToken: string) {
        this.accessToken = accessToken;
        localStorage.setItem('accessToken', accessToken);
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
        
        if (response.data.success && response.data.data.authResponse.accessToken) {
            this.setTokens(response.data.data.authResponse.accessToken);
            this.setDeviceInfo(response.data.data.userAgentResponse);
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
        if (!this.accessToken) {
            throw new Error('No access token available');
        }

        if (this.refreshAccessTokenPromise) {
            return this.refreshAccessTokenPromise;
        }

        this.refreshAccessTokenPromise = api.post<ApiResponse<AccessTokenResponse>>(
            '/api/auth/access-token',
            {},
            {
                headers: this.getHeaders()
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

    async getAllLoggedInDevices(): Promise<ApiResponse<Device[]>> {
        const response = await api.get<ApiResponse<Device[]>>('/api/auth/sessions/devices', {
            headers: this.getHeaders()
        });
        return response.data;
    }

    async revokeDevices(deviceNames: string[]): Promise<ApiResponse<string>> {
        const response = await api.post<ApiResponse<string>>(
            '/api/auth/sessions/devices/revoke',
            {
                devices: deviceNames.map(name => ({ deviceName: name }))
            },
            {
                headers: this.getHeaders()
            }
        );
        return response.data;
    }
}

export default AuthService.getInstance();
