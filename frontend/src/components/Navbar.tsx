import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { Button, Dropdown, MenuProps } from 'antd'; // Update import
import { UserOutlined, LogoutOutlined } from '@ant-design/icons';
import SearchBar from './SearchBar';
import { useAuth } from '../hooks/useAuth';

const Nav = styled.nav`
  background-color: #1a1a1a;
  padding: 1rem 2rem;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
`;

const Logo = styled(Link)`
  font-size: 1.5rem;
  font-weight: bold;
  color: #e50914;
  text-decoration: none;
  margin-right: 2rem;
`;

const NavLinks = styled.div`
  display: flex;
  gap: 1.5rem;
  align-items: center;
`;

const NavLink = styled(Link)`
  color: #ffffff;
  text-decoration: none;
  font-size: 1rem;
  transition: color 0.3s;

  &:hover {
    color: #1890ff;
  }
`;

const SearchContainer = styled.div`
  flex: 1;
  max-width: 500px;
  margin: 0 2rem;
`;

const AuthButton = styled(Button)`
  margin-left: 1rem;
`;

const Navbar: React.FC = () => {
  const { user, logout, loading } = useAuth();
  const navigate = useNavigate();
  const [searchLoading, setSearchLoading] = useState(false);

  const handleSearch = async (value: string) => {
    if (!value.trim()) return;

    setSearchLoading(true);
    try {
      navigate(`/search?q=${encodeURIComponent(value)}`);
    } finally {
      setSearchLoading(false);
    }
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