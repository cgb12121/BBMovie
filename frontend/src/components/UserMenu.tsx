import React from 'react';
import { MenuProps, Dropdown, Button } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { logout } from '../redux/authSlice';

interface UserMenuProps {
  userEmail: string | undefined;
}

const UserMenu: React.FC<UserMenuProps> = ({ userEmail }) => {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      label: <Link to="/profile">Profile</Link>,
      icon: <UserOutlined />,
    },
    {
      key: 'logout',
      label: 'Logout',
      icon: <UserOutlined />,
      onClick: handleLogout,
    },
  ];

  return (
    <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
      <Button type="text" icon={<UserOutlined />} style={{ color: '#fff' }}>
        {userEmail ?? 'Account'}
      </Button>
    </Dropdown>
  );
};

export default UserMenu;