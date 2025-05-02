import React from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../redux/store';
import { Card, Avatar, Typography, Button, Space, Alert } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import { DEFAULT_PROFILE_PICTURE } from '../assets/DefaultProfilePicture';

const { Title, Text } = Typography;

const Profile: React.FC = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  const role = useSelector((state: RootState) => state.auth.auth?.role);

  if (!user) {
    return (
      <div
        style={{
          minHeight: '100vh',
          backgroundColor: '#141414',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: '2rem',
        }}
      >
        <Alert
          message="Please log in to view your profile."
          type="warning"
          showIcon
          style={{
            maxWidth: 400,
            backgroundColor: '#2a2a2a',
            border: 'none',
            color: '#ffffff',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
          }}
        />
      </div>
    );
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: '#141414',
        padding: '2rem',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
      }}
    >
      <Card
        style={{
          maxWidth: 600,
          width: '100%',
          backgroundColor: '#1f1f1f',
          border: 'none',
          borderRadius: 12,
          boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
          color: '#ffffff',
        }}
        bodyStyle={{ padding: '2rem' }}
      >
        <Space direction="vertical" size="large" style={{ width: '100%', textAlign: 'center' }}>
          <Avatar
            size={150}
            src={user.profilePictureUrl || DEFAULT_PROFILE_PICTURE}
            style={{
              border: '2px solid #E50914',
              objectFit: 'cover',
            }}
          />
          <Title level={2} style={{ color: '#ffffff', margin: 0 }}>
            Welcome, {user.firstName} {user.lastName}
          </Title>
          <Text style={{ color: '#b3b3b3', fontSize: '1.1rem' }}>
            Email: {user.email}
          </Text>
          {role && (
            <Text style={{ color: '#b3b3b3', fontSize: '1.1rem' }}>
              Role: {role}
            </Text>
          )}
          <Button
            type="primary"
            icon={<EditOutlined />}
            style={{
              backgroundColor: '#E50914',
              borderColor: '#E50914',
              borderRadius: 4,
              padding: '0.5rem 2rem',
              height: 'auto',
              fontWeight: 'bold',
            }}
            onClick={() => {
              console.log('Edit Profile clicked');
            }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#F40612')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '#E50914')}
          >
            Edit Profile
          </Button>
        </Space>
      </Card>
    </div>
  );
};

export default Profile;