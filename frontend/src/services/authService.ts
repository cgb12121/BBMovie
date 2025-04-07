import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL ?? 'http://localhost:8080/api';

export interface LoginCredentials {
    username: string;
    password: string;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
}

class AuthService {
    private static instance: AuthService;
    private accessToken: string | null = null;
    private refreshToken: string | null = null;
    private refreshPromise: Promise<AuthResponse> | null = null;

    private constructor() {
        this.accessToken = localStorage.getItem('accessToken');
        this.refreshToken = localStorage.getItem('refreshToken');
        this.setupAxiosInterceptors();
    }

    public static getInstance(): AuthService {
        if (!AuthService.instance) {
            AuthService.instance = new AuthService();
        }
        return AuthService.instance;
    }

    private setupAxiosInterceptors() {
        axios.interceptors.request.use(
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

        axios.interceptors.response.use(
            (response) => response,
            async (error) => {
                const originalRequest = error.config;

                if (error.response?.status === 401 && !originalRequest._retry) {
                    originalRequest._retry = true;

                    try {
                        const newTokens = await this.refreshAccessToken();
                        originalRequest.headers.Authorization = `Bearer ${newTokens.accessToken}`;
                        return axios(originalRequest);
                    } catch (refreshError) {
                        this.logout();
                        return Promise.reject(refreshError as Error);
                    }
                }

                return Promise.reject(new Error(error));
            }
        );
    }

    private async refreshAccessToken(): Promise<AuthResponse> {
        if (!this.refreshToken) {
            throw new Error('No refresh token available');
        }

        if (this.refreshPromise) {
            return this.refreshPromise;
        }

        this.refreshPromise = axios.post<AuthResponse>(`${API_URL}/auth/refresh`, {
            refreshToken: this.refreshToken
        }).then(response => {
            const { accessToken, refreshToken } = response.data;
            this.setTokens(accessToken, refreshToken);
            return response.data;
        }).finally(() => {
            this.refreshPromise = null;
        });

        return this.refreshPromise;
    }

    private setTokens(accessToken: string, refreshToken: string) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
    }

    public async login(credentials: LoginCredentials): Promise<AuthResponse> {
        const response = await axios.post<AuthResponse>(`${API_URL}/auth/login`, credentials);
        const { accessToken, refreshToken } = response.data;
        this.setTokens(accessToken, refreshToken);
        return response.data;
    }

    public async logout(): Promise<void> {
        if (this.refreshToken) {
            try {
                await axios.post(`${API_URL}/auth/logout`, {
                    refreshToken: this.refreshToken
                });
            } catch (error) {
                console.error('Logout error:', error);
            }
        }
        
        this.accessToken = null;
        this.refreshToken = null;
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
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
