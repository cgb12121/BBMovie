export type EndpointMethod = 'GET' | 'POST';

export interface AuthEndpointContract {
    method: EndpointMethod;
    path: string;
    scope: 'public' | 'authenticated' | 'admin' | 'internal';
    notes?: string;
}

export const authApiContracts: Record<string, AuthEndpointContract> = {
    authLogin: { method: 'POST', path: '/api/auth/login', scope: 'public' },
    authRegister: { method: 'POST', path: '/api/auth/register', scope: 'public' },
    authAccessToken: { method: 'POST', path: '/api/auth/access-token', scope: 'public', notes: 'Requires Authorization header' },
    authAbacAccessToken: { method: 'GET', path: '/api/auth/abac/new-access-token', scope: 'public', notes: 'Requires Authorization header' },
    authLogout: { method: 'POST', path: '/api/auth/logout', scope: 'public', notes: 'Requires Authorization header' },
    authVerifyEmail: { method: 'GET', path: '/api/auth/verify-email', scope: 'public', notes: 'token query param' },
    authSendVerification: { method: 'POST', path: '/api/auth/send-verification', scope: 'public' },
    authForgotPassword: { method: 'POST', path: '/api/auth/forgot-password', scope: 'public' },
    authResetPassword: { method: 'POST', path: '/api/auth/reset-password', scope: 'public', notes: 'token query param' },
    authChangePassword: { method: 'POST', path: '/api/auth/change-password', scope: 'authenticated' },
    authOAuthCallback: { method: 'GET', path: '/api/auth/oauth2-callback', scope: 'public' },
    authUserAgent: { method: 'GET', path: '/api/auth/user-agent', scope: 'public' },
    authCsrf: { method: 'GET', path: '/api/auth/csrf', scope: 'public' },

    sessionListAll: { method: 'GET', path: '/api/device/v1/sessions/all', scope: 'authenticated' },
    sessionRevoke: { method: 'POST', path: '/api/device/v1/sessions/revoke', scope: 'authenticated' },

    mfaSetup: { method: 'POST', path: '/api/mfa/setup', scope: 'authenticated' },
    mfaVerify: { method: 'POST', path: '/api/mfa/verify', scope: 'authenticated' },

    studentApply: { method: 'POST', path: '/api/student-program/apply', scope: 'authenticated', notes: 'multipart payload + document' },
    studentApplicationGet: { method: 'GET', path: '/api/student-program/applications/:applicationId', scope: 'authenticated' },
    studentApplicationsAll: { method: 'GET', path: '/api/student-program/applications', scope: 'admin' },
    studentApplicationsPending: { method: 'GET', path: '/api/student-program/applications/pending', scope: 'admin' },
    studentApplicationsRejected: { method: 'GET', path: '/api/student-program/applications/rejected', scope: 'admin' },
    studentAccounts: { method: 'GET', path: '/api/student-program/students', scope: 'admin' },
    studentDecision: { method: 'POST', path: '/api/student-program/applications/:userId/decision', scope: 'admin', notes: 'approve query param' },
    studentFinalize: { method: 'POST', path: '/api/student-program/internal/applications/:applicationId/finalize', scope: 'internal', notes: 'status/message query params' },

    jwkPublic: { method: 'GET', path: '/api/.well-known/jwks.json', scope: 'public' },
    jwkAdminAll: { method: 'GET', path: '/api/admin/jwks/all', scope: 'admin' },
    jwkAdminActive: { method: 'GET', path: '/api/admin/jwks/active', scope: 'admin' }
};
