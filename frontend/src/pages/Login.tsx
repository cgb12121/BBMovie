import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, message, Typography, Card, Space, Alert } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GoogleOutlined, FacebookOutlined } from '@ant-design/icons';
import styled, { keyframes } from 'styled-components';
import Particles from '@tsparticles/react';
import axios from 'axios';

const { Title, Text } = Typography;

const fadeIn = keyframes`
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
`;


const bounce = keyframes`
    0%, 20%, 50%, 80%, 100% {
        transform: translateY(0);
    }
    40% {
        transform: translateY(-10px);
    }
    60% {
        transform: translateY(-5px);
    }
`;

const LoginPageContainer = styled.div`
    min-height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background: url('https://images.unsplash.com/photo-1489599849927-2ee91cede3cf?q=80&w=2070&auto=format&fit=crop') no-repeat center center fixed;
    background-size: cover;
    padding: 1rem;
    position: relative;

    &:before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.6); /* Dark overlay for readability */
        z-index: 1;
    }

    > * {
        position: relative;
        z-index: 2;
    }
`;

const StyledCard = styled(Card)`
    max-width: 450px;
    width: 100%;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    animation: ${fadeIn} 0.8s ease-out;
    padding: 1rem;
`;

const StyledTitle = styled(Title)`
    text-align: center;
    color: #ffffff !important;
    font-family: 'Poppins', sans-serif;
    animation: ${bounce} 1s ease;
`;

const StyledInput = styled(Input)`
    border-radius: 8px;
    padding: 10px 12px;
    background: rgb(240, 240, 240);
    border: 1px solid rgba(0, 0, 0, 0.2);
    color: #000000;
    caret-color: black;
    transition: all 0.3s ease;

    &:hover,
    &:focus {
        border-color: rgb(0, 0, 0);
        background: rgba(255, 255, 255, 0.3);
    }

    &::placeholder {
        color: #444444;
    }

    .ant-input {
        background-color: transparent;
        color: #000000;
    }

    svg {
        color: black !important;
    }

    &:-webkit-autofill {
        box-shadow: 0 0 0px 1000px rgba(255, 255, 255, 0.3) inset !important;
        -webkit-text-fill-color: #000000 !important;
    }
`;

const StyledInputPassword = styled(Input.Password)`
    border-radius: 8px;
    padding: 10px 12px;
    background: rgb(240, 240, 240);
    border: 1px solid rgba(0, 0, 0, 0.2);
    color: #000000;
    caret-color: black;
    transition: all 0.3s ease;

    &:hover,
    &:focus {
        border-color: rgb(0, 0, 0);
        background: rgba(255, 255, 255, 0.3);
    }

    &::placeholder {
        color: #444444;
    }

    .ant-input {
        background-color: transparent;
        color: #000000;
    }

    svg {
        color: black !important;
    }

    &:-webkit-autofill {
        box-shadow: 0 0 0px 1000px rgba(255, 255, 255, 0.3) inset !important;
        -webkit-text-fill-color: #000000 !important;
    }
`;



const StyledButton = styled(Button)`
    border-radius: 8px;
    padding: 10px;
    font-weight: 600;
    transition: all 0.3s ease;
    background: #1890ff;
    border: none;

    &:hover {
        background: #40a9ff;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
    }
`;

const LinksContainer = styled(Space)`
    a {
        color: #40a9ff;
        transition: color 0.3s ease;

        &:hover {
            color: #69b1ff;
        }
    }
`;

interface LoginFormData {
    usernameOrEmail: string;
    password: string;
}

const Login: React.FC = () => {
    const [form] = Form.useForm();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isEmail, setIsEmail] = useState(false);

    const onFinish = async (values: LoginFormData) => {
        try {
            setLoading(true);
            setError(null);
            const response = await axios.post('/api/auth/login', {
                usernameOrEmail: values.usernameOrEmail,
                password: values.password,
            });

            localStorage.setItem('user', JSON.stringify(response.data));

            message.success('Login successful!');
            navigate('/dashboard');
        } catch (error: any) {
            if (error.response) {
                setError(error.response.data.message);
            } else {
                setError('An error occurred during login. Please try again.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        setIsEmail(value.includes('@'));
    };

    return (
        <LoginPageContainer>
            <Particles
                id="tsparticles"
                options={{
                    background: { color: { value: "transparent" } },
                    particles: {
                        number: {
                            value: 50,
                            density: {
                                enable: true
                            },
                        },
                        color: { value: "#ffffff" },
                        opacity: { value: 0.5 },
                        size: { value: 3 },
                        move: { enable: true, speed: 2 },
                    },
                }}
                style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%' }}
            />
            <StyledCard>
                <Space direction="vertical" size="large" style={{ width: '100%' }}>
                    <div style={{ textAlign: 'center' }}>
                        <h1 style={{ color: '#e50914', fontSize: '2rem', marginBottom: 0, fontFamily: 'Poppins, sans-serif' }}>
                            BBMovie
                        </h1>
                    </div>
                    <StyledTitle level={2}>Login</StyledTitle>

                    {error && (
                        <Alert
                            message="Login Failed"
                            description={error}
                            type="error"
                            showIcon
                            style={{ marginBottom: 24 }}
                        />
                    )}

                    <Form
                        form={form}
                        name="login"
                        onFinish={onFinish}
                        layout="vertical"
                        size="large"
                    >
                        <Form.Item
                            name="usernameOrEmail"
                            rules={[
                                { required: true, message: 'Please input your username or email!' },
                            ]}
                        >
                            <StyledInput
                                prefix={isEmail ? <MailOutlined /> : <UserOutlined />}
                                placeholder="Username or Email"
                                autoComplete="username"
                                onChange={handleInputChange}
                            />
                        </Form.Item>

                        <Form.Item
                            name="password"
                            rules={[
                                { required: true, message: 'Please input your password!' },
                            ]}
                        >
                            <StyledInputPassword
                                prefix={<LockOutlined />}
                                placeholder="Password"
                                autoComplete="current-password"
                            />
                        </Form.Item>

                        <Form.Item>
                            <StyledButton type="primary" htmlType="submit" loading={loading} block>
                                Login
                            </StyledButton>
                        </Form.Item>

                        <div style={{ display: 'flex', gap: '16px' }}>
                            <Form.Item style={{ flex: 1, marginBottom: 0 }}>
                                <Button
                                    type="default"
                                    block
                                    icon={<GoogleOutlined style={{ fontSize: 18 }} />}
                                    style={{
                                        borderRadius: 8,
                                        background: '#ffffff',
                                        color: '#3c4043',
                                        border: '1px solid #dadce0',
                                        fontWeight: 500,
                                        boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        gap: 8,
                                    }}
                                    onClick={() => message.info('Google login coming soon!')}
                                    onMouseEnter={(e) => (e.currentTarget.style.background = '#f7f8f8')}
                                    onMouseLeave={(e) => (e.currentTarget.style.background = '#ffffff')}
                                >
                                    Google
                                </Button>
                            </Form.Item>

                            <Form.Item style={{ flex: 1, marginBottom: 0 }}>
                                <Button
                                    type="default"
                                    block
                                    icon={<FacebookOutlined style={{ fontSize: 18 }} />}
                                    style={{
                                        borderRadius: 8,
                                        background: '#1877f2',
                                        color: '#ffffff',
                                        border: '1px solid #1877f2',
                                        fontWeight: 500,
                                        boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        gap: 8,
                                    }}
                                    onClick={() => message.info('Facebook login coming soon!')}
                                    onMouseEnter={(e) => (e.currentTarget.style.background = '#166fe5')}
                                    onMouseLeave={(e) => (e.currentTarget.style.background = '#1877f2')}
                                >
                                    Facebook
                                </Button>
                            </Form.Item>
                        </div>

                        <Form.Item style={{ textAlign: 'center', marginTop: 10 }}>
                            <LinksContainer direction="vertical" size="small">
                                <Text style={{ color: '#d9d9d9' }}>
                                    Don't have an account?{' '}
                                    <a href="/register">Register</a>
                                </Text>
                                <Text style={{ color: '#d9d9d9' }}>
                                    Forgot your password?{' '}
                                    <a href="/forgot-password">Reset Password</a>
                                </Text>
                            </LinksContainer>
                        </Form.Item>
                    </Form>
                </Space>
            </StyledCard>
        </LoginPageContainer>
    );
};

export default Login;