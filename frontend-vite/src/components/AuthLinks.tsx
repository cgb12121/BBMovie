import React from 'react';
import { Link } from 'react-router-dom';
import { AuthButton } from '../styles/NavbarStyles';

const AuthLinks: React.FC = () => {
  return (
    <>
      <Link to="/login">Login</Link>
      <AuthButton type="primary">
        <Link to="/register">Register</Link>
      </AuthButton>
    </>
  );
};

export default AuthLinks;
