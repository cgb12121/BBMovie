import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, message, Typography, Card, Space } from 'antd';
import { UserOutlined, MailOutlined, LockOutlined } from '@ant-design/icons';
import axios from 'axios';

const { Title, Text } = Typography;

interface RegistrationFormData {
    username: string;
    email: string;
    password: string;
    confirmPassword: string;
    firstName: string;
    lastName: string;
}

const RegistrationForm: React.FC = () => {
    const [form] = Form.useForm();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);

    const onFinish = async (values: RegistrationFormData) => {
        try {
            setLoading(true);
            await axios.post('/api/auth/register', {
                username: values.username,
                email: values.email,
                password: values.password,
                firstName: values.firstName,
                lastName: values.lastName
            });
            message.success('Registration successful! Please check your email for verification.');
            navigate('/login');
        } catch (error: any) {
            if (error.response) {
                message.error(error.response.data.message);
            } else {
                message.error('An error occurred during registration. Please try again.');
            }
        } finally {
            setLoading(false);
        }
    };

    const validatePassword = (_: any, value: string) => {
        if (!value) {
            return Promise.reject('Please input your password!');
        }
        if (value.length < 8) {
            return Promise.reject('Password must be at least 8 characters long!');
        }
        if (!/[A-Z]/.test(value)) {
            return Promise.reject('Password must contain at least one uppercase letter!');
        }
        if (!/[a-z]/.test(value)) {
            return Promise.reject('Password must contain at least one lowercase letter!');
        }
        if (!/\d/.test(value)) {
            return Promise.reject('Password must contain at least one number!');
        }
        if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value)) {
            return Promise.reject('Password must contain at least one special character!');
        }
        return Promise.resolve();
    };

    return (
        <Card style={{ maxWidth: 400, margin: '0 auto', marginTop: 50 }}>
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
                <Title level={2} style={{ textAlign: 'center' }}>Register</Title>
                <Text type="secondary" style={{ display: 'block', textAlign: 'center', marginBottom: 24 }}>
                    Create your BBMovie account
                </Text>

                <Form
                    form={form}
                    name="register"
                    onFinish={onFinish}
                    layout="vertical"
                    size="large"
                >
                    <Form.Item
                        name="username"
                        rules={[
                            { required: true, message: 'Please input your username!' },
                            { min: 3, message: 'Username must be at least 3 characters long!' }
                        ]}
                    >
                        <Input prefix={<UserOutlined />} placeholder="Username" />
                    </Form.Item>

                    <Form.Item
                        name="email"
                        rules={[
                            { required: true, message: 'Please input your email!' },
                            { type: 'email', message: 'Please enter a valid email!' }
                        ]}
                    >
                        <Input prefix={<MailOutlined />} placeholder="Email" />
                    </Form.Item>

                    <Form.Item
                        name="firstName"
                        rules={[{ required: true, message: 'Please input your first name!' }]}
                    >
                        <Input placeholder="First Name" />
                    </Form.Item>

                    <Form.Item
                        name="lastName"
                        rules={[{ required: true, message: 'Please input your last name!' }]}
                    >
                        <Input placeholder="Last Name" />
                    </Form.Item>

                    <Form.Item
                        name="password"
                        rules={[
                            { required: true, message: 'Please input your password!' },
                            { validator: validatePassword }
                        ]}
                    >
                        <Input.Password prefix={<LockOutlined />} placeholder="Password" />
                    </Form.Item>

                    <Form.Item
                        name="confirmPassword"
                        dependencies={['password']}
                        rules={[
                            { required: true, message: 'Please confirm your password!' },
                            ({ getFieldValue }) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue('password') === value) {
                                        return Promise.resolve();
                                    }
                                    return Promise.reject('The two passwords do not match!');
                                },
                            }),
                        ]}
                    >
                        <Input.Password prefix={<LockOutlined />} placeholder="Confirm Password" />
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" loading={loading} block>
                            Register
                        </Button>
                    </Form.Item>

                    <Form.Item style={{ textAlign: 'center' }}>
                        <Text>
                            Already have an account?{' '}
                            <a href="/login">Login</a>
                        </Text>
                    </Form.Item>
                </Form>
            </Space>
        </Card>
    );
};

export default RegistrationForm; 