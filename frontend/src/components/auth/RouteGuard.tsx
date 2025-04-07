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
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    if (roles.length > 0 && (!user || !roles.includes(user.roles.toString()))) {
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
};

export default RouteGuard; 