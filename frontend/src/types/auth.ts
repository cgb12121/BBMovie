export interface UserResponse {
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    profilePictureUrl: string | null;
}

export interface AuthResponse {
    accessToken: string;
    email: string;
    role: string;
}

export interface UserAgentResponse {
    deviceName: string;
    deviceOs: string;
    deviceIpAddress: string;
    browser: string;
    browserVersion: string;
}

export interface LoginResponse {
    userResponse: UserResponse;
    authResponse: AuthResponse;
    userAgentResponse: UserAgentResponse;
}

export interface ApiResponse<T> {
    success: boolean;
    data: T;
    message: string | null;
    errors: string[] | null;
}

export interface AccessTokenResponse {
    accessToken: string;
}

export interface LoginCredentials {
     username: string;
     password: string;
}

export interface Device {
  deviceName: string;
  deviceIpAddress: string;
  deviceOs: string;
  browser: string;
  browserVersion: string;
}

export interface RevokeDeviceRequest {
  devices: {
    deviceName: string;
  }[];
}