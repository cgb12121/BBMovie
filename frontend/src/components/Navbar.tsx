import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Button, Dropdown, MenuProps } from 'antd';
import { UserOutlined, LogoutOutlined } from '@ant-design/icons';
import SearchBar from './SearchBar';
import { useAuth } from '../hooks/useAuth';
import { AuthButton, Logo, Nav, NavLinks, SearchContainer } from '../styles/NavbarStyles';

const Navbar: React.FC = () => {
  const { user, logout, loading } = useAuth();
  const navigate = useNavigate();
  const [searchLoading, setSearchLoading] = useState(false);

  const handleSearch = (value: string) => {
    if (!value.trim()) return;

    setSearchLoading(true);
    navigate(`/search?q=${encodeURIComponent(value)}`);
    setSearchLoading(false);
  };

  const handleLogout = () => {
    logout();
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
      icon: <LogoutOutlined />,
      onClick: handleLogout,
    },
  ];

  return (
    <Nav>
      <Logo to="/">BBMovie</Logo>

      <SearchContainer>
        <SearchBar
          placeholder="Search movies, categories..."
          onSearch={handleSearch}
          loading={searchLoading}
        />
      </SearchContainer>

      <NavLinks>
        <NavLink to="/movies">Movies</NavLink>
        <NavLink to="/categories">Categories</NavLink>

        {!loading && (
          user ? (
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Button type="text" icon={<UserOutlined />} style={{ color: '#fff' }}>
                {user?.firstName + ' ' + user?.lastName}
              </Button>
            </Dropdown>
          ) : (
            <>
              <NavLink to="/login">Login</NavLink>
              <AuthButton type="primary">
                <Link to="/register">Register</Link>
              </AuthButton>
            </>
          )
        )}
      </NavLinks>
    </Nav>
  );
};

export default Navbar;