import React from 'react';
import { Link } from 'react-router-dom';
import styled from 'styled-components';

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
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
`;

const Logo = styled(Link)`
  font-size: 1.5rem;
  font-weight: bold;
  color: #e50914;
  text-decoration: none;
`;

const NavLinks = styled.div`
  display: flex;
  gap: 2rem;
  align-items: center;
`;

const NavLink = styled(Link)`
  color: #ffffff;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s;

  &:hover {
    color: #e50914;
  }
`;

const AuthButton = styled(Link)`
  background-color: #e50914;
  color: #ffffff;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
  transition: background-color 0.3s;

  &:hover {
    background-color: #f40612;
  }
`;

const Navbar: React.FC = () => {
  return (
    <Nav>
      <Logo to="/">BBMovie</Logo>
      <NavLinks>
        <NavLink to="/movies">Movies</NavLink>
        <NavLink to="/tv-shows">TV Shows</NavLink>
        <NavLink to="/categories">Categories</NavLink>
        <AuthButton to="/login">Login</AuthButton>
        <AuthButton to="/register">Register</AuthButton>
      </NavLinks>
    </Nav>
  );
};

export default Navbar; 