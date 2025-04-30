import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Result, Button, Space, message } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import api from '../services/api';

const EmailVerification: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [verificationStatus, setVerificationStatus] = useState<'success' | 'error' | 'loading'>('loading');
    const [errorMessage, setErrorMessage] = useState<string>('');

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
        }
    };

    if (verificationStatus === 'loading') {
        return (
            <Result
                icon={<LoadingOutlined />}
                title="Verifying your email..."
                subTitle="Please wait while we verify your email address."
            />
        );
    }

    if (verificationStatus === 'success') {
        return (
            <Result
                icon={<CheckCircleOutlined style={{ color: '#52c41a' }} />} // Green
                title="Email Verified Successfully!"
                subTitle="Your email has been verified. You can now login to your account."
                extra={[
                    <Button type="primary" key="login" onClick={() => navigate('/login')}>
                        Go to Login
                    </Button>
                ]}
            />
        );
    }

    return (
        <Result
            icon={<CloseCircleOutlined style={{ color: '#ff4d4f' }} />} // Red
            title="Email Verification Failed"
            subTitle={errorMessage}
            extra={[
                <Space direction="vertical" key="actions" style={{ width: '100%' }}>
                    <Button type="primary" onClick={() => navigate('/register')}>
                        Register Again
                    </Button>
                    <Button onClick={handleResendVerification}>
                        Resend Verification Email
                    </Button>
                </Space>
            ]}
        />
    );
};

export default EmailVerification;