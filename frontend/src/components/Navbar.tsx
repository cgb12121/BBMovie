import React, { useEffect, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Button, Dropdown, MenuProps } from 'antd';
import { UserOutlined, LogoutOutlined } from '@ant-design/icons';
import SearchBar from './SearchBar';
import { AuthButton, Logo, Nav, NavLinks, SearchContainer } from '../styles/NavbarStyles';

import { useDispatch, useSelector } from 'react-redux';
import { RootState } from '../redux/store';
import { logout } from '../redux/authSlice';


const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const [searchLoading, setSearchLoading] = useState(false);

  const user = useSelector((state: RootState) => state.auth.user);
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const dispatch = useDispatch();

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const handleSearch = (query: string, limit: number = 10) => {
    if (!query.trim()) return;

    setSearchLoading(true);
    navigate(`/search?query=${encodeURIComponent(query)}&limit=${limit}`);
    setSearchLoading(false);
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

        {isAuthenticated ? (
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <Button type="text" icon={<UserOutlined />} style={{ color: '#fff' }}>
              {user?.email ?? 'Account'}
            </Button>
          </Dropdown>
        ) : (
          <>
            <NavLink to="/login">Login</NavLink>
            <AuthButton type="primary">
              <Link to="/register">Register</Link>
            </AuthButton>
          </>
        )}
      </NavLinks>
    </Nav>
  );
};

export default Navbar;
