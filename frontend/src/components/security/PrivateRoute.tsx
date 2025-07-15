// frontend/src/components/PrivateRoute.tsx
import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';

const isAuthenticated = () => {
  // Replace with your actual auth logic (e.g., check Redux, context, or localStorage)
  return !!localStorage.getItem('accessToken');
};

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const location = useLocation();
  if (!isAuthenticated()) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return <>{children}</>;
};

export default PrivateRoute;