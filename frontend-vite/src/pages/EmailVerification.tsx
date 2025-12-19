import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { message } from 'antd';
import { Mail, CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import api from '../services/api';

const EmailVerification: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [verificationStatus, setVerificationStatus] = useState<'success' | 'error' | 'loading'>('loading');
    const [errorMessage, setErrorMessage] = useState<string>('');
    const [isResending, setIsResending] = useState(false);

    useEffect(() => {
        const verifyEmail = async () => {
            const token = searchParams.get('token');
            if (!token) {
                setVerificationStatus('error');
                setErrorMessage('No verification token provided');
                return;
            }

            try {
                await api.get(`/api/auth/verify-email?token=${token}`);
                setVerificationStatus('success');
            } catch (error: any) {
                setVerificationStatus('error');
                if (error.response) {
                    setErrorMessage(error.response.data.message);
                } else {
                    setErrorMessage('An error occurred during verification. Please try again.');
                }
            }
        };

        verifyEmail();
    }, [searchParams]);

    const handleResendVerification = async () => {
        setIsResending(true);
        try {
            const email = searchParams.get('email');
            if (!email) {
                message.error('No email provided for resending verification');
                return;
            }

            await api.post(`/api/auth/resend-verification?email=${email}`);
            message.success('Verification email has been resent. Please check your email.');
        } catch (error: any) {
            if (error.response) {
                message.error(error.response.data.message);
            } else {
                message.error('Failed to resend verification email. Please try again.');
            }
        } finally {
            setIsResending(false);
        }
    };

    const email = searchParams.get('email') || 'your email';

    // Loading State
    if (verificationStatus === 'loading') {
        return (
            <div className="min-h-screen bg-black flex items-center justify-center px-4 py-12">
                <div className="w-full max-w-md space-y-8">
                    <div className="text-center">
                        <h1 className="text-red-600 text-4xl font-bold tracking-wider">BBMOVIE</h1>
                    </div>

                    <Card className="bg-gray-900 border-gray-800">
                        <CardHeader className="text-center space-y-4">
                            <div className="mx-auto bg-blue-900/20 border border-blue-800 rounded-full p-4 w-fit">
                                <Loader2 className="h-12 w-12 text-blue-500 animate-spin" />
                            </div>
                            <CardTitle className="text-white text-2xl">Verifying Email...</CardTitle>
                            <CardDescription className="text-base">
                                Please wait while we verify your email address.
                            </CardDescription>
                        </CardHeader>
                    </Card>
                </div>
            </div>
        );
    }

    // Success State
    if (verificationStatus === 'success') {
        return (
            <div className="min-h-screen bg-black flex items-center justify-center px-4 py-12">
                <div className="w-full max-w-md space-y-8">
                    <div className="text-center">
                        <h1 className="text-red-600 text-4xl font-bold tracking-wider">BBMOVIE</h1>
                    </div>

                    <Card className="bg-gray-900 border-gray-800">
                        <CardHeader className="text-center space-y-4">
                            <div className="mx-auto bg-green-900/20 border border-green-800 rounded-full p-4 w-fit">
                                <CheckCircle className="h-12 w-12 text-green-500" />
                            </div>
                            <CardTitle className="text-white text-2xl">Email Verified!</CardTitle>
                            <CardDescription className="text-base">
                                Your email has been verified successfully.<br />
                                You can now login to your account.
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <Button
                                className="w-full bg-red-600 hover:bg-red-700 text-white"
                                onClick={() => navigate('/login')}
                            >
                                Go to Login
                            </Button>
                        </CardContent>
                    </Card>
                </div>
            </div>
        );
    }

    // Error State
    return (
        <div className="min-h-screen bg-black flex items-center justify-center px-4 py-12">
            <div className="w-full max-w-md space-y-8">
                <div className="text-center">
                    <h1 className="text-red-600 text-4xl font-bold tracking-wider">BBMOVIE</h1>
                </div>

                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader className="text-center space-y-4">
                        <div className="mx-auto bg-red-900/20 border border-red-800 rounded-full p-4 w-fit">
                            <XCircle className="h-12 w-12 text-red-500" />
                        </div>
                        <CardTitle className="text-white text-2xl">Verification Failed</CardTitle>
                        <CardDescription className="text-base">
                            {errorMessage || 'Failed to verify your email address.'}
                        </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        <div className="bg-gray-800 border border-gray-700 rounded-lg p-4 space-y-3">
                            <div className="flex items-start gap-3">
                                <CheckCircle className="h-5 w-5 text-green-500 flex-shrink-0 mt-0.5" />
                                <p className="text-sm text-gray-300">
                                    Click the link in the email to verify your account
                                </p>
                            </div>
                            <div className="flex items-start gap-3">
                                <CheckCircle className="h-5 w-5 text-green-500 flex-shrink-0 mt-0.5" />
                                <p className="text-sm text-gray-300">
                                    The link will expire in 24 hours
                                </p>
                            </div>
                            <div className="flex items-start gap-3">
                                <CheckCircle className="h-5 w-5 text-green-500 flex-shrink-0 mt-0.5" />
                                <p className="text-sm text-gray-300">
                                    Check your spam folder if you don't see it
                                </p>
                            </div>
                        </div>

                        <div className="space-y-3">
                            <Button
                                variant="outline"
                                className="w-full border-gray-700 text-white hover:bg-gray-800"
                                onClick={handleResendVerification}
                                disabled={isResending}
                            >
                                {isResending ? (
                                    <>
                                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                        Resending...
                                    </>
                                ) : (
                                    'Resend Verification Email'
                                )}
                            </Button>

                            <Button
                                className="w-full bg-red-600 hover:bg-red-700 text-white"
                                onClick={() => navigate('/login')}
                            >
                                Back to Login
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

export default EmailVerification;
