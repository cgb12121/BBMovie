// frontend/src/components/PrivateRoute.tsx
import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { getAccessToken, isTokenExpired } from '../../utils/AccessTokenUtil';
import authService from '../../services/authService';

const isAuthenticated = (): boolean => {
  const token = getAccessToken();
  if (!token) return false;
  return !isTokenExpired(token);
};

interface PrivateRouteProps {
  children: React.ReactNode;
  requiredRoles?: string[];
}

const hasRequiredRoles = (requiredRoles?: string[]): boolean => {
  if (!requiredRoles || requiredRoles.length === 0) return true;
  return requiredRoles.some((role) => authService.hasRole(role));
};

const PrivateRoute: React.FC<PrivateRouteProps> = ({ children, requiredRoles }) => {
  const location = useLocation();
  if (!isAuthenticated()) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (!hasRequiredRoles(requiredRoles)) {
    return <Navigate to="/forbidden" state={{ from: location }} replace />;
  }
  return <>{children}</>;
};

export default PrivateRoute;
