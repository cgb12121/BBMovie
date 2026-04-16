export interface UserResponse {
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    profilePictureUrl: string | null;
    roles?: string[];
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
     email: string;
     password: string;
}

export interface RegisterRequest {
    email: string;
    username: string;
    password: string;
    confirmPassword: string;
    firstName: string;
    lastName: string;
}

export interface ForgotPasswordRequest {
    email: string;
}

export interface ResetPasswordRequest {
    newPassword: string;
    confirmNewPassword: string;
}

export interface SendVerificationEmailRequest {
    email: string;
}

export interface ChangePasswordRequest {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
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
    ip: string;
  }[];
}

// Matches backend LoggedInDeviceResponse
export interface LoggedInDeviceResponse {
  deviceName: string;
  ipAddress: string;
  current: boolean;
}

export interface StudentVerificationRequest {
    studentId: string;
    fullName: string;
    universityName: string;
    universityDomain: string;
    universityCountry: string;
    graduationYear: number;
    universityEmail: string;
}

export type StudentVerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export interface StudentVerificationResponse {
    status: StudentVerificationStatus;
    documentUrl: string | null;
    matchedUniversity: string | null;
    message: string;
}

export interface StudentApplicationObject {
    id: string;
    email: string;
    displayedUsername: string;
    status: StudentVerificationStatus;
    applyStudentStatusDate: string | null;
    student: boolean;
    studentStatusExpireAt: string | null;
    studentDocumentUrl: string | null;
}

export type VerificationOutcome =
    | 'AUTO_APPROVE'
    | 'AUTO_REJECT'
    | 'NEEDS_REVIEW'
    | 'VERIFIED'
    | 'REJECTED';

export interface MfaSetupResponse {
    secret: string;
    qrCode: string;
}

export interface MfaVerifyRequest {
    code: string;
}

export interface MfaVerifyResponse {
    message: string;
}

export interface JwkKeySet {
    keys: Record<string, unknown>[];
}
