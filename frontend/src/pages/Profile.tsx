import React, { useState, useEffect } from 'react';
import { Typography, Card, Form, Input, Button, Avatar, message, Spin } from 'antd';
import { UserOutlined, MailOutlined, LockOutlined } from '@ant-design/icons';
import styled from 'styled-components';
import { useAuth } from '../hooks/useAuth';

const { Title, Text } = Typography;

const ProfileContainer = styled.div`
    padding: 2rem;
    max-width: 800px;
    margin: 0 auto;
`;

const ProfileHeader = styled.div`
    display: flex;
    align-items: center;
    gap: 2rem;
    margin-bottom: 2rem;
`;

const ProfileCard = styled(Card)`
    margin-bottom: 2rem;
`;

const Profile: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [form] = Form.useForm();
    const { user, updateProfile } = useAuth();

    useEffect(() => {
        if (user) {
            form.setFieldsValue({
                firstName: user.firstName,
                lastName: user.lastName,
                email: user.email
            });
        }
    }, [user, form]);

    const handleSubmit = async (values: any) => {
        try {
            setLoading(true);
            await updateProfile(values);
            message.success('Profile updated successfully');
        } catch (error) {
            message.error('Failed to update profile');
        } finally {
            setLoading(false);
        }
    };

    if (!user) {
        return (
            <div style={{ textAlign: 'center', padding: '2rem' }}>
                <Spin size="large" />
            </div>
        );
    }

    return (
        <ProfileContainer>
            <ProfileHeader>
                <Avatar size={64} icon={<UserOutlined />} />
                <div>
                    <Title level={2}>{user.firstName} {user.lastName}</Title>
                    <Text type="secondary">{user.email}</Text>
                </div>
            </ProfileHeader>

            <ProfileCard title="Profile Information">
                <Form
                    form={form}
                    layout="vertical"
                    onFinish={handleSubmit}
                    initialValues={{
                        firstName: user.firstName,
                        lastName: user.lastName,
                        email: user.email
                    }}
                >
                    <Form.Item
                        name="firstName"
                        label="First Name"
                        rules={[{ required: true, message: 'Please input your first name!' }]}
                    >
                        <Input prefix={<UserOutlined />} />
                    </Form.Item>

                    <Form.Item
                        name="lastName"
                        label="Last Name"
                        rules={[{ required: true, message: 'Please input your last name!' }]}
                    >
                        <Input prefix={<UserOutlined />} />
                    </Form.Item>

                    <Form.Item
                        name="email"
                        label="Email"
                        rules={[
                            { required: true, message: 'Please input your email!' },
                            { type: 'email', message: 'Please enter a valid email!' }
                        ]}
                    >
                        <Input prefix={<MailOutlined />} />
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" loading={loading}>
                            Update Profile
                        </Button>
                    </Form.Item>
                </Form>
            </ProfileCard>

            <ProfileCard title="Change Password">
                <Form layout="vertical">
                    <Form.Item
                        name="currentPassword"
                        label="Current Password"
                        rules={[{ required: true, message: 'Please input your current password!' }]}
                    >
                        <Input.Password prefix={<LockOutlined />} />
                    </Form.Item>

                    <Form.Item
                        name="newPassword"
                        label="New Password"
                        rules={[{ required: true, message: 'Please input your new password!' }]}
                    >
                        <Input.Password prefix={<LockOutlined />} />
                    </Form.Item>

                    <Form.Item
                        name="confirmPassword"
                        label="Confirm New Password"
                        dependencies={['newPassword']}
                        rules={[
                            { required: true, message: 'Please confirm your new password!' },
                            ({ getFieldValue }) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue('newPassword') === value) {
                                        return Promise.resolve();
                                    }
                                    return Promise.reject(new Error('The two passwords do not match!'));
                                },
                            }),
                        ]}
                    >
                        <Input.Password prefix={<LockOutlined />} />
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" loading={loading}>
                            Change Password
                        </Button>
                    </Form.Item>
                </Form>
            </ProfileCard>
        </ProfileContainer>
    );
};

export default Profile; 