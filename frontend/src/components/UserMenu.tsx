import React from 'react';
import { MenuProps, Dropdown, Avatar, Menu, Space, Typography, message } from 'antd';
import { UserOutlined, SettingOutlined, QuestionCircleOutlined, EyeOutlined, MessageOutlined, LogoutOutlined, ProfileOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { RootState } from '../redux/store';
import { logout } from '../redux/authSlice';
import api from '../services/api';
import { DEFAULT_PROFILE_PICTURE } from '../assets/DefaultProfilePicture';

const { Text } = Typography;

const UserMenu: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const user = useSelector((state: RootState) => state.auth.user);

  const handleLogout = async () => {
    try {
      await api.post('/api/auth/logout');
      dispatch(logout());
      message.success('Logged out successfully');
      navigate('/login');
    } catch (err) {
      console.error('Logout error:', err);
      message.error('Failed to log out. Please try again.');
    }
  };

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'user-profile',
      label: (
        <Space align="center">
          <Avatar
            src={user?.profilePictureUrl || DEFAULT_PROFILE_PICTURE}
            size={40}
            icon={<UserOutlined />}
            alt={user ? `${user.firstName} ${user.lastName}` : 'User'}
          />
          <Text strong style={{ color: '#fff', fontSize: '16px' }}>
            {user ? `${user.firstName} ${user.lastName}` : 'User'}
          </Text>
        </Space>
      ),
      disabled: true,
    },
    {
      type: 'divider',
    },
    {
      key: 'profile',
      label: <Link to={"/profile"}>Profile</Link>,
      icon: <ProfileOutlined />,
    },
    {
      key: 'settings',
      label: <Link to="/settings">Settings & Privacy</Link>,
      icon: <SettingOutlined />,
    },
    {
      key: 'help',
      label: <Link to="/help">Help & Support</Link>,
      icon: <QuestionCircleOutlined />,
    },
    {
      key: 'display',
      label: <Link to="/display">Display & Accessibility</Link>,
      icon: <EyeOutlined />,
    },
    {
      key: 'feedback',
      label: 'Give Feedback',
      icon: <MessageOutlined />,
      extra: 'Ctrl B',
      onClick: () => navigate('/feedback'),
    },
    {
      key: 'logout',
      label: 'Log Out',
      icon: <LogoutOutlined />,
      onClick: handleLogout,
    },
    {
      type: 'divider',
    },
  ];

  return (
    <Dropdown
      menu={{ items: userMenuItems }}
      placement="bottomRight"
      overlayStyle={{
        backgroundColor: '#1c2526',
        border: '1px solid #333',
        borderRadius: '8px',
        width: '250px',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
      }}
      overlayClassName="custom-user-menu"
    >
      <Avatar
        src={user?.profilePictureUrl || DEFAULT_PROFILE_PICTURE}
        size={32}
        icon={!user?.profilePictureUrl && <UserOutlined />}
        alt={user ? `${user.firstName} ${user.lastName}` : 'User'}
        style={{
          cursor: 'pointer',
          border: '2px solid #fff',
        }}
      />
    </Dropdown>
  );
};

const styles = `
  .custom-user-menu .ant-dropdown-menu {
    background-color: #1c2526 !important;
    padding: 8px 0 !important;
  }
  .custom-user-menu .ant-dropdown-menu-item,
  .custom-user-menu .ant-dropdown-menu-item-disabled {
    color: #fff !important;
    padding: 8px 16px !important;
    font-size: 14px !important;
  }
  .custom-user-menu .ant-dropdown-menu-item:hover:not(.ant-dropdown-menu-item-disabled) {
    background-color: #2a3435 !important;
  }
  .custom-user-menu .ant-dropdown-menu-item-icon {
    margin-right: 12px !important;
    font-size: 16px !important;
  }
  .custom-user-menu .ant-dropdown-menu-item-divider {
    background-color: #444 !important;
    margin: 4px 0 !important;
  }
  .custom-user-menu .ant-dropdown-menu-item a {
    color: #fff !important;
  }
  .custom-user-menu .ant-dropdown-menu-item a:hover {
    color: #fff !important;
  }
`;

const styleSheet = document.createElement('style');
styleSheet.innerText = styles;
document.head.appendChild(styleSheet);

export default UserMenu;