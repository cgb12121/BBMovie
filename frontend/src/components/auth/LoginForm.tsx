import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, message, Typography, Card, Space, Alert } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';
import axios from 'axios';

const { Title, Text } = Typography;

interface LoginFormData {
    usernameOrEmail: string;
    password: string;
}

const LoginForm: React.FC = () => {
    const [form] = Form.useForm();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const onFinish = async (values: LoginFormData) => {
        try {
            setLoading(true);
            setError(null);
            const response = await axios.post('/api/auth/login', {
                usernameOrEmail: values.usernameOrEmail,
                password: values.password
            });
            
            // Store user data in localStorage or context
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

    return (
        <Card style={{ maxWidth: 400, margin: '0 auto', marginTop: 50 }}>
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
                <Title level={2} style={{ textAlign: 'center' }}>Login</Title>
                <Text type="secondary" style={{ display: 'block', textAlign: 'center', marginBottom: 24 }}>
                    Welcome back to BBMovie
                </Text>

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
                            { required: true, message: 'Please input your username or email!' }
                        ]}
                    >
                        <Input 
                            prefix={<UserOutlined />} 
                            placeholder="Username or Email"
                            autoComplete="username"
                        />
                    </Form.Item>

                    <Form.Item
                        name="password"
                        rules={[
                            { required: true, message: 'Please input your password!' }
                        ]}
                    >
                        <Input.Password 
                            prefix={<LockOutlined />} 
                            placeholder="Password"
                            autoComplete="current-password"
                        />
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" loading={loading} block>
                            Login
                        </Button>
                    </Form.Item>

                    <Form.Item style={{ textAlign: 'center' }}>
                        <Space direction="vertical" size="small">
                            <Text>
                                Don't have an account?{' '}
                                <a href="/register">Register</a>
                            </Text>
                            <Text>
                                Forgot your password?{' '}
                                <a href="/forgot-password">Reset Password</a>
                            </Text>
                        </Space>
                    </Form.Item>
                </Form>
            </Space>
        </Card>
    );
};

export default LoginForm; 