import api from "./api";


export interface LoginCredentials {
    username: string;
    password: string;
}

export interface AccessTokenResponse {
    accessToken: string;
}


class AuthService {
    private static instance: AuthService;
    private accessToken: string | null = null;
    private refreshAccessTokenPromise: Promise<AccessTokenResponse> | null = null;

    private constructor() {
        this.accessToken = localStorage.getItem('accessToken');
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
                        originalRequest.headers.Authorization = `Bearer ${newTokens.accessToken}`;
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

    private async refreshAccessToken(): Promise<AccessTokenResponse> {
        if (!this.accessToken) {
            throw new Error('No new access token available');
        }

        if (this.refreshAccessTokenPromise) {
            return this.refreshAccessTokenPromise;
        }

        this.refreshAccessTokenPromise = api.post<AccessTokenResponse>(`/api/auth/access-token`, {
            accessToken: this.accessToken
        }).then(response => {
            const { accessToken } = response.data;
            this.setTokens(accessToken);
            return response.data;
        }).finally(() => {
            this.refreshAccessTokenPromise = null;
        });

        return this.refreshAccessTokenPromise;
    }

    private setTokens(accessToken: string) {
        this.accessToken = accessToken;
        localStorage.setItem('accessToken', accessToken);
    }

    public async login(credentials: LoginCredentials): Promise<AccessTokenResponse> {
        const response = await api.post<AccessTokenResponse>(`$/api/auth/login`, credentials);
        const { accessToken } = response.data;
        this.setTokens(accessToken);
        return response.data;
    }

    public async logout(): Promise<void> {
        if (this.accessToken) {
            try {
                await api.post(`/api/auth/logout`, {
                    accessToken: this.accessToken
                });
            } catch (error) {
                console.error('Logout error:', error);
            }
        }
        
        this.accessToken = null;
        localStorage.removeItem('accessToken');
    }

    public getAccessToken(): string | null {
        return this.accessToken;
    }

    public isAuthenticated(): boolean {
        return !!this.accessToken;
    }

    public getUser(): any {
        const userStr = localStorage.getItem('user');
        return userStr ? JSON.parse(userStr) : null;
    }

    public hasRole(role: string): boolean {
        const user = this.getUser();
        return user?.roles?.includes(role) || false;
    }
}

export default AuthService.getInstance();
