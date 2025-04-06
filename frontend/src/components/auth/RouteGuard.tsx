import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

interface RouteGuardProps {
    children: React.ReactNode;
    isProtected?: boolean;
    roles?: string[];
}

const RouteGuard: React.FC<RouteGuardProps> = ({ 
    children, 
    isProtected = false,
    roles = []
}) => {
    const { isAuthenticated, user } = useAuth();
    const location = useLocation();

    if (isProtected && !isAuthenticated) {
        // Redirect to login page with return url
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    if (roles.length > 0 && (!user || !roles.includes(user.role))) {
        // Redirect to home page if user doesn't have required role
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
};

export default RouteGuard; 